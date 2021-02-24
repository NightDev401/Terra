package com.dfsek.terra.fabric.inventory;

import com.dfsek.terra.api.platform.handle.ItemHandle;
import com.dfsek.terra.api.platform.inventory.Item;
import com.dfsek.terra.api.platform.inventory.item.Enchantment;
import com.dfsek.terra.fabric.world.FabricAdapter;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ItemStackArgumentType;

import java.util.Collections;
import java.util.Set;

public class FabricItemHandle implements ItemHandle {

    @Override
    public Item createItem(String data) {
        try {
            return FabricAdapter.adapt(new ItemStackArgumentType().parse(new StringReader(data)).getItem());
        } catch(CommandSyntaxException e) {
            throw new IllegalArgumentException("Invalid item data \"" + data + "\"", e);
        }
    }

    @Override
    public Enchantment getEnchantment(String id) {
        return null;
    }

    @Override
    public Set<Enchantment> getEnchantments() {
        return Collections.emptySet();
    }
}
