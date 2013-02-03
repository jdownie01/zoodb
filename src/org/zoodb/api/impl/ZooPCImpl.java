/*
 * Copyright 2009-2011 Tilmann Z�schke. All rights reserved.
 * 
 * This file is part of ZooDB.
 * 
 * ZooDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ZooDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ZooDB.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the README and COPYING files for further information. 
 */
package org.zoodb.api.impl;

import java.lang.reflect.Field;
import java.util.Collection;

import javax.jdo.JDOUserException;
import javax.jdo.ObjectState;
import javax.jdo.PersistenceManager;

import org.zoodb.jdo.api.DBArrayList;
import org.zoodb.jdo.internal.Node;
import org.zoodb.jdo.internal.Session;
import org.zoodb.jdo.internal.ZooClassDef;
import org.zoodb.jdo.internal.ZooFieldDef;
import org.zoodb.jdo.internal.client.PCContext;
import org.zoodb.jdo.internal.util.Util;
import org.zoodb.jdo.spi.PersistenceCapableImpl;
import org.zoodb.jdo.spi.StateManagerImpl;
import org.zoodb.profiling.api.AbstractActivation;
import org.zoodb.profiling.api.Activation;
import org.zoodb.profiling.api.FieldAccess;
import org.zoodb.profiling.api.impl.ProfilingManager;

/**
 * This is the common super class of all persistent classes.
 * It is separate from PersistenceCapabaleImpl to allow easier separation
 * from JDO. For example, PersistenceCapableImpl implements the
 * PersistenceCapable interface.
 * 
 * @author Tilmann Z�schke
 */
public abstract class ZooPCImpl {

	private static final byte PS_PERSISTENT = 1;
	private static final byte PS_TRANSACTIONAL = 2;
	private static final byte PS_DIRTY = 4;
	private static final byte PS_NEW = 8;
	private static final byte PS_DELETED = 16;
	private static final byte PS_DETACHED = 32;

//	public enum STATE {
//		TRANSIENT, //optional JDO 2.2
//		//TRANSIENT_CLEAN, //optional JDO 2.2
//		//TRANSIENT_DIRTY, //optional JDO 2.2
//		PERSISTENT_NEW,
//		//PERSISTENT_NON_TRANS, //optional JDO 2.2
//		//PERSISTENT_NON_TRANS_DIRTY, //optional JDO 2.2
//		PERSISTENT_CLEAN,
//		PERSISTENT_DIRTY,
//		HOLLOW,
//		PERSISTENT_DELETED,
//		PERSISTENT_NEW_DELETED,
//		DETACHED_CLEAN,
//		DETACHED_DIRTY
//		;
//	}
	
	
	//store only byte i.o. reference!
	//TODO store only one of the following?
	private transient ObjectState status;
	private transient byte stateFlags;
	
	private transient PCContext context;
	
	private transient long[] prevValues = null;
	
	//profiling fields
	private transient ZooPCImpl activationPathPredecessor = null;
	private transient boolean activeAndQueryRoot;
	private transient int pageId = -1;
	private transient String predecessorField = null;
	private transient AbstractActivation activation;
	//end profiling fields
	
	public String getPredecessorField() {
		return predecessorField;
	}
	public void setPredecessorField(String predecessorField) {
		this.predecessorField = predecessorField;
	}
	public AbstractActivation getActivation() {
		return activation;
	}
	public void setActivation(AbstractActivation activation) {
		this.activation = activation;
	}
	
	public final boolean jdoZooIsDirty() {
		return (stateFlags & PS_DIRTY) != 0;
	}
	public final boolean jdoZooIsNew() {
		return (stateFlags & PS_NEW) != 0;
	}
	public final boolean jdoZooIsDeleted() {
		return (stateFlags & PS_DELETED) != 0;
	}
	public final boolean jdoZooIsTransactional() {
		return (stateFlags & PS_TRANSACTIONAL) != 0;
	}
	public final boolean jdoZooIsPersistent() {
		return (stateFlags & PS_PERSISTENT) != 0;
	}
	/**
	 * Not part of the JDO state API. This can also return true if the instance is pers-deleted.
	 * @return  if instance is hollow.
	 */
	public final boolean zooIsHollow() {
		return (stateFlags & PS_PERSISTENT) != 0;
	}
	public final Node jdoZooGetNode() {
		return context.getNode();
	}
	//not to be used from outside
	private final void setPersNew() {
		status = ObjectState.PERSISTENT_NEW;
		stateFlags = PS_PERSISTENT | PS_TRANSACTIONAL | PS_DIRTY | PS_NEW;
		context.getSession().internalGetCache().notifyDirty(this);
	}
	private final void setPersClean() {
		status = ObjectState.PERSISTENT_CLEAN;
		stateFlags = PS_PERSISTENT | PS_TRANSACTIONAL;
	}
	private final void setPersDirty() {
		status = ObjectState.PERSISTENT_DIRTY;
		stateFlags = PS_PERSISTENT | PS_TRANSACTIONAL | PS_DIRTY;
		context.getSession().internalGetCache().notifyDirty(this);
	}
	private final void setHollow() {
		status = ObjectState.HOLLOW_PERSISTENT_NONTRANSACTIONAL;
		stateFlags = PS_PERSISTENT;
	}
	private final void setPersDeleted() {
		status = ObjectState.PERSISTENT_DELETED;
		if ((stateFlags &= PS_TRANSACTIONAL) != 0) {
			stateFlags = PS_PERSISTENT | PS_TRANSACTIONAL | PS_DIRTY | PS_DELETED;
		} else {
			//This can happen if a hollow instance is deleted
			//See Test_091, where hollow deleted instances need to be loaded to remove their values
			//from indices.
			stateFlags = PS_PERSISTENT | PS_DIRTY | PS_DELETED;
		}
		context.getSession().internalGetCache().notifyDelete(this);
	}
	private final void setPersNewDeleted() {
		status = ObjectState.PERSISTENT_NEW_DELETED;
		stateFlags = PS_PERSISTENT | PS_TRANSACTIONAL | PS_DIRTY | PS_NEW | PS_DELETED;
		context.getSession().internalGetCache().notifyDelete(this);
	}
	private final void setDetachedClean() {
		status = ObjectState.DETACHED_CLEAN;
		stateFlags = PS_DETACHED;
	}
	private final void setDetachedDirty() {
		status = ObjectState.DETACHED_DIRTY;
		stateFlags = PS_DETACHED | PS_DIRTY;
	}
	private final void setTransient() {
		status = ObjectState.TRANSIENT; //TODO other transient states?
		stateFlags = 0;
		jdoZooOid = Session.OID_NOT_ASSIGNED;
	}
	public final void jdoZooMarkClean() {
		//TODO is that all?
		setPersClean();
		prevValues = null;
	}
//	public final void jdoZooMarkNew() {
//		ObjectState statusO = status;
//		if (statusO == ObjectState.TRANSIENT) {
//			setPersNew();
//		} else if (statusO == ObjectState.PERSISTENT_NEW) {
//			//ignore
//		} else { 
//		throw new IllegalStateException("Illegal state transition: " + status 
//				+ " -> Persistent New: " + Util.oidToString(jdoZooOid));
//		}
//	}
	public final void jdoZooMarkDirty() {
		ObjectState statusO = status;
		if (statusO == ObjectState.PERSISTENT_CLEAN) {
			setPersDirty();
			getPrevValues();
		} else if (statusO == ObjectState.PERSISTENT_NEW) {
			//is already dirty
			//status = ObjectState.PERSISTENT_DIRTY;
		} else if (statusO == ObjectState.PERSISTENT_DIRTY) {
			//is already dirty
			//status = ObjectState.PERSISTENT_DIRTY;
		} else if (statusO == ObjectState.HOLLOW_PERSISTENT_NONTRANSACTIONAL) {
			//refresh first, then make dirty
			zooActivateRead();
			jdoZooMarkDirty();
		} else {
			throw new IllegalStateException("Illegal state transition: " + status + "->Dirty: " + 
					Util.oidToString(jdoZooOid));
		}
	}
	public final void jdoZooMarkDeleted() {
		ObjectState statusO = status;
		if (statusO == ObjectState.PERSISTENT_CLEAN ||
				statusO == ObjectState.PERSISTENT_DIRTY) {
			setPersDeleted();
		} else if (statusO == ObjectState.PERSISTENT_NEW) {
			setPersNewDeleted();
		} else if (statusO == ObjectState.HOLLOW_PERSISTENT_NONTRANSACTIONAL) {
			setPersDeleted();
		} else if (statusO == ObjectState.PERSISTENT_DELETED 
				|| statusO == ObjectState.PERSISTENT_NEW_DELETED) {
			throw new JDOUserException("The object has already been deleted: " + 
					Util.oidToString(jdoZooOid));
		} else {
			throw new IllegalStateException("Illegal state transition: " + status + "->Deleted");
		}
	}
	public final void jdoZooMarkHollow() {
		//TODO is that all?
		setHollow();
		prevValues = null;
	}

	public final void jdoZooMarkTransient() {
		ObjectState statusO = status;
		if (statusO == ObjectState.TRANSIENT) {
			//nothing to do 
		} else if (statusO == ObjectState.PERSISTENT_CLEAN ||
				statusO == ObjectState.HOLLOW_PERSISTENT_NONTRANSACTIONAL) {
			setTransient();
		} else if (statusO == ObjectState.PERSISTENT_NEW) {
			throw new JDOUserException("The object is new.");
		} else if (statusO == ObjectState.PERSISTENT_DIRTY) {
			throw new JDOUserException("The object is dirty.");
		} else if (statusO == ObjectState.PERSISTENT_DELETED 
				|| statusO == ObjectState.PERSISTENT_NEW_DELETED) {
			throw new JDOUserException("The object has already been deleted: " + 
					Util.oidToString(jdoZooOid));
		} else {
			throw new IllegalStateException("Illegal state transition: " + status + "->Deleted");
		}
	}

	public final boolean jdoZooIsStateHollow() {
		return status == ObjectState.HOLLOW_PERSISTENT_NONTRANSACTIONAL;
	}

	public final PersistenceManager jdoZooGetPM() {
		return context.getSession().getPersistenceManager();
	}

	public final PCContext jdoZooGetContext() {
		return context;
	}

	public final ZooClassDef jdoZooGetClassDef() {
		return context.getClassDef();
	}

	public final void jdoZooEvict() {
		context.getEvictor().evict(this);
		jdoZooMarkHollow();
	}

	public final boolean jdoZooHasState(ObjectState state) {
		return this.status == state;
	}

	public final void jdoZooInit(ObjectState state, PCContext bundle, long oid) {
		this.context = bundle;
		jdoZooSetOid(oid);
		this.status = state;
		switch (state) {
		case PERSISTENT_NEW: { 
			setPersNew();
			break;
		}
		case PERSISTENT_CLEAN: { 
			setPersClean();
			break;
		}
		case HOLLOW_PERSISTENT_NONTRANSACTIONAL: { 
			setHollow();
			break;
		}
		default:
			throw new UnsupportedOperationException("" + state);
		}
		if (this instanceof PersistenceCapableImpl) {
			((PersistenceCapableImpl)this).jdoNewInstance(StateManagerImpl.STATEMANAGER);
		}
	}
	
	
	private final void getPrevValues() {
		if (prevValues != null) {
			throw new IllegalStateException();
		}
		ZooFieldDef[] fields = context.getClassDef().getAllFields();
		prevValues = new long[fields.length];
		prevValues = context.getIndexer().getBackup(this);
		
	}
	
	public long[] jdoZooGetBackup() {
		return prevValues;
	}

	
	//Specific to ZooDB
	public void setActivationPathPredecessor(ZooPCImpl predecessor) {
		this.activationPathPredecessor = predecessor;
	}
	
	public ZooPCImpl getActivationPathPredecessor() {
		return activationPathPredecessor;
	}
	
	public int getPageId() {
		return pageId;
	}
	
	public void setPageId(int pageId) {
		this.pageId = pageId;
	}
	
	/**
	 * This method ensures that the specified object is in the cache.
	 * 
	 * It should be called in the beginning of every method that reads persistent fields.
	 * 
	 * For generated calls, we should not forget private method, because they can be called
	 * from other instances.
	 */
	public final void zooActivateRead() {
		switch (status) {
		case HOLLOW_PERSISTENT_NONTRANSACTIONAL:
			//pc.jdoStateManager.getPersistenceManager(pc).refresh(pc);
			if (jdoZooGetPM().isClosed()) {
				throw new JDOUserException("The PersitenceManager of this object is not open.");
			}
			if (!jdoZooGetPM().currentTransaction().isActive()) {
				throw new JDOUserException("The PersitenceManager of this object is not active " +
						"(-> use begin()).");
			}
			jdoZooGetNode().refreshObject(this);
			
			return;
		case PERSISTENT_DELETED:
		case PERSISTENT_NEW_DELETED:
			throw new JDOUserException("The object has been deleted.");
		case PERSISTENT_NEW:
		case PERSISTENT_CLEAN:
		case PERSISTENT_DIRTY:
			//nothing to do
			return;
		case TRANSIENT:
		case TRANSIENT_CLEAN:
		case TRANSIENT_DIRTY:
			//not persistent yet
			return;

		default:
			throw new IllegalStateException("" + status);
			//break;
		}
//		//if (pc.jdoZooOid == null || pc.jdoZooOid.equals(Session.OID_NOT_ASSIGNED)) {
//		if (jdoZooOid == Session.OID_NOT_ASSIGNED) {
//			//not persistent yet
//			return;
//		}
//		if (isDeleted()) {
//			throw new JDOUserException("The object has been deleted.");
//		}
//		if (isStateHollow()) {
//			//pc.jdoStateManager.getPersistenceManager(pc).refresh(pc);
//			if (getPM().isClosed()) {
//				throw new JDOUserException("The PersitenceManager of this object is not open.");
//			}
//			getNode().refreshObject(this);
//		}
	}
	
	/**
	 * This method ensures that the specified object is in the cache and then flags it as dirty.
	 * It includes a call to zooActivateRead().
	 * 
	 * It should be called in the beginning of every method that writes persistent fields.
	 * 
	 * For generated calls, we should not forget private method, because they can be called
	 * from other instances.
	 */
	public final void zooActivateWrite() {
		switch (status) {
		case HOLLOW_PERSISTENT_NONTRANSACTIONAL:
			//pc.jdoStateManager.getPersistenceManager(pc).refresh(pc);
			if (jdoZooGetPM().isClosed()) {
				throw new JDOUserException("The PersitenceManager of this object is not open.");
			}
			if (!jdoZooGetPM().currentTransaction().isActive()) {
				throw new JDOUserException("The PersitenceManager of this object is not active " +
						"(-> use begin()).");
			}
			jdoZooGetNode().refreshObject(this);
			break;
		case PERSISTENT_DELETED:
		case PERSISTENT_NEW_DELETED:
			throw new JDOUserException("The object has been deleted.");
		case TRANSIENT:
		case TRANSIENT_CLEAN:
		case TRANSIENT_DIRTY:
			//not persistent yet
			return;

		default:
			break;
		}
//		zooActivateRead();
//		if (jdoZooOid == Session.OID_NOT_ASSIGNED) {
//			//not persistent yet
//			return;
//		}
		jdoZooMarkDirty();
	}
	
	public boolean isActiveAndQueryRoot() {
		return activeAndQueryRoot;
	}
	public void setActiveAndQueryRoot(boolean activeAndQueryRoot) {
		this.activeAndQueryRoot = activeAndQueryRoot;
	}
	
	public final void zooActivateWrite(String field) {
		//Here we can not skip loading the field to be loaded, because it may be read beforehand
		zooActivateWrite();
	}
	
	//	private long jdoZooFlags = 0;
    //TODO instead use some fixed value like INVALID_OID
	private transient long jdoZooOid = Session.OID_NOT_ASSIGNED;
	
//	void jdoZooSetFlag(long flag) {
//		jdoZooFlags |= flag;
//	}
//	
//	void jdoZooUnsetFlag(long flag) {
//		jdoZooFlags &= ~flag;
//	}
//	
//	boolean jdoZooFlagIsSet(long flag) {
//		return (jdoZooFlags & flag) > 0;
//	}
//	public void jdoZooSetDirty() { jdoZooSetFlag(StateManagerImpl.JDO_PC_DIRTY); }
//	public void jdoZooSetNew() { jdoZooSetFlag(StateManagerImpl.JDO_PC_NEW); }
//	public void jdoZooSetDeleted() { jdoZooSetFlag(StateManagerImpl.JDO_PC_DELETED); }
//	public void jdoZooSetPersistent() { jdoZooSetFlag(StateManagerImpl.JDO_PC_PERSISTENT); }
//	public void jdoZooSetDirtyNewFalse() { 
//		jdoZooUnsetFlag(StateManagerImpl.JDO_PC_DIRTY | StateManagerImpl.JDO_PC_NEW); 
//	}
	public final void jdoZooSetOid(long oid) { jdoZooOid = oid; }
	public final long jdoZooGetOid() { return jdoZooOid; }
	
	
	//TODO
	public ZooPCImpl() {
		super();
		setTransient();
		//jdoStateManager = StateManagerImpl.SINGLE;
	}
	
	@Override
	public String toString() {
		return super.toString() + " oid=" + Util.oidToString(jdoZooOid) + " state=" + status; 
	}
} // end class definition

