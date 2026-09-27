// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Mekiks

package com.mekiks.opponentarmor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/** Flat, rounded settings controls used by {@link OpponentArmorScreen}. */
public final class SettingsWidgets {

    static final int ACCENT = 0xFF6C8CFF;
    static final int TEXT = 0xFFE8EAF0;
    static final int MUTED = 0xFF8C92A0;
    static final int TRACK = 0xFF2E323C;
    static final int HOVER = 0x18FFFFFF;

    private SettingsWidgets() {
    }

    private static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    /** A full-width row: label on the left, control drawn by the subclass on the right. */
    abstract static class Row extends ClickableWidget {

        Row(int x, int y, int w, int h, Text label) {
            super(x, y, w, h, label);
        }

        int centerY() {
            return getY() + getHeight() / 2;
        }

        int right() {
            return getX() + getWidth() - 6;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            if (isHovered()) {
                Gfx.roundRect(ctx, getX(), getY(), getWidth(), getHeight(), 4, HOVER);
            }
            ctx.drawText(font(), getMessage(), getX() + 6, centerY() - 4, TEXT, false);
            renderControl(ctx);
        }

        abstract void renderControl(DrawContext ctx);

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    /** On/off pill switch. */
    static final class Toggle extends Row {
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;

        Toggle(int x, int y, int w, int h, Text label, BooleanSupplier get, Consumer<Boolean> set) {
            super(x, y, w, h, label);
            this.get = get;
            this.set = set;
        }

        @Override
        void renderControl(DrawContext ctx) {
            boolean on = get.getAsBoolean();
            int pw = 20, ph = 10;
            int px = right() - pw, py = centerY() - ph / 2;
            Gfx.roundRect(ctx, px, py, pw, ph, 5, on ? ACCENT : TRACK);
            int k = ph - 4;
            int kx = on ? px + pw - 2 - k : px + 2;
            Gfx.roundRect(ctx, kx, py + 2, k, k, 3, on ? 0xFFFFFFFF : 0xFFA8ADB8);
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            set.accept(!get.getAsBoolean());
        }
    }

    /** Cycles through enum values: left click forward, right click back. */
    static final class Cycle<E extends Enum<E>> extends Row {
        private final E[] values;
        private final Supplier<E> get;
        private final Consumer<E> set;
        private final Function<E, Text> name;

        Cycle(int x, int y, int w, int h, Text label, E[] values, Supplier<E> get, Consumer<E> set,
              Function<E, Text> name) {
            super(x, y, w, h, label);
            this.values = values;
            this.get = get;
            this.set = set;
            this.name = name;
        }

        @Override
        void renderControl(DrawContext ctx) {
            TextRenderer tr = font();
            Text value = name.apply(get.get());
            int vw = tr.getWidth(value);
            int y = centerY() - 4;
            int x = right() - tr.getWidth("›");
            ctx.drawText(tr, "›", x, y, MUTED, false);
            x -= 4 + vw;
            ctx.drawText(tr, value, x, y, isHovered() ? 0xFFFFFFFF : TEXT, false);
            x -= 4 + tr.getWidth("‹");
            ctx.drawText(tr, "‹", x, y, MUTED, false);
        }

        @Override
        protected boolean isValidClickButton(MouseInput input) {
            return input.button() == 0 || input.button() == 1;
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            int i = get.get().ordinal() + (click.button() == 1 ? -1 : 1);
            set.accept(values[Math.floorMod(i, values.length)]);
        }
    }

    /** Draggable slider with a value readout; mouse wheel nudges by one step. */
    static final class Slider extends Row {
        private static final int TRACK_W = 60;

        private final double min, max, step;
        private final DoubleSupplier get;
        private final DoubleConsumer set;
        private final DoubleFunction<String> format;
        private boolean dragging;

        Slider(int x, int y, int w, int h, Text label, double min, double max, double step,
               DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> format) {
            super(x, y, w, h, label);
            this.min = min;
            this.max = max;
            this.step = step;
            this.get = get;
            this.set = set;
            this.format = format;
        }

        private int trackX() {
            return right() - TRACK_W;
        }

        @Override
        void renderControl(DrawContext ctx) {
            TextRenderer tr = font();
            double v = get.getAsDouble();
            float f = (float) ((v - min) / (max - min));
            int tx = trackX(), ty = centerY() - 1;
            Gfx.roundRect(ctx, tx, ty, TRACK_W, 3, 1, TRACK);
            int kx = tx + Math.round(f * (TRACK_W - 4));
            Gfx.roundRect(ctx, tx, ty, kx - tx + 2, 3, 1, ACCENT);
            Gfx.roundRect(ctx, kx, centerY() - 5, 4, 10, 2, dragging || isHovered() ? 0xFFFFFFFF : 0xFFD8DBE2);
            String text = format.apply(v);
            ctx.drawText(tr, text, tx - 6 - tr.getWidth(text), centerY() - 4, MUTED, false);
        }

        private void setFromMouse(double mouseX) {
            double f = (mouseX - trackX() - 2) / (TRACK_W - 4);
            apply(min + Math.max(0, Math.min(1, f)) * (max - min));
        }

        private void apply(double raw) {
            double snapped = Math.round((raw - min) / step) * step + min;
            set.accept(Math.max(min, Math.min(max, snapped)));
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            if (click.x() >= trackX() - 6) {
                dragging = true;
                setFromMouse(click.x());
            }
        }

        @Override
        protected void onDrag(Click click, double offsetX, double offsetY) {
            if (dragging) {
                setFromMouse(click.x());
            }
        }

        @Override
        public void onRelease(Click click) {
            dragging = false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
            if (vertical == 0) {
                return false;
            }
            apply(get.getAsDouble() + Math.signum(vertical) * step);
            return true;
        }
    }

    /** Four clickable armor icons, one per slot; highlighted ones are shown on the HUD. */
    static final class ArmorPicker extends Row {
        private static final int BOX = 18, GAP = 2;

        private final ItemStack[] icons;
        private final BooleanSupplier[] get;
        private final Consumer<Boolean>[] set;

        @SafeVarargs
        ArmorPicker(int x, int y, int w, int h, Text label, ItemStack[] icons, BooleanSupplier[] get,
                    Consumer<Boolean>... set) {
            super(x, y, w, h, label);
            this.icons = icons;
            this.get = get;
            this.set = set;
        }

        private int boxX(int i) {
            return right() - 4 * BOX - 3 * GAP + i * (BOX + GAP);
        }

        @Override
        void renderControl(DrawContext ctx) {
            int by = centerY() - BOX / 2;
            for (int i = 0; i < 4; i++) {
                boolean on = get[i].getAsBoolean();
                int bx = boxX(i);
                Gfx.roundRect(ctx, bx, by, BOX, BOX, 4, on ? 0x556C8CFF : 0x14FFFFFF);
                if (on) {
                    ctx.fill(bx + 4, by + BOX - 1, bx + BOX - 4, by + BOX, ACCENT);
                }
                ctx.drawItem(icons[i], bx + 1, by + 1);
            }
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            for (int i = 0; i < 4; i++) {
                int bx = boxX(i);
                if (click.x() >= bx && click.x() < bx + BOX) {
                    set[i].accept(!get[i].getAsBoolean());
                    return;
                }
            }
        }
    }

    /** Rounded push button; {@code primary} ones use the accent color. */
    static final class FlatButton extends ClickableWidget {
        private final Runnable action;
        private final boolean primary;

        FlatButton(int x, int y, int w, int h, Text label, boolean primary, Runnable action) {
            super(x, y, w, h, label);
            this.action = action;
            this.primary = primary;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int bg = primary
                    ? (isHovered() ? 0xFF819DFF : ACCENT)
                    : (isHovered() ? 0xFF3A3F4B : 0xFF272A32);
            Gfx.roundRect(ctx, getX(), getY(), getWidth(), getHeight(), 5, bg);
            TextRenderer tr = font();
            int tw = tr.getWidth(getMessage());
            ctx.drawText(tr, getMessage(), getX() + (getWidth() - tw) / 2, getY() + (getHeight() - 8) / 2,
                    0xFFFFFFFF, false);
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            action.run();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
}
