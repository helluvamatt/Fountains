package com.schneenet.fountainsplugin;

import com.schneenet.fountainsplugin.models.Fountain;
import com.schneenet.fountainsplugin.models.Intake;
import com.schneenet.fountainsplugin.models.RedstoneRequirementState;
import com.schneenet.fountainsplugin.models.Valve;

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
public class FountainsDal {
	private File databaseFile;

	private static final String INITIALIZE_SCHEMA = "CREATE TABLE IF NOT EXISTS schema (version INTEGER NOT NULL)";

	private static final String[][] MIGRATIONS = {
			{
					// Version 1
					"CREATE TABLE IF NOT EXISTS fountains (name TEXT UNIQUE, world TEXT, x INTEGER, y INTEGER, z INTEGER, power INTEGER, redstone INTEGER)",
					"CREATE TABLE IF NOT EXISTS intakes (name TEXT UNIQUE, world TEXT, x INTEGER, y INTEGER, z INTEGER, speed INTEGER, redstone INTEGER)",
					"CREATE TABLE IF NOT EXISTS valves (name TEXT UNIQUE, world TEXT, x INTEGER, y INTEGER, z INTEGER)"
			}
	};

	/**
	 * C'tor
	 *
	 * @param databaseFile Database file
	 * @throws DalException When an error occurs
	 */
	public FountainsDal(File databaseFile) throws DalException {
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

	/**
	 * Get all fountains in the database
	 *
	 * @return List of Fountain
	 * @throws DalException When an error occurs
	 */
	public List<Fountain> getFountains() throws DalException {
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

	/**
	 * Get a fountain by its row ID
	 *
	 * @param id ID
	 * @return Fountain, null if no fountain exists with the given row ID
	 * @throws DalException When an error occurs
	 */
	public Fountain getFountain(long id) throws DalException {
		try {
			Fountain returnVal = null;
			Connection conn = this.openConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT name, world, x, y, z, power, redstone, rowid FROM fountains WHERE rowid = ?");
			stmt.setLong(1, id);
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

	/**
	 * Get a single fountain by name
	 *
	 * @param name Name
	 * @return Fountain, null if no fountain exists by the given name
	 * @throws DalException When an error occurs
	 */
	public Fountain getFountain(String name) throws DalException {
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

	/**
	 * Persist a fountain to the database
	 *
	 * @param fountain Fountain to persist
	 * @return true on success
	 * @throws DalException When an error occurs
	 */
	public boolean saveFountain(Fountain fountain) throws DalException {
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
					return pStmt.executeUpdate() == 1;
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
							return true;
						}
					}
				}
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
			return false;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	/**
	 * Delete a fountain from the database
	 *
	 * @param fountain Fountain to delete
	 * @return true if the fountain was deleted successfully
	 * @throws DalException When an error occurs
	 */
	public boolean deleteFountain(Fountain fountain) throws DalException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("DELETE FROM fountains WHERE rowid = ?");
				pStmt.setLong(1, fountain.getId());
				return pStmt.executeUpdate() == 1;
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

	/**
	 * Get all Intakes
	 *
	 * @return List of Intake
	 * @throws DalException When an error occurs
	 */
	public List<Intake> getIntakes() throws DalException {
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

	/**
	 * Get a single Intake by row ID
	 *
	 * @param id ID
	 * @return Intake, null if none exists for the given row ID
	 * @throws DalException When an error occurs
	 */
	public Intake getIntake(long id) throws DalException {
		try {
			Intake returnVal = null;
			Connection conn = this.openConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT name, world, x, y, z, speed, redstone, rowid FROM intakes WHERE rowid = ?");
			stmt.setLong(1, id);
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

	/**
	 * Get a single Intake by name
	 *
	 * @param name Name
	 * @return Intake, null if none exists for the given name
	 * @throws DalException When an error occurs
	 */
	public Intake getIntake(String name) throws DalException {
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

	/**
	 * Save an Intake to the database
	 *
	 * @param intake Intake to persist
	 * @return true on success
	 * @throws DalException When an error occurs
	 */
	public boolean saveIntake(Intake intake) throws DalException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				if (intake.getId() > 0) {
					pStmt = conn.prepareStatement("UPDATE intakes SET speed = ? WHERE rowid = ?");
					pStmt.setInt(1, intake.getSpeed());
					pStmt.setLong(2, intake.getId());
					return pStmt.executeUpdate() == 1;
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
							return true;
						}
					}
				}
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
			return false;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	/**
	 * Delete an Intake from the database
	 *
	 * @param intake Intake to delete
	 * @return true on success
	 * @throws DalException When an error occurs
	 */
	public boolean deleteIntake(Intake intake) throws DalException {
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("DELETE FROM intakes WHERE rowid = ?");
				pStmt.setLong(1, intake.getId());
				return pStmt.executeUpdate() == 1;
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

	/**
	 * Get all Valve objects
	 * @return List of Valve objects
	 * @throws DalException When an error occurs
	 */
	public List<Valve> getValves() throws DalException
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

	/**
	 * Get a Valve by its row ID
	 * @param id ID
	 * @return Valve, null if none exist
	 * @throws DalException When an error occurs
	 */
	public Valve getValve(long id) throws DalException
	{
		try {
			Valve returnVal = null;
			Connection conn = this.openConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT name, world, x, y, z, rowid FROM valves WHERE rowid = ?");
			stmt.setLong(1, id);
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

	/**
	 * Get a Valve by name
	 * @param name Name
	 * @return Valve, null if none exists
	 * @throws DalException When an error occurs
	 */
	public Valve getValve(String name) throws DalException
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

	/**
	 * Save a Valve to the database
	 * @param valve Valve
	 * @return true on success
	 * @throws DalException When an error occurs
	 */
	public boolean saveValve(Valve valve) throws DalException
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
						return true;
					}
				}
			} finally {
				if (pStmt != null)
					pStmt.close();
				if (conn != null)
					conn.close();
			}
			return false;
		} catch (SQLException ex) {
			throw new DalException(ex);
		}
	}

	/**
	 *
	 * @param valve Valve to delete
	 * @return true on success
	 * @throws DalException When an error occurs
	 */
	public boolean deleteValve(Valve valve) throws DalException
	{
		try {
			Connection conn = null;
			PreparedStatement pStmt = null;
			try {
				conn = this.openConnection();
				pStmt = conn.prepareStatement("DELETE FROM valves WHERE rowid = ?");
				pStmt.setLong(1, valve.getId());
				return pStmt.executeUpdate() == 1;
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

}
