package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.Sprinkler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RemoveSprinklerAction extends Action {

	private static final String USAGE = "Usage: /intake remove <name>";

	private FountainsManager manager;

	public RemoveSprinklerAction(FountainsManager manager, String[] args) {
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
		Sprinkler sprinkler = manager.getSprinkler(name);
		if (sprinkler != null) {
			if (manager.removeSprinkler(sprinkler)) {
				sender.sendMessage(ChatColor.GREEN + "Sprinkler removed.");
			} else {
				sender.sendMessage(ChatColor.RED + "An error occurred while removing the sprinkler.");
			}
		} else {
			sender.sendMessage(ChatColor.RED + "Sprinkler not found with that name.");
		}
	}

}
