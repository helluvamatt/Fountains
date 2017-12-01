package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.Valve;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RemoveValveAction extends Action {
	private static final String USAGE = "Usage: /valves remove <name>";

	private FountainsManager manager;

	public RemoveValveAction(FountainsManager manager, String[] args) {
		super(args);
		this.manager = manager;
	}

	@Override
	public void run(CommandSender sender) {
		if (args.length != 1) {
			sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Missing required argument.") + " " + USAGE);
			return;
		}
		String name = args[0];
		Valve valve = manager.getValve(name);
		if (valve != null) {
			if (manager.removeValve(valve)) {
				sender.sendMessage(ChatColor.GREEN + "Valve removed.");
			} else {
				sender.sendMessage(ChatColor.RED + "An error occurred while removing the valve.");
			}
		} else {
			sender.sendMessage(ChatColor.RED + "Valve not found with that name.");
		}
	}
}
