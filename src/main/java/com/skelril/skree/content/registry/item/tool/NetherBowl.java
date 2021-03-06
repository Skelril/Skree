/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.item.tool;

import com.skelril.nitro.registry.item.CustomItem;
import com.skelril.nitro.selector.EventAwareContent;
import com.skelril.skree.content.registry.item.Teleporter;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.skelril.nitro.transformer.ForgeTransformer.tf;

public class NetherBowl extends CustomItem implements EventAwareContent, Teleporter {

  @Override
  public String __getId() {
    return "nether_bowl";
  }

  @Override
  public List<String> __getMeshDefinitions() {
    List<String> baseList = super.__getMeshDefinitions();
    baseList.add("nether_bowl_full");
    return baseList;
  }

  @Override
  public int __getMaxStackSize() {
    return 1;
  }

  @Override
  public CreativeTabs __getCreativeTab() {
    return CreativeTabs.MATERIALS;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void getSubItems(net.minecraft.item.Item itemIn, CreativeTabs tab, List subItems) {
    subItems.add(new ItemStack(itemIn, 1, 0));
  }

  @Override
  public EnumAction getItemUseAction(ItemStack stack) {
    return EnumAction.DRINK;
  }

  @Override
  public int getMaxItemUseDuration(ItemStack stack) {
    return 32;
  }

  @Override
  public ItemStack onItemUseFinish(ItemStack stack, net.minecraft.world.World worldIn, EntityLivingBase entityLiving) {
    Optional<Location<World>> optLoc = getDestination(stack);
    if (optLoc.isPresent()) {
      tf(entityLiving).setLocation(optLoc.get());
    }

    return new ItemStack(this);
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(net.minecraft.world.World worldIn, EntityPlayer playerIn, EnumHand handIn) {
    ItemStack itemStackIn = playerIn.getHeldItem(handIn);
    if (getDestination(itemStackIn).isPresent()) {
      playerIn.setActiveHand(handIn);
      return new ActionResult<>(EnumActionResult.SUCCESS, itemStackIn);
    } else {
      return new ActionResult<>(EnumActionResult.FAIL, itemStackIn);
    }
  }

  @Override
  public void setDestination(ItemStack stack, Location<World> target) {
    Teleporter.super.setDestination(stack, target);
    stack.setItemDamage(1);
  }

  // Modified Native Item methods

  @SuppressWarnings("unchecked")
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable net.minecraft.world.World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
    Optional<String> optDestStr = getClientDestination(stack);
    if (optDestStr.isPresent()) {
      tooltip.add("Drink to return to your grave.");
    } else {
      tooltip.add("An evil looking bowl.");
    }
  }
}