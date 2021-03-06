/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.registry.block.region;

import com.skelril.nitro.registry.block.ICustomBlock;
import com.skelril.nitro.selector.EventAwareContent;
import com.skelril.skree.service.RegionService;
import com.skelril.skree.service.internal.region.Region;
import com.skelril.skree.service.internal.region.RegionErrorStatus;
import com.skelril.skree.service.internal.region.RegionPoint;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class RegionMarker extends Block implements ICustomBlock, EventAwareContent {
  public RegionMarker() {
    super(new Material(MapColor.STONE));
    this.setCreativeTab(CreativeTabs.DECORATIONS);

    // Data applied for Vanilla blocks in net.minecraft.block.Block
    this.setHardness(1.5F);
    this.setResistance(6000000.0F);
  }

  @Override
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.125F, 0.125F, 0.125F, 0.875F, 0.875F, 0.875F);
  }

  @Override
  public String __getID() {
    return "region_marker";
  }

  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
    return null;
  }

  @Override
  public boolean isOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
    return true;
  }

  @Override
  public boolean isFullCube(IBlockState state) {
    return false;
  }

  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getBlockLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Listener
  public void onBlockPlace(ChangeBlockEvent.Place event, @First Player player) {
    Optional<RegionService> optService = Sponge.getServiceManager().provide(RegionService.class);
    if (!optService.isPresent()) {
      return;
    }

    RegionService service = optService.get();

    for (Transaction<BlockSnapshot> block : event.getTransactions()) {
      if (!block.isValid()) {
        continue;
      }

      if (block.getFinal().getState().getType() != this) {
        continue;
      }

      Optional<Location<World>> optLoc = block.getFinal().getLocation();
      if (optLoc.isPresent()) {
        Optional<Region> optRef = service.getSelectedRegion(player);
        if (optRef.isPresent()) {
          Location<World> loc = optLoc.get();
          Region ref = optRef.get();
          if (ref.getWorldName().equals(loc.getExtent().getName())) {
            if (ref.isMember(player)) {
              RegionErrorStatus status = ref.addPoint(new RegionPoint(loc.getPosition()));
              if (status == RegionErrorStatus.NONE) {
                player.sendMessage(Text.of(TextColors.YELLOW, "Region marker added!"));
                continue;
              } else if (status == RegionErrorStatus.INTERSECT) {
                player.sendMessage(Text.of(TextColors.RED, "No two regions can occupy the same space!"));
              } else if (status == RegionErrorStatus.REGION_TOO_LARGE) {
                player.sendMessage(Text.of(TextColors.RED, "You do not have enough power to expand your region!"));
              }
            }
          }
        }
      }
      block.setValid(false);
    }
  }

  @Listener
  public void onBlockBreak(ChangeBlockEvent.Break event, @First Player player) {
    Optional<RegionService> optService = Sponge.getServiceManager().provide(RegionService.class);
    if (!optService.isPresent()) {
      return;
    }

    RegionService service = optService.get();

    for (Transaction<BlockSnapshot> block : event.getTransactions()) {
      if (!block.isValid()) {
        continue;
      }

      if (block.getOriginal().getState().getType() != this) {
        continue;
      }

      Optional<Location<World>> optLoc = block.getOriginal().getLocation();
      if (optLoc.isPresent()) {
        Optional<Region> optRef = service.getMarkedRegion(optLoc.get());
        if (optRef.isPresent()) {
          Region ref = optRef.get();
          if (ref.isMember(player)) {
            ref.remPoint(new RegionPoint(optLoc.get().getPosition()));
            player.sendMessage(Text.of(TextColors.YELLOW, "Region marker deleted!"));
          }
        }
      }
    }
  }
}