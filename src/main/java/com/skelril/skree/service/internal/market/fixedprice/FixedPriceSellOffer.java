/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.service.internal.market.fixedprice;

import com.skelril.skree.service.internal.market.MarketEntry;
import com.skelril.skree.service.internal.market.OfferStatus;
import com.skelril.skree.service.internal.market.SellOffer;

import java.math.BigDecimal;
import java.util.UUID;

public class FixedPriceSellOffer implements SellOffer {

    private UUID offerer;
    private MarketEntry entry;
    private BigDecimal price;
    private int amount;
    private int completed = 0;
    private OfferStatus status = OfferStatus.OKAY;

    public FixedPriceSellOffer(UUID offerer, MarketEntry entry, BigDecimal price, int amount) {
        this.offerer = offerer;
        this.entry = entry;
        this.price = price;
        this.amount = amount;
    }

    @Override
    public BigDecimal getMinPricePerItem() {
        return price;
    }

    @Override
    public UUID getOfferer() {
        return offerer;
    }

    @Override
    public MarketEntry getEntry() {
        return entry;
    }

    @Override
    public int getAmountRequested() {
        return amount;
    }

    protected void complete(int amount) {
        this.completed += amount;
    }

    @Override
    public int getAmountCompleted() {
        return completed;
    }

    protected void setStatus(OfferStatus status) {
        this.status = status;
    }

    @Override
    public OfferStatus getStatus() {
        return status;
    }
}
