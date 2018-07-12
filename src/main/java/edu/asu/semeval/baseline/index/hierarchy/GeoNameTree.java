package edu.asu.semeval.baseline.index.hierarchy;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

//javac -d src src/geoHierarchy/*.java
//java -cp bin geoHierarchy.TestMain >out.log 2>&1 &
//Tail -f out.log

/**
* Tree structure for storing GeoName relationships
* Similar to the GenBankTree
* @author demetri
*/
public class GeoNameTree extends Tree {
	private final Logger log = Logger.getLogger("GeoNameTree");
	private Map<Integer, GeoNameNode> mapIDNodes = null;
	private Map<String, Integer> countryLookup = null;
	private Map<String, Integer> admLookup = null;
	private final String GeoInfoFile;
	private final String GeoMappingFile;
	private final String GeoCountryFile;
	private final String GeoADMFile;
	private static GeoNameTree tree = null;
	
	private GeoNameTree(String geoDirectory) {
		log.info("Initializing HashMaps...");
		mapIDNodes = new HashMap<Integer, GeoNameNode>(6000000, (float) 0.975);
		countryLookup = new HashMap<String, Integer>(300, (float) 0.9);
		admLookup = new HashMap<String, Integer>(5000, (float) 0.8);

		log.info("Initializing Tree...");
		GeoNameLocation earth = new GeoNameLocation();
		earth.setId(6295630);
		earth.setName("Earth");
		root = new GeoNameNode(true, earth);
		mapIDNodes.put(root.getID(), (GeoNameNode) root);
		addContinents();

		log.info("Loading geonames files...");
		GeoInfoFile = geoDirectory + "allCountries.txt";
		GeoMappingFile = geoDirectory + "hierarchy.txt";
		GeoCountryFile = geoDirectory + "countryInfo.txt";
		GeoADMFile = geoDirectory + "admin1CodesASCII.txt";

		fillTree();
		
		log.info("Finished constructing GeoName Tree");
	}
	
	/**
	 * Reads through the give geo_map_file and fills out the GeoNameTree
	 */
	protected void fillTree() {
		
		try {
			allCountries();
			hierarchy();
			countryInfo();
			admin1CodesASCII();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error Filling GeoNameTree: " + e.getMessage());
		}
	}
	
	//open allcountries file and load required fields of each record into mapIDNodes
		private void allCountries() throws FileNotFoundException {
			File geoFile = new File(GeoInfoFile);
			Scanner scan = new Scanner(geoFile);
			int count =0, increment=50000;
			while (scan.hasNext()) {
				String record = scan.nextLine().trim();
				if(record.split("\t").length==19){
				String[] geoname = record.split("\t"); 
				String f_class = geoname[6];
				String f_code = geoname[7];
				if(!(f_class.equalsIgnoreCase("A") && f_code.equalsIgnoreCase("ZN"))) {	//remove regions like trade unions
					if(geoname[0].equalsIgnoreCase("6295630")){							//ignore EARTH, already included
						System.out.println("Skipping Earth");
						continue;
					}
					GeoNameLocation g = new GeoNameLocation();
					g.setId(Integer.parseInt(geoname[0]));
					if(f_code.equalsIgnoreCase("PCLI")) {								//country
						g.setName(geoname[1] + " (" + geoname[8] + ")");
					}else {
						g.setName(geoname[1]);
					}
					//g.setName(geoname[1]);
					g.setLatitude(Double.parseDouble(geoname[4]));
					g.setLongitude(Double.parseDouble(geoname[5]));
					g.setTypeClass(f_class);
					g.setType(f_code);
					g.setCountry(geoname[8]);
					g.setAdm1(geoname[10]);
					g.setPopulation(geoname[14]);
//					g.setAdm2(geoname[11]); 
//					g.setAdm3(geoname[12]);
//					g.setAdm4(geoname[13]);
					GeoNameNode gNode = new GeoNameNode(false, g);
					mapIDNodes.put(gNode.getID(), gNode);
					
					count++;
					if(count==increment) {
						log.info("allCountries.txt count: "+ count);
						increment+=50000;
					}
				}
				}else {
					log.info("------error: String size too long");
					log.info("length: "+ record.split("\t").length +" String: "+ record.substring(0, 10) );
				}
			}	
			scan.close();
			log.info("----------AllCountries.txt completed, count: "+ count);
		}

	//open hierarchy file and add parent and child of each node.
	//if record is not found in mapIdNodes then create a new entry
	private void hierarchy() throws FileNotFoundException {
		File geoFile = new File(GeoMappingFile);
		Scanner scan = new Scanner(geoFile);
		int count =0, increment=50000;
		while (scan.hasNext()) {
			String[] geoname = scan.nextLine().trim().split("\t"); 
			int parent_id = Integer.parseInt(geoname[0]);
			int child_id = Integer.parseInt(geoname[1]);
			try {
				GeoNameNode c = mapIDNodes.get(child_id);
				GeoNameNode p = mapIDNodes.get(parent_id);
				if(c!=null && p!=null) {
					if(c.getFather()!=null) {
						log.info(c.getID()+ " has parent "+ c.getFather().getID() + " AND " + p.getID());
					}
					p.addChild(c);
					c.setFather(p);
				}
				
				count++;
				if(count==increment) {
					log.info("hierarchy.txt count: "+ count);
					increment+=50000;
				}
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "ERROR parsing GeoID: " + child_id + " : " + e.getMessage());
			}
		}
		scan.close();
		log.info("---------hierarchy.txt completed, count: "+ count);
		
		
	}
	
	//open countryInfo file a record ISO and geoname of all countires
	private void countryInfo() throws FileNotFoundException {
		String line;
		int count = 0;
		File geoFile = new File(GeoCountryFile);
		Scanner scan = new Scanner(geoFile);
		while (scan.hasNext()) {
			line = scan.nextLine();
			if (!line.startsWith("#")) {
				String[] geoname = line.trim().split("\t");
				String name = geoname[0];					//ISO
				int id = Integer.parseInt(geoname[16]);		//geonameid
				countryLookup.put(name,id);
			}
			count++;
			if(count==20000) {
				log.info("countryInfo.txt counted 20000");
				count=0;
			}
		}
		scan.close();
	}

	//open admin1CodesASCII and record  the admin level and geoname of each location
	private void admin1CodesASCII() throws FileNotFoundException {
		File geoFile = new File(GeoADMFile);
		int count = 0;
		String line;
		Scanner scan = new Scanner(geoFile);
		while (scan.hasNext()) {
			line = scan.nextLine();
			if (!line.startsWith("#")) {
				String[] geoname = line.trim().split("\t");
				String name = geoname[0];
				int id = Integer.parseInt(geoname[3]);
				admLookup.put(name,id);
			}
			count++;
			if(count==20000) {
				log.info("admin1CodesASCII.txt counted 20000");
				count=0;
			}
		}
		scan.close();
	}
	
	/**
	 * The all countries file does not include continents, so here we manually add them to  the tree
	 */
	private void addContinents() {
		GeoNameNode temp;
		//Africa//
		GeoNameLocation africa = new GeoNameLocation();
		africa.setId(6255146);
		africa.setName("Africa");
		africa.setLatitude(7.1881);
		africa.setLongitude(21.09375);
		africa.setContinent("AF");
		africa.setPopulation("1030400000");
		africa.setTypeClass("L");
		africa.setType("CONT");
		temp = new GeoNameNode(false, africa);
		mapIDNodes.put(temp.getID(), temp);
		temp.setFather(root);
		root.addChild(temp);

		//Asia//
		GeoNameLocation asia = new GeoNameLocation();
		asia.setId(6255147);
		asia.setName("Asia");
		asia.setLatitude(29.84064);
		asia.setLongitude(89.29688);
		asia.setContinent("AS");
		asia.setPopulation("4157300000");
		asia.setTypeClass("L");
		asia.setType("CONT");
		temp = new GeoNameNode(false, asia);
		mapIDNodes.put(temp.getID(), temp);
		temp.setFather(root);
		root.addChild(temp);
		//Europe//
		GeoNameLocation europe = new GeoNameLocation();
		europe.setId(6255148);
		europe.setName("Europe");
		europe.setLatitude(48.69096);
		europe.setLongitude(9.14062);
		europe.setContinent("EU");
		europe.setPopulation("738600000");
		europe.setTypeClass("L");
		europe.setType("CONT");
		temp = new GeoNameNode(false, europe);
		mapIDNodes.put(temp.getID(), temp);
		temp.setFather(root);
		root.addChild(temp);
		//North America//
		GeoNameLocation nAmerica = new GeoNameLocation();
		nAmerica.setId(6255149);
		nAmerica.setName("North America");
		nAmerica.setLatitude(46.07323);
		nAmerica.setLongitude(-100.54688);
		nAmerica.setContinent("NA");
		nAmerica.setPopulation("461114000");
		nAmerica.setTypeClass("L");
		nAmerica.setType("CONT");
		temp = new GeoNameNode(false, nAmerica);
		mapIDNodes.put(temp.getID(), temp);
		temp.setFather(root);
		root.addChild(temp);
		//Oceania//
		GeoNameLocation oceania = new GeoNameLocation();
		oceania.setId(6255151);
		oceania.setName("Oceania");
		oceania.setLatitude(-18.31281);
		oceania.setLongitude(138.51562);
		oceania.setContinent("OC");
		oceania.setPopulation("36700000");
		oceania.setTypeClass("L");
		oceania.setType("CONT");
		temp = new GeoNameNode(false, oceania);
		mapIDNodes.put(temp.getID(), temp);
		temp.setFather(root);
		root.addChild(temp);
		//South America//
		GeoNameLocation sAmerica = new GeoNameLocation();
		sAmerica.setId(6255150);
		sAmerica.setName("South America");
		sAmerica.setLatitude(-14.60485);
		sAmerica.setLongitude(-57.65625);
		sAmerica.setContinent("SA");
		sAmerica.setPopulation("390700000");
		sAmerica.setTypeClass("L");
		sAmerica.setType("CONT");
		temp = new GeoNameNode(false, sAmerica);
		mapIDNodes.put(temp.getID(), temp);
		temp.setFather(root);
		root.addChild(temp);
		//Antarctica//
		GeoNameLocation antarctica = new GeoNameLocation();
		antarctica.setId(6255152);
		antarctica.setName("Antarctica");
		antarctica.setLatitude(-78.15856);
		antarctica.setLongitude(16.40626);
		antarctica.setContinent("AN");
		antarctica.setPopulation("1000");
		antarctica.setTypeClass("L");
		antarctica.setType("CONT");
		temp = new GeoNameNode(false, antarctica);
		mapIDNodes.put(temp.getID(), temp);
		temp.setFather(root);
		root.addChild(temp);
	}

	public void free() {
		mapIDNodes.clear();
	}
	
	public Node getNode(int ID) {
		return mapIDNodes.get(ID);
	}
	/**
	 * for testing and manual data extraction only
	 */
	public void getNodeChildrenLists(int ID) {
		GeoNameNode node = mapIDNodes.get(ID);
		StringBuilder children = new StringBuilder();
		StringBuilder vals = new StringBuilder();
		for (Node n : node.getChildren()) {
			children.append("\"");
			children.append(n.concept);
			children.append("\",");
			vals.append(n.ID);
			vals.append(",");
		}
		String c = children.toString();
		String v = vals.toString();
		log.info(c);
		log.info(v);
	}
	
	public static GeoNameTree getInstance(String dir) {
		if (tree == null) {
			tree = new GeoNameTree(dir);
		}
		return tree;
	}
	
	public Node getRoot() {
		return root;
	}
	
	public Map<String, Integer> getCountryLookup() {
		return countryLookup;
	}
	public Map<Integer, GeoNameNode> getMapIDNodes() {
		return mapIDNodes;
	}
	public Map<String, Integer> getAdmLookup() {
		return admLookup;
	}
}