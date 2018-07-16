package edu.asu.semeval.baseline.rest.search;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import edu.asu.semeval.baseline.rest.exception.InvalidLuceneQueryException;
import edu.asu.semeval.baseline.rest.exception.LuceneSearcherException;

/**
 * Responsible for retrieving information from Lucene
 * @author amagge
 */
@Repository("LuceneSearcher")
public class LuceneSearcher {
	
	private Directory indexDirectory;
	private QueryParser queryParser;
	
	private static final FieldType LONG_FIELD_TYPE_STORED_SORTED = new FieldType();
	static {
		LONG_FIELD_TYPE_STORED_SORTED.setTokenized(true);
		LONG_FIELD_TYPE_STORED_SORTED.setOmitNorms(true);
		LONG_FIELD_TYPE_STORED_SORTED.setIndexOptions(IndexOptions.DOCS);
		LONG_FIELD_TYPE_STORED_SORTED.setNumericType(FieldType.NumericType.LONG);
		LONG_FIELD_TYPE_STORED_SORTED.setStored(true);
		LONG_FIELD_TYPE_STORED_SORTED.setDocValuesType(DocValuesType.NUMERIC);
		LONG_FIELD_TYPE_STORED_SORTED.freeze();
	}

	private final static Logger logger = Logger.getLogger("LuceneSearcher");
	
	/**
	 * Method that starts the Lucene Service and sanity checks the index
	 */
	public LuceneSearcher(@Value("${lucene.index.location}") String indexLocation) throws LuceneSearcherException {
		try {
			Path index = Paths.get(indexLocation);
			indexDirectory = FSDirectory.open(index);
			CharArraySet stopWordsOverride = new CharArraySet(Collections.emptySet(), true);
			Analyzer analyzer = new StandardAnalyzer(stopWordsOverride);
			queryParser = new QueryParser("Name", analyzer); 
			// Analyzer analyzer = new KeywordAnalyzer(stopWordsOverride);
			// queryParser = new QueryParser("dummyfield", new KeywordAnalyzer());
			logger.info("Connected to Index at: "+indexLocation);
			IndexReader reader = DirectoryReader.open(indexDirectory);
			logger.info("Number of docs: "+reader.numDocs());
			if(reader.numDocs()>0){
				logger.info("Getting fields for a sample document in the index. . .");
				reader.document(1).getFields();
				List<IndexableField> fields = reader.document(1).getFields();
				for(int i=0; i<fields.size();i++){
					logger.info(i+1 + ") " + fields.get(i).name() + ":"+ fields.get(i).stringValue());
				}
			} else {
				logger.warning("Index is empty!!");
			}
			reader.close();
		} catch (IOException ioe) {
			logger.log(Level.SEVERE, "Could not open Lucene Index at: "+indexLocation+ " : "+ioe.getMessage());
			throw new LuceneSearcherException("Could not open Lucene Index at: "+indexLocation+ " : "+ioe.getMessage());
		}
	}
	
	/**
	 * Closes Lucene resources
	 */
	@PreDestroy
	private void close() {
		try {
			indexDirectory.close();
			logger.info("Lucene Index closed");
		}
		catch (IOException ioe) {
			logger.warning("Issue closing Lucene Index: "+ioe.getMessage());
		}
	}

	/**
	 * Search Lucene Index for records matching querystring
	 * @param querystring - valid Lucene query string
	 * @param numRecords - number of requested records 
	 * @param showAvailable - check for number of matching available records 
	 * @return Top Lucene query results as a Result object
	 * @throws LuceneSearcherException 
	 * @throws InvalidLuceneQueryException 
	 */
	public Result searchIndex(String querystring, int numRecords, boolean showAvailable) throws LuceneSearcherException, InvalidLuceneQueryException {
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		TotalHitCountCollector collector = null;
		try {
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			query = queryParser.parse(querystring);
			logger.info("'" + querystring + "' ==> '" + query.toString() + "'");
			if(showAvailable){
				collector = new TotalHitCountCollector();
				indexSearcher.search(query, collector);
			}
			documents = indexSearcher.search(query, numRecords);
			List<Map<String,String>> mapList = new LinkedList<Map<String,String>>();
			for (ScoreDoc scoreDoc : documents.scoreDocs) {
				Document document = indexSearcher.doc(scoreDoc.doc);
				Map<String,String> docMap = new HashMap<String,String>();
				List<IndexableField> fields = document.getFields();
				for(IndexableField field : fields){
					docMap.put(field.name(), field.stringValue());
				}
				mapList.add(docMap);
			}
			Result result = new Result(mapList, 
					mapList.size(), 
					collector==null?(mapList.size() < numRecords?mapList.size():-1)
							:collector.getTotalHits());
			return result;
		} catch (ParseException pe) {
			throw new InvalidLuceneQueryException(pe.getMessage());
		} catch (Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				logger.warning("Could not close IndexReader: "+ioe.getMessage()); 
			}
		}
	}

	/**
	 * Search Lucene Index for list of locations and return best matched geonameid
	 * @param querystring - valid Lucene query string
	 * @param numRecords - number of requested records 
	 * @param showAvailable - check for number of matching available records 
	 * @return Top Lucene query results as a Result object
	 * @throws LuceneSearcherException 
	 * @throws InvalidLuceneQueryException 
	 */
	public Result searchLocation(String location) throws LuceneSearcherException, InvalidLuceneQueryException {
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		TotalHitCountCollector collector = null;
		try {
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			String querystring = getQueryString(location); //"Name:"+namedEntity.getText().trim();
			query = queryParser.parse(querystring);
			logger.info("'" + querystring + "' ==> '" + query.toString() + "'");
			collector = new TotalHitCountCollector();
			SortField sortField = new SortField("Population", SortField.Type.LONG, true);
			Sort sort = new Sort(sortField);
			indexSearcher.search(query, collector);
			int totalCounts = collector.getTotalHits();
			documents = indexSearcher.search(query, 1, sort);
			List<Map<String,String>> mapList = new LinkedList<Map<String,String>>();
			if (totalCounts > 0){
				Document document = indexSearcher.doc(documents.scoreDocs[0].doc);
				System.out.println(document.toString());
				Map<String,String> docMap = new HashMap<String,String>();
				List<IndexableField> fields = document.getFields();
				for(IndexableField field : fields){
					docMap.put(field.name(), field.stringValue());
				}
				mapList.add(docMap);
			}
			Result result = new Result(mapList, mapList.size(), totalCounts);
			return result;
		} catch (ParseException pe) {
			throw new InvalidLuceneQueryException(pe.getMessage());
		} catch (Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				logger.warning("Could not close IndexReader: "+ioe.getMessage()); 
			}
		}
	}

	private String getQueryString(String location) {
		String[] locations = location.split(",",2);
		String parents ="",queryString="";
		if(locations.length>1) {
			String child = locations[0];
			parents = locations[1];
			parents = parents.replace(",", " ").trim();
			if (child.isEmpty() || parents.isEmpty()) {
				queryString += parents.isEmpty()?"":"AncestorsNames:"+parents;
				queryString += child.isEmpty()?"":"Name:"+child;
				if (child.isEmpty() && parents.isEmpty()) {
					queryString = "Name:NOTAVALIDLOCATIONNAME";
				}
			} else {
				queryString +="AncestorsNames:"+parents+" AND Name:"+child;
			}
		} else {
			//if there are multiple keywords, search by keyword as well
			String[] keywords = location.split(" ");
			if(keywords.length>1) {
				for(String keyword : keywords){
					queryString += "Name:\""+keyword +"\" OR ";
				}
			}
			queryString = "Name:\""+location +"\"";
		}
		logger.info("'" + location + "' ==> '" + queryString + "'");
		return queryString;
	}

}
