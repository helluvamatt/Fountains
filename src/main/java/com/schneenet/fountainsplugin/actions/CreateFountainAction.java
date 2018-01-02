package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.config.Strings;
import com.schneenet.fountainsplugin.models.Fountain;
import com.schneenet.fountainsplugin.models.RedstoneRequirementState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.DirectionalContainer;

public class CreateFountainAction extends Action {

	private static final String USAGE = "/fountains create <name> <power> [<redstone state>]";

	private FountainsManager manager;

	public CreateFountainAction(FountainsManager manager, String[] args, Strings l10n) {
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
					else sender.sendMessage(l10n.getFormattedString(R.string.redstone_invalid));
				}
				if (power >= Fountain.MIN_FILL_POWER) {
					int maxFillHeight = power - Fountain.MIN_FILL_POWER + 1;
					for (int i = 1; i <= maxFillHeight; i++) {
						Block airAbove = target.getRelative(BlockFace.UP, i);
						if (airAbove != null && !manager.checkFillArea(airAbove.getLocation())) {
							sender.sendMessage(l10n.getFormattedString(R.string.errors_fill_area_too_large));
							break;
						}
					}
				}
				Fountain fountain = new Fountain(name, player.getWorld().getName(), target.getX(), target.getY(), target.getZ(), power, redstoneState);
				if (manager.createFountain(sender, fountain)) {
					sender.sendMessage(l10n.getFormattedString(R.string.created_fountain, name));
				} else {
					sender.sendMessage(l10n.getFormattedString(R.string.errors_generic_create_fountain));
				}
			} else {
				sender.sendMessage(l10n.getFormattedString(R.string.errors_invalid_target_fountain));
			}
		} catch (NumberFormatException ex) {
			sender.sendMessage(l10n.getFormattedString(R.string.errors_power_format));
		}
	}
}
