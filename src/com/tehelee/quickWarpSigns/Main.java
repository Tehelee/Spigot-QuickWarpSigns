package com.tehelee.quickWarpSigns;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin
{
	public static FileConfiguration config;
	public static ConsoleCommandSender console;
	public static Server server;
	
	public static Main instance;
	
	public static PluginManager pluginManager;
	
	public Main()
	{
		Main.instance = this;
	}
	
	@Override
	public void onEnable()
	{
		initializeConfig();
		
		Main.server = getServer();
		
		Main.console = Main.server.getConsoleSender();
		
		Main.pluginManager = Main.server.getPluginManager();
		
		QuickWarp.populateRegister();
		
		Main.pluginManager.registerEvents(new SignListener(), this);
		
		this.getCommand("QuickWarp").setExecutor(new CmdQuickWarp());
		
		message(null, HelpText.logStart, true);
		
		Reload.onEnable();
	}
	
	@Override
	public void onDisable()
	{	
		message(null, HelpText.logStop, true);
	}
	
	private void initializeConfig()
	{
		Main.config = this.getConfig();
		
		Main.config.options().copyDefaults(true);
		
		writeConfig();
	}
	
	public static void writeConfig()
	{
		if (null != instance)
		{
			config.addDefault("WarpSound", "ENTITY_GHAST_SHOOT");
			
			instance.saveConfig();
			
			instance.reloadConfig();
			
			Main.config = instance.getConfig();
		}
	}
	
	public static void message(Player player, String message)
	{
		message((CommandSender)player, message);
	}
	
	public static void message(CommandSender sender, String message)
	{
		message(sender, message, false);
	}
	
	public static void message(CommandSender sender, String message, boolean prefix)
	{	
		if ((null != sender) && (sender instanceof Player))
		{
			sender.sendMessage((prefix ? HelpText.PluginName : "" ) + ChatColor.WHITE + message);
		}
		else
		{
			Main.console.sendMessage((prefix ? HelpText.PluginName : "" ) + ChatColor.GRAY + ChatColor.stripColor(message));
		}
	}	
}
