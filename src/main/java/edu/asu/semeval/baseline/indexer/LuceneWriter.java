package edu.asu.semeval.baseline.indexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.asu.semeval.baseline.indexer.geotree.GeoNameLocation;

import org.apache.lucene.document.FieldType;

public class LuceneWriter {
	static IndexWriter writer = null;
	private final static Logger log = Logger.getLogger("writeToLucene");
	private static final FieldType LONG_FIELD_TYPE_STORED_SORTED = new FieldType();
	
	// We create this explicit field type so that we can sort by population
	static {
		LONG_FIELD_TYPE_STORED_SORTED.setTokenized(true);
		LONG_FIELD_TYPE_STORED_SORTED.setOmitNorms(true);
		LONG_FIELD_TYPE_STORED_SORTED.setIndexOptions(IndexOptions.DOCS);
		LONG_FIELD_TYPE_STORED_SORTED.setNumericType(FieldType.NumericType.LONG);
		LONG_FIELD_TYPE_STORED_SORTED.setStored(true);
		LONG_FIELD_TYPE_STORED_SORTED.setDocValuesType(DocValuesType.NUMERIC);
		LONG_FIELD_TYPE_STORED_SORTED.freeze();
	}

	
	public LuceneWriter(String pathToIndex) {
		log.info("Creating Lucene Indexer at '" + pathToIndex + "'");
		setupWriter(pathToIndex);
	}

	private void setupWriter(String pathToIndex) {
		try {
			Directory dir = FSDirectory.open(Paths.get(pathToIndex));
			List<String> stops = Arrays.asList("a", "and", "are", "but", "by",
					"for", "if","into", "not", "such","that", "the", "their", 
					"then", "there", "these","they", "this", "was", "will", "with"); 
			CharArraySet stopWordsOverride = new CharArraySet(stops, true);
			Analyzer analyzer = new StandardAnalyzer(stopWordsOverride);
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(OpenMode.CREATE);
			writer = new IndexWriter(dir, iwc);
		} catch (Exception e){
			e.printStackTrace();
			log.info("error: "+e);
		}
	}

	public void exitWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.info("error: "+e);
		}
	}

	public void indexRecords(List<GeoNameLocation> geoNameLocs) {
		int count = 0,increments = 500000 ;
		for(GeoNameLocation geoNameLoc : geoNameLocs) {
			indexRecord(geoNameLoc);
			if(count++ % increments == 0) {
				log.info("Lucene processed records: "+ count);
			}
		}
		log.info("----Lucene process completed, records: "+ count + "/" + geoNameLocs.size());
	}

	public void indexRecord(GeoNameLocation geoNameLoc) {
		try {
			// Main document object for indexing
			Document doc = new Document();
			boolean print = false;
			
			StringBuilder ancestorsNames = new StringBuilder();
			StringBuilder ancestorsIds = new StringBuilder();
			
			//Normalize names and formats for indexing
			String id = String.valueOf(geoNameLoc.getId());
			doc.add(new StringField("GeonameId", id, Field.Store.YES));

			String name = geoNameLoc.getName();
			List<String> alternateNames = new ArrayList<String>();
			if(!geoNameLoc.getName().equalsIgnoreCase(geoNameLoc.getAsciiname())){
				alternateNames.add(geoNameLoc.getAsciiname());
			}
			
			String typeClass = String.valueOf(geoNameLoc.getTypeClass());
			doc.add(new StringField("Class", typeClass, Field.Store.YES));

			String typeCode = String.valueOf(geoNameLoc.getTypeCode());
			doc.add(new StringField("Code", typeCode, Field.Store.YES));
			
			Long population = Long.parseLong(geoNameLoc.getPopulation());
			doc.add(new LongField("Population", population, LONG_FIELD_TYPE_STORED_SORTED));
			
			String latitude = String.valueOf(geoNameLoc.getLatitude());
			doc.add(new StringField("Latitude", latitude, Field.Store.YES));

			String longitude = String.valueOf(geoNameLoc.getLongitude());
			doc.add(new StringField("Longitude", longitude, Field.Store.YES));
			
			//Add county if available
			if(geoNameLoc.getCounty() != null){
				String adm = String.valueOf(geoNameLoc.getCounty().getName());
				String admId = String.valueOf(geoNameLoc.getCounty().getId());
				doc.add(new TextField("County", adm, Field.Store.YES));
				ancestorsNames.append(adm + ", ");
				ancestorsIds.append(admId + ", ");
			}
			
			//Add state if available
			if(geoNameLoc.getState() != null){
				String adm = geoNameLoc.getState().getName();
				String stateCode = geoNameLoc.getState().getCode().split("\\.")[1];
				if(stateCode.matches("[A-Z]{2,5}")){
					adm += "(" + stateCode + ")";
					// if this is a state record record all variations of names
					if (typeCode.equals("ADM1") && typeClass.equals("A")){
						alternateNames.add(stateCode);
					}
				}
				String admId = String.valueOf(geoNameLoc.getState().getId());
				doc.add(new TextField("State", adm, Field.Store.YES));
				ancestorsNames.append(adm + ", ");
				ancestorsIds.append(admId + ", ");
			}
			
			//Add country if not a country or continent itself
			if(geoNameLoc.getCountry() != null){
				String country = String.valueOf(geoNameLoc.getCountry().getName());
				// if this is a country record record all variations of names
				if (typeCode.equals("PCLI") && typeClass.equals("A")){
					if(!name.equalsIgnoreCase(country)){
						String cleanName = cleanCountryNames(name, country);
						if (name.equals(cleanName)){
							alternateNames.add(country);
						} else {
							name = cleanName;
						}
					}
					alternateNames.add(geoNameLoc.getCountry().getIso());
					alternateNames.add(geoNameLoc.getCountry().getIso3());
				} else {
					country += " (" + geoNameLoc.getCountry().getIso() + ", "
							+ geoNameLoc.getCountry().getIso3() + ")"; 
				}
				doc.add(new TextField("Country", country, Field.Store.YES));
				String countryId = String.valueOf(geoNameLoc.getCountry().getId());
				ancestorsNames.append(country + ", ");
				ancestorsIds.append(countryId + ", ");
				String continent = String.valueOf(geoNameLoc.getCountry().getContinent());
				doc.add(new TextField("Continent", continent, Field.Store.YES));
				ancestorsNames.append(continent);
				
				//create ancestors for easy querying
				doc.add(new TextField("AncestorsNames", ancestorsNames.toString(), Field.Store.YES));
				// TODO: Add method for appending continentId
				//doc.add(new TextField("AncestorsIds", ancestorsIds.toString(), Field.Store.YES));
				
			} else {
				//Check when it is not a country or continent or major region
				print = true;
			}
			
			//Finally add the name field
			if(alternateNames.size() > 0){
				name += " (" ;
				for (String alternateName : alternateNames){
					name += alternateName + ", "; 
				}
				name = name.substring(0, name.length()-2) + ")" ;
			}
			doc.add(new TextField("Name", name, Field.Store.YES));
			
			
			if(print){
				for(IndexableField field: doc.getFields()){
					System.out.print(field.name() + ":" + field.stringValue() + ", ");
				}
				System.out.println();
			}
			
			//Create fields and index to lucene
			writer.addDocument(doc);
		} catch (Exception e){
			log.info("error: "+ e.getMessage() + "for" + geoNameLoc);
			e.printStackTrace();
		}
	}

	private String cleanCountryNames(String name, String country){
		if (name.startsWith("United Kingdom of ")){
			// We make this change as conjunctions in union names turns out
			// to be a bad idea overall for search operations
			// Also add abbrv UK which is not the ISO code for UK
			name = "United Kingdom (Great Britain, UK)";
		} else if (name.startsWith("United Arab Emirates")){
			// Add missing abbrv UAE which is not the ISO code for it
			name = "United Arab Emirates (UAE)";
		}
		return name;
	}
}
