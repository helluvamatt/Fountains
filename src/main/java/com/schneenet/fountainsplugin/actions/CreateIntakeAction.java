package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.DrainAreaTooLargeException;
import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.config.Strings;
import com.schneenet.fountainsplugin.models.Intake;
import com.schneenet.fountainsplugin.models.RedstoneRequirementState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateIntakeAction extends Action {

	private static final String USAGE = "/intakes create <name> <speed> [<redstone state>]";

	private FountainsManager manager;

	public CreateIntakeAction(FountainsManager manager, String[] args, Strings l10n) {
		super(args, l10n);
		this.manager = manager;
	}

	@Override
	public void run(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(l10n.getFormattedString(R.string.errors_only_players));
			return;
		}
		Player player = (Player) sender;
		if (args.length < 1) {
			sender.sendMessage(l10n.getFormattedString(R.string.errors_missing_required_arg, "name", USAGE));
			return;
		}
		if (args.length < 2) {
			sender.sendMessage(l10n.getFormattedString(R.string.errors_missing_required_arg, "power", USAGE));
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
					else sender.sendMessage(l10n.getFormattedString(R.string.redstone_invalid));
				}
				if (speed >= Intake.MIN_DRAIN_SPEED) {
					Block waterTop = target.getRelative(BlockFace.UP);
					Block above = waterTop;
					while (above != null && (above.getType() == Material.WATER || above.getType() == Material.STATIONARY_WATER)) {
						waterTop = above;
						above = above.getRelative(BlockFace.UP);
					}
					try {
						manager.findDrainArea(waterTop.getLocation());
					}
					catch (DrainAreaTooLargeException ex) {
						sender.sendMessage(l10n.getFormattedString(R.string.errors_drain_area_too_large));
					}
				}
				Intake intake = new Intake(name, player.getWorld().getName(), target.getX(), target.getY(), target.getZ(), speed, redstoneState);
				if (manager.createIntake(sender, intake)) {
					sender.sendMessage(l10n.getFormattedString(R.string.created_intake, name));
				} else {
					sender.sendMessage(l10n.getFormattedString(R.string.errors_generic_create_intake));
				}
			} else {
				sender.sendMessage(l10n.getFormattedString(R.string.errors_invalid_target_intake));
			}
		} catch (NumberFormatException ex) {
			sender.sendMessage(l10n.getFormattedString(R.string.errors_speed_format));
		}
	}

}
