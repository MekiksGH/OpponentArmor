package com.mekiks.opponentarmor;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the opponent-armor panel. Everything is laid out in unscaled pixels with
 * the origin at the panel's top-left; the configured scale and corner position
 * are applied through the GUI matrix stack.
 */
public final class ArmorHudRenderer {

    private static final int PAD = 5;
    private static final int ICON = 16;
    private static final int TEXT_GAP = 4;       // icon -> text (vertical layout)
    private static final int ROW_GAP = 2;
    private static final int CELL_GAP = 4;       // between cells (horizontal layout)
    private static final int BAR_H = 2;
    private static final int MIN_BAR_W = 30;
    private static final int HEAD = 8;
    private static final int HEADER_GAP = 5;     // header -> first row
    private static final int NAME_MAX_W = 100;

    private static final int BG_RGB = 0x0E0F13;
    private static final int TEXT = 0xFFF2F3F5;
    private static final int MUTED = 0xFF9AA0AA;
    private static final int TRACK = 0x33FFFFFF;
    private static final int DIVIDER = 0x1FFFFFFF;

    // Durability gradient stops: empty -> full.
    private static final int RED = 0xFFFF5C5C;
    private static final int ORANGE = 0xFFFFA64D;
    private static final int YELLOW = 0xFFFFE066;
    private static final int GREEN = 0xFF6EE07A;

    /** One armor piece prepared for drawing. */
    private record Entry(ItemStack stack, String label, int color, float fraction, boolean breakable) {
    }

    private ArmorHudRenderer() {
    }

    /**
     * Draws the panel in its configured corner.
     *
     * @param armor head, chest, legs, feet — in that order; empty stacks are allowed
     */
    public static void render(DrawContext ctx, TextRenderer tr, String name, @Nullable SkinTextures skin,
                              ItemStack[] armor) {
        List<Entry> entries = entries(tr, armor);
        boolean header = ArmorConfig.showHeader;
        String shownName = tr.trimToWidth(name, NAME_MAX_W);
        Text empty = Text.translatable("opponentarmor.hud.no_armor");

        boolean vertical = ArmorConfig.layout == ArmorConfig.Layout.VERTICAL;
        int textW = 0;
        for (Entry e : entries) {
            textW = Math.max(textW, tr.getWidth(e.label()));
        }

        // ---- measure ----
        int contentW;
        int contentH;
        int cellW = 0;
        if (entries.isEmpty()) {
            contentW = tr.getWidth(empty);
            contentH = tr.fontHeight - 1;
        } else if (vertical) {
            int colW = Math.max(textW, ArmorConfig.showBars ? MIN_BAR_W : 0);
            contentW = ICON + TEXT_GAP + colW;
            contentH = entries.size() * ICON + (entries.size() - 1) * ROW_GAP;
        } else {
            cellW = Math.max(ICON + 2, Math.max(textW, ArmorConfig.showBars ? 18 : 0));
            contentW = entries.size() * cellW + (entries.size() - 1) * CELL_GAP;
            contentH = ICON + 2 + (tr.fontHeight - 1) + (ArmorConfig.showBars ? 2 + BAR_H : 0);
        }
        int headerW = header ? HEAD + 4 + tr.getWidth(shownName) : 0;
        int headerH = header ? HEAD + HEADER_GAP : 0;
        int innerW = Math.max(contentW, headerW);
        int w = PAD * 2 + innerW;
        int h = PAD * 2 + headerH + contentH;

        // ---- place ----
        float s = ArmorConfig.scale;
        int sw = ctx.getScaledWindowWidth();
        int shh = ctx.getScaledWindowHeight();
        float px = switch (ArmorConfig.anchor) {
            case BOTTOM_RIGHT, TOP_RIGHT -> sw - ArmorConfig.offsetX - w * s;
            case BOTTOM_LEFT, TOP_LEFT -> ArmorConfig.offsetX;
        };
        float py = switch (ArmorConfig.anchor) {
            case BOTTOM_RIGHT, BOTTOM_LEFT -> shh - ArmorConfig.offsetY - h * s;
            case TOP_RIGHT, TOP_LEFT -> ArmorConfig.offsetY;
        };

        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(Math.round(px), Math.round(py));
        m.scale(s, s);

        int alpha = Math.round(ArmorConfig.bgOpacity * 2.55f);
        if (alpha > 0) {
            Gfx.roundRect(ctx, 0, 0, w, h, ArmorConfig.radius, Gfx.withAlpha(BG_RGB, alpha));
        }

        boolean shadow = ArmorConfig.textShadow;
        int y = PAD;
        if (header) {
            if (skin != null) {
                PlayerSkinDrawer.draw(ctx, skin, PAD, y, HEAD);
            }
            ctx.drawText(tr, shownName, PAD + HEAD + 4, y, TEXT, shadow);
            ctx.fill(PAD, y + HEAD + 2, w - PAD, y + HEAD + 3, DIVIDER);
            y += headerH;
        }

        if (entries.isEmpty()) {
            ctx.drawText(tr, empty, PAD, y, MUTED, shadow);
        } else if (vertical) {
            int colW = contentW - ICON - TEXT_GAP;
            int tx = PAD + ICON + TEXT_GAP;
            for (Entry e : entries) {
                ctx.drawItem(e.stack(), PAD, y);
                if (ArmorConfig.showBars && e.breakable()) {
                    ctx.drawText(tr, e.label(), tx, y + 1, e.color(), shadow);
                    bar(ctx, tx, y + 12, colW, e);
                } else {
                    ctx.drawText(tr, e.label(), tx, y + 4, e.color(), shadow);
                }
                y += ICON + ROW_GAP;
            }
        } else {
            int x = PAD + (innerW - contentW) / 2;
            for (Entry e : entries) {
                ctx.drawItem(e.stack(), x + (cellW - ICON) / 2, y);
                int ty = y + ICON + 2;
                ctx.drawText(tr, e.label(), x + (cellW - tr.getWidth(e.label())) / 2, ty, e.color(), shadow);
                if (ArmorConfig.showBars && e.breakable()) {
                    bar(ctx, x + 1, ty + tr.fontHeight + 1, cellW - 2, e);
                }
                x += cellW + CELL_GAP;
            }
        }

        m.popMatrix();
    }

    private static void bar(DrawContext ctx, int x, int y, int width, Entry e) {
        ctx.fill(x, y, x + width, y + BAR_H, TRACK);
        int filled = Math.round(width * e.fraction());
        if (filled > 0) {
            ctx.fill(x, y, x + filled, y + BAR_H, e.color());
        }
    }

    private static List<Entry> entries(TextRenderer tr, ItemStack[] armor) {
        boolean[] wanted = {ArmorConfig.helmet, ArmorConfig.chestplate, ArmorConfig.leggings, ArmorConfig.boots};
        List<Entry> out = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            ItemStack stack = armor[i];
            if (!wanted[i] || stack == null || stack.isEmpty()) {
                continue;
            }
            if (!stack.isDamageable() || stack.getMaxDamage() <= 0) {
                out.add(new Entry(stack, "—", MUTED, 1f, false));
                continue;
            }
            int max = stack.getMaxDamage();
            int left = Math.max(0, max - stack.getDamage());
            float f = left / (float) max;
            out.add(new Entry(stack, label(left, max, f), color(f), f, true));
        }
        return out;
    }

    private static String label(int left, int max, float f) {
        return switch (ArmorConfig.mode) {
            case PERCENT -> {
                int pct = Math.round(f * 100);
                if (pct == 0 && left > 0) {
                    pct = 1;                 // never claim 0% while the piece still holds
                }
                yield pct + "%";
            }
            case DURABILITY -> String.valueOf(left);
            case FULL -> left + "/" + max;
        };
    }

    private static int color(float f) {
        if (!ArmorConfig.colorByDurability) {
            return TEXT;
        }
        if (f < 0.3f) {
            return Gfx.lerpColor(RED, ORANGE, f / 0.3f);
        }
        if (f < 0.6f) {
            return Gfx.lerpColor(ORANGE, YELLOW, (f - 0.3f) / 0.3f);
        }
        return Gfx.lerpColor(YELLOW, GREEN, (f - 0.6f) / 0.4f);
    }
}
