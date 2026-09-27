package com.mekiks.opponentarmor;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * All user settings, persisted to {@code config/opponentarmor.properties}.
 * Unknown or broken values silently fall back to the defaults below.
 */
public final class ArmorConfig {

    /** How each piece's durability is written. */
    public enum Mode { PERCENT, DURABILITY, FULL }

    /** Rows stacked top-to-bottom, or cells side by side. */
    public enum Layout { VERTICAL, HORIZONTAL }

    /** Which screen corner the panel hugs; offsets are measured from it. */
    public enum Anchor { BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT }

    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("opponentarmor.properties");

    // General
    public static boolean enabled = true;
    public static Mode mode = Mode.PERCENT;
    public static int holdSeconds = 5;           // 1..30, how long after the last hit
    public static boolean showOnAim = false;     // also show while the crosshair is on a player
    public static boolean helmet = true;
    public static boolean chestplate = true;
    public static boolean leggings = true;
    public static boolean boots = true;
    public static boolean showHeader = true;     // head + nickname
    public static boolean showBars = true;

    // Look
    public static float scale = 1.0f;            // 0.5..2.5
    public static Layout layout = Layout.VERTICAL;
    public static Anchor anchor = Anchor.BOTTOM_RIGHT;
    public static int offsetX = 6;               // px from the anchor corner
    public static int offsetY = 6;
    public static int radius = 4;                // 0..8
    public static int bgOpacity = 55;            // 0..100 %
    public static boolean colorByDurability = true;
    public static boolean textShadow = true;

    private ArmorConfig() {
    }

    public static void reset() {
        enabled = true;
        mode = Mode.PERCENT;
        holdSeconds = 5;
        showOnAim = false;
        helmet = chestplate = leggings = boots = true;
        showHeader = true;
        showBars = true;
        scale = 1.0f;
        layout = Layout.VERTICAL;
        anchor = Anchor.BOTTOM_RIGHT;
        offsetX = 6;
        offsetY = 6;
        radius = 4;
        bgOpacity = 55;
        colorByDurability = true;
        textShadow = true;
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(PATH)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        enabled = bool(p, "enabled", enabled);
        mode = enumOf(Mode.class, p.getProperty("mode"), mode);
        holdSeconds = clamp(integer(p, "holdSeconds", holdSeconds), 1, 30);
        showOnAim = bool(p, "showOnAim", showOnAim);
        helmet = bool(p, "helmet", helmet);
        chestplate = bool(p, "chestplate", chestplate);
        leggings = bool(p, "leggings", leggings);
        boots = bool(p, "boots", boots);
        showHeader = bool(p, "showHeader", showHeader);
        showBars = bool(p, "showBars", showBars);
        scale = Math.max(0.5f, Math.min(2.5f, floatOf(p, "scale", scale)));
        layout = enumOf(Layout.class, p.getProperty("layout"), layout);
        anchor = enumOf(Anchor.class, p.getProperty("anchor"), anchor);
        offsetX = clamp(integer(p, "offsetX", offsetX), 0, 10000);
        offsetY = clamp(integer(p, "offsetY", offsetY), 0, 10000);
        radius = clamp(integer(p, "radius", radius), 0, 8);
        bgOpacity = clamp(integer(p, "bgOpacity", bgOpacity), 0, 100);
        colorByDurability = bool(p, "colorByDurability", colorByDurability);
        textShadow = bool(p, "textShadow", textShadow);
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", String.valueOf(enabled));
        p.setProperty("mode", mode.name());
        p.setProperty("holdSeconds", String.valueOf(holdSeconds));
        p.setProperty("showOnAim", String.valueOf(showOnAim));
        p.setProperty("helmet", String.valueOf(helmet));
        p.setProperty("chestplate", String.valueOf(chestplate));
        p.setProperty("leggings", String.valueOf(leggings));
        p.setProperty("boots", String.valueOf(boots));
        p.setProperty("showHeader", String.valueOf(showHeader));
        p.setProperty("showBars", String.valueOf(showBars));
        p.setProperty("scale", String.valueOf(scale));
        p.setProperty("layout", layout.name());
        p.setProperty("anchor", anchor.name());
        p.setProperty("offsetX", String.valueOf(offsetX));
        p.setProperty("offsetY", String.valueOf(offsetY));
        p.setProperty("radius", String.valueOf(radius));
        p.setProperty("bgOpacity", String.valueOf(bgOpacity));
        p.setProperty("colorByDurability", String.valueOf(colorByDurability));
        p.setProperty("textShadow", String.valueOf(textShadow));
        try (OutputStream out = Files.newOutputStream(PATH)) {
            p.store(out, "OpponentArmor settings");
        } catch (IOException ignored) {
        }
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int integer(Properties p, String key, int def) {
        try {
            return Integer.parseInt(p.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float floatOf(Properties p, String key, float def) {
        try {
            return Float.parseFloat(p.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String value, E def) {
        if (value == null) {
            return def;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
