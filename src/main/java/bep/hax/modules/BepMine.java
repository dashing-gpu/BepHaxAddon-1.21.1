package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class BepMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between mining blocks in ticks.")
        .defaultValue(0)
        .min(0)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when mining.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> doubleBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("double-break")
        .description("Allows mining two blocks at once.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Double> mineSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("mine-speed")
        .description("The mining speed multiplier.")
        .defaultValue(1.0)
        .min(0.1)
        .sliderRange(0.1, 2.0)
        .build()
    );

    private final Setting<GrimMode> grimMode = sgGeneral.add(new EnumSetting.Builder<GrimMode>()
        .name("grim-mode")
        .description("Grim bypass mode: Off (none), Old (legacy packet sequence for older Grim versions), New (updated sequence for Grim V3), Advanced (enhanced with extra abort packets for stricter checks).")
        .defaultValue(GrimMode.Off)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render the block being mined.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> readySideColor = sgRender.add(new ColorSetting.Builder()
        .name("ready-side-color")
        .description("The color of the sides of the blocks that can be broken.")
        .defaultValue(new SettingColor(0, 204, 0, 10))
        .build()
    );

    private final Setting<SettingColor> readyLineColor = sgRender.add(new ColorSetting.Builder()
        .name("ready-line-color")
        .description("The color of the lines of the blocks that can be broken.")
        .defaultValue(new SettingColor(0, 204, 0, 255))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);
    public final List<MyBlock> blocks = new ArrayList<>();

    public BepMine() {
        super(Bep.CATEGORY, "bep-mine", "Mines blocks using packets without animation.");
    }

    @Override
    public void onDeactivate() {
        for (MyBlock block : blocks) blockPool.free(block);
        blocks.clear();
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!BlockUtils.canBreak(event.blockPos)) return;

        event.cancel();

        if (!isMiningBlock(event.blockPos)) {
            blocks.add(0, blockPool.get().set(event)); // add to beginning
            if (blocks.size() > (doubleBreak.get() ? 2 : 1)) {
                blocks.remove(blocks.size() - 1);
            }
        }
    }

    public boolean isMiningBlock(BlockPos pos) {
        for (MyBlock block : blocks) {
            if (block.blockPos.equals(pos)) return true;
        }

        return false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        blocks.removeIf(MyBlock::shouldRemove);

        for (MyBlock block : blocks) {
            if (block.timer > 0) {
                block.timer--;
                continue;
            }

            if (!block.mining) {
                block.sendStart();
                block.mining = true;
            }

            block.updateProgress();

            if (block.isReady() && !block.finished) {
                block.sendStop();
                block.finished = true;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            for (MyBlock block : blocks) {
                if (Modules.get().get(BreakIndicators.class).isActive() && Modules.get().get(BreakIndicators.class).packetMine.get() && block.mining) {
                    continue;
                } else block.render(event);
            }
        }
    }

    public enum GrimMode {
        Off,
        Old,
        New,
        Advanced
    }

    public class MyBlock {
        public BlockPos blockPos;
        public BlockState blockState;
        public Block block;

        public Direction direction;

        public int timer;
        public boolean mining;
        public double progress;
        public boolean finished;

        private int startTick;

        public MyBlock set(StartBreakingBlockEvent event) {
            this.blockPos = event.blockPos;
            this.direction = event.direction;
            this.blockState = mc.world.getBlockState(blockPos);
            this.block = blockState.getBlock();
            this.timer = delay.get();
            this.mining = false;
            this.progress = 0;
            this.finished = false;
            this.startTick = mc.player.age;

            return this;
        }

        public boolean shouldRemove() {
            boolean remove = mc.world.getBlockState(blockPos).getBlock() != block || Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, blockPos.getX() + direction.getOffsetX(), blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) > mc.player.getBlockInteractionRange();

            boolean timeout = progress > 2 && (mc.player.age - startTick > 60);

            if (remove || timeout) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }

            return remove || timeout;
        }

        public boolean isReady() {
            return progress >= 1;
        }

        public void updateProgress() {
            progress += BlockUtils.getBreakDelta(mc.player.getInventory().selectedSlot, blockState) * mineSpeed.get();
        }

        public void sendStart() {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::sendStartPackets);
            else sendStartPackets();
        }

        private void sendStartPackets() {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            if (grimMode.get() != GrimMode.Off) {
                if (grimMode.get() == GrimMode.Old) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                } else if (grimMode.get() == GrimMode.New) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                } else if (grimMode.get() == GrimMode.Advanced) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
            }
        }

        public void sendStop() {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::sendStopPackets);
            else sendStopPackets();
        }

        private void sendStopPackets() {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
            if (grimMode.get() != GrimMode.Off) {
                BlockPos fakePos = new BlockPos(blockPos.getX(), blockPos.getY() + 500, blockPos.getZ());
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, fakePos, direction));
                if (grimMode.get() == GrimMode.Advanced) {
                    BlockPos fakePos2 = new BlockPos(blockPos.getX(), blockPos.getY() - 500, blockPos.getZ());
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, fakePos2, direction));
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
            }
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }

        public void render(Render3DEvent event) {
            VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

            double x1 = blockPos.getX();
            double y1 = blockPos.getY();
            double z1 = blockPos.getZ();
            double x2 = blockPos.getX() + 1;
            double y2 = blockPos.getY() + 1;
            double z2 = blockPos.getZ() + 1;

            if (!shape.isEmpty()) {
                x1 = blockPos.getX() + shape.getMin(Direction.Axis.X);
                y1 = blockPos.getY() + shape.getMin(Direction.Axis.Y);
                z1 = blockPos.getZ() + shape.getMin(Direction.Axis.Z);
                x2 = blockPos.getX() + shape.getMax(Direction.Axis.X);
                y2 = blockPos.getY() + shape.getMax(Direction.Axis.Y);
                z2 = blockPos.getZ() + shape.getMax(Direction.Axis.Z);
            }

            if (isReady()) {
                event.renderer.box(x1, y1, z1, x2, y2, z2, readySideColor.get(), readyLineColor.get(), shapeMode.get(), 0);
            } else {
                event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}
