package bep.hax.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import bep.hax.Bep;
import bep.hax.BlackOutModule;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.render.FogShape;

/**
 * @author OLEPOSSU
 */

public class Fog extends BlackOutModule {
    public Fog() {
        super(Bep.BLACKOUT, "Fog", "Customizable fog.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<FogShape> shape = sgGeneral.add(new EnumSetting.Builder<FogShape>()
        .name("Shape")
        .description("Fog shape.")
        .defaultValue(FogShape.SPHERE)
        .build()
    );
    public final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("Distance")
        .description("How far away should the fog start rendering.")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 100)
        .build()
    );
    public final Setting<Integer> fading = sgGeneral.add(new IntSetting.Builder()
        .name("Fading")
        .description("How smoothly should the fog fade.")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 1000)
        .build()
    );
    public final Setting<Double> thickness = sgGeneral.add(new DoubleSetting.Builder()
        .name("Thickness")
        .description(".")
        .defaultValue(10)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );
    public final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("Color")
        .description("Color of the fog.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    public net.minecraft.client.render.Fog getFogParams() {
        float red = color.get().r / 255f;
        float green = color.get().g / 255f;
        float blue = color.get().b / 255f;
        double denom = (100 - thickness.get()) * 2.55;
        float density = color.get().a / (float) denom;

        float start = distance.get().floatValue();
        double endCalc = distance.get() + fading.get();
        float end = (float) endCalc;

        return new net.minecraft.client.render.Fog(start, end, shape.get(), red, green, blue, density);
    }
}
