package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.config.Strings;

import java.util.List;

public class ListSprinklersAction extends PagedListingAction {

	private FountainsManager manager;

	public ListSprinklersAction(FountainsManager manager, String[] args, Strings l10n)
	{
		super(args, l10n);
		this.manager = manager;
	}

	@Override
	List<String> getItems() {
		return select(manager.getSprinklers(), item -> l10n.getFormattedListingString(item, false));
	}
}
