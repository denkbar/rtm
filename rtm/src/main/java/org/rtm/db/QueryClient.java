package org.rtm.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bson.Document;
import org.rtm.commons.MeasurementAccessor;
import org.rtm.commons.MeasurementConstants;
import org.rtm.range.time.LongTimeInterval;
import org.rtm.request.selection.Selector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;

public class QueryClient {

	private static final Logger logger = LoggerFactory.getLogger(QueryClient.class);

	MeasurementAccessor ma = MeasurementAccessor.getInstance();

	public QueryClient() {
		this.ma = MeasurementAccessor.getInstance();
	}

	@SuppressWarnings("rawtypes")
	public Iterable<? extends Map> executeQuery(Document timelessQuery) {
		return ma.find(timelessQuery);
	}

	@SuppressWarnings("rawtypes")
	public Iterable<? extends Map> executeQuery(Document timelessQuery, String sortKey, Integer sortDirection) {
		return ma.find(timelessQuery, new BasicDBObject(sortKey, sortDirection));
	}

	@SuppressWarnings("rawtypes")
	public Iterable<? extends Map> executeQuery(Document timelessQuery, String sortKey, Integer sortDirection, int skip, int limit) {
		return ma.find(timelessQuery, new BasicDBObject(sortKey, sortDirection), skip, limit);
	}

	public static BsonQuery buildQuery(List<Selector> sel, LongTimeInterval bucket) {
		BsonQuery aQuery = new BsonQuery(BsonQuery.selectorsToQuery(sel));
		return new BsonQuery(mergeTimelessWithTimeCriterion((Document)aQuery, buildTimeCriterion(bucket)));
	}

	public static Document mergeTimelessWithTimeCriterion(Document timelessQuery, BasicDBObject timeCriterion) {
		Document obj = new Document();
		obj.putAll(timelessQuery);
		obj.putAll(new Document(timeCriterion));
		return obj;
	}

	public static BasicDBObject buildTimeCriterion(LongTimeInterval bucket) {
		List<BasicDBObject> criteria = new ArrayList<BasicDBObject>();
		criteria.add(new BasicDBObject(MeasurementConstants.BEGIN_KEY, new BasicDBObject("$gte", bucket.getBegin())));
		criteria.add(new BasicDBObject(MeasurementConstants.BEGIN_KEY, new BasicDBObject("$lt", bucket.getEnd())));
		return new BasicDBObject("$and", criteria);
	}

	@SuppressWarnings("rawtypes")
	public static LongTimeInterval findEffectiveBoundariesViaMongo(LongTimeInterval lti, List<Selector> sel) throws Exception {
		BsonQuery query = new BsonQuery(BsonQuery.selectorsToQuery(sel));

		QueryClient db = new QueryClient();

		//TODO: get Time key from request Properties
		//logger.debug(completeQuery.toString());
		MongoCursor naturalIt = (MongoCursor)db.executeQuery(query, MeasurementConstants.BEGIN_KEY, 1).iterator();
		MongoCursor reverseIt = (MongoCursor)db.executeQuery(query, MeasurementConstants.BEGIN_KEY, -1).iterator();
		Map min = null;
		Map max = null;
		try{
			min = (Map) naturalIt.next();
			max = (Map) reverseIt.next();
		}catch(NoSuchElementException e){
			return new LongTimeInterval(0L, 1L);
		}

		Long maxVal = (Long)max.get(MeasurementConstants.BEGIN_KEY) +1; // $lt operator 
		Long minVal = (Long)min.get(MeasurementConstants.BEGIN_KEY);

		long result = maxVal - minVal;
		//logger.debug("time window : " + maxVal + " - " + minVal + " = " + result);

		if(result < 1L)
			throw new Exception("Could not compute auto-granularity : result="+result);

		naturalIt.close();
		reverseIt.close();

		return new LongTimeInterval(minVal, maxVal);
	}

	public static long computeOptimalIntervalSize(long timeWindow, int targetSeriesDots){
		return Math.abs(timeWindow / targetSeriesDots) + 1;
	}

	public static long run90PclOnFirstSample(int heuristicSampleSize, List<Selector> sel) {
		logger.debug("Starting sampling of first " + heuristicSampleSize + " data points...");
		long start = System.currentTimeMillis();
		BsonQuery query = new BsonQuery(BsonQuery.selectorsToQuery(sel));

		QueryClient db = new QueryClient();

		SortedSet<Long> sortedValues = new TreeSet<>();

		Iterable it = db.executeQuery(query, MeasurementConstants.BEGIN_KEY, 1, 0, heuristicSampleSize);
		MongoCursor cursor = (MongoCursor) it.iterator();
		Map dot = null;
		for(Object o : it) {
			try{
				dot = (Map) o;
			}catch(Exception e){
				//no more elements
				break;
			}
			if(dot != null) {
				sortedValues.add((Long)dot.get(MeasurementConstants.VALUE_KEY));
			}
		}
		cursor.close();
		
		int position = Math.round(sortedValues.size()*0.9F);

		logger.debug("Sampling complete. Duration was "+ (System.currentTimeMillis() - start) +" ms.");

		if(position >= sortedValues.size())
			return sortedValues.last();
		else
			return sortedValues.toArray(new Long[0])[position];
	}
}
