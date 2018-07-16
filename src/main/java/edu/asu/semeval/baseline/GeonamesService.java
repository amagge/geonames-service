package edu.asu.semeval.baseline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import edu.asu.semeval.baseline.indexer.Indexer;

@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})

@SpringBootApplication
public class GeonamesService {

    public static void main(String[] args) {
    	if(args.length > 0 && args[0].equalsIgnoreCase("create")){
    		Indexer.createIndex();
    	} else {
    		SpringApplication.run(GeonamesService.class, args);
    	}
    }
}
