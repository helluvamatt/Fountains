package com.schneenet.fountainsplugin.config;

import org.bukkit.configuration.ConfigurationSection;

public class DynmapConfig {

	private static final String KEY_SHOW_FOUNTAINS = "showFountains";
	private static final String KEY_SHOW_INTAKES = "showIntakes";
	private static final String KEY_SHOW_VALVES = "showValves";
	private static final String KEY_SHOW_SPRINKLERS = "showSprinklers";
	private static final String KEY_SHOW_PIPES = "showPipes";
	
	private boolean showFountains;
	private boolean showIntakes;
	private boolean showValves;
	private boolean showSprinklers;
	private boolean showPipes;
	
	DynmapConfig(ConfigurationSection section) {
		showFountains = section.getBoolean(KEY_SHOW_FOUNTAINS, false);
		showIntakes = section.getBoolean(KEY_SHOW_INTAKES, false);
		showValves = section.getBoolean(KEY_SHOW_VALVES, false);
		showSprinklers = section.getBoolean(KEY_SHOW_SPRINKLERS, false);
		showPipes = section.getBoolean(KEY_SHOW_PIPES, false);
	}

	public boolean isShowFountains() {
		return showFountains;
	}

	public boolean isShowIntakes() {
		return showIntakes;
	}

	public boolean isShowValves() {
		return showValves;
	}

	public boolean isShowSprinklers() {
		return showSprinklers;
	}

	public boolean isShowPipes() {
		return showPipes;
	}
}
