package com.skelril.skree.service.internal.market;

import com.skelril.nitro.Clause;
import org.spongepowered.api.item.inventory.ItemStack;

public interface TypeDeducer {
  ItemStack getItemStack(Clause<String, String> idVariant);

  Clause<String, String> getVariant(ItemStack stack);
}
