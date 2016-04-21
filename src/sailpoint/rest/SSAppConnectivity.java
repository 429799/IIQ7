/* (c) Copyright 2013 SailPoint Technologies, Inc., All Rights Reserved. */
package sailpoint.rest;

import java.util.*;
import java.util.concurrent.Callable;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import sailpoint.api.Localizer;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.authorization.CapabilityAuthorizer;
import sailpoint.authorization.RightAuthorizer;
import sailpoint.connector.Connector;
import sailpoint.connector.ConnectorException;
import sailpoint.integration.ListResult;
import sailpoint.integration.RequestResult;
import sailpoint.object.Attributes;
import sailpoint.object.Capability;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.QueryOptions;
import sailpoint.object.Application;
import sailpoint.object.ResourceObject;
import sailpoint.object.SPRight;
import sailpoint.tools.CloseableIterator;
import sailpoint.tools.GeneralException;

import com.sailpoint.services.standard.SSThreadPoolTasksProcessor;

/**
 * A resource to get the connection information for all applications .
 *
 * @author <a href="mailto:rohit.gupta@sailpoint.com">Rohit Gupta</a>
 */
@Path("ssApps")
public class SSAppConnectivity extends BaseResource  
{    

    private static final Log log = LogFactory.getLog(SSAppConnectivity.class);
    private  List<Map<String,Object>>  results = new ArrayList<Map<String, Object>>();
    private  List<Application> instances ; 
    private List<Callable<Map<String, Object>> > callableList= new ArrayList<Callable<Map<String, Object>> >();
    public  SSAppConnectivity()
    {
   	
    	log.debug("In Constructor GEAppConnectivity");
    }
    
    @GET
    /** 
     * This method returns connection of all the applications defined 
     * If application name supplied it will return only that given application information
     * 
     * @param startParm
     * @param limitParm
     * @param sortFieldParm
     * @param sortDirParm
     * @param threadsCount
     * @param timeOutMillSecs
     * @param applicationName
     * @return ListResult
     * @throws GeneralException
     */
    public ListResult getAppsInfo(@QueryParam("start") int startParm,
            @QueryParam("limit") int limitParm, 
            @QueryParam("sort") String sortFieldParm, 
            @QueryParam("dir") String sortDirParm,
            @QueryParam("threads") int threadsCount,
            @QueryParam("timeout") int timeOutMillSecs,
            @QueryParam("applicationName") String applicationName) throws GeneralException 
      {
    	    authorize(new CapabilityAuthorizer(Capability.SYSTEM_ADMINISTRATOR));
        QueryOptions qo = new QueryOptions();
     	String sort = sortFieldParm,sortBy = null,sortDirection = null;
     	log.debug("sort "+sort);
     	int threads=threadsCount;
     	log.debug("Threads "+threads);
     	int timeout=timeOutMillSecs;
     	log.debug("timeout "+timeout);
        String dir = sortDirParm;
    	    log.debug("dir "+dir);
    	    String appNameQuery=applicationName;
    	    log.debug("appNameQuery "+appNameQuery);
    	    if(appNameQuery!=null)
    	    {
    	    	qo.addFilter(Filter.eq("name",appNameQuery));
    	    }
        if (dir != null) 
        {
             sortBy = sort;
        } 
        else 
        {
            JSONArray sortArray = null;
            try
            {
            sortArray = new JSONArray(sort);
            JSONObject sortObject = sortArray.getJSONObject(0);
            sortBy = sortObject.getString("property");
            sortDirection = sortObject.getString("direction");
            }catch(Exception e)
            {
                log.debug("Invalid sort input.");
            }
        }
        
        if(startParm > 0)
        {
            qo.setFirstRow(startParm);
        }
        log.debug("limitParm "+limitParm);
        if(limitParm > 0)
        {
            qo.setResultLimit(limitParm);
        }
        if(sortBy != null)
        {
            qo.addOrdering(sortBy, !sortDirection.equals("DESC"));
        }
        log.debug( "QueryOptions "+qo.toString());
        int count = getContext().countObjects(Application.class, qo);
        if(count>0) 
        {
            log.debug( "There are "+count+" application instances running");
        }else
        {
            log.debug( "No application instances found");
        }
         instances = getContext().getObjects(Application.class, qo);
         log.debug("Main Thread Id "+Thread.currentThread().getId());
 		 log.debug("Main Thread Name "+Thread.currentThread().getName());
 		 log.debug("Thread Active Count "+Thread.activeCount());
 
 		for( Application app : this.instances)
		{     
     		GEAppConnectivityCallable GEAppConnectivityCallable = new GEAppConnectivityCallable(app.getName());
     		 callableList.add(GEAppConnectivityCallable);
            
		}
 		   SSThreadPoolTasksProcessor processor = new SSThreadPoolTasksProcessor(threads!=0?threads:4,timeout!=0?timeout:20000,callableList,results);
           processor.start();
           log.debug("All concurrent tasks submitted.");
           List<String> processedAps= new ArrayList<String>();
           List<String> unProcessedAps= new ArrayList<String>();
           for( Application app : this.instances)
   		   {  
        	       boolean processed=false;
	           for(Map<String,Object> result: this.results)
	           {
		        	   if(app.getName().equalsIgnoreCase((String) result.get("name")))
		        	   {
		        		   processedAps.add(app.getName());
		        		   processed=true;
		        		   break;
		        	   }
	           }
	           if(!processed)
	           unProcessedAps.add(app.getName());
   		   }
           for( String appName : unProcessedAps)
           {
        	   results.add(convertAppToMapRedFlag(appName));
           }
           log.debug("Processed Apps "+processedAps.toString());
           log.debug("Un Processed Apps "+unProcessedAps.toString());
	       ListResult lr = new ListResult(results, count);
           return lr;
    }
    
    @GET @Path("{applicationName}/{timeout}/{typeConnection}/info")
    /**
     * This method only returns the given applications connection type info
     * It could be group, accounts, or just test
     * @param timeOutMillSecs
     * @param applicationName
     * @param typeConnection
     * @return ListResult
     * @throws GeneralException
     */
    public ListResult getAppsInfoTypeBased(
    		   
            @PathParam("timeout") int timeOutMillSecs,
            @PathParam("applicationName") String applicationName,
            @PathParam("typeConnection") String typeConnection
            ) throws GeneralException 
      {
    	    authorize(new CapabilityAuthorizer(Capability.SYSTEM_ADMINISTRATOR));
        QueryOptions qo = new QueryOptions();
     	
     	
     	int timeout=timeOutMillSecs;
     	log.debug("timeout "+timeout);
        String appNameQuery=applicationName;
    	    log.debug("appNameQuery "+appNameQuery);
    	    String type=typeConnection;
    	    log.debug("type "+type);
    	    if(appNameQuery!=null)
    	    {
    	    	qo.addFilter(Filter.eq("name",appNameQuery));
    	    }
        
        log.debug( "QueryOptions "+qo.toString());
        int count = getContext().countObjects(Application.class, qo);
        if(count>0) 
        {
            log.debug( "There are "+count+" application instances running");
        }else
        {
            log.debug( "No application instances found");
        }
         instances = getContext().getObjects(Application.class, qo);
         log.debug("Main Thread Id "+Thread.currentThread().getId());
 		 log.debug("Main Thread Name "+Thread.currentThread().getName());
 		 log.debug("Thread Active Count "+Thread.activeCount());
 
 		for( Application app : this.instances)
		{     
     		GEAppConnectivityCallable GEAppConnectivityCallable = new GEAppConnectivityCallable(app.getName(),type);
     		 callableList.add(GEAppConnectivityCallable);
            
		}
 		   SSThreadPoolTasksProcessor processor = new SSThreadPoolTasksProcessor(1,timeout!=0?timeout:20000,callableList,results);
           processor.start();
           log.debug("All concurrent tasks submitted.");
           List<String> processedAps= new ArrayList<String>();
           List<String> unProcessedAps= new ArrayList<String>();
           for( Application app : this.instances)
   		   {  
        	       boolean processed=false;
	           for(Map<String,Object> result: this.results)
	           {
		        	   if(app.getName().equalsIgnoreCase((String) result.get("name")))
		        	   {
		        		   processedAps.add(app.getName());
		        		   processed=true;
		        		   break;
		        	   }
	           }
	           if(!processed)
	           unProcessedAps.add(app.getName());
   		   }
           for( String appName : unProcessedAps)
           {
        	   results.add(convertAppToMapRedFlagTypeBased(appName));
           }
           log.debug("Processed Apps "+processedAps.toString());
           log.debug("Un Processed Apps "+unProcessedAps.toString());
	       ListResult lr = new ListResult(results, count);
           return lr;
    }
    
    
    @POST @Path("/manage")
   	/**
   	 * This rest service  manages the  applications extended attribute 
   	 * appLevelTwoApproval & appLevelTwoApprovers
   	 * @param map
   	 * @return RequestResult
   	 * @throws GeneralException
   	 */
       public RequestResult manageApplicationExtendedAttributes(List<HashMap> inputList) throws GeneralException 
       {
       	log.trace("Enter manageApplicationExtendedAttributes ");
       	authorize(new RightAuthorizer(SPRight.WebServices));
       	 RequestResult result = new RequestResult();  
       	 Map mainMap= new HashMap();
       	 Map outcome=null;
       	 for(HashMap innerMap: inputList)
       	 {
	       	 outcome=setExtendedAttributes((String)innerMap.get("applicationName"),innerMap,false);
	       	 if(outcome!=null)
	       	 {
	       		mainMap.putAll(outcome);
	       	 }
	       	
	         	
       	 }
       	 result.setMetaData(mainMap);
       	 log.trace("End manageApplicationExtendedAttributes "+ mainMap);
       	 return result;
       	
       }
    /**
     * This method updates extended attributes on given application
     * @param applicationName
     * @param innerMap
     * @param newMangedAttribute
     * @return Map
     */
      private  Map setExtendedAttributes( String applicationName, Map innerMap, Boolean newMangedAttribute) 
      {
   	     log.trace("Enter setExtendedAttributes");
      	     log.debug("applicationName "+applicationName);
      	     log.debug("innerMap "+innerMap);
      	   Map metaData = new HashMap();
      	 
      	     try
      	     {
   			if (applicationName != null && innerMap!=null)
   			{
   				
   			    Application applicationObj=(Application)getContext().getObjectByName(Application.class, applicationName);
   			    if(applicationObj==null)
   			    {
   			    	throw new GeneralException ("Unable to find application");
   			    }
   	 			Attributes<String,Object> attrs = applicationObj.getAttributes();
   	 			Map attrsMap=null;
   	 			if(attrs!=null)
   	 			{
   	 				attrsMap = attrs.getMap();
   	 				log.debug("Previous Map :: " + attrsMap);
   	 			}
   	 			
   				StringBuilder ownerStringLevel2 = new StringBuilder();
   				/*
   				 * Level 2 Logic
   				 */
   				if( innerMap.get("appLevelTwoApproval")!=null && attrsMap!=null)
   				{
   					attrsMap.put("appLevelTwoApproval", (Boolean)innerMap.get("appLevelTwoApproval"));
   				}
   				
   				if( innerMap.get("appLevelTwoApprovers")!=null)
   				{
   					List<String> list = (List) innerMap.get("appLevelTwoApprovers");
   					for(int index=0;index<list.size();index++)
   					{
   						if((Identity)getContext().getObjectByName(Identity.class, list.get(index))!=null)
   						{
   							ownerStringLevel2.append(list.get(index));
   							if(list.size()>index+1)
   							{
   								ownerStringLevel2.append(",");
   							}
   						}
   					}
   					log.debug(" ownerStringLevel2.toString() " + ownerStringLevel2.toString());
   					/*
   					 * If entLevelThreeEntitlementOwners are provided we will set entLevelThreeAutoApprove value to false
   					 */
   					attrsMap.put("appLevelTwoApprovers", ownerStringLevel2.toString());
   					attrsMap.put("appLevelTwoApproval", true);
   				}
   				Date today = new Date();
   				attrsMap.put("updatedByRestService", today.toString());
   				log.debug("New Map :: " + attrsMap);
   				if(attrsMap!=null)
   				{
   				attrs.setMap(attrsMap);
   				applicationObj.setAttributes(attrs);
   				getContext().saveObject(applicationObj);
   				getContext().commitTransaction();
   				getContext().decache();
   				}
   				
   				metaData.put(applicationName+" action", "Updated Application Extended  Attribute");
   				metaData.put(applicationName+" applicationName",applicationName);
   				metaData.put(applicationName+" outcome","Success");
   				
   				
   			}
      	     }
      	     catch (Exception ex)
      	     {
      	    	 log.error("Error updating application "+ex.getMessage());
      	    	metaData.put(applicationName+" outcome","Failure");
      	   	metaData.put(applicationName+" failure",ex.getMessage());
      	     }
      	    log.trace("End setExtendedAttributes "+ metaData);
   			return metaData;
   			  
   	}
 
    /**
     * Converts an application�s connection details into a map with red, green, and yellow status based on a given connection type.
	 * This method adds owner, description,  type, and remmediator details to the map.
	 *	 @param app the application name
	 *	 @param type the connection name
	 *   @return a Map 
     */
    private  Map<String, Object> convertAppToMapTypeConnection(String  app, String type){
    	Application appObject=null;
    	SailPointContext _threadContext=null;
    	HashMap<String,Object> appAttributes = new HashMap<String,Object>();
    	 
     	try 
     	{
     	_threadContext=init();
     	appObject=_threadContext.getObjectByName(Application.class,app);
		log.trace("Enter convertAppToMap");
      	log.debug("Scanning appObject "+appObject.getName());
      	 appAttributes.put("name", appObject.getName());
        Connector appConnector=null;
		try 
		{
		   	
			appConnector = sailpoint.connector.ConnectorFactory.getConnector(appObject, null);
			log.debug( "appConnector "+appConnector);
		} 
		catch (GeneralException e) 
		{
			log.debug( "Application connector settings are not right "+e.getMessage());
		}
        if (appConnector!=null && type!=null && (type.equalsIgnoreCase("Connection")||type.equalsIgnoreCase("All"))) 
    	    {
	    	    	try 
	    	    	{
	    	    		log.debug( "appObject.getConnector() "+appObject.getConnector());
	    	    		 if(appObject.getConnector()!=null&& appObject.getConnector().toString().equalsIgnoreCase("sailpoint.connector.DefaultLogicalConnector"))
	    	    		 {
	    	    			 appAttributes.put("connection", "Yellow");
	    	    		  }
	    	    		 else
	    	    		 {
	    	    		   appConnector.testConfiguration();
	    	    		   appAttributes.put("connection", "Green");
	    	    		 }

    	    		} 
	    	    	catch (Exception ex) 
    	    		{
	    	    		  appAttributes.put("connection", "Red");
    	    		   log.debug( "Application settings are not right "+ex.getMessage());
    	    		}
    		}
        else
        {
        	/*
   		  * No rest request
   		  */
        appAttributes.put("connection", "Blue");
        }
        
        
   	 if (appConnector!=null && type!=null && (type.equalsIgnoreCase("Accounts")||type.equalsIgnoreCase("All"))) 
	    {
            if(appObject.getAccountSchema() != null) 
	        {
	        	CloseableIterator<ResourceObject> iteratorAccounts=null;
		        try 
		        {
		        
			 	    	   
			        	 iteratorAccounts=appConnector.iterateObjects(Application.SCHEMA_ACCOUNT, null, null);
						if(iteratorAccounts!=null )
						{
							
							appAttributes.put("accounts", "Green");
							iteratorAccounts.close();
						}
		     	    
				} 
		        catch (ConnectorException e) 
		        {
					log.error( "Application account settings are not right "+e.getMessage());
					appAttributes.put("accounts", "Red");
				}
		        catch (RuntimeException e) 
		        {
					log.error( "Application account settings are not right "+e.getMessage());
					appAttributes.put("accounts", "Yellow");
				}
		        finally
		        {
	                if (iteratorAccounts != null)
	                {
	                	iteratorAccounts.close();
	                }
	            }
	        }
	        else
	        {
	        	/*
	        	 * No Schema
	        	 */
	        	appAttributes.put("accounts", "Yellow");
	        }
	    }
   	 else
   	 {
   		 /*
		  * No rest request
		  */
   	 	appAttributes.put("accounts", "Blue");
   	 }
   	 
   	 
   	 if (appConnector!=null && type!=null && (type.equalsIgnoreCase("Groups")||type.equalsIgnoreCase("All"))) 
	    {
	   
	        if(appObject.getGroupSchema() != null) 
	        {
	        	 CloseableIterator<ResourceObject> iteratorGroups=null;
	        	 try 
	        	 {
	        		  iteratorGroups =  appConnector.iterateObjects(Application.SCHEMA_GROUP, null, null);
					if(iteratorGroups!=null)
					{
						appAttributes.put("groups", "Green");
						iteratorGroups.close();
					}
				} 
	        	 catch (ConnectorException e) 
	        	 {
					 log.error( "Application group settings are not right "+e.getMessage());
					appAttributes.put("groups", "Red");
			}
	        	 catch (RuntimeException e) 
	        	 {
	 				log.error( "Application group settings are not right "+e.getMessage());
	 				appAttributes.put("groups", "Yellow");
	 			}
	        	 finally
	        	 {
	                 if (iteratorGroups != null)
	                 {
	                	 iteratorGroups.close();
	                 }
	             }
	        
	        }
	        else
	        {
	        	/*
	        	 * No Schema
	        	 */
	        	appAttributes.put("groups", "Yellow");
	        }
	    }
	 else
   	 {
		 /*
		  * No rest request
		  */
   	 	appAttributes.put("groups", "Blue");
   	 }
      
     	}
     	catch (GeneralException e) 
     	{
			log.error( "Error Finding Application Object "+app);
		}
     	catch (Exception e) 
     	{
			log.error(" Error Creating Context "+e.getMessage());
		}
     	finally
     	{
     		try
     		{
				SailPointFactory.releaseContext(_threadContext);
			} 
     		catch (GeneralException e) 
			{
				log.error(" Error releasing Context "+e.getMessage());
			}
     	}
       	log.trace("End convertAppToMap "+appAttributes);
        return appAttributes;
 
    }
    /**
     * Converts an application�s connection details into a map with red, green, and yellow status.
	 * This method adds owner, description,  type, and remmediator details to the map.
	 *	 @param app the application name
	 *   @return a Map 
     */
    private  Map<String, Object> convertAppToMap(String  app){
    	Application appObject=null;
    	SailPointContext _threadContext=null;
    	HashMap<String,Object> appAttributes = new HashMap<String,Object>();
     	try 
     	{
     	_threadContext=init();
		appObject=_threadContext.getObjectByName(Application.class,app);
		log.trace("Enter convertAppToMap");
      	log.debug("Scanning appObject "+appObject.getName());
        String result="Red";
        Localizer localizer = new Localizer(_threadContext);
        log.debug("Thread.currentThread() convertAppToMap Id "+Thread.currentThread().getId());
		log.debug("Thread.currentThread() convertAppToMap Name "+Thread.currentThread().getName());
        Connector appConnector=null;
		try 
		{
		   	
			appConnector = sailpoint.connector.ConnectorFactory.getConnector(appObject, null);
			log.debug( "appConnector "+appConnector);
		} 
		catch (GeneralException e) 
		{
			log.debug( "Application connector settings are not right "+e.getMessage());
		}
        appAttributes.put("name", appObject.getName());
        appAttributes.put("description", localizer.getLocalizedValue(appObject, Localizer.ATTR_DESCRIPTION, localizer.getDefaultLocale()));
        appAttributes.put("type", appObject.getType());
        if (appObject.getOwner() != null){
            appAttributes.put("ownerId", appObject.getOwner().getId());
            appAttributes.put("owner", appObject.getOwner().getName());
            appAttributes.put("ownerDisplayName", appObject.getOwner().getDisplayableName());
        }
        if (appObject.getRemediators() != null)
        {
            List<String> remediators = new ArrayList<String>();
            for(Identity identity : appObject.getRemediators())
            {
                String displayName=identity.getDisplayName();
                if(displayName==null)
                {
                	displayName=identity.getName();
                }
                remediators.add(identity.getDisplayName());
            }

            appAttributes.put("remediators", remediators);
        }
        if (appConnector!=null) 
    	    {
	    	    	try 
	    	    	{
	    	    		log.debug( "appObject.getConnector() "+appObject.getConnector());
	    	    		 if(appObject.getConnector()!=null&& appObject.getConnector().toString().equalsIgnoreCase("sailpoint.connector.DefaultLogicalConnector"))
	    	    		 {
	    	    			   result = "Yellow"; 
	    	    		  }
	    	    		 else
	    	    		 {
	    	    		   appConnector.testConfiguration();
	    	    		   result = "Green";
	    	    		 }

    	    		} 
	    	    	catch (Exception ex) 
    	    		{
    	    		   result = "Red";
    	    		   log.debug( "Application settings are not right "+ex.getMessage());
    	    		}
    		}
        appAttributes.put("connection", result);
       if(appObject.getAccountSchema() != null) 
	        {
	        	CloseableIterator<ResourceObject> iteratorAccounts=null;
		        try 
		        {
		        	 iteratorAccounts=appConnector.iterateObjects(Application.SCHEMA_ACCOUNT, null, null);
					if(iteratorAccounts!=null)
					{
						appAttributes.put("accounts", "Green");
						iteratorAccounts.close();
					}
				} 
		        catch (ConnectorException e) 
		        {
					log.error( "Application account settings are not right "+e.getMessage());
					appAttributes.put("accounts", "Red");
				}
		        catch (RuntimeException e) 
		        {
					log.error( "Application account settings are not right "+e.getMessage());
					appAttributes.put("accounts", "Yellow");
				}
		        finally
		        {
	                if (iteratorAccounts != null)
	                {
	                	iteratorAccounts.close();
	                }
	            }
	        }
	        else
	        {
	        	appAttributes.put("accounts", "Yellow");
	        }
	        if(appObject.getGroupSchema() != null) 
	        {
	        	 CloseableIterator<ResourceObject> iteratorGroups=null;
	        	 try 
	        	 {
	        		  iteratorGroups =  appConnector.iterateObjects(Application.SCHEMA_GROUP, null, null);
					if(iteratorGroups!=null)
					{
						appAttributes.put("groups", "Green");
						iteratorGroups.close();
					}
				} 
	        	 catch (ConnectorException e) 
	        	 {
					 log.error( "Application group settings are not right "+e.getMessage());
					appAttributes.put("groups", "Red");
			}
	        	 catch (RuntimeException e) 
	        	 {
	 				log.error( "Application group settings are not right "+e.getMessage());
	 				appAttributes.put("groups", "Yellow");
	 			}
	        	 finally
	        	 {
	                 if (iteratorGroups != null)
	                 {
	                	 iteratorGroups.close();
	                 }
	             }
	        
	        }
	        else
	        {
	        	appAttributes.put("groups", "Yellow");
	        }
        
     
     	}
     	catch (GeneralException e) 
     	{
			log.error( "Error Finding Application Object "+app);
		}
     	catch (Exception e) 
     	{
			log.error(" Error Creating Context "+e.getMessage());
		}
     	finally
     	{
     		try
     		{
				SailPointFactory.releaseContext(_threadContext);
			} 
     		catch (GeneralException e) 
			{
				log.error(" Error releasing Context "+e.getMessage());
			}
     	}
       	log.trace("End convertAppToMap "+appAttributes);
        return appAttributes;
 
    }
    
    /**
     * Converts an application�s connection details into a map with red status.
	 * This method adds owner, description,  type, and remmediator details to the map.
	 *	 @param app the application name
	 *   @return a Map 
     */
    private  Map<String, Object>  convertAppToMapRedFlag(String app){
    	    log.debug("Enter convertAppToMapRedFlag");
    	    Application appObject=null;
        HashMap<String,Object> appAttributes = new HashMap<String,Object>();
        String redFlag="Red";
        try
        {
        	appObject=getContext().getObjectByName(Application.class,app);
		Localizer localizer = new Localizer(getContext());
        appAttributes.put("name", appObject.getName());
        appAttributes.put("description", localizer.getLocalizedValue(appObject, Localizer.ATTR_DESCRIPTION, getLocale()));
        appAttributes.put("type", appObject.getType());
        if (appObject.getOwner() != null){
            appAttributes.put("ownerId", appObject.getOwner().getId());
            appAttributes.put("owner", appObject.getOwner().getName());
            appAttributes.put("ownerDisplayName", appObject.getOwner().getDisplayableName());
        }
        if (appObject.getRemediators() != null)
        {
            List<String> remediators = new ArrayList<String>();
            for(Identity identity : appObject.getRemediators())
            {
                String displayName=identity.getDisplayName();
                if(displayName==null)
                {
                	displayName=identity.getName();
                }
                remediators.add(identity.getDisplayName());
            }

            appAttributes.put("remediators", remediators);
        }
        appAttributes.put("connection", redFlag);
        	appAttributes.put("accounts", redFlag);
        	appAttributes.put("groups", redFlag);
        }
     	catch (GeneralException e) 
     	{
    		log.error( "Error Finding Application Object "+e.getMessage());
    	    }
        catch (Exception e) 
        {
    		log.error( "Error "+e.getMessage());
       	}
        log.debug("End convertAppToMapRedFlag "+appAttributes);
        return appAttributes;
    }
    
    
    /**
     * Converts an application�s connection details into a map with red status based on a given connection type.
	 * This method adds owner, description,  type, and remmediator details to the map.
	 *	 @param app the application name
	 *   @return a Map 
     */
    private  Map<String, Object>  convertAppToMapRedFlagTypeBased(String app){
    	    log.debug("Enter convertAppToMapRedFlag");
    	    Application appObject=null;
        HashMap<String,Object> appAttributes = new HashMap<String,Object>();
        String redFlag="Red";
        try
        {
        	appObject=getContext().getObjectByName(Application.class,app);
		appAttributes.put("name", appObject.getName());
        appAttributes.put("connection", redFlag);
        	appAttributes.put("accounts", redFlag);
        	appAttributes.put("groups", redFlag);
        }
     	catch (GeneralException e) 
     	{
    		log.error( "Error Finding Application Object "+e.getMessage());
    	    }
        catch (Exception e) 
        {
    		log.error( "Error "+e.getMessage());
       	}
        log.debug("End convertAppToMapRedFlag "+appAttributes);
        return appAttributes;
    }
    
    /**
     * Since the inner class "GEAppConnectivityCallable" implements callable interface
     * so there are no SailPointContext/s for this thread. 
     * Therefore this method is used to create context. 
     * It is called by other methods to get SailPointContext at the beginning.
     *
     */
    private SailPointContext init() throws Exception 
    {
    	SailPointContext _threadContext = SailPointFactory.createContext();
    	log.debug("Creating Sailpoint Context");
    	return _threadContext;
    }
    /*
     * Inner Class
     * This class is developed to create callable objects for each application. 
     * As a result, these objects can be used by GEThreadPoolTasksProcessor to 
     * create tasks for each callabale object.
     */
       class GEAppConnectivityCallable implements Callable <Map<String, Object>> 
       {
           private String app;
           private String type;
           public GEAppConnectivityCallable(String app, String type)
           {
           	log.debug(" Start GEAppConnectivityCallable Constructor");
           	this.app=app;
           	this.type=type;
           log.debug(" End GEAppConnectivityCallable Constructor");
           }
           public GEAppConnectivityCallable(String app)
           {
           	log.debug(" Start GEAppConnectivityCallable Constructor");
           	this.app=app;
           log.debug(" End GEAppConnectivityCallable Constructor");
           }
       	@Override
       	public  Map<String, Object> call() 
       	{
       		try
       	      {
       			log.debug("Running Processor Thread "+Thread.currentThread().getId());
       			log.debug("Running Processor Thread Name "+Thread.currentThread().getName());
       			log.debug("Running Processor Thread Interrupted "+Thread.currentThread().isInterrupted());
       			log.debug("Processor Thread is running and scanning app "+app);
       			log.debug("Scanning of app is successful  "+app);
       			if(type!=null)
       			return SSAppConnectivity.this.convertAppToMapTypeConnection(app,type);
       			else
       			return SSAppConnectivity.this.convertAppToMap(app);
       			
       	      }
       		  catch (Exception exception)
       	      {
       	    	  log.error(" Processor Thread Exception "+exception.getMessage());
       	      }
			return null;
       		}

        }

}
