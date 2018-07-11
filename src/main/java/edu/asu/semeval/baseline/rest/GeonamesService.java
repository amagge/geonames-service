package edu.asu.semeval.baseline.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import edu.asu.semeval.baseline.index.Indexer;

@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})

@SpringBootApplication
public class GeonamesService {

    public static void main(String[] args) {
    	for(String arg : args)
    		System.out.println(arg);
    	if(args.length > 0 && args[0].equalsIgnoreCase("create")){
    		Indexer.createIndex();
    	} else {
    		SpringApplication.run(GeonamesService.class, args);
    	}
    }
}
