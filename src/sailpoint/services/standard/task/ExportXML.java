package sailpoint.services.standard.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;






import sailpoint.task.AbstractTaskExecutor;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;
import sailpoint.tools.xml.AbstractXmlObject;
import sailpoint.api.SailPointContext;
import sailpoint.object.*;
import sailpoint.server.Exporter.Cleaner;

/**
 * Export XML objects from IIQ
 *
 * @author <a href="mailto:paul.wheeler@sailpoint.com">Paul Wheeler</a>
 */

public class ExportXML extends AbstractTaskExecutor {
	private static Log log = LogFactory.getLog(ExportXML.class);
	
    /**
     * Path that we will output directory structure to
     */
    public static final String ARG_BASE_PATH = "basePath";
    
    /**
     * Remove IDs and creation/modification timestamps if true
     */
	public static final String ARG_REMOVE_IDS = "removeIDs";
	
    /**
     * Comma-separated list of classes that we will export (all if blank)
     */
	public static final String ARG_CLASS_NAMES = "classNames";
	
    /**
     * Only export object if created or modified after this date (all dates if blank)
     */
    public static final String ARG_FROM_DATE = "fromDate";

    /**
     * Target properties file for reverse lookup of tokens that will replace matched text
     */
    public static final String ARG_TARGET_PROPS_FILE = "targetPropsFile";

    /**
     * Custom naming format for exported file using these tokens:
     * $Class$ = Object Class, $Name$ = Object Name
     */
    public static final String ARG_CUSTOM_NAMING_FORMAT = "namingFormat";
    
	String _basePath;
	boolean _removeIDs;
	List<String> _classNames = new ArrayList<String>();
	Date _fromDate;
	String _targetPropsFile;
	String _namingFormat;
	

	boolean terminate = false;
	int totalExported = 0;
	String exportDetails = null;
	int classObjectsExported = 0;
	

	@SuppressWarnings("unchecked")
	public void execute( SailPointContext context, TaskSchedule schedule, TaskResult result, Attributes<String, Object> args) throws Exception {

    //    try {

	    	 log.debug("Starting XML exporter...");
	    	 // Get arguments
	    	 _basePath = args.getString(ARG_BASE_PATH);
	    	 _removeIDs = args.getBoolean(ARG_REMOVE_IDS);
	    	 _classNames = args.getStringList(ARG_CLASS_NAMES);
	    	 _fromDate = args.getDate(ARG_FROM_DATE);
	    	 _targetPropsFile = args.getString(ARG_TARGET_PROPS_FILE);
	    	 _namingFormat = args.getString(ARG_CUSTOM_NAMING_FORMAT);
	    	 
        	 log.debug("Base path: " + _basePath);
        	 log.debug("Remove IDs: " + _removeIDs);
        	 log.debug("Class names: " + _classNames);
        	 log.debug("From date: " + _fromDate);
        	 log.debug("Target properties file: " + _targetPropsFile);
        	 log.debug("Naming format: " + _namingFormat);
	    	 
	    	 Map tokenMap = new HashMap();
	    	 
	    	 if (null != _targetPropsFile) {
        	 
	    	    BufferedReader br = new BufferedReader(new FileReader(_targetPropsFile));
	    	    try {
	    	        String line = br.readLine();
	
	    	        while (line != null) {
	    	        	if (line.startsWith("%%") && line.contains("=") && !line.contains("%%TARGET%%")) {
	    	        		String[] splitLine = line.split("=", 2);
	    	        		tokenMap.put(splitLine[0], splitLine[1]);
	    	        	}
	    	            line = br.readLine();
	    	        }
	
	    	    } finally {
	    	        br.close();
	    	    }
	    	 }
	    	 
	    	      	 
        	 // Convert backslashes to forward slashes (will still work in Windows)
        	 _basePath = _basePath.replaceAll("\\\\", "/");
        	 if (!_basePath.endsWith("/")) {
				 _basePath = _basePath + "/";
			 }
        	 
        	 
        	 // If there's no fromDate assume we want all objects
			 if (null == _fromDate) {
				 _fromDate = new Date(Long.MIN_VALUE);
			 }
        	 
        	 if (null != _classNames && _classNames.size() > 0) {
        		 // If we find "default" in the list we need to merge in the default set of classes
        		 if (_classNames.contains("default")) {
        			 String defaultClasses = "Application,AuditConfig,Capability,Configuration,CorrelationConfig,Custom,DashboardContent,DynamicScope,EmailTemplate,Form,FullTextIndex,GroupFactory,IdentityTrigger,IntegrationConfig,LocalizedAttribute,ObjectConfig,Policy,QuickLink,Rule,ScoreConfig,TaskDefinition,TaskSchedule,UIConfig,Workflow";
        			 List<String> defaultClassList = Arrays.asList(defaultClasses.split(","));
        			 // Use a set to ensure we don't have duplicates if the user has added any default classes to the list
        			 HashSet<String> mergedClassNames = new HashSet<String>(_classNames);
        			 mergedClassNames.addAll(defaultClassList);
        			 _classNames.clear();
        			 _classNames.addAll(mergedClassNames);
        			 _classNames.remove("default");
        		 }
	        	 for (String className : _classNames) {
	        		 updateProgress(context, result, "Exporting class " + className);
	        		 exportClassObjects(context, className, tokenMap);
	        	 }

        	 } else {
  
	        	 Class<?>[] allClasses = ClassLists.MajorClasses;
	             for (int i = 0 ; i < allClasses.length ; i++) {
	                 log.debug(allClasses[i].getName());
	                 String className = allClasses[i].getSimpleName();
	                 updateProgress(context, result, "Exporting class " + className);
	                 exportClassObjects(context, className, tokenMap);
	             }
        	 }
        	 result.setAttribute("exportDetails", exportDetails);
        	 result.setAttribute("objectsExported", totalExported);

	}
	
	private void exportClassObjects(SailPointContext context, String className, Map<String, String> tokenMap) throws GeneralException {
	
		log.debug("Starting export of class " + className);
		String fullClassName = "sailpoint.object." + className;
		Class currentClass = null;
		try {
			currentClass = Class.forName(fullClassName);
		
		} catch (Exception e) {
			StringBuffer sb = new StringBuffer();
            sb.append("Could not find class: ");
            sb.append(fullClassName);
            throw new GeneralException(sb.toString());
		}
   
		QueryOptions qo = new QueryOptions();
		Filter dateFilter = Filter.or(Filter.ge("created", _fromDate), Filter.ge("modified", _fromDate));
		qo.add(dateFilter);
		 
		List<Object> objects = new ArrayList<Object>();
		try {
			objects = context.getObjects(currentClass, qo);
		} catch (Exception e) {
			if (e.getMessage().contains("could not resolve property:")) {
				log.debug("Ignoring class " + className + " as it has no created or modified property");
				return;
			}
			
		}

		if (null != objects && !objects.isEmpty()) {
			File dir = new File(_basePath + className);		 
			// Create object directory if it doesn't exist
			if (!dir.exists()) {
				 if (dir.mkdirs()) { 
					 log.debug("Created directory " + dir.getPath());
				 } else {
					 log.error("Could not create directory " + dir.getPath() + "!");
				 }
			} else {
				 log.debug("Directory " + dir.getPath() + " already exists");
			}      
		}
		 
		classObjectsExported = 0;
	   	for (Object object : objects) {
	   		String objectName = ((SailPointObject) object).getName();
	   		if (null == objectName) {
	   			objectName = ((SailPointObject) object).getId(); 
	   		}
	   		// Replace all illegal filename characters and spaces with underscore
	   		objectName = objectName.replaceAll("[^a-zA-Z0-9.-]", "_");
	   		String fileName;
	   		if (null != _namingFormat) {
	   			fileName = _namingFormat;
	   			fileName = fileName.replaceAll("\\$Name\\$", objectName);
	   			fileName = fileName.replaceAll("\\$Class\\$", className);
	   		} else {
	   			fileName = objectName + ".xml";
	   		}
	   		String xml = ((AbstractXmlObject) object).toXml();
	   		List<String> propertiesToClean = new ArrayList<String>(); 
	   		if (_removeIDs) {
	   			propertiesToClean.add("id");
        		propertiesToClean.add("created");
        		propertiesToClean.add("modified");
        		propertiesToClean.add("targetId");
        		propertiesToClean.add("assignedScopePath");
	   		}
	   		Cleaner cleaner = new Cleaner(propertiesToClean);
			xml = cleaner.clean(xml);
			if (className.equals("IdentityTrigger") && !propertiesToClean.isEmpty()) {
				// Workaround for bug #16553
				int ind = xml.indexOf("value=\"");
				if (ind != -1) {
					String workflowId = xml.substring(ind + 7, ind + 39);
					Workflow workflow = context.getObjectById(Workflow.class, workflowId);
					if (null != workflow) {
						String workflowName = workflow.getName();
						log.debug("Replacing workflow id " + workflowId + " with workflow name " + workflowName);
						xml = xml.replaceAll(workflowId, workflowName);
					}
				}
			}
			if (className.equals("Scope") && !propertiesToClean.isEmpty()) {
				// Workaround for issue with Scope paths being exported as IDs
				int ind = xml.indexOf("path=\"");
				if (ind != -1) {
					String pathId = xml.substring(ind + 6, ind + 38);
					Scope scope = context.getObjectById(Scope.class, pathId);
					if (null != scope) {
						String scopeName = scope.getName();
						log.debug("Replacing path id " + pathId + " with scope name " + scopeName);
						xml = xml.replaceAll(pathId, scopeName);
					}
				}
			}
			
			if (!tokenMap.isEmpty()) {
			    Iterator<Entry<String, String>> it = tokenMap.entrySet().iterator();
			    while (it.hasNext()) {
			        Map.Entry pairs = (Map.Entry)it.next();
			        String token = (String) pairs.getKey();
			        String value = (String) pairs.getValue();
			        String containsValue = value.replace("\\\\", "\\");  // Deal with backslashes
			        log.debug("Checking for token value " + value);
			        if (xml.contains(containsValue)) {
			        	// Escape regex special characters
			        	String replaceValue = value.replaceAll("\\\\", "\\\\");  
				        replaceValue = replaceValue.replaceAll("\\+", "\\\\+");
				        replaceValue = replaceValue.replaceAll("\\^", "\\\\^");
				        replaceValue = replaceValue.replaceAll("\\$", "\\\\" + Matcher.quoteReplacement("$")); // $ sign has a special meaning in replaceAll
				        replaceValue = replaceValue.replaceAll("\\.", "\\\\.");
				        replaceValue = replaceValue.replaceAll("\\|", "\\\\|");
				        replaceValue = replaceValue.replaceAll("\\?", "\\\\?");
				        replaceValue = replaceValue.replaceAll("\\*", "\\\\*");
				        replaceValue = replaceValue.replaceAll("\\(", "\\\\(");
				        replaceValue = replaceValue.replaceAll("\\)", "\\\\)");
				        replaceValue = replaceValue.replaceAll("\\[", "\\\\[");
				        replaceValue = replaceValue.replaceAll("\\{", "\\\\{");				        
			        	log.debug("Found value " + value + ", replacing with token " + token);
			        	xml = xml.replaceAll(replaceValue, token);
			        }
			    }
	   		}

			// Ensure the file opens nicely in Notepad if anyone cares.
			// In some cases we will just have a LF at the end of the line but in others we will already have CRLF
			// so make them all LF, then replace with CRLF.
			xml = xml.replaceAll("\\r\\n", "\n");
			xml = xml.replaceAll("\\n", "\r\n");
			
			log.debug("Exporting " + className + " " + objectName + " to " + _basePath + className + "/" + fileName);
	   		Util.writeFile(_basePath + className + "/" + fileName, xml);
	   		totalExported++;
	   		classObjectsExported++;
	   	}
		if (classObjectsExported > 0) {
    		 if (null == exportDetails) {
    			 exportDetails = className + ": " + classObjectsExported;
    		 } else {
    			 exportDetails = exportDetails + ", " + className + ": " + classObjectsExported;
    		 }
		}
	}
	

	
	public boolean terminate() {

		terminate = true;

		return terminate;
	}

}
