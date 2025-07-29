package bep.hax.mixin;

import bep.hax.modules.Fog;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class MixinBackground {
    @Inject(method = "applyFog", at = @At("TAIL"), cancellable = true)
    private static void applyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance, boolean thickFog, float tickDelta, CallbackInfoReturnable<net.minecraft.client.render.Fog> info) {
        Fog fog = Modules.get().get(Fog.class);
        if (fog != null && fog.isActive() && fogType == BackgroundRenderer.FogType.FOG_TERRAIN) {
            info.setReturnValue(fog.getFogParams());
        }
    }
}
