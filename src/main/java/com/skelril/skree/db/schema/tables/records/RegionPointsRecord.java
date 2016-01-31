/**
 * This class is generated by jOOQ
 */
package com.skelril.skree.db.schema.tables.records;


import com.skelril.skree.db.schema.tables.RegionPoints;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
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
public class RegionPointsRecord extends UpdatableRecordImpl<RegionPointsRecord> implements Record5<Integer, Integer, Double, Double, Double> {

	private static final long serialVersionUID = -1039579622;

	/**
	 * Setter for <code>mc_db.region_points.id</code>.
	 */
	public void setId(Integer value) {
		setValue(0, value);
	}

	/**
	 * Getter for <code>mc_db.region_points.id</code>.
	 */
	public Integer getId() {
		return (Integer) getValue(0);
	}

	/**
	 * Setter for <code>mc_db.region_points.region_id</code>.
	 */
	public void setRegionId(Integer value) {
		setValue(1, value);
	}

	/**
	 * Getter for <code>mc_db.region_points.region_id</code>.
	 */
	public Integer getRegionId() {
		return (Integer) getValue(1);
	}

	/**
	 * Setter for <code>mc_db.region_points.x</code>.
	 */
	public void setX(Double value) {
		setValue(2, value);
	}

	/**
	 * Getter for <code>mc_db.region_points.x</code>.
	 */
	public Double getX() {
		return (Double) getValue(2);
	}

	/**
	 * Setter for <code>mc_db.region_points.y</code>.
	 */
	public void setY(Double value) {
		setValue(3, value);
	}

	/**
	 * Getter for <code>mc_db.region_points.y</code>.
	 */
	public Double getY() {
		return (Double) getValue(3);
	}

	/**
	 * Setter for <code>mc_db.region_points.z</code>.
	 */
	public void setZ(Double value) {
		setValue(4, value);
	}

	/**
	 * Getter for <code>mc_db.region_points.z</code>.
	 */
	public Double getZ() {
		return (Double) getValue(4);
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
	// Record5 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row5<Integer, Integer, Double, Double, Double> fieldsRow() {
		return (Row5) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row5<Integer, Integer, Double, Double, Double> valuesRow() {
		return (Row5) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Integer> field1() {
		return RegionPoints.REGION_POINTS.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Integer> field2() {
		return RegionPoints.REGION_POINTS.REGION_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Double> field3() {
		return RegionPoints.REGION_POINTS.X;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Double> field4() {
		return RegionPoints.REGION_POINTS.Y;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Double> field5() {
		return RegionPoints.REGION_POINTS.Z;
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
	public Integer value2() {
		return getRegionId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double value3() {
		return getX();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double value4() {
		return getY();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double value5() {
		return getZ();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionPointsRecord value1(Integer value) {
		setId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionPointsRecord value2(Integer value) {
		setRegionId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionPointsRecord value3(Double value) {
		setX(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionPointsRecord value4(Double value) {
		setY(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionPointsRecord value5(Double value) {
		setZ(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RegionPointsRecord values(Integer value1, Integer value2, Double value3, Double value4, Double value5) {
		value1(value1);
		value2(value2);
		value3(value3);
		value4(value4);
		value5(value5);
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached RegionPointsRecord
	 */
	public RegionPointsRecord() {
		super(RegionPoints.REGION_POINTS);
	}

	/**
	 * Create a detached, initialised RegionPointsRecord
	 */
	public RegionPointsRecord(Integer id, Integer regionId, Double x, Double y, Double z) {
		super(RegionPoints.REGION_POINTS);

		setValue(0, id);
		setValue(1, regionId);
		setValue(2, x);
		setValue(3, y);
		setValue(4, z);
	}
}