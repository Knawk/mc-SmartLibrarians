package net.knawk.mc.smartlibrarians;

import com.google.common.collect.Lists;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Logger;

public class UpgradeLibrarianTradeListener implements Listener {
    private final Logger log;

    UpgradeLibrarianTradeListener(Logger log) {
        this.log = log;
    }

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        // Only modify librarians' enchanted book trades
        Villager villager = (Villager) event.getEntity();
        if (villager.getProfession() != Villager.Profession.LIBRARIAN) return;
        MerchantRecipe originalTrade = event.getRecipe();
        if (originalTrade.getResult().getType() != Material.ENCHANTED_BOOK) return;

        // Only modify if there's exactly one nearby framed item and it has compatible enchantments
        Location lecternLocation = Objects.requireNonNull(villager.getMemory(MemoryKey.JOB_SITE));
        Optional<ItemFrame> frame = getNearbyItemFrame(lecternLocation);
        if (frame.isEmpty()) return;
        List<Enchantment> candidateEnchants = compatibleEnchants(frame.get().getItem());
        if (candidateEnchants.isEmpty()) return;

        MerchantRecipe upgradedTrade = upgradeTrade(originalTrade, getRandomEnchant(candidateEnchants));
        event.setRecipe(upgradedTrade);
        spawnSuccessEffectCloud(frame.get());

        log.info(String.format("found %s, set trade %s", frame.get().getItem(), formatTrade(upgradedTrade)));
    }

    /**
     * @param location a block location
     * @return the sole item frame above {@code location}, or {@code Optional.empty()} if there are zero or multiple
     */
    private static Optional<ItemFrame> getNearbyItemFrame(final Location location) {
        World world = Objects.requireNonNull(location.getWorld());
        Location above = location.add(0, 1, 0);
        Collection<Entity> entities = world.getNearbyEntities(BoundingBox.of(above.getBlock()));
        List<ItemFrame> frames = entities
                .stream()
                .filter(entity -> entity.getType() == EntityType.ITEM_FRAME)
                .map(entity -> (ItemFrame) entity)
                .toList();

        // Must have exactly one frame
        return (frames.size() == 1) ? Optional.of(frames.get(0)) : Optional.empty();
    }

    /**
     * @param stack item stack
     * @return a list of enchantments compatible with {@code stack}
     */
    private static List<Enchantment> compatibleEnchants(final ItemStack stack) {
        return Arrays
                .stream(Enchantment.values())
                .filter(enchant -> !enchant.getKey().toString().equals("minecraft:soul_speed"))
                .filter(enchant -> enchant.canEnchantItem(stack))
                .toList();
    }

    /**
     * @param enchants list of enchantments
     * @return a random element of {@code enchants}
     */
    private static Enchantment getRandomEnchant(final List<Enchantment> enchants) {
        return enchants.get(new Random().nextInt(enchants.size()));
    }

    /**
     * @param referenceTrade a reference enchanted-book trade
     * @param enchant        enchantment to use
     * @return a max-level enchanted-book trade with the given enchantment
     */
    private static MerchantRecipe upgradeTrade(final MerchantRecipe referenceTrade, final Enchantment enchant) {
        ItemStack book = new ItemStack(referenceTrade.getResult());
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) Objects.requireNonNull(referenceTrade.getResult().getItemMeta()).clone();
        meta.getStoredEnchants().keySet().forEach(meta::removeStoredEnchant);
        meta.addStoredEnchant(enchant, enchant.getMaxLevel(), false);
        book.setItemMeta(meta);

        MerchantRecipe upgradedTrade = new MerchantRecipe(
                book,
                referenceTrade.getUses(),
                referenceTrade.getMaxUses(),
                referenceTrade.hasExperienceReward(),
                referenceTrade.getVillagerExperience(),
                referenceTrade.getPriceMultiplier());
        ItemStack emeralds = new ItemStack(Material.EMERALD, getRandomPrice(enchant));
        upgradedTrade.setIngredients(Lists.newArrayList(emeralds, new ItemStack(Material.BOOK)));
        return upgradedTrade;
    }

    /**
     * Returns a random trade price (in emeralds) for an enchanted book with the given enchantment at max level.
     * <p>
     * Formula taken from Minecraft 1.17 source code.
     *
     * @param enchant enchantment on the desired book
     * @return random trade price
     */
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

    private static void spawnSuccessEffectCloud(final ItemFrame frame) {
        World world = Objects.requireNonNull(frame.getWorld());
        Vector center = frame.getBoundingBox().getCenter();
        Location location = new Location(world, center.getX(), center.getY(), center.getZ());
        world.spawn(location, AreaEffectCloud.class, cloud -> {
            cloud.setDuration(20);
            cloud.setRadius(0.7f);
            cloud.setParticle(Particle.ENCHANTMENT_TABLE);
        });
    }
}
