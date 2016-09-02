package net.agazed.bounty;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Bounty extends JavaPlugin implements Listener {

	public void onEnable() {
		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(this, this);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("bounty")) {
			if (!(sender instanceof Player)) {
				getServer().getConsoleSender().sendMessage(ChatColor.RED + "Command can only be run as a player!");
				return true;
			}
			Player player = (Player) sender;

			// Help

			if ((args.length == 0) || (args[0].equalsIgnoreCase("help"))) {
				player.sendMessage("----- " + ChatColor.DARK_AQUA + ChatColor.BOLD + "Bounty Help" + ChatColor.WHITE + " -----");
				player.sendMessage(ChatColor.DARK_AQUA + "/bounty " + ChatColor.GRAY + "help " + ChatColor.WHITE + "- Displays this page");
				player.sendMessage(ChatColor.DARK_AQUA + "/bounty " + ChatColor.GRAY + "place <player> " + ChatColor.WHITE + "- Place a bounty on a player");
				player.sendMessage(ChatColor.DARK_AQUA + "/bounty " + ChatColor.GRAY + "remove <player> " + ChatColor.WHITE + "- Remove a bounty on a player");
			}

			// Place

			if (args[0].equalsIgnoreCase("place")) {
				if (args.length != 2) {
					player.sendMessage(ChatColor.RED + "Usage: /bounty place <player>");
					return true;
				}
				Player target = getServer().getPlayerExact(args[1]);
				if (target == null) {
					sender.sendMessage(ChatColor.RED + "Player is offline or does not exist!");
					return true;
				}
				if (player == target) {
					sender.sendMessage(ChatColor.RED + "You cannot set a bounty on yourself!");
					return true;
				}
				String bounty = getConfig().getString("bounties." + target.getName());
				if (bounty != null) {
					player.sendMessage(ChatColor.RED + "There is already a bounty placed on this player!");
					return true;
				}
				getConfig().set("bounties." + target.getName(), player.getName());
				saveConfig();
				List<String> bountyplaced = getConfig().getStringList("bounty-placed");
				for (String command : bountyplaced) {
					getServer().dispatchCommand(getServer().getConsoleSender(), command.replace("%PLACED_PLAYER%", target.getName()).replace("%PLACER_PLAYER%", player.getName()));
				}
				player.sendMessage(ChatColor.GREEN + "Successfully placed bounty on " + target.getName());
				return true;
			}

			// Remove

			if (args[0].equalsIgnoreCase("remove")) {
				if (args.length != 2) {
					player.sendMessage(ChatColor.RED + "Usage: /bounty remove <player>");
					return true;
				}
				Player target = getServer().getPlayerExact(args[1]);
				if (target == null) {
					sender.sendMessage(ChatColor.RED + "Player is offline or does not exist!");
					return true;
				}
				if (getConfig().getString("bounties." + target.getName()).equalsIgnoreCase(player.getName())) {
					List<String> bountyremoved = getConfig().getStringList("bounty-removed");
					for (String command : bountyremoved) {
						getServer().dispatchCommand(getServer().getConsoleSender(), command.replace("%PLACED_PLAYER%", args[1]).replace("%PLACER_PLAYER%", player.getName()));
					}
					getConfig().set("bounties." + target.getName(), null);
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Successfully removed bounty on " + args[1]);
					return true;
				}
			}

			// Reload

			if (args[0].equalsIgnoreCase("reload")) {
				if (args.length == 1) {
					if (!sender.hasPermission("bounty.reload")) {
						sender.sendMessage(ChatColor.RED + "No permission!");
						return true;
					}
					reloadConfig();
					player.sendMessage(ChatColor.GREEN + "Successfully reloaded config!");
					return true;
				}

				if (!player.hasPermission("bounty.use")) {
					player.sendMessage(ChatColor.RED + "No permission!");
					return true;
				}
			}
			player.sendMessage(ChatColor.RED + "Unknown argument!");
			return true;
		}
		return true;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player target = event.getEntity();
		if (((target.getKiller() instanceof Player)) && (getConfig().getString("bounties." + target.getName()) != null)) {
			List<String> bountyplayerkilled = getConfig().getStringList("bounty-player-killed");
			for (String command : bountyplayerkilled) {
				getServer().dispatchCommand(
						getServer().getConsoleSender(),
						command.replace("%KILLER%", target.getKiller().getName()).replace("%PLACED_PLAYER%", target.getName())
								.replace("%PLACER_PLAYER%", getConfig().getString("bounties." + target.getName())));
			}
			getConfig().set("bounties." + target.getName(), null);
			saveConfig();
		}
	}
}
