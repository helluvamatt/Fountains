package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.RedstoneRequirementState;
import com.schneenet.fountainsplugin.models.Sprinkler;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateSprinklerAction extends Action {

	private static final String USAGE = "Usage: /sprinklers create <name> <spread> [<redstone state>]";

	private FountainsManager manager;

	public CreateSprinklerAction(FountainsManager manager, String[] args) {
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
		if (args.length < 2) {
			sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Missing required argument.") + " " + USAGE);
			return;
		}
		try {
			String name = args[0];
			int spread = Integer.parseInt(args[1]);
			if (spread > Sprinkler.MAX_SPREAD) spread = Sprinkler.MAX_SPREAD;
			if (spread < 1) spread = 1;
			String redstone = args.length > 2 ? args[2] : null;
			Block target = player.getTargetBlock(null, 16);
			if (target != null && target.getType() == Material.DISPENSER ) {
				RedstoneRequirementState redstoneState = RedstoneRequirementState.IGNORED;
				if (redstone != null) {
					if (redstone.equalsIgnoreCase("on") || redstone.equalsIgnoreCase("active"))
						redstoneState = RedstoneRequirementState.ACTIVE;
					else if (redstone.equalsIgnoreCase("off") || redstone.equalsIgnoreCase("inactive"))
						redstoneState = RedstoneRequirementState.INACTIVE;
					else sender.sendMessage("Redstone value invalid. Redstone will be ignored.");
				}
				Sprinkler sprinkler = new Sprinkler(name, player.getWorld().getName(), target.getX(), target.getY(), target.getZ(), spread, redstoneState);
				if (manager.createSprinkler(sender, sprinkler)) {
					sender.sendMessage(ChatColor.GREEN + "Sprinkler created.");
				} else {
					sender.sendMessage(ChatColor.RED + "An error occurred while creating the sprinkler.");
				}
			} else {
				sender.sendMessage(ChatColor.RED + "Target is invalid. Make sure you are looking at a Dropper from within 16 blocks.");
			}
		} catch (NumberFormatException ex) {
			sender.sendMessage(ChatColor.RED + "Spread must be a whole number.");
		}
	}
}
