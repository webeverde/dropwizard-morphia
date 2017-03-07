package de.webever.dropwizard.morphia.model;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateMask<T extends MorphiaModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMask.class);

    private HashMap<String, Method> getters = new HashMap<>();

    public UpdateMask(Class<T> clazz, String... fields) {
	super();
	for (String string : fields) {
	    if (string != null) {

		try {
		    PropertyDescriptor pd = new PropertyDescriptor(string, clazz);
		    Method m = pd.getReadMethod();
		    getters.put(string, m);
		} catch (IntrospectionException e) {
		    LOGGER.warn("No getter for " + string + " found!");
		}
	    }
	}
    }

    /**
     * @return the getters
     */
    public HashMap<String, Method> getGetters() {
	return getters;
    };

    public void removeField(String fieldName) {
	getters.remove(fieldName);
    }

}
