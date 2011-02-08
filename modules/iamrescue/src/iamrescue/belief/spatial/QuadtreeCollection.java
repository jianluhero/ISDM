package iamrescue.belief.spatial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rescuecore2.standard.entities.StandardEntity;

import com.vividsolutions.jts.index.quadtree.Quadtree;

public class QuadtreeCollection {
    private List<Quadtree> quadtrees;
    private List<Collection<Class<? extends StandardEntity>>> classes;

    public QuadtreeCollection() {
        this.quadtrees = new ArrayList<Quadtree>();
        this.classes = new ArrayList<Collection<Class<? extends StandardEntity>>>();
    }

    public void createNewQuadtree(
            Collection<Class<? extends StandardEntity>> responsibleClasses) {
        for (Class<? extends StandardEntity> newClass : responsibleClasses) {
            for (Collection<Class<? extends StandardEntity>> collection : classes) {
                for (Class<? extends StandardEntity> already : collection) {
                    if (already.isAssignableFrom(newClass)
                            || newClass.isAssignableFrom(already)) {
                        throw new IllegalArgumentException(
                                "Adding the class "
                                        + newClass
                                        + " would create overlapping quadtrees, as "
                                        + "there is already a quadtree responsible for the class "
                                        + already);
                    }
                }
            }
        }
        quadtrees.add(new Quadtree());
    }
}
