package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.Intake;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RemoveIntakeAction extends Action {

	private static final String USAGE = "Usage: /intake remove <name>";

	private FountainsManager manager;

	public RemoveIntakeAction(FountainsManager manager, String[] args) {
		super(args);
		this.manager = manager;
	}

	@Override
	public void run(CommandSender sender) {
		if (args.length != 1) {
			sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Missing required argument.") + " " + USAGE);
			return;
		}
		String name = args[0];
		Intake intake = manager.getIntake(name);
		if (intake != null) {
			manager.removeIntake(intake);
			sender.sendMessage(ChatColor.GREEN + "Intake removed.");
		} else {
			sender.sendMessage(ChatColor.RED + "Intake not found with that name.");
		}
	}
}
