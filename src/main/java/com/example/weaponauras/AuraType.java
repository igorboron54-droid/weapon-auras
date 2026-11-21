package com.example.weaponauras;

public enum AuraType {
    FIRE(2.2), LIGHTNING(1.3), ICE(1.7), POISON(1.7), SHARPNESS(1.4), NETHER(1.9);

    /** Base particles per tick (before intensity / quality scaling). */
    public final double density;
    AuraType(double density) { this.density = density; }
}
