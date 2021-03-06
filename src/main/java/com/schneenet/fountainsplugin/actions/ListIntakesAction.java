package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.config.Strings;

import java.util.List;

public class ListIntakesAction extends PagedListingAction {

	private FountainsManager manager;

	public ListIntakesAction(FountainsManager manager, String[] args, Strings l10n)
	{
		super(args, l10n);
		this.manager = manager;
	}

	@Override
	List<String> getItems() {
		return select(manager.getIntakes(),item -> l10n.getFormattedListingString(item, false));
	}
}
