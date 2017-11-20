package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.Fountain;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RemoveFountainAction extends Action {

	private static final String USAGE = "Usage: /fountains remove <name>";

	private FountainsManager manager;

	public RemoveFountainAction(FountainsManager manager, String[] args) {
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
		Fountain fountain = manager.getFountain(name);
		if (fountain != null) {
			if (manager.removeFountain(fountain)) {
				sender.sendMessage(ChatColor.GREEN + "Fountain removed.");
			} else {
				sender.sendMessage(ChatColor.RED + "An error occurred while removing the fountain.");
			}
		} else {
			sender.sendMessage(ChatColor.RED + "Fountain not found with that name.");
		}
	}
}
