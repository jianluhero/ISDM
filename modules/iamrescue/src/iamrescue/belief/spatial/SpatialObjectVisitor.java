package iamrescue.belief.spatial;

import iamrescue.util.SpatialUtils;

import java.util.List;
import java.util.Map;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.ItemVisitor;

public class SpatialObjectVisitor implements ItemVisitor {

	private final Geometry queryGeometry;
	private final boolean usingRepresentativePoints;
	private final double distance;
	private final Class<? extends StandardEntity> queryClass;
	private List<StandardEntity> list;
	private Map<EntityID, Geometry> geometries;
	private StandardWorldModel worldModel;

	public SpatialObjectVisitor(SpatialQuery query,
			List<StandardEntity> listToFill,
			Map<EntityID, Geometry> geometries, StandardWorldModel worldModel) {
		this.queryGeometry = query.getGeometry();
		this.usingRepresentativePoints = query
				.isUsingOnlyRepresentativePoints();
		this.distance = query.getDistance();
		this.queryClass = query.getQueryClass();
		this.geometries = geometries;
		this.worldModel = worldModel;
		this.list = listToFill;
	}

	public void visitItem(Object object) {
		StandardEntity spObj = (StandardEntity) object;
		if (queryClass.isAssignableFrom(spObj.getClass())) {
			Geometry objectGeometry;
			if (usingRepresentativePoints) {
				objectGeometry = SpatialUtils.convertApexes(spObj
						.getLocation(worldModel));
			} else {
				objectGeometry = geometries.get(spObj.getID());
				if (objectGeometry == null) {
					objectGeometry = SpatialUtils.createGeometry(spObj,
							worldModel);
					geometries.put(spObj.getID(), objectGeometry);
				}
			}
			if (queryGeometry.intersects(objectGeometry)
					|| queryGeometry.distance(objectGeometry) <= distance) {
				list.add(spObj);
			}
		}
	}
}
