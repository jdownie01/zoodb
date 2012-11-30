package org.zoodb.profiling.suggestion;

import java.util.List;

public class ReferenceShortcutSuggestion extends FieldSuggestion {
	
	private final String identifier = "REF_SHORTCUT";
	
	/**
	 * Name of the class which should be the target of the reference 
	 */
	private String refTarget;
	
	/**
	 * List of classnames (these classes would be omitted when traversing the suggested reference)
	 * Important: starting class is not in this list!
	 */
	private List<String> intermediates;

	
	
	public String getRefTarget() {
		return refTarget;
	}

	public void setRefTarget(String refTarget) {
		this.refTarget = refTarget;
	}

	public List<String> getIntermediates() {
		return intermediates;
	}

	public void setIntermediates(List<String> intermediates) {
		this.intermediates = intermediates;
	}
	
	public String getText() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Add reference on class");
		sb.append(getClazzName());
		sb.append(" with target ");
		sb.append(refTarget);
		sb.append(" is faster than the path: ");
		sb.append(" {TODO: insert path}");
		
		return sb.toString();
	}
	
	
	public String provideLabelForColumn(int columnIndex) {
		switch(columnIndex) {
			case 0:
				return getText();
			case 1:
				return getClazzName();
			default:
				return null;
		}
	}


}