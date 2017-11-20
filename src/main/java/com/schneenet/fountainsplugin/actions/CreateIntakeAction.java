package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.Intake;
import com.schneenet.fountainsplugin.models.RedstoneRequirementState;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateIntakeAction extends Action {

	private static final String USAGE = "Usage: /intakes create <name> <speed> [<redstone state>]";

	private FountainsManager manager;

	public CreateIntakeAction(FountainsManager manager, String[] args) {
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
			int speed = Integer.parseInt(args[1]);
			if (speed > Intake.MAX_SPEED) speed = Intake.MAX_SPEED;
			if (speed < 1) speed = 1;
			String redstone = args.length > 2 ? args[2] : null;
			Block target = player.getTargetBlock(null, 16);
			if (target != null && target.getType() == Material.HOPPER) {
				RedstoneRequirementState redstoneState = RedstoneRequirementState.IGNORED;
				if (redstone != null) {
					if (redstone.equalsIgnoreCase("on") || redstone.equalsIgnoreCase("active"))
						redstoneState = RedstoneRequirementState.ACTIVE;
					else if (redstone.equalsIgnoreCase("off") || redstone.equalsIgnoreCase("inactive"))
						redstoneState = RedstoneRequirementState.INACTIVE;
					else sender.sendMessage("Redstone value invalid. Redstone will be ignored.");
				}
				Intake intake = new Intake(name, player.getWorld().getName(), target.getX(), target.getY(), target.getZ(), speed, redstoneState);
				if (manager.createIntake(sender, intake)) {
					sender.sendMessage(ChatColor.GREEN + "Intake created.");
				} else {
					sender.sendMessage(ChatColor.RED + "An error occurred while creating the intake.");
				}
			} else {
				sender.sendMessage(ChatColor.RED + "Target is invalid. Make sure you are looking at a Dropper from within 16 blocks.");
			}
		} catch (NumberFormatException ex) {
			sender.sendMessage(ChatColor.RED + "Speed must be a whole number.");
		}
	}

}
