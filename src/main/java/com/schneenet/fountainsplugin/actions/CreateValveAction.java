package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.config.Strings;
import com.schneenet.fountainsplugin.models.Valve;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateValveAction extends Action {

	private static final String USAGE = "/valves create <name> [<redstone state>]";

	private FountainsManager manager;

	public CreateValveAction(FountainsManager manager, String[] args, Strings l10n) {
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
		String name = args[0];
		Block target = player.getTargetBlock(null, 16);
		if (target != null && (target.getType() == Material.REDSTONE_LAMP_ON || target.getType() == Material.REDSTONE_LAMP_OFF)) {
			Valve valve = new Valve(name, player.getWorld().getName(), target.getX(), target.getY(), target.getZ());
			if (manager.createValve(sender, valve)) {
				sender.sendMessage(l10n.getFormattedString(R.string.created_valve, name));
			} else {
				sender.sendMessage(l10n.getFormattedString(R.string.errors_generic_create_valve));
			}
		} else {
			sender.sendMessage(l10n.getFormattedString(R.string.errors_invalid_target_valve));
		}
	}
}
