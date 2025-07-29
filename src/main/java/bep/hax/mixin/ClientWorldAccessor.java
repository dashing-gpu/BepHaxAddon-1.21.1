package bep.hax.mixin;

import net.minecraft.client.network.PendingUpdateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.minecraft.client.world.ClientWorld.class)
public interface ClientWorldAccessor {
    @Invoker("getPendingUpdateManager")
    PendingUpdateManager invokeGetPendingUpdateManager();
}
