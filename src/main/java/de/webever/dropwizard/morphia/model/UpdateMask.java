package de.webever.dropwizard.morphia.model;

import static java.util.Locale.ENGLISH;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
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

    private static String capitalize(String name) {
	if (name == null || name.length() == 0) {
	    return name;
	}
	return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
    }

    public UpdateMask(Class<T> clazz, Field... fields) {
	super();
	for (Field field : fields) {
	    if (field != null) {
		String name = field.getName();
		try {
		    PropertyDescriptor pd = new PropertyDescriptor(name, clazz);
		    Method m = pd.getReadMethod();
		    getters.put(name, m);
		} catch (IntrospectionException e1) {
		    try {
			PropertyDescriptor pd = new PropertyDescriptor(field.getName(), clazz, "get" + capitalize(name),
				"set" + capitalize(name));
			Method m = pd.getReadMethod();
			getters.put(name, m);
		    } catch (IntrospectionException e) {
			LOGGER.warn("No getter for " + name + " found!");
		    }
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
