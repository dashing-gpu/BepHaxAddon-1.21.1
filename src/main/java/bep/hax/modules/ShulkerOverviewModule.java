package bep.hax.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ShulkerOverviewModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> iconSize = sgGeneral.add(new IntSetting.Builder()
        .name("icon-size")
        .description("Size of the item icon overlay.")
        .defaultValue(12)
        .min(4)
        .max(16)
        .sliderMin(4)
        .sliderMax(16)
        .build()
    );

    public final Setting<IconPosition> iconPosition = sgGeneral.add(new EnumSetting.Builder<IconPosition>()
        .name("icon-position")
        .description("Position of the item icon overlay.")
        .defaultValue(IconPosition.Center)
        .build()
    );

    public final Setting<String> multipleText = sgGeneral.add(new StringSetting.Builder()
        .name("multiple-indicator")
        .description("Text to show when shulker contains multiple item types.")
        .defaultValue("+")
        .build()
    );

    public final Setting<Integer> multipleSize = sgGeneral.add(new IntSetting.Builder()
        .name("multiple-size")
        .description("Size of the multiple indicator text.")
        .defaultValue(8)
        .min(4)
        .max(16)
        .sliderMin(4)
        .sliderMax(16)
        .build()
    );

    public final Setting<Boolean> debugMode = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-mode")
        .description("Show debug information.")
        .defaultValue(false)
        .build()
    );

    public ShulkerOverviewModule() {
        super(Categories.Render, "shulker-overview", "Overlays most common item icon on shulker boxes in inventory.");
    }

    public enum IconPosition {
        BottomRight,
        BottomLeft,
        TopRight,
        TopLeft,
        Center
    }
}
