package de.webever.dropwizard.morphia.model;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;

public class UpdateMask<T extends MorphiaModel> {

	private HashMap<String, Method> getters = new HashMap<>();
	
	public UpdateMask(Class<T> clazz, String...fields) throws IntrospectionException {
		super();
		for (String string : fields) {
			PropertyDescriptor pd = new PropertyDescriptor(string, clazz);
			Method m = pd.getReadMethod();
			getters.put(string, m);
		}
	}
	
	/**
	 * @return the getters
	 */
	public HashMap<String, Method> getGetters() {
		return getters;
	};
	
	public void removeField(String fieldName){
		getters.remove(fieldName);
	}

	
}
