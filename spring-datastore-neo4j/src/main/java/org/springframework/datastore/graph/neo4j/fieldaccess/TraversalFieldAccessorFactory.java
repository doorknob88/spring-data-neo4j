package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.annotations.GraphTraversal;
import org.springframework.datastore.graph.api.FieldTraversalDescriptionBuilder;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.finder.Finder;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.doReturn;

@Configurable
public class TraversalFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
    @Autowired
    private FinderFactory finderFactory;

    @Override
    public boolean accept(final Field f) {
        final GraphTraversal graphEntityTraversal = f.getAnnotation(GraphTraversal.class);
        return graphEntityTraversal != null
                && graphEntityTraversal.traversalBuilder() != FieldTraversalDescriptionBuilder.class
                && f.getType().equals(Iterable.class);
    }


    @Override
    public FieldAccessor<NodeBacked, ?> forField(final Field field) {
        return new TraversalFieldAccessor(field, finderFactory);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class TraversalFieldAccessor implements FieldAccessor<NodeBacked, Object> {
	    protected final Field field;
	    private final FinderFactory finderFactory;
	    private final FieldTraversalDescriptionBuilder fieldTraversalDescriptionBuilder;
	    private Class<? extends NodeBacked> target;

	    public TraversalFieldAccessor(final Field field, FinderFactory finderFactory) {
	        this.field = field;
	        this.finderFactory = finderFactory;
            final GraphTraversal graphEntityTraversal = field.getAnnotation(GraphTraversal.class);
	        this.target = graphEntityTraversal.elementClass();
	        this.fieldTraversalDescriptionBuilder = createTraversalDescription(graphEntityTraversal);
	    }

	    @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return false;
	    }

	    @Override
	    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
	        throw new InvalidDataAccessApiUsageException("Cannot set readonly traversal description field " + field);
	    }

	    @Override
	    public Object getValue(final NodeBacked nodeBacked) {
	        final Finder<? extends NodeBacked> finder = finderFactory.getFinderForClass(target);
	        final TraversalDescription traversalDescription = fieldTraversalDescriptionBuilder.build(nodeBacked,field);
	        return doReturn(finder.findAllByTraversal(nodeBacked, traversalDescription));
	    }


	    private FieldTraversalDescriptionBuilder createTraversalDescription(final GraphTraversal graphEntityTraversal) {
	        try {
	            final Class<? extends FieldTraversalDescriptionBuilder> traversalDescriptionClass = graphEntityTraversal.traversalBuilder();
	            final Constructor<? extends FieldTraversalDescriptionBuilder> constructor = traversalDescriptionClass.getDeclaredConstructor();
	            constructor.setAccessible(true);
	            return constructor.newInstance();
	        } catch (Exception e) {
	            throw new RuntimeException("Error creating TraversalDescription from " + field,e);
	        }
	    }

	}
}
