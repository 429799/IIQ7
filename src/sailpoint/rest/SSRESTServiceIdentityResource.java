/* (c) Copyright 2009 SailPoint Technologies, Inc., All Rights Reserved. */
package sailpoint.rest;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.SailPointContext;
import sailpoint.authorization.AllowAllAuthorizer;
import sailpoint.authorization.CapabilityAuthorizer;
import sailpoint.authorization.LcmRequestAuthorizer;
import sailpoint.authorization.RightAuthorizer;
import sailpoint.authorization.UnauthorizedAccessException;
import sailpoint.authorization.WorkItemAuthorizer;
import sailpoint.integration.IIQClient;
import sailpoint.object.Application;
import sailpoint.object.Capability;
import sailpoint.object.Configuration;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.ObjectConfig;
import sailpoint.object.QueryOptions;
import sailpoint.object.RemoteLoginToken;
import sailpoint.object.SPRight;
import sailpoint.object.SailPointObject;
import sailpoint.object.Schema;
import sailpoint.object.WorkItem;
import sailpoint.service.IdentityDetailsService;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Internationalizer;
import sailpoint.tools.Message;
import sailpoint.tools.Util;
import sailpoint.web.lcm.LCMConfigService;
import sailpoint.web.lcm.RequestPopulationBean;
import sailpoint.web.messages.MessageKeys;
import sailpoint.web.view.IdentitySummary;


/**
 * REST methods for the "identities" resource.
 *
 * @author <a href="mailto:rohit.gupta@sailpoint.com">Rohit Gupta</a>
 */
public class SSRESTServiceIdentityResource extends BaseResource {
    
    String identityName;
    private static final Log log = LogFactory.getLog(SSRESTServiceIdentityResource.class);
    

    /**
     * Constructor for this sub-resource.
     * 
     * @param  identityName  The name of the identity this sub-resource is
     *                       servicing.
     * @param  parent        The parent of this sub-resource.
     */
    public SSRESTServiceIdentityResource(String identityName, BaseResource parent) {
    
        super(parent);
    	    log.trace("Enter GERESTServiceIdentityResource");
        this.identityName = decodeRestUriComponent(identityName);
        log.debug("GERESTServiceIdentityResource idenityName " + identityName);
     	log.trace("End GERESTServiceIdentityResource");
    }

    
   
    /**
     * Return a map representation of the given identity.
     * 
     * @param  identityName  The name of the identity.
     * 
     * @return A Map representation of the given identity.
     */
    @GET
    public Map<String,Object> getIdentity()
        throws GeneralException {
    	log.trace("Enter GERESTServiceIdentityResource getIdentity");
    	authorize(new RightAuthorizer(SPRight.WebServices));
      	log.trace("End GERESTServiceIdentityResource getIdentity");
        return getHandler().showIdentity(identityName);
    }
    /**
     * This method returns the summary of the given Identity
     * @return IdentitySummary
     * @throws GeneralException
     */

    @GET @Path("/summary")
    public IdentitySummary getIdentitySummary()
        throws GeneralException {
    	log.trace("Enter  getIdentitySummary");
    	authorize(new AllowAllAuthorizer());

        IdentitySummary summary = null;
        QueryOptions ops =  new QueryOptions(Filter.eq("name", identityName));
        ops.add(Filter.or(Filter.eq("workgroup", true), Filter.eq("workgroup", false)));
        Iterator<Object[]> results = this.getContext().search(Identity.class,ops, Arrays.asList("name", "displayName"));
        if (results.hasNext()){
            Object[] row = results.next();
            summary = new IdentitySummary(null, (String)row[0], (String)row[1]);
        }
     	log.trace("End  getIdentitySummary");
        return summary;
    }

    
}
