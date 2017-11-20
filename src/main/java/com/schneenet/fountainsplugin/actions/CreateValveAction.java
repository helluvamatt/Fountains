package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.Valve;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateValveAction extends Action {

	private FountainsManager manager;

	private static final String USAGE = "Usage: /valves create <name> [<redstone state>]";

	public CreateValveAction(FountainsManager manager, String[] args) {
		super(args);
		this.manager = manager;
	}

	@Override
	public void run(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Only players can execute this command."));
			return;
		}
		Player player = (Player) sender;
		if (args.length < 1) {
			sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Missing required argument.") + " " + USAGE);
			return;
		}
		String name = args[0];
		Block target = player.getTargetBlock(null, 16);
		if (target != null && (target.getType() == Material.REDSTONE_LAMP_ON || target.getType() == Material.REDSTONE_LAMP_OFF)) {
			Valve valve = new Valve(name, player.getWorld().getName(), target.getX(), target.getY(), target.getZ());
			if (manager.createValve(sender, valve)) {
				sender.sendMessage(ChatColor.GREEN + "Valve created.");
			} else {
				sender.sendMessage(ChatColor.RED + "An error occurred while creating the fountain.");
			}
		} else {
			sender.sendMessage(ChatColor.RED + "Target is invalid. Make sure you are looking at a Lamp from within 16 blocks.");
		}
	}
}
