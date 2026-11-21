package com.example.weaponauras;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class AuraEmitter {
    private static final int MAX_PARTICLES_PER_TICK = 48; // global lag cap
    private static int budget;
    private static double quality = 1.0;

    // NOTE: if the build says DustParticleOptions wants a different constructor, adjust these 4 lines.
    private static final DustParticleOptions ICE_DUST = new DustParticleOptions(0x8CD9FF, 0.7f);
    private static final DustParticleOptions POISON_DUST = new DustParticleOptions(0x59E633, 0.8f);
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(0xF21A1F, 0.75f);
    private static final DustParticleOptions NETHER_DUST = new DustParticleOptions(0x731ABF, 0.8f);

    public static void beginTick(Minecraft mc) {
        budget = MAX_PARTICLES_PER_TICK;
        quality = switch (mc.options.particles().get()) {
            case ALL -> 1.0;
            case DECREASED -> 0.5;
            default -> 0.2;
        };
    }

    private static void add(ClientLevel w, ParticleOptions e, double x, double y, double z, double vx, double vy, double vz) {
        if (budget <= 0) return;
        budget--;
        w.addParticle(e, x, y, z, vx, vy, vz);
    }

    /**
     * @param base   start of the "blade" (hand / ground)
     * @param dir    unit direction along the weapon
     * @param len    weapon length in blocks
     * @param rate   overall rate multiplier
     * @param spread random sideways jitter around the blade
     */
    public static void emit(ClientLevel w, AuraProfile profile, Vec3 base, Vec3 dir, double len, double rate, double spread) {
        RandomSource r = w.random;
        int n = 0;
        for (Map.Entry<AuraType, Float> e : profile.entries()) if (AuraConfig.isOn(e.getKey())) n++;
        if (n == 0) return;

        Vec3 helper = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = dir.cross(helper).normalize();
        Vec3 v = dir.cross(u).normalize();
        double share = 1.15 / Math.sqrt(n);

        int k = 0;
        for (Map.Entry<AuraType, Float> en : profile.entries()) {
            AuraType type = en.getKey();
            if (!AuraConfig.isOn(type)) continue;
            float i = en.getValue() * AuraConfig.intensity(type);
            double lo = Math.max(0, (double) k / n - 0.15), hi = Math.min(1, (double) (k + 1) / n + 0.15);
            k++;

            double expected = type.density * (0.5 + 0.8 * i) * rate * share * quality;
            int count = (int) expected + (r.nextDouble() < expected - (int) expected ? 1 : 0);
            for (int c = 0; c < count; c++) {
                double t = lo + (hi - lo) * r.nextDouble();
                double sx = r.nextGaussian() * spread, sy = r.nextGaussian() * spread * 0.5, sz = r.nextGaussian() * spread;
                Vec3 p = base.add(dir.scale(len * t)).add(sx, sy, sz);
                spawn(w, r, type, i, p, u, v, t);
            }
        }
    }

    private static void spawn(ClientLevel w, RandomSource r, AuraType type, float i, Vec3 p, Vec3 u, Vec3 v, double t) {
        double jx = r.nextGaussian() * 0.01, jz = r.nextGaussian() * 0.01;
        switch (type) {
            case FIRE -> {
                add(w, ParticleTypes.FLAME, p.x, p.y, p.z, jx, 0.01 + 0.02 * i, jz);
                if (r.nextFloat() < 0.25f) add(w, ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, jx * 2, 0.02, jz * 2);
                if (r.nextFloat() < 0.08f + 0.15f * i) add(w, ParticleTypes.LAVA, p.x, p.y, p.z, 0, 0, 0);
                if (r.nextFloat() < 0.07f) add(w, ParticleTypes.SMOKE, p.x, p.y + 0.1, p.z, 0, 0.03, 0);
            }
            case LIGHTNING -> {
                add(w, ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, r.nextGaussian() * 0.15, r.nextGaussian() * 0.15, r.nextGaussian() * 0.15);
                if (r.nextFloat() < 0.05f + 0.07f * i) {
                    double x = p.x, y = p.y, z = p.z;
                    for (int s = 0; s < 5; s++) {
                        add(w, ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
                        x += r.nextGaussian() * 0.1; y += r.nextGaussian() * 0.1; z += r.nextGaussian() * 0.1;
                    }
                }
                if (r.nextFloat() < 0.03f * i) add(w, ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0, 0);
            }
            case ICE -> {
                add(w, ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, jx, -0.02, jz);
                if (r.nextFloat() < 0.6f) add(w, ICE_DUST, p.x, p.y, p.z, jx, -0.005, jz);
                if (r.nextFloat() < 0.10f * i) add(w, ParticleTypes.CLOUD, p.x, p.y - 0.05, p.z, 0, -0.003, 0);
            }
            case POISON -> {
                double a = w.getGameTime() * 0.35 + t * 7.0, rad = 0.16;
                double cx = Math.cos(a), sn = Math.sin(a);
                double ox = (u.x * cx + v.x * sn) * rad, oy = (u.y * cx + v.y * sn) * rad, oz = (u.z * cx + v.z * sn) * rad;
                double tx = (-u.x * sn + v.x * cx) * 0.03, ty = (-u.y * sn + v.y * cx) * 0.03, tz = (-u.z * sn + v.z * cx) * 0.03;
                add(w, POISON_DUST, p.x + ox, p.y + oy, p.z + oz, tx, ty + 0.01, tz);
                if (r.nextFloat() < 0.25f) add(w, ParticleTypes.BUBBLE_POP, p.x + ox, p.y + oy, p.z + oz, 0, 0.02, 0);
                if (r.nextFloat() < 0.12f) add(w, ParticleTypes.ITEM_SLIME, p.x, p.y, p.z, 0, 0, 0);
                if (r.nextFloat() < 0.08f) add(w, ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 0, 0.01, 0);
            }
            case SHARPNESS -> {
                add(w, RED_DUST, p.x, p.y, p.z, jx, 0.01, jz);
                if (r.nextFloat() < 0.2f * i) add(w, ParticleTypes.CRIT, p.x, p.y, p.z, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08);
            }
            case NETHER -> {
                add(w, ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, jx, 0.012, jz);
                if (r.nextFloat() < 0.3f) add(w, NETHER_DUST, p.x, p.y, p.z, jx, 0.01, jz);
                if (r.nextFloat() < 0.15f + 0.1f * i) add(w, ParticleTypes.SOUL, p.x, p.y, p.z, 0, 0.02, 0);
                if (r.nextFloat() < 0.4f) add(w, ParticleTypes.PORTAL, p.x, p.y, p.z, r.nextGaussian() * 0.4, r.nextGaussian() * 0.4, r.nextGaussian() * 0.4);
            }
        }
    }
}
