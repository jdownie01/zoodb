package net.sf.oval.constraint;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;

import javax.jdo.ObjectState;
import javax.jdo.Query;

import ch.ethz.oserb.ConstraintManager;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

public class ForeignKeyCheck extends AbstractAnnotationCheck<ForeignKey>{

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -6157962048692406325L;
	
	private Class<?> clazz;
	private String attr;
	
	@Override
	public void configure(final ForeignKey foreignKeyAnnotation)
	{
		super.configure(foreignKeyAnnotation);
		this.clazz = foreignKeyAnnotation.clazz();
		this.attr = foreignKeyAnnotation.attr();		
	}
			
	@Override
	public boolean isSatisfied(Object validatedObject, Object valueToValidate, OValContext context, Validator validator) throws OValException {
		// get instance of constraint manager to query db
		ConstraintManager cm;
		try {
			cm = ConstraintManager.getInstance();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		// check db for corresponding entry
		String filter = attr+"=="+valueToValidate;
		Query query = cm.newQuery (clazz, filter);
		@SuppressWarnings("unchecked")
		List<Object> results = (List<Object>) query.execute();
		for(Object obj:results){
			// if there is an object with the same composite key which is the object to validate->return false
			if(!obj.equals(validatedObject))return true;
		}
		
		// check managed object for corresponding entry
		try {
			Field field = clazz.getDeclaredField(attr);
			field.setAccessible(true);
			for(Object obj:cm.getManagedObjects(EnumSet.of(ObjectState.PERSISTENT_DIRTY, ObjectState.PERSISTENT_NEW),clazz)){
				if(obj.equals(validatedObject))continue;
				if(field.get(obj)==valueToValidate)return true;
			}
		}catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		
		// if no corresponding object found
		return false;
	}

}
