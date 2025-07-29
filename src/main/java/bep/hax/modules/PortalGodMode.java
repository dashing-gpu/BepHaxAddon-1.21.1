package bep.hax.modules;

import bep.hax.Bep;
import bep.hax.BlackOutModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

public class PortalGodMode extends BlackOutModule {
    public PortalGodMode() {super(Bep.BLACKOUT, "Portal God Mode", "Prevents taking damage while in portals");}
    @EventHandler
    private void onSend(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket) {
            event.cancel();
        }
    }
}
