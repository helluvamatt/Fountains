package com.schneenet.fountainsplugin;

import com.schneenet.fountainsplugin.config.FountainsConfig;
import com.schneenet.fountainsplugin.config.R;
import com.schneenet.fountainsplugin.models.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Hopper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FountainsManager implements Listener, Runnable {

	private static final String DYNMAP_PLUGIN = "dynmap";
	private static final String DYNMAP_MARKER_ICON_FOUNTAIN = "fountain";
	private static final String DYNMAP_MARKER_SET_FOUNTAIN = "fountains";
	private static final String DYNMAP_MARKER_ICON_INTAKE = "intake";
	private static final String DYNMAP_MARKER_SET_INTAKE = "intakes";
	private static final String DYNMAP_MARKER_ICON_VALVE = "valve";
	private static final String DYNMAP_MARKER_SET_VALVE = "valves";
	private static final String DYNMAP_MARKER_ICON_SPRINKLER = "sprinkler";
	private static final String DYNMAP_MARKER_SET_SPRINKLER = "sprinklers";
	private static final String DYNMAP_MARKER_SET_PIPES = "pipes";

	private boolean dynmapEnabled = false;
	private MarkerSet dynmapFountains;
	private MarkerSet dynmapIntakes;
	private MarkerSet dynmapValves;
	private MarkerSet dynmapSprinklers;
	private MarkerSet dynmapPipes;

	private HashMap<Location, Model<Fountain>> fountains;
	private HashMap<Location, Model<Intake>> intakes;
	private HashMap<Location, Model<Valve>> valves;
	private HashMap<Location, Model<Sprinkler>> sprinklers;

	private FountainsDal dal;
	private Logger logger;
	private FountainsPlugin plugin;
	private FountainsConfig config;

	private Random rng = new Random();

	public FountainsManager(FountainsPlugin plugin, FountainsConfig config, FountainsDal dal, Logger logger) {
		this.plugin = plugin;
		this.config = config;
		this.dal = dal;
		this.logger = logger;
		fountains = new HashMap<>();
		intakes = new HashMap<>();
		valves = new HashMap<>();
		sprinklers = new HashMap<>();

		try {
			for (Fountain fountain : dal.getFountains()) {
				World world = plugin.getServer().getWorld(fountain.getWorldName());
				if (world != null) {
					enableFountain(fountain);
				} else {
					dal.deleteFountain(fountain);
				}
			}
			logger.log(Level.INFO, String.format("Loaded %d Fountains from database.", fountains.size()));

			for (Intake intake : dal.getIntakes()) {
				World world = plugin.getServer().getWorld(intake.getWorldName());
				if (world != null) {
					enableIntake(intake);
				} else {
					dal.deleteIntake(intake);
				}
			}
			logger.log(Level.INFO, String.format("Loaded %d Intakes from database.", intakes.size()));

			for (Valve valve : dal.getValves()) {
				World world = plugin.getServer().getWorld(valve.getWorldName());
				if (world != null) {
					enableValve(valve);
				} else {
					dal.deleteValve(valve);
				}
			}
			logger.log(Level.INFO, String.format("Loaded %d Valves from database.", valves.size()));

			for (Sprinkler sprinkler : dal.getSprinklers()) {
				World world = plugin.getServer().getWorld(sprinkler.getWorldName());
				if (world != null) {
					enableSprinkler(sprinkler);
				} else {
					dal.deleteSprinkler(sprinkler);
				}
			}
			logger.log(Level.INFO, String.format("Loaded %d Sprinklers from database.", sprinklers.size()));

			Plugin dynmap = plugin.getServer().getPluginManager().getPlugin(DYNMAP_PLUGIN);
			if (dynmap != null) {
				enableDynmap((DynmapAPI) dynmap);
			}

		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to initialize FountainsManager: " + ex.getMessage(), ex);
		}
	}

	void shutdown() {
		dynmapEnabled = false;
		try {
			for (Fountain fountain : dal.getFountains()) {
				World world = plugin.getServer().getWorld(fountain.getWorldName());
				if (world != null) {
					disableFountain(fountain);
				} else {
					dal.deleteFountain(fountain);
				}
			}

			for (Intake intake : dal.getIntakes()) {
				World world = plugin.getServer().getWorld(intake.getWorldName());
				if (world != null) {
					disableIntake(intake);
				} else {
					dal.deleteIntake(intake);
				}
			}

			for (Valve valve : dal.getValves()) {
				World world = plugin.getServer().getWorld(valve.getWorldName());
				if (world != null) {
					disableValve(valve);
				} else {
					dal.deleteValve(valve);
				}
			}

			for (Sprinkler sprinkler : dal.getSprinklers()) {
				World world = plugin.getServer().getWorld(sprinkler.getWorldName());
				if (world != null) {
					disableSprinkler(sprinkler);
				} else {
					dal.deleteSprinkler(sprinkler);
				}
			}
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to shutdown FountainsManager: " + ex.getMessage(), ex);
		}
	}

	public List<Fountain> getFountains() {
		try {
			return dal.getFountains();
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to get fountains: " + ex.getMessage(), ex);
		}
		return Collections.emptyList();
	}

	public Fountain getFountain(String name) {
		try {
			return dal.getFountain(name);
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to get fountain: " + ex.getMessage(), ex);
		}
		return null;
	}

	public boolean createFountain(CommandSender sender, Fountain fountain) {
		try {
			World world = plugin.getServer().getWorld(fountain.getWorldName());
			Location loc = getLocation(world, fountain);
			if (fountains.containsKey(loc)) {
				sender.sendMessage(Utils.colorSpan(ChatColor.RED, "This is already a fountain."));
				return false;
			}
			dal.saveFountain(fountain);
			enableFountain(fountain);
			return true;
		} catch (DuplicateKeyException ex) {
			sender.sendMessage(ChatColor.RED + "Fountain already exists with that name.");
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to create fountain: " + ex.getMessage(), ex);
		}
		return false;
	}

	public boolean removeFountain(Fountain fountain) {
		try {
			disableFountain(fountain);
			dal.deleteFountain(fountain);
			return true;
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to remove fountain: " + ex.getMessage(), ex);
		}
		return false;
	}

	private void enableFountain(Fountain fountain) {
		World world = plugin.getServer().getWorld(fountain.getWorldName());
		Location loc = getLocation(world, fountain);
		fountains.put(loc, new Model<>(world, fountain));
		if (dynmapEnabled && dynmapFountains != null) {
			MarkerIcon icon = dynmapFountains.getDefaultMarkerIcon();
			dynmapFountains.createMarker(fountain.getName(), "Fountain: \"" + fountain.getName() + "\"", false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
		}
	}

	private void disableFountain(Fountain fountain) {
		World world = plugin.getServer().getWorld(fountain.getWorldName());
		Location loc = getLocation(world, fountain);
		buildWaterColumn(world, loc, 0);
		if (dynmapEnabled && dynmapFountains != null) {
			Marker marker = dynmapFountains.findMarker(fountain.getName());
			if (marker != null) {
				marker.deleteMarker();
			}
		}
		fountains.remove(loc);
	}

	public List<Intake> getIntakes() {
		try {
			return dal.getIntakes();
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to get intakes: " + ex.getMessage(), ex);
		}
		return Collections.emptyList();
	}

	public Intake getIntake(String name) {
		try {
			return dal.getIntake(name);
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to get intake: " + ex.getMessage(), ex);
		}
		return null;
	}

	public boolean createIntake(CommandSender sender, Intake intake) {
		try {
			World world = plugin.getServer().getWorld(intake.getWorldName());
			Location loc = getLocation(world, intake);
			if (fountains.containsKey(loc)) {
				sender.sendMessage(Utils.colorSpan(ChatColor.RED, "This is already an intake."));
				return false;
			}
			dal.saveIntake(intake);
			enableIntake(intake);
			return true;
		} catch (DuplicateKeyException ex) {
			sender.sendMessage(ChatColor.RED + "Intake already exists with that name.");
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to create intake: " + ex.getMessage(), ex);
		}
		return false;
	}

	public boolean removeIntake(Intake intake) {
		try {
			disableIntake(intake);
			dal.deleteIntake(intake);
			return true;
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to remove intake: " + ex.getMessage(), ex);
		}
		return false;
	}

	private void enableIntake(Intake intake) {
		World world = plugin.getServer().getWorld(intake.getWorldName());
		Location loc = getLocation(world, intake);
		intakes.put(loc, new Model<>(world, intake));
		if (dynmapEnabled) {
			if (dynmapIntakes != null) {
				MarkerIcon icon = dynmapIntakes.getDefaultMarkerIcon();
				dynmapIntakes.createMarker(intake.getName(), "Intake: \"" + intake.getName() + "\"", false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
			}
			if (dynmapPipes != null) {
				enableDynmapPipes();
			}
		}
	}

	private void disableIntake(Intake intake) {
		World world = plugin.getServer().getWorld(intake.getWorldName());
		Location loc = getLocation(world, intake);
		intakes.remove(loc);
		if (dynmapEnabled) {
			if (dynmapIntakes != null) {
				Marker marker = dynmapIntakes.findMarker(intake.getName());
				if (marker != null) {
					marker.deleteMarker();
				}
			}
			if (dynmapPipes != null) {
				enableDynmapPipes();
			}
		}
	}

	public List<Valve> getValves() {
		try {
			return dal.getValves();
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to get valves: " + ex.getMessage(), ex);
		}
		return Collections.emptyList();
	}

	public Valve getValve(String name) {
		try {
			return dal.getValve(name);
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to get valve: " + ex.getMessage(), ex);
		}
		return null;
	}

	public boolean createValve(CommandSender sender, Valve valve) {
		try {
			World world = plugin.getServer().getWorld(valve.getWorldName());
			Location loc = getLocation(world, valve);
			if (fountains.containsKey(loc)) {
				sender.sendMessage(Utils.colorSpan(ChatColor.RED, "This is already a valve."));
				return false;
			}
			dal.saveValve(valve);
			enableValve(valve);
			return true;
		} catch (DuplicateKeyException ex) {
			sender.sendMessage(ChatColor.RED + "Valve already exists with that name.");
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to create valve: " + ex.getMessage(), ex);
		}
		return false;
	}

	public boolean removeValve(Valve valve) {
		try {
			disableValve(valve);
			dal.deleteValve(valve);
			return true;
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to remove valve: " + ex.getMessage(), ex);
		}
		return false;
	}

	private void enableValve(Valve valve) {
		World world = plugin.getServer().getWorld(valve.getWorldName());
		Location loc = getLocation(world, valve);
		valves.put(loc, new Model<>(world, valve));
		if (dynmapEnabled && dynmapValves != null) {
			MarkerIcon icon = dynmapValves.getDefaultMarkerIcon();
			dynmapValves.createMarker(valve.getName(), "Valve: \"" + valve.getName() + "\"", false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
		}
	}

	private void disableValve(Valve valve) {
		World world = plugin.getServer().getWorld(valve.getWorldName());
		Location loc = getLocation(world, valve);
		valves.remove(loc);
		if (dynmapEnabled && dynmapValves != null) {
			Marker marker = dynmapValves.findMarker(valve.getName());
			if (marker != null) {
				marker.deleteMarker();
			}
		}
	}

	public List<Sprinkler> getSprinklers() {
		try {
			return dal.getSprinklers();
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to get sprinklers: " + ex.getMessage(), ex);
		}
		return Collections.emptyList();
	}

	public Sprinkler getSprinkler(String name) {
		try {
			return dal.getSprinkler(name);
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to get sprinkler: " + ex.getMessage(), ex);
		}
		return null;
	}

	public boolean createSprinkler(CommandSender sender, Sprinkler sprinkler) {
		try {
			World world = plugin.getServer().getWorld(sprinkler.getWorldName());
			Location loc = getLocation(world, sprinkler);
			if (sprinklers.containsKey(loc)) {
				sender.sendMessage(Utils.colorSpan(ChatColor.RED, "This is already a sprinkler."));
				return false;
			}
			dal.saveSprinkler(sprinkler);
			enableSprinkler(sprinkler);
			return true;
		} catch (DuplicateKeyException ex) {
			sender.sendMessage(ChatColor.RED + "Sprinkler already exists with that name.");
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to create sprinkler: " + ex.getMessage(), ex);
		}
		return false;
	}

	public boolean removeSprinkler(Sprinkler sprinkler) {
		try {
			disableSprinkler(sprinkler);
			dal.deleteSprinkler(sprinkler);
			return true;
		} catch (DalException ex) {
			logger.log(Level.SEVERE, "Failed to remove sprinkler: " + ex.getMessage(), ex);
		}
		return false;
	}

	private void enableSprinkler(Sprinkler sprinkler) {
		World world = plugin.getServer().getWorld(sprinkler.getWorldName());
		Location loc = getLocation(world, sprinkler);
		sprinklers.put(loc, new Model<>(world, sprinkler));
		if (dynmapEnabled && dynmapSprinklers != null) {
			MarkerIcon icon = dynmapSprinklers.getDefaultMarkerIcon();
			dynmapSprinklers.createMarker(sprinkler.getName(), "Sprinkler: \"" + sprinkler.getName() + "\"", false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
		}
	}

	private void disableSprinkler(Sprinkler sprinkler) {
		World world = plugin.getServer().getWorld(sprinkler.getWorldName());
		Location loc = getLocation(world, sprinkler);
		sprinklers.remove(loc);
		if (dynmapEnabled && dynmapSprinklers != null) {
			Marker marker = dynmapSprinklers.findMarker(sprinkler.getName());
			if (marker != null) {
				marker.deleteMarker();
			}
		}
	}

	public ILocatable findByLocation(Location location) {
		if (fountains.containsKey(location)) {
			return fountains.get(location).get();
		}
		else if (intakes.containsKey(location)) {
			return intakes.get(location).get();
		}
		else if (valves.containsKey(location)) {
			return valves.get(location).get();
		}
		else if (sprinklers.containsKey(location)) {
			return sprinklers.get(location).get();
		}
		return null;
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void onDispense(BlockDispenseEvent event) {
		Block block = event.getBlock();
		Location loc = block.getLocation();
		if (fountains.containsKey(loc) && block.getState() instanceof Dispenser && event.getItem().getType() == Material.WATER_BUCKET) {
			DirectionalContainer dirContainer = (DirectionalContainer) ((Dispenser) block).getData();
			if (dirContainer.getFacing() == BlockFace.UP) {
				event.setCancelled(true);
			}
		}
		if (sprinklers.containsKey(loc) && block.getState() instanceof Dispenser && event.getItem().getType() == Material.WATER_BUCKET) {
			DirectionalContainer dirContainer = (DirectionalContainer) ((Dispenser) block).getData();
			if (dirContainer.getFacing() == BlockFace.DOWN) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Location loc = block.getLocation();
		if (fountains.containsKey(loc)) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "This dispenser is a fountain. The fountain must be removed before the dispenser can be removed.");
		}
		if (intakes.containsKey(loc)) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "This hopper is an intake. The intake must be removed before the hopper can be removed.");
		}
		if (valves.containsKey(loc)) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "This lamp is a valve. The valve must be removed before the lamp can be removed.");
		}
		if (sprinklers.containsKey(loc)) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "This dispenser is a sprinkler. The sprinkler must be removed before the dispenser can be removed.");
		}
		if (block.getType() == Material.COBBLE_WALL && dynmapEnabled && dynmapPipes != null) {
			enableDynmapPipes();
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.COBBLE_WALL && dynmapEnabled && dynmapPipes != null) {
			enableDynmapPipes();
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void onPluginEnabled(PluginEnableEvent event) {
		Plugin other = event.getPlugin();
		if (other.getName().equals(DYNMAP_PLUGIN))
		{
			enableDynmap((DynmapAPI)other);
		}
	}

	private void enableDynmap(DynmapAPI dynmapAPI) {
		if (!dynmapEnabled && dynmapAPI.markerAPIInitialized()) {
			MarkerAPI markerAPI = dynmapAPI.getMarkerAPI();
			if (config.getDynmapConfig().isShowFountains()) {
				MarkerIcon icon = markerAPI.getMarkerIcon(DYNMAP_MARKER_ICON_FOUNTAIN);
				if (icon == null) {
					icon = markerAPI.createMarkerIcon(DYNMAP_MARKER_ICON_FOUNTAIN, "Fountains", plugin.getResource(R.resource.ICON_FOUNTAIN));
				}
				dynmapFountains = markerAPI.createMarkerSet(DYNMAP_MARKER_SET_FOUNTAIN, "Fountains", Collections.singleton(icon), false);
				dynmapFountains.setDefaultMarkerIcon(icon);
				for (Map.Entry<Location, Model<Fountain>> entry : fountains.entrySet()) {
					Location loc = entry.getKey();
					Fountain fountain = entry.getValue().get();
					dynmapFountains.createMarker(fountain.getName(), "Fountain: \"" + fountain.getName() + "\"", false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
				}
			}
			if (config.getDynmapConfig().isShowIntakes()) {
				MarkerIcon icon = markerAPI.getMarkerIcon(DYNMAP_MARKER_ICON_INTAKE);
				if (icon == null) {
					icon = markerAPI.createMarkerIcon(DYNMAP_MARKER_ICON_INTAKE, "Intakes", plugin.getResource(R.resource.ICON_INTAKE));
				}
				dynmapIntakes = markerAPI.createMarkerSet(DYNMAP_MARKER_SET_INTAKE, "Intakes", Collections.singleton(icon), false);
				dynmapIntakes.setDefaultMarkerIcon(icon);
				for (Map.Entry<Location, Model<Intake>> entry : intakes.entrySet()) {
					Location loc = entry.getKey();
					Intake intake = entry.getValue().get();
					dynmapIntakes.createMarker(intake.getName(), "Intake: \"" + intake.getName() + "\"", false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
				}
			}
			if (config.getDynmapConfig().isShowValves()) {
				MarkerIcon icon = markerAPI.getMarkerIcon(DYNMAP_MARKER_ICON_VALVE);
				if (icon == null) {
					icon = markerAPI.createMarkerIcon(DYNMAP_MARKER_ICON_VALVE, "Valves", plugin.getResource(R.resource.ICON_VALVE));
				}
				dynmapValves = markerAPI.createMarkerSet(DYNMAP_MARKER_SET_VALVE, "Valves", Collections.singleton(icon), false);
				dynmapValves.setDefaultMarkerIcon(icon);
				for (Map.Entry<Location, Model<Valve>> entry : valves.entrySet()) {
					Location loc = entry.getKey();
					Valve valve = entry.getValue().get();
					dynmapValves.createMarker(valve.getName(), "Valve: \"" + valve.getName() + "\"", false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
				}
			}
			if (config.getDynmapConfig().isShowSprinklers()) {
				MarkerIcon icon = markerAPI.getMarkerIcon(DYNMAP_MARKER_ICON_SPRINKLER);
				if (icon == null) {
					icon = markerAPI.createMarkerIcon(DYNMAP_MARKER_ICON_SPRINKLER, "Sprinklers", plugin.getResource(R.resource.ICON_FOUNTAIN));
				}
				dynmapSprinklers = markerAPI.createMarkerSet(DYNMAP_MARKER_SET_SPRINKLER, "Sprinklers", Collections.singleton(icon), false);
				dynmapSprinklers.setDefaultMarkerIcon(icon);
				for (Map.Entry<Location, Model<Sprinkler>> entry : sprinklers.entrySet()) {
					Location loc = entry.getKey();
					Sprinkler sprinkler = entry.getValue().get();
					dynmapSprinklers.createMarker(sprinkler.getName(), "Sprinkler: \"" + sprinkler.getName() + "\"", false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
				}
			}
			if (config.getDynmapConfig().isShowPipes()) {
				dynmapPipes = dynmapAPI.getMarkerAPI().createMarkerSet(DYNMAP_MARKER_SET_PIPES, "Pipes", null, false);
				enableDynmapPipes();

			}
			dynmapEnabled = true;
		}
	}

	private void enableDynmapPipes() {
		for (PolyLineMarker m : dynmapPipes.getPolyLineMarkers()) {
			m.deleteMarker();
		}
		HashSet<Location> pipeNodes = new HashSet<>();
		BiFunction<Block, Boolean, Boolean> callback = (b, fromPipe) -> {
			if (isPipe(b, false)) {
				pipeNodes.add(b.getLocation());
			}
			if (fountains.containsKey(b.getLocation()) && fromPipe) {
				pipeNodes.add(b.getLocation());
			}
			return false;
		};
		for (Map.Entry<Location, Model<Intake>> entry : intakes.entrySet()) {
			Location loc = entry.getKey();
			pipeNodes.add(loc);
			walkPipes(new HashSet<>(), entry.getValue().getWorld().getBlockAt(loc), false, callback);
		}
		// Create line pairs
		HashSet<Line> lines = new HashSet<>();
		for (Location location : pipeNodes) {
			Block block = location.getWorld().getBlockAt(location);
			if (pipeNodes.contains(block.getRelative(BlockFace.NORTH).getLocation())) lines.add(new Line(location, block.getRelative(BlockFace.NORTH).getLocation()));
			if (pipeNodes.contains(block.getRelative(BlockFace.SOUTH).getLocation())) lines.add(new Line(location, block.getRelative(BlockFace.SOUTH).getLocation()));
			if (pipeNodes.contains(block.getRelative(BlockFace.EAST).getLocation())) lines.add(new Line(location, block.getRelative(BlockFace.EAST).getLocation()));
			if (pipeNodes.contains(block.getRelative(BlockFace.WEST).getLocation())) lines.add(new Line(location, block.getRelative(BlockFace.WEST).getLocation()));
			if (pipeNodes.contains(block.getRelative(BlockFace.UP).getLocation())) lines.add(new Line(location, block.getRelative(BlockFace.UP).getLocation()));
			if (pipeNodes.contains(block.getRelative(BlockFace.DOWN).getLocation())) lines.add(new Line(location, block.getRelative(BlockFace.DOWN).getLocation()));
		}
		// Create dynmap poly-line markers from unique lines
		for (Line line : lines) {
			PolyLineMarker marker = dynmapPipes.createPolyLineMarker(null, "", false, line.world.getName(), new double[] { line.location1.getBlockX() + 0.5, line.location2.getBlockX() + 0.5 }, new double[] { line.location1.getBlockY() + 0.5, line.location2.getBlockY() + 0.5 }, new double[] { line.location1.getBlockZ() + 0.5, line.location2.getBlockZ() + 0.5 }, false);
			marker.setLineStyle(2, 1, Color.fromRGB(0x42, 0x86, 0xf4).asRGB());
		}

	}

	private Location getLocation(World world, ILocatable obj) {
		return new Location(world, obj.getX(), obj.getY(), obj.getZ());
	}

	private void buildWaterColumn(World world, Location location, int height) {
		Block rel = world.getBlockAt(location);
		for (int offsetY = 0; offsetY < Fountain.MAX_POWER && offsetY + location.getBlockY() < world.getMaxHeight(); offsetY++) {
			rel = rel.getRelative(BlockFace.UP);
			if (rel.getType() == Material.WATER || rel.getType() == Material.STATIONARY_WATER || rel.getType() == Material.AIR) {
				boolean isPool = rel.getRelative(BlockFace.EAST).getType() == Material.STATIONARY_WATER
						&& rel.getRelative(BlockFace.NORTH).getType() == Material.STATIONARY_WATER
						&& rel.getRelative(BlockFace.WEST).getType() == Material.STATIONARY_WATER
						&& rel.getRelative(BlockFace.SOUTH).getType() == Material.STATIONARY_WATER
						&& rel.getRelative(BlockFace.NORTH_EAST).getType() == Material.STATIONARY_WATER
						&& rel.getRelative(BlockFace.NORTH_WEST).getType() == Material.STATIONARY_WATER
						&& rel.getRelative(BlockFace.SOUTH_EAST).getType() == Material.STATIONARY_WATER
						&& rel.getRelative(BlockFace.SOUTH_WEST).getType() == Material.STATIONARY_WATER;
				rel.setType(isPool ? Material.STATIONARY_WATER : offsetY < height ? Material.WATER : Material.AIR);
			} else {
				break;
			}
		}
	}

	private boolean isPipe(Block block, boolean requireFlow) {
		return block.getType() == Material.COBBLE_WALL || ((block.getType() == Material.REDSTONE_LAMP_ON || (!requireFlow && block.getType() == Material.REDSTONE_LAMP_OFF)) && valves.containsKey(block.getLocation()) && (!requireFlow || block.isBlockIndirectlyPowered() || block.isBlockPowered()));
	}

	private void visitPipeDirection(HashSet<Location> visited, Block block, BlockFace direction, boolean requireFlow, BiFunction<Block, Boolean, Boolean> callback) {
		Block related = block.getRelative(direction);
		boolean fromPipe = block.getType() == Material.COBBLE_WALL;
		if (related != null && !visited.contains(related.getLocation())) {
			if (!callback.apply(related, fromPipe) && isPipe(related, requireFlow)) {
				visited.add(block.getLocation());
				walkPipes(visited, related, requireFlow, callback);
			}
		}
	}

	private void walkPipes(HashSet<Location> visited, Block block, boolean requireFlow, BiFunction<Block, Boolean, Boolean> callback) {
		visitPipeDirection(visited, block, BlockFace.NORTH, requireFlow, callback);
		visitPipeDirection(visited, block, BlockFace.SOUTH, requireFlow, callback);
		visitPipeDirection(visited, block, BlockFace.EAST, requireFlow, callback);
		visitPipeDirection(visited, block, BlockFace.WEST, requireFlow, callback);
		visitPipeDirection(visited, block, BlockFace.UP, requireFlow, callback);
		visitPipeDirection(visited, block, BlockFace.DOWN, requireFlow, callback);
	}

	private List<Block> propagatePipes(Block block) {
		ArrayList<Block> destinations = new ArrayList<>();
		BiFunction<Block, Boolean, Boolean> callback = (b, fromPipe) -> {
			if (valves.containsKey(b.getLocation())) {
				valves.get(b.getLocation()).setOn(true);
			}
			if (fountains.containsKey(b.getLocation()) && fromPipe) {
				destinations.add(b);
			}
			if (sprinklers.containsKey(b.getLocation()) && fromPipe) {
				destinations.add(b);
			}
			return false;
		};
		walkPipes(new HashSet<>(), block, true, callback);
		return destinations;
	}

	private boolean isLocationWithinWaterColumn(Location testLoc, Location fountainLoc, int height) {
		return testLoc.getBlockX() == fountainLoc.getBlockX()
				&& testLoc.getBlockZ() == fountainLoc.getBlockZ()
				&& testLoc.getBlockY() > fountainLoc.getBlockY()
				&& testLoc.getBlockY() < fountainLoc.getBlockY() + height;
	}

	@Override
	public void run() {
		for (Map.Entry<Location, Model<Fountain>> entry : fountains.entrySet()) {
			Location loc = entry.getKey();
			Model<Fountain> model = entry.getValue();
			Block block = model.getWorld().getBlockAt(loc);
			if (block.getType() == Material.DISPENSER && block.getState() instanceof Dispenser) {
				Inventory inventory = ((Dispenser) block.getState()).getInventory();
				Fountain fountain = model.get();
				int power = fountain.getPower();
				boolean redstonePowered = block.isBlockIndirectlyPowered() || block.isBlockPowered();
				boolean active = true;
				switch (fountain.getRedstoneRequirementState()) {
					case ACTIVE:
						active = redstonePowered;
						break;
					case INACTIVE:
						active = !redstonePowered;
						break;
				}
				if (active) {
					if (inventory.contains(Material.WATER_BUCKET)) {
						inventory.remove(Material.WATER_BUCKET);
						model.reset();
						model.setOn(true);
					} else if (model.getCounter() < power + Fountain.MAX_POWER) {
						model.increment();
					} else {
						model.setOn(false);
					}
					if (model.isOn()) {
						int height = power + Fountain.MAX_POWER - model.getCounter();
						if (height < 0) height = 0;
						if (height > power) height = power;
						buildWaterColumn(model.getWorld(), loc, height);
						for (Player player : plugin.getServer().getOnlinePlayers()) {
							Location playerLoc = player.getLocation().clone();
							if (isLocationWithinWaterColumn(playerLoc, loc, height)) {
								player.setVelocity(new Vector(0, power * 0.05, 0));
							}
						}
					}
				}
			}
		}

		for (Map.Entry<Location, Model<Sprinkler>> entry : sprinklers.entrySet()) {
			Location location = entry.getKey();
			Model<Sprinkler> model = entry.getValue();
			Block block = model.getWorld().getBlockAt(location);
			if (block.getType() == Material.DISPENSER && block.getState() instanceof Dispenser) {
				Inventory inventory = ((Dispenser) block.getState()).getInventory();
				Sprinkler sprinkler = model.get();
				int spread = sprinkler.getSpread();
				boolean redstonePowered = block.isBlockIndirectlyPowered() || block.isBlockPowered();
				boolean active = true;
				switch (sprinkler.getRedstoneRequirementState()) {
					case ACTIVE:
						active = redstonePowered;
						break;
					case INACTIVE:
						active = !redstonePowered;
						break;
				}
				if (active) {
					if (inventory.contains(Material.WATER_BUCKET)) {
						inventory.remove(Material.WATER_BUCKET);
						model.reset();
						model.setOn(true);
					} else if (model.getCounter() < spread + Fountain.MAX_POWER) {
						model.increment();
					} else {
						model.setOn(false);
					}
					if (model.isOn()) {
						for (int i = 0; i < spread * 20; i++) {
							double randAngle = rng.nextDouble() * 2 * Math.PI;
							double randDistance = rng.nextDouble() * spread;
							double xOffset = Math.cos(randAngle) * randDistance;
							double zOffset = Math.sin(randAngle) * randDistance;
							double yOffset = randDistance * 3 / Sprinkler.MAX_SPREAD;
							entry.getValue().getWorld().spawnParticle(Particle.WATER_DROP, location.getX() + 0.5 + xOffset, location.getY() - 0.1 - yOffset, location.getZ() + 0.5 + zOffset, 1);
						}
					}
				}
			}
		}

		// Start each valve as off (no water present), intakes will propagate water to them if water is present
		for (Map.Entry<Location, Model<Valve>> entry : valves.entrySet()) {
			entry.getValue().setOn(false);
		}

		for (Map.Entry<Location, Model<Intake>> entry : intakes.entrySet()) {
			Location loc = entry.getKey();
			Model<Intake> model = entry.getValue();
			Intake intake = model.get();
			Block block = model.getWorld().getBlockAt(loc);
			if (block.getType() == Material.HOPPER && block.getState() instanceof Hopper) {
				Block above = block.getRelative(BlockFace.UP);
				boolean redstonePowered = block.isBlockIndirectlyPowered() || block.isBlockPowered();
				boolean active = true;
				switch (intake.getRedstoneRequirementState()) {
					case ACTIVE:
						active = redstonePowered;
						break;
					case INACTIVE:
						active = !redstonePowered;
						break;
				}
				if (above.getType() == Material.WATER || above.getType() == Material.STATIONARY_WATER && active) {
					List<Block> pipeDestinations = propagatePipes(block);
					for (int i = 0; i < intake.getSpeed(); i++) {
						if (pipeDestinations.isEmpty()) {
							Inventory inventory = ((Hopper) block.getState()).getInventory();
							inventory.addItem(new ItemStack(Material.WATER_BUCKET));
						} else {
							Block dest = pipeDestinations.get(rng.nextInt(pipeDestinations.size()));
							Inventory inventory = ((Dispenser) dest.getState()).getInventory();
							inventory.addItem(new ItemStack(Material.WATER_BUCKET));
						}
					}
				}
			}
		}

		// Randomly play the water drip effect for valves
		for (Map.Entry<Location, Model<Valve>> entry : valves.entrySet()) {
			if (entry.getValue().isOn() && rng.nextDouble() > 0.95) {
				Location loc = entry.getKey();
				entry.getValue().getWorld().spawnParticle(Particle.DRIP_WATER, loc.getBlockX() + rng.nextDouble() * 0.9 + 0.05, loc.getBlockY() - 0.05, loc.getBlockZ() + rng.nextDouble() * 0.9 + 0.05, 1);
			}
		}

	}

	class Model<T extends ILocatable> {
		private int counter;
		private T obj;
		private World world;
		private boolean on;

		Model(World world, T obj) {
			reset();
			this.world = world;
			this.obj = obj;
		}

		void reset() {
			counter = 0;
		}

		void increment() {
			++counter;
		}

		int getCounter() {
			return counter;
		}

		T get() {
			return obj;
		}

		World getWorld() {
			return world;
		}

		void setOn(boolean on) {
			this.on = on;
		}

		boolean isOn() {
			return on;
		}
	}

	class Line {
		World world;
		Location location1, location2;
		Line(Location location1, Location location2) {
			this.world = location1.getWorld();
			this.location1 = location1;
			this.location2 = location2;
		}

		@Override
		public int hashCode() {
			return location1.hashCode() + 7 * location2.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Line) {
				Line other = (Line)obj;
				return (location1.equals(other.location1) && location2.equals(other.location2)) || (location1.equals(other.location2) && location2.equals(other.location1));
			}
			return false;
		}
	}
}
