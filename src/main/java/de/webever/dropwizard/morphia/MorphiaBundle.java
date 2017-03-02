package de.webever.dropwizard.morphia;

import de.webever.dropwizard.morphia.crypt.Cryptor;
import de.webever.dropwizard.morphia.database.MongoDBClient;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public abstract class MorphiaBundle<T extends Configuration> implements ConfiguredBundle<T> {

	protected MongoDBClient mongoDBClient;

	@Override
	public void run(T configuration, Environment environment) throws Exception {
		final MongoDBConfiguration dbConfiguration = getMongoDBConfiguration(configuration);
		mongoDBClient = new MongoDBClient(dbConfiguration, getModelPackage());
		Cryptor.initCrypt(dbConfiguration.cryptSeed, getModelPackage());
		environment.lifecycle().manage(mongoDBClient);
		environment.healthChecks().register("MongoDB", new MongoDBHealthCheck(mongoDBClient));
	}

	protected abstract MongoDBConfiguration getMongoDBConfiguration(T configuration);

	protected abstract String getModelPackage();

	@Override
	public void initialize(Bootstrap<?> bootstrap) {
		// TODO Auto-generated method stub

	}

	public MongoDBClient getMongoDBClient() {
		return mongoDBClient;
	}

}
