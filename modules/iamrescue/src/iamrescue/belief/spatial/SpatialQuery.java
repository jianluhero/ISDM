package iamrescue.belief.spatial;

import iamrescue.util.PositionXY;
import iamrescue.util.SpatialUtils;
import rescuecore2.standard.entities.StandardEntity;

import com.vividsolutions.jts.geom.Geometry;

public class SpatialQuery {

	private Geometry geometry;

	private int distance;

	private boolean usingOnlyRepresentativePoints;

	private Class<? extends StandardEntity> queryClass;

	public SpatialQuery(PositionXY[] apexes, int distance,
			boolean useOnlyRepresentativePoints,
			Class<? extends StandardEntity> queryClass) {
		this(SpatialUtils.convertApexes(apexes), distance,
				useOnlyRepresentativePoints, queryClass);
	}

	public SpatialQuery(Geometry geometry, int distance,
			boolean useOnlyRepresentativePoints,
			Class<? extends StandardEntity> queryClass) {
		this.geometry = geometry;
		this.distance = distance;
		this.usingOnlyRepresentativePoints = useOnlyRepresentativePoints;
		this.queryClass = queryClass;
	}

	/**
	 * @return the geometry
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * @param geometry
	 *            the geometry to set
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	/**
	 * @return the distance
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * @param distance
	 *            the distance to set
	 */
	public void setDistance(int distance) {
		this.distance = distance;
	}

	/**
	 * @return the useOnlyRepresentativePoints
	 */
	public boolean isUsingOnlyRepresentativePoints() {
		return usingOnlyRepresentativePoints;
	}

	/**
	 * @param useOnlyRepresentativePoints
	 *            the useOnlyRepresentativePoints to set
	 */
	public void setUsingOnlyRepresentativePoints(
			boolean useOnlyRepresentativePoints) {
		this.usingOnlyRepresentativePoints = useOnlyRepresentativePoints;
	}

	/**
	 * @return the queryClass
	 */
	public Class<? extends StandardEntity> getQueryClass() {
		return queryClass;
	}

	/**
	 * @param queryClass
	 *            the queryClass to set
	 */
	public void setQueryClass(Class<? extends StandardEntity> queryClass) {
		this.queryClass = queryClass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Query[c:" + queryClass.getName() + ",d:" + distance + ",g:"
				+ geometry + ",rep:" + usingOnlyRepresentativePoints + "]";
	}
}
