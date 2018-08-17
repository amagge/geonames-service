package edu.asu.semeval.baseline.indexer.geotree;

public class Country {
	String iso;
	String iso3;
	String name;
	double area;
	int population;
	int id;
	String contName;
	int contId;
	
	public Country(String iso, String iso3, String name, double area, int population,
					int id, String contName, int contId) {
		this.iso = iso;
		this.iso3 = iso3;
		this.name = name;
		this.area = area;
		this.population = population;
		this.id = id;
		this.contName = contName;
		this.contId = contId;
	}

	public String getIso() {
		return iso;
	}

	public String getIso3() {
		return iso3;
	}

	public String getName() {
		return name;
	}

	public double getArea() {
		return area;
	}

	public int getPopulation() {
		return population;
	}

	public int getId() {
		return id;
	}
	
	public String getContinentName() {
		return contName;
	}
	
	public int getContinentId() {
		return contId;
	}
}
