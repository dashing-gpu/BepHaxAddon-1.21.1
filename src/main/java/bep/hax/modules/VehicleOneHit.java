package bep.hax.modules;


import bep.hax.Bep;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.EntityHitResult;

public class VehicleOneHit extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("The number of packets to send.")
        .defaultValue(16)
        .range(1, 100)
        .sliderRange(1, 20)
        .build()
    );

    public VehicleOneHit() {
        super(Bep.CATEGORY, "vehicle-one-hit", "Destroy vehicles with one hit by REJECTS Anti-Cope.");
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerInteractEntityC2SPacket)
            || !(mc.crosshairTarget instanceof EntityHitResult ehr)
            || (!(ehr.getEntity() instanceof AbstractMinecartEntity) && !(ehr.getEntity() instanceof BoatEntity))
        ) return;

        for (int i = 0; i < amount.get() - 1; i++) {
            mc.player.networkHandler.getConnection().send(event.packet, null);
        }
    }
}
