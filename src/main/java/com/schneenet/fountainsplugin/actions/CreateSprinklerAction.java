package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.config.Strings;
import com.schneenet.fountainsplugin.models.RedstoneRequirementState;
import com.schneenet.fountainsplugin.models.Sprinkler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateSprinklerAction extends Action {

	private static final String USAGE = "/sprinklers create <name> <spread> [<redstone state>]";

	private FountainsManager manager;

	public CreateSprinklerAction(FountainsManager manager, String[] args, Strings l10n) {
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
			sender.sendMessage(l10n.getFormattedString(R.string.errors_missing_required_arg, "spread", USAGE));
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
					else sender.sendMessage(l10n.getFormattedString(R.string.redstone_invalid));
				}
				Sprinkler sprinkler = new Sprinkler(name, player.getWorld().getName(), target.getX(), target.getY(), target.getZ(), spread, redstoneState);
				if (manager.createSprinkler(sender, sprinkler)) {
					sender.sendMessage(l10n.getFormattedString(R.string.created_sprinkler, name));
				} else {
					sender.sendMessage(l10n.getFormattedString(R.string.errors_generic_create_sprinkler));
				}
			} else {
				sender.sendMessage(l10n.getFormattedString(R.string.errors_invalid_target_sprinkler));
			}
		} catch (NumberFormatException ex) {
			sender.sendMessage(l10n.getFormattedString(R.string.errors_spread_format));
		}
	}
}
