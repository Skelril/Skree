/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.anticheat;

import com.skelril.skree.SkreePlugin;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Piston;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;

public class AntiRailDupeListener {
  private static final List<BlockType> RAIL_BLOCKS = new ArrayList<>();

  static {
    RAIL_BLOCKS.add(BlockTypes.RAIL);
    RAIL_BLOCKS.add(BlockTypes.ACTIVATOR_RAIL);
    RAIL_BLOCKS.add(BlockTypes.DETECTOR_RAIL);
    RAIL_BLOCKS.add(BlockTypes.GOLDEN_RAIL);
  }

  @Listener
  public void onPistonMove(ChangeBlockEvent event, @First Piston piston) {
    event.getTransactions().stream().map(Transaction::getFinal).forEach(block -> {
      BlockType finalType = block.getState().getType();
      if (RAIL_BLOCKS.contains(finalType)) {
        Location<World> location = block.getLocation().get();
        Task.builder().execute(() -> {
          location.setBlockType(BlockTypes.AIR);
        }).delayTicks(1).submit(SkreePlugin.inst());
      }
    });
  }
}
