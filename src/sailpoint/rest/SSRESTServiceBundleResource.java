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
 * REST methods for the "Bundle" resource.
 *
 * @author <a href="mailto:rohit.gupta@sailpoint.com">Rohit Gupta</a>
 */
@Path("/ss/bundles")
public class SSRESTServiceBundleResource extends BaseResource{
	private static final Log log = LogFactory.getLog(SSRESTServiceBundleResource.class);
	@QueryParam("type") protected String bundleType;
	
    /**
     * This method returns the total number of bundle count based on query parameter "type"
     * @return count for given bundle type.
     **/
     @GET @Path("/count")
     public String getBundleCount()throws GeneralException 
     {
    	 log.trace("Enter getBundleCount");
    	 authorize(new RightAuthorizer(SPRight.WebServices));
    	 QueryOptions qo = new QueryOptions();
    	 log.debug("getBundleCount qo "+qo);
    	 log.trace("Exit getBundleCount");
    	 qo=getBunldeQueryOptions();
     int count = getContext().countObjects(Bundle.class, qo);
     return Integer.toString(count);
     }
     /**
      * This method returns bundle attributes based on query parameter "type"
      * @return ListResult JSON with representations of bundles 
      * @throws GeneralException
      */
     @GET
     public ListResult getBundles() throws GeneralException {
    	 log.trace("Enter getBundles");
    	 authorize(new RightAuthorizer(SPRight.WebServices));
    	     QueryOptions qo = getBunldeQueryOptions();
          int totalCount= getContext().countObjects(Bundle.class, qo);
          log.debug("getBundles totalCount "+totalCount);
          ListResult listResult = new ListResult(getBundleResults(qo), totalCount);
         log.debug("getBundles listResult "+listResult);
         log.trace("Exit getBundles");
         return listResult;
         
     
     }
     /**
      * This method builds up query options based on query parameter
      * @return
      * @throws GeneralException
      */
     private QueryOptions getBunldeQueryOptions()
             throws GeneralException {
    	 log.trace("Enter getBunldeQueryOptions");
         QueryOptions ops = new QueryOptions();
         
         String bundleType=getBundleTypes();
         if(bundleType!=null )
         {
         ops.add(Filter.eq("type", bundleType));
         }
         log.debug("getBunldeQueryOptions "+ ops);
         log.trace("Exit getBunldeQueryOptions");
         return ops;
     }

     /**
      * Given the Bundle types query parameter, return full set of bundle types 
      * to include in list.  
      * 
      * @return String
      * */
     
     private String getBundleTypes() {
    	 log.trace("Enter getBundleTypes");
     String queryBundleType=null;

         if (null != this.bundleType) {
        	 queryBundleType=this.bundleType;
         }
     log.debug("getBundleTypes  "+queryBundleType);
    	 log.trace("End getBundleTypes");
         
         return queryBundleType;
     }
     /**
      * This method returns the list of bundles based on query options
      * @param qo
      * @return List
      * @throws GeneralException
      */
     private List<Map<String,Object>> getBundleResults(QueryOptions qo)throws GeneralException 
     {
    	 log.trace("Enter getBundleResults");
		Iterator<Bundle> rows = getContext().search(Bundle.class, qo);
		
		List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
		if (rows != null) 
		{
			while (rows.hasNext()) 
			{
				Bundle item= rows.next();
				 log.debug(" getBundleResults String row"+item.toString());
				 log.debug(" getBundleResults XML row"+item.toXml());
			HashMap map = new HashMap();
			map.put("id", item.getId());
			if(item.getName()!=null)
			map.put("name", item.getName());
			if(item.getOwner()!=null && item.getOwner().getName()!=null)
			map.put("bundleOwner", item.getOwner().getName());
			if(item.getDescription(Locale.ENGLISH)!=null)
			map.put("description",item.getDescription(Locale.ENGLISH));
			if(item.getDisplayName()!=null)
			map.put("displayName", item.getDisplayName());
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
			results.add(map);
			}
		}
		makeJsonSafeKeys(results);
		log.trace("getBundleResults results "+results);
		log.trace("End getBundleResults");
		return results;
    }

}
