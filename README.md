# BloodGlucoseMeteron
This is our award winning 'Blood Glucose Meteron' app.    
It was created to play around with the new FHIR standard in context of the FHIR Dev Days 2015 in Amsterdam. It is the winner of the Student Track at the FHIR DevDays 2015.    
There is also a little website that displays the sent data in a graph.    
We publish our code so other students or anyone else who wants to get started with FHIR has an example how to do it in Java.

## Structure
folder js: website that displays the data using SMART for FHIR   
rest: app

## Features
The app is able to pull data from a blood glucose meter via Bluetooth Low Energy. It transforms this data into FHIR resources and posts them to a FHIR server.

## Requirements
Android 4.4 and Bluetooth Low Energy is required.

## Notes
1. Please note that the app is just a prototype. Though the code quality is not at its finest and it may contain bugs.  
   Do not use this app in a real world scenario as it was just designed for testing purposes. 
2. The app is based on **[nrfToolbox](https://github.com/NordicSemiconductor/Android-nRF-Toolbox)** by Nordic Semiconductors.
   Most of the code is not necessary. Just the classes in the package **gls** are primarily used.

## License
The app is licensed under the MIT License.
