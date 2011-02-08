package iamrescue.belief.spatial;

import java.util.Collection;

import rescuecore2.standard.entities.StandardEntity;

public interface ISpatialIndex {
    /**
     * Returns all objects that satisfy the given query.
     * 
     * @param query
     *            The query (see SpatialQuery for details).
     * @return All objects that match the query.
     */
    public Collection<StandardEntity> query(SpatialQuery query);

}
