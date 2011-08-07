package org.zoodb.jdo;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jdo.Extent;
import javax.jdo.FetchPlan;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.spi.PersistenceCapable;

import org.zoodb.jdo.stuff.CloseableIterator;

/**
 * This class implements JDO behavior for the class Extend.
 * @param <T>
 * 
 * @author Tilmann Z�schke
 */
public class ExtentImpl<T> implements Extent<T> {
    
    private final Class<T> _class;
    private final boolean _subclasses;
    private final List<CloseableIterator<T>> _allIterators = new LinkedList<CloseableIterator<T>>();
    private final PersistenceManagerImpl _pManager;
    
    /**
     * @param persistenceCapableClass
     * @param subclasses
     * @param pm
     */
    public ExtentImpl(Class<T> persistenceCapableClass, 
            boolean subclasses, PersistenceManagerImpl pm) {
    	if (!PersistenceCapable.class.isAssignableFrom(persistenceCapableClass)) {
    		throw new JDOUserException("CLass is not persistence capabale: " + 
    				persistenceCapableClass.getName());
    	}
        _class = persistenceCapableClass;
        _subclasses = subclasses;
        _pManager = pm;
    }

    /**
     * @see org.zoodb.jdo.oldStuff.Extent#iterator()
     */
    public Iterator<T> iterator() {
    	@SuppressWarnings("unchecked")
		CloseableIterator<T> it = 
    		(CloseableIterator<T>) _pManager.getSession().loadAllInstances(_class, _subclasses);
    	_allIterators.add(it);
    	return it;
    }

    /**
     * @see org.zoodb.jdo.oldStuff.Extent#close(java.util.Iterator)
     */
    public void close(Iterator<T> i) {
        CloseableIterator.class.cast(i).close();
        _allIterators.remove(i);
    }

    /**
     * @see org.zoodb.jdo.oldStuff.Extent#closeAll()
     */
    public void closeAll() {
        for (CloseableIterator<T> i: _allIterators) {
            i.close();
        }
        _allIterators.clear();
    }

    /**
     * @see org.zoodb.jdo.oldStuff.Extent#hasSubclasses()
     */
    public boolean hasSubclasses() {
        return _subclasses;
    }

    /**
     * @see org.zoodb.jdo.oldStuff.Extent#getPersistenceManager()
     */
    public PersistenceManager getPersistenceManager() {
        return _pManager;
    }
    
	@Override
	public Class<T> getCandidateClass() {
		return _class;
	}

	@Override
	public FetchPlan getFetchPlan() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}
