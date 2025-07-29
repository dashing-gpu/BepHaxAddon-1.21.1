package bep.hax.hud;

import bep.hax.Bep;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import java.util.List;

public class BlockCounterHud extends HudElement {
    public static final HudElementInfo<BlockCounterHud> INFO = new HudElementInfo<>(Bep.HUD_GROUP, "BlockCounterHud", "Displays selected blocks and their inventory counts.", BlockCounterHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Blocks to display in the HUD.")
        .build()
    );

    private final Setting<Boolean> showTitle = sgGeneral.add(new BoolSetting.Builder()
        .name("show-title")
        .description("Display the HUD title.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> itemScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("item-scale")
        .description("Scale of the item icons.")
        .defaultValue(1.0)
        .min(0.1)
        .sliderRange(0.1, 3.0)
        .build()
    );

    private final Setting<Double> textScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Scale of the count text.")
        .defaultValue(1.0)
        .min(0.1)
        .sliderRange(0.1, 3.0)
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the count text.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Boolean> textShadow = sgGeneral.add(new BoolSetting.Builder()
        .name("text-shadow")
        .description("Render shadow behind the text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showZero = sgGeneral.add(new BoolSetting.Builder()
        .name("show-zero")
        .description("Show blocks with zero count.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Layout> layout = sgGeneral.add(new EnumSetting.Builder<Layout>()
        .name("layout")
        .description("Layout of the displayed blocks.")
        .defaultValue(Layout.Vertical)
        .build()
    );

    public enum Layout {
        Vertical,
        Horizontal
    }

    public BlockCounterHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        double curX = this.x;
        double curY = this.y;
        double width = 0;
        double height = 0;

        if (isInEditor()) {
            renderer.text("Block Counter", curX, curY, textColor.get(), textShadow.get(), textScale.get());
            setSize(renderer.textWidth("Block Counter", textShadow.get(), textScale.get()), renderer.textHeight(textShadow.get(), textScale.get()));
            return;
        }

        if (showTitle.get()) {
            renderer.text("Block Counter", curX, curY, textColor.get(), textShadow.get(), textScale.get());
            curY += renderer.textHeight(textShadow.get(), textScale.get()) + 2;
            height += renderer.textHeight(textShadow.get(), textScale.get()) + 2;
        }

        double itemSize = 16 * itemScale.get();
        double textHeight = renderer.textHeight(textShadow.get(), textScale.get());
        double spacing = 2;

        for (Block block : blocks.get()) {
            ItemStack stack = new ItemStack(block.asItem());
            int count = InvUtils.find(stack.getItem()).count();

            if (count == 0 && !showZero.get()) continue;

            renderer.item(stack, (int) curX, (int) curY, itemScale.get().floatValue(), true);
            double textX = curX + itemSize + spacing;
            double textY = curY + (itemSize / 2 - textHeight / 2);
            renderer.text(String.valueOf(count), textX, textY, textColor.get(), textShadow.get(), textScale.get());

            if (layout.get() == Layout.Horizontal) {
                curX += itemSize + spacing + renderer.textWidth(String.valueOf(count), textShadow.get(), textScale.get()) + spacing;
                width += itemSize + spacing + renderer.textWidth(String.valueOf(count), textShadow.get(), textScale.get()) + spacing;
                height = Math.max(height, itemSize);
            } else {
                curY += itemSize + spacing;
                height += itemSize + spacing;
                width = Math.max(width, itemSize + spacing + renderer.textWidth(String.valueOf(count), textShadow.get(), textScale.get()));
            }
        }

        setSize(width, height);
    }
}
