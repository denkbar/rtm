package org.rtm.metrics.accumulation.base;

import java.util.Properties;

import org.rtm.metrics.WorkObject;
import org.rtm.metrics.accumulation.Accumulator;
import org.rtm.metrics.accumulation.histograms.Histogram;
import org.rtm.utils.ServiceUtils;

public class HistogramAccumulator implements Accumulator<Long, Histogram>{

	private int nbPairs;
	private int approxMs;
	
	@Override
	public void initAccumulator(Properties props) {
		nbPairs = Integer.parseInt((String)ServiceUtils.decideServiceProperty(props, "histogram.nbPairs", 40));
		approxMs = Integer.parseInt((String)ServiceUtils.decideServiceProperty(props, "histogram.approxMs", 200));
	}

	@Override
	public WorkObject buildStateObject() {
		return new HistogramAccumulatorState(nbPairs, approxMs);
	}

	@Override
	public void accumulate(WorkObject wobj, Long value) {
		((HistogramAccumulatorState) wobj).ingest(value);
	}

	@Override
	public void mergeLeft(WorkObject wobj1, WorkObject wobj2) {
		try {
			((HistogramAccumulatorState) wobj1).merge(((HistogramAccumulatorState)wobj2));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Couldn't merge histograms.");
		}
	}

	@Override
	public Histogram getValue(WorkObject wobj) {
		return (Histogram) wobj;
	}

	public class HistogramAccumulatorState extends Histogram implements WorkObject{

		public HistogramAccumulatorState(int nbPairs, int approxMs) {
			super(nbPairs, approxMs);
		}

	}

}
