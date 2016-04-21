package sailpoint.services.task.genericImport;

import java.util.Iterator;
import java.util.List;
import sailpoint.object.Attributes;
import sailpoint.object.Rule;
import sailpoint.tools.GeneralException;

/**
 * Generic Import interface, defines the signature for this class, implemented in the
 * Abastact Generic Import class to define default behaviour.
 * 
 * @author christian.cairney
 *
 */
public interface GenericImport {
	
	public Rule getTransformRule();
	
	public void setTransformRuleName(String ruleName) throws GeneralException;
	
	public void setTransformRule(Rule rule);
	
	public Rule getRowRule();
	
	public void setRowRule(Rule rule);
	
	public void setRowRuleName(String ruleName) throws GeneralException;
	
	public Rule getInitRule();
	
	public void setInitRule(Rule rule);
	
	public void setInitRuleName(String ruleName) throws GeneralException;
	
	public Rule getFinalizeRule();
	
	public void setFinalizeRule(Rule rule);
	
	public void setFinalizeRuleName(String ruleName) throws GeneralException;
	
	public Attributes<String,Object> getAttributes();
	
	public void setAttributes(Attributes<String,Object> attributes);
	
	public void setGroupBy(List<String> groupBy);
	
	public List<String> getGroupBy();
	
	public void setMvFields(List<String> mvFieldNames);
	
	public List<String> getMvFields();
	
	public void open() throws GeneralException;
	
	public void close() throws GeneralException;
	
	public Iterator iterator();
}
