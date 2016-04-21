
APPROVAL FRAMEWORK README

Quick Deployment Steps:
1. Copy SP_CST_ApprovalObjectMappings.xml to /config/Custom folder and rename file and object for client
2. Update target property %%APPROVAL_FRAMEWORK_CUSTOM_OBJECT%% with name of object in step 1
3. Update approval object mappings with appropriate configurations and rule names.  Samples are provided.
4. Update calling workflow to call SP Dynamic Approval Sub **.  Input vars are:
	- identityName
	- project
	- approvalSet
	- requestor
	- identityRequestId
	- emailArgList
	- approvedTo
	- rejectedTo
	- approvedTemplate
	- rejectedTemplate
	
** Alternative is to call the SP Provision Processor Sub with same input variables as
this framework automatically calls the approval framework


**NOTE:  As of 3/5/2014, new way of calling rule logic has been provided.  Under /Custom is a sample rule library, which
can be deployed in the same manner as the Custom mappings object.  After deployed, there are target properties for the given
library to denote the new location and name.  

## ENTER THE NAME OF THE APPROVAL FRAMEWORK CUSTOM RULE LIBRARY
%%APPROVAL_FRAMEWORK_CUSTOM_RULE%%=HLTN Approval Framework Custom Rule Library
## ENTER THE FILE PATH OF THE APPROVAL FRAMEWORK CUSTOM RULE LIBRARY
%%APPROVAL_FRAMEWORK_CUSTOM_RULE_PATH%%=Rule/Approvals/HLTN_ApprovalFramework_Custom_RuleLibrary.xml

Update these.  Once in place, inside of the custom mapping object, a method in the library can be called directly by 
prefixing the name of the method with "method:".  For example, "method:cstPreApprovalDefaultSplitterRule".  The prefix "rule:" 
can be used to denote that the it is still an actual rule call.  However, no prefix will default to a rule.  

