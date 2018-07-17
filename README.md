# lucene-services
A java REST service for creating and searching GeoNames Apache Lucene indexes.

## Dependencies:
* [JDK 1.8.x](http://www.oracle.com/technetwork/java/javase/overview/index.html)
* [Maven 3.x](https://maven.apache.org/index.html)

## Setup:

1) Install Java 1.8 and Maven if not already installed

2) Navigate to the GeoNames download site: http://download.geonames.org/export/dump/ and download the following four files into the ```resources``` directory:
* ```admin1CodesASCII.txt```
* ```admin2Codes.txt```
* ```allCountries.txt```
* ```countryInfo.txt``` 
3) Create a copy of the [application.properties.template](config/application.properties.template) file in the ```config``` folder. Rename it to ```application.properties```. Configure the paths according to the instructions shown.

4) To create the binaries, run the command:
```
mvn clean package
```
This will download the required packages using Maven and build the system. The build should run successfully and generate a runnable jar in the ```target``` folder which can be run via terminal as shown below.

5) Create the lucene index using the command.
```
java -jar target/geonames-service-0.1.0.jar create
```

6) Run the services for querying data
```
java -jar target/geonames-service-0.1.0.jar
```
If it runs successfully, you should see a list of messages ending with 
```
Tomcat started on port(s): 8091 (http)
Started GeonamesService in 4.045 seconds (JVM running for 4.52)
```

# Using Services
The services may be accessed via HTTP requests. They return data in JSON format. There are two main search APIs that are available.

## Using Generic Search
* Type: GET
* Path: ```/location?location=<LOCATION_NAME>```

Usage Examples:
```
http://localhost:8091/location?location=AZ
```
This will search for "AZ" in the field "Name" of the Lucene index.
```
{
    "records": [
        {
            "AncestorsNames": "Azerbaijan, Asia",
            "Continent": "Asia",
            "GeonameId": "587116",
            "Population": "8303512",
            "Country": "Azerbaijan",
            "Class": "A",
            "Latitude": "40.5",
            "Code": "PCLI",
            "Longitude": "47.5",
            "Name": "Republic of Azerbaijan (Azerbaijan, AZ, AZE)"
        }
    ],
    "retrieved": 1,
    "available": 1556
}
```
You may have meant AZ as in Arizona, so you could search for that specifically by providing the country information (USA) as shown below:
```
http://localhost:8091/location?location=AZ,USA

{
    "records": [
        {
            "AncestorsNames": "Arizona(AZ), United States (US, USA), North America",
            "Continent": "North America",
            "GeonameId": "5551752",
            "State": "Arizona(AZ)",
            "Population": "5863809",
            "Country": "United States (US, USA)",
            "Class": "A",
            "Latitude": "34.5003",
            "Code": "ADM1",
            "Longitude": "-111.50098",
            "Name": "Arizona (AZ)"
        }
    ],
    "retrieved": 1,
    "available": 2
}
```
## Using Standard Analyzer Syntax
* Type: GET
* Path: ```/search?query=<LUCENE_QUERY>&count=<150|all>```

Usage Examples:
```
http://localhost:8091/search?query=GeonameId:390903

{
    "records": [
        {
            "AncestorsNames": "Greece, Europe",
            "Continent": "Europe",
            "GeonameId": "390903",
            "Population": "11000000",
            "Country": "Greece",
            "Class": "A",
            "Latitude": "39.0",
            "Code": "PCLI",
            "Longitude": "22.0",
            "Name": "Hellenic Republic (Greece, GR, GRC)"
        }
    ],
    "retrieved": 1,
    "available": 1
}
```
You can limit number of results using the ```count``` URL parameter as shown below:
```
http://localhost:8091/search?query=Name:Springfield AND State:PA&count=2

{
    "records": [
        {
            "AncestorsNames": "Delaware County, Pennsylvania(PA), United States (US, USA), North America",
            "Continent": "North America",
            "GeonameId": "4561407",
            "State": "Pennsylvania(PA)",
            "Population": "23363",
            "Country": "United States (US, USA)",
            "Class": "P",
            "Latitude": "39.93067",
            "Code": "PPL",
            "Longitude": "-75.32019",
            "County": "Delaware County",
            "Name": "Springfield"
        },
        {
            "AncestorsNames": "Bradford County, Pennsylvania(PA), United States (US, USA), North America",
            "Continent": "North America",
            "GeonameId": "5213459",
            "State": "Pennsylvania(PA)",
            "Population": "0",
            "Country": "United States (US, USA)",
            "Class": "P",
            "Latitude": "41.84924",
            "Code": "PPL",
            "Longitude": "-76.74579",
            "County": "Bradford County",
            "Name": "Springfield"
        }
    ],
    "retrieved": 2,
    "available": -1
}
```

## Resources
* Details on Lucene Query syntax can be found [here](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html) and [here](https://lucene.apache.org/core/6_6_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description)
* Details on GeoNames fields and format can be found [here](http://download.geonames.org/export/dump/)

