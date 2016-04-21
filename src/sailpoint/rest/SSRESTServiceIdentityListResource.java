/**
 * 
 */
package sailpoint.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.ObjectUtil;
import sailpoint.authorization.IdentityMatchAuthorizer;
import sailpoint.authorization.LcmEnabledAuthorizer;
import sailpoint.authorization.RightAuthorizer;
import sailpoint.integration.IIQClient;
import sailpoint.integration.ListResult;
import sailpoint.object.Application;
import sailpoint.object.ApprovalItem;
import sailpoint.object.ApprovalSet;
import sailpoint.object.Attributes;
import sailpoint.object.CertificationItem;
import sailpoint.object.ColumnConfig;
import sailpoint.object.Filter;
import sailpoint.object.GroupDefinition;
import sailpoint.object.Identity;
import sailpoint.object.IdentityEntitlement;
import sailpoint.object.IdentityRequestItem;
import sailpoint.object.Link;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.QueryOptions;
import sailpoint.object.SPRight;
import sailpoint.object.UIConfig;
import sailpoint.object.WorkItem;
import sailpoint.rest.ui.ApprovalWorkItemListResource.ApprovalItemDecision;
import sailpoint.service.ApprovalItemsService;
import sailpoint.tools.GeneralException;
import sailpoint.tools.JSONDeserializerFactory;
import sailpoint.tools.Util;
import sailpoint.web.identity.IdentityListBean;
import sailpoint.web.lcm.AccessRequestBean;
import sailpoint.web.lcm.BaseRequestBean;
import sailpoint.web.lcm.EntitlementsRequestBean;
import sailpoint.web.lcm.LCMConfigService;
import sailpoint.web.lcm.RequestPopulationBean;
import sailpoint.web.lcm.RolesRequestBean;
import sailpoint.web.util.WebUtil;

/**
 * @author <a href="mailto:rohit.gupta@sailpoint.com">Rohit Gupta</a>
 */
@Path("/ss/identities")
public class SSRESTServiceIdentityListResource extends BaseListResource {
	 @QueryParam("type") protected String workItemType;
    private static final Log log = LogFactory.getLog(SSRESTServiceIdentityListResource.class);
    private static final String COL_ID = "id";
    private WorkItem workItem;
    private Identity identity;
    private Application application;
   
    private static final List<String> WORK_ITEM_TYPES = 
            new ArrayList<String>(Arrays.asList(
                    WorkItem.Type.Approval.toString(),
                    WorkItem.Type.ManualAction.toString()          
                 ));
    
   

    /* Calculated column data indices */
    private static final String COL_APPROVAL_ITEMS = "approvalItems";
    


    /** Sub Resource Methods **/
    @Path("{identityName}")
    public SSRESTServiceIdentityResource getIdentity(@PathParam("identityName") String identityName)
    throws GeneralException {
    	 log.trace("Enter  GERESTServiceIdentityListResource getIdentity");
     log.trace("End GERESTServiceIdentityListResource getIdentity");
        return new SSRESTServiceIdentityResource(identityName, this);
    }
    /**
     * @param {identityNameOrId} - it is identityName or ID for whom workItem count is calculated.
     * @param {type} - user has workItem and workItem type can be Approval, Certification, Delegation etc...
     * If type is null/empty or all, then this method calculates count of all work item associated with Identity.
     * @return count for given work item type.
     **/
     @GET @Path("{identityNameOrId}/workItemsCount")
     public String getWorkItemCount(@PathParam("identityNameOrId") String identityNameOrId
                                    )
         throws GeneralException {
    	 log.trace("Enter getWorkItemCount");
    	 
    		 identityNameOrId = decodeRestUriComponent(identityNameOrId);
        this.identity = getContext().getObject(Identity.class, identityNameOrId);
         if (null == identity) {
             throw new GeneralException("Unable to find identity");
         }
         QueryOptions qo = new QueryOptions();
         if (this.workItemType!=null ) 
         {
        	 qo.add(Filter.and(Filter.eq("owner", identity),
                     Filter.eq("type", this.workItemType)));

         }
           
         
         else {
        	  qo.add(Filter.eq("owner", identity));
         }
    	 log.debug("getWorkItemCount qo "+qo);
    	 log.trace("Exit getWorkItemCount");
    	 
            
         int count = getContext().countObjects(WorkItem.class, qo);
         return Integer.toString(count);
     }
     
     /**
      * List all the approval work items of the specified type of identity request.   
      * @return ListResult JSON with representations of work items with approval sets 
      * @throws GeneralException
      */
     @GET @Path("{identityNameOrId}/workItems")
     public ListResult getWorkItems(@PathParam("identityNameOrId") String identityNameOrId) throws GeneralException {
    	 log.trace("Enter getWorkItems");
    	 identityNameOrId = decodeRestUriComponent(identityNameOrId);
        this.identity = getContext().getObject(Identity.class, identityNameOrId);
         if (null == identity) {
             throw new GeneralException("Unable to find identity");
         }
         QueryOptions qo = getWorkItemQueryOptions();
          int totalCount= getContext().countObjects(WorkItem.class, qo);
          log.debug("getWorkItems totalCount "+totalCount);
          ListResult listResult = new ListResult(getResults(qo), totalCount);
         log.debug("getWorkItems listResult "+listResult);
         log.trace("Exit getWorkItems");
         return listResult;
         
     
     }
     
	   /**
      * List of all  links for an identity.   
      * @return ListResult JSON with representations of links 
      * @throws GeneralException
      */

  @POST @Path("{identityNameOrId}/links")
     public ListResult getLinks(@PathParam("identityNameOrId") String identityNameOrId, @QueryParam("applicationNameOrId") String applicationNameOrId) throws GeneralException {
    	 log.trace("Enter getLinks");
    	 identityNameOrId = decodeRestUriComponent(identityNameOrId);
        this.identity = getContext().getObject(Identity.class, identityNameOrId);
        QueryOptions qo = getLinkQueryOptions();
         if (null == identity) {
             throw new GeneralException("Unable to find identity");
         }
         if (null != applicationNameOrId) {
        	 Application app =
                     getContext().getObject(Application.class, decodeRestUriComponent(applicationNameOrId));
                 qo.add(Filter.eq("application", app));
         }
         
       
          int totalCount= getContext().countObjects(Link.class, qo);
          log.debug("getLinks totalCount "+totalCount);
          ListResult listResult = new ListResult(getLinkResults(qo), totalCount);
         log.debug("getLink listResult "+listResult);
         log.trace("Exit getLinks");
         return listResult;
         
     
     }



     /**
      * List of all  entitlements for an identity based on an application.   
      * @return ListResult JSON with representations of entitlements 
      * @throws GeneralException
      */
     @GET @Path("{identityNameOrId}/{applicationNameOrId}/entitlements")
     public ListResult getApplicationEntitlements(@PathParam("identityNameOrId") String identityNameOrId,@PathParam("applicationNameOrId") String applicationNameOrId) throws GeneralException {
    	 log.trace("Enter getEntitlements");
    	 identityNameOrId = decodeRestUriComponent(identityNameOrId);
    	 applicationNameOrId = decodeRestUriComponent(applicationNameOrId);
        this.identity = getContext().getObject(Identity.class, identityNameOrId);
        this.application = getContext().getObject(Application.class, applicationNameOrId);
         if (null == identity) {
             throw new GeneralException("Unable to find identity");
         }
         if (null == application) {
             throw new GeneralException("Unable to find application");
         }
         QueryOptions qo = getEntitlementQueryOptions();
          int totalCount= getContext().countObjects(IdentityEntitlement.class, qo);
          log.debug("getEntitlements totalCount "+totalCount);
          ListResult listResult = new ListResult(getEntitlementResults(qo), totalCount);
         log.debug("getEntitlements listResult "+listResult);
         log.trace("Exit getEntitlements");
         return listResult;
         
     
     }
     
     /**
      * List of all entitlements for an identity.   
      * @return ListResult JSON with representations of work items with approval sets 
      * @throws GeneralException
      */
     @GET @Path("{identityNameOrId}/entitlements")
     public ListResult getEntitlements(@PathParam("identityNameOrId") String identityNameOrId) throws GeneralException {
    	 log.trace("Enter getEntitlements");
    	 identityNameOrId = decodeRestUriComponent(identityNameOrId);
    	    this.identity = getContext().getObject(Identity.class, identityNameOrId);
         if (null == identity) {
             throw new GeneralException("Unable to find identity");
         }
        
         QueryOptions qo = getEntitlementQueryOptions();
          int totalCount= getContext().countObjects(IdentityEntitlement.class, qo);
          log.debug("getEntitlements totalCount "+totalCount);
          ListResult listResult = new ListResult(getEntitlementResults(qo), totalCount);
         log.debug("getEntitlements listResult "+listResult);
         log.trace("Exit getEntitlements");
         return listResult;
         
     
     }
     
     
     

     private QueryOptions getWorkItemQueryOptions()
             throws GeneralException {
    	 log.trace("Enter getWorkItemQueryOptions");
         QueryOptions ops = super.getQueryOptions(colKey);
         
         //Join to IdentityRequest table for type filter
         ops.add(Filter.join("identityRequestId", "IdentityRequest.name"));
         String workItemType=getWorkItemTypes();
         if(workItemType!=null )
         {
         ops.add(Filter.eq("type", workItemType));
         }
         ops.add(ObjectUtil.getOwnerFilterForIdentity(this.identity));
         log.debug("getWorkItemQueryOptions "+ ops);
         log.trace("Exit getWorkItemQueryOptions");
         return ops;
     }



 private QueryOptions getLinkQueryOptions()
             throws GeneralException {
    	 log.trace("Enter getLinkQueryOptions");
    	 QueryOptions qo = new QueryOptions();
         qo.add(Filter.eq("identity", getIdentity()));
         
                 
         log.debug("getLinkQueryOptions "+ qo);
         log.trace("Exit getLinkQueryOptions");
         return qo;
     }

	   private Identity getIdentity() throws GeneralException {
         return getContext().getObject(Identity.class, this.identity.getName());
     }

     
     private QueryOptions getEntitlementQueryOptions()
             throws GeneralException 
             {
    	    log.trace("Enter getEntitlementQueryOptions");
         QueryOptions ops = new QueryOptions();
         if(this.identity!=null)
         {
        	 ops.add(Filter.eq("identity", this.identity));
         }
         if(this.application!=null)
         {
        	 ops.add(Filter.eq("application", this.application));
         }
         log.debug("getEntitlementQueryOptions "+ ops);
         log.trace("Exit getEntitlementQueryOptions");
         return ops;
     }

     
     /**
      * Given the workItemTypes query parameter, return full set of work item types 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getWorkItemTypes() {
    	 log.trace("Enter getWorkItemTypes");
     String queryWorkItemType=null;

         if (null != this.workItemType) {
        	 queryWorkItemType=this.workItemType;
         }
     log.debug("getWorkItemTypes queryWorkItemType "+queryWorkItemType);
    	 log.trace("End getWorkItemTypes");
         
         return queryWorkItemType;
     }

    
     
     public List<Map<String,Object>> getResults(QueryOptions qo)throws GeneralException 
     {
    	 log.trace("Enter getResults");
		Iterator<WorkItem> rows = getContext().search(WorkItem.class, qo);
		
		List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
		if (rows != null) 
		{
			while (rows.hasNext()) 
			{
				WorkItem item= rows.next();
				 log.debug(" getResults String row"+item.toString());
				 log.debug(" getResults XML row"+item.toXml());
			HashMap map = new HashMap();
			map.put("id", item.getId());
			if(item.getName()!=null)
			map.put("name", item.getName());
			if(item.getIdentityRequestId()!=null)
			map.put("identityRequestId", item.getIdentityRequestId());
			if(item.getOwner()!=null&& item.getOwner().getName()!=null)
			map.put("ownerName",item.getOwner().getName());
			if(item.getAssignee()!=null&& item.getAssignee().getName()!=null)
			map.put("assigneeName", item.getAssignee().getName());
			if(item.getDescription()!=null)
			map.put("workItemDesc", item.getDescription());
			if(item.getRequester()!=null&& item.getRequester().getName()!=null)
			map.put("requesterName", item.getRequester().getName());
			if(item.getTargetName()!=null)
			map.put("targetName", item.getTargetName());
			if(item.getCreated()!=null)
			map.put("Creationdate", item.getCreated());
			if(item.getAttributes()!=null)
			map.put("Attributes", item.getAttributes());
			results.add(map);
			}
		}
		makeJsonSafeKeys(results);
		log.trace("getResults results "+results);
		log.trace("End getResults");
		return results;
    }
    


  public List<Map<String,Object>> getLinkResults(QueryOptions qo)throws GeneralException 
     {
    	 log.trace("Enter getResults");
		Iterator<Link> rows = getContext().search(Link.class, qo);
		
		List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
		if (rows != null) 
		{
			while (rows.hasNext()) 
			{
				Link link= rows.next();
				 log.debug(" getResults String row"+link.toString());
				 log.debug(" getResults XML row"+link.toXml());
			HashMap map = new HashMap();
			map.put("id", link.getApplicationId());
			map.put("isLinkDisabled", link.isDisabled());
			if(link.getApplicationName()!=null)
			map.put("name", link.getApplicationName());
			if(link.getDisplayName() !=null)
			map.put("ApplicationDisplayname", link.getDisplayName());
			if(link.getOwner()!=null&& link.getOwner().getName()!=null)
			map.put("ownerName",link.getOwner().getName());
			if(link.getDescription()!=null)
			map.put("Desc", link.getDescription());
			if(link.getCreated()!=null)
			map.put("Creationdate", link.getCreated());
			if(link.getAttributes()!=null)
			map.put("Attributes", link.getAttributes());
			results.add(map);
			}
		}
		makeJsonSafeKeys(results);
		log.trace("getResults results "+results);
		log.trace("End getResults");
		return results;
    }



     
     public List<Map<String,Object>> getEntitlementResults(QueryOptions qo)throws GeneralException 
     {
    	 log.trace("Enter getEntitlementResults");
		Iterator<IdentityEntitlement> rows = getContext().search(IdentityEntitlement.class, qo);
		
		List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
		if (rows != null) 
		{
			while (rows.hasNext()) 
			{
			IdentityEntitlement item= rows.next();
			log.debug(" getEntitlementResults String row"+item.toString());
			log.debug(" getEntitlementResults XML row"+item.toXml());
			HashMap map = new HashMap();
			map.put("id", item.getId());
			if(item.getName()!=null)
			map.put("attribute", item.getName());
			if(item.getIdentity()!=null&& item.getIdentity().getName()!=null)
		    map.put("identityName", item.getIdentity().getName());
			if(item.getValue()!=null)
			map.put("value", item.getValue());
			if(item.getNativeIdentity()!=null)
			map.put("accountName", item.getNativeIdentity());
			if(item.getAssigner()!=null)
			map.put("assigner", item.getAssigner());
			if(item.getType()!=null)
			map.put("type", item.getType());
			if(item.getSource()!=null)
			map.put("source",item.getSource());
			if(item.getAggregationState()!=null)
			map.put("aggregationState", item.getAggregationState());
			if(item.getDescription()!=null)
			map.put("description", item.getDescription());
			if(item.getAppName()!=null)
			map.put("applicatioName", item.getAppName());
			if(item.getDisplayName()!=null)
			map.put("displayName", item.getDisplayName());
			if(item.getPendingRequestItem()!=null)
			{
			map.put("pendingrequest", "true");
			}
			else
			{
			map.put("pendingrequest", "false");	
			}
			if(item.getPendingCertificationItem()!=null)
			{
			map.put("pendingcertification", "true");
			}
			else
			{
			map.put("pendingcertification", "false");	
			}
			if(item.isAllowed())
			{
			map.put("isAllowed", "true");
			}
			else
			{
			map.put("isAllowed", "false");
			}
			if(item.isGrantedByRole())
			{
			map.put("isGrantedByRole", "true");
			}
			else
			{
			map.put("isGrantedByRole", "false");
			}
			if(item.isAssigned())
			{
			map.put("isAssigned", "true");
			}
			else
			{
			map.put("isAssigned", "false");
			}
			IdentityRequestItem iRItem=item.getRequestItem();
			if(iRItem!=null)
			{
				if(iRItem.getIdentityRequest()!=null && iRItem.getIdentityRequest().getName()!=null)
				{
					map.put("lastRequestId", iRItem.getIdentityRequest().getName());
				}
			}
			
			CertificationItem certItem=item.getCertificationItem();
			if(certItem!=null)
			{
				if(certItem.getModified()!=null)
				{
					map.put("lastCertDate", certItem.getModified().toString());
				}
				if(certItem.getParent()!=null && certItem.getParent().getCertification()!=null && certItem.getParent().getCertification().getName()!=null)
				{
					map.put("lastCertName", certItem.getParent().getCertification().getName());
				}
				
				
			}
			results.add(map);
			}
		}
		makeJsonSafeKeys(results);
		log.trace("getEntitlementResults results "+results);
		log.trace("End getEntitlementResults");
		return results;
    }
    
     
     
     
   
}
