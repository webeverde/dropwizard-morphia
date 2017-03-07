package de.webever.dropwizard.morphia.model;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Transient;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DBObject;

import de.webever.dropwizard.morphia.crypt.Crypt;
import de.webever.dropwizard.morphia.crypt.Cryptor;

/**
 * Encrypts every string field annoted with {@link Crypt}.
 * 
 * @author Richard Naeve
 *
 */
public abstract class CryptModel extends Model {

    @Transient
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    static Reflections reflections;
    
    private static boolean enabled = true;

    public static void init(String packageName, boolean enable) {
	reflections = new Reflections(packageName, new FieldAnnotationsScanner());
	enabled = enable;
    }

    @PrePersist
    void encryptFields(final DBObject dbObj) throws IllegalArgumentException, IllegalAccessException, IOException {
	forEveryCrypt((input) -> {
	    try {
		return Cryptor.encrypt(input);
	    } catch (IOException e) {
		LOGGER.error("Encrypt failed!", e);
	    }
	    return null;
	});
    }

    @PostLoad
    void decryptFields() throws IllegalArgumentException, IllegalAccessException, IOException {
	forEveryCrypt((input) -> {
	    try {
		return Cryptor.decrypt(input);
	    } catch (IOException e) {
		LOGGER.error("Decrypt failed!", e);
	    }
	    return null;
	});
    }

    private void forEveryCrypt(Function<String, String> f) throws IllegalArgumentException, IllegalAccessException {
	if(!enabled){
	    return;
	}
	Set<Field> fields = reflections.getFieldsAnnotatedWith(Crypt.class);
	for (Field field : fields) {
	    if (field.getDeclaringClass().equals(getClass())) {
		field.setAccessible(true);
		Object o = field.get(this);
		if (o != null) {
		    if (String.class.isAssignableFrom(field.getType())) {
			String output = f.apply(o.toString());
			field.set(this, output);
		    } else if (field.getType().isArray()) {
			Object[] list = (Object[]) o;
			for (int i = 0; i < list.length; i++) {
			    Object value = list[i];
			    if (String.class.isAssignableFrom(value.getClass())) {
				list[i] = f.apply((String) value);
			    }
			}
			field.set(this, list);
		    } else if (List.class.isAssignableFrom(field.getType())) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) o;
			for (int i = 0; i < list.size(); i++) {
			    Object value = list.get(i);
			    if (String.class.isAssignableFrom(value.getClass())) {
				list.set(i, f.apply((String) value));
			    }
			}
			field.set(this, list);
		    } else if (Map.class.isAssignableFrom(field.getType())) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) o;
			map.forEach((key, value) -> {
			    if (String.class.isAssignableFrom(value.getClass())) {
				map.put(key, f.apply((String) value));
			    }
			});
			field.set(this, map);
		    }
		}
	    }
	}
    }

}
