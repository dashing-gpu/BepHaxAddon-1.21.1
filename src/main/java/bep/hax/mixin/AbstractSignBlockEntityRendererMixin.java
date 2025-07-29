package bep.hax.mixin;

import java.util.Arrays;
import java.util.stream.Collectors;

import bep.hax.modules.AntiToS;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.HangingSignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ SignBlockEntityRenderer.class, HangingSignBlockEntityRenderer.class })
abstract class SignRendererMixin {

    /**
     * Swap out any blacklisted text before the sign is drawn.
     */
    @ModifyVariable(
        method    = "renderText",
        at        = @At("HEAD"),
        argsOnly  = true
    )
    private SignText modifyRenderedText(SignText original) {
        Modules mods = Modules.get();
        if (mods == null) return original;

        AntiToS anti = mods.get(AntiToS.class);
        if (!anti.isActive()) return original;

        String joined = Arrays.stream(original.getMessages(false))
            .map(Text::getString)
            .collect(Collectors.joining(" "))
            .trim();

        return anti.containsBlacklistedText(joined)
            ? anti.familyFriendlySignText(original)
            : original;
    }

    /**
     * Cancel the entire sign render if configured (NoRender or AntiToS NoRender mode).
     */
    @Inject(
        method      = "render(Lnet/minecraft/block/entity/SignBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at          = @At("HEAD"),
        cancellable = true
    )
    private void onRender(
        SignBlockEntity            sign,
        float                      delta,
        MatrixStack                matrices,
        VertexConsumerProvider     vcp,
        int                        light,
        CallbackInfo               ci
    ) {
        Modules mods = Modules.get();
        if (mods == null) return;

        // 1) NoRender “cody-signs” toggle
        NoRender nr = mods.get(NoRender.class);
        var setting = nr.settings.get("cody-signs");
        if (nr.isActive() && setting != null && (boolean) setting.get() && isCodySign(sign)) {
            ci.cancel();
            return;
        }

        // 2) AntiToS NoRender mode
        AntiToS anti = mods.get(AntiToS.class);
        if (anti.isActive() && anti.signMode.get() == AntiToS.SignMode.NoRender) {
            String text = Arrays.stream(sign.getFrontText().getMessages(false))
                .map(Text::getString)
                .collect(Collectors.joining());
            if (anti.containsBlacklistedText(text)) {
                ci.cancel();
            }
        }
    }

    @Unique
    private boolean isCodySign(SignBlockEntity sign) {
        return Arrays.stream(sign.getFrontText().getMessages(false))
            .map(Text::getString)
            .anyMatch(msg ->
                msg.contains("codysmile11") ||
                    msg.toLowerCase().contains("has been here :)")
            );
    }
}
