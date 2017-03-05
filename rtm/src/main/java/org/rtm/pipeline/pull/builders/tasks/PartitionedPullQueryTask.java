package org.rtm.pipeline.pull.builders.tasks;

import java.util.List;
import java.util.Properties;

import org.rtm.pipeline.commons.BlockingMode;
import org.rtm.pipeline.pull.PullPipeline;
import org.rtm.pipeline.pull.builders.PullPipelineBuilder;
import org.rtm.pipeline.pull.builders.SimplePipelineBuilder;
import org.rtm.pipeline.tasks.MergingPartitionedQueryTask;
import org.rtm.range.RangeBucket;
import org.rtm.request.selection.Selector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//public class PartitionedPullQueryTask extends SharingPartitionedQueryTask{
public class PartitionedPullQueryTask extends MergingPartitionedQueryTask{

	private static final Logger logger = LoggerFactory.getLogger(PartitionedPullQueryTask.class);

	public PartitionedPullQueryTask(List<Selector> sel, Properties prop, long partitioningFactor, int subPoolSize, long timeoutSecs) {
		super(sel, prop, partitioningFactor, subPoolSize, timeoutSecs);
	}

	@Override
	protected void executeParallel(RangeBucket<Long> bucket) throws Exception {

		PullTaskBuilder tb = new PullQueryBuilder(super.sel, super.accumulator);

		logger.debug("Booting new pipe for bucket " + bucket + " with subsize " + super.subsize);

		PullPipelineBuilder ppb = new SimplePipelineBuilder(
				bucket.getLowerBound(),
				bucket.getUpperBound(),
				super.subsize, super.resultHandler, tb);

		PullPipeline pp = new PullPipeline(ppb, poolSize, super.timeoutSecs, BlockingMode.BLOCKING);

		pp.execute();
	}

}
