package de.webever.dropwizard.morphia.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.mongodb.MongoClient;

import de.webever.dropwizard.morphia.MongoDBConfiguration;
import de.webever.dropwizard.morphia.api.Model;
import io.dropwizard.lifecycle.Managed;

public class MongoDBClient implements Managed {

    final Morphia morphia = new Morphia();

    final MongoDBConfiguration configuration;

    final Datastore datastore;

    final MongoClient client;

    private HashMap<Class<? extends Model>, List<Consumer<? extends Model>>> saveHooks = new HashMap<>();

    public MongoDBClient(MongoDBConfiguration configuration, String modelPackage) {
	this.configuration = configuration;
	morphia.mapPackage(modelPackage, true);
	client = new MongoClient(configuration.host, configuration.port);
	datastore = morphia.createDatastore(client, configuration.dataStore);
	datastore.ensureIndexes();
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
	client.close();
    }

    /**
     * Returns the raw mongodb client. Use with caution.
     * 
     * @return the raw client
     */
    public MongoClient getClient() {
	return client;
    }

    public <T extends Model> void addSaveHook(Class<T> clazz, Consumer<T> consumer) {
	List<Consumer<? extends Model>> consumers = saveHooks.get(clazz);
	if (consumers == null) {
	    consumers = new ArrayList<>();
	}
	consumers.add(consumer);
	saveHooks.put(clazz, consumers);
    }

    @SuppressWarnings("unchecked")
    public <T extends Model> T save(T model) {
	if (model.id == null) {
	    model.id = UUID.randomUUID().toString();
	}
	Key<T> key = datastore.save(model);
	T savedModel = findById(key.getId().toString(), (Class<T>) model.getClass());
	List<Consumer<? extends Model>> consumers = saveHooks.get(model.getClass());
	if (consumers != null) {
	    for (Consumer<? extends Model> c : consumers) {
		Consumer<T> consumer = (Consumer<T>) c;
		consumer.accept(savedModel);
	    }
	}
	return savedModel;
    }

    public <T extends Model> T findById(String id, Class<T> clazz) {
	return findByField("id", id, clazz);
    }

    public <T extends Model> T findByField(String field, Object value, Class<T> clazz) {
	return datastore.createQuery(clazz).field(field).equal(value).get();
    }

    public boolean healthy() {
	try {
	    client.getDatabase(configuration.dataStore).listCollections();
	    return true;
	} catch (Exception e) {
	    return false;
	}
    }

    public <T extends Model> List<T> findAll(Class<T> clazz) {
	return datastore.createQuery(clazz).asList();
    }

    public <T extends Model> Query<T> createQuery(Class<T> clazz) {
	return datastore.createQuery(clazz);
    }

    public <T extends Model> void delete(T model) {
	datastore.delete(model);
    }
    
    public <T extends Model> void delete(Query<T> query) {
	datastore.delete(query);
    }

    public <T extends Model> void updateField(String fieldName, Object value, T model) {
	@SuppressWarnings("unchecked")
	UpdateOperations<T> operation = (UpdateOperations<T>) datastore.createUpdateOperations(model.getClass());
	operation.set(fieldName, value);
	datastore.update(model, operation);
    }

    public <T extends Model> Query<? extends Model> refModelIn(Query<? extends Model> query, String field, Class<T> clazz, List<T> models) {
	List<String> ids = new ArrayList<>();
	for (T t : models) {
	    ids.add(t.id);
	}
	return refIn(query, field, clazz, ids);
    }

    public <T extends Model> Query<? extends Model> refIn(Query<? extends Model> query, String field, Class<T> clazz, List<String> ids) {
	List<Key<T>> list = new ArrayList<>();
	for (String id : ids) {
	    list.add(new Key<>(clazz, clazz.getSimpleName(), id));
	}
	return query.field(field).in(list);
    }

    public <T extends Model> Query<? extends Model> refEqual(Query<? extends Model> query, String field, Class<T> clazz, String id) {
	return query.field(field).equal(new Key<>(clazz, clazz.getSimpleName(), id));
    }

}
