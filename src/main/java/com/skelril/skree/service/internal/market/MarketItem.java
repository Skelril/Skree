/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.service.internal.market;

import java.util.Collection;
import java.util.UUID;

public interface MarketItem {
    UUID getID();

    String getItemID();

    String getName();
    Collection<String> getAliases();
}
