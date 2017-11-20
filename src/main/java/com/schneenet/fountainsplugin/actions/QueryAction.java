package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;
import com.schneenet.fountainsplugin.models.ILocatable;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QueryAction extends Action {

	private FountainsManager manager;

	public QueryAction(FountainsManager manager, String[] args) {
		super(args);
		this.manager = manager;
	}

	@Override
	public void run(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Utils.colorSpan(ChatColor.RED, "Only players can execute this command."));
			return;
		}
		Player player = (Player) sender;
		Block target = player.getTargetBlock(null, 16);
		if (target != null) {
			ILocatable item = manager.findByLocation(target.getLocation());
			if (item != null) {
				String type = item.getClass().getSimpleName();
				player.sendMessage(Utils.colorSpan(ChatColor.AQUA, type) + ": " + Utils.toChatString(item));
				return;
			}
		}
		player.sendMessage(Utils.colorSpan(ChatColor.RED, "There is no fountain, intake, or valve at that location."));
	}
}
