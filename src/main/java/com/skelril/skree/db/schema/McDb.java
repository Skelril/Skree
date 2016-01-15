/**
 * This class is generated by jOOQ
 */
package com.skelril.skree.db.schema;


import com.skelril.skree.db.schema.tables.*;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.6.0"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class McDb extends SchemaImpl {

	private static final long serialVersionUID = 312772327;

	/**
	 * The reference instance of <code>mc_db</code>
	 */
	public static final McDb MC_DB = new McDb();

	/**
	 * No further instances allowed
	 */
	private McDb() {
		super("mc_db");
	}

	@Override
	public final List<Table<?>> getTables() {
		List result = new ArrayList();
		result.addAll(getTables0());
		return result;
	}

	private final List<Table<?>> getTables0() {
		return Arrays.<Table<?>>asList(
			ItemAliases.ITEM_ALIASES,
			ItemData.ITEM_DATA,
			Modifiers.MODIFIERS,
			Players.PLAYERS,
			Regions.REGIONS,
			RegionMembers.REGION_MEMBERS,
			RegionPoints.REGION_POINTS,
			Worlds.WORLDS);
	}
}
