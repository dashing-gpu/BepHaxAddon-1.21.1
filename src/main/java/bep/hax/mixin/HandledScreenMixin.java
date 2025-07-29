package bep.hax.mixin;

import bep.hax.modules.ShulkerOverviewModule;
import bep.hax.util.ShulkerDataParser;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        ShulkerOverviewModule module = Modules.get().get(ShulkerOverviewModule.class);
        if (module == null || !module.isActive()) return;

        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;

        // Check if it's a shulker box
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

        // Parse shulker contents using our utility
        Map<Item, Integer> itemCounts = ShulkerDataParser.parseShulkerContents(stack);
        if (itemCounts.isEmpty()) return;

        // Find most common item
        Map.Entry<Item, Integer> mostCommon = itemCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (mostCommon == null) return;

        Item item = mostCommon.getKey();
        int count = mostCommon.getValue();
        boolean hasMultiple = itemCounts.size() > 1;

        // Debug mode
        if (module.debugMode.get()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            String debug = String.format("Items: %d, Most: %s x%d",
                itemCounts.size(),
                item.getName().getString(),
                count);
            context.drawText(mc.textRenderer, debug, slot.x, slot.y - 10, 0xFFFFFF, true);
        }

        // Calculate render position based on setting
        int iconSize = module.iconSize.get();
        int iconX, iconY;
        switch (module.iconPosition.get()) {
            case BottomLeft -> {
                iconX = slot.x;
                iconY = slot.y + 16 - iconSize;
            }
            case TopRight -> {
                iconX = slot.x + 16 - iconSize;
                iconY = slot.y;
            }
            case TopLeft -> {
                iconX = slot.x;
                iconY = slot.y;
            }
            case Center -> {
                iconX = slot.x + (16 - iconSize) / 2;
                iconY = slot.y + (16 - iconSize) / 2;
            }
            default -> {
                iconX = slot.x + 16 - iconSize;
                iconY = slot.y + 16 - iconSize;
            }
        }

        // Render the item icon with proper depth
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200); // Lowered z to ensure under tooltip layer

        if (iconSize == 16) {
            context.drawItem(new ItemStack(item), iconX, iconY);
        } else {
            float scale = iconSize / 16.0f;
            context.getMatrices().translate(iconX, iconY, 0);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.drawItem(new ItemStack(item), 0, 0);
        }

        context.getMatrices().pop();

        // Draw multiple indicator
        if (hasMultiple && !module.multipleText.get().isEmpty()) {
            renderMultipleIndicator(context, slot.x, slot.y, module.multipleText.get(), module.multipleSize.get());
        }
    }

    private void renderMultipleIndicator(DrawContext context, int slotX, int slotY, String text, int size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Text textComponent = Text.literal(text);
        float defaultFontHeight = mc.textRenderer.fontHeight; // Typically 9
        float scale = (float) size / defaultFontHeight;

        int unscaledWidth = mc.textRenderer.getWidth(textComponent);
        int textX = slotX + 16 - Math.round(unscaledWidth * scale) - 1;
        int textY = slotY + 1;

        // Push matrices for proper depth and scaling
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300); // Lowered z to ensure under tooltip layer
        context.getMatrices().translate(textX, textY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        context.drawText(mc.textRenderer, textComponent, 0, 0, 0xFFFF00, false);

        context.getMatrices().pop();
    }
}
