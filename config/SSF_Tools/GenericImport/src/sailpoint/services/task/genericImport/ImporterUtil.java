package sailpoint.services.task.genericImport;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.api.Terminator;
import sailpoint.object.*;
import sailpoint.tools.GeneralException;
import sailpoint.tools.RFC4180LineParser;

public class ImporterUtil {

	private static final Logger log = Logger.getLogger(ImporterUtil.class);

	public static void logToFile(String filename, String message) {
		
		logToFile(filename, message, true);
		
	}
	
	/**
	 * Log a message to the file system and get on with it.
	 * 
	 * Not very efficient but useful in an state-less environment
	 * 
	 * @param filename
	 * @param message
	 * @param append
	 */
	public static void logToFile(String filename, String message, boolean append) {
		
		try {
			FileWriter fw = new FileWriter(filename, append);
			fw.write(message + "\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			log.error("Could not log to filename '" + filename + "' with message '" + message + "'", e);
		}
		
	}
	
	/**
	 * Get a single sailpoint object from a search
	 * 
	 * @param className
	 * @param qo
	 * @param returnNullIfMultiple
	 * @return
	 * @throws GeneralException
	 */
	public static SailPointObject getSingleObjectFromSearch(Class<? extends SailPointObject> className,
			QueryOptions qo, boolean returnNullIfMultiple) throws GeneralException {

		log.debug("Starting getSingleObjectFromSearch()");
		SailPointContext context = SailPointFactory.getCurrentContext();
		Iterator<? extends SailPointObject> itr = context.search(className, qo);
		SailPointObject object = null;
		int count = 0;
		
		while (itr.hasNext()) {
			object = (SailPointObject) itr.next();
			count++;
			if (count > 1) break;
		}
		
		sailpoint.tools.Util.flushIterator(itr);
		
		if (count > 1 && returnNullIfMultiple) {
			if (log.isDebugEnabled()) log.debug(" Returning Null as " + String.valueOf(count) + " objects were found and returnNullIfMultiple flag is true");
			log.debug("Exiting getSingleObjectFromSearch()");
			return null;
		} else {
			if (log.isDebugEnabled()) {
				log.debug("  Returning object: " + object.toString());
				log.debug("Exiting getSingleObjectFromSearch()");
			}
			return object;
		}

		
	}
	
	public static SailPointObject getSingleObjectFromSearch(Class<? extends SailPointObject> className,
			QueryOptions qo) throws GeneralException {

		return getSingleObjectFromSearch( className, qo, false);
		
	}

	public static Object getObjectByName(String className, String objectName,
			boolean autoCreate) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException, GeneralException {

		return getObjectByName(Class.forName(className), objectName, autoCreate);

	}

	public static SailPointObject getObjectByName(Class className, String objectName,
			boolean autoCreate) throws InstantiationException,
			IllegalAccessException, GeneralException {

		SailPointContext context = SailPointFactory.getCurrentContext();
		SailPointObject ret = null;
		try {
			ret = context.getObjectByName(className, objectName);
		} catch (GeneralException e) {
			log.error("Get object by name returned the following error", e);
		}

		if (ret == null && autoCreate) {
			ret = (SailPointObject) className.newInstance();
			ret.setName(objectName);
			context.saveObject(ret);
			context.commitTransaction();
			ret = context.getObjectByName(className, objectName);
		}

		return ret;
	}

	public static boolean deleteObjectByName(Class className, String objectName) throws GeneralException {

		SailPointContext context = SailPointFactory.getCurrentContext();
		SailPointObject obj = null;
		try {
			obj =  context.getObjectByName(className, objectName);
		} catch (GeneralException e) {
			log.error(
					"Got a general exception in deleteObjectByName when retrieving object '"
							+ objectName + "' of class '" + className.getName()
							+ "'.", e);
			return false;
		}
		if (obj == null) {
			if (log.isDebugEnabled()) {
				log.debug("Object '" + objectName + "' of class '"
						+ className.getName() + "' not found.");
			}
		} else {
			try {
				context.removeObject(obj);
			} catch (GeneralException e) {
				log.error(
						"Got a general exception in deleteObjectByName when attempting to delete object '"
								+ objectName
								+ "' of class '"
								+ className.getName() + "'.", e);
				return false;
			}
		}

		return true;
	}

	public static void terminateObjectByName(Class className, String objectName)
			throws GeneralException {

		SailPointContext context = SailPointFactory.getCurrentContext();
		SailPointObject obj = null;
		obj = context.getObjectByName(className, objectName);

		terminateObject(obj);

	}

	public static void terminateObject(SailPointObject object) throws GeneralException {

		SailPointContext context = SailPointFactory.getCurrentContext();
		Terminator term = new Terminator(context);
		term.deleteObject(object);

	}

	public static ArrayList parse(String parseString, String delimiter)
			throws GeneralException {

		RFC4180LineParser parser = new RFC4180LineParser(delimiter);
		return parser.parseLine(parseString);

	}

	/*
	 * Get the role attributes from the object config and schema and merge with
	 * existing values in the row map.
	 */
	public static void mergeSchemaWithRow(SailPointObject object,Attributes<String, Object> row) throws GeneralException {
		ImporterUtil.mergeSchemaWithRow(object, row, null);
	}

	public static void mergeSchemaWithRow(SailPointObject object,
			Attributes<String, Object> row, String attributePrecursor) throws GeneralException {

		log.debug("Entering getBundleSchema");
		SailPointContext context = SailPointFactory.getCurrentContext();
		
		// Using reflection, get the methods needed for this class from the object
		Method bundleGetType = null;
		Method objectSetAttribute = null;
		
		try {
			bundleGetType = object.getClass().getMethod("getType");
			objectSetAttribute = object.getClass().getMethod("setAttribute", String.class, Object.class);
		} catch (NoSuchMethodException e) {
			// Don't care, if not available then don't use them
		} catch (SecurityException e) {
			//Keh?
			throw new GeneralException("Security exection in mergeSchemaWithRow when getting a method from an object using reflection", e);
		}
		
		
		String objectClassName = object.getClass().getSimpleName();

		ObjectConfig objectConfig = context.getObjectByName(ObjectConfig.class, objectClassName);

		if (objectConfig == null) {
			// No object config, so this won't have any extended attributes
			// well. thats the assumption anyhow.
		} else {
			List<String> disallowedAttributes;
			
			// If it's a bundle, then we need to get a list of
			// disallowed attributes
			
			if (bundleGetType != null && objectClassName == "Bundle" ) {
				// Get a list of disallowed object attributes
				// Use reflection to get the role type.
				RoleTypeDefinition roleTypeDef = null;
				
				if (bundleGetType != null) {
					try {
						roleTypeDef = objectConfig.getRoleTypesMap()
							.get(bundleGetType.invoke(object));
					} catch (IllegalAccessException e) {
						throw new GeneralException("Exception thrown when invoking bundle.getType() via reflection", e);
					} catch (IllegalArgumentException e) {
						throw new GeneralException("Exception thrown when invoking bundle.getType() via reflection", e);
					} catch (InvocationTargetException e) {
						throw new GeneralException("Exception thrown when invoking bundle.getType() via reflection", e);
					}
				}
				disallowedAttributes = roleTypeDef.getDisallowedAttributes();
			} else {
				disallowedAttributes = new ArrayList<String>();
			}

			List<ObjectAttribute> objectAttributes = objectConfig.getObjectAttributes();

			if (objectAttributes != null) {
				for (ObjectAttribute amd : objectAttributes) {

					String amdName = amd.getName();

					if (!disallowedAttributes.contains(amdName)) {
						if (log.isDebugEnabled())
							log.debug("  Allowing attribute '" + amdName + "'");

						// Add the role meta data if need be.

						String rowKey;
						// Decide how to create the rowkey
						if (attributePrecursor != null
								&& attributePrecursor.length() > 0) {
							rowKey = attributePrecursor.concat(amdName);
						} else {
							rowKey = amdName;
						}

						if (row.containsKey(rowKey)) {
							try {
								objectSetAttribute.invoke(object, amdName, row.get(rowKey));
							} catch (IllegalAccessException e) {
								log.error("Cannot set attribute " + amdName + " with value " + row.get(rowKey), e);
							} catch (IllegalArgumentException e) {
								log.error("Cannot set attribute " + amdName + " with value " + row.get(rowKey), e);								e.printStackTrace();
							} catch (InvocationTargetException e) {
								log.error("Cannot set attribute " + amdName + " with value " + row.get(rowKey), e);
							}
						}
					} else {
						if (log.isDebugEnabled())
							log.debug("  Attribute '" + amdName
									+ "' is not allowed.");
					}
				}
			} else {
				log.debug("  Could not find any objectAttributes in schema");
			}
		}

		log.debug("Exiting getBundleSchema");
	}
	
}
