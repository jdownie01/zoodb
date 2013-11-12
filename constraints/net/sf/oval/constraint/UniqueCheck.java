package net.sf.oval.constraint;

import static net.sf.oval.Validator.getCollectionFactory;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.jdo.ObjectState;
import javax.jdo.Query;

import ch.ethz.oserb.ConstraintManager;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

public class UniqueCheck extends AbstractAnnotationCheck<Unique>{
	
	/**
	 * generated serial version uid.
	 */
	private static final long serialVersionUID = 2857169880015248488L;
	private String[] attributes;
	private Map<String, Object> attributeSet;
	
	@Override
	public void configure(final Unique uniqueAnnotation)
	{
		super.configure(uniqueAnnotation);
		this.attributes = uniqueAnnotation.attr();
		attributeSet = getCollectionFactory().createMap(attributes.length);
		requireMessageVariablesRecreation();
	}
		
	@Override
	public boolean isSatisfied(Object validatedObject, Object valueToValidate, OValContext context, Validator validator) throws OValException {
		StringBuilder filter = new StringBuilder();
		Class<?> clazz = validatedObject.getClass();
		// setup composite key set and filter
		try{
			for(String attribute:attributes){
				Field field = clazz.getDeclaredField(attribute);
				field.setAccessible(true);
				attributeSet.put(attribute, field.get(validatedObject));
				filter.append(attribute+"=="+field.get(validatedObject)+" && ");
			}
			filter.delete(filter.length()-4, filter.length());
		}catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}
		
		// get instance of constraint manager to query db
		ConstraintManager cm;
		try {
			cm = ConstraintManager.getInstance();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		
		// unique
			
		// check db for corresponding entry
		Query query = cm.newQuery (validatedObject.getClass(), filter.toString());
		@SuppressWarnings("unchecked")
		List<Object> results = (List<Object>) query.execute();
		for(Object obj:results){
			// if there is an object with the same composite key which is the object to validate->return false
			if(!obj.equals(validatedObject))return false;
		}
		
		// check managed object for corresponding entry
		Map<String, Object> keySetOther = getCollectionFactory().createMap(attributes.length);
		try {
			for(Object obj:cm.getManagedObjects(EnumSet.of(ObjectState.PERSISTENT_DIRTY, ObjectState.PERSISTENT_NEW),clazz)){
				for(String attribute:attributes){
					// if the current object is the object to validate->skip
					if(obj.equals(validatedObject))continue;
					Field field = clazz.getDeclaredField(attribute);
					field.setAccessible(true);
					keySetOther.put(attribute, field.get(obj));
				}
				if(attributeSet.equals(keySetOther))return false;
			}
		}catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		
		// if no corresponding object found*/
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> createMessageVariables()
	{
		final Map<String, String> messageVariables = getCollectionFactory().createMap(2);
		messageVariables.put("attr", attributeSet.entrySet().toString());
		return messageVariables;
	}

}
