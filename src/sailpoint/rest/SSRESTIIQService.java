package sailpoint.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sailpoint.services.standard.SSThreadPoolTasksProcessor;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.connector.RPCService;
import sailpoint.integration.RequestResult;
import sailpoint.object.RpcRequest;
import sailpoint.object.RpcResponse;
import sailpoint.tools.GeneralException;
/**
 * REST methods for the "IIQService" resource.
 *
 * @author <a href="mailto:rohit.gupta@sailpoint.com">Rohit Gupta</a>
 */
/**
 * <?xml version='1.0' encoding='UTF-8'?>
 *  <RpcRequest method="DoNothing" service="ADConnector" version="1.0">
 * </RpcRequest>
 * @param args
 */
	@Path("/ss/iqService")
public class SSRESTIIQService extends BaseResource
{
	private static final Log log = LogFactory.getLog(SSRESTIIQService.class);
	private RPCService rpService;
	private  List<Map<String,Object>>  results = new ArrayList<Map<String, Object>>();
	private 	SailPointContext _threadContext=null;
	@GET @Path("{host}/info")
	public List<Map<String,Object>> testIIQService(@PathParam("host") String host,
			@QueryParam("port") int port, @QueryParam("timeout") int timeout)
	{
		log.debug("Enter testIIQService");
		RpcRequest rpcRequest = new RpcRequest("ADConnector","DoNothing",null);
		if(port==0)
		{
			port=5050;
		}
		List<Callable<Map<String, Object>> > callableList= new ArrayList<Callable<Map<String, Object>> >();
		GEIIQServiceCallable GEIIQServiceCallable = new GEIIQServiceCallable(host,port);
		callableList.add(GEIIQServiceCallable);
		SSThreadPoolTasksProcessor processor = new SSThreadPoolTasksProcessor(1,timeout!=0?timeout:10000,callableList,results);
		processor.start();
        log.debug("All concurrent tasks submitted.");
		log.debug("End testIIQService");
		log.debug("Results -> "+ results);
		return results;
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
		 _threadContext = SailPointFactory.createContext();
		log.debug("Creating Sailpoint Context");
		return _threadContext;
	}


	private Map<String, Object> checkOnIIQService(String host, int port)throws GeneralException
	{
	log.debug("Enter checkOnIIQService");
	HashMap metaDataMap = new HashMap();
	RpcResponse rpcResponse=null;
	try
	{
	init();
	RpcRequest rpcRequest = new RpcRequest("ADConnector","DoNothing",null);
	rpService= new RPCService(host,port);
	rpcResponse= rpService.execute(rpcRequest);
	log.debug(" Result "+rpcResponse.toXml());
	metaDataMap.put("sucessMessage", rpcResponse.toXml());
	metaDataMap.put("sucess", "true");
	}
	catch (Exception e) 
	{
		try
		{
	
		e.printStackTrace();
		log.error(" Result "+rpcResponse.toXml());
		metaDataMap.put("sucessMessage", rpcResponse.toXml());
		metaDataMap.put("sucess", "true");
		return metaDataMap;
		}
		catch(Exception ex)
		{
			log.error(" Exception ex "+e.getMessage());
			
		}
		
	}

	finally
	{
				if(rpService!=null)
				{
					rpService.close();
				}
				if(_threadContext!=null)
				{
					_threadContext.close();
				}
	}
	log.debug("End checkOnIIQService");
	return metaDataMap;
}
	
	

/*
 * Inner Class
 * This class is developed to create callable objects for each application. 
 * As a result, these objects can be used by GEThreadPoolTasksProcessor to 
 * create tasks for each callabale object.
 */
   class GEIIQServiceCallable implements Callable <Map<String, Object>> 
   {
       private String host;
       private int port;
       public GEIIQServiceCallable(String host, int port)
       {
       	log.debug(" Start GEIIQServiceCallable Constructor");
       	this.host=host;
       	this.port=port;
       log.debug(" End GEIIQServiceCallable Constructor");
       }
       
   	@Override
   	public  Map<String, Object> call() 
   	{
   		try
   	      {
   			 return checkOnIIQService(host,port);
   	      }
   		  catch (Exception exception)
   	      {
   	    	  log.error(" Processor Thread Exception "+exception.getMessage());
   	      }
		return null;
		 
   		}

    }

}
