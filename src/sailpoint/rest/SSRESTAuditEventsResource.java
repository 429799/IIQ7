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
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;
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
 * REST methods for the "Audit" resource.
 *
 * @author <a href="mailto:rohit.gupta@sailpoint.com">Rohit Gupta</a>
 */
@Path("/ss/ae")
public class SSRESTAuditEventsResource extends BaseResource{
	private static final Log log = LogFactory.getLog(SSRESTAuditEventsResource.class);
	@QueryParam("target") protected String target;
	@QueryParam("source") protected String source;
	@QueryParam("action") protected String action;
	

  
     /**
      * This method returns identity request attributes based on query parameter "target" and "source"
      * @return ListResult JSON with representations of IR 
      * @throws GeneralException
      */
     @GET
     public ListResult getAuditEvents() throws GeneralException {
    	log.debug("Enter getAuditEvents");
    	 authorize(new RightAuthorizer(SPRight.WebServices));
    	     QueryOptions qo = getAEQueryOptions();
          int totalCount= getContext().countObjects(AuditEvent.class, qo);
          log.debug("getAuditEvents totalCount "+totalCount);
          ListResult listResult = new ListResult(getAEResults(qo), totalCount);
         log.debug("getAuditEvents listResult "+listResult);
        log.debug("Exit getAuditEvents");
         return listResult;
         
     
     }
     
     private QueryOptions getAEQueryOptions()
             throws GeneralException {
    	log.debug("Enter getIRQueryOptions");
         QueryOptions ops = new QueryOptions();
         String target=getTarget();
         String source=getsource();
         String action=getAction();
         
         if(action!=null )
         {
         ops.add(Filter.eq("action", action));
         }
         if(target!=null )
         {
         ops.add(Filter.eq("target", target));
         }
         if(source!=null )
         {
         ops.add(Filter.eq("source", source));
         }
         log.debug("getAEQueryOptions "+ ops);
        log.debug("Exit getAEQueryOptions");
         return ops;
     }

     /**
      * Given the target query parameter, return full set of IR 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getTarget() {
    	log.debug("Enter getTarget");
     String querytarget=null;

         if (null != this.target) {
        	 querytarget=this.target;
         }
     log.debug("getTarget  "+querytarget);
    	log.debug("End getTarget");
         
         return querytarget;
     }
     
     
     
     /**
      * Given the source query parameter, return full set of AE 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getAction() {
    	log.debug("Enter getAction");
     String queryaction=null;

         if (null != this.action) {
        	 queryaction=this.action;
         }
     log.debug("getAction  "+queryaction);
    	log.debug("End getAction");
         
         return queryaction;
     }
     
     
     /**
      * Given the source query parameter, return full set of IR 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getsource() {
    	log.debug("Enter getsource");
     String querysource=null;

         if (null != this.source) {
        	 querysource=this.source;
         }
     log.debug("getsource  "+querysource);
    	log.debug("End getsource");
         
         return querysource;
     }
     
     private List<Map<String,Object>> getAEResults(QueryOptions qo)throws GeneralException 
     {
    	log.debug("Enter getAEResults");
		Iterator<AuditEvent> rows = getContext().search(AuditEvent.class, qo);
		
		List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
		if (rows != null) 
		{
			while (rows.hasNext()) 
			{
				AuditEvent item= rows.next();
				 log.debug(" getAEResults String row"+item.toString());
				 log.debug(" getAEResults XML row"+item.toXml());
			HashMap map = new HashMap();
			map.put("id", item.getId());
			if(item.getName()!=null)
			map.put("name", item.getName());
			if(item.getOwner()!=null && item.getOwner().getName()!=null)
			map.put("owner", item.getOwner().getName());
			if(item.getSource()!=null)
			map.put("source", item.getSource());
			if(item.getAction()!=null)
			map.put("action", item.getAction());
			if(item.getTarget()!=null)
			map.put("target", item.getTarget());
			if(item.getAttributes()!=null)
			{
				Attributes attributes = item.getAttributes();
				if(attributes.getMap()!=null)
				map.put("attributes", attributes.getMap().toString());
						
			}
			
			if(item.getCreated()!=null)
			{
			map.put("created", item.getCreated().toString());
			}
			
			

			if(item.getAssignedScope()!=null)
			{
				if(item.getAssignedScope()!=null&& item.getAssignedScope().getName()!=null)
				{
						map.put("assignedScopeName", item.getAssignedScope().getName());
				}
			}
			
			if(item.getCreated()!=null){
				map.put("Creationdate", item.getCreated());
			}
			
			
			log.debug("map "+map);
			results.add(map);
			
			}
		}
		makeJsonSafeKeys(results);
		log.debug("getAEResults results "+results);
		log.debug("End getAEResults");
		return results;
    }

}
