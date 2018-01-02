package com.schneenet.fountainsplugin.config;

import com.schneenet.fountainsplugin.R;
import com.schneenet.fountainsplugin.models.Fountain;
import com.schneenet.fountainsplugin.models.ILocatable;
import com.schneenet.fountainsplugin.models.Intake;
import com.schneenet.fountainsplugin.models.Sprinkler;
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

	public String getFormattedListingString(ILocatable item, boolean includeTypePrefix) {
		StringBuilder stringBuilder = new StringBuilder();
		if (includeTypePrefix) {
			stringBuilder.append(getFormattedString(R.string.locatable_type_listing_prefix).replace("{type}", item.getClass().getSimpleName()));
		}
		stringBuilder.append(getFormattedString(R.string.listing_locatable, item));
		if (item instanceof Fountain) {
			Fountain fountain = (Fountain) item;
			stringBuilder.append(getFormattedString(R.string.listing_fountain).replace("{power}", String.valueOf(fountain.getPower())).replace("{redstone}", fountain.getRedstoneRequirementState().toString()));
		}
		else if (item instanceof Intake) {
			Intake intake = (Intake) item;
			stringBuilder.append(getFormattedString(R.string.listing_intake).replace("{speed}", String.valueOf(intake.getSpeed())).replace("{redstone}", intake.getRedstoneRequirementState().toString()));
		}
		else if (item instanceof Sprinkler) {
			Sprinkler sprinkler = (Sprinkler) item;
			stringBuilder.append(getFormattedString(R.string.listing_sprinkler).replace("{spread}", String.valueOf(sprinkler.getSpread())).replace("{redstone}", sprinkler.getRedstoneRequirementState().toString()));
		}
		return stringBuilder.toString();
	}

	public String getFormattedString(String name, ILocatable item) {
		return getFormattedString(name).replace("{name}", item.getName()).replace("{world}", item.getWorldName()).replace("{x}", Long.toString(item.getX())).replace("{y}", Long.toString(item.getY())).replace("{z}", Long.toString(item.getZ()));
	}

	public String getFormattedString(String name, String arg) {
		return getFormattedString(name).replace("{name}", arg);
	}

	public String getFormattedString(String name, String arg, String usage) {
		return getFormattedString(name).replace("{arg}", arg).replace("{usage}", usage);
	}

	public String getFormattedString(String name) {
		return formatString(getLocalizedString(name));
	}

	private String formatString(String str) {
		return ChatColor.translateAlternateColorCodes('%', str).replace("%%", "%");
	}

}
