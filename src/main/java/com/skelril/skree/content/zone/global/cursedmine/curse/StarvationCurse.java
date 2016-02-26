/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.content.zone.global.cursedmine.curse;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.function.Consumer;

public class StarvationCurse implements Consumer<Player> {
    @Override
    public void accept(Player player) {
        if (player.get(Keys.FOOD_LEVEL).orElse(0) > 0) {
            player.sendMessage(Text.of(TextColors.RED, "Tasty..."));
            player.offer(Keys.FOOD_LEVEL, player.get(Keys.FOOD_LEVEL).get() - 1);
        }
    }
}
