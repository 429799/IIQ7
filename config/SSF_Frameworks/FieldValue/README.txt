
FIELD VALUE FRAMEWORK README

Quick Deployment Steps:
1. Copy the file SP_CUSTOM_FieldValue_RuleLibrary.xml to a customer config folder **
2. Rename the custom fv rule library and file **
3. Update the properties %%SP_CUSTOM_FV_RULE_LIBRARY_NAME%% and %%SP_CUSTOM_FV_RULE_LIBRARY_PATH%% to 
	reflect the new object name and path **
4. Build out the methods for each app name/field name combination
5. Add the field value rule to the provisioning policy of the app or it role such as:

<Template name="Account" usage="Create">
	<Field displayName="sAMAccountName" name="sAMAccountName" type="string" application="SAMPLE APP" template="Create">
	 <RuleRef>
	   <Reference class="sailpoint.object.Rule" name="SP Dynamic Field Value Rule"/>
	 </RuleRef>
	</Field>
</Template>

NOTE:  The method is derived by concatenating the application and name attrs of the field.  
	All spaces and dashes need to be replaced by an underscore.  The method name is case sensitive and must
	match the field definition.  
	
	In the case above, the method name should be:
	getFV_SAMPLE_APP_sAMAccountName_Rule
	
	The value of the template attribute maps to the "op" argument in the method.  
	
** By default, the properties already point to the existing custom library location.  Steps 1-3 are optional
	but it is recommended that the file and name be changed to reflect the given customer.  