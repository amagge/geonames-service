package edu.asu.semeval.baseline.index;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Logger;

import edu.asu.semeval.baseline.index.hierarchy.GeoNameLocation;
import edu.asu.semeval.baseline.index.hierarchy.GeoNameNode;
import edu.asu.semeval.baseline.index.hierarchy.GeoNameTree;
import edu.asu.semeval.baseline.index.hierarchy.Node;
import edu.asu.semeval.baseline.index.lucene.LuceneObject;
import edu.asu.semeval.baseline.index.lucene.WriteToLucene;


public class Indexer {

	private static Logger log;
	static String dir = null;//"/Users/bhangal/Desktop/Zoophy/hierarchy/";//"/home/kbhangal/new_hierarchy/hierarchy/";//
	static String rawFilePath = dir + "complete_hierarchy.txt";
	private static  Set<Integer> continents;
	private static  Set<Integer> naCountries;
	private static  Set<Integer> saCountries;
	private static  Set<Integer> afCountries;
	private static  Set<Integer> asCountries;
	private static  Set<Integer> euCountries;
	private static  Set<Integer> ocCountries;
	static Map<Integer, GeoNameNode> mapIDNodes;

	//	public static void main(String[] args) {
	public static void createIndex() {
		continents = new HashSet<Integer>(Arrays.asList(6255146,6255147,6255148,6255149,6255150,6255151,6255152));
		asCountries = new HashSet<Integer>(Arrays.asList(6255147,1149361,174982,587116,1210997,290291,1820814,1252634,1547376,1814991,2078138,614540,1643084,294640,1269750,1282588,99237,130758,248816,1861060,1527747,1831722,1873107,1835841,285570,1522867,1655842,272103,1227603,1327865,2029969,1282028,1733045,1282988,286963,1694008,1168579,6254930,289688,102358,1880251,163843,1605651,1220409,1218197,298795,1668284,290557,1512440,1562822,69543));
		afCountries = new HashSet<Integer>(Arrays.asList(6255146,2589581,3351879,2361809,433561,2395170,933860,239880,2260494,2287781,2233387,3374766,203312,223816,357994,338010,337996,2400553,2300660,2413451,2420477,2309096,2372248,192950,921929,2275384,932692,2215636,2542007,1062947,2453866,2378080,934292,927384,1036973,3355338,2440476,2328926,935317,49518,241170,366755,3370751,2403846,2245662,51537,7909807,2410758,934841,2434508,2363686,2464461,149590,226074,1024031,953987,2461445,895949,878675));
		saCountries = new HashSet<Integer>(Arrays.asList(6255150,3865483,3923057,3469034,3895114,3686110,3658394,3474414,3378535,3437598,3932488,3382998,3439705,3625428));
		naCountries = new HashSet<Integer>(Arrays.asList(6255149,3576396,3573511,8505032,3577279,3374084,3578476,3573345,7626844,3572887,3582678,6251999,3624060,3562981,7626836,3575830,3508796,3580239,3425505,3579143,3595528,3608932,3723988,3489940,3575174,3580718,3576468,3578421,3570311,3578097,3996063,3617476,3703430,3424932,4566966,3585968,7609695,3576916,3573591,6252001,3577815,3577718,4796775));
		euCountries = new HashSet<Integer>(Arrays.asList(6255148,3041565,783754,2782113,661882,3277605,2802361,732800,630336,2658434,146669,3077311,2921044,2623032,453733,2510769,660013,2622320,3017382,2635167,3042362,2411586,390903,3202326,719819,2963597,3042225,2629691,3175395,3042142,3042058,597427,2960313,458258,2993457,617790,3194884,718075,2562770,2750405,3144096,798544,2264397,798549,6290252,0,2661886,3190538,607072,3057568,3168068,690791,3164670,831053));
		ocCountries = new HashSet<Integer>(Arrays.asList(6255151,5880801,2077456,1899402,2205218,2081918,4043988,4030945,2080185,4041468,2139685,2155115,2110425,4036232,2186224,4030656,2088628,4030699,1559582,2103350,4031074,1966436,4032283,2110297,5854968,2134431,4034749,4034894));

		loadProperties();
		GeoNameTree  geoTree = GeoNameTree.getInstance(dir);	// load everything

		Node node = null;
		log = Logger.getLogger("Indexer");
		int count =0, increment=50000;		

		log.info("complete_hierarchy started");
		mapIDNodes = geoTree.getMapIDNodes();
		log.info("mapIDNodes size: "+mapIDNodes.size());

		fixMissingHierarchy(mapIDNodes,geoTree);
		List<LuceneObject> arrayList = new LinkedList<LuceneObject>();

		try {
			for(Integer id : mapIDNodes.keySet()) {
				if(mapIDNodes.get(id).getLocation().getTypeClass()!=null) {
					String classType = mapIDNodes.get(id).getLocation().getTypeClass();
					if(classType.equalsIgnoreCase("A") || classType.equalsIgnoreCase("P") ||
							classType.equalsIgnoreCase("L")) {

						LuceneObject luceneObject = new LuceneObject();
						node = geoTree.getNode(id);
						try {
							setLocationState(node);
							List<Integer> parentsID = generateAncestorIdList(node);
							String parentsName = generateAncestorNameList(parentsID);
							String name = retreiveName(id);


							luceneObject.setId(id);
							luceneObject.setName(name);
							luceneObject.setParentsID(listToString(parentsID));
							luceneObject.setParentsName(parentsName);
							luceneObject.setLocation(createLocationObject(id, geoTree));
							arrayList.add(luceneObject);

							count++;
							if(count==increment) {
								log.info("complete_hierarchy.txt count: "+ count);
								increment+=50000;
							}
						}catch(NullPointerException e) {
							log.info("No hierarchy found for "+node.getID());
						}

					}
				}
			}
		}catch (Exception e) {
			log.info("Error setting up hierarchy file");
		}

		log.info("---------Hierarchy file succesfully generated, records: "+count);


		//writeToFile(arrayList);
		WriteToLucene.toLucene(arrayList);

	}

	//if location is a state, set state name as state field
	private static void setLocationState(Node node) {
		Integer nodeID = node.getID();
		GeoNameLocation location = mapIDNodes.get(nodeID).getLocation();
		if(location!=null) {
			if(location.getType().equalsIgnoreCase("ADM1")) {			//primary administrative division of a country like state/province
				String name = location.getName();
				location.setState(name);
				mapIDNodes.get(nodeID).setLocation(location);
			}
		}
	}

	private static void loadProperties() {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("config/config.properties");
			prop.load(input);
			dir = prop.getProperty("hierarchy.location");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static String retreiveName(int id) {
		if(mapIDNodes.get(id).getLocation().getName()!=null)
			return simplifyLocationName(mapIDNodes.get(id).getLocation().getName());
		else {
			log.info("******complete_hierarchy: population missing "+ id);
			return "";
		}
	}

	private static GeoNameLocation createLocationObject(int id, GeoNameTree  geoTree) {
		GeoNameLocation location = new GeoNameLocation();
		GeoNameLocation tempLocation = new GeoNameLocation();

		try {
			if(mapIDNodes.get(id).getLocation()!=null) {
				tempLocation = mapIDNodes.get(id).getLocation();
				if(tempLocation.getType()!=null) {
					location.setType(tempLocation.getType());
				}else {
					location.setType("NA");
					log.info("******complete_hierarchy: admi1 missing "+ id);
				}

				if(tempLocation.getState()!=null) {
					location.setState(tempLocation.getState());
				}else {
					location.setState("Unkown");
				}

				if(tempLocation.getCountry()!=null) {
					String countryCode = tempLocation.getCountry();
					int countryId = geoTree.getCountryLookup().get(countryCode);
					GeoNameNode countryNode = geoTree.getMapIDNodes().get(countryId);
					if(countryNode!=null) {
						location.setCountry(simplifyLocationName(countryNode.getLocation().getName()));
					}else {
						location.setCountry("NA");
						log.info("******complete_hierarchy: country missing "+ id);
					}
				}else {
					location.setCountry("NA");
					log.info("******complete_hierarchy: country missing "+ id);
				}

				location.setPopulation(tempLocation.getPopulation());
				location.setLatitude(tempLocation.getLatitude());
				location.setLongitude(tempLocation.getLongitude());
				return location;
			}else {
				log.info("******complete_hierarchy: location object missing ERROR"+ id);
				return null;
			}
		}catch(Exception e) {
			log.info("DAMN! location error"+ id);
			return null;
		}
	}



	private static String listToString(List<Integer> parentsID) {
		StringJoiner stringJoiner = new StringJoiner(",");
		for(int id: parentsID) {
			stringJoiner.add(String.valueOf(id));
		}
		return stringJoiner.toString();	
	}


	private static List<Integer> generateAncestorIdList(Node node) throws NullPointerException{
		Set<Integer> ancestors = new LinkedHashSet<Integer>();
		List<Integer> parentsList = new LinkedList<Integer>();
		int nodeID = node.getID();

		for(Node n: node.getAncestors()) {
			ancestors.add(n.getID());
			parentsList.add(n.getID());	
			retrieveStateName(nodeID,n.getID());
		}
		//if there is no continent in its ancestors
		if(Collections.disjoint(ancestors, continents)) {			
			int id = assignContinent(ancestors);
			if(id!=1) {
				int continentsPosiiton = parentsList.size();
				if(continentsPosiiton!=0) {
					continentsPosiiton -= 1;
				}
				parentsList.add(continentsPosiiton,id);
			}
		}

		return parentsList;
	}

	//if location is not a state, retrieve state from ancestors
	private static void retrieveStateName(int nodeID, int currentID) {
		if(mapIDNodes.get(nodeID).getLocation()!=null && mapIDNodes.get(nodeID).getLocation().getState()==null) {
			if(mapIDNodes.get(currentID).getLocation()!=null) {
				GeoNameLocation location = mapIDNodes.get(currentID).getLocation();
				if(location.getType()!=null && location.getType().equalsIgnoreCase("ADM1")) {				
					mapIDNodes.get(nodeID).getLocation().setState(simplifyLocationName(location.getName()));
				}
			}
		}
	}

	private static String generateAncestorNameList(List<Integer> parentsList) throws NullPointerException{
		StringBuilder hierarchy = new StringBuilder();

		String comma = "";
		for(int n: parentsList) {
			hierarchy.append(comma);
			comma = ", ";
			if(mapIDNodes.get(n).getLocation().getName()!=null)
				hierarchy.append(simplifyLocationName(mapIDNodes.get(n).getLocation().getName()));	
			else {
				log.info("******complete_hierarchy: name missing "+ n);
			}
		}
		return hierarchy.toString();
	}

	private static void fixMissingHierarchy(Map<Integer, GeoNameNode> mapIDNodes,GeoNameTree  geoTree) {
		int count = 0;
		int count1 = 0;	
		for(GeoNameNode g : mapIDNodes.values()) {
			try {
				if (g.getID() != ((GeoNameNode) geoTree.getRoot()).getID() && g.getFather() == null) {
					g.setFather((GeoNameNode) geoTree.getRoot());
					if (g.getLocation() != null) {
						if (g.getLocation().getCountry() != null) {
							if (g.getLocation().getAdm1() != null) {
								String fullAdm = g.getLocation().getCountry() + "." + g.getLocation().getAdm1();
								try {
									int amdId = geoTree.getAdmLookup().get(fullAdm);
									GeoNameNode adm = geoTree.getMapIDNodes().get(amdId);
									if (g != adm && adm != null) {
										g.setFather(adm);
										count ++;
									}
								}
								catch (Exception e) {
									if(geoTree.getCountryLookup().get(g.getLocation().getCountry())!=null) {
										int countryId = geoTree.getCountryLookup().get(g.getLocation().getCountry());
										GeoNameNode country = geoTree.getMapIDNodes().get(countryId);
										if (g != country && country != null) {
											g.setFather(country);
											count ++;
										}
									}
								}
							}
							else {
								try {
									int countryId = geoTree.getCountryLookup().get(g.getLocation().getCountry());
									GeoNameNode country = geoTree.getMapIDNodes().get(countryId);
									if (g != country && country != null) {
										g.setFather(country);
										count ++;
									}
								}catch(Exception e) {
									log.info("no country");
								}
							}
						}
					}
				}
				count1++;
				if(count1==20000) {
					log.info("20000 additional hierarchies added");
					count1 =0;
				}
			}catch(Exception e) {
				log.info("---------ERROR: "+g.getID() + " "+ g.getLocation().getCountry() +" "+ g.getLocation().getAdm1() +" : "+ e+" -------------");
			}
		}
		log.info("Added additional "+count+ " ancestors");
	}

	/**
	 * Assigns continent to locations missing Continent (27% of total) in their tree
	 * @param gIDs
	 * @return GeonameID of assigned Continent
	 */
	private static Integer assignContinent(Set<Integer> gIDs) {
		if (!Collections.disjoint(gIDs, naCountries)) {
			return 6255149;
		}
		else if (!Collections.disjoint(gIDs, saCountries)) {
			return 6255150; 
		}
		else if (!Collections.disjoint(gIDs, asCountries)) {
			return 6255147;
		}
		else if (!Collections.disjoint(gIDs, afCountries)) {
			return 6255146;
		}
		else if (!Collections.disjoint(gIDs, euCountries)) {
			return 6255148;
		}
		else if (!Collections.disjoint(gIDs, ocCountries)) {
			return 6255151;
		}
		else {
			return 1;
		}
	}

	/**
	 * Simplify country name
	 */
	private static String simplifyLocationName(String country_name) {
		if (country_name != null) {
			if (country_name.contains("Great Britain")) {
				country_name = "United Kingdom (UK)";
			}
			else if (country_name.contains("Russian Federation")) {
				country_name = "Russia (RU)";
			}
			else if (country_name.contains("Repubblica Italiana")) {
				country_name = "Italy (IT)";
			}
			else if (country_name.contains("Polynésie Française")) {
				country_name = "French Polynesi (PF)";
			}
			else if (country_name.contains("Lao People’s Democratic Republic")) {
				country_name = "Laos (LA)";
			}
			else if (country_name.contains("Argentine Republic")){
				country_name = "Argentina (AR)";
			}
			else if (country_name.contains("Portuguese Republic")){
				country_name = "Portugal (PT)";
			}
			else {
				if (country_name.contains("Republic of ")) {
					country_name = country_name.substring(country_name.indexOf("Republic of ")+12);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.contains("Kingdom of ")) {
					country_name = country_name.substring(country_name.indexOf("Kingdom of ")+11);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.contains("Union of ")) {
					country_name = country_name.substring(country_name.indexOf("Union of ")+9);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.contains("State of ")) {
					country_name = country_name.substring(country_name.indexOf("State of ")+9);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.contains("Commonwealth of ")) {
					country_name = country_name.substring(country_name.indexOf("Commonwealth of ")+16);
					if (country_name.contains("the ")) {
						country_name = country_name.substring(country_name.indexOf("the ")+4);
					}
				}
				else if (country_name.endsWith("Special Administrative Region")) {
					country_name = country_name.substring(0,country_name.indexOf("Special Administrative Region")-1);
				}
			}
		}
		else {
			country_name = "Unknown";
		}
		return country_name;
	}

	private static void writeToFile(List<LuceneObject> arraylist) {
		StringBuilder stringBuilder = new StringBuilder();
		PrintWriter printer = null;
		try {
			log.info("Start file write");
			printer = new PrintWriter(rawFilePath);

			for(LuceneObject luceneObject : arraylist) {
				stringBuilder = new StringBuilder();
				StringJoiner stringJoiner = new StringJoiner(" ");
				stringJoiner.add(String.valueOf(luceneObject.getId()));
				stringJoiner.add(luceneObject.getName());
				stringJoiner.add(luceneObject.getParentsID());
				stringJoiner.add(luceneObject.getParentsName());
				stringBuilder.append(stringJoiner);
				stringBuilder.append("\n");

				printer.write(stringBuilder.toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		printer.close();
		log.info("--------Write to file completed ");	
	}
}
