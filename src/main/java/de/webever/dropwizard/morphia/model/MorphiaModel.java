package de.webever.dropwizard.morphia.model;

import org.mongodb.morphia.annotations.Entity;

@Entity
public interface MorphiaModel {
	
	public String getId();

	public void setId(String id);
	
}
