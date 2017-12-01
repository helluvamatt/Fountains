package com.schneenet.fountainsplugin;

import com.schneenet.fountainsplugin.actions.*;
import com.schneenet.fountainsplugin.config.FountainsConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FountainsPlugin extends JavaPlugin {
	private static final String DB_NAME = "Fountains.db";

	private static final String CMD_FOUNTAINS = "fountains";
	private static final String CMD_INTAKES = "intakes";
	private static final String CMD_VALVES = "valves";
	private static final String CMD_SPRINKLERS = "sprinklers";

	private static final String ACTION_CREATE = "create";
	private static final String ACTION_REMOVE = "remove";
	private static final String ACTION_LIST = "list";
	private static final String ACTION_QUERY = "query";

	private FountainsManager manager;

	@Override
	public void onEnable() {
		File dataFolder = getDataFolder();
		try {
			saveDefaultConfig();
			File dbFile = new File(dataFolder, DB_NAME);
			FountainsDal dal = new FountainsDal(dbFile);
			this.manager = new FountainsManager(this, new FountainsConfig(getConfig()), dal, getLogger());
			getServer().getPluginManager().registerEvents(manager, this);
			getServer().getScheduler().scheduleSyncRepeatingTask(this, manager, 0L, 1L);
		} catch (DalException ex) {
			getLogger().severe("Failed to initialize Fountains: " + ex.getMessage());
		}
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		HandlerList.unregisterAll(manager);
		if (manager != null) manager.shutdown();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (manager == null) return false;
		List<String> argList = new ArrayList<>(Arrays.asList(args));
		if (command.getName().equalsIgnoreCase(CMD_FOUNTAINS)) {
			if (!sender.hasPermission("fountains.fountains")) return false;
			String action = argList.isEmpty() ? null : argList.remove(0);
			if (action != null) {
				if (action.equalsIgnoreCase(ACTION_LIST)) {
					new ListFountainsAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_CREATE)) {
					new CreateFountainAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_REMOVE)) {
					new RemoveFountainAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_QUERY)) {
					new QueryAction(manager, argList.toArray(new String[] {})).run(sender);
					return true;
				} else {
					sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Invalid action.") + " <action> must be one of 'create', 'remove', 'list', 'query'.");
				}
			} else {
				sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Not enough arguments."));
			}
		} else if (command.getName().equalsIgnoreCase(CMD_INTAKES)) {
			if (!sender.hasPermission("fountains.intakes")) return false;
			String action = argList.isEmpty() ? null : argList.remove(0);
			if (action != null) {
				if (action.equalsIgnoreCase(ACTION_LIST)) {
					new ListIntakesAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_CREATE)) {
					new CreateIntakeAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_REMOVE)) {
					new RemoveIntakeAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_QUERY)) {
					new QueryAction(manager, argList.toArray(new String[] {})).run(sender);
					return true;
				} else {
					sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Invalid action.") + " <action> must be one of 'create', 'remove', 'list', 'query'.");
				}
			} else {
				sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Not enough arguments."));
			}
		} else if (command.getName().equalsIgnoreCase(CMD_VALVES)) {
			if (!sender.hasPermission("fountains.valves")) return false;
			String action = argList.isEmpty() ? null : argList.remove(0);
			if (action != null) {
				if (action.equalsIgnoreCase(ACTION_LIST)) {
					new ListValvesAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_CREATE)) {
					new CreateValveAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_REMOVE)) {
					new RemoveValveAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_QUERY)) {
					new QueryAction(manager, argList.toArray(new String[] {})).run(sender);
					return true;
				} else {
					sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Invalid action.") + " <action> must be one of 'create', 'remove', 'list', 'query'.");
				}
			} else {
				sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Not enough arguments."));
			}
		} else if (command.getName().equalsIgnoreCase(CMD_SPRINKLERS)) {
			if (!sender.hasPermission("fountains.sprinklers")) return false;
			String action = argList.isEmpty() ? null : argList.remove(0);
			if (action != null) {
				if (action.equalsIgnoreCase(ACTION_LIST)) {
					new ListSprinklersAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_CREATE)) {
					new CreateSprinklerAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_REMOVE)) {
					new RemoveSprinklerAction(manager, argList.toArray(new String[]{})).run(sender);
					return true;
				} else if (action.equalsIgnoreCase(ACTION_QUERY)) {
					new QueryAction(manager, argList.toArray(new String[] {})).run(sender);
					return true;
				} else {
					sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Invalid action.") + " <action> must be one of 'create', 'remove', 'list', 'query'.");
				}
			} else {
				sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Not enough arguments."));
			}
		}
		return false;
	}

	// TODO Tab completions
}
