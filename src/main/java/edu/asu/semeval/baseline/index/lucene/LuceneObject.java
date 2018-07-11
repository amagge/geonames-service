package edu.asu.semeval.baseline.index.lucene;

import edu.asu.semeval.baseline.index.hierarchy.GeoNameLocation;

public class LuceneObject {
	
	private int id;
	private String name;
	private String parentsID;
	private String parentsName;
	private GeoNameLocation location;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getParentsID() {
		return parentsID;
	}
	public void setParentsID(String parentsID) {
		this.parentsID = parentsID;
	}
	public String getParentsName() {
		return parentsName;
	}
	public void setParentsName(String parentsName) {
		this.parentsName = parentsName;
	}
	public GeoNameLocation getLocation() {
		return location;
	}
	public void setLocation(GeoNameLocation location) {
		this.location = location;
	}
	
}
