package de.webever.dropwizard.morphia.database;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;

import de.webever.dropwizard.morphia.MongoDBConfiguration;
import de.webever.dropwizard.morphia.model.Model;
import de.webever.dropwizard.morphia.model.MorphiaModel;
import de.webever.dropwizard.morphia.model.UpdateMask;
import io.dropwizard.lifecycle.Managed;

/**
 * @author richardnaeve
 *
 */
public class MongoDBClient implements Managed {

    private Logger LOGGER = LoggerFactory.getLogger(MongoDBClient.class);

    final Morphia morphia = new Morphia();

    final MongoDBConfiguration configuration;

    final Datastore datastore;

    final MongoClient client;

    private HashMap<Class<? extends MorphiaModel>, UpdateMask<? extends MorphiaModel>> updateMasks = new HashMap<>();

    private HashMap<Class<? extends MorphiaModel>, List<Consumer<? extends MorphiaModel>>> saveHooks = new HashMap<>();

    public MongoDBClient(MongoDBConfiguration configuration, String modelPackage) throws IntrospectionException {
	this.configuration = configuration;
	morphia.mapPackage(modelPackage, true);
	if (configuration.replicaSetName != null && !configuration.replicaSetName.isEmpty()) {
	    MongoClientOptions options = new MongoClientOptions.Builder()
		    .requiredReplicaSetName(configuration.replicaSetName).build();
	    client = new MongoClient(new ServerAddress(configuration.host, configuration.port), options);
	} else {
	    client = new MongoClient(configuration.host, configuration.port);
	}
	datastore = morphia.createDatastore(client, configuration.dataStore);
	datastore.ensureIndexes();
    }

    private <T extends Model> void readFields(Class<T> clazz, List<Field> allFields) {
	Field[] fields = clazz.getDeclaredFields();
	for (int i = 0; i < fields.length; i++) {
	    allFields.add(fields[i]);
	}
	@SuppressWarnings("unchecked")
	Class<T> superClass = (Class<T>) clazz.getSuperclass();
	if (!superClass.equals(Model.class)) {
	    readFields(superClass, allFields);
	} else {
	    try {
		allFields.add(Model.class.getDeclaredField("lastUpdate"));
	    } catch (NoSuchFieldException | SecurityException e) {
		LOGGER.error("Field should exist!", e);
	    }
	}
    }

    private <T extends Model> UpdateMask<T> initMask(Class<T> clazz) throws IntrospectionException {
	List<Field> allFields = new ArrayList<>();
	readFields(clazz, allFields);
	Field[] fieldNames = new Field[allFields.size()];
	for (int i = 0; i < fieldNames.length; i++) {
	    Field f = allFields.get(i);
	    if (!Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())
		    && !Modifier.isTransient(f.getModifiers()) && !f.isAnnotationPresent(Transient.class)) {
		fieldNames[i] = allFields.get(i);
	    }
	}
	return new UpdateMask<>(clazz, fieldNames);
    }

    private <T extends Model> UpdateMask<T> getOrCreateUpdateMask(Class<T> clazz) {
	@SuppressWarnings("unchecked")
	UpdateMask<T> mask = (UpdateMask<T>) updateMasks.get(clazz);
	if (mask == null) {
	    try {
		mask = (UpdateMask<T>) initMask(clazz);
	    } catch (IntrospectionException e) {
		LOGGER.error("Error in initMask", e);
		return null;
	    }
	    updateMasks.put(clazz, mask);
	}
	return mask;
    }

    public <T extends Model> void removeFieldsFromUpdateMask(Class<T> clazz, String... fields) {
	UpdateMask<? extends MorphiaModel> mask = getOrCreateUpdateMask(clazz);
	for (String fieldName : fields) {
	    mask.removeField(fieldName);
	}
    }

    @Override
    public void start() throws Exception {
	LOGGER = LoggerFactory.getLogger(MongoDBClient.class);
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

    public <T extends MorphiaModel> void addSaveHook(Class<T> clazz, Consumer<T> consumer) {
	List<Consumer<? extends MorphiaModel>> consumers = saveHooks.get(clazz);
	if (consumers == null) {
	    consumers = new ArrayList<>();
	}
	consumers.add(consumer);
	saveHooks.put(clazz, consumers);
    }

    @SuppressWarnings("unchecked")
    public <T extends MorphiaModel> T save(T model) {
	LOGGER.debug("saving model" + model.getClass().getSimpleName());
	if (model.getId() == null) {
	    model.setId(UUID.randomUUID().toString());
	}
	LOGGER.debug("write to db");
	Key<T> key = datastore.save(model);
	LOGGER.debug("loading saved model");
	T savedModel = findById(key.getId().toString(), (Class<T>) model.getClass());
	LOGGER.debug("running hooks");
	List<Consumer<? extends MorphiaModel>> consumers = saveHooks.get(model.getClass());
	if (consumers != null) {
	    for (Consumer<? extends MorphiaModel> c : consumers) {
		Consumer<T> consumer = (Consumer<T>) c;
		consumer.accept(savedModel);
	    }
	}
	LOGGER.debug("done");
	return savedModel;
    }

    @SuppressWarnings("unchecked")
    public <T extends Model> T update(T model) {
	Class<? extends Model> clazz = model.getClass();
	UpdateMask<T> mask = (UpdateMask<T>) getOrCreateUpdateMask(clazz);
	model.setLastUpdate(new Date());
	return update(model, mask);
    }

    @SuppressWarnings("unchecked")
    public <T extends MorphiaModel> T update(T model, UpdateMask<T> mask) {
	UpdateOperations<T> query = datastore.createUpdateOperations((Class<T>) model.getClass());
	HashMap<String, Method> map = mask.getGetters();
	for (String fieldName : map.keySet()) {
	    try {
		Object value = map.get(fieldName).invoke(model);
		if (value != null) {
		    query.set(fieldName, value);
		} else {
		    query.unset(fieldName);
		}
	    } catch (Exception e) {
		LOGGER.error("Getter invokation failed!", e);
	    }
	}
	datastore.update(model, query);
	return findById(model.getId().toString(), (Class<T>) model.getClass());
    }

    public <T extends MorphiaModel> T findById(String id, Class<T> clazz) {
	return findByField("id", id, clazz);
    }

    public <T extends MorphiaModel> T findByField(String field, Object value, Class<T> clazz) {
	return datastore.createQuery(clazz).field(field).equal(value).get();
    }

    public <T extends MorphiaModel> List<T> findAllByField(String field, Object value, Class<T> clazz) {
	return datastore.createQuery(clazz).field(field).equal(value).asList();
    }

    public <T extends MorphiaModel> List<T> findAllByPattern(String field, Pattern pattern, Class<T> clazz) {
	return datastore.createQuery(clazz).filter(field, pattern).asList();
    }

    public <T extends MorphiaModel> List<T> findAllByPattern(String field, String regex, Class<T> clazz) {
	Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	return findAllByPattern(field, pattern, clazz);
    }

    public boolean healthy() {
	try {
	    client.getDatabase(configuration.dataStore).listCollections();
	    return true;
	} catch (Exception e) {
	    return false;
	}
    }

    public <T extends MorphiaModel> List<T> findAll(Class<T> clazz) {
	return datastore.createQuery(clazz).asList();
    }

    public <T extends MorphiaModel> Query<T> createQuery(Class<T> clazz) {
	return datastore.createQuery(clazz);
    }

    public <T extends MorphiaModel> void delete(T model) {
	datastore.delete(model);
    }

    public <T extends MorphiaModel> void delete(Query<T> query) {
	datastore.delete(query);
    }

    public <T extends MorphiaModel> void updateField(String fieldName, Object value, T model) {
	@SuppressWarnings("unchecked")
	UpdateOperations<T> operation = (UpdateOperations<T>) datastore.createUpdateOperations(model.getClass());
	operation.set(fieldName, value);
	datastore.update(model, operation);
    }

    public <T extends MorphiaModel> void addToField(String fieldName, Object value, T model) {
	@SuppressWarnings("unchecked")
	UpdateOperations<T> operation = (UpdateOperations<T>) datastore.createUpdateOperations(model.getClass());
	operation.addToSet(fieldName, value);
	datastore.update(model, operation);
    }

    public <T extends MorphiaModel> UpdateOperations<T> createUpdateOperation(Class<T> clazz) {
	return (UpdateOperations<T>) datastore.createUpdateOperations(clazz);
    }

    public <T extends MorphiaModel> UpdateResults runUpdateOperation(Query<T> query, UpdateOperations<T> operations) {
	return datastore.update(query, operations);
    }

    /**
     * Updates a query for a field that references another {@link MorphiaModel}
     * and is in the supplied list of models.
     * 
     * @param query
     *            the query to update
     * @param field
     *            the field to compare
     * @param clazz
     *            the class of the field
     * @param models
     *            the models to compare to
     * @return the updated query
     */
    public <T extends MorphiaModel> Query<? extends MorphiaModel> refModelIn(Query<? extends MorphiaModel> query,
	    String field, Class<T> clazz, List<T> models) {
	List<String> ids = new ArrayList<>();
	for (T t : models) {
	    ids.add(t.getId());
	}
	return refIn(query, field, clazz, ids);
    }

    /**
     * Updates a query for a field that references another {@link MorphiaModel}
     * and is in the supplied list of ids.
     * 
     * @param query
     *            the query to update
     * @param field
     *            the field to compare
     * @param clazz
     *            the class of the field
     * @param ids
     *            the ids
     * @return the updated query
     */
    public <T extends MorphiaModel> Query<? extends MorphiaModel> refIn(Query<? extends MorphiaModel> query,
	    String field, Class<T> clazz, List<String> ids) {
	List<Key<T>> list = new ArrayList<>();
	for (String id : ids) {
	    list.add(new Key<>(clazz, clazz.getSimpleName(), id));
	}
	return query.field(field).in(list);
    }

    /**
     * Updates a query for a field that references another {@link MorphiaModel}
     * and is equal to the supplied id.
     * 
     * @param query
     *            the query to update
     * @param field
     *            the field to compare
     * @param clazz
     *            the class of the field
     * @param ids
     *            the ids
     * @return the updated query
     */
    public <T extends MorphiaModel> Query<? extends MorphiaModel> refEqual(Query<? extends MorphiaModel> query,
	    String field, Class<T> clazz, String id) {
	return query.field(field).equal(new Key<>(clazz, clazz.getSimpleName(), id));
    }

    /**
     * @return the datastore
     */
    public Datastore getDatastore() {
	return datastore;
    }

}
