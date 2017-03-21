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
    @NotEmpty
    public String host = "localhost";
    @NotEmpty
    public int port = 27017;
    @NotEmpty
    public String dataStore = "dropwizard";
    @NotEmpty
    @Size(min = 16, max = 16)
    public String cryptSeed = "1234567890123456";
    @NotNull
    public boolean enableCrypt = false;

    public MongoDBConfiguration() {
	super();
    }

}
