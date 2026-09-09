package com.chillpavz.dynamicnutrition.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import com.chillpavz.dynamicnutrition.Constants;

/**
 * The cloth-config data class, NeoForge copy.
 *
 * <p>It is duplicated per loader rather than shared because {@code common} cannot see cloth at all,
 * and it holds no logic: {@link ClothCompat} copies every value into {@link NutritionConfig}, which
 * is the only thing the rest of the mod reads.
 *
 * <p><b>Nothing outside {@link ClothCompat} may name this class.</b> It implements a cloth
 * interface, so merely loading it needs cloth on the classpath, and cloth is optional.
 */
@Config(name = Constants.MOD_ID)
public class ClothConfigData implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean hudEnabled = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = -120, max = 120)
    public int hudOffsetX = 0;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = -80, max = 40)
    public int hudOffsetY = 0;

    @ConfigEntry.Gui.Tooltip
    public boolean buttonEnabled = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = -160, max = 4)
    public int buttonOffsetX = 0;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = -4, max = 150)
    public int buttonOffsetY = 0;

    @ConfigEntry.Gui.Tooltip
    public boolean tooltipEnabled = true;

    @ConfigEntry.Gui.Tooltip
    public boolean varietyEnabled = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 32)
    public int varietyWindow = 8;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int varietyPenaltyPercent = 20;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int varietyFloorPercent = 30;

    @ConfigEntry.Gui.Tooltip
    public boolean effectsEnabled = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5)
    public int malnourishedHeartsLost = 1;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5)
    public int wellNourishedHeartsGained = 2;

    @ConfigEntry.Gui.Tooltip
    public boolean announceStatusChanges = true;
}
