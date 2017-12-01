package com.schneenet.fountainsplugin.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public class Strings {

	private ConfigurationSection section;

	Strings(ConfigurationSection section) {
		this.section = section;
	}

	public String getLocalizedString(String name) {
		return section.getString(name);
	}

	public String getFormattedString(String name) {
		return formatString(getLocalizedString(name));
	}

	private String formatString(String str) {
		return ChatColor.translateAlternateColorCodes('%', str);
		// TODO Alter this to allow %% for literal percent
	}

}
