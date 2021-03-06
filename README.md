# BloodGlucoseMeteron
This is our award winning 'Blood Glucose Meteron' app by students of medical informatics from Heilbronn university (Germany).
It was created to play around with the new FHIR standard in context of the FHIR Dev Days 2015 in Amsterdam. It is the winner of the Student Track at the FHIR DevDays 2015.      
We publish our code so other students or anyone else who wants to get started with FHIR has an example how to do it in Java.

## Features
The app is able to pull data from a blood glucose meter via Bluetooth Low Energy. It transforms this data into FHIR resources and posts them to a FHIR server.   
The user is also able to provide his name and birth date.

## Structure
folder js: website that displays the data using SMART for FHIR   
rest: app

## Requirements
Android 4.4 and Bluetooth Low Energy is required.

## Notes
The app is based on **[nrfToolbox](https://github.com/NordicSemiconductor/Android-nRF-Toolbox)** by Nordic Semiconductors.
Most of the code is not necessary. Just the classes in the package **gls** are primarily used.

## Credits
* The app is based on **[nrfToolbox](https://github.com/NordicSemiconductor/Android-nRF-Toolbox)** by Nordic Semiconductors
* [HAPI FHIR - Java API for HL7 FHIR Clients and Servers](https://github.com/jamesagnew/hapi-fhir/)
 
## Disclaimer
Please note that the app is just a prototype. Though the code quality is not at its finest and it may contain bugs.  
 *Do not use this app in a real world scenario as it was just designed for testing purposes.*
