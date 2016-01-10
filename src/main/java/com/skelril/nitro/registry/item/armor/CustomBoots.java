/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.skelril.nitro.registry.item.armor;

public abstract class CustomBoots extends CustomArmor {
    public CustomBoots() {
        super(3);
    }

    @Override
    public String __getArmorCategory() {
        return "boots";
    }
}
