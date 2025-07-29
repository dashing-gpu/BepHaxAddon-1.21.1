package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class AutoSmithing extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoCraft = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-craft")
        .description("Automatically crafts the upgrade when items are placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("delay")
        .description("Delay between actions in seconds.")
        .defaultValue(0.5)
        .min(0.0)
        .sliderMax(2.0)
        .build()
    );

    private final Setting<Item> templateItem = sgGeneral.add(new ItemSetting.Builder()
        .name("template-item")
        .description("The item to use as template.")
        .defaultValue(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
        .filter(item -> item == Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE ||
            item == Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE ||
            item == Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE)
        .build()
    );

    private final Setting<Item> upgradeMaterial = sgGeneral.add(new ItemSetting.Builder()
        .name("upgrade-material")
        .description("The material to use for upgrading or trimming.")
        .defaultValue(Items.NETHERITE_INGOT)
        .filter(item -> item == Items.AMETHYST_SHARD ||
            item == Items.COPPER_INGOT ||
            item == Items.DIAMOND ||
            item == Items.EMERALD ||
            item == Items.GOLD_INGOT ||
            item == Items.IRON_INGOT ||
            item == Items.LAPIS_LAZULI ||
            item == Items.NETHERITE_INGOT ||
            item == Items.QUARTZ ||
            item == Items.REDSTONE)
        .build()
    );

    private final Setting<List<Item>> itemsToUpgrade = sgGeneral.add(new ItemListSetting.Builder()
        .name("items-to-upgrade")
        .description("The items to upgrade or trim.")
        .defaultValue(Arrays.asList(
            Items.DIAMOND_SWORD,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_AXE,
            Items.DIAMOND_SHOVEL,
            Items.DIAMOND_HOE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS
        ))
        .filter(item -> item == Items.LEATHER_HELMET ||
            item == Items.LEATHER_CHESTPLATE ||
            item == Items.LEATHER_LEGGINGS ||
            item == Items.LEATHER_BOOTS ||
            item == Items.CHAINMAIL_HELMET ||
            item == Items.CHAINMAIL_CHESTPLATE ||
            item == Items.CHAINMAIL_LEGGINGS ||
            item == Items.CHAINMAIL_BOOTS ||
            item == Items.IRON_HELMET ||
            item == Items.IRON_CHESTPLATE ||
            item == Items.IRON_LEGGINGS ||
            item == Items.IRON_BOOTS ||
            item == Items.GOLDEN_HELMET ||
            item == Items.GOLDEN_CHESTPLATE ||
            item == Items.GOLDEN_LEGGINGS ||
            item == Items.GOLDEN_BOOTS ||
            item == Items.DIAMOND_HELMET ||
            item == Items.DIAMOND_CHESTPLATE ||
            item == Items.DIAMOND_LEGGINGS ||
            item == Items.DIAMOND_BOOTS ||
            item == Items.NETHERITE_HELMET ||
            item == Items.NETHERITE_CHESTPLATE ||
            item == Items.NETHERITE_LEGGINGS ||
            item == Items.NETHERITE_BOOTS ||
            item == Items.DIAMOND_SWORD ||
            item == Items.DIAMOND_PICKAXE ||
            item == Items.DIAMOND_AXE ||
            item == Items.DIAMOND_SHOVEL ||
            item == Items.DIAMOND_HOE)
        .build()
    );

    private final Setting<Boolean> ignoreTrimmed = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-trimmed")
        .description("Ignore trimmed armor when upgrading.")
        .defaultValue(true)
        .build()
    );

    private boolean isSmithingScreen;
    private int tickCounter;
    private int delayTicks;
    private boolean hasNotifiedTemplateMissing;
    private boolean hasNotifiedItemMissing;
    private boolean hasNotifiedMaterialMissing;

    public AutoSmithing() {
        super(Bep.CATEGORY, "auto-smithing", "Automatically upgrades items in a smithing table.");
    }

    @Override
    public void onActivate() {
        isSmithingScreen = false;
        tickCounter = 0;
        hasNotifiedTemplateMissing = false;
        hasNotifiedItemMissing = false;
        hasNotifiedMaterialMissing = false;
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof SmithingScreen) {
            isSmithingScreen = true;
            tickCounter = 0;
            hasNotifiedTemplateMissing = false;
            hasNotifiedItemMissing = false;
            hasNotifiedMaterialMissing = false;
        } else {
            isSmithingScreen = false;
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onTick(TickEvent.Post event) {
        if (!isSmithingScreen || mc.player == null || mc.interactionManager == null) return;

        delayTicks = (int) (delay.get() * 20);
        if (tickCounter < delayTicks) {
            tickCounter++;
            return;
        }
        tickCounter = 0;

        // Check slot 0 (template)
        if (mc.player.currentScreenHandler.getSlot(0).getStack().isEmpty()) {
            if (!hasNotifiedTemplateMissing) {
                Item template = templateItem.get();
                int templatePlayerSlot = InvUtils.find(template).slot();
                if (templatePlayerSlot != -1) {
                    int templateContainerSlot = getContainerSlotFromPlayerSlot(templatePlayerSlot);
                    if (templateContainerSlot != -1) {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, templateContainerSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.PICKUP, mc.player);
                    }
                } else {
                    mc.player.sendMessage(Text.of("[Upgrader] No " + Registries.ITEM.getId(template)), false);
                    hasNotifiedTemplateMissing = true;
                }
            }
            return;
        } else {
            hasNotifiedTemplateMissing = false;
        }

        // Check slot 1 (base item)
        if (mc.player.currentScreenHandler.getSlot(1).getStack().isEmpty()) {
            if (!hasNotifiedItemMissing) {
                boolean foundItem = false;
                for (Item item : itemsToUpgrade.get()) {
                    Predicate<ItemStack> predicate = stack -> stack.isOf(item) && (!ignoreTrimmed.get() || !isTrimmed(stack));
                    if (InvUtils.find(predicate).slot() != -1) {
                        foundItem = true;
                        break;
                    }
                }
                if (!foundItem) {
                    mc.player.sendMessage(Text.of("[Upgrader] No items to upgrade found from the selected list"), false);
                    hasNotifiedItemMissing = true;
                } else {
                    for (Item item : itemsToUpgrade.get()) {
                        Predicate<ItemStack> predicate = stack -> stack.isOf(item) && (!ignoreTrimmed.get() || !isTrimmed(stack));
                        int itemPlayerSlot = InvUtils.find(predicate).slot();
                        if (itemPlayerSlot != -1) {
                            int itemContainerSlot = getContainerSlotFromPlayerSlot(itemPlayerSlot);
                            if (itemContainerSlot != -1) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemContainerSlot, 0, SlotActionType.PICKUP, mc.player);
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 1, 0, SlotActionType.PICKUP, mc.player);
                            }
                            break;
                        }
                    }
                }
            }
            return;
        } else {
            hasNotifiedItemMissing = false;
        }

        // Check slot 2 (upgrade material)
        if (mc.player.currentScreenHandler.getSlot(2).getStack().isEmpty()) {
            if (!hasNotifiedMaterialMissing) {
                Item material = upgradeMaterial.get();
                int materialPlayerSlot = InvUtils.find(material).slot();
                if (materialPlayerSlot != -1) {
                    int materialContainerSlot = getContainerSlotFromPlayerSlot(materialPlayerSlot);
                    if (materialContainerSlot != -1) {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, materialContainerSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 2, 0, SlotActionType.PICKUP, mc.player);
                    }
                } else {
                    mc.player.sendMessage(Text.of("[Upgrader] No " + Registries.ITEM.getId(material)), false);
                    hasNotifiedMaterialMissing = true;
                }
            }
            return;
        } else {
            hasNotifiedMaterialMissing = false;
        }

        // All slots filled, take result if autoCraft is enabled
        if (autoCraft.get() && !mc.player.currentScreenHandler.getSlot(3).getStack().isEmpty()) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 3, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }

    private boolean isTrimmed(ItemStack stack) {
        return stack.get(DataComponentTypes.TRIM) != null;
    }

    private int getContainerSlotFromPlayerSlot(int playerSlot) {
        int containerSize = 4; // Smithing table has 4 slots (0-3)
        if (playerSlot >= 0 && playerSlot <= 8) { // Hotbar
            return containerSize + 27 + playerSlot; // Slots 31-39
        } else if (playerSlot >= 9 && playerSlot <= 35) { // Main inventory
            return containerSize + (playerSlot - 9); // Slots 4-30
        }
        return -1; // Invalid slot (armor and offhand not accessible)
    }
}
