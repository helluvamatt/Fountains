package com.schneenet.fountainsplugin.actions;

import com.schneenet.fountainsplugin.config.Strings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class PagedListingAction extends Action {

	private int page;

	private static final int PAGE_SIZE = 10;

	PagedListingAction(String[] args, Strings l10n)
	{
		super(args, l10n);
		if (args.length > 0)
		{
			String pageArg = args[0];
			try
			{
				page = Integer.parseInt(pageArg);
			}
			catch (NumberFormatException ex)
			{
				page = 1;
			}
			if (page < 1) page = 1;
		}
		else
		{
			this.page = 1;
		}
	}

	abstract List<String> getItems();

	@Override
	public void run(CommandSender sender)
	{
		List<String> items = getItems();
		if ((page - 1) * PAGE_SIZE < items.size()) {
			List<String> itemsPage = items.subList((page - 1) * PAGE_SIZE, Integer.min(page * PAGE_SIZE, items.size()));
			for(String item : itemsPage)
			{
				sender.sendMessage(item);
			}
		}
		else
		{
			sender.sendMessage(ChatColor.GOLD + "No items found.");
		}
	}

	<T> List<String> select(List<T> list, Function<T, String> action)
	{
		ArrayList<String> sList = new ArrayList<>();
		for (T item : list)
		{
			sList.add(action.apply(item));
		}
		return sList;
	}
}
