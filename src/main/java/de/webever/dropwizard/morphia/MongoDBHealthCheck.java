package de.webever.dropwizard.morphia;

import com.codahale.metrics.health.HealthCheck;

import de.webever.dropwizard.morphia.database.MongoDBClient;


public class MongoDBHealthCheck extends HealthCheck {

    final private MongoDBClient mongoDBClient;

    public MongoDBHealthCheck(MongoDBClient mongoDBClient) {
	this.mongoDBClient = mongoDBClient;
    }

    @Override
    protected Result check() throws Exception {
	if (!mongoDBClient.healthy()) {
	    return Result.unhealthy("MongoDB not alive!");
	}
	return Result.healthy();
    }

}
