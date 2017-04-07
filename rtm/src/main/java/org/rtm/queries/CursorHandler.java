package org.rtm.queries;

import java.util.Map;
import java.util.Properties;

import org.rtm.stream.Dimension;
import org.rtm.stream.LongAccumulationHelper;
import org.rtm.stream.TimeValue;
import org.rtm.time.RangeBucket;

@SuppressWarnings("rawtypes")
public class CursorHandler{

	public TimeValue handle(Iterable<? extends Map> iterable, RangeBucket<Long> myBucket, Properties prop) {

		TimeValue tv = new TimeValue(myBucket.getLowerBound());
		MeasurementHelper mh = new MeasurementHelper(prop);

		for(Map m : iterable){

			String m_dimension = mh.getPrimaryDimensionValue(prop, m);
			
			if(m_dimension == null || m_dimension.isEmpty()){
				// default fall back
				m_dimension = "groupall";
			}
			
			Dimension d = tv.getDimension(m_dimension);
			if(d == null){
				d = new Dimension(m_dimension);
				tv.setDimension(d);
			}
			
			LongAccumulationHelper la = d.getAccumulationHelper();
			//TODO: we'll only do hardcoded counts for now
			if(la.isInit("count"))
				la.initializeAccumulatorForMetric("count", (x,y) -> x+1, 0L);
			la.accumulateMetric("count", 1);
		}
		
		tv.values().stream().forEach(v -> v.copyAndFlush());
		
		return tv;
	}

}
