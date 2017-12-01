package com.schneenet.fountainsplugin;

import com.schneenet.fountainsplugin.models.*;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fountains DAL
 *
 * @author Matt Schneeberger
 */
class FountainsDal {
	private File databaseFile;

	private static final String INITIALIZE_SCHEMA = "CREATE TABLE IF NOT EXISTS schema (version INTEGER NOT NULL)";

	private static final String[][] MIGRATIONS = {
			{
					// Version 1
					"CREATE TABLE IF NOT EXISTS fountains (name TEXT UNIQUE, world TEXT, x INTEGER, y INTEGER, z INTEGER, power INTEGER, redstone INTEGER)",
					"CREATE TABLE IF NOT EXISTS intakes (name TEXT UNIQUE, world TEXT, x INTEGER, y INTEGER, z INTEGER, speed INTEGER, redstone INTEGER)",
					"CREATE TABLE IF NOT EXISTS valves (name TEXT UNIQUE, world TEXT, x INTEGER, y INTEGER, z INTEGER)"
			},
			{
					// Version 2
					"CREATE TABLE IF NOT EXISTS sprinklers (name TEXT UNIQUE, world TEXT, x INTEGER, y INTEGER, z INTEGER, spread INTEGER, redstone INTEGER)",
			}
	};

	/**
	 * C'tor
	 *
	 * @param databaseFile Database file
	 * @throws DalException When an error occurs
	 */
	FountainsDal(File databaseFile) throws DalException {
		try {
			this.databaseFile = databaseFile;
			this.databaseFile.createNewFile();
			Connection conn = null;
			Statement stmt = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				stmt = conn.createStatement();
				stmt.executeUpdate(INITIALIZE_SCHEMA);
				ResultSet schemaVersionResult = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema");
				int schemaVersion = 0;
				if (schemaVersionResult.next()) {
					schemaVersion = schemaVersionResult.getInt(1);
				}
				schemaVersionResult.close();
				if (schemaVersion < MIGRATIONS.length) {
					for (int i = schemaVersion; i < MIGRATIONS.length; i++) {
						for (String sql : MIGRATIONS[i]) {
							stmt.execute(sql);
						}
					}
					pStmt = conn.prepareStatement("INSERT INTO schema (version) VALUES (?)");
					pStmt.setInt(1, MIGRATIONS.length);
					pStmt.executeUpdate();
				}
			} finally {
				if (pStmt != null) pStmt.close();
				if (stmt != null) stmt.close();
				if (conn != null) conn.close();
			}
		} catch (IOException | SQLException ex) {
			throw new DalException(ex);
		}
	}

	List<Fountain> getFountains() throws DalException {
		try {
			Connection conn = this.openConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT name, world, x, y, z, power, redstone, rowid FROM fountains ORDER BY rowid ASC");
			ArrayList<Fountain> fountains = new ArrayList<>();
			while (rs.next()) {
				fountains.add(new Fountain(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getInt(6), RedstoneRequirementState.valueOf(rs.getInt(7)), rs.getLong(8)));
			}
			rs.close();
			stmt.close();
			conn.close();
			return fountains;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	Fountain getFountain(String name) throws DalException {
		try {
			Fountain returnVal = null;
			Connection conn = this.openConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT name, world, x, y, z, power, redstone, rowid FROM fountains WHERE name = ?");
			stmt.setString(1, name);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				returnVal = new Fountain(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getInt(6), RedstoneRequirementState.valueOf(rs.getInt(7)), rs.getLong(8));
			}
			rs.close();
			stmt.close();
			conn.close();
			return returnVal;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	void saveFountain(Fountain fountain) throws DalException, DuplicateKeyException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				if (fountain.getId() > 0) {
					pStmt = conn.prepareStatement("UPDATE fountains SET power = ?, redstone = ? WHERE rowid = ?");
					pStmt.setInt(1, fountain.getPower());
					pStmt.setInt(2, fountain.getRedstoneRequirementState().getValue());
					pStmt.setLong(3, fountain.getId());
					pStmt.executeUpdate();
				} else {
					pStmt = conn.prepareStatement("INSERT INTO fountains (name, world, x, y, z, power, redstone) VALUES (?, ?, ?, ?, ?, ?, ?)");
					pStmt.setString(1, fountain.getName());
					pStmt.setString(2, fountain.getWorldName());
					pStmt.setLong(3, fountain.getX());
					pStmt.setLong(4, fountain.getY());
					pStmt.setLong(5, fountain.getZ());
					pStmt.setInt(6, fountain.getPower());
					pStmt.setInt(7, fountain.getRedstoneRequirementState().getValue());
					if (pStmt.executeUpdate() == 1) {
						ResultSet keyResult = pStmt.getGeneratedKeys();
						if (keyResult.next()) {
							fountain.setId(keyResult.getLong(1));
							keyResult.close();
						}
					}
				}
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
		} catch (SQLException ex) {
			handleSqlException(ex);
		}
	}

	void deleteFountain(Fountain fountain) throws DalException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("DELETE FROM fountains WHERE rowid = ?");
				pStmt.setLong(1, fountain.getId());
				pStmt.executeUpdate();
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	List<Intake> getIntakes() throws DalException {
		try {
			Connection conn = this.openConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT name, world, x, y, z, speed, redstone, rowid FROM intakes ORDER BY rowid ASC");
			ArrayList<Intake> intakes = new ArrayList<>();
			while (rs.next()) {
				intakes.add(new Intake(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getInt(6), RedstoneRequirementState.valueOf(rs.getInt(7)), rs.getLong(8)));
			}
			rs.close();
			stmt.close();
			conn.close();
			return intakes;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	Intake getIntake(String name) throws DalException {
		try {
			Intake returnVal = null;
			Connection conn = this.openConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT name, world, x, y, z, speed, redstone, rowid FROM intakes WHERE name = ?");
			stmt.setString(1, name);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				returnVal = new Intake(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getInt(6), RedstoneRequirementState.valueOf(rs.getInt(7)), rs.getLong(8));
			}
			rs.close();
			stmt.close();
			conn.close();
			return returnVal;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	void saveIntake(Intake intake) throws DalException, DuplicateKeyException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				if (intake.getId() > 0) {
					pStmt = conn.prepareStatement("UPDATE intakes SET speed = ? WHERE rowid = ?");
					pStmt.setInt(1, intake.getSpeed());
					pStmt.setLong(2, intake.getId());
					pStmt.executeUpdate();
				} else {
					pStmt = conn.prepareStatement("INSERT INTO intakes (name, world, x, y, z, speed) VALUES (?, ?, ?, ?, ?, ?)");
					pStmt.setString(1, intake.getName());
					pStmt.setString(2, intake.getWorldName());
					pStmt.setLong(3, intake.getX());
					pStmt.setLong(4, intake.getY());
					pStmt.setLong(5, intake.getZ());
					pStmt.setInt(6, intake.getSpeed());
					if (pStmt.executeUpdate() == 1) {
						ResultSet keyResult = pStmt.getGeneratedKeys();
						if (keyResult.next()) {
							intake.setId(keyResult.getLong(1));
							keyResult.close();
						}
					}
				}
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
		} catch (SQLException ex) {
			handleSqlException(ex);
		}
	}

	void deleteIntake(Intake intake) throws DalException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("DELETE FROM intakes WHERE rowid = ?");
				pStmt.setLong(1, intake.getId());
				pStmt.executeUpdate();
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	List<Valve> getValves() throws DalException
	{
		try {
			List<Valve> returnVal = new ArrayList<>();
			Connection conn = this.openConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT name, world, x, y, z, rowid FROM valves ORDER BY rowid ASC");
			while (rs.next()) {
				returnVal.add(new Valve(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6)));
			}
			rs.close();
			stmt.close();
			conn.close();
			return returnVal;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	Valve getValve(String name) throws DalException
	{
		try {
			Valve returnVal = null;
			Connection conn = this.openConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT name, world, x, y, z, rowid FROM valves WHERE name = ?");
			stmt.setString(1, name);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				returnVal = new Valve(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6));
			}
			rs.close();
			stmt.close();
			conn.close();
			return returnVal;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	void saveValve(Valve valve) throws DalException, DuplicateKeyException
	{
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("INSERT INTO valves (name, world, x, y, z) VALUES (?, ?, ?, ?, ?)");
				pStmt.setString(1, valve.getName());
				pStmt.setString(2, valve.getWorldName());
				pStmt.setLong(3, valve.getX());
				pStmt.setLong(4, valve.getY());
				pStmt.setLong(5, valve.getZ());
				if (pStmt.executeUpdate() == 1) {
					ResultSet keyResult = pStmt.getGeneratedKeys();
					if (keyResult.next()) {
						valve.setId(keyResult.getLong(1));
						keyResult.close();
					}
				}
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
		} catch (SQLException ex) {
			handleSqlException(ex);
		}
	}

	void deleteValve(Valve valve) throws DalException
	{
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("DELETE FROM valves WHERE rowid = ?");
				pStmt.setLong(1, valve.getId());
				pStmt.executeUpdate();
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	List<Sprinkler> getSprinklers() throws DalException {
		try {
			List<Sprinkler> returnVal = new ArrayList<>();
			Connection conn = this.openConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT name, world, x, y, z, spread, redstone, rowid FROM sprinklers ORDER BY rowid ASC");
			while (rs.next()) {
				returnVal.add(new Sprinkler(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getInt(6), RedstoneRequirementState.valueOf(rs.getInt(7)), rs.getLong(8)));
			}
			rs.close();
			stmt.close();
			conn.close();
			return returnVal;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	Sprinkler getSprinkler(String name) throws DalException {
		try {
			Sprinkler returnVal = null;
			Connection conn = this.openConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT name, world, x, y, z, spread, redstone, rowid FROM sprinklers WHERE name = ?");
			stmt.setString(1, name);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				returnVal = new Sprinkler(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getInt(6), RedstoneRequirementState.valueOf(rs.getInt(7)), rs.getLong(8));
			}
			rs.close();
			stmt.close();
			conn.close();
			return returnVal;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	void saveSprinkler(Sprinkler sprinkler) throws DalException, DuplicateKeyException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("INSERT INTO sprinklers (name, world, x, y, z, spread, redstone) VALUES (?, ?, ?, ?, ?, ?, ?)");
				pStmt.setString(1, sprinkler.getName());
				pStmt.setString(2, sprinkler.getWorldName());
				pStmt.setLong(3, sprinkler.getX());
				pStmt.setLong(4, sprinkler.getY());
				pStmt.setLong(5, sprinkler.getZ());
				pStmt.setInt(6, sprinkler.getSpread());
				pStmt.setInt(7, sprinkler.getRedstoneRequirementState().getValue());
				pStmt.executeUpdate();
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
		} catch (SQLException ex) {
			handleSqlException(ex);
		}
	}

	void deleteSprinkler(Sprinkler sprinkler) throws DalException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("DELETE FROM sprinklers WHERE rowid = ?");
				pStmt.setLong(1, sprinkler.getId());
				pStmt.executeUpdate();
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	private Connection openConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile.getPath());
	}

	private void handleSqlException(SQLException ex) throws DalException, DuplicateKeyException {
		if (ex instanceof SQLiteException && ((SQLiteException) ex).getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE) {
			throw new DuplicateKeyException(ex);
		} else {
			throw new DalException(ex);
		}
	}

}
