package com.schneenet.fountainsplugin;

import com.schneenet.fountainsplugin.models.Fountain;
import com.schneenet.fountainsplugin.models.ILocatable;
import com.schneenet.fountainsplugin.models.Intake;
import org.bukkit.ChatColor;

public class Utils {

	/**
	 * Format a span of text with the given ChatColor
	 * @param color Color to use
	 * @param spannedText Text that will be formatted
	 * @return Formatted text
	 */
	public static String colorSpan(ChatColor color, String spannedText) {
		return color + spannedText + ChatColor.RESET;
	}

	/**
	 * Format an item into a colored string appropriate for sending to a user as a chat message
	 * @param item Item to be formatted
	 * @return Formatted string
	 */
	public static String toChatString(ILocatable item) {
		String str = colorSpan(ChatColor.BLUE, item.getName()) + " [World: " + colorSpan(ChatColor.GREEN, item.getWorldName()) + "] (" + item.getX() + "," + item.getY() + "," + item.getZ() + ")";
		if (item instanceof Fountain) {
			Fountain fountain = (Fountain) item;
			str = str + " Power: " + colorSpan(ChatColor.AQUA, String.valueOf(fountain.getPower())) + " Redstone: " + colorSpan(ChatColor.RED, fountain.getRedstoneRequirementState().toString());
		}
		else if (item instanceof Intake) {
			Intake intake = (Intake) item;
			str = str + " Speed: " + colorSpan(ChatColor.AQUA, String.valueOf(intake.getSpeed())) + " Redstone: " + colorSpan(ChatColor.RED, intake.getRedstoneRequirementState().toString());
		}
		return str;
	}

}
