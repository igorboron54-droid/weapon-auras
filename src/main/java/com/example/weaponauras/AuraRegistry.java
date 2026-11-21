package com.example.weaponauras;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Maps enchantments -> aura types and caches the result per enchantment set. */
public final class AuraRegistry {
    private static final Map<String, AuraType> EXPLICIT = new HashMap<>();
    static {
        // Vanilla mapping. Edit freely.
        EXPLICIT.put("fire_aspect", AuraType.FIRE);
        EXPLICIT.put("flame", AuraType.FIRE);
        EXPLICIT.put("channeling", AuraType.LIGHTNING);
        EXPLICIT.put("knockback", AuraType.ICE);          // "arctic gust"
        EXPLICIT.put("punch", AuraType.ICE);
        EXPLICIT.put("bane_of_arthropods", AuraType.POISON);
        EXPLICIT.put("sharpness", AuraType.SHARPNESS);
        EXPLICIT.put("power", AuraType.SHARPNESS);
        EXPLICIT.put("sweeping_edge", AuraType.SHARPNESS);
        EXPLICIT.put("smite", AuraType.NETHER);
    }

    private static final Map<ItemEnchantments, AuraProfile> CACHE = new HashMap<>();

    public static AuraProfile get(ItemStack stack) {
        if (stack.isEmpty()) return AuraProfile.EMPTY;
        ItemEnchantments comp = stack.get(DataComponents.ENCHANTMENTS);
        if (comp == null || comp.isEmpty() || !isWeapon(stack)) return AuraProfile.EMPTY;
        if (AuraConfig.forced != null) return AuraProfile.single(AuraConfig.forced);
        if (CACHE.size() > 512) CACHE.clear();
        return CACHE.computeIfAbsent(comp, AuraRegistry::build);
    }

    /** Detected by item id so spears, modded swords/bows etc. work too. */
    private static boolean isWeapon(ItemStack stack) {
        String p = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return p.endsWith("sword") || p.endsWith("_axe") || p.endsWith("spear") || p.endsWith("bow")
                || p.endsWith("trident") || p.endsWith("mace");
    }

    private static AuraProfile build(ItemEnchantments comp) {
        EnumMap<AuraType, Float> m = new EnumMap<>(AuraType.class);
        for (Object2IntMap.Entry<Holder<Enchantment>> e : comp.entrySet()) {
            Holder<Enchantment> holder = e.getKey();
            String name = holder.getRegisteredName();           // e.g. "minecraft:fire_aspect"
            String path = name.substring(name.indexOf(':') + 1);
            AuraType t = classify(path);
            if (t == null) continue;
            int max = Math.max(1, holder.value().getMaxLevel());
            float intensity = 0.4f + 0.6f * Math.min(1f, e.getIntValue() / (float) max);
            m.merge(t, intensity, Math::max);
        }
        return m.isEmpty() ? AuraProfile.EMPTY : new AuraProfile(m);
    }

    /** Explicit vanilla map first, then keyword matching so modded enchants work too. */
    private static AuraType classify(String p) {
        AuraType t = EXPLICIT.get(p);
        if (t != null) return t;
        if (p.matches(".*(fire|flame|burn|ember|lava|inferno|blaze).*")) return AuraType.FIRE;
        if (p.matches(".*(lightning|thunder|shock|electr|storm).*")) return AuraType.LIGHTNING;
        if (p.matches(".*(frost|ice|freez|cold|chill|snow|glacia).*")) return AuraType.ICE;
        if (p.matches(".*(poison|venom|toxic|plague|acid).*")) return AuraType.POISON;
        if (p.matches(".*(sharp|keen|slash|cleav).*")) return AuraType.SHARPNESS;
        if (p.matches(".*(soul|nether|wither|hell|decay).*")) return AuraType.NETHER;
        return null;
    }
}
