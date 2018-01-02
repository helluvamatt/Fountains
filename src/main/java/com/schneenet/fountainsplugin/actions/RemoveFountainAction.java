package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.config.Strings;
import com.schneenet.fountainsplugin.models.Fountain;
import org.bukkit.command.CommandSender;

public class RemoveFountainAction extends Action {

	private static final String USAGE = "/fountains remove <name> [<name> ...]";

	private FountainsManager manager;

	public RemoveFountainAction(FountainsManager manager, String[] args, Strings l10n) {
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
			Fountain fountain = manager.getFountain(name);
			if (fountain != null) {
				if (manager.removeFountain(fountain)) {
					sender.sendMessage(l10n.getFormattedString(R.string.removed_fountain, name));
				} else {
					sender.sendMessage(l10n.getFormattedString(R.string.errors_generic_remove_fountain, name));
				}
			} else {
				sender.sendMessage(l10n.getFormattedString(R.string.errors_not_found_fountain, name));
			}
		}

	}
}
