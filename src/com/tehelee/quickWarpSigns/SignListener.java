package com.tehelee.quickWarpSigns;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener
{
	@EventHandler
	public void onSignChange(SignChangeEvent e)
	{	
		Player p = e.getPlayer();
		
		if ((p != null) && (p.hasPermission(HelpText.PermissionPrefix+"create")))
		{	
			String header = ChatColor.stripColor(e.getLine(0));
			
			if (header.equalsIgnoreCase(HelpText.SignHeader) || header.equalsIgnoreCase("[qw]") || header.equalsIgnoreCase("[QWarp]") || header.equalsIgnoreCase("[QuickWarp]"))
			{
				Block block = e.getBlock();
				Location location = block.getLocation();
				QuickWarp qw = QuickWarp.create(location);
				
				if (!qw.hasDestination())
				{
					e.setLine(0, "§4" + HelpText.SignHeader);
					
					QuickWarp.inProgress.put(p, qw);
					
					Main.message(p, HelpText.BeginQuickWarp);
				}
			}
		}
	}
	
	@EventHandler
	public void onSignBreak(BlockBreakEvent e)
	{
		Block b = e.getBlock();
		if (QuickWarp.isSign(b.getType()))
		{
			QuickWarp qw = QuickWarp.getQuickWarp(b.getLocation());
			
			if (qw != null)
			{
				if (e.getPlayer().hasPermission(HelpText.PermissionPrefix+"create"))
				{
					QuickWarp.removeQuickWarp(qw);
				}
				else
				{
					e.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onSignClick(PlayerInteractEvent e)
	{
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		
		Block b = e.getClickedBlock();
		if (QuickWarp.isSign(b.getType()))
		{
			QuickWarp qw = QuickWarp.getQuickWarp(b.getLocation());
			
			if (qw != null)
			{
				Player p = e.getPlayer();
				if (p.hasPermission(HelpText.PermissionPrefix+"create") && p.isSneaking())
				{
					if (QuickWarp.inProgress.containsKey(p))
					{
						QuickWarp.inProgress.remove(p);
						Main.message(p, HelpText.CancelEdit);
					}
					else
					{
						QuickWarp.inProgress.put(p, qw);
						Main.message(p, HelpText.EditQuickWarp);
						Main.message(p, HelpText.SneakCancelEdit);
					}
				}
				else
				{
					qw.warpPlayer(p);
				}
				
				e.setCancelled(true);
			}
		}
	}
}
