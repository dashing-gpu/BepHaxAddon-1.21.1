package bep.hax.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShulkerDataParser {

    /**
     * Parses shulker box contents from an ItemStack
     * Tries multiple methods to ensure compatibility
     */
    public static Map<Item, Integer> parseShulkerContents(ItemStack shulkerStack) {
        Map<Item, Integer> itemCounts = new HashMap<>();

        // Method 1: Try ContainerComponent (newer method)
        ContainerComponent container = shulkerStack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            List<ItemStack> items = container.stream().toList();
            for (ItemStack itemStack : items) {
                if (!itemStack.isEmpty()) {
                    itemCounts.merge(itemStack.getItem(), itemStack.getCount(), Integer::sum);
                }
            }
            if (!itemCounts.isEmpty()) {
                return itemCounts;
            }
        }

        // Method 2: Try BlockEntityData (legacy/alternative method)
        NbtComponent blockEntityData = shulkerStack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            NbtCompound nbt = blockEntityData.copyNbt();
            if (nbt != null && nbt.contains("Items", NbtElement.LIST_TYPE)) {
                NbtList items = nbt.getList("Items", NbtElement.COMPOUND_TYPE);

                for (int i = 0; i < items.size(); i++) {
                    NbtCompound itemTag = items.getCompound(i);
                    ItemStack parsed = parseItemFromNbt(itemTag);
                    if (!parsed.isEmpty()) {
                        itemCounts.merge(parsed.getItem(), parsed.getCount(), Integer::sum);
                    }
                }
            }
        }

        // Method 3: Try custom data
        NbtComponent customData = shulkerStack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt != null && nbt.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE)) {
                NbtCompound blockEntityTag = nbt.getCompound("BlockEntityTag");
                if (blockEntityTag.contains("Items", NbtElement.LIST_TYPE)) {
                    NbtList items = blockEntityTag.getList("Items", NbtElement.COMPOUND_TYPE);

                    for (int i = 0; i < items.size(); i++) {
                        NbtCompound itemTag = items.getCompound(i);
                        ItemStack parsed = parseItemFromNbt(itemTag);
                        if (!parsed.isEmpty()) {
                            itemCounts.merge(parsed.getItem(), parsed.getCount(), Integer::sum);
                        }
                    }
                }
            }
        }

        return itemCounts;
    }

    private static ItemStack parseItemFromNbt(NbtCompound itemTag) {
        // Parse item ID
        String id = itemTag.getString("id");
        if (id.isEmpty()) return ItemStack.EMPTY;

        // Parse count (try both formats)
        int count = 1;
        if (itemTag.contains("count", NbtElement.NUMBER_TYPE)) {
            count = itemTag.getInt("count");
        } else if (itemTag.contains("Count", NbtElement.NUMBER_TYPE)) {
            count = itemTag.getByte("Count");
        }

        // Get item from registry
        Identifier itemId = Identifier.tryParse(id);
        if (itemId == null) return ItemStack.EMPTY;

        Item item = Registries.ITEM.get(itemId);
        if (item == null || item == Registries.ITEM.get(Registries.ITEM.getDefaultId())) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item, count);
    }
}
