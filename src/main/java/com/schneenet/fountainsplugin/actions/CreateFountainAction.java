package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.Fountain;
import com.schneenet.fountainsplugin.models.RedstoneRequirementState;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.DirectionalContainer;

public class CreateFountainAction extends Action {

	private static final String USAGE = "Usage: /fountains create <name> <power> [<redstone state>]";

	private FountainsManager manager;

	public CreateFountainAction(FountainsManager manager, String[] args) {
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
			int power = Integer.parseInt(args[1]);
			if (power > Fountain.MAX_POWER) power = Fountain.MAX_POWER;
			if (power < 1) power = 1;
			String redstone = args.length > 2 ? args[2] : null;
			Block target = player.getTargetBlock(null, 16);
			if (target != null && target.getType() == Material.DISPENSER && target.getState().getData() instanceof DirectionalContainer && ((DirectionalContainer) target.getState().getData()).getFacing() == BlockFace.UP) {
				RedstoneRequirementState redstoneState = RedstoneRequirementState.IGNORED;
				if (redstone != null) {
					if (redstone.equalsIgnoreCase("on") || redstone.equalsIgnoreCase("active"))
						redstoneState = RedstoneRequirementState.ACTIVE;
					else if (redstone.equalsIgnoreCase("off") || redstone.equalsIgnoreCase("inactive"))
						redstoneState = RedstoneRequirementState.INACTIVE;
					else sender.sendMessage("Redstone value invalid. Redstone will be ignored.");
				}
				Fountain fountain = new Fountain(name, player.getWorld().getName(), target.getX(), target.getY(), target.getZ(), power, redstoneState);
				if (manager.createFountain(sender, fountain)) {
					sender.sendMessage(ChatColor.GREEN + "Fountain created.");
				} else {
					sender.sendMessage(ChatColor.RED + "An error occurred while creating the fountain.");
				}
			} else {
				sender.sendMessage(ChatColor.RED + "Target is invalid. Make sure you are looking at a Dispenser facing up from within 16 blocks.");
			}
		} catch (NumberFormatException ex) {
			sender.sendMessage(ChatColor.RED + "Power must be a whole number.");
		}
	}
}
