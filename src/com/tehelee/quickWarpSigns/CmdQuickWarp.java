package com.tehelee.quickWarpSigns;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

public class CmdQuickWarp implements CommandExecutor
{	
	public static void playerLeave(Player p)
	{
		if (QuickWarp.inProgress.containsKey(p)) QuickWarp.inProgress.remove(p);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (args.length > 0)
		{
			Player p = null;
			if (sender instanceof Player) p = (Player)sender;
			
			if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?"))
			{
				commandHelp(sender, label);
			}
			else if (args[0].equalsIgnoreCase("reload"))
			{
				if ((p == null) || p.hasPermission(HelpText.PermissionPrefix + "reload"))
				{
					Reload.reload(p);
				}
				else
				{
					Main.message(p, HelpText.MissingPermission);
				}
			}
			else if (args[0].equalsIgnoreCase("set") && (p != null))
			{
				if (p.hasPermission(HelpText.PermissionPrefix + "create"))
				{
					if (QuickWarp.inProgress.containsKey(p))
					{
						QuickWarp qw = QuickWarp.inProgress.get(p);
						
						qw.setDestination(p.getLocation());
						
						Main.message(p, HelpText.LinkQuickWarp);
					}
					else
					{
						Main.message(p, HelpText.NeedQuickWarp);
					}
				}
				else
				{
					Main.message(p, HelpText.MissingPermission);
				}
			}
			else if (args[0].equalsIgnoreCase("edit") && (p != null))
			{
				if (p.hasPermission(HelpText.PermissionPrefix + "create"))
				{
					Block b = getPlayerBlockTarget(p, false);
					
					QuickWarp qw = null;
					
					if ((b != null) && (QuickWarp.isSign(b.getType()))) qw = QuickWarp.getQuickWarp(b.getLocation());
					
					if (qw != null)
					{
						QuickWarp.inProgress.put(p, qw);
						
						qw.updateSign("§4"+HelpText.SignHeader);
						
						Main.message(p, HelpText.EditQuickWarp);
					}
					else
					{
						Main.message(p, HelpText.LookToEdit);
					}
				}
				else
				{
					Main.message(p, HelpText.MissingPermission);
				}
			}
			else if (args[0].equalsIgnoreCase("cancel") && (p != null))
			{
				if (p.hasPermission(HelpText.PermissionPrefix + "create"))
				{
					if (QuickWarp.inProgress.containsKey(p))
					{
						QuickWarp qw = QuickWarp.inProgress.get(p);
						
						qw.updateSign(qw.hasDestination() ? "§5"+HelpText.SignHeader : "");
						
						QuickWarp.inProgress.remove(p);
						Main.message(p, HelpText.CancelEdit);
					}
				}
				else
				{
					Main.message(p, HelpText.MissingPermission);
				}	
			}
			else
			{
				commandHelp(sender, label);
			}
		}
		else
		{
			commandHelp(sender, label);
		}
		
		return true;
	}
	
	private static void commandHelp(CommandSender sender, String label)
	{	
		List<String> perms = new ArrayList<String>();
		
		if (sender.hasPermission(HelpText.PermissionPrefix + "reload")) perms.add("reload");
		if (sender.hasPermission(HelpText.PermissionPrefix + "create"))
		{
			perms.add("set");
			perms.add("edit");
			perms.add("cancel");
		}
		
		if (perms.size() == 0)
		{
			Main.message(sender, HelpText.MissingPermission);
			return;
		}
		
		String cmds = perms.get(0);
		
		for (int i = 1; i < perms.size(); i++)
		{
			cmds += " | " + perms.get(i);
		}
		
		if (perms.size() > 1)
		{
			cmds = "[" + cmds + "]";
		}
		
		Main.message(sender, "/" + label + " " + cmds);
	}
	
	public Block getPlayerBlockTarget(Player player, boolean getLastAirBlock)
	{
		BlockIterator iter = new BlockIterator(player, 10);
		
		Block lastBlock = null, nextBlock = null;
		
		while (iter.hasNext())
		{
			lastBlock = nextBlock;
			nextBlock = iter.next();
			
			if (nextBlock.getType() != Material.AIR)
			{
				if (getLastAirBlock)
				{
					if (lastBlock == null)
					{
						return player.getLocation().getBlock();
					}
					else
					{
						return lastBlock;
					}
				}
				else
				{
					return nextBlock;
				}
			}
		}
		
		return null;
	}
}
