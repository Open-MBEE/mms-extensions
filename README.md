# mms-extensions

extra functions for dealing with documents with mms (donbot) (experimental)

## Prerequisites
- JDK 1.8

## Configuration
- An example application.properties file exists in src/main/resources
- Fill out all properties that are applicable

## Running the application locally
- In the project root, run `./gradlew run`
- debug : `./gradlew run --debug-jvm`, connect debugger to proceed

## Make zip for lambda proxy or alb
- `./gradlew buildZip`

## Endpoints
- see DocInfoController for getting info on views and instances for a doc
  
  ex.
    
      GET localhost:8080/mms-doc-info/mms.openmbee.org/{projectId}/{refId}/{docId}?alf_ticket={ticket}
      
  can also give basic auth instead of ticket
      
- see DocSyncController for copying a doc across server/branches - tweak as needed (currently assumes view hierarchy is already established and have same ids on target branch)

  ex.
  
        curl -X POST "localhost:8080/mms-doc-sync" -H 'Content-Type: application/json' -d'
        {
          "docId": "",
        
          "fromMmsServer": "https://mms.openmbee.org",
          "fromTicket": "",
          "fromProjectId": "",
          "fromRefId": "",
        
          "toMmsServer": "https://mms.openmbee.org",
          "toTicket": ""
          "toProjectId": "",
          "toRefId": "",
        
          "extraKey": "",
          "extraValue": "",
          "comment": ""
        }
        '
        
  if basic auth is given, fromTicket and toTicket can be omitted
  
  extraKey and extraValue are used to ensure deleted elements on target branch can be resurrected, key can be anything but should have _ prefix (like _dummy)
       
    