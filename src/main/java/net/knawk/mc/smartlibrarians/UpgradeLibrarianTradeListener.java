package net.knawk.mc.smartlibrarians;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Map;
import java.util.logging.Logger;

public class UpgradeLibrarianTradeListener implements Listener {
    private final Logger log;

    UpgradeLibrarianTradeListener(Logger log) {
        this.log = log;
    }

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        MerchantRecipe trade = event.getRecipe();
        AbstractVillager villager = event.getEntity();
        log.info(String.format("villager %s at %s acquired trade %s",
                villager.getUniqueId(),
                villager.getLocation(),
                formatTrade(trade)));

    }

    private static String formatTrade(final MerchantRecipe trade) {
        StringBuilder builder = new StringBuilder(formatItemStack(trade.getIngredients().get(0)));
        trade.getIngredients().stream().skip(1).forEach(stack -> {
            builder.append(" + ");
            builder.append(formatItemStack(stack));
        });
        builder.append(" => ");
        builder.append(formatItemStack(trade.getResult()));
        return builder.toString();
    }

    private static String formatItemStack(final ItemStack stack) {
        StringBuilder builder = new StringBuilder();
        if (stack.getAmount() > 1) {
            builder.append(stack.getAmount());
            builder.append('*');
        }
        builder.append(stack.getType().name());
        if (stack.getItemMeta() instanceof EnchantmentStorageMeta) {
            Map<Enchantment, Integer> enchants = ((EnchantmentStorageMeta) stack.getItemMeta()).getStoredEnchants();
            enchants.forEach(((enchantment, level) -> {
                builder.append('+');
                builder.append(enchantment.getName());
                builder.append('@');
                builder.append(level);
            }));
        }
        return builder.toString();
    }
}
