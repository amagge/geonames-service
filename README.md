# lucene-services
A java service for creating and searching Geonames Lucene indexes.

## Dependencies:
* [JDK 1.8.x](http://www.oracle.com/technetwork/java/javase/overview/index.html)
* [Maven 3.x](https://maven.apache.org/index.html)
* [Lucene 5.5.x](https://lucene.apache.org/core/5_5_0/) for Lucene Index
* Java IDE, [Spring Tool Suite](https://spring.io/tools) is heavily recommended for best Spring integration

## Setup:

1) Import the project into an IDE as "Existing Maven Project"

2) Create an application.properties file in the config folder with your Lucene index location and port number. Refer to [application.properties.template](config/application.properties.template)

3) Run -> mvn clean package

4) The build should run successfully and generate a runnable jar in the target folder. This can be run via terminal, or in Spring Tool Suite click Run As "Spring Boot App"

## Using Services
The services may be accessed via HTTP requests. They return data in JSON format.

### Lucene query for Indexed records
* Type: GET
* Path: /search?query=<Lucene_Query>&count=<150|all>
* Lucene Terms: name,country
* Example Lucene Query: name:"Paris" AND country:"France"
* Details on Lucene Query syntax can be found [here](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html) and [here](https://lucene.apache.org/core/6_6_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description)
* Lucene services uses the StandardAnalyzer for its QueryParser
* Make sure you use the index the fields appropriately

