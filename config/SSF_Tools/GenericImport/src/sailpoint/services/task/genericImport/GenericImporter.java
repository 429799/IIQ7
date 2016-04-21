package sailpoint.services.task.genericImport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Rule;
import sailpoint.object.TaskResult;
import sailpoint.tools.GeneralException;
import bsh.EvalError;
import bsh.Interpreter;

public class GenericImporter {

	private GenericImport genericImport;
	private Interpreter beanshell = null;
	private SailPointContext context = null;
	private Iterator<?> genericImportIterator;
	private TaskResult taskResult = null;
	private Attributes<String, Object> taskAttributes = null;

	// For group by
	private Attributes<String, Object> lastRow = null;
	private Attributes<String, Object> newRow = null;

	private static Logger log = Logger.getLogger(GenericImporter.class);

	public GenericImporter(GenericImport gi) throws GeneralException {

		genericImport = gi;
		clearGrouped();

		try {
			context = sailpoint.api.SailPointFactory.getCurrentContext();
		} catch (GeneralException e) {
			// Cannot get SailPoint's current context.
			throw new GeneralException(
					"Could not get current context in GenericImporter", e);
		}

		beanshell = new Interpreter();
		try {
			beanshell.eval("import sailpoint.services.task.genericImport.*;");
		} catch (EvalError e) {
			throw new GeneralException(
					"Somewhat unexpected, but there is a EvalError in AbstractGenericImport constructor",
					e);
		}

	}

	public TaskResult getTaskResult() {
		return taskResult;
	}

	public void setTaskResult(TaskResult taskResult) {
		this.taskResult = taskResult;
	}

	public Attributes<String, Object> getTaskAttributes() {
		return taskAttributes;
	}

	public void setTaskAttributes(Attributes<String, Object> taskAttributes) {
		this.taskAttributes = taskAttributes;
	}

	// Transform the schema and types from one value to another. No business
	// logic
	// should be here, just data type transformations
	private Attributes<String, Object> transformRow(
			Attributes<String, Object> row) throws GeneralException {

		Attributes<String, Object> transform = new Attributes<String, Object>();

		Rule ruleTransform = genericImport.getTransformRule();

		if (ruleTransform == null) {
			// Just copy the HashMap to a more generic
			// version
			for (String columnName : row.keySet()) {
				transform.put(columnName, row.get(columnName));
			}
			return transform;

		} else {
			// Pass to beanshell for the transformation

			try {
				beanshell.set("log", log);
				beanshell.set("context", context);
				beanshell.set("row", row);
				beanshell.set("taskResult", this.taskResult);
				beanshell.set("taskAttributes", this.taskAttributes);
				beanshell.set("transform", transform);
				
				transform = (Attributes<String, Object>) beanshell
						.eval(ruleTransform.getSource());

				if (log.isDebugEnabled()) {					
					if (transform != null) {
						log.debug("Transformed row is:");
						for (String key : transform.getKeys()) {
							String value = null;
							if (transform.get(key) != null)
								value = transform.get(key).toString();
							log.debug("  Key: '" + key + "' Value: '" + value + "'");
						}
					} else {
						log.debug("Transformed row is NULL");
					}
				}
				
				return transform;
			} catch (EvalError e) {
				throw new GeneralException(beanshellErrorReport(e,"transformRow"), e);
			}
		}
	}

	private void processRow(Attributes<String, Object> row)
			throws GeneralException {

		Rule ruleRow = genericImport.getRowRule();

		if (ruleRow == null) {
			if (log.isDebugEnabled())
				log.debug("No processRow rule available.");
		} else {

			try {
				beanshell.set("log", log);
				beanshell.set("context", context);
				beanshell.set("row", row);
				beanshell.set("taskResult", this.taskResult);
				beanshell.set("taskAttributes", this.taskAttributes);
				beanshell.eval(ruleRow.getSource());

			} catch (EvalError e) {
				throw new GeneralException(beanshellErrorReport(e,"processRow") , e);
			}
		}
	}

	private void initImport() throws GeneralException {

		Rule ruleInit = genericImport.getInitRule();

		if (ruleInit == null) {
			if (log.isDebugEnabled())
				log.debug("No initImport rule available.");
		} else {

			try {
				beanshell.set("log", log);
				beanshell.set("context", context);
				beanshell.set("taskResult", this.taskResult);
				beanshell.set("taskAttributes", this.taskAttributes);
				beanshell.eval(ruleInit.getSource());

			} catch (EvalError e) {
				throw new GeneralException(beanshellErrorReport(e,"initImport"), e);
			}
		}
	}

	public void finalizeImport() throws GeneralException {

		Rule ruleFinalize = genericImport.getFinalizeRule();

		if (ruleFinalize == null) {
			if (log.isDebugEnabled())
				log.debug("No finalizeImport rule available.");
		} else {

			try {
				beanshell.set("log", log);
				beanshell.set("context", context);
				beanshell.set("taskResult", this.taskResult);
				beanshell.set("taskAttributes", this.taskAttributes);
				beanshell.eval(ruleFinalize.getSource());

			} catch (EvalError e) {
				throw new GeneralException(beanshellErrorReport(e,"finalizeImport"), e);
			}
		}
	}

	private String beanshellErrorReport(EvalError e, String process) {
		
		String errorText = null;
		String errorLine = null;
		
		try {
			errorText = e.getErrorText();
		} catch(Exception e2) {
			errorText = null;
		}
		try {
			errorLine = String.valueOf(e.getErrorLineNumber());
		} catch(Exception e2) {
			errorLine = "0";
		}
		
		if (errorText == null) errorText = "(n/a)";
		
		String message;

		message = "Evaluation error in " + process + " Rule." +
			 "\n | Error line number " + errorLine + ", " 
			+ errorText + "\n | Message: " + e.getMessage();
		
		return message;
	}
	
	public void open() throws GeneralException {

		log.debug("Entering GenericImport open()");

		initImport();
		genericImport.open();
		genericImportIterator = genericImport.iterator();

		log.debug("Exiting GenericImport open()");

	}

	public boolean hasNext() {
		return genericImportIterator.hasNext();
	}

	public void next() throws GeneralException {

		// Grab the current row from the iterator
		Attributes<String, Object> row = (Attributes<String, Object>) genericImportIterator
				.next();

		if (isGrouped(row, lastRow)) {
			if (log.isDebugEnabled()) 
				log.debug("  Row to be grouped: " + row.toString());
			newRow = groupRow(row);
			
		} else {
			if (log.isDebugEnabled()) 
				log.debug("  Row is not to be grouped: " + row.toString());
			
			// Transform and process the row
			Attributes<String, Object> transform = transformRow(newRow);
			if (transform != null) processRow(transform);
			
			clearGrouped();
			newRow = groupRow(row);
		}

		lastRow = row;
		
		if (!hasNext()){
			Attributes<String, Object> transform = transformRow(newRow);
			if (transform != null) processRow(transform);
		}


	}

	private void clearGrouped() {
		newRow = new Attributes<String,Object>();
		
	}
	

	private Attributes<String, Object> groupRow(Attributes<String, Object> currentRow) throws GeneralException {

		List<String> groupBy = genericImport.getGroupBy();
		List<String> mvFields = genericImport.getMvFields();

		// Copy the row into the newRow
		for (String fieldname : currentRow.getKeys()) {

			// Check to see if this is a multi valued field
			if (mvFields.contains(fieldname)) {
				List mvValue = null;
				if (newRow.containsKey(fieldname))
					mvValue = newRow.getList(fieldname);
				if (mvValue == null)
					mvValue = new ArrayList();
				mvValue.add(currentRow.get(fieldname));
				newRow.put(fieldname, mvValue);
			} else {
				newRow.put(fieldname, currentRow.get(fieldname));
			}
		}

		return newRow;

	}

	private boolean isGrouped(Attributes<String, Object> currentRow,
			Attributes<String, Object> lastRow) throws GeneralException {

		List<String> groupBy = genericImport.getGroupBy();
		List<String> mvFields = genericImport.getMvFields();

		boolean groupRow = true;

		if (groupBy.size() > 0) {

			if (lastRow == null) {
				if (log.isDebugEnabled()) log.debug("isGroup data set created from new");
				// Check schema
				for (String fieldname : groupBy) {
					if (!currentRow.containsKey(fieldname)) {
						throw new GeneralException(
								"Could not find groupBy key " + fieldname
										+ " in the import row.");
					}
				}

			} else {

				// Check to see if this is a grouped row
				if (log.isDebugEnabled())
					log.debug("Checking groupBy");
				for (String fieldname : groupBy) {

					Object currentRowValue = currentRow.get(fieldname);
					Object lastRowValue = lastRow.get(fieldname);

					if (currentRowValue != null
							&& !currentRowValue.equals(lastRowValue)) {
						groupRow = false;
						if (log.isDebugEnabled())
							log.debug("  Current value '" + currentRowValue
									+ "' and last value '" + lastRowValue
									+ "' are not the same.");
						break;
					} else {
						if (log.isDebugEnabled())
							log.debug("  Current values:' " + currentRowValue
									+ "' and last value '" + lastRowValue
									+ "' match..");
					}
				}

			}
		} else {
			groupRow = false;
		}

		return groupRow;
	}

	public void close() throws GeneralException {

		log.debug("Entering GenericImporter close()");

		genericImport.close();

		log.debug("Exiting GenericImporter close()");
	}

}
