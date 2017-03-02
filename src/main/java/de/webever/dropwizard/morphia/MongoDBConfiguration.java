package de.webever.dropwizard.morphia;

import org.hibernate.validator.constraints.NotEmpty;

import io.dropwizard.Configuration;

/**
 * @author richin
 *
 */
public class MongoDBConfiguration extends Configuration {
    @NotEmpty
    public String host = "localhost";
    @NotEmpty
    public int port = 27017;
    @NotEmpty
    public String dataStore = "dropwizard";
    @NotEmpty
    public String cryptSeed = "12345";
    
    public MongoDBConfiguration() {
	super();
    }

}
