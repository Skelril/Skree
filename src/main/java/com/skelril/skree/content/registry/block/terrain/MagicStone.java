/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.block.terrain;

import com.skelril.nitro.registry.block.ICustomBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;

import java.util.Random;

public class MagicStone extends Block implements ICustomBlock {

  public MagicStone() {
    super(new Material(MapColor.STONE));
    this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);

    // Data applied for Vanilla blocks in net.minecraft.block.Block
    this.setHardness(1.5F);
    this.setResistance(10.0F);
    this.setSoundType(SoundType.STONE);
  }

  @Override
  public String __getID() {
    return "magic_stone";
  }

  @Override
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Item.getItemFromBlock(Blocks.COBBLESTONE);
  }
}
