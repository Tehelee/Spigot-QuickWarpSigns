package com.tehelee.quickWarpSigns;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class QuickWarp
{
	public static Hashtable<Player,QuickWarp> inProgress = new Hashtable<Player,QuickWarp>();
	
	private static Hashtable<String,QuickWarp> register = new Hashtable<String,QuickWarp>();
	
	private static Sound warpSound;
	
	private static boolean validLocation(Location loc)
	{
		return ((loc != null) && (loc.getWorld() != null));
	}
	
	public static void populateRegister()
	{
		try
		{
			warpSound = Sound.valueOf(Main.config.getString("WarpSound", "ENTITY_GHAST_SHOOT"));
		}
		catch (IllegalArgumentException ex)
		{
			warpSound = null;
		}
		
		File file = new File(Main.instance.getDataFolder(), "//" + "quick_warps.yml");
		FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		
		for (String compressedLocation : yaml.getKeys(false))
		{
			ConfigurationSection config;
			
			if (yaml.isConfigurationSection(compressedLocation))
				config = yaml.getConfigurationSection(compressedLocation);
			else
				continue;

			if (register.containsKey(compressedLocation)) continue;
			
			Location signLocation = extractLocation(compressedLocation);
			
			if (!validLocation(signLocation)) continue;
			
			Location warpLocation = extractLocation(config.getString("Destination"));
			
			if (!validLocation(warpLocation)) continue;
			
			QuickWarp qw = new QuickWarp(signLocation, warpLocation, (float)config.getDouble("Pitch"), (float)config.getDouble("Yaw"));
			
			register.put(compressedLocation, qw);
		}
	}
	
	public static String compressLocation(Location loc)
	{
		return String.format("%1$s %2$.0f %3$.0f %4$.0f", loc.getWorld().getUID().toString(), loc.getX(), loc.getY(), loc.getZ());
	}
	
	public static Location extractLocation(String str)
	{
		String[] split = str.split(" ");
		
		if (split.length != 4) return null;
		
		World world = Main.server.getWorld(UUID.fromString(split[0]));
		
		if (world == null) return null;
		
		Location loc = new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3])); 
		
		return loc;
	}
	
	public static Location getBlockLocation(Location original)
	{
		Location loc = new Location(original.getWorld(), original.getBlockX(), original.getBlockY(), original.getBlockZ());
		
		loc.setPitch(original.getPitch());
		loc.setYaw(original.getYaw());
		
		return loc;
	}
	
	public static QuickWarp create(Location loc)
	{
		if (!validLocation(loc)) return null;
		
		QuickWarp existing = null;
		
		Location blockLoc = getBlockLocation(loc);
		
		String compressed = compressLocation(blockLoc);
		
		if (register.containsKey(compressed))
			existing = register.get(compressed);
		
		QuickWarp qw = (existing != null) ? existing : new QuickWarp(blockLoc);
		
		qw.save();
		
		register.put(compressed, qw);
		
		return qw;
	}
	
	public static QuickWarp createFromString(String compressedSignLocation)
	{
		if ((compressedSignLocation == null) || (compressedSignLocation.isEmpty())) return null;
		
		Location loc = extractLocation(compressedSignLocation);
		
		return validLocation(loc) ? new QuickWarp(loc) : null;
	}
	
	public static void removeQuickWarp(QuickWarp qw)
	{	
		if (register.containsKey(qw.compressedSignLocation))
		{
			register.remove(qw.compressedSignLocation);
			
			File file = new File(Main.instance.getDataFolder(), "//" + "quick_warps.yml");
			FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
			
			yaml.set(qw.compressedSignLocation, null);
			
			try
			{
				yaml.save(file);
			}
			catch (IOException e)
			{
				Main.message(null, "Failed to remove quick warp @: " + qw.compressedSignLocation);
			}
		}
	}
	
	public static QuickWarp getQuickWarp(Location loc)
	{	
		String compressedLocation = compressLocation(getBlockLocation(loc));
		
		return register.containsKey(compressedLocation) ? register.get(compressedLocation) : null;
	}
	
	public static boolean isSign(Material m)
	{
		switch(m)
		{
			case SIGN:
			case SIGN_POST:
			case WALL_SIGN:
				return true;
			default:
				return false;
		}
	}
	
	private Location signLocation, warpLocation;
	private String compressedSignLocation, compressedWarpLocation;
	private float pitch = 0, yaw = 0;
	
	public QuickWarp(Location signLocation)
	{
		this.signLocation = signLocation;
		this.warpLocation = null;
		compressedSignLocation = compressLocation(signLocation);
		compressedWarpLocation = "";
	}
	
	public QuickWarp(Location signLocation, Location warpLocation, float pitch, float yaw)
	{
		this.signLocation = signLocation;
		this.warpLocation = warpLocation;
		compressedSignLocation = compressLocation(signLocation);
		compressedWarpLocation = compressLocation(warpLocation);
		this.pitch = pitch;
		this.yaw = yaw;
	}
	
	private void save()
	{
		if ((signLocation == null) || (warpLocation == null)) return;
		
		File file = new File(Main.instance.getDataFolder(), "//" + "quick_warps.yml");
		FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		
		ConfigurationSection config;
		
		if (yaml.isConfigurationSection(compressedSignLocation))
			config = yaml.getConfigurationSection(compressedSignLocation);
		else
			config = yaml.createSection(compressedSignLocation);
		
		config.set("Destination", compressedWarpLocation);
		config.set("Pitch", pitch);
		config.set("Yaw", yaw);
		
		try
		{
			yaml.save(file);
		}
		catch (IOException e)
		{
			Main.message(null, "Failed to save quick warp @: " + compressedSignLocation);
		}
	}
	
	public void updateSign(String title)
	{
		Block b = signLocation.getWorld().getBlockAt(signLocation);
		
		Main.message(null, "updateSign()");
		
		Main.message(null, "null: "+(b != null));
		Main.message(null, "isSign: "+QuickWarp.isSign(b.getType()));
		
		if ((b != null) && QuickWarp.isSign(b.getType()))
		{
			Main.message(null, "writeSign");
			Sign s = (Sign)b.getState();
			s.setLine(0, title);
			s.update();
		}
	}
	
	public void setDestination(Location loc)
	{
		Main.message(null, "setDestination()");
		
		if (!validLocation(loc)) return;
		
		Location destination = getBlockLocation(loc);
		
		destination.setY(loc.getY());
		
		pitch = destination.getPitch();
		yaw = destination.getYaw();
		
		warpLocation = destination;
		compressedWarpLocation = compressLocation(warpLocation);
		
		updateSign("§5"+HelpText.SignHeader);
		
		save();
	}
	
	public boolean hasDestination()
	{
		return validLocation(warpLocation);
	}
	
	public void warpPlayer(Player p)
	{	
		if (!validLocation(this.warpLocation)) return;
		
		Location destination = new Location(this.warpLocation.getWorld(), this.warpLocation.getX() + 0.5, this.warpLocation.getY(), this.warpLocation.getZ() + 0.5);
		
		destination.setPitch(pitch);
		destination.setYaw(yaw);
		
		p.teleport(destination, TeleportCause.COMMAND);
		
		
		if (warpSound != null)
			p.playSound(p.getLocation(), warpSound, 100, 100);
	}
}
