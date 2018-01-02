package com.schneenet.fountainsplugin;

import com.schneenet.fountainsplugin.config.FountainsConfig;
import com.schneenet.fountainsplugin.models.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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

	private static final int MAX_DRAIN_DISTANCE = 100;

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
				sender.sendMessage(config.getLocalization().getFormattedString(R.string.errors_fountain_exists));
				return false;
			}
			dal.saveFountain(fountain);
			enableFountain(fountain);
			return true;
		} catch (DuplicateKeyException ex) {
			sender.sendMessage(config.getLocalization().getFormattedString(R.string.errors_fountain_duplicate));
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
			dynmapFountains.createMarker(fountain.getName(), config.getLocalization().getFormattedString(R.string.fountain, fountain), false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
		}
	}

	private void disableFountain(Fountain fountain) {
		World world = plugin.getServer().getWorld(fountain.getWorldName());
		Location loc = getLocation(world, fountain);
		buildWaterColumn(world, loc, 0);
		BlockState state = loc.getBlock().getState();
		if (state instanceof Container) {
			((Container)state).getInventory().clear();
		}
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
				sender.sendMessage(config.getLocalization().getFormattedString(R.string.errors_intake_exists));
				return false;
			}
			dal.saveIntake(intake);
			enableIntake(intake);
			return true;
		} catch (DuplicateKeyException ex) {
			sender.sendMessage(config.getLocalization().getFormattedString(R.string.errors_intake_duplicate));
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
				dynmapIntakes.createMarker(intake.getName(), config.getLocalization().getFormattedString(R.string.intake, intake), false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
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
		BlockState state = loc.getBlock().getState();
		if (state instanceof Container) {
			((Container)state).getInventory().clear();
		}
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
				sender.sendMessage(config.getLocalization().getFormattedString(R.string.errors_valve_exists));
				return false;
			}
			dal.saveValve(valve);
			enableValve(valve);
			return true;
		} catch (DuplicateKeyException ex) {
			sender.sendMessage(config.getLocalization().getFormattedString(R.string.errors_valve_duplicate));
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
			dynmapValves.createMarker(valve.getName(), config.getLocalization().getFormattedString(R.string.valve, valve), false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
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
				sender.sendMessage(config.getLocalization().getFormattedString(R.string.errors_sprinkler_exists));
				return false;
			}
			dal.saveSprinkler(sprinkler);
			enableSprinkler(sprinkler);
			return true;
		} catch (DuplicateKeyException ex) {
			sender.sendMessage(config.getLocalization().getFormattedString(R.string.errors_sprinkler_duplicate));
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
			dynmapSprinklers.createMarker(sprinkler.getName(), config.getLocalization().getFormattedString(R.string.sprinkler, sprinkler), false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
		}
	}

	private void disableSprinkler(Sprinkler sprinkler) {
		World world = plugin.getServer().getWorld(sprinkler.getWorldName());
		Location loc = getLocation(world, sprinkler);
		BlockState state = loc.getBlock().getState();
		if (state instanceof Container) {
			((Container)state).getInventory().clear();
		}
		sprinklers.remove(loc);
		if (dynmapEnabled && dynmapSprinklers != null) {
			Marker marker = dynmapSprinklers.findMarker(sprinkler.getName());
			if (marker != null) {
				marker.deleteMarker();
			}
		}
	}

	private ILocatable findByLocation(Location location) {
		if (fountains.containsKey(location)) {
			return fountains.get(location).obj;
		}
		else if (intakes.containsKey(location)) {
			return intakes.get(location).obj;
		}
		else if (valves.containsKey(location)) {
			return valves.get(location).obj;
		}
		else if (sprinklers.containsKey(location)) {
			return sprinklers.get(location).obj;
		}
		return null;
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void onDispense(BlockDispenseEvent event) {
		Block block = event.getBlock();
		Location loc = block.getLocation();
		if (block.getState() instanceof Dispenser && event.getItem().getType() == Material.WATER_BUCKET && (fountains.containsKey(loc) || sprinklers.containsKey(loc))) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Location loc = block.getLocation();
		if (fountains.containsKey(loc)) {
			event.setCancelled(true);
			String name = fountains.get(loc).obj.getName();
			event.getPlayer().sendMessage(config.getLocalization().getFormattedString(R.string.errors_cannot_break_fountain, name));
		}
		if (intakes.containsKey(loc)) {
			event.setCancelled(true);
			String name = intakes.get(loc).obj.getName();
			event.getPlayer().sendMessage(config.getLocalization().getFormattedString(R.string.errors_cannot_break_intake, name));
		}
		if (valves.containsKey(loc)) {
			event.setCancelled(true);
			String name = valves.get(loc).obj.getName();
			event.getPlayer().sendMessage(config.getLocalization().getFormattedString(R.string.errors_cannot_break_valve, name));
		}
		if (sprinklers.containsKey(loc)) {
			event.setCancelled(true);
			String name = sprinklers.get(loc).obj.getName();
			event.getPlayer().sendMessage(config.getLocalization().getFormattedString(R.string.errors_cannot_break_sprinkler, name));
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

	@EventHandler
	@SuppressWarnings("unused")
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block target = event.getClickedBlock();
			if (target != null) {
				ILocatable item = findByLocation(target.getLocation());
				if (item != null) {
					event.getPlayer().sendMessage(config.getLocalization().getFormattedListingString(item, true));
				}
			}
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void onInventoryOpen(InventoryOpenEvent event) {
		Location location = event.getInventory().getLocation();
		if (fountains.containsKey(location) || intakes.containsKey(location) || sprinklers.containsKey(location)) {
			event.setCancelled(true);
		}
	}

	private void enableDynmap(DynmapAPI dynmapAPI) {
		if (!dynmapEnabled && dynmapAPI.markerAPIInitialized()) {
			MarkerAPI markerAPI = dynmapAPI.getMarkerAPI();
			if (config.getDynmapConfig().isShowFountains()) {
				MarkerIcon icon = markerAPI.getMarkerIcon(DYNMAP_MARKER_ICON_FOUNTAIN);
				if (icon == null) {
					icon = markerAPI.createMarkerIcon(DYNMAP_MARKER_ICON_FOUNTAIN, config.getLocalization().getLocalizedString(R.string.fountains), plugin.getResource(R.resource.icons_fountain));
				}
				dynmapFountains = markerAPI.createMarkerSet(DYNMAP_MARKER_SET_FOUNTAIN, config.getLocalization().getLocalizedString(R.string.fountains), Collections.singleton(icon), false);
				dynmapFountains.setDefaultMarkerIcon(icon);
				for (Map.Entry<Location, Model<Fountain>> entry : fountains.entrySet()) {
					Location loc = entry.getKey();
					Fountain fountain = entry.getValue().obj;
					dynmapFountains.createMarker(fountain.getName(), config.getLocalization().getFormattedString(R.string.fountain, fountain), false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
				}
			}
			if (config.getDynmapConfig().isShowIntakes()) {
				MarkerIcon icon = markerAPI.getMarkerIcon(DYNMAP_MARKER_ICON_INTAKE);
				if (icon == null) {
					icon = markerAPI.createMarkerIcon(DYNMAP_MARKER_ICON_INTAKE, config.getLocalization().getLocalizedString(R.string.intakes), plugin.getResource(R.resource.icons_intake));
				}
				dynmapIntakes = markerAPI.createMarkerSet(DYNMAP_MARKER_SET_INTAKE, config.getLocalization().getLocalizedString(R.string.intakes), Collections.singleton(icon), false);
				dynmapIntakes.setDefaultMarkerIcon(icon);
				for (Map.Entry<Location, Model<Intake>> entry : intakes.entrySet()) {
					Location loc = entry.getKey();
					Intake intake = entry.getValue().obj;
					dynmapIntakes.createMarker(intake.getName(), config.getLocalization().getFormattedString(R.string.intake, intake), false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
				}
			}
			if (config.getDynmapConfig().isShowValves()) {
				MarkerIcon icon = markerAPI.getMarkerIcon(DYNMAP_MARKER_ICON_VALVE);
				if (icon == null) {
					icon = markerAPI.createMarkerIcon(DYNMAP_MARKER_ICON_VALVE, config.getLocalization().getLocalizedString(R.string.valves), plugin.getResource(R.resource.icons_valve));
				}
				dynmapValves = markerAPI.createMarkerSet(DYNMAP_MARKER_SET_VALVE, config.getLocalization().getLocalizedString(R.string.valves), Collections.singleton(icon), false);
				dynmapValves.setDefaultMarkerIcon(icon);
				for (Map.Entry<Location, Model<Valve>> entry : valves.entrySet()) {
					Location loc = entry.getKey();
					Valve valve = entry.getValue().obj;
					dynmapValves.createMarker(valve.getName(), config.getLocalization().getFormattedString(R.string.valve, valve), false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
				}
			}
			if (config.getDynmapConfig().isShowSprinklers()) {
				MarkerIcon icon = markerAPI.getMarkerIcon(DYNMAP_MARKER_ICON_SPRINKLER);
				if (icon == null) {
					icon = markerAPI.createMarkerIcon(DYNMAP_MARKER_ICON_SPRINKLER, config.getLocalization().getLocalizedString(R.string.sprinklers), plugin.getResource(R.resource.icons_fountain));
				}
				dynmapSprinklers = markerAPI.createMarkerSet(DYNMAP_MARKER_SET_SPRINKLER, config.getLocalization().getLocalizedString(R.string.sprinklers), Collections.singleton(icon), false);
				dynmapSprinklers.setDefaultMarkerIcon(icon);
				for (Map.Entry<Location, Model<Sprinkler>> entry : sprinklers.entrySet()) {
					Location loc = entry.getKey();
					Sprinkler sprinkler = entry.getValue().obj;
					dynmapSprinklers.createMarker(sprinkler.getName(), config.getLocalization().getFormattedString(R.string.sprinkler, sprinkler), false, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
				}
			}
			if (config.getDynmapConfig().isShowPipes()) {
				dynmapPipes = dynmapAPI.getMarkerAPI().createMarkerSet(DYNMAP_MARKER_SET_PIPES, config.getLocalization().getLocalizedString(R.string.pipes), null, false);
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
			walkPipes(new HashSet<>(), entry.getValue().world.getBlockAt(loc), false, callback);
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

	private int buildWaterColumn(World world, Location location, int height) {
		Block rel = world.getBlockAt(location);
		for (int offsetY = 0; offsetY < Fountain.MAX_POWER && offsetY + location.getBlockY() < world.getMaxHeight(); offsetY++) {
			rel = rel.getRelative(BlockFace.UP);
			if (rel != null && (rel.getType() == Material.WATER || rel.getType() == Material.STATIONARY_WATER || rel.getType() == Material.AIR)) {
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
				return offsetY;
			}
		}
		return height;
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
				valves.get(b.getLocation()).on = true;
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

	public Set<Location> findDrainArea(Location start) throws DrainAreaTooLargeException {
		HashSet<Location> drainSpreadSet = new HashSet<>();
		Block startBlock = start.getBlock();
		if (startBlock.getType() == Material.STATIONARY_WATER || startBlock.getType() == Material.WATER) {
			HashSet<Block> visited = new HashSet<>();
			Stack<Block> stack = new Stack<>();
			stack.push(startBlock);
			while (!stack.empty()) {
				Block block = stack.pop();
				if (!visited.add(block)) continue;
				if (manhattanDistance(block.getLocation(), start) > MAX_DRAIN_DISTANCE) {
					throw new DrainAreaTooLargeException();
				}
				drainSpreadSet.add(block.getLocation());
				Block north = block.getRelative(BlockFace.NORTH);
				if (north != null && (north.getType() == Material.WATER || north.getType() == Material.STATIONARY_WATER)) stack.push(north);
				Block south = block.getRelative(BlockFace.SOUTH);
				if (south != null && (south.getType() == Material.WATER || south.getType() == Material.STATIONARY_WATER)) stack.push(south);
				Block east = block.getRelative(BlockFace.EAST);
				if (east != null && (east.getType() == Material.WATER || east.getType() == Material.STATIONARY_WATER)) stack.push(east);
				Block west = block.getRelative(BlockFace.WEST);
				if (west != null && (west.getType() == Material.WATER || west.getType() == Material.STATIONARY_WATER)) stack.push(west);
			}
		}
		return drainSpreadSet;
	}

	public boolean checkFillArea(Location start) {
		Block startBlock = start.getBlock();
		if (startBlock.getType() == Material.AIR) {
			HashSet<Block> visited = new HashSet<>();
			Stack<Block> stack = new Stack<>();
			stack.push(startBlock);
			while (!stack.empty()) {
				Block block = stack.pop();
				if (!visited.add(block)) continue;
				if (manhattanDistance(block.getLocation(), start) > MAX_DRAIN_DISTANCE) {
					return false;
				}
				Block north = block.getRelative(BlockFace.NORTH);
				if (north != null && (north.getType() == Material.AIR || north.getType() == Material.WATER || north.getType() == Material.STATIONARY_WATER)) stack.push(north);
				Block south = block.getRelative(BlockFace.SOUTH);
				if (south != null && (south.getType() == Material.AIR || south.getType() == Material.WATER || south.getType() == Material.STATIONARY_WATER)) stack.push(south);
				Block east = block.getRelative(BlockFace.EAST);
				if (east != null && (east.getType() == Material.AIR || east.getType() == Material.WATER || east.getType() == Material.STATIONARY_WATER)) stack.push(east);
				Block west = block.getRelative(BlockFace.WEST);
				if (west != null && (west.getType() == Material.AIR || west.getType() == Material.WATER || west.getType() == Material.STATIONARY_WATER)) stack.push(west);
			}
		}
		return true;
	}

	private Set<Location> findNextFillSpread(Location start) throws DrainAreaTooLargeException {
		HashSet<Location> fillSpreadSet = new HashSet<>();
		Block startBlock = start.getBlock();
		if (startBlock.getType() == Material.STATIONARY_WATER || startBlock.getType() == Material.WATER) {
			HashSet<Block> visited = new HashSet<>();
			Stack<Block> stack = new Stack<>();
			stack.push(startBlock);
			while (!stack.empty()) {
				Block block = stack.pop();
				if (!visited.add(block)) continue;
				if (manhattanDistance(block.getLocation(), start) > MAX_DRAIN_DISTANCE) {
					throw new DrainAreaTooLargeException();
				}
				Block north = block.getRelative(BlockFace.NORTH);
				if (north != null) {
					if (north.getType() == Material.WATER) fillSpreadSet.add(block.getLocation());
					else if (north.getType() == Material.STATIONARY_WATER) stack.push(north);
				}

				Block south = block.getRelative(BlockFace.SOUTH);
				if (south != null) {
					if (south.getType() == Material.WATER) fillSpreadSet.add(block.getLocation());
					else if (south.getType() == Material.STATIONARY_WATER) stack.push(south);
				}

				Block east = block.getRelative(BlockFace.EAST);
				if (east != null) {
					if (east.getType() == Material.WATER) fillSpreadSet.add(block.getLocation());
					else if (east.getType() == Material.STATIONARY_WATER) stack.push(east);
				}

				Block west = block.getRelative(BlockFace.WEST);
				if (west != null) {
					if (west.getType() == Material.WATER) fillSpreadSet.add(block.getLocation());
					else if (west.getType() == Material.STATIONARY_WATER) stack.push(west);
				}
			}
		}
		return fillSpreadSet;
	}

	private int manhattanDistance(Location from, Location to) {
		return Math.abs(from.getBlockX() - to.getBlockX()) + Math.abs(from.getBlockY() - to.getBlockY()) + Math.abs(from.getBlockZ() - to.getBlockZ());
	}

	@Override
	public void run() {
		for (Map.Entry<Location, Model<Fountain>> entry : fountains.entrySet()) {
			Location loc = entry.getKey();
			Model<Fountain> model = entry.getValue();
			Block block = model.world.getBlockAt(loc);
			if (block.getType() == Material.DISPENSER && block.getState() instanceof Dispenser) {
				Inventory inventory = ((Dispenser) block.getState()).getInventory();
				boolean redstonePowered = block.isBlockIndirectlyPowered() || block.isBlockPowered();
				boolean active = true;
				switch (model.obj.getRedstoneRequirementState()) {
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
						model.counter1 = 0;
						model.on = true;
					} else if (model.counter1 < model.obj.getPower() + Fountain.MAX_POWER) {
						model.counter1++;
					} else {
						model.on = false;
					}
					if (model.on) {
						int power = model.obj.getPower();
						boolean isFilling = false;
						if (power >= Fountain.MIN_FILL_POWER) {
							power -= (Fountain.MIN_FILL_POWER - 1);
							isFilling = true;
						}
						int height = power + Fountain.MAX_POWER - model.counter1;
						if (height < 0) height = 0;
						if (height > power) height = power;
						height = buildWaterColumn(model.world, loc, height);
						for (Player player : plugin.getServer().getOnlinePlayers()) {
							Location playerLoc = player.getLocation().clone();
							if (isLocationWithinWaterColumn(playerLoc, loc, height)) {
								player.setVelocity(new Vector(0, height * 0.05, 0));
							}
						}
						if (isFilling) {
							if (model.counter2 > (Fountain.MAX_POWER - model.obj.getPower())) {
								model.counter2 = 0;
								int maxFillHeight = Math.max(height, model.obj.getPower() - (Fountain.MIN_FILL_POWER - 1));
								int fillHeight = 1;
								Set<Location> fillSpread;
								do {
									Block fillStart = block.getRelative(BlockFace.UP, fillHeight);
									if (fillStart != null && checkFillArea(fillStart.getLocation())) {
										try {
											fillSpread = findNextFillSpread(fillStart.getLocation());
										} catch (DrainAreaTooLargeException ex) {
											// Should never happen, but should be same as if checkFillArea had returned false
											fillSpread = null;
											break;
										}
										if (fillSpread != null && fillSpread.isEmpty()) fillHeight++;
									} else {
										fillSpread = null;
										break;
									}
								}
								while (fillHeight <= maxFillHeight && fillSpread != null && fillSpread.isEmpty());
								if (fillSpread != null && !fillSpread.isEmpty()) {
									for (Location fillLocation : fillSpread) {
										fillLocation.getBlock().setType(Material.STATIONARY_WATER);
									}
								}
							}
							model.counter2++;
						}
					}
				}
			}
		}

		for (Map.Entry<Location, Model<Sprinkler>> entry : sprinklers.entrySet()) {
			Location location = entry.getKey();
			Model<Sprinkler> model = entry.getValue();
			Block block = model.world.getBlockAt(location);
			if (block.getType() == Material.DISPENSER && block.getState() instanceof Dispenser) {
				Inventory inventory = ((Dispenser) block.getState()).getInventory();
				Sprinkler sprinkler = model.obj;
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
						model.counter1 = 0;
						model.on = true;
					} else if (model.counter1 < spread + Fountain.MAX_POWER) {
						model.counter1++;
					} else {
						model.on = false;
					}
					if (model.on) {
						for (int i = 0; i < spread * 20; i++) {
							double randAngle = rng.nextDouble() * 2 * Math.PI;
							double randDistance = rng.nextDouble() * spread;
							double xOffset = Math.cos(randAngle) * randDistance;
							double zOffset = Math.sin(randAngle) * randDistance;
							double yOffset = randDistance * 3 / Sprinkler.MAX_SPREAD;
							entry.getValue().world.spawnParticle(Particle.WATER_DROP, location.getX() + 0.5 + xOffset, location.getY() - 0.1 - yOffset, location.getZ() + 0.5 + zOffset, 1);
						}
					}
				}
			}
		}

		// Start each valve as off (no water present), intakes will propagate water to them if water is present
		for (Map.Entry<Location, Model<Valve>> entry : valves.entrySet()) {
			entry.getValue().on = false;
		}

		for (Map.Entry<Location, Model<Intake>> entry : intakes.entrySet()) {
			Location loc = entry.getKey();
			Model<Intake> model = entry.getValue();
			Intake intake = model.obj;
			Block block = model.world.getBlockAt(loc);
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
					boolean pulledWater = false;
					for (int i = 0; i < intake.getSpeed(); i++) {
						HashMap<Integer, ItemStack> inventoryResult;
						if (pipeDestinations.isEmpty()) {
							Inventory inventory = ((Hopper) block.getState()).getInventory();
							inventoryResult = inventory.addItem(new ItemStack(Material.WATER_BUCKET));
						} else {
							Block dest = pipeDestinations.get(rng.nextInt(pipeDestinations.size()));
							Inventory inventory = ((Dispenser) dest.getState()).getInventory();
							inventoryResult = inventory.addItem(new ItemStack(Material.WATER_BUCKET));
						}
						if (inventoryResult.isEmpty()) pulledWater = true;
					}
					if (intake.getSpeed() >= Intake.MIN_DRAIN_SPEED && pulledWater) {
						if (model.counter1 > (Intake.MAX_SPEED - intake.getSpeed())) {
							model.counter1 = 0;
							Block waterTop = block.getRelative(BlockFace.UP);
							Block waterAbove = waterTop;
							while (waterAbove != null && (waterAbove.getType() == Material.WATER || waterAbove.getType() == Material.STATIONARY_WATER)) {
								waterTop = waterAbove;
								waterAbove = waterAbove.getRelative(BlockFace.UP);
							}
							try {
								Set<Location> pool = findDrainArea(waterTop.getLocation());
								boolean drainLayer = true;
								for (Location poolLocation : pool) {
									Block poolBlock = poolLocation.getBlock();
									if (poolBlock.getType() == Material.WATER) drainLayer = false;
								}
								if (drainLayer) {
									for (Location poolLocation : pool) {
										Block poolBlock = poolLocation.getBlock();
										poolBlock.setType(Material.WATER);
										// TODO Bukkit has no API to set water level, so we have to use setData
										poolBlock.setData((byte)1);
									}
								}
							} catch (DrainAreaTooLargeException ex) {
								// Do nothing
							}
						}
						model.counter1++;
					}
				}
			}
		}

		// Randomly play the water drip effect for valves
		for (Map.Entry<Location, Model<Valve>> entry : valves.entrySet()) {
			if (entry.getValue().on && rng.nextDouble() > 0.95) {
				Location loc = entry.getKey();
				entry.getValue().world.spawnParticle(Particle.DRIP_WATER, loc.getBlockX() + rng.nextDouble() * 0.9 + 0.05, loc.getBlockY() - 0.05, loc.getBlockZ() + rng.nextDouble() * 0.9 + 0.05, 1);
			}
		}

	}

	class Model<T extends ILocatable> {
		int counter1;
		int counter2;
		T obj;
		World world;
		boolean on;

		Model(World world, T obj) {
			counter1 = 0;
			counter2 = 0;
			on = false;
			this.world = world;
			this.obj = obj;
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
