package bep.hax.mixin;

import java.util.Arrays;
import java.util.stream.Collectors;

import net.minecraft.block.entity.SignBlockEntity;  // remove if unused
import net.minecraft.block.entity.SignText;         // remove if unused

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;

import bep.hax.modules.Loadouts;
import bep.hax.util.StardustUtil;
import meteordevelopment.meteorclient.systems.modules.Modules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas]
 */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin {
    // shadow the recipe‐book widget & layout fields from InventoryScreen
    @Shadow private RecipeBookWidget<PlayerScreenHandler> recipeBookWidget;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    @Unique private Loadouts loadouts;
    @Unique private ButtonWidget saveLoadoutButton;
    @Unique private ButtonWidget loadLoadoutButton;

    // Called at the end of InventoryScreen.init(...)
    @Inject(method = "init(Lnet/minecraft/client/util/math/MatrixStack;II)V", at = @At("TAIL"))
    private void onInit(MatrixStack matrices, int mouseX, int mouseY, CallbackInfo ci) {
        var mods = Modules.get();
        if (mods == null) return;
        loadouts = mods.get(Loadouts.class);
        if (loadouts == null || !loadouts.quickLoadout.get()) return;

        // position buttons underneath the inventory
        int btnW = 42, btnH = 16;
        int saveX = x + backgroundWidth/2 - btnW - 2;
        int loadX = x + backgroundWidth/2 + 2;
        int btnY = y + backgroundHeight - btnH - 5;

        saveLoadoutButton = addDrawableChild(
            ButtonWidget.builder(Text.of(StardustUtil.rCC() + "§o✨§fSave"), b -> {
                    loadouts.saveLoadout("quicksave");
                    b.setMessage(Text.of(StardustUtil.rCC() + "§o✨§fSave"));
                })
                .dimensions(saveX, btnY, btnW, btnH)
                .tooltip(Tooltip.of(Text.of("§7§oSave your current inventory to Loadouts.")))
                .build()
        );
        loadLoadoutButton = addDrawableChild(
            ButtonWidget.builder(Text.of("Load" + StardustUtil.rCC() + "§o✨"), b -> {
                    loadouts.loadLoadout("quicksave");
                    b.setMessage(Text.of("Load" + StardustUtil.rCC() + "§o✨"));
                })
                .dimensions(loadX, btnY, btnW, btnH)
                .tooltip(Tooltip.of(Text.of("§7§oLoad your quicksave loadout.")))
                .build()
        );

        saveLoadoutButton.visible = loadouts.isActive();
        loadLoadoutButton.visible = loadouts.isActive();
    }

    // Called each frame at the end of InventoryScreen.render(...)
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", at = @At("TAIL"))
    private void onRender(MatrixStack ms, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (loadouts == null) return;
        if (!loadouts.quickLoadout.get()) return;
        saveLoadoutButton.visible = loadouts.isActive();
        loadLoadoutButton.visible = loadouts.isActive();
    }

    // Called each tick
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (loadouts == null) return;
        if (!loadouts.quickLoadout.get()) return;
        if (loadouts.isActive() && saveLoadoutButton != null) {
            saveLoadoutButton.setMessage(Text.of(StardustUtil.rCC() + "§o✨§fSave"));
            loadLoadoutButton.setMessage(Text.of("Load" + StardustUtil.rCC() + "§o✨"));
        }
    }
}
