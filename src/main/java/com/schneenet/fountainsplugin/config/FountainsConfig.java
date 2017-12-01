package com.schneenet.fountainsplugin.config;

import org.bukkit.configuration.Configuration;

public class FountainsConfig {

	private static final String SECTION_DYNMAP = "dynmap";
	private static final String SECTION_STRINGS = "strings";

	private DynmapConfig dynmapConfig;
	private Strings localization;

	public FountainsConfig(Configuration configuration) {
		this.dynmapConfig = new DynmapConfig(configuration.getConfigurationSection(SECTION_DYNMAP));
		this.localization = new Strings(configuration.getConfigurationSection(SECTION_STRINGS));
	}

	public DynmapConfig getDynmapConfig() {
		return dynmapConfig;
	}

	public Strings getLocalization() {
		return localization;
	}
}
