package bep.hax.mixin;

import java.util.Arrays;
import net.minecraft.text.Text;
import java.util.stream.Collectors;
import bep.hax.modules.AntiToS;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.block.entity.SignText;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(AbstractSignBlockEntityRenderer.class)
public abstract class AbstractSignBlockEntityRendererMixin implements BlockEntityRenderer<SignBlockEntity> {

    // See AntiToS.java
    @ModifyVariable(method = "renderText", at = @At("HEAD"), argsOnly = true)
    private SignText modifyRenderedText(SignText signText) {
        Modules modules = Modules.get();
        if (modules == null ) return signText;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return signText;

        String testText = Arrays.stream(signText.getMessages(false))
            .map(Text::getString)
            .collect(Collectors.joining(" "))
            .trim();
        return antiToS.containsBlacklistedText(testText) ? antiToS.familyFriendlySignText(signText) : signText;
    }

    // See NoRenderMixin.java
    @Inject(method = "render(Lnet/minecraft/block/entity/SignBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V", at = @At("HEAD"), cancellable = true)
    private void onRender(SignBlockEntity signBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        AntiToS antiToS = mods.get(AntiToS.class);
        NoRender noRender = mods.get(NoRender.class);
        var signSetting = noRender.settings.get("cody-signs");
        if (signSetting == null) return;
        if (noRender.isActive() && (boolean) signSetting.get() && isCodySign(signBlockEntity)) {
            ci.cancel();
        }

        if (antiToS.isActive() && antiToS.signMode.get().equals(AntiToS.SignMode.NoRender)) {
            if (antiToS.containsBlacklistedText(Arrays.stream(signBlockEntity.getFrontText().getMessages(false)).map(Text::getString).collect(Collectors.joining()))) {
                ci.cancel();
            }
        }
    }

    @Unique
    private boolean isCodySign(SignBlockEntity sbe) {
        SignText frontText = sbe.getFrontText();
        return Arrays.stream(frontText.getMessages(false)).anyMatch(msg -> msg.getString().contains("codysmile11") || msg.getString().toLowerCase().contains("has been here :)"));
    }
}
