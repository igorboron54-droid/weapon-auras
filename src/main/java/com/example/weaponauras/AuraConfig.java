package com.example.weaponauras;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Persistent user settings (config/weaponauras.json). */
public final class AuraConfig {
    public static boolean enabled = true, held = true, dropped = true;
    /** null = automatic (by enchantments); otherwise this aura is used on every enchanted weapon. */
    public static AuraType forced = null;

    private static final EnumMap<AuraType, Boolean> ON = new EnumMap<>(AuraType.class);
    private static final EnumMap<AuraType, Float> INTENSITY = new EnumMap<>(AuraType.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class Data {
        boolean enabled = true, held = true, dropped = true;
        String forced = null;
        Map<String, Boolean> on = new HashMap<>();
        Map<String, Float> intensity = new HashMap<>();
    }

    static { reset(); }

    public static boolean isOn(AuraType t) { return ON.getOrDefault(t, true); }
    public static void setOn(AuraType t, boolean v) { ON.put(t, v); }
    public static float intensity(AuraType t) { return INTENSITY.getOrDefault(t, 1f); }
    public static void setIntensity(AuraType t, float v) { INTENSITY.put(t, Math.max(0.25f, Math.min(2f, v))); }

    public static void cycleForced() {
        AuraType[] all = AuraType.values();
        if (forced == null) forced = all[0];
        else if (forced.ordinal() + 1 >= all.length) forced = null;
        else forced = all[forced.ordinal() + 1];
    }

    public static void reset() {
        enabled = held = dropped = true;
        forced = null;
        for (AuraType t : AuraType.values()) { ON.put(t, true); INTENSITY.put(t, 1f); }
    }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("weaponauras.json"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            try (Reader r = Files.newBufferedReader(f)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d == null) return;
                enabled = d.enabled; held = d.held; dropped = d.dropped;
                forced = null;
                if (d.forced != null) try { forced = AuraType.valueOf(d.forced); } catch (IllegalArgumentException ignored) {}
                for (AuraType t : AuraType.values()) {
                    if (d.on != null && d.on.containsKey(t.name())) ON.put(t, d.on.get(t.name()));
                    if (d.intensity != null && d.intensity.containsKey(t.name())) setIntensity(t, d.intensity.get(t.name()));
                }
            }
        } catch (Exception e) { System.err.println("[weaponauras] could not load config: " + e); }
    }

    public static void save() {
        try {
            Data d = new Data();
            d.enabled = enabled; d.held = held; d.dropped = dropped;
            d.forced = forced == null ? null : forced.name();
            for (AuraType t : AuraType.values()) { d.on.put(t.name(), isOn(t)); d.intensity.put(t.name(), intensity(t)); }
            try (Writer w = Files.newBufferedWriter(file())) { GSON.toJson(d, w); }
        } catch (Exception e) { System.err.println("[weaponauras] could not save config: " + e); }
    }
}
