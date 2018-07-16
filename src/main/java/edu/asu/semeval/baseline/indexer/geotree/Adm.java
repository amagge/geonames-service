package edu.asu.semeval.baseline.indexer.geotree;

public class Adm {
	String code;
	String name;
	String asciiname;
	int id;

	public Adm(String code, String name, String asciiname, int id) {
		this.code = code;
		this.name = name;
		this.asciiname = asciiname;
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getAsciiname() {
		return asciiname;
	}

	public int getId() {
		return id;
	}
	
}
