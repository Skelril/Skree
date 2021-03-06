/**
 * This class is generated by jOOQ
 */
package com.skelril.skree.db.schema.tables.records;


import com.skelril.skree.db.schema.tables.Regions;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record8;
import org.jooq.Row8;
import org.jooq.impl.UpdatableRecordImpl;

import javax.annotation.Generated;


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
public class RegionsRecord extends UpdatableRecordImpl<RegionsRecord> implements Record8<Integer, String, Integer, Double, Double, Double, String, Integer> {

	private static final long serialVersionUID = -455998510;

	/**
	 * Setter for <code>mc_db.regions.id</code>.
	 */
	public void setId(Integer value) {
		setValue(0, value);
	}

	/**
	 * Getter for <code>mc_db.regions.id</code>.
	 */
	public Integer getId() {
		return (Integer) getValue(0);
	}

	/**
	 * Setter for <code>mc_db.regions.uuid</code>.
	 */
	public void setUuid(String value) {
		setValue(1, value);
	}

	/**
	 * Getter for <code>mc_db.regions.uuid</code>.
	 */
	public String getUuid() {
		return (String) getValue(1);
	}

	/**
	 * Setter for <code>mc_db.regions.world_id</code>.
	 */
	public void setWorldId(Integer value) {
		setValue(2, value);
	}

	/**
	 * Getter for <code>mc_db.regions.world_id</code>.
	 */
	public Integer getWorldId() {
		return (Integer) getValue(2);
	}

	/**
	 * Setter for <code>mc_db.regions.x</code>.
	 */
	public void setX(Double value) {
		setValue(3, value);
	}

	/**
	 * Getter for <code>mc_db.regions.x</code>.
	 */
	public Double getX() {
		return (Double) getValue(3);
	}

	/**
	 * Setter for <code>mc_db.regions.y</code>.
	 */
	public void setY(Double value) {
		setValue(4, value);
	}

	/**
	 * Getter for <code>mc_db.regions.y</code>.
	 */
	public Double getY() {
		return (Double) getValue(4);
	}

	/**
	 * Setter for <code>mc_db.regions.z</code>.
	 */
	public void setZ(Double value) {
		setValue(5, value);
	}

	/**
	 * Getter for <code>mc_db.regions.z</code>.
	 */
	public Double getZ() {
		return (Double) getValue(5);
	}

	/**
	 * Setter for <code>mc_db.regions.name</code>.
	 */
	public void setName(String value) {
		setValue(6, value);
	}

	/**
	 * Getter for <code>mc_db.regions.name</code>.
	 */
	public String getName() {
		return (String) getValue(6);
	}

	/**
	 * Setter for <code>mc_db.regions.power</code>.
	 */
	public void setPower(Integer value) {
		setValue(7, value);
	}

	/**
	 * Getter for <code>mc_db.regions.power</code>.
	 */
	public Integer getPower() {
		return (Integer) getValue(7);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Record1<Integer> key() {
		return (Record1) super.key();
	}

	// -------------------------------------------------------------------------
	// Record8 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row8<Integer, String, Integer, Double, Double, Double, String, Integer> fieldsRow() {
		return (Row8) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row8<Integer, String, Integer, Double, Double, Double, String, Integer> valuesRow() {
		return (Row8) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Integer> field1() {
		return Regions.REGIONS.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field2() {
		return Regions.REGIONS.UUID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Integer> field3() {
		return Regions.REGIONS.WORLD_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Double> field4() {
		return Regions.REGIONS.X;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Double> field5() {
		return Regions.REGIONS.Y;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Double> field6() {
		return Regions.REGIONS.Z;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field7() {
		return Regions.REGIONS.NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Integer> field8() {
		return Regions.REGIONS.POWER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer value1() {
		return getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value2() {
		return getUuid();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer value3() {
		return getWorldId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double value4() {
		return getX();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double value5() {
		return getY();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double value6() {
		return getZ();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value7() {
		return getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer value8() {
		return getPower();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord value1(Integer value) {
		setId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord value2(String value) {
		setUuid(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord value3(Integer value) {
		setWorldId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord value4(Double value) {
		setX(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord value5(Double value) {
		setY(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord value6(Double value) {
		setZ(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord value7(String value) {
		setName(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord value8(Integer value) {
		setPower(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionsRecord values(Integer value1, String value2, Integer value3, Double value4, Double value5, Double value6, String value7, Integer value8) {
		value1(value1);
		value2(value2);
		value3(value3);
		value4(value4);
		value5(value5);
		value6(value6);
		value7(value7);
		value8(value8);
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached RegionsRecord
	 */
	public RegionsRecord() {
		super(Regions.REGIONS);
	}

	/**
	 * Create a detached, initialised RegionsRecord
	 */
	public RegionsRecord(Integer id, String uuid, Integer worldId, Double x, Double y, Double z, String name, Integer power) {
		super(Regions.REGIONS);

		setValue(0, id);
		setValue(1, uuid);
		setValue(2, worldId);
		setValue(3, x);
		setValue(4, y);
		setValue(5, z);
		setValue(6, name);
		setValue(7, power);
	}
}
