/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/**
 * This class is generated by jOOQ
 */
package com.skelril.skree.db.schema;


import com.skelril.skree.db.schema.tables.ItemAliases;
import com.skelril.skree.db.schema.tables.ItemId;
import com.skelril.skree.db.schema.tables.ItemValues;
import com.skelril.skree.db.schema.tables.Modifiers;

import javax.annotation.Generated;


/**
 * Convenience access to all tables in mc_db
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.6.0"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

	/**
	 * The table mc_db.item_aliases
	 */
	public static final ItemAliases ITEM_ALIASES = com.skelril.skree.db.schema.tables.ItemAliases.ITEM_ALIASES;

	/**
	 * The table mc_db.item_id
	 */
	public static final ItemId ITEM_ID = com.skelril.skree.db.schema.tables.ItemId.ITEM_ID;

	/**
	 * The table mc_db.item_values
	 */
	public static final ItemValues ITEM_VALUES = com.skelril.skree.db.schema.tables.ItemValues.ITEM_VALUES;

	/**
	 * The table mc_db.modifiers
	 */
	public static final Modifiers MODIFIERS = com.skelril.skree.db.schema.tables.Modifiers.MODIFIERS;
}
