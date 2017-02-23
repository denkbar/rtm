package org.rtm.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.bson.Document;
import org.rtm.commons.MeasurementConstants;
import org.rtm.db.DBClient;
import org.rtm.request.selection.Selector;
import org.rtm.stream.TimeValue;
import org.rtm.time.LongTimeInterval;
import org.rtm.time.RangeBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;

public class QueryCallable implements Callable<TimeValue>{
	
	private static final Logger logger = LoggerFactory.getLogger(QueryCallable.class);

	private BsonQuery query;
	private RangeBucket<Long> bucket;
	private Properties prop;

	public QueryCallable(List<Selector> sel, RangeBucket<Long> bucket,Properties requestProp) {
		this.bucket = bucket;
		this.prop = requestProp;
		this.query = buildQuery(sel, RangeBucket.toLongTimeInterval(bucket));
	}

	private BsonQuery buildQuery(List<Selector> sel, LongTimeInterval bucket) {
		BsonQuery aQuery = new BsonQuery(BsonQuery.selectorsToQuery(sel));
		return new BsonQuery(mergeTimelessWithTimeCriterion((Document)aQuery, buildTimeCriterion(bucket)));
	}

	@Override
	public TimeValue call() throws Exception {
		return new MeasurementIterableHandler().handle(
										new DBClient().executeQuery(query),
										bucket,
										prop);
	}

	private static Document mergeTimelessWithTimeCriterion(Document timelessQuery, BasicDBObject timeCriterion) {
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
	public static LongTimeInterval figureEffectiveTimeBoundariesViaMongoDirect(LongTimeInterval lti, List<Selector> sel) throws Exception {
		BsonQuery baseQuery = new BsonQuery(BsonQuery.selectorsToQuery(sel));
		Document completeQuery = mergeTimelessWithTimeCriterion(baseQuery, buildTimeCriterion(lti));
		DBClient db = new DBClient();
		
		//TODO: get Time key from request Properties
		Iterable naturalIt = db.executeQuery(completeQuery, MeasurementConstants.BEGIN_KEY, 1);
		Iterable reverseIt = db.executeQuery(completeQuery, MeasurementConstants.BEGIN_KEY, -1);
		
		Map min = (Map) naturalIt.iterator().next();
		Map max = (Map) reverseIt.iterator().next();

		Long maxVal = (Long)max.get(MeasurementConstants.BEGIN_KEY); 
		Long minVal = (Long)min.get(MeasurementConstants.BEGIN_KEY);
		
		long result = maxVal - minVal;
		logger.debug("time window : " + maxVal + " - " + minVal + " = " + result);
		
		if(result < 1L)
			throw new Exception("Could not compute auto-granularity : result="+result);
		
		((MongoCursor) naturalIt.iterator()).close();
		((MongoCursor) reverseIt.iterator()).close();
	
		return new LongTimeInterval(minVal, maxVal, 0L);
	}

	public static long computeOptimalIntervalSize(long timeWindow, int targetSeriesDots){
		long result = Math.abs(timeWindow / targetSeriesDots);
		return result;
	}
}