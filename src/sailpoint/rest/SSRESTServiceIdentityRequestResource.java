package sailpoint.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.ObjectUtil;
import sailpoint.authorization.RightAuthorizer;
import sailpoint.integration.ListResult;
import sailpoint.integration.RequestResult;
import sailpoint.object.Bundle;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.IdentityRequest;
import sailpoint.object.IdentityRequestItem;
import sailpoint.object.QueryOptions;
import sailpoint.object.SPRight;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.WorkItem;
import sailpoint.tools.GeneralException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
/**
 * REST methods for the "IdentityRequest" resource.
 *
 * @author <a href="mailto:rohit.gupta@sailpoint.com">Rohit Gupta</a>
 */
@Path("/ss/ir")
public class SSRESTServiceIdentityRequestResource extends BaseResource{
	private static final Log log = LogFactory.getLog(SSRESTServiceIdentityRequestResource.class);
	@QueryParam("requestor") protected String requestor;
	@QueryParam("requestee") protected String requestee;
	@QueryParam("requestId") protected String requestId;
	@QueryParam("requestType") protected String requestType;
	@QueryParam("type") protected String type;
  
     /**
      * This method returns identity request attributes based on query parameter "requestor" and "requestee"
      * @return ListResult JSON with representations of IR 
      * @throws GeneralException
      */
     @GET
     public ListResult getIdentityRequests() throws GeneralException {
    	log.debug("Enter getIdentityRequests");
    	 authorize(new RightAuthorizer(SPRight.WebServices));
    	     QueryOptions qo = getIRQueryOptions();
          int totalCount= getContext().countObjects(IdentityRequest.class, qo);
          log.debug("getIdentityRequests totalCount "+totalCount);
          ListResult listResult = new ListResult(getIRResults(qo), totalCount);
         log.debug("getIdentityRequests listResult "+listResult);
        log.debug("Exit getIdentityRequests");
         return listResult;
         
     
     }
     
     private QueryOptions getIRQueryOptions()
             throws GeneralException {
    	log.debug("Enter getIRQueryOptions");
         QueryOptions ops = new QueryOptions();
         String requestor=getRequestor();
         String requestee=getRequestee();
         String requestId=getRequestId();
         String requestType=getRequesttype();
         String type=getType();
         if(type!=null)
         {
        	  ops.add(Filter.eq("type",type));
         }
         else
         {
        	 ops.add(Filter.eq("type","AccessRequest"));
         }
         if(requestId!=null )
         {
         ops.add(Filter.eq("name", requestId));
         }
         if(requestor!=null )
         {
         ops.add(Filter.eq("requesterDisplayName", requestor));
         }
         if(requestee!=null )
         {
         ops.add(Filter.eq("targetDisplayName", requestee));
         }
      
         log.debug("getIRQueryOptions "+ ops);
        log.debug("Exit getIRQueryOptions");
         return ops;
     }

     /**
      * Given the requestor query parameter, return full set of IR 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getRequestor() {
    	log.debug("Enter getRequestor");
     String queryRequestor=null;

         if (null != this.requestor) {
        	 queryRequestor=this.requestor;
         }
     log.debug("getRequestor  "+queryRequestor);
    	log.debug("End getRequestor");
         
         return queryRequestor;
     }
     
     
     /**
      * Given the type query parameter, return full set of IR 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getType() {
    	log.debug("Enter getType");
     String queryType=null;

         if (null != this.type) {
        	 queryType=this.type;
         }
     log.debug("getType  "+queryType);
    	log.debug("End getType");
         
         return queryType;
     }
     
     /**
      * Given the requestee query parameter, return full set of IR 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getRequestId() {
    	log.debug("Enter getRequestId");
     String queryRequestId=null;

         if (null != this.requestId) {
        	 queryRequestId=this.requestId;
         }
     log.debug("getRequestId  "+queryRequestId);
    	log.debug("End getRequestId");
         
         return queryRequestId;
     }
     
     
     /**
      * Given the requesttype query parameter, return full set of IR 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getRequesttype() {
    	log.debug("Enter getRequesttype");
     String queryRequesttype=null;

         if (null != this.requestType) {
        	 queryRequesttype=this.requestType;
         }
     log.debug("getRequesttype  "+queryRequesttype);
    	log.debug("End getRequestId");
         
         return queryRequesttype;
     }
     
     /**
      * Given the requestee query parameter, return full set of IR 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getRequestee() {
    	log.debug("Enter getRequestee");
     String queryRequestee=null;

         if (null != this.requestee) {
        	 queryRequestee=this.requestee;
         }
     log.debug("getRequestee  "+queryRequestee);
    	log.debug("End getRequestee");
         
         return queryRequestee;
     }
     
     private List<Map<String,Object>> getIRResults(QueryOptions qo)throws GeneralException 
     {
    	log.debug("Enter getIRResults");
		Iterator<IdentityRequest> rows = getContext().search(IdentityRequest.class, qo);
		
		List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
		if (rows != null) 
		{
			while (rows.hasNext()) 
			{
				IdentityRequest item= rows.next();
				 log.debug(" getIRResults String row"+item.toString());
				 log.debug(" getIRResults XML row"+item.toXml());
			HashMap map = new HashMap();
			map.put("id", item.getId());
			if(item.getName()!=null)
			map.put("name", item.getName());
			if(item.getOwner()!=null && item.getOwner().getName()!=null)
			map.put("owner", item.getOwner().getName());
			if(item.getRequesterDisplayName()!=null)
			{
			map.put("requestor", item.getRequesterDisplayName());
			}
			if(item.getTargetDisplayName()!=null)
			{
			map.put("requestee", item.getTargetDisplayName());
			}
			if(item.getErrors()!=null)
			{
			map.put("errors", item.getErrors());
			}
			if(item.getEndDate()!=null)
			{
			map.put("endDate", item.getEndDate().toString());
			}
			if(item.getCreated()!=null)
			{
			map.put("created", item.getCreated().toString());
			}
			if(item.getMessages()!=null)
			{
			map.put("meassages", item.getMessages());
			}
			if(item.getCompletionStatus()!=null)
			{
			map.put("completionStatus", item.getCompletionStatus());
			}
			if(item.getExecutionStatus()!=null)
			{
			map.put("executionStatus", item.getExecutionStatus());
			}
			

			if(item.getAssignedScope()!=null)
			{
				if(item.getAssignedScope()!=null&& item.getAssignedScope().getName()!=null)
				{
						map.put("assignedScopeName", item.getAssignedScope().getName());
				}
			}
			if(item.getType()!=null)
			{
			map.put("type", item.getType());
			}
			if(item.getCreated()!=null){
				map.put("Creationdate", item.getCreated());
			}
			
			List<IdentityRequestItem> items=item.getItems();
			if(items!=null)
			{ 
				int count=0;
				for(IdentityRequestItem identityRequestItem:items)
				{
					if(identityRequestItem!=null)
					{
						map.put("identityRequestItem " + count, identityRequestItem.toXml());
						count=count+1;
					}
					
				}
			}
			
			log.debug("map "+map);
			
			if(this.requestType!=null && this.requestType.equalsIgnoreCase("OneUI REST"))
			{
				if(item.getExecutionStatus()==null || !"Completed".equalsIgnoreCase(item.getExecutionStatus().toString()))
						{
						results.add(map);
						}

			}else
			{
				results.add(map);
			}
			}
		}
		makeJsonSafeKeys(results);
		log.debug("getIRResults results "+results);
		log.debug("End getIRResults");
		return results;
    }

}
