package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;

import java.util.List;

public class ListSprinklersAction extends PagedListingAction {

	private FountainsManager manager;

	public ListSprinklersAction(FountainsManager manager, String[] args)
	{
		super(args);
		this.manager = manager;
	}

	@Override
	List<String> getItems() {
		return select(manager.getSprinklers(), Utils::toChatString);
	}
}
