package com.schneenet.fountainsplugin.actions;

import org.bukkit.command.CommandSender;

public abstract class Action {

	protected String[] args;

	Action(String[] args) {
		this.args = args;
	}

	public abstract void run(CommandSender sender);
}
