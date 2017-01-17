package de.webever.dropwizard.morphia;

import org.hibernate.validator.constraints.NotEmpty;

import io.dropwizard.Configuration;

public class MongoDBConfiguration extends Configuration {
    @NotEmpty
    public String host = "localhost";
    @NotEmpty
    public int port = 27017;
    @NotEmpty
    public String dataStore = "contentware";
    
    @NotEmpty
    public String modelPackage;

    public MongoDBConfiguration(String modelPackage) {
	super();
	this.modelPackage = modelPackage;
    }
    
    public MongoDBConfiguration() {
	super();
    }
    
    

}
