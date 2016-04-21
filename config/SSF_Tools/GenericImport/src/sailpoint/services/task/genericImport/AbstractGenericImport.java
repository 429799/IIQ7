package sailpoint.services.task.genericImport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Rule;
import sailpoint.tools.GeneralException;

/**
 * Abstract Generic Import class gives the default methods for
 * import classes to extend.
 * 
 * 19/Aug/2014	1.0	Initial release
 * 
 * @author christian.cairney
 * @version 1.0
 * @since 0.1 (Pre-release)
 *
 */
public abstract class AbstractGenericImport implements GenericImport {

	private Rule ruleTransform = null;						// Stores the transform rule
	private Rule ruleRow = null;							// Stores the per row rule
	private Rule ruleInit = null;							// Stores the Initialization rule
	private Rule ruleFinalize = null;						// Stores the Finalize rule
	private SailPointContext context = null;				// The SailPoint context
	private Attributes<String, Object> attributes = null;	// Attributes used to define the import
															// parameters.
	private List<String> groupBy = new ArrayList<String>();		// Group the fields up per row
	private List<String> mvFields = new ArrayList<String>();	// Designate the multi valued fields
	
	// Our friend the logger.
	private static Logger log = Logger.getLogger(AbstractGenericImport.class);
	// Common delimiter used as by default
	public static final String STRING_DELIMITER = ";";
	public static final String LOGGER = "logger";
	
	/**
	 * The constructor, this should be called by
	 * all extending classes, as this would be super() ! :)
	 * 
	 * @throws GeneralException
	 */
	public AbstractGenericImport() throws GeneralException {
		
		log.debug("Entering AbstractGenericImport constructor");
		
		// Create the new attributes map just in case
		attributes = new Attributes<String,Object>();
		try {
			context = sailpoint.api.SailPointFactory.getCurrentContext();
		} catch (GeneralException e) {
			// Cannot get SailPoint's current context.
			throw new GeneralException("Could not get current context in AbstractGenericImport", e);
		}
		
		log.debug("Exiting AbstractGenericImport constructor");
	
	}

	/**
	 * Retrieve the rule object by name, used by
	 * the byName setters
	 * 
	 * @param ruleName
	 * @return	A SailPointObject Rule
	 * @throws GeneralException
	 */
	private Rule getRuleFromName(String ruleName) throws GeneralException {
		
		Rule rule = context.getObjectByName(Rule.class, ruleName);
		return rule;
		
	}
	
	/** 
	 * Grab the Transform rule if one exists
	 */
	public Rule getTransformRule() {
		return ruleTransform;
	}
	/**
	 * Set the transform rule by the rule name
	 */
	public void setTransformRuleName(String ruleName) throws GeneralException {
		setTransformRule(getRuleFromName(ruleName));
	}
	/**
	 * Set the transform rule by rule object
	 */
	public void setTransformRule(Rule rule) {
		ruleTransform = rule;
	}
	/**
	 * Get the row rule is it exists
	 */
	public Rule getRowRule() {
		return ruleRow;
	}
	/**
	 * Set the row rule by rule object
	 */
	public void setRowRule(Rule rule) {
		ruleRow = rule;
	}
	/**
	 * Set the row rule by rule name
	 */
	public void setRowRuleName(String ruleName) throws GeneralException {
		setRowRule(getRuleFromName(ruleName));
	}
	/**
	 * get the Initialization rule if one exists
	 */
	public Rule getInitRule() {
		return ruleInit;
	}
	/**
	 * Set the initialization rule by rule object
	 */
	public void setInitRule(Rule rule) {
		ruleInit = rule;
	}
	/**
	 * Set the initialization rule by rule name
	 */
	public void setInitRuleName(String ruleName) throws GeneralException {
		setInitRule(getRuleFromName(ruleName));
	}
	/**
	 * Get the finalization rule if one exists
	 */
	public Rule getFinalizeRule() {
		return ruleFinalize;
	}
	/**
	 * Set the finalization rule by rule object
	 */
	public void setFinalizeRule(Rule rule) {
		ruleFinalize = rule;
	}
	/**
	 * Set the finalization rule by rule name
	 */
	public void setFinalizeRuleName(String ruleName) throws GeneralException {
		setFinalizeRule(getRuleFromName(ruleName));
	}
	
	/**
	 * Attributes hold the specific configuration for each type of
	 * importer.
	 */
	public Attributes<String,Object> getAttributes() {
		return this.attributes;
	}
	/**
	 * 
	 */
	public void setAttributes(Attributes<String,Object> attributes) {	
		
		this.attributes = attributes;
		
	}
	/**
	 * Set the group by attributes
	 */
	public void setGroupBy(List<String> groupBy) {
		if (groupBy == null) {
			this.groupBy.clear();
		} else {
			this.groupBy = groupBy;
		}
	}
	
	/**
	 * Get the group by attributes.
	 * @return	List<String> object of at least 0 in size
	 */
	public List<String> getGroupBy() {
		return this.groupBy;
	}
	
	/**
	 * Get a list of multi valued fields
	 */
	public List<String> getMvFields() {
		return mvFields;
	}

	/**
	 * Set a list of Multi valued fields
	 */
	public void setMvFields(List<String> mvFields) {
		if (mvFields == null) {
			this.mvFields.clear();
		} else { 
			this.mvFields = mvFields;
		}
	}
	
	/**
	 * Abstract 
	 */
	public abstract void open() throws GeneralException;
	public abstract void close() throws GeneralException;
	public abstract Iterator<HashMap<String,Object>> iterator();
	
}
