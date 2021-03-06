package de.webever.dropwizard.morphia;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import io.dropwizard.Configuration;

/**
 * @author richin
 *
 */
public class MongoDBConfiguration extends Configuration {
    
    public String url;
    
    public String host = "localhost";
    public int port = 27017;
    
    @NotEmpty
    public String dataStore = "dropwizard";
    
    public String replicaSetName = null;
    @NotEmpty
    @Size(min = 16, max = 16)
    public String cryptSeed = "$5$9a8shdfoiuq23";
    @NotNull
    public boolean enableCrypt = false;

    public MongoDBConfiguration() {
	super();
    }

}
