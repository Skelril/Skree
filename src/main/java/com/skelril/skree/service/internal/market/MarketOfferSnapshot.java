/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.service.internal.market;

import java.math.BigDecimal;
import java.util.UUID;

public interface MarketOfferSnapshot {
    UUID getOfferID();
    UUID getOfferer();

    MarketItem getItem();
    BigDecimal getPrice();

    MarketOfferStatus getStatus();

    int getCompletedQuantity();
    default int getRemainingQuantity() {
        return getTotalQuantity() - getCompletedQuantity();
    }
    int getTotalQuantity();
}
