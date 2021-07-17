package net.knawk.mc.smartlibrarians;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

public class UpgradeLibrarianTradeListener implements Listener {
    private final Logger log;

    UpgradeLibrarianTradeListener(Logger log) {
        this.log = log;
    }

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        MerchantRecipe originalTrade = event.getRecipe();
        AbstractVillager villager = event.getEntity();

        Optional<MerchantRecipe> upgradedTrade = upgradeTrade(originalTrade);
        if (upgradedTrade.isPresent()) {
            event.setRecipe(upgradedTrade.get());
            log.info(String.format("original: %s", formatTrade(originalTrade)));
            log.info(String.format("upgraded: %s", formatTrade(upgradedTrade.get())));
        }
    }

    /**
     * @param originalTrade a villager trade
     * @return a level-upgraded copy of the given trade if it's an enchanted book, or {@code Optional.empty()} otherwise.
     */
    private static Optional<MerchantRecipe> upgradeTrade(final MerchantRecipe originalTrade) {
        if (originalTrade.getResult().getType() != Material.ENCHANTED_BOOK) {
            return Optional.empty();
        }

        EnchantmentStorageMeta originalBookMeta = (EnchantmentStorageMeta) Objects.requireNonNull(originalTrade.getResult().getItemMeta());
        assert originalBookMeta.getStoredEnchants().size() == 1;
        Enchantment enchant = originalBookMeta.getStoredEnchants().keySet().iterator().next();

        EnchantmentStorageMeta upgradedBookMeta = originalBookMeta.clone();
        upgradedBookMeta.removeStoredEnchant(enchant);
        upgradedBookMeta.addStoredEnchant(enchant, enchant.getMaxLevel(), false);

        ItemStack upgradedBook = new ItemStack(originalTrade.getResult());
        upgradedBook.setItemMeta(upgradedBookMeta);

        MerchantRecipe upgradedTrade = new MerchantRecipe(
                upgradedBook,
                originalTrade.getUses(),
                originalTrade.getMaxUses(),
                originalTrade.hasExperienceReward(),
                originalTrade.getVillagerExperience(),
                originalTrade.getPriceMultiplier());
        final int upgradedPrice = getRandomPrice(enchant);
        upgradedTrade.setIngredients(Lists.newArrayList(new ItemStack(Material.EMERALD, upgradedPrice), new ItemStack(Material.BOOK)));

        return Optional.of(upgradedTrade);
    }

    private static int getRandomPrice(final Enchantment enchant) {
        final int level = enchant.getMaxLevel();
        Random random = new Random();
        int price = 2 + random.nextInt(5 + level * 10) + 3 * level;
        if (enchant.isTreasure()) {
            price *= 2;
        }
        if (price > 64) {
            price = 64;
        }
        return price;
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
