package me.polaris120990.BuySun;

import java.util.ArrayList;
import java.util.List;


import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;

public class BuySunListener extends PlayerListener
{
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		if(BuySun.Players.get(event.getPlayer().getName() + ".canuse") == null)
		{
			if(BuySun.Players.get("player.list") == null)
			{
				List<String> players = new ArrayList<String>();
				players.add(event.getPlayer().getName());
				BuySun.Players.set("player.list", players);
			}
			else
			{
				@SuppressWarnings("unchecked")
				List<String> playerlist = BuySun.Players.getList("player.list");
				playerlist.add(event.getPlayer().getName());
				BuySun.Players.set("player.list", playerlist);		
			}
			BuySun.Players.set(event.getPlayer().getName() + ".canuse", true);
			BuySun.saveYamls();
		}
	}
}
