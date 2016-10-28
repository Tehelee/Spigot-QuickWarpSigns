package com.tehelee.quickWarpSigns;

import java.awt.Event;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;

public class Reload
{
	@SuppressWarnings("unchecked")
	public static void reload(CommandSender sender)
	{
		if (sender instanceof Player)
		{
			Main.message(sender, "Reloading...", true);
			
			Path file = Paths.get(Main.instance.getDataFolder() + "//" + "reloader");
			
			List<String> lines = Arrays.asList(((Player) sender).getName());

			try 
			{
				Files.write(file, lines, Charset.forName("UTF-8"));
			}
			catch (IOException e) { }
		}
		
		final Plugin plugin = (Plugin)Main.instance;
		
		final PluginManager manager = Main.pluginManager;
		
		String name = plugin.getName();
		
		SimpleCommandMap commandMap = null;

        List<Plugin> plugins = null;

        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;

        boolean reloadlisteners = true;

        if (manager != null) {

        	manager.disablePlugin(plugin);

            try {

                Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
                pluginsField.setAccessible(true);
                plugins = (List<Plugin>) pluginsField.get(manager);

                Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
                lookupNamesField.setAccessible(true);
                names = (Map<String, Plugin>) lookupNamesField.get(manager);

                try {
                    Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                    listenersField.setAccessible(true);
                    listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(manager);
                } catch (Exception e) {
                    reloadlisteners = false;
                }

                Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                commandMap = (SimpleCommandMap) commandMapField.get(manager);

                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                commands = (Map<String, Command>) knownCommandsField.get(commandMap);

            }
            catch (NoSuchFieldException e)
            {
                Main.message(null, "Unload failed.", true);
            }
            catch (IllegalAccessException e)
            {
            	Main.message(null, "Unload failed.", true);
            }
        }

        manager.disablePlugin(plugin);

        if (plugins != null && plugins.contains(plugin))
            plugins.remove(plugin);

        if (names != null && names.containsKey(name))
            names.remove(name);

        if (listeners != null && reloadlisteners)
        {
            for (SortedSet<RegisteredListener> set : listeners.values())
            {
                for (Iterator<RegisteredListener> it = set.iterator(); it.hasNext(); )
                {
                    RegisteredListener value = it.next();
                    if (value.getPlugin() == plugin)
                    {
                        it.remove();
                    }
                }
            }
        }

        if (commandMap != null)
        {
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand)
                {
                    PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin() == plugin)
                    {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }
        }

        // Attempt to close the classloader to unlock any handles on the plugin's
        // jar file.
        ClassLoader cl = plugin.getClass().getClassLoader();

        if (cl instanceof URLClassLoader)
        {
            try
            {
                ((URLClassLoader) cl).close();
            }
            catch (IOException ex)
            {
                Logger.getLogger(ClassLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Will not work on processes started with the -XX:+DisableExplicitGC flag,
        // but lets try it anyway. This tries to get around the issue where Windows
        // refuses to unlock jar files that were previously loaded into the JVM.
        System.gc();
		
		Plugin target = null;

		File pluginDir = new File("plugins");

		if (!pluginDir.isDirectory())
		{
			Main.message(null, "Could not load plugin directory.", true);
			return;
		}

		File pluginFile = new File(pluginDir, name + ".jar");

		if (!pluginFile.isFile())
		{
			for (File f : pluginDir.listFiles())
			{
				if (f.getName().endsWith(".jar"))
				{
					try
					{
						PluginDescriptionFile desc = Main.instance.getPluginLoader().getPluginDescription(f);
						if (desc.getName().equalsIgnoreCase(name))
						{
							pluginFile = f;
							break;
						}
					}
					catch (InvalidDescriptionException e)
					{
						Main.message(null, "Could not load plugin description.", true);
						return;
					}
				}
			}
		}

		try
		{
			Main.pluginManager.disablePlugin(plugin);
			target = Main.pluginManager.loadPlugin(pluginFile);
		}
		catch (InvalidDescriptionException e)
		{
			Main.message(null, "Invalid plugin description.", true);
			return;
		}
		catch (InvalidPluginException e)
		{
			Main.message(null, "Invalid plugin.", true);
			return;
		}

		target.onLoad();
		Main.pluginManager.enablePlugin(target);
		
		Main.message(null, HelpText.logRestart, true);

		return;
	}
	
	public static void onEnable()
	{
		Path file = Paths.get(Main.instance.getDataFolder() + "//" + "reloader");
		
		List<String> lines = null;
		try
		{
			lines = Files.readAllLines(file);
		}
		catch (IOException e)
		{ }
		
		if ((lines != null) && (lines.size() > 0))
		{
			String name = lines.get(0);
			for (Player p : Main.server.getOnlinePlayers())
			{
				if (p.getName().equals(name))
				{
					Main.message(p, HelpText.logStart, true);
				}
			}
		}
		
		try
		{
			Files.delete(file);
		}
		catch (IOException e)
		{ }
	}
}
