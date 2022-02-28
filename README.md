# veracode-create-list-of-sandboxes
This plugin allows for bulk creation of Sandboxes, it provides 2 actions:
- createForNewApplication:
  - creates an application profile and initializes it with a list of sandboxes </br>
  Parameters:
    - Sandbox Names - comma-delimited list of sandboxes to create
        - --sandbox_names or -sn
    - Application Name* - name of the application profile:
      - --application_name or -an
    - Business Criticality* - business criticality of the application:
      - --business_criticality or -bc
    - Description - application description:
      - --description or -d
    - Business Unit - guid of the application business unit:
      - --business_unit or -b
    - Business Owner - application business owner's name:
      - --business_owner or -bo
    - Business Owner E-mail - application business owner's e-mail:
      -  --business_owner_email or -boe
    - Teams - comma-delimited list of team guids:
      -  --teams or -teams
- createForAllApplications:
  - creates a list of sandboxes to all application profiles (can ignore selected profiles)</br>
   Parameters:
    - Sandbox Names* - comma-delimited list of sandboxes to create:
      - --sandbox_names or -sn
    - Applications not to modify - comma-delimited list of application profiles to ignore:
      - --exceptions or -e
Both actions require API credentials:
  - API ID*: 
    - --veracode_id or -vi
  - API Key*: 
    - --veracode_key or -vk