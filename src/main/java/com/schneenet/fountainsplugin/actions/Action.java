package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.config.Strings;
import org.bukkit.command.CommandSender;

public abstract class Action {

	protected String[] args;
	protected Strings l10n;

	Action(String[] args, Strings l10n) {
		this.args = args;
		this.l10n = l10n;
	}

	public abstract void run(CommandSender sender);
}
