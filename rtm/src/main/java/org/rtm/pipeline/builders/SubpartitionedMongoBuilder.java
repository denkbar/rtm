package org.rtm.pipeline.builders;

import java.util.List;
import java.util.Properties;

import org.rtm.pipeline.task.PartitionedQueryTask;
import org.rtm.pipeline.task.RangeTask;
import org.rtm.request.selection.Selector;

public class SubpartitionedMongoBuilder extends PartitionedBuilder {

	private List<Selector> selectors;
	private Properties prop;
	private long partitioningFactor;
	private int poolSize;	
	
	public SubpartitionedMongoBuilder(Long start, Long end, Long increment, List<Selector> selectors, Properties prop,
			long partitioningFactor, int poolSize){
		super(start, end, increment);
		this.selectors = selectors;
		this.prop = prop;
		this.partitioningFactor = partitioningFactor;
		this.poolSize = poolSize;
	}

	@Override
	protected RangeTask createTask() {
		return new PartitionedQueryTask(this.selectors, this.prop, this.partitioningFactor, this.poolSize);
	}

}