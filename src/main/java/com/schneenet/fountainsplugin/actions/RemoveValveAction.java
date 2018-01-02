package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.config.Strings;
import com.schneenet.fountainsplugin.models.Valve;
import org.bukkit.command.CommandSender;

public class RemoveValveAction extends Action {
	private static final String USAGE = "/valves remove <name> [<name> ...]";

	private FountainsManager manager;

	public RemoveValveAction(FountainsManager manager, String[] args, Strings l10n) {
		super(args, l10n);
		this.manager = manager;
	}

	@Override
	public void run(CommandSender sender) {
		if (args.length < 1) {
			sender.sendMessage(l10n.getFormattedString(R.string.errors_missing_required_arg, "name", USAGE));
			return;
		}
		for (String name : args) {
			Valve valve = manager.getValve(name);
			if (valve != null) {
				if (manager.removeValve(valve)) {
					sender.sendMessage(l10n.getFormattedString(R.string.removed_valve, name));
				} else {
					sender.sendMessage(l10n.getFormattedString(R.string.errors_generic_remove_valve, name));
				}
			} else {
				sender.sendMessage(l10n.getFormattedString(R.string.errors_not_found_valve, name));
			}
		}
	}
}
