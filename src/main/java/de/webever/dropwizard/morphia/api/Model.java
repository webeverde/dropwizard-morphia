package de.webever.dropwizard.morphia.api;

import java.util.Date;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mongodb.DBObject;

@Entity
public abstract class Model implements MorphiaModel {
	@Id
	private String id;

	private Date lastUpdate;
	private Date creationDate = new Date();

	@JsonCreator
	public Model() {
	}

	public Model(String id, Date lastUpdate, Date creationDate) {
		this.id = id;
		this.lastUpdate = lastUpdate;
		this.creationDate = creationDate;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the lastUpdate
	 */
	public Date getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @param lastUpdate
	 *            the lastUpdate to set
	 */
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * 
	 * @param creationDate
	 *            the creationDate to set
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	// TODO CreationDate should not be overwritten.
	@PrePersist
	DBObject prePersist(final DBObject dbObj) {
		//  CreationDate is only written once on creation. DOESNT WORK!
		lastUpdate = new Date();
		return dbObj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Model other = (Model) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
