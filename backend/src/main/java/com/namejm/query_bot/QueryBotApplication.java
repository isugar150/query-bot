package com.namejm.query_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QueryBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueryBotApplication.class, args);
	}

}
