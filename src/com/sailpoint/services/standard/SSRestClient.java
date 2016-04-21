package com.sailpoint.services.standard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.xml.xsom.util.DomAnnotationParserFactory;

import sailpoint.integration.IIQClient;
import sailpoint.integration.ProvisioningPlan.AccountRequest;
import sailpoint.integration.ProvisioningPlan.AttributeRequest;
import sailpoint.integration.JsonUtil;
import sailpoint.integration.RequestResult;
import sailpoint.integration.ProvisioningPlan;
/**
 * 
 * @author Rohit Gupta
 * 
 * SSRESTCLIENT->OOTB IIQClient->Java Methods->REST Server
 * SSRESTCLIENT->SSRESTSERVICECLIENT->Java Methods->REST SERVER
 *
 *REST Service can be tested via curl
 *for example curl --user spadmin:admin --url -X GET  http://localhost:8080/IdentityIQ63/rest/identities/Adam.Kennedy
 *
 *User Name, Password, and BaseURL can be defined in iiqClient.properties.
 *BaseURL: http://localhost:8080/IdentityIQ63
 *This properties file is bundled with iiqIntegration.jar
 *
 *Custom REST Client URLS
 *http://localhost:8080/IdentityIQ63/rest/ss/identities/Adam.Kennedy
 *http://localhost:8080/IdentityIQ63/rest/ss/identities/Adam.Kennedy/summary
 *http://localhost:8080/IdentityIQ63/rest/ss/identities/Adam.Kennedy/workItemsCount
 *http://localhost:8080/IdentityIQ63/rest/ss/identities/Rohit.Gupta/workItemsCount?type=Approval
 *http://localhost:8080/IdentityIQ63/rest/ss/identities/Adam.Kennedy/workItems
 *http://localhost:8080/IdentityIQ63/rest/ss/identities/Adam.Kennedy/workItems?type=Approval
 *
 */
/**
 * Configure the IIQClient reading the properties from CONFIG_FILE.
 * If there is no properties file, just initialize using the given
 * parameters.
 * 
 * @param  baseURL=http://localhost:8080/IdentityIQ63   The base URL to use to override the properties file.
 * @param  user=Chris.Hamlin  The user to use to override the properties file.
 * @param  password=1:p+qvPBo4Rig8PYlNWbr3Zg==  The password to use to override the properties file.
 */

public class SSRestClient {
	public static void main(String[] args)  
	{
		System.out.println("Hello IdentityIQ REST API World.");
		IIQClient iiqClient = null;
		SSRESTServiceClient gERESTServiceClient = null;
		try {
			/**
			 * Connection details will be picked up from iiqclient.properties
			 */
			iiqClient = new IIQClient();
			gERESTServiceClient = new SSRESTServiceClient();
			
		} catch (Exception e) {
			System.err.println("ERROR: Failed to build IIQ client!");
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
		// Try some REST functions that call into the IIQ system.
		try {

			doPingTest(iiqClient);
			doGetManagedEntitlement(gERESTServiceClient);
			doWorkflowLaunchAndCheckStatus(iiqClient);
			doShowIdentity(gERESTServiceClient);
			doSummaryIdentity(gERESTServiceClient);
			doGetWorkItemsCount(gERESTServiceClient);
			doGetWorkItemsCountNoParam(gERESTServiceClient);
			doGetWorkItems(gERESTServiceClient);
			doGetWorkItemsNoParam(gERESTServiceClient);
			doGetEntitlements(gERESTServiceClient);
			doGetAppEntitlements(gERESTServiceClient);
			doGetBundles(gERESTServiceClient);
			doGetBundlesCount(gERESTServiceClient);
			doWorkflowLaunchAndCheckStatusRoleProv(iiqClient);
			doWorkflowLaunchAndCheckStatusSSAMAccounts(iiqClient);
			doGetAppInfo(gERESTServiceClient);
			doGetIRObjects(gERESTServiceClient);
			doManageManagedEntitlement(gERESTServiceClient);
			doGetAppInfo(gERESTServiceClient);
			doManageAppExt(gERESTServiceClient);
			doGetLinks(gERESTServiceClient);
			doGetAEObjects(gERESTServiceClient);
			

		} catch (Exception e) {
			System.err.println("ERROR: Failed to communicate with IIQ system!");
			String exceptionMessage = e.getLocalizedMessage();
			if (exceptionMessage.startsWith("401:")) {
				System.err.println("ERROR: Invalid user name or password given.");
			}
			System.err.println(exceptionMessage);
			e.printStackTrace();
		} 
	}
	public static void doPingTest(IIQClient client) 
			throws Exception {
		String result = client.ping();
		if (null != result) {
			System.out.println(" doPingTest result->" +result); 
		}
	}
	

	public static void doRemoteLogin(IIQClient client ) 
			throws Exception {

		String userName = "Aaron.Nichols";
		String tokenId = client.remoteLogin(userName);
		//System.out.println("Remote Token ID["+tokenId+"] + url http://localhost:8080/identityiq/lcm/manageAccounts.jsf?lt=" + Base64.encodeBytes(tokenId.getBytes()) +"&in="+Base64.encodeBytes(userName.getBytes()));
	}

	public static void doGetIdentityList(IIQClient client) 
			throws Exception {

		String result = client.getIdentityList("Aaron.Nichols");
		System.out.println("result " + JsonUtil.parse(result));        
	}

	public static void doShowIdentity(IIQClient client) 
			throws Exception {

		String   result = client.showIdentity("Aaron.Nichols");
		System.out.println("result " + JsonUtil.parse(result));        
	}
	public static void doGetLinks(IIQClient client) 
			throws Exception {

		RequestResult result = client.getLinks("Aaron.Nichols", true);
		System.out.println("result " + result);         
	}
	
	public static void doGetWorkItemsCount(SSRESTServiceClient client) 
			throws Exception {
		Map queryParameters = new HashMap();
        queryParameters.put("type","ManualAction");
		String result = client.getWorkItemCount("Aaron.Nichols",queryParameters);
		System.out.println("RESULT WORKITEM COUNT " + JsonUtil.parse(result));          
	}
	
	public static void doGetWorkItemsCountNoParam(SSRESTServiceClient client) 
			throws Exception {

		String result = client.getWorkItemCountNoParam("Aaron.Nichols");
		System.out.println("RESULT WORKITEM COUNT " + JsonUtil.parse(result));          
	}
	
	public static void doGetWorkItems(SSRESTServiceClient client) 
			throws Exception {
        Map queryParameters = new HashMap();
        queryParameters.put("type","Approval");
		String result = client.getWorkItems("Aaron.Nichols", queryParameters);
		System.out.println("RESULT WORKITEM " + JsonUtil.parse(result));          
	}
	
	public static void doGetBundlesCount(SSRESTServiceClient client) 
			throws Exception {
		Map queryParameters = new HashMap();
        queryParameters.put("type","Admin Role");
		String result = client.getBundlesCount(queryParameters);
		System.out.println("RESULT BUNDLES COUNT " + JsonUtil.parse(result));          
	}
	
	public static void doGetIRObjects(SSRESTServiceClient client) 
			throws Exception {
		Map queryParameters = new HashMap();
        //queryParameters.put("requestee","Adam.Kennedy");
       //queryParameters.put("requestor","The Administrator");
        queryParameters.put("requestId","0000000255");
        queryParameters.put("requestType","OneUI REST");
        String result = client.getIRObjectS(queryParameters);
		System.out.println("RESULT IR " + JsonUtil.parse(result));           
	}
	
	
	public static void doGetAEObjects(SSRESTServiceClient client) 
			throws Exception {
		Map queryParameters = new HashMap();
        queryParameters.put("source","Adam.Kennedy");
       queryParameters.put("target","The Administrator");
       //PasswordChange or PasswordChangeFailure
        queryParameters.put("action","PasswordChange");
        String result = client.getAEObjectS(queryParameters);
		System.out.println("RESULT AE " + JsonUtil.parse(result));           
	}
	
	
	public static void doGetBundles(SSRESTServiceClient client) 
			throws Exception {
        Map queryParameters = new HashMap();
        queryParameters.put("type","Admin Role");
		String result = client.getBundles(queryParameters);
		System.out.println("RESULT BUNDLES " + JsonUtil.parse(result));          
	}
	
	
	public static void doGetEntitlements(SSRESTServiceClient client) 
			throws Exception {
       
		String result = client.getAllEntitlements("Adam.Kennedy");
		System.out.println("RESULT ENTITLEMENTS " + JsonUtil.parse(result));          
	}
	
	public static void doGetAppEntitlements(SSRESTServiceClient client) 
			throws Exception {
       
		String result = client.getEntitlementsOnApp("Adam.Kennedy", "Sailpoint LDAP LOCAL");
		System.out.println("RESULT ENTITLEMENTS " + JsonUtil.parse(result));          
	}
	
	public static void doGetWorkItemsNoParam(SSRESTServiceClient client) 
			throws Exception {
        
		String result = client.getWorkItemsWithNoParam("Aaron.Nichols");
		System.out.println("RESULT WORKITEM " + JsonUtil.parse(result));          
	}
	
	public static void doSummaryIdentity(SSRESTServiceClient client) 
			throws Exception {

		String   result = client.getSummary("Adam.kennedy");
		System.out.println("result " + JsonUtil.parse(result));        
	}
	
	public static void doShowIdentity(SSRESTServiceClient client) 
			throws Exception {

		String   result = client.showIdentity("Adam.kennedy");
		System.out.println("result " + JsonUtil.parse(result));        
	}

	public static void doGetLinks(SSRESTServiceClient client) 
			throws Exception {
		Map queryParameters = new HashMap();
		queryParameters.put("applicationNameOrId","Active_Directory");
       	String result = client.getLink("Rohit.Gupta",queryParameters);
		System.out.println("RESULT ENTITLEMENTS " + JsonUtil.parse(result));                   
	}
	
	public static void doGetManagedEntitlement(SSRESTServiceClient client) 
			throws Exception 
			{
		Map managedEntArgs = new HashMap();
		
		managedEntArgs.put("attribute", "memberOf");
		managedEntArgs.put("value", "CN=APP_Cisco_Test_100012,OU=Managed,OU=Groups,OU=Enterprise,DC=lab,DC=ds,DC=ge,DC=com");
		managedEntArgs.put("application", "Active Directory Logon User");
		
		
			RequestResult result = client.getManagedEntitlement(managedEntArgs);
		Map<String, Object> map=result.toMap();
		for (Map.Entry<String, Object> entry : map.entrySet())
		{
			if(entry.getKey().equalsIgnoreCase("attributes"))
			{
		    System.out.println(entry.getKey() + "/" + entry.getValue());
			}
		}
		     
	}
	
	public static void doManageManagedEntitlement(SSRESTServiceClient client) throws Exception 
	{
		Map managedEntArgs = new HashMap();
		managedEntArgs.put("attribute", "memberOf");
		managedEntArgs.put("value", "CN=APP_Cisco_Test_100,OU=Managed,OU=Groups,OU=Enterprise,DC=lab,DC=ds,DC=ge,DC=com");
		managedEntArgs.put("application", "Active Directory Logon User");
		managedEntArgs.put("type", "Group");
		List level3 = new ArrayList();
		level3.add("Adam.kennedy");
		level3.add("Aaron.Nichols");
		managedEntArgs.put("entLevelThreeEntitlementOwners", level3);
		List level4 = new ArrayList();
		level4.add("Adam.kennedy");
		level4.add("Aaron.Nichols");
		managedEntArgs.put("entLevelFourEntitlementOwners", level4);
		managedEntArgs.put("description", "Nice Description for AD Group");
		managedEntArgs.put("owner", "spadmin");
		managedEntArgs.put("displayName", "Nice Name for AD Group");

		
		Map managedEntArgs2 = new HashMap();
		managedEntArgs2.put("attribute", "suppliercode");
		managedEntArgs2.put("value", "1234");
		managedEntArgs2.put("application", "LDAP-SCWC");
		managedEntArgs2.put("type", "Group");
		List managedEntArgs2level3 = new ArrayList();
		managedEntArgs2level3.add("Adam.kennedy");
		managedEntArgs2level3.add("Aaron.Nichols");
		managedEntArgs.put("entLevelThreeEntitlementOwners", managedEntArgs2level3);
		List managedEntArgs2level4 = new ArrayList();
		managedEntArgs2level4.add("Adam.kennedy");
		managedEntArgs2level4.add("Aaron.Nichols");
		managedEntArgs2.put("description", "Nice Description for AD Group");
		managedEntArgs2.put("entLevelFourEntitlementOwners", managedEntArgs2level4);
		managedEntArgs2.put("owner", "spadmin");
		managedEntArgs2.put("displayName", "Nice Name for AD Group2");
		
		
		List inputList = new ArrayList();
		inputList.add(managedEntArgs);
		inputList.add(managedEntArgs2);
		
		RequestResult result = client.manageManagedEntitlement(inputList);
		Map<String, Object> map=result.toMap();
		 System.out.println("Result : " +map);
	}
       
	
	public static void doGetAppInfo(SSRESTServiceClient client) 
			throws Exception {
       
		String result = client.getAppConnections("Sailpoint LDAP Local", "20000", "All");
		System.out.println("RESULT App Info " + JsonUtil.parse(result));          
	}
	
	public static void doManageAppExt(SSRESTServiceClient client) throws Exception 
	{
		List level2 = new ArrayList();
		level2.add("Adam.kennedy");
		level2.add("Aaron.Nichols");
		Map managedAppArgs = new HashMap();
		managedAppArgs.put("appLevelTwoApprovers", level2);
		managedAppArgs.put("applicationName", "Active Directory Logon User");
		Map managedAppArgs2 = new HashMap();
		managedAppArgs2.put("appLevelTwoApprovers", level2);
		managedAppArgs2.put("applicationName", "Company Directory");
		List inputList = new ArrayList();
		inputList.add(managedAppArgs);
		inputList.add(managedAppArgs2);
		RequestResult result = client.manageAppExtendedAttr(inputList);
		Map<String, Object> map=result.toMap();
		 System.out.println("Result : " +map);
	}
	
	public  static void doWorkflowLaunchAndCheckStatus(IIQClient client) throws Exception {

		ProvisioningPlan plan = new ProvisioningPlan(); 
		Map customAttributesMapPlan = new HashMap();
		Map customAttributesMapAccount = new HashMap();
		Map customAttributesMapAttributes = new HashMap();
		customAttributesMapPlan.put("customFormFieldOneValue", "FromOneUI");
		customAttributesMapPlan.put("assignment", "true");
		
		plan.setIdentity("102000811");
		AccountRequest req = new AccountRequest();
		req.setApplication("NDBPP");
		req.setOperation(ProvisioningPlan.OP_ACCOUNT_MODIFY);
		req.setArguments(customAttributesMapAccount);
		AttributeRequest attReq = new AttributeRequest();
		attReq.setName("geaenavdbsslvpnrole");
		attReq.setValue("Test");
		attReq.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq2 = new AttributeRequest();
		attReq2.setName("SCWCEntitlements");
		attReq2.setValue("Test2");
		attReq2.setOperation(ProvisioningPlan.OP_ADD);
		attReq2.setArguments(customAttributesMapAttributes);
		AttributeRequest attReq3 = new AttributeRequest();
		attReq3.setName("SCWCEntitlements");
		attReq3.setValue("Test3");
		attReq3.setOperation(ProvisioningPlan.OP_ADD);
		attReq3.setArguments(customAttributesMapAttributes);
		
		req.add(attReq);
		//req.add(attReq2);
		//req.add(attReq3);
		plan.add(req);
		plan.setArguments(customAttributesMapPlan);

		Map launchArgs = new HashMap();
		launchArgs.put("identityName", "102000811");
		launchArgs.put("flow", "AccessRequest");
		launchArgs.put("launcher", "spadmin");
		launchArgs.put("requestor", "spadmin");
		launchArgs.put("source", "LCM");
		launchArgs.put("planMap", plan.toMap());
		launchArgs.put("trace", "true");
		launchArgs.put("foregroundProvisioning", "false");
		launchArgs.put("optimisticProvisioning", "false");
		launchArgs.put("requestType", "OneUI REST");
		launchArgs.put("fallbackApprover", "SS-WorkGroup-FallBackOwners");
		
		RequestResult result = client.launchWorkflow("SSF-FrameWork-Wrapper-Workflow", launchArgs);
		System.out.println("doWorkflowLaunchAndCheckStatus result->" + result); 
		System.out.println("doWorkflowLaunchAndCheckStatus result map->" + result.toMap()); 

		String taskResultId = result.getRequestID();
		Map map=result.getAttributes();
		if(map!=null)
		{
		String identityRequestId =(String)map.get("identityRequestId");
		System.out.println("doWorkflowLaunchAndCheckStatus identityRequestId->" + identityRequestId) ;
		}
		System.out.println("doWorkflowLaunchAndCheckStatus taskResultId->" + taskResultId) ;
		if ( taskResultId != null ) {
			RequestResult taskResult = client.getTaskResultStatus(taskResultId);
			System.out.println("doWorkflowLaunchAndCheckStatus taskResult->" + taskResult) ;
			
		}

	}
	
	
	public  static void doWorkflowLaunchAndCheckStatusSSAMAccounts(IIQClient client) throws Exception {

		ProvisioningPlan plan = new ProvisioningPlan(); 
		Map customAttributesMapPlan = new HashMap();
		customAttributesMapPlan.put("requester", "spadmin");
		customAttributesMapPlan.put("source", "LCM");
		plan.setIdentity("Adam.Kennedy");
		AccountRequest req = new AccountRequest();
		req.setNativeIdentity("cn=Adam.Kennedy,ou=Standard,ou=Users,ou=Enterprise,DC=lab,DC=ds,DC=ge,DC=com");
		req.setApplication("Infra-Application");
		req.setOperation(ProvisioningPlan.OP_ACCOUNT_CREATE);
		AttributeRequest attReq = new AttributeRequest();
		attReq.setName("sAMAccountName");
		attReq.setValue("Adam.Kennedy");
		attReq.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq2 = new AttributeRequest();
		attReq2.setName("userAccountControl");
		attReq2.setValue("512");
		attReq2.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq3 = new AttributeRequest();
		attReq3.setName("description");
		attReq3.setValue("Provisioned through SailPoint REST IIQ");
		attReq3.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq4 = new AttributeRequest();
		attReq4.setName("pwdLastSet");
		attReq4.setValue(true);
		attReq4.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq5 = new AttributeRequest();
		attReq5.setName("password");
		attReq5.setValue("Idm12rocks");
		attReq5.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq6 = new AttributeRequest();
		attReq6.setName("sn");
		attReq6.setValue("Kennedy");
		attReq6.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq7 = new AttributeRequest();
		attReq7.setName("givenName");
		attReq7.setValue("Adam");
		attReq7.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq8 = new AttributeRequest();
		attReq8.setName("Mail");
		attReq8.setValue("Adam.Kennedy@sailpoint.com");
		attReq8.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq9 = new AttributeRequest();
		attReq9.setName("Company");
		attReq9.setValue("sailpoint");
		attReq9.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq10 = new AttributeRequest();
		attReq10.setName("department");
		attReq10.setValue("TEST Aviation");
		attReq10.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq11 = new AttributeRequest();
		attReq11.setName("gehrbusinesssegmentid");
		attReq11.setValue("123");
		attReq11.setOperation(ProvisioningPlan.OP_ADD);
		AttributeRequest attReq12 = new AttributeRequest();
		attReq12.setName("gehrindustrygroupid");
		attReq12.setValue("123");
		attReq12.setOperation(ProvisioningPlan.OP_ADD);
		req.add(attReq);
		req.add(attReq2);
		req.add(attReq3);
		req.add(attReq4);
		req.add(attReq5);
		req.add(attReq6);
		req.add(attReq7);
		req.add(attReq8);
		req.add(attReq9);
		req.add(attReq10);
		req.add(attReq11);
		req.add(attReq12);
		plan.add(req);
		plan.setArguments(customAttributesMapPlan);
		Map launchArgs = new HashMap();
		launchArgs.put("identityName", "Adam.Kennedy");
		launchArgs.put("flow", "AccessRequest");
		launchArgs.put("launcher", "spadmin");
		launchArgs.put("requestor", "spadmin");
		launchArgs.put("source", "LCM");
		launchArgs.put("planMap", plan.toMap());
		launchArgs.put("trace", "true");
		launchArgs.put("foregroundProvisioning", "false");
		launchArgs.put("optimisticProvisioning", "false");
		launchArgs.put("requestType", "TEST REQUEST TYPE");
		launchArgs.put("fallbackApprover", "SS-WorkGroup-FallBackOwners");
		
		RequestResult result = client.launchWorkflow("SSF-FrameWork-Wrapper-Workflow", launchArgs);
		System.out.println("doWorkflowLaunchAndCheckStatus result->" + result); 
		System.out.println("doWorkflowLaunchAndCheckStatus result map->" + result.toMap()); 

		String taskResultId = result.getRequestID();
		Map map=result.getAttributes();
		if(map!=null)
		{
		String identityRequestId =(String)map.get("identityRequestId");
		System.out.println("doWorkflowLaunchAndCheckStatus identityRequestId->" + identityRequestId) ;
		}
		System.out.println("doWorkflowLaunchAndCheckStatus taskResultId->" + taskResultId) ;
		if ( taskResultId != null ) {
			RequestResult taskResult = client.getTaskResultStatus(taskResultId);
			System.out.println("doWorkflowLaunchAndCheckStatus taskResult->" + taskResult) ;
			
		}

	}
	
	public  static void doWorkflowLaunchAndCheckStatusRoleProv(IIQClient client) throws Exception {

		ProvisioningPlan plan = new ProvisioningPlan(); 
		Map accountAttributesMapPlan = new HashMap();
		accountAttributesMapPlan.put("name", "Admin Role One");
		plan.setIdentity("Adam.Kennedy");
		AccountRequest req = new AccountRequest();
		req.setApplication("IIQ");
		req.setOperation(ProvisioningPlan.OP_ACCOUNT_MODIFY);
		req.setArguments(accountAttributesMapPlan);
		AttributeRequest attReq = new AttributeRequest();
		attReq.setName("assignedRoles");
		attReq.setValue("Admin Role One");
		attReq.setOperation(ProvisioningPlan.OP_ADD);
		req.add(attReq);
		plan.add(req);
		Map launchArgs = new HashMap();
		launchArgs.put("identityName", "Adam.Kennedy");
		launchArgs.put("flow", "AccessRequest");
		launchArgs.put("launcher", "spadmin");
		launchArgs.put("requestor", "spadmin");
		launchArgs.put("source", "LCM");
		launchArgs.put("planMap", plan.toMap());
		launchArgs.put("trace", "true");
		launchArgs.put("foregroundProvisioning", "false");
		launchArgs.put("optimisticProvisioning", "false");
		launchArgs.put("requestType", "Admin Role");
		launchArgs.put("fallbackApprover", "SS-WorkGroup-FallBackOwners");
		
		RequestResult result = client.launchWorkflow("SSF-FrameWork-Wrapper-Workflow", launchArgs);
		System.out.println("doWorkflowLaunchAndCheckStatus result->" + result); 
		System.out.println("doWorkflowLaunchAndCheckStatus result map->" + result.toMap()); 

		String taskResultId = result.getRequestID();
		Map map=result.getAttributes();
		if(map!=null)
		{
		String identityRequestId =(String)map.get("identityRequestId");
		System.out.println("doWorkflowLaunchAndCheckStatus identityRequestId->" + identityRequestId) ;
		}
		System.out.println("doWorkflowLaunchAndCheckStatus taskResultId->" + taskResultId) ;
		if ( taskResultId != null ) {
			RequestResult taskResult = client.getTaskResultStatus(taskResultId);
			System.out.println("doWorkflowLaunchAndCheckStatus taskResult->" + taskResult) ;
			
		}

	}
} 