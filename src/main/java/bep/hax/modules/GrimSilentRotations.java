package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

public class GrimSilentRotations extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> maxDelta = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-delta")
        .description("Maximum rotation change per packet to bypass Grim checks.")
        .defaultValue(60.0)
        .min(10.0)
        .max(180.0)
        .sliderRange(10.0, 180.0)
        .build()
    );

    private final Setting<Boolean> randomize = sgGeneral.add(new BoolSetting.Builder()
        .name("randomize")
        .description("Randomize the rotation delta slightly.")
        .defaultValue(true)
        .build()
    );

    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;

    public GrimSilentRotations() {
        super(Bep.CATEGORY, "grim-silent-rotations", "Applies silent and smooth rotations to bypass Grim V3 checks.");
    }

    @Override
    public void onActivate() {
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket.LookAndOnGround packet) {
            float targetYaw = packet.getYaw(0);
            float targetPitch = packet.getPitch(0);

            // Calculate shortest delta
            float deltaYaw = MathHelper.wrapDegrees(targetYaw - lastYaw);
            float deltaPitch = MathHelper.wrapDegrees(targetPitch - lastPitch);

            double effectiveMaxDelta = maxDelta.get();
            if (randomize.get()) {
                effectiveMaxDelta *= (0.9 + Math.random() * 0.2); // Randomize between 90% and 110%
            }

            // Limit delta
            boolean needsAdjustment = false;
            if (Math.abs(deltaYaw) > effectiveMaxDelta) {
                deltaYaw = (float) (Math.signum(deltaYaw) * effectiveMaxDelta);
                needsAdjustment = true;
            }
            if (Math.abs(deltaPitch) > effectiveMaxDelta) {
                deltaPitch = (float) (Math.signum(deltaPitch) * effectiveMaxDelta);
                needsAdjustment = true;
            }

            float newYaw = lastYaw + deltaYaw;
            float newPitch = lastPitch + deltaPitch;

            if (needsAdjustment) {
                event.setCancelled(true);
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(newYaw, newPitch, packet.isOnGround(), packet.horizontalCollision()));
            }

            lastYaw = newYaw;
            lastPitch = newPitch;
        }
    }
}
