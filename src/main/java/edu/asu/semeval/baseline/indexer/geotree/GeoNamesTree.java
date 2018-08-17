package edu.asu.semeval.baseline.indexer.geotree;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * File to build the GeonameTree
 * @author amagge
 */
public class GeoNamesTree {
	private final Logger log = Logger.getLogger("GeoNameTree");

	private static Map<String, Country> countryLookup = null;
	private static Map<String, Adm> adm1Lookup = null;
	private static Map<String, Adm> adm2Lookup = null;
	private static Map<String, String> continentLookup = null;

	private final String GeoCountryFile;
	private final String GeoADM1File;
	private final String GeoADM2File;
	private static GeoNamesTree tree = null;

    static
    {
    	continentLookup = new HashMap<String, String>();
    	continentLookup.put("EU", "6255148,Europe");
    	continentLookup.put("AS", "6255147,Asia");
    	continentLookup.put("NA", "6255149,North America");
    	continentLookup.put("SA", "6255150,South America");
    	continentLookup.put("AF", "6255146,Africa");
    	continentLookup.put("OC", "6255151,Oceania");
    	continentLookup.put("AN", "6255152,Antarctica");
    }
	
	private GeoNamesTree(String geoDirectory) {
		log.info("Loading geonames files...");
		GeoCountryFile = geoDirectory + "countryInfo.txt";
		GeoADM1File = geoDirectory + "admin1CodesASCII.txt";
		GeoADM2File = geoDirectory + "admin2Codes.txt";
		log.info("Creating lookups...");
		createCountryLookup();
		adm1Lookup = adminLookup(GeoADM1File);
		adm2Lookup = adminLookup(GeoADM2File);
		log.info("Finished processing Admin and Country lookups");
	}

	protected void createCountryLookup() {
		countryLookup = new HashMap<String, Country>();
		String line;
		Scanner scan;
		try {
			File geoFile = new File(GeoCountryFile);
			scan = new Scanner(geoFile);
			while (scan.hasNext()) {
				line = scan.nextLine();
				if (!line.startsWith("#")) {
					String[] geoname = line.trim().split("\t");
					String iso = geoname[0];
					String iso3 = geoname[1];
					String name = geoname[4];
					double area = Double.parseDouble(geoname[6]);
					int population = Integer.parseInt(geoname[7]);
					String continentName = "";
					int continentId = -1;
					String continentCode = geoname[8];
					if(continentLookup.containsKey(continentCode)){
						String[] continentParts = continentLookup.get(continentCode).split(",");
						continentId = Integer.parseInt(continentParts[0]);
						continentName = continentParts[1];
					}
					int id = Integer.parseInt(geoname[16]);
					Country country = new Country(iso, iso3, name, area, population, id,
													continentName, continentId);
					countryLookup.put(iso,country);
				}
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		log.info("Countries loaded: " + countryLookup.size());
	}
	
	private Map<String, Adm> adminLookup(String filename) {
		Map<String, Adm> lookup = new HashMap<String, Adm>();
		File geoFile = new File(filename);
		String line;
		try{
			Scanner scan = new Scanner(geoFile);
			while (scan.hasNext()) {
				line = scan.nextLine();
				if (!line.startsWith("#")) {
					String[] geoname = line.trim().split("\t");
					String code = geoname[0];
					String name = geoname[1];
					String asciiname = geoname[2];
					int id = Integer.parseInt(geoname[3]);
					Adm adm = new Adm(code, name, asciiname, id);
					lookup.put(code,adm);
				}
			}
			scan.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Admin loaded: " + lookup.size());
		return lookup;
	}
	
	public static GeoNamesTree getInstance(String dir) {
		if (tree == null) {
			tree = new GeoNamesTree(dir);
		}
		return tree;
	}

	public Map<String, Country> getCountryLookup() {
		return countryLookup;
	}

	public Map<String, Adm> getAdm1Lookup() {
		return adm1Lookup;
	}

	public Map<String, Adm> getAdm2Lookup() {
		return adm2Lookup;
	}

}