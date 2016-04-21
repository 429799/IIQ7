package com.sailpoint.services.standard;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.integration.ApacheHttpClient;
import sailpoint.integration.HttpClient;
import sailpoint.integration.IIQClient.AuthorizationService.CheckAuthorizationResult;
import sailpoint.integration.IIQClient.IdentityService.CreateOrUpdateResult;
import sailpoint.integration.IIQClient.PasswordService.CheckPasswordResult;
import sailpoint.integration.JsonUtil;
import sailpoint.integration.RequestResult;


/**
 * Class providing a convenient interface for SailPoint IIQ web services.
 *
 * @ignore
 * This uses the Apache HttpClient to make requests using the IIQ REST
 * interface for Basic SSL (https) and Non-SSL(http) 
 * 
 */
public class SSRESTServiceClient {

    //////////////////////////////////////////////////////////////////////
    //
    // Constants
    //
    //////////////////////////////////////////////////////////////////////

	private static Log log = LogFactory.getLog(SSRESTServiceClient.class);

    private static final String CONFIG_FILE = "/iiqclient.properties";

    private static final String PROP_PREFIX = "iiqClient.";
    private static final String PROP_BASE_URL = PROP_PREFIX + "baseURL";
    private static final String PROP_USERNAME = PROP_PREFIX + "username";
    private static final String PROP_PASSWORD = PROP_PREFIX + "password";
    private static final String PROP_TIMEOUT = PROP_PREFIX + "timeout";   




    //////////////////////////////////////////////////////////////////////
    //
    // Fields
    //
    //////////////////////////////////////////////////////////////////////

    /**
     * Base URL to the IIQ server.
     */
    String _baseUrl;

    /**
     * The HttpClient to use to send requests.
     */
    HttpClient _client;

    /**
     * Username. This is the person who logs in/requests WS calls
     * This is set as part of the client set up
     * Keeping here as some WS need this info 
     * Username is assumed as plain text for now
     */
    String _username;
    
    

    //////////////////////////////////////////////////////////////////////
    //
    // Constructor. Use version with username, password (encrypted allowed 
    // also) for including authentication as part of the public methods
    // being used for the web service, specially needed for ARM
    //
    //////////////////////////////////////////////////////////////////////

    /**
     * Default constructor that initializes all properties using the config file.
     */
    public SSRESTServiceClient() throws Exception {
        this(null);
    }

    /**
     * Constructor that accepts a URL.  All other properties are initialized
     * using the config file.
     */
    public SSRESTServiceClient(String url) 
    	throws Exception {
        this(url, null, null);
    }

    /**
     * Constructor that accepts a username and password.  All other properties
     * are initialized using the config file.
     */
    public SSRESTServiceClient(String username, 
    		         String password) 
    	throws Exception {
    	this(null, username, password);
    }

    /**
     * Constructor that accepts a URL, username, and password.
     */
    public SSRESTServiceClient(String url,
    		         String username, 
    		         String password) 
    	throws Exception {

    	// pull in any defaults from the iiqclient.properties file.  Override
        // the properties file values with the specified values (if not null).
    	configure(url, username, password);
    }

    /**
     * Return the username being used for authentication.
     */
    public String getUsername() {
		return _username;
	}

    /**
     * Set the username to use for authentication.
     */
	public void setUsername(String _username) {
		this._username = _username;
	}

	private void setupClient(String url, String username, String password,
	                         String timeout, Map options)
	    throws Exception {

	    _baseUrl = url;
	    _client = new ApacheHttpClient();
	    _client.setup(checkHttpsUrl(url), getPort(url), username, password,
	                  timeout, options);
	}

    //////////////////////////////////////////////////////////////////////
    //
    // Configuration
    //
    //////////////////////////////////////////////////////////////////////

	/**
     * Configure the IIQClient reading the properties from CONFIG_FILE.
	 */
	public void configure() throws Exception {
	    this.configure(null, null, null);
	}
	
    /**
     * Configure the IIQClient reading the properties from CONFIG_FILE.
     * If there is no properties file, just initialize using the given
     * parameters.
     * 
     * @param  baseURL   The base URL to use to override the properties file.
     * @param  user      The user to use to override the properties file.
     * @param  password  The password to use to override the properties file.
     */
    public void configure(String baseURL, String user, String password)
        throws Exception {

        Properties props = new Properties();

        // Consider allowing reading this file location from a system
        // property.
        InputStream is = SSRESTServiceClient.class.getResourceAsStream(CONFIG_FILE);
        if (null != is) {
        
            props.load(is);

            if (null == baseURL) {
                baseURL = props.getProperty(PROP_BASE_URL);
            }
            

            if (null == user) {
                user = props.getProperty(PROP_USERNAME);
            }
            if (null == password) {
                password = props.getProperty(PROP_PASSWORD);
            }
            String timeout = props.getProperty(PROP_TIMEOUT);

        
            
  
            // Pull any other generic properties that we support and put them in
            // the options map.
            Map options = new HashMap();
            for (int i=0; i<HttpClient.OPTS.length; i++) {
                String val = props.getProperty(PROP_PREFIX + HttpClient.OPTS[i]);
                if (null != val) {
                    options.put(HttpClient.OPTS[i], val);
                }
            }
            
            setupClient(baseURL, user, password, timeout, options);
        } else {
            // in the absense of a file still need to setup
            setupClient(baseURL, user, password, null, null);
        }
    }

    /**
     * Set the base URL to use for requests.
     */
    public void setBaseUrl(String s) {
        _baseUrl = (s != null) ? s.trim() : null;
    }

    /**
     * Return the base URL being used for requests.
     */
    public String getBaseUrl() {
        return _baseUrl;
    }
    



    //////////////////////////////////////////////////////////////////////
    //
    // URI Building
    //
    //////////////////////////////////////////////////////////////////////

    private String formatUrl(String resource, String identity, 
                             String arg1, String value1)
        throws Exception {

        List resources = new ArrayList();
        resources.add(resource);
        if (null != identity) {
            resources.add(identity);
        }
        return formatUrl(resources, arg1, value1);
    }
    
    
    private String formatUrl(List resources) throws Exception {
    	return formatUrl(resources, null, null);
    }
    
    private String formatUrl(List resources, String arg1, String value1)
        throws Exception {

        Map parameters = null;
        if (arg1 != null && value1 != null) {
            parameters = new HashMap();
            parameters.put(arg1, value1);
        }
        return formatUrl(resources, parameters);
    }

    private String formatUrl(List resources, Map queryParameters) {

        // Construct a URI with the base so we can extract the components.
        URI baseURI = null;
        try {
            baseURI = new URI( _baseUrl);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        StringBuffer pathBuf = new StringBuffer(baseURI.getPath());
        
        // web.xml must plug the REST servlet under here
       
        if (pathBuf.charAt(pathBuf.length()-1) != '/') {
            pathBuf.append("/");
        }
        pathBuf.append("rest");

        if (resources != null) {
            for (Iterator it=resources.iterator(); it.hasNext(); ) {
                Object o = it.next();
                String s = ((o != null) ? o.toString() : null);
                if (s != null && s.length() > 0) {
                    pathBuf.append("/").append(s);
                }
            }
        }

        String query = null;
        if ((null != queryParameters) && !queryParameters.isEmpty()) {
            StringBuffer queryBuf = new StringBuffer();
            String sep = "";
            for (Iterator it=queryParameters.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                List vals = null;
                Object val = entry.getValue();
                if (val instanceof List) {
                    vals = (List) val;
                }
                else {
                    vals = new ArrayList();
                    vals.add(val);
                }

                for (Iterator valIt=vals.iterator(); valIt.hasNext(); ) {
                    queryBuf.append(sep).append(entry.getKey()).append("=").append(valIt.next());
                    sep = "&";
                }
            }
            query = queryBuf.toString();
        }
        
        // Build a URI with the components and convert it to a string.  This
        // ensures proper escaping.
        String url = null;
        try {
            URI uri = new URI(baseURI.getScheme(), null, baseURI.getHost(),
                              baseURI.getPort(), pathBuf.toString(), query, null);
            url = uri.toString();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
                          
        return url;
    }
        
    
    

   
   

    
    //////////////////////////////////////////////////////////////////////
    //
    // getConfiguration. Passes the name of a configuration value to IIQ
    // and gets the value back.
    //
    //////////////////////////////////////////////////////////////////////
    
    /**
     * The path to the configuration REST resource.
     */
    public static final String RESOURCE_CONFIGURATION = "configuration";

    /**
     * The name of the query parameter that specifies which configuration
     * value to return in {@link #getConfiguration(String)}.
     */
    public static final String ARG_ATTRIBUTE_NAME = "attributeName";
    
    /**
     * Return a system configuration value for the requested attribute.
     */
    public String getConfiguration(String configName) 
    throws Exception {
        
        String url = formatUrl(RESOURCE_CONFIGURATION, null, ARG_ATTRIBUTE_NAME, configName);
        String result = clientGet(url);
        return result;
    }
    
       /**
     * The path to the GE REST service.
     */
    public static final String RESOURCE_IDENTITIES = "ss/identities";
    public static final String RESOURCE_BUNDLES = "ss/bundles";
    public static final String RESOURCE_AE = "ss/ae";
    public static final String RESOURCE_IR = "ss/ir";
    public static final String RESOURCE_BUNDLES_COUNT = "count";
    public static final String RESOURCE_MANAGED_ENTITLEMENTS_MANAGE = "manage";
    public static final String RESOURCE_MANAGED_APPLICATION_MANAGE = "manage";
    public static final String RESOURCE_MANAGED_ENTITLEMENTS = "ss/managedEntitlements";
    public static final String RESOURCE_APPS_STATUS = "geApps";  
    public static final String RESOURCE_APPS_STATUS_INFO = "info";
    public static final String RESOURCE_IDENTITIES_WORKITEMS_COUNT = "workItemsCount";
    public static final String RESOURCE_IDENTITIES_WORKITEMS = "workItems";
    public static final String RESOURCE_IDENTITIES_ENTITLEMENTS = "entitlements";
    public static final String RESOURCE_IDENTITIES_LINKS = "links";
    public static final String SUB_RESOURCE_IDENTITIES_SUMMARY = "summary";
    /**
     * POST request body parameter that contains managed entitlement search arguments.
     */
    public static final String ARG_MANAGED_ENTITLEMENT_INPUTS_LIST = "managedEntArgsList";
    
    /**
     * POST request body parameter that contains managed entitlement search arguments.
     */
    public static final String ARG_MANAGED_ENTITLEMENT_INPUTS = "managedEntArgs";
    
    /**
     * POST request body parameter that contains Application search arguments.
     */
    public static final String ARG_MANAGED_APP_INPUTS_LIST = "managedAppArgsList";
    
   
    /**
     * Service that gets managed entitlement. 
     *      
     * URI:     
     *    /ss/managedEntitlements
     * @param managedEntArgs
     * @return managedEntArgs    The arguments for the Managed Entitlement search launch.
     * @throws Exception
     */
    public RequestResult getManagedEntitlement(Map managedEntArgs) 
        throws Exception {
        
        ArrayList resources = new ArrayList();
        resources.add(RESOURCE_MANAGED_ENTITLEMENTS);
        String url = formatUrl(resources);
        System.out.println("url "+url);
        Map inputs = new HashMap();        
        inputs.put(ARG_MANAGED_ENTITLEMENT_INPUTS, managedEntArgs);
        String argsJson = JsonUtil.render(inputs);        
        System.out.println("managedEntArgs "+managedEntArgs);
        RequestResult result = null;
        String json = clientPost(url, argsJson);
        if ( json != null ) {
            Map map = (Map)JsonUtil.parse(json); 
            result = new RequestResult();
            result.fromMap(map);
        }        
        return result;
    }
    
    
    /**
     * client that calls managed entitlement service. 
     *      
     * URI:     
     *    /ss/managedEntitlements/manage
     * @param inputList.
     *
     * @return String with the results of the search.
     */
    public RequestResult manageManagedEntitlement(List inputList) 
        throws Exception {
        
        ArrayList resources = new ArrayList();
        resources.add(RESOURCE_MANAGED_ENTITLEMENTS);
        resources.add(RESOURCE_MANAGED_ENTITLEMENTS_MANAGE);
        String url = formatUrl(resources);
        Map inputs = new HashMap();        
        String argsJson = JsonUtil.render(inputList);        
        RequestResult result = null;
        String json = clientPost(url, argsJson);
        if ( json != null ) {
            Map map = (Map)JsonUtil.parse(json); 
            result = new RequestResult();
            result.fromMap(map);
        }        
        return result;
    }
    
    
    /**
     * client that calls managed application service. 
     *      
     * URI:     
     *    /geapps/{appName}/manage
     * @param managedAppArgs    The arguments for the application.
     *
     * @return String with the results of the search.
     */
    public RequestResult manageAppExtendedAttr(List inputList) 
        throws Exception {
        
        ArrayList resources = new ArrayList();
	    	resources.add(RESOURCE_APPS_STATUS);
	    resources.add(RESOURCE_MANAGED_APPLICATION_MANAGE);
        String url = formatUrl(resources);
        String argsJson = JsonUtil.render(inputList);   
        RequestResult result = null;
        String json = clientPost(url, argsJson);
        if ( json != null ) {
            Map map = (Map)JsonUtil.parse(json); 
            result = new RequestResult();
            result.fromMap(map);
        }        
        return result;
    }
	
    //////////////////////////////////////////////////////////////////////
    //
    // Show Identity including already assigned roles.
    // Here identity is passed from the ui as selected.
    // It may be any identity submitted, not necessarily the user 
    // who logged in
    // 
    //////////////////////////////////////////////////////////////////////
    
    /**
     * Return a JSON string that has a map with the roles and viewable
     * attributes for the requested identity.
     *
     * @param identity  The name of the identity to return.
     */
    public String showIdentity(String identity)
    throws Exception {
    	
    	String result = null;  
    	if (identity == null) {
    	  return result;	
    	}    		
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IDENTITIES);
    	resources.add(identity);
    	String url = formatUrl(resources);
     	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
        
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getSummary(String identity)
    throws Exception {
    	
    	String result = null;  
    	if (identity == null) {
    	  return result;	
    	}    		
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IDENTITIES);
    	resources.add(identity);
    	resources.add(SUB_RESOURCE_IDENTITIES_SUMMARY);
    	String url = formatUrl(resources);
     	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }


      /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    
    public String getLink(String identity, Map queryParameters)
            throws Exception {
                
                String result = null;  
                if (identity == null) {
                  return result;    
                }    
                        
                List resources = new ArrayList();
                resources.add(RESOURCE_IDENTITIES);
                resources.add(identity);
                resources.add(RESOURCE_IDENTITIES_LINKS);
                String url = formatUrl(resources);
                System.out.println("URL: "+url);
                result = clientPost(url,queryParameters); 
                return result;
        }

    
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getWorkItemCountNoParam(String identity)
    throws Exception {
    	
    	String result = null;  
    	if (identity == null) {
    	  return result;	
    	}    		
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IDENTITIES);
    	resources.add(identity);
    resources.add(RESOURCE_IDENTITIES_WORKITEMS_COUNT);
    	String url = formatUrl(resources);
     	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    
    
    
    
    
    
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getWorkItemCount(String identity, Map queryParameters)
    throws Exception {
    	
    	String result = null;  
    	if (identity == null) {
    	  return result;	
    	}    		
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IDENTITIES);
    	resources.add(identity);
    resources.add(RESOURCE_IDENTITIES_WORKITEMS_COUNT);
 	String url = formatUrl(resources,queryParameters);
     	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getWorkItems(String identity, Map queryParameters)
    throws Exception {
    	
    	String result = null;  
    	if (identity == null) {
    	  return result;	
    	}    		
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IDENTITIES);
    	resources.add(identity);
    resources.add(RESOURCE_IDENTITIES_WORKITEMS);
    	String url = formatUrl(resources,queryParameters);
    	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    
    /**
     * Return a JSON string 
     *
     * @param queryParameters
     */
    public String getBundles( Map queryParameters)
    throws Exception {
    	
    	String result = null;  
    	List resources = new ArrayList();
    	resources.add(RESOURCE_BUNDLES);
    	String url = formatUrl(resources,queryParameters);
    	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    /**
     * Return a JSON string 
     *
     * @param queryParameters
     */
    public String getIRObjectS( Map queryParameters)
    throws Exception {
    	
    	String result = null;  
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IR);
    	String url = formatUrl(resources,queryParameters);
    	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    /**
     * Return a JSON string 
     *
     * @param queryParameters
     */
    public String getAEObjectS( Map queryParameters)
    throws Exception {
    	
    	String result = null;  
    	List resources = new ArrayList();
    	resources.add(RESOURCE_AE);
    	String url = formatUrl(resources,queryParameters);
    	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    
   
    
    
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getBundlesCount( Map queryParameters)
    throws Exception {
    	
    	String result = null;  
    	List resources = new ArrayList();
    	resources.add(RESOURCE_BUNDLES);
    resources.add(RESOURCE_BUNDLES_COUNT);
   String url = formatUrl(resources,queryParameters);
     	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getAppConnections( String appName, String timeout, String type)
    throws Exception {
    	
    	String result = null;  
    	List resources = new ArrayList();
    	resources.add(RESOURCE_APPS_STATUS);
    resources.add(appName);
    resources.add(timeout);
    resources.add(type);
    resources.add(RESOURCE_APPS_STATUS_INFO);
   String url = formatUrl(resources);
     	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    
    
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getAllEntitlements(String identity)
    throws Exception {
    	
    	String result = null;  
    	if (identity == null) {
    	  return result;	
    	}    		
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IDENTITIES);
    	resources.add(identity);
    resources.add(RESOURCE_IDENTITIES_ENTITLEMENTS);
    	String url = formatUrl(resources);
    	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getEntitlementsOnApp(String identity, String application)
    throws Exception {
    	
    	String result = null;  
    	if (identity == null) {
    	  return result;	
    	}    
     if (application == null) {
      	  return result;	
      	}    		
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IDENTITIES);
    	resources.add(identity);
    resources.add(application);
    resources.add(RESOURCE_IDENTITIES_ENTITLEMENTS);
    	String url = formatUrl(resources);
    	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    
    
    
    
    
    /**
     * Return a JSON string 
     *
     * @param identity  The name of the identity to return.
     */
    public String getWorkItemsWithNoParam(String identity)
    throws Exception {
    	
    	String result = null;  
    	if (identity == null) {
    	  return result;	
    	}    		
    	List resources = new ArrayList();
    	resources.add(RESOURCE_IDENTITIES);
    	resources.add(identity);
    resources.add(RESOURCE_IDENTITIES_WORKITEMS);
    	String url = formatUrl(resources);
    	System.out.println("URL: "+url);
    	result = clientGet(url); 
        return result;
     }
    


    
   

    //////////////////////////////////////////////////////////////////////
    //
    // HttpClient
    //
    //////////////////////////////////////////////////////////////////////
    
	private static boolean checkHttpsUrl(String url) {
	    return (null != url) && url.toLowerCase().startsWith("https");
	}

    private int getPort(String url) throws MalformedURLException {

        int port = -1;

        if (null != url) {
            URL checkurl = new URL(url);
            port = checkurl.getPort();
        }

        if (port < 0) {
            port = 80; 
        }
        
        if((null != url) && url.toLowerCase().startsWith("https"))
        {
        	port=443;
        }
       
        return port;
    }

    private String clientGet(String url) throws Exception {
       
        int status = _client.get(url);
        return processWSResponse(status, _client.getBody());
    }

    private String clientDelete(String url) throws Exception {
        
        int status = _client.delete(url);
        return processWSResponse(status, _client.getBody());
    }

    private String clientPost(String url, String postData) throws Exception {
      
        int status = _client.post(url, postData);
        return processWSResponse(status, _client.getBody());
    }

    private String clientPost(String url, Map postData) throws Exception {
       
        int status = _client.post(url, postData);
        return processWSResponse(status, _client.getBody());
    }

    private String clientPut(String url, String data) throws Exception {
     
        int status = _client.put(url, data);
        return processWSResponse(status, _client.getBody());
    }

    private String processWSResponse(int status, String responseBody) 
        throws Exception {
    	// Other 200 codes indicate success ... may want to change this if our
    	// web services start returning other 200 codes (eg - 201 created).
        if (status != 200)
            throwException(status, responseBody);
        return responseBody;
    }

	private void throwException(int status, String responseBody) 
	throws Exception {

		// is it necessary to have the status in here?  Won't
		// make sense to the end user anyway...
		throw new Exception(itoa(status) + ": " + responseBody);
	}	

	/**
	 * Convert the given int to a string.
	 */
	static public String itoa(int i) {
		return new Integer(i).toString();
	}


	
}
