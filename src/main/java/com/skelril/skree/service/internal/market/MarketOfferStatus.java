/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.skree.service.internal.market;

public enum MarketOfferStatus {
    COMPLETE(0),
    PENDING(1),
    CANCELLED_BY_ADMIN(2),
    CANCELLED_BY_USER(3),
    REJECTED_NO_DEAL(4),
    REJECTED_NO_SPACE(5);

    private final int statusCode;

    MarketOfferStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
