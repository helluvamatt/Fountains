package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.config.Strings;
import com.schneenet.fountainsplugin.models.Intake;
import org.bukkit.command.CommandSender;

public class RemoveIntakeAction extends Action {

	private static final String USAGE = "Usage: /intake remove <name> [<name> ...]";

	private FountainsManager manager;

	public RemoveIntakeAction(FountainsManager manager, String[] args, Strings l10n) {
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
			Intake intake = manager.getIntake(name);
			if (intake != null) {
				if (manager.removeIntake(intake)) {
					sender.sendMessage(l10n.getFormattedString(R.string.removed_intake, name));
				} else {
					sender.sendMessage(l10n.getFormattedString(R.string.errors_generic_remove_intake, name));
				}
			} else {
				sender.sendMessage(l10n.getFormattedString(R.string.errors_not_found_intake, name));
			}
		}
	}
}
