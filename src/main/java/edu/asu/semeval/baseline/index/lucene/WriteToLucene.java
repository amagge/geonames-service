package edu.asu.semeval.baseline.index.lucene;

import java.io.IOException;
import java.nio.file.Paths;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.FieldType;

public class WriteToLucene {
	public static final String INDEX_PATH = "index/geonames";
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
	
	public static void toLucene(List<LuceneObject> arraylist) {
		log.info("Creating Lucene file");
		setupWriter();
		readFileAndWriteToIndex(arraylist);
		exitWriter();
	}
	
	private static void exitWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.info("error: "+e);
		}
	}

	private static void setupWriter() {
		try {
			Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
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

	private static void readFileAndWriteToIndex(List<LuceneObject> arraylist) {
		int count = 0,increment = 50000 ;
		try {
			for(LuceneObject luceneObject : arraylist) {
				try {
						Document doc = new Document();
						doc.add(new StringField("GeonameId", String.valueOf(luceneObject.getId()), Field.Store.YES));
						doc.add(new TextField("Name", luceneObject.getName(), Field.Store.YES));
						doc.add(new TextField("State", String.valueOf(luceneObject.getLocation().getState()), Field.Store.YES));
						doc.add(new TextField("Country", String.valueOf(luceneObject.getLocation().getCountry()), Field.Store.YES));
						doc.add(new StringField("LocationType", String.valueOf(luceneObject.getLocation().getType()), Field.Store.YES));
						doc.add(new LongField("Population", Long.parseLong(luceneObject.getLocation().getPopulation()), LONG_FIELD_TYPE_STORED_SORTED));
						doc.add(new StringField("Latitude", String.valueOf(luceneObject.getLocation().getLatitude()), Field.Store.YES));
						doc.add(new StringField("Longitude", String.valueOf(luceneObject.getLocation().getLongitude()), Field.Store.YES));
						doc.add(new TextField("AncestorId", luceneObject.getParentsID(), Field.Store.YES));
						doc.add(new TextField("AncestorName", luceneObject.getParentsName(), Field.Store.YES));
						writer.addDocument(doc);
			
						count++;
						if(count==increment) {
							log.info("Lucene processed records: "+ count);
							increment+=50000;
						}

				} catch (Exception e){
					e.printStackTrace();
				}
			}
			log.info("------------Lucene process completed, records: "+ count);
		} catch(Exception e) {
			e.printStackTrace();
			log.info("error: "+e);
		} 
	}


}
