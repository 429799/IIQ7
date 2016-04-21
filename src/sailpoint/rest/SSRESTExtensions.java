package sailpoint.rest;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.util.Iterator;
import java.util.Set;

/**
 * GE Custom REST Extension
 */
public class SSRESTExtensions extends SailPointRestApplication {

    private static final Log log = LogFactory.getLog(SSRESTExtensions.class);

    @Override
    public Set<Class<?>> getClasses() {

        Set<Class<?>> classes = super.getClasses();
        classes.add(SSRESTServiceIdentityListResource.class);
        classes.add(SSRESTServiceIdentityResource.class);
        classes.add(SSRESTServiceManagedAttributeResource.class);
        classes.add(SSRESTServiceBundleResource.class);
        classes.add(SSRESTServiceIdentityRequestResource.class);
        classes.add(SSRESTAuditEventsResource.class);
        classes.add(SSRESTIIQService.class);
        classes.add(SSAppConnectivity.class);
        classes.add(SSRESTIIQService.class);
   


        for (Iterator<Class<?>> iterator = classes.iterator(); iterator.hasNext(); ) {
            Class<?> next = iterator.next();
            log.debug("GERESTExtensions"+next.getName());
        }
        log.debug("Loading GE REST EXTENSIONS###");
        return classes;
    }
}