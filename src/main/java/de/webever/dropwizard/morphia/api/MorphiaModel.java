package de.webever.dropwizard.morphia.api;

import org.mongodb.morphia.annotations.Entity;

@Entity
public interface MorphiaModel {
	public abstract String getId();

	public abstract void setId(String id);
}
