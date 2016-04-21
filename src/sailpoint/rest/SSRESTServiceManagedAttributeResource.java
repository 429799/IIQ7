package sailpoint.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.authorization.RightAuthorizer;
import sailpoint.integration.RequestResult;
import sailpoint.object.Attributes;
import sailpoint.object.Filter;
import sailpoint.object.Application;
import sailpoint.object.Identity;
import sailpoint.object.QueryOptions;
import sailpoint.object.SPRight;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.WorkItem.Type;
import sailpoint.tools.GeneralException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
/**
 * REST methods for the "ManagedAttribute" resource.
 *
 * @author <a href="mailto:rohit.gupta@sailpoint.com">Rohit Gupta</a>
 */
@Path("/ss/managedEntitlements")
public class SSRESTServiceManagedAttributeResource extends BaseResource
{
	private static final Log log = LogFactory.getLog(SSRESTServiceManagedAttributeResource.class);

	 /**
     * Return a map representation of the given managedAttribute map.
     * 
     * @param  map  The name of the identity.
     * 
     * @return A Map representation on OneUI requested items.
     */
    @POST
    public RequestResult getManagedAttribute(HashMap<String,Object> map)
        throws GeneralException 
{
    	log.trace("Enter GERESTServiceManagedAttributeResource getManagedAttribute");
    	log.debug("map "+map);
    	authorize(new RightAuthorizer(SPRight.WebServices));
    Map<String,Object> resultMap = new HashMap<String,Object>();
    HashMap innerMap=(HashMap)map.get("managedEntArgs");
    RequestResult result = new RequestResult();   
    if(innerMap!=null)
    {
     	log.debug("innerMap "+innerMap);
	    String attribute=(String) innerMap.get("attribute");
	    String value=(String) innerMap.get("value");
	    String application=(String) innerMap.get("application");
	    if(attribute!=null && value!=null && application!=null)
	    {
			QueryOptions qo = new QueryOptions();
			Filter filterOne = Filter.eq("attribute", attribute);
			Filter filterTwo = Filter.eq("value", value);
			Filter filterThree = Filter.eq("application.name", application);
			qo.addFilter(filterOne);
			qo.addFilter(filterTwo);
			qo.addFilter(filterThree);
		    List<ManagedAttribute> mas = getContext().getObjects(ManagedAttribute.class, qo);
			for(ManagedAttribute ma:mas)
			{
				resultMap.put(ma.getId(),ma.getAttributes());
				
			}
			result.setAttributes(resultMap);
	    }
    }
	log.trace("End GERESTServiceIdentityResource getManagedAttribute "+resultMap);
	return result;
	}
    
    /**
     * This method finds the existence of given managed attribute
     * @param innerMap
     * @return ManagedAttribute
     * @throws GeneralException
     */
    private ManagedAttribute findManagedAttribute(HashMap<String,Object> innerMap)
        throws GeneralException 
{
    	log.trace("Enter findManagedAttribute");
    	log.debug("innerMap "+innerMap);
    if(innerMap!=null)
    {
     	log.debug("innerMap "+innerMap);
	    String attribute=(String) innerMap.get("attribute");
	    String value=(String) innerMap.get("value");
	    String application=(String) innerMap.get("application");
	    if(attribute!=null && value!=null && application!=null)
	    {
			QueryOptions qo = new QueryOptions();
			Filter filterOne = Filter.eq("attribute", attribute);
			Filter filterTwo = Filter.eq("value", value);
			Filter filterThree = Filter.eq("application.name", application);
			qo.addFilter(filterOne);
			qo.addFilter(filterTwo);
			qo.addFilter(filterThree);
		    List<ManagedAttribute> mas = getContext().getObjects(ManagedAttribute.class, qo);
			for(ManagedAttribute ma:mas)
			{
				if(ma.getId()!=null && ma.getAttributes()!=null)
				{
					log.trace("End findManagedAttribute "+ma);
					return ma;
				}
				
			}
			
	    }
    }
	log.trace("End findManagedAttribute ");
	return null;
	}
    /**
     * This methods creates new managed attribute with owners and other attributes 
     * @param innerMap
     * @return Map
     */
    private  HashMap setNewManagedEntAndOwners(  HashMap<String,Object> innerMap) 
    {
 	     log.trace("Enter setNewManagedEntAndOwners");
    	     log.debug("innerMap "+innerMap);
    	     HashMap metaData = new HashMap();
    	     ManagedAttribute managedAttr = new ManagedAttribute();
    	     try
    	     {
 			if ( innerMap!=null)
 			{
 				log.debug("innerMap "+innerMap);
 			    String attribute=(String) innerMap.get("attribute");
 			    String value=(String) innerMap.get("value");
 			    String application=(String) innerMap.get("application");
 			    String type=(String) innerMap.get("type");
 			    String description=(String) innerMap.get("description");
	 			String owner=(String) innerMap.get("owner");
	 			String displayName=(String) innerMap.get("displayName");
 			    managedAttr.setRequestable(true);
 				managedAttr.setAttribute(attribute);
 				managedAttr.setType(sailpoint.object.ManagedAttribute.Type.Entitlement);
 				if(type!=null && type.equalsIgnoreCase("Group"))
 				{
 				managedAttr.setGroup(true);
 				}
 				managedAttr.setApplication((Application)getContext().getObjectByName(Application.class, application));
 				managedAttr.setValue(value);
 				if(description!=null)
 				{
 				HashMap map = new HashMap();
 				map.put("en_US", description);
 				managedAttr.setDescriptions(map);
 				}
 				else
 				{
 					throw new GeneralException("Please provide description for new managed entitlement");
 				}
 				if(owner!=null)
 				{
 					managedAttr.setOwner((Identity)getContext().getObjectByName(Identity.class, owner));
 				}
 				else
 				{
 					managedAttr.setOwner((Identity)getContext().getObjectByName(Identity.class, "spadmin"));
 				}
 				if(displayName!=null)
 				{
 	 				managedAttr.setDisplayName(displayName);
 				}
 				else
 				{
 					throw new GeneralException("Please provide display name for new managed entitlement");
 				}
 				getContext().saveObject(managedAttr);
				getContext().commitTransaction();
				getContext().decache();
				ManagedAttribute newManagedAttr =findManagedAttribute(innerMap);
				metaData=setOwnersAndAdditionalAttributes(newManagedAttr,innerMap, true);
 				
 			}
    	     }
    	     catch (Exception ex)
    	     {
    	    	 log.error("Error creating managed entitlement "+ex.getMessage());
    	    	   
    	     }
    	    log.trace("End setNewManagedEntAndOwners "+metaData);
 		return metaData;
 			  
 	}
 /**
  * This method updates managed attribute with owners and other attributes
  * @param managedAttr
  * @param innerMap
  * @param newMangedAttribute
  * @return Map
 * @throws GeneralException 
  */
   private  HashMap setOwnersAndAdditionalAttributes( ManagedAttribute managedAttr,HashMap<String,Object> innerMap, Boolean newMangedAttribute) throws GeneralException 
   {
	     log.trace("Enter setOwnersAndAdditionalAttributes");
   	     log.debug("innerMap "+innerMap);
   	     String id=managedAttr.getId();
   	     log.debug("managedAttr "+managedAttr);
   	     HashMap metaData = new HashMap();
   	     try
   	     {
			if (managedAttr != null && innerMap!=null)
			{
				
				log.debug(" working on " + managedAttr.getDisplayName());
				String description=(String) innerMap.get("description");
	 			String owner=(String) innerMap.get("owner");
	 			String displayName=(String) innerMap.get("displayName");
	 			Attributes<String,Object> attrs = managedAttr.getAttributes();
	 			Map attrsMap;
	 			if(attrs!=null)
	 			{
	 				attrsMap = attrs.getMap();
	 				log.debug("Previous Map :: " + attrsMap);
	 			}
	 			else
	 			{
	 			     attrs = new Attributes();
	 				attrsMap= new HashMap();
	 			}
				
				StringBuilder ownerStringLevel3 = new StringBuilder();
				/*
				 * Level 3 Logic
				 */
				if( innerMap.get("entLevelThreeAutoApprove")!=null)
				{
					attrsMap.put("entLevelThreeAutoApprove", (Boolean)innerMap.get("entLevelThreeAutoApprove"));
				}
				
				if( innerMap.get("entLevelThreeEntitlementOwners")!=null)
				{
					List<String> list = (List) innerMap.get("entLevelThreeEntitlementOwners");
					for(int index=0;index<list.size();index++)
					{
						if((Identity)getContext().getObjectByName(Identity.class, list.get(index))!=null)
						{
							ownerStringLevel3.append(list.get(index));
							if(list.size()>index+1)
							{
								ownerStringLevel3.append(",");
							}
						}
					}
					log.debug(" ownerStringLevel3.toString() " + ownerStringLevel3.toString());
					/*
					 * If entLevelThreeEntitlementOwners are provided we will set entLevelThreeAutoApprove value to false
					 */
					attrsMap.put("entLevelThreeEntitlementOwners", ownerStringLevel3.toString());
					attrsMap.put("entLevelThreeAutoApprove", false);
				}
				/*
				 * Level 4 logic
				 */
				StringBuilder ownerStringLevel4 = new StringBuilder();
				if( innerMap.get("entLevelFourEntitlementOwners")!=null)
				{
					List<String> list = (List) innerMap.get("entLevelFourEntitlementOwners");
					for(int index=0;index<list.size();index++)
					{
						if((Identity)getContext().getObjectByName(Identity.class, list.get(index))!=null)
						{
							ownerStringLevel4.append(list.get(index));
							if(list.size()>index+1)
							{
							ownerStringLevel4.append(",");
							}
						}
					}
					log.debug(" ownerStringLevel4.toString() " + ownerStringLevel4.toString());
					/*
					 * If entLevelFourEntitlementOwners are provided we will set entLevelFourRequiredApprovals value to true
					 */
					attrsMap.put("entLevelFourEntitlementOwners", ownerStringLevel4.toString());
					attrsMap.put("entLevelFourRequiredApprovals", true);
					
				}
				Date today = new Date();
				attrsMap.put("updatedByRestService", today.toString());
				if(newMangedAttribute)
				{
				attrsMap.put("createdByRestService", today.toString());
				}
				log.debug("New Map :: " + attrsMap);
				attrs.setMap(attrsMap);
				managedAttr.setAttributes(attrs);
				if(description!=null)
 				{
 				HashMap map = new HashMap();
 				map.put("en_US", description);
 				managedAttr.setDescriptions(map);
 				}
 				if(owner!=null)
 				{
 					managedAttr.setOwner((Identity)getContext().getObjectByName(Identity.class, owner));
 				}
 				if(displayName!=null)
 				{
 	 				managedAttr.setDisplayName(displayName);
 				}
 				metaData.put(id+" value", managedAttr.getValue());
				metaData.put(id+" application", managedAttr.getApplication().getName());
				metaData.put(id+" attrName", managedAttr.getAttribute());
				if(newMangedAttribute)
				{
				metaData.put(id+" action", "Created/Updated Managed Attribute");
				}
				else
				{
				metaData.put(id+" action", "Updated Managed Attribute");
				
				}
 				managedAttr.setRequestable(true);
				getContext().saveObject(managedAttr);
				getContext().commitTransaction();
				getContext().decache();
				metaData.put((String)managedAttr.getId()+" outcome","Success");
					
			}
   	     }
   	     catch (Exception ex)
   	     {
   	    	 log.error("Error updating managed entitlement "+ex.getMessage());
   	    	metaData.put(id+" outcome","Failure");
   	  	metaData.put(id+" failure",ex.getMessage());
   	     }
   	    log.trace("End setOwnersAndAdditionalAttributes "+ metaData);
			return metaData;
			  
	}
	
    @POST @Path("/manage")
	/**
	 * This rest service  manages the  managed attribute
	 * @param map
	 * @return RequestResult
	 * @throws GeneralException
	 */
    public RequestResult manageEntitlements(List<HashMap> inputList) throws GeneralException 
    {
    	log.trace("Enter manageEntitlements ");
    	authorize(new RightAuthorizer(SPRight.WebServices));
    	 RequestResult result = new RequestResult(); 
      	 Map mainMap= new HashMap();
       	 HashMap<String,Object> outcome=null;
  	 for(HashMap<String,Object> innerMap: inputList)
	  	 {
	    	 ManagedAttribute ma = findManagedAttribute(innerMap);
	    	 if(ma!=null)
	    	 {
	    		
	    		 outcome=setOwnersAndAdditionalAttributes(ma,innerMap,false);
	    		 if(outcome!=null)
	    			 mainMap.putAll(outcome);
	    	 }
	    	 else
	    	 {
	    		 outcome=setNewManagedEntAndOwners(innerMap);
	    		 if(outcome!=null)
	    			 mainMap.putAll(outcome);
	    	 }
	    	 
	  	 }
     	log.trace("End manageEntitlements "+ mainMap);
     	result.setMetaData(mainMap);
    	 return result;
    	
    }
}
