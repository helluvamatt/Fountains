package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.FountainsManager;
import com.schneenet.fountainsplugin.Utils;

import java.util.List;

public class ListIntakesAction extends PagedListingAction {

	private FountainsManager manager;

	public ListIntakesAction(FountainsManager manager, String[] args)
	{
		super(args);
		this.manager = manager;
	}

	@Override
	List<String> getItems() {
		return select(manager.getIntakes(), (item) -> Utils.toChatString(item));
	}
}
