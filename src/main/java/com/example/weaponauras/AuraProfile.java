package com.example.weaponauras;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Which auras a weapon has, and how intense each is (0.4 .. 1.0). */
public final class AuraProfile {
    public static final AuraProfile EMPTY = new AuraProfile(new EnumMap<>(AuraType.class));
    private static final EnumMap<AuraType, AuraProfile> SINGLES = new EnumMap<>(AuraType.class);
    static {
        for (AuraType t : AuraType.values()) {
            EnumMap<AuraType, Float> m = new EnumMap<>(AuraType.class);
            m.put(t, 1.0f);
            SINGLES.put(t, new AuraProfile(m));
        }
    }
    public static AuraProfile single(AuraType t) { return SINGLES.get(t); }

    private final EnumMap<AuraType, Float> map;

    public AuraProfile(EnumMap<AuraType, Float> map) { this.map = map; }
    public boolean isEmpty() { return map.isEmpty(); }
    public int size() { return map.size(); }
    public Set<Map.Entry<AuraType, Float>> entries() { return map.entrySet(); }
}
