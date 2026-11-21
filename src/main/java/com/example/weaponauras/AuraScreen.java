package com.example.weaponauras;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class AuraScreen extends Screen {
    private final Screen parent;

    public AuraScreen(Screen parent) {
        super(Component.literal("Weapon Auras"));
        this.parent = parent;
    }

    private static Component onOff(String label, boolean on) {
        return Component.literal(label + ": ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private Button toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        return Button.builder(onOff(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(onOff(label, get.getAsBoolean()));
        }).bounds(x, y, w, 20).build();
    }

    private static Component forcedText() {
        return AuraConfig.forced == null
                ? Component.literal("Aura choice: Auto (from enchantments)")
                : Component.literal("Aura choice: Force ")
                        .append(Component.literal(pretty(AuraConfig.forced)).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" on all enchanted weapons"));
    }

    private static String pretty(AuraType t) {
        return t.name().charAt(0) + t.name().substring(1).toLowerCase();
    }

    @Override
    protected void init() {
        int cx = width / 2;

        addRenderableWidget(new StringWidget(cx - 155, 10, 310, 12, title, font).alignCenter());

        addRenderableWidget(toggle(cx - 155, 28, 100, "Auras", () -> AuraConfig.enabled, v -> AuraConfig.enabled = v));
        addRenderableWidget(toggle(cx - 50, 28, 100, "Held", () -> AuraConfig.held, v -> AuraConfig.held = v));
        addRenderableWidget(toggle(cx + 55, 28, 100, "Dropped", () -> AuraConfig.dropped, v -> AuraConfig.dropped = v));

        addRenderableWidget(Button.builder(forcedText(), b -> {
            AuraConfig.cycleForced();
            b.setMessage(forcedText());
        }).bounds(cx - 155, 50, 310, 20).build());

        addRenderableWidget(new StringWidget(cx - 155, 63, 310, 10,
                Component.literal("Hold an enchanted weapon (F5 = 3rd person) to preview live"), font).alignCenter());

        int y = 76;
        for (AuraType t : AuraType.values()) {
            addRenderableWidget(toggle(cx - 155, y, 120, pretty(t), () -> AuraConfig.isOn(t), v -> AuraConfig.setOn(t, v)));
            addRenderableWidget(new IntensitySlider(cx - 30, y, 185, 20, t));
            y += 21;
        }

        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            AuraConfig.reset();
            minecraft.setScreen(new AuraScreen(parent));
        }).bounds(cx - 155, height - 26, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx + 5, height - 26, 150, 20).build());
    }

    @Override public boolean isPauseScreen() { return false; } // world keeps running so auras animate behind the menu
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void removed() { AuraConfig.save(); }

    private static class IntensitySlider extends AbstractSliderButton {
        private final AuraType type;

        IntensitySlider(int x, int y, int w, int h, AuraType type) {
            super(x, y, w, h, Component.empty(), (AuraConfig.intensity(type) - 0.25) / 1.75);
            this.type = type;
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.literal("Intensity: " + Math.round((0.25 + value * 1.75) * 100) + "%"));
        }

        @Override protected void applyValue() {
            AuraConfig.setIntensity(type, (float) (0.25 + value * 1.75));
        }
    }
}
