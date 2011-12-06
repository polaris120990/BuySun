package me.polaris120990.BuySun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BuySun extends JavaPlugin
{
    public static Permission permission = null;
    public static Economy economy = null;
	public static File ConfigFile;
	public static FileConfiguration Config;
	public static File PlayerFile;
	public static FileConfiguration Players;
	HashMap<String, Integer> scheds = new HashMap<String, Integer>();
	HashMap<String, Integer> times = new HashMap<String, Integer>();
	HashMap<String, Boolean> status = new HashMap<String, Boolean>();
	BuySunListener Plistener = new BuySunListener();
	public final Logger logger = Logger.getLogger("Minecraft");
	
	public void onEnable()
	{
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_JOIN, Plistener, Event.Priority.Highest, this);
		setupEconomy();
		setupPermissions();
		ConfigFile = new File(getDataFolder(), "config.yml");
		PlayerFile = new File(getDataFolder(), "players.yml");
	    try {
	        firstRun();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    Config = new YamlConfiguration();
	    Players = new YamlConfiguration();
	    loadYamls();
	    reHash();
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info("[" + pdfFile.getName() + "] v" + pdfFile.getVersion() + " has been enabled.");
	}
	
	@SuppressWarnings("unchecked")
	public void reHash()
	{
		if(Players.get("player.list") != null)
		{
			List<String> playerlist = Players.getList("player.list");
			final String[] players;
			players = playerlist.toArray(new String[]{});
			int j = 0;
			while(j < players.length)
			{
				final int i = j;
				if(Players.getBoolean(players[i] + ".canuse") == false)
				{
					if(Players.get(players[i] + ".timeleft") == null)
					{
						Players.set(players[i] + ".canuse", true);
						saveYamls();
						j++;
						continue;
					}
					status.put(players[i], false);
					times.put(players[i], Players.getInt(players[i] + ".timeleft"));
					scheds.put(players[i],Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
						public void run(){
						   int tr = times.get(players[i]);
						   times.put(players[i], tr - 1);
						   Players.set(players[i] + ".timeleft", times.get(players[i]));
						   if(times.get(players[i]) == 0)
						   {
							   Bukkit.getServer().getScheduler().cancelTask(scheds.get(players[i]));
							   status.put(players[i], true);
							   Players.set(players[i] + ".canuse", true);
							   Players.set(players[i] + ".timeleft", null);
							   saveYamls();
						   }
					   }
					},0, 20L));
					
				}
				j++;
			}
		}
	}
	public void onDisable()
	{
		saveYamls();
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info("[" + pdfFile.getName() + "] Data saved. Plugin successfully disabled.");
		
	}
	public boolean onCommand(CommandSender sender, Command cmd, String CommandLabel, String[] args)
	{
		readCommand((Player) sender, CommandLabel, args);
		return false;
	}
	
	public void readCommand(final Player sender, String command, String[] args)
	{
		if(command.equalsIgnoreCase("buysun"))
		{
			if(!permission.has(sender, "buysun.use"))
			{
				sender.sendMessage(ChatColor.RED + "You do not have permission to do this!");
				return;
			}
			Integer price = Config.getInt("price");
			if(status.get(sender.getName()) == null)
			{
				status.put(sender.getName(), Players.getBoolean(sender.getName() + ".canuse"));
			}
			if(economy.has(sender.getName(), price))
			{
				if(status.get(sender.getName()) == true)
				{
					times.put(sender.getName(), Config.getInt("timedelay"));
					Bukkit.getWorld(sender.getWorld().getName()).setTime(0);
					Bukkit.getWorld(sender.getWorld().getName()).setStorm(false);
					economy.withdrawPlayer(sender.getName(), price);
					status.put(sender.getName(), false);
					Players.set(sender.getName() + ".canuse", false);
					saveYamls();
					sender.sendMessage(ChatColor.GOLD + "You have spent " + ChatColor.GREEN + price.toString() + ChatColor.GOLD + " to buy the sun!");
					Bukkit.broadcastMessage(ChatColor.AQUA + sender.getName() + ChatColor.GOLD + " has bought the sun!");
					scheds.put(sender.getName(),Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
						public void run(){
						   int tr = times.get(sender.getName());
						   times.put(sender.getName(), tr - 1);
						   Players.set(sender.getName() + ".timeleft", times.get(sender.getName()));
						   if(times.get(sender.getName()) == 0)
						   {
							   Bukkit.getServer().getScheduler().cancelTask(scheds.get(sender.getName()));
							   status.put(sender.getName(), true);
							   Players.set(sender.getName() + ".canuse", true);
							   Players.set(sender.getName() + ".timeleft", null);
							   saveYamls();
						   }
					   }
					},0, 20L));
					return;
				}
				else if(status.get(sender.getName()) == false)
				{

					Integer timeremain = times.get(sender.getName());
					if(timeremain > 3600)
					{
						Integer hours = (timeremain / 3600);
						Integer minutes = ((timeremain % 3600) / 60);
						Integer seconds = ((timeremain % 3600) % 60);
						sender.sendMessage(ChatColor.YELLOW + "You have " + ChatColor.BLUE + hours.toString() + "h " + ChatColor.RED + minutes.toString() + "m " + ChatColor.GREEN + seconds.toString() + "s " + ChatColor.YELLOW + "until you can buy the sun again!");
					}
					else if(timeremain > 60)
					{
						Integer minutes = (timeremain / 60);
						Integer seconds = (timeremain % 60);
						sender.sendMessage(ChatColor.YELLOW + "You have " + ChatColor.RED + minutes.toString() + "m " + ChatColor.GREEN + seconds.toString() + "s " + ChatColor.YELLOW + "until you can buy the sun again!");
					}
					else if(timeremain < 60)
					{
						sender.sendMessage(ChatColor.YELLOW + "You have " + ChatColor.GREEN + timeremain.toString() + "s " + ChatColor.YELLOW + "until you can buy the sun again!");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "He's dead, Jim! The player.yml file did not autoconfigure properly. Please try relogging. If that doesn't work contact the plugin creator.");
				}
			}
			else if(!economy.has(sender.getName(), price))
			{
				sender.sendMessage(ChatColor.RED + "It costs " + ChatColor.GREEN + price.toString() + ChatColor.RED + " to buy the sun!");
			}
		}
		
	}
    private Boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
    private Boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
	public static void saveYamls() {
	    try {
	        Config.save(ConfigFile);
	        Players.save(PlayerFile);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	public void loadYamls() {
	    try {
	        Config.load(ConfigFile);
	        Players.load(PlayerFile);

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	private void firstRun() throws Exception
	{
	    if(!ConfigFile.exists()){
	        ConfigFile.getParentFile().mkdirs();
	        copy(getResource("config.yml"), ConfigFile);
	    }
	    if(!PlayerFile.exists()){
	        PlayerFile.getParentFile().mkdirs();
	        copy(getResource("players.yml"), PlayerFile);
	    }
	}

	private void copy(InputStream in, File file) {
	    try {
	        OutputStream out = new FileOutputStream(file);
	        byte[] buf = new byte[1024];
	        int len;
	        while((len=in.read(buf))>0){
	            out.write(buf,0,len);
	        }
	        out.close();
	        in.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

}
