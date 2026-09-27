package com.mekiks.opponentarmor;

import com.mekiks.opponentarmor.SettingsWidgets.ArmorPicker;
import com.mekiks.opponentarmor.SettingsWidgets.Cycle;
import com.mekiks.opponentarmor.SettingsWidgets.FlatButton;
import com.mekiks.opponentarmor.SettingsWidgets.Slider;
import com.mekiks.opponentarmor.SettingsWidgets.Toggle;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Two-column settings screen with a live preview of the panel drawn where it
 * will actually sit on the HUD. Settings are saved when the screen closes.
 */
public class OpponentArmorScreen extends Screen {

    private static final int COL_W = 176;
    private static final int COL_GAP = 12;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 2;
    private static final int ROWS = 8;
    private static final int CARD = 0xB0101318;

    private final @Nullable Screen parent;
    private final ItemStack[] previewArmor;
    private final ItemStack[] pickerIcons;

    private int left;
    private int top;
    private int rowsBottom;

    private boolean dragging;
    private double grabX;
    private double grabY;

    public OpponentArmorScreen(@Nullable Screen parent) {
        super(Text.translatable("opponentarmor.title"));
        this.parent = parent;
        this.previewArmor = new ItemStack[]{
                damaged(new ItemStack(Items.DIAMOND_HELMET), 0.18f),
                damaged(new ItemStack(Items.NETHERITE_CHESTPLATE), 0.46f),
                damaged(new ItemStack(Items.DIAMOND_LEGGINGS), 0.73f),
                damaged(new ItemStack(Items.DIAMOND_BOOTS), 0.92f)
        };
        this.pickerIcons = new ItemStack[]{
                new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)
        };
    }

    private static ItemStack damaged(ItemStack stack, float wornFraction) {
        stack.setDamage(Math.round(stack.getMaxDamage() * wornFraction));
        return stack;
    }

    private static Text opt(String key) {
        return Text.translatable("opponentarmor.option." + key);
    }

    private static Text enumText(String group, Enum<?> value) {
        return Text.translatable("opponentarmor." + group + "." + value.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void init() {
        int total = COL_W * 2 + COL_GAP;
        left = Math.max(8, (width - total) / 2);
        top = 52;
        rowsBottom = top + ROWS * (ROW_H + ROW_GAP) - ROW_GAP;
        int right = left + COL_W + COL_GAP;

        // ---- General ----
        int y = top;
        addDrawableChild(new Toggle(left, y, COL_W, ROW_H, opt("enabled"),
                () -> ArmorConfig.enabled, v -> ArmorConfig.enabled = v));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Cycle<>(left, y, COL_W, ROW_H, opt("mode"), ArmorConfig.Mode.values(),
                () -> ArmorConfig.mode, v -> ArmorConfig.mode = v, v -> enumText("mode", v)));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Slider(left, y, COL_W, ROW_H, opt("hold"), 1, 30, 1,
                () -> ArmorConfig.holdSeconds, v -> ArmorConfig.holdSeconds = (int) v,
                v -> Text.translatable("opponentarmor.unit.seconds", (int) v).getString()));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new ArmorPicker(left, y, COL_W, ROW_H, opt("armor"), pickerIcons,
                new BooleanSupplier[]{
                        () -> ArmorConfig.helmet, () -> ArmorConfig.chestplate,
                        () -> ArmorConfig.leggings, () -> ArmorConfig.boots},
                v -> ArmorConfig.helmet = v, v -> ArmorConfig.chestplate = v,
                v -> ArmorConfig.leggings = v, v -> ArmorConfig.boots = v));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Toggle(left, y, COL_W, ROW_H, opt("header"),
                () -> ArmorConfig.showHeader, v -> ArmorConfig.showHeader = v));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Toggle(left, y, COL_W, ROW_H, opt("bars"),
                () -> ArmorConfig.showBars, v -> ArmorConfig.showBars = v));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Toggle(left, y, COL_W, ROW_H, opt("color"),
                () -> ArmorConfig.colorByDurability, v -> ArmorConfig.colorByDurability = v));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Toggle(left, y, COL_W, ROW_H, opt("aim"),
                () -> ArmorConfig.showOnAim, v -> ArmorConfig.showOnAim = v));

        // ---- Look ----
        y = top;
        addDrawableChild(new Slider(right, y, COL_W, ROW_H, opt("scale"), 0.5, 2.5, 0.05,
                () -> ArmorConfig.scale, v -> ArmorConfig.scale = (float) v,
                v -> String.format(Locale.ROOT, "%.2fx", v)));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Cycle<>(right, y, COL_W, ROW_H, opt("layout"), ArmorConfig.Layout.values(),
                () -> ArmorConfig.layout, v -> ArmorConfig.layout = v, v -> enumText("layout", v)));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Cycle<>(right, y, COL_W, ROW_H, opt("anchor"), ArmorConfig.Anchor.values(),
                () -> ArmorConfig.anchor, v -> ArmorConfig.anchor = v, v -> enumText("anchor", v)));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Slider(right, y, COL_W, ROW_H, opt("offset_x"), 0, width, 1,
                () -> ArmorConfig.offsetX, v -> ArmorConfig.offsetX = (int) v, v -> (int) v + " px"));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Slider(right, y, COL_W, ROW_H, opt("offset_y"), 0, height, 1,
                () -> ArmorConfig.offsetY, v -> ArmorConfig.offsetY = (int) v, v -> (int) v + " px"));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Slider(right, y, COL_W, ROW_H, opt("radius"), 0, 8, 1,
                () -> ArmorConfig.radius, v -> ArmorConfig.radius = (int) v, v -> String.valueOf((int) v)));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Slider(right, y, COL_W, ROW_H, opt("background"), 0, 100, 5,
                () -> ArmorConfig.bgOpacity, v -> ArmorConfig.bgOpacity = (int) v, v -> (int) v + "%"));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new Toggle(right, y, COL_W, ROW_H, opt("shadow"),
                () -> ArmorConfig.textShadow, v -> ArmorConfig.textShadow = v));

        // ---- Buttons ----
        int by = Math.max(rowsBottom + 12, height - 28);
        int bw = 100;
        addDrawableChild(new FlatButton(width / 2 - bw - 4, by, bw, 18,
                Text.translatable("opponentarmor.button.reset"), false, ArmorConfig::reset));
        addDrawableChild(new FlatButton(width / 2 + 4, by, bw, 18, ScreenTexts.DONE, true, this::close));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        Text hint = Text.translatable("opponentarmor.hint.drag");
        ctx.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, 22, SettingsWidgets.MUTED, false);

        int right = left + COL_W + COL_GAP;
        card(ctx, left, Text.translatable("opponentarmor.section.general"));
        card(ctx, right, Text.translatable("opponentarmor.section.look"));

        super.render(ctx, mouseX, mouseY, delta);

        // Live preview at the real HUD position; it can be dragged anywhere.
        String name = client.player != null ? client.player.getName().getString() : client.getSession().getUsername();
        SkinTextures skin = client.player != null ? client.player.getSkin() : DefaultSkinHelper.getSteve();
        ArmorHudRenderer.render(ctx, textRenderer, name, skin, previewArmor);

        boolean over = overPreview(mouseX, mouseY);
        if (dragging || over) {
            outline(ctx, ArmorHudRenderer.lastX - 2, ArmorHudRenderer.lastY - 2,
                    ArmorHudRenderer.lastW + 4, ArmorHudRenderer.lastH + 4,
                    dragging ? SettingsWidgets.ACCENT : 0x996C8CFF);
            ctx.setCursor(StandardCursors.RESIZE_ALL);
        }
    }

    private static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static boolean overPreview(double mx, double my) {
        return mx >= ArmorHudRenderer.lastX && mx < ArmorHudRenderer.lastX + ArmorHudRenderer.lastW
                && my >= ArmorHudRenderer.lastY && my < ArmorHudRenderer.lastY + ArmorHudRenderer.lastH;
    }

    // ---- dragging the preview ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        // The preview sits on top of everything, so it gets the click first.
        if (click.button() == 0 && overPreview(click.x(), click.y())) {
            dragging = true;
            grabX = click.x() - ArmorHudRenderer.lastX;
            grabY = click.y() - ArmorHudRenderer.lastY;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging) {
            moveTo(click.x() - grabX, click.y() - grabY);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging && click.button() == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    /**
     * Places the panel's top-left at (x, y), then re-expresses that as the nearest
     * corner plus offsets, so the panel keeps its spot when the window is resized.
     */
    private void moveTo(double x, double y) {
        int w = ArmorHudRenderer.lastW;
        int h = ArmorHudRenderer.lastH;
        int nx = (int) Math.round(Math.max(0, Math.min(x, width - w)));
        int ny = (int) Math.round(Math.max(0, Math.min(y, height - h)));
        boolean rightSide = nx + w / 2 >= width / 2;
        boolean bottom = ny + h / 2 >= height / 2;
        ArmorConfig.anchor = bottom
                ? (rightSide ? ArmorConfig.Anchor.BOTTOM_RIGHT : ArmorConfig.Anchor.BOTTOM_LEFT)
                : (rightSide ? ArmorConfig.Anchor.TOP_RIGHT : ArmorConfig.Anchor.TOP_LEFT);
        ArmorConfig.offsetX = rightSide ? width - nx - w : nx;
        ArmorConfig.offsetY = bottom ? height - ny - h : ny;
    }

    private void card(DrawContext ctx, int x, Text heading) {
        Gfx.roundRect(ctx, x - 4, top - 18, COL_W + 8, rowsBottom - top + 22, 6, CARD);
        ctx.drawText(textRenderer, heading, x + 6, top - 12, SettingsWidgets.MUTED, false);
    }

    @Override
    public void removed() {
        ArmorConfig.save();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
