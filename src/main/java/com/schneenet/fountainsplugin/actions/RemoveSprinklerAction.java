package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.config.Strings;
import com.schneenet.fountainsplugin.models.Sprinkler;
import org.bukkit.command.CommandSender;

public class RemoveSprinklerAction extends Action {

	private static final String USAGE = "/intake remove <name>";

	private FountainsManager manager;

	public RemoveSprinklerAction(FountainsManager manager, String[] args, Strings l10n) {
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
			Sprinkler sprinkler = manager.getSprinkler(name);
			if (sprinkler != null) {
				if (manager.removeSprinkler(sprinkler)) {
					sender.sendMessage(l10n.getFormattedString(R.string.removed_sprinkler, name));
				} else {
					sender.sendMessage(l10n.getFormattedString(R.string.errors_generic_remove_sprinkler, name));
				}
			} else {
				sender.sendMessage(l10n.getFormattedString(R.string.errors_not_found_sprinkler, name));
			}
		}
	}

}
