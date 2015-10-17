/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.system.market;

import com.google.inject.Inject;
import com.skelril.skree.SkreePlugin;
import com.skelril.skree.content.market.MarketCommand;
import com.skelril.skree.content.modifier.ModifierNotifier;
import com.skelril.skree.service.MarketService;
import com.skelril.skree.service.internal.market.MarketServiceImpl;
import com.skelril.skree.system.ServiceProvider;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.ProviderExistsException;

public class MarketSystem implements ServiceProvider<MarketService> {
    private MarketService service;

    @Inject
    public MarketSystem(SkreePlugin plugin, Game game) {
        service = new MarketServiceImpl();

        // Register the service
        try {
            game.getEventManager().registerListeners(plugin, new ModifierNotifier());
            game.getServiceManager().setProvider(plugin, MarketService.class, service);
            game.getCommandDispatcher().register(plugin, MarketCommand.aquireSpec(game), "market", "mk");
        } catch (ProviderExistsException e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public MarketService getService() {
        return service;
    }
}