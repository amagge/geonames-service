package edu.asu.semeval.baseline.index.hierarchy; 

public class GeoNameNode extends Node {

	protected GeoNameLocation location;
	
	public GeoNameNode(boolean isRoot, GeoNameLocation g) {
		super(isRoot, g.getName(), g.getId());
		setLocation(g);
	}

	public GeoNameLocation getLocation() {
		return location;
	}

	public void setLocation(GeoNameLocation location) {
		this.location = location;
	}
}