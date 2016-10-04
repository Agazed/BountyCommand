package net.agazed.bounty;

import java.util.Arrays;
import java.util.List;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Bounty extends JavaPlugin implements Listener {

	private static Economy econ;

	public void onEnable() {
		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(this, this);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("bounty")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Command can only be run as a player!");
				return true;
			}
			Player player = (Player) sender;

			// Help

			if ((args.length == 0) || (args[0].equalsIgnoreCase("help"))) {
				player.sendMessage("----- " + ChatColor.DARK_AQUA + ChatColor.BOLD + "Bounty Help" + ChatColor.WHITE + " -----");
				player.sendMessage(ChatColor.DARK_AQUA + "/bounty " + ChatColor.GRAY + "help " + ChatColor.WHITE + "- Displays this page");
				player.sendMessage(ChatColor.DARK_AQUA + "/bounty " + ChatColor.GRAY + "place <player> <amount> " + ChatColor.WHITE + "- Place a bounty on a player");
				player.sendMessage(ChatColor.DARK_AQUA + "/bounty " + ChatColor.GRAY + "remove <player> " + ChatColor.WHITE + "- Remove a bounty on a player");
				player.sendMessage(ChatColor.DARK_AQUA + "/bounty " + ChatColor.GRAY + "list " + ChatColor.WHITE + "- View players with bounties");
				return true;
			}

			// Place

			if (args[0].equalsIgnoreCase("place")) {
				if (args.length != 3) {
					player.sendMessage(ChatColor.RED + "Usage: /bounty place <player> <amount>");
					return true;
				}
				int amount;
				try {
					amount = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					player.sendMessage(ChatColor.RED + "Invalid amount!");
					return true;
				}
				int minamount = getConfig().getInt("min-amount");
				if (amount < minamount) {
					player.sendMessage(ChatColor.RED + "Minimum amount is $" + Integer.toString(minamount) + "!");
					return true;
				}
				if (amount > econ.getBalance(player)) {
					sender.sendMessage(ChatColor.RED + "You don't have enough money!");
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
				getConfig().set("bounties." + target.getName() + ".placer", player.getName());
				getConfig().set("bounties." + target.getName() + ".amount", amount);
				saveConfig();
				List<String> bountyplaced = getConfig().getStringList("bounty-placed");
				for (String command : bountyplaced) {
					getServer().dispatchCommand(getServer().getConsoleSender(),
							command.replace("%PLACED_PLAYER%", target.getName()).replace("%PLACER_PLAYER%", player.getName()).replace("%AMOUNT%", Integer.toString(amount)));
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
				if (getConfig().getString("bounties." + target.getName() + ".placer").equalsIgnoreCase(player.getName())) {
					List<String> bountyremoved = getConfig().getStringList("bounty-removed");
					for (String command : bountyremoved) {
						getServer().dispatchCommand(
								getServer().getConsoleSender(),
								command.replace("%PLACED_PLAYER%", args[1]).replace("%PLACER_PLAYER%", player.getName())
										.replace("%AMOUNT%", Integer.toString(getConfig().getInt("bounties." + target.getName() + ".amount"))));
					}
					getConfig().set("bounties." + target.getName(), null);
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Successfully removed bounty on " + args[1]);
					return true;
				}
			}

			// List

			if (args[0].equalsIgnoreCase("list")) {
				if (args.length == 1) {
					int number = 0;
					Inventory inv = getServer().createInventory(null, 54, "Bounties");
					for (String bountied : getConfig().getConfigurationSection("bounties").getKeys(false)) {
						if (number >= 54) {
							break;
						}
						ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
						SkullMeta meta = (SkullMeta) head.getItemMeta();
						meta.setOwner(bountied);
						ChatColor isOnline = getServer().getPlayerExact(bountied) != null ? ChatColor.GREEN : ChatColor.RED;
						meta.setDisplayName(isOnline + bountied);
						meta.setLore(Arrays.asList(ChatColor.GRAY + "$" + Integer.toString(getConfig().getInt("bounties." + bountied + ".amount"))));
						head.setItemMeta(meta);
						inv.setItem(number, head);
						number++;
					}
					player.openInventory(inv);
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
								.replace("%PLACER_PLAYER%", getConfig().getString("bounties." + target.getName() + ".placer"))
								.replace("%AMOUNT%", Integer.toString(getConfig().getInt("bounties." + target.getName() + ".amount"))));
			}
			getConfig().set("bounties." + target.getName(), null);
			saveConfig();
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getName().equalsIgnoreCase("Bounties")) {
			event.setCancelled(true);
		}
	}
}
