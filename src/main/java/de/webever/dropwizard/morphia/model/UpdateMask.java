package de.webever.dropwizard.morphia.model;

import static java.util.Locale.ENGLISH;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.PreSave;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class UpdateMask<T extends MorphiaModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMask.class);

    private HashMap<String, Method> getters = new HashMap<>();

    private List<Method> preSave = new ArrayList<>();
    private List<Method> prePersist = new ArrayList<>();

    public UpdateMask(Class<T> clazz, String... fields) {

	List<Field> allFields = new ArrayList<>();
	List<String> fieldList = Lists.newArrayList(fields);
	readFields(clazz, allFields, fieldList);
	fillGetters(clazz, allFields);
    }

    private static String capitalize(String name) {
	if (name == null || name.length() == 0) {
	    return name;
	}
	return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
    }

    public UpdateMask(Class<T> clazz, List<Method> preSave, List<Method> prePersist, Field... fields) {
	fillGetters(clazz, Lists.newArrayList(fields));
    }

    private void fillGetters(Class<T> clazz, List<Field> fields) {
	for (Field field : fields) {
	    if (field != null) {
		String name = field.getName();
		try {
		    PropertyDescriptor pd = new PropertyDescriptor(name, clazz);
		    Method m = pd.getReadMethod();
		    getters.put(name, m);
		} catch (IntrospectionException e1) {
		    try {
			Method m = null;
			if (Boolean.class.isAssignableFrom(field.getType())) {
			    m = clazz.getMethod("is" + capitalize(name));
			} else {
			    m = clazz.getMethod("get" + capitalize(name));
			}
			getters.put(name, m);
		    } catch (NoSuchMethodException | SecurityException e) {
			LOGGER.warn("No getter for " + name + " found!");
		    }
		}
	    }
	}
    }

    private static <Z extends Model> void readModelFields(Class<Z> clazz, List<Field> allFields) {
	Field[] fields = clazz.getDeclaredFields();
	for (int i = 0; i < fields.length; i++) {
	    allFields.add(fields[i]);
	}
	@SuppressWarnings("unchecked")
	Class<Z> superClass = (Class<Z>) clazz.getSuperclass();
	if (!superClass.equals(Model.class)) {
	    readModelFields(superClass, allFields);
	} else {
	    try {
		allFields.add(Model.class.getDeclaredField("lastUpdate"));
	    } catch (NoSuchFieldException | SecurityException e) {
		LOGGER.error("Field should exist!", e);
	    }
	}
    }

    private static <Z extends Model> void readPreMethods(Class<Z> clazz, List<Method> preSaveMethods,
	    List<Method> prePersistMethods) {
	Method[] methods = clazz.getDeclaredMethods();
	for (int i = 0; i < methods.length; i++) {
	    if (methods[i].isAnnotationPresent(PreSave.class)) {
		preSaveMethods.add(methods[i]);
	    } else if (methods[i].isAnnotationPresent(PrePersist.class)) {
		prePersistMethods.add(methods[i]);
	    }

	}
	@SuppressWarnings("unchecked")
	Class<Z> superClass = (Class<Z>) clazz.getSuperclass();
	if (!superClass.equals(Model.class)) {
	    readPreMethods(superClass, preSaveMethods, prePersistMethods);
	}
    }

    private static <Z extends MorphiaModel> void readFields(Class<Z> clazz, List<Field> allFields, List<String> names) {
	Field[] fields = clazz.getDeclaredFields();
	if (names == null) {
	    names = new ArrayList<>();
	}
	for (int i = 0; i < fields.length; i++) {
	    if (names.isEmpty() || names.contains(fields[i].getName())) {
		allFields.add(fields[i]);
	    }
	}
	@SuppressWarnings("unchecked")
	Class<Z> superClass = (Class<Z>) clazz.getSuperclass();
	if (superClass != null && !superClass.equals(Model.class)) {
	    readFields(superClass, allFields, names);
	}
    }

    public static <Z extends Model> UpdateMask<Z> createMask(Class<Z> clazz) throws IntrospectionException {
	List<Field> allFields = new ArrayList<>();
	readModelFields(clazz, allFields);
	Field[] fieldNames = new Field[allFields.size()];
	for (int i = 0; i < fieldNames.length; i++) {
	    Field f = allFields.get(i);
	    if (!Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())
		    && !Modifier.isTransient(f.getModifiers()) && !f.isAnnotationPresent(Transient.class)) {
		fieldNames[i] = allFields.get(i);
	    }
	}

	List<Method> preSaveMethods = new ArrayList<>();
	List<Method> prePersistMethods = new ArrayList<>();
	readPreMethods(clazz, preSaveMethods, prePersistMethods);

	return new UpdateMask<>(clazz, preSaveMethods, prePersistMethods, fieldNames);
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

    public List<Method> getPreSave() {
	return preSave;
    }

    public void setPreSave(List<Method> preSave) {
	this.preSave = preSave;
    }

    public List<Method> getPrePersist() {
	return prePersist;
    }

    public void setPrePersist(List<Method> prePersist) {
	this.prePersist = prePersist;
    }

}
