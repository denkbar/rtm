package org.rtm.metrics.postprocessing;

import java.util.Map;

import org.rtm.metrics.WorkObject;
import org.rtm.metrics.accumulation.base.CountAccumulator.CountAccumulatorState;
import org.rtm.metrics.accumulation.base.SumAccumulatorState;

public class AverageMetric implements SubscribedMetric<Float>{

	@Override
	public String[] getSubscribedAccumulatorsList() {
		return new String[] {"org.rtm.metrics.accumulation.base.CountAccumulator", "org.rtm.metrics.accumulation.base.SumAccumulator"};
	}

	@Override
	public Float computeMetric(Map<String, WorkObject> wobjs, Long intervalSize) {
		CountAccumulatorState countState = (CountAccumulatorState)wobjs.get("org.rtm.metrics.accumulation.base.CountAccumulator");
		SumAccumulatorState sumState = (SumAccumulatorState)wobjs.get("org.rtm.metrics.accumulation.base.SumAccumulator");
		
		return (float) (sumState.getAccumulator().get() / countState.getAccumulator().get());
	}

	@Override
	public String getDisplayName() {
		return "std_avg";
	}

}
