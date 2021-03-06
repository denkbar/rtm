/*******************************************************************************
 * (C) Copyright 2016 Dorian Cransac and Jerome Comte
 *
 * This file is part of rtm
 *
 * rtm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * rtm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with rtm.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.rtm.request;

import java.util.List;
import java.util.Properties;

import org.rtm.measurement.MeasurementStatistics;
import org.rtm.metrics.postprocessing.MetricsManager;
import org.rtm.pipeline.PullPipelineExecutor;
import org.rtm.pipeline.builders.pipeline.PullRunableBuilder;
import org.rtm.pipeline.builders.pipeline.RunableBuilder;
import org.rtm.pipeline.builders.task.PartitionedRangeTaskBuilder;
import org.rtm.pipeline.builders.task.RangeTaskBuilder;
import org.rtm.pipeline.commons.BlockingMode;
import org.rtm.pipeline.commons.PipelineExecutionHelper;
import org.rtm.request.aggregation.StreamResponseWrapper;
import org.rtm.rest.partitioner.PartitionerRequest;
import org.rtm.rest.partitioner.RefreshIdRequest;
import org.rtm.selection.Selector;
import org.rtm.stream.Stream;
import org.rtm.stream.StreamBroker;
import org.rtm.stream.StreamId;
import org.rtm.stream.result.ResultHandler;
import org.rtm.stream.result.StreamResultHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;


/**
 * @author doriancransac
 *
 */
@SuppressWarnings({"unchecked"})
public class PartitionerService extends AbstractMessageHandler{

	private StreamBroker streamBroker;

	//TODO tmp for PoC
	public PartitionerService(){
		this.streamBroker = new StreamBroker();
	}

	public PartitionerService(StreamBroker streamBroker){
		this.streamBroker = streamBroker;
	}

	public StreamId aggregate(List<Selector> sel, Properties prop, long subPartitioning, int subPoolSize, int timeoutSecs,
			long start, long end, long increment, long optimalSize) throws Exception{

		Stream<Long> stream = initStream(timeoutSecs, optimalSize, prop);
		ResultHandler<Long> rh = new StreamResultHandler(stream);

		/*Partitioning logic - how do we produce ranges*/
		RangeTaskBuilder tb = new PartitionedRangeTaskBuilder(sel, prop, subPartitioning, subPoolSize, timeoutSecs);

		/*Pull logic - produce runables which will do work while the partioner still has ranges*/
		RunableBuilder runableBuilder = new PullRunableBuilder(
				start, end, increment, rh, tb);

		// It's only useful to // at this level if we're looking to produce highly granular results,
		// which should almost never be the case
		PullPipelineExecutor pp = new PullPipelineExecutor(runableBuilder, /*poolSize*/ 1, timeoutSecs, BlockingMode.BLOCKING);

		PipelineExecutionHelper.executeAndsetListeners(pp, stream);

		streamBroker.registerStreamSession(stream);


		return stream.getId();
	}

	private Stream<Long> initStream(long timeout, Long optimalSize, Properties prop) {

		prop.setProperty(Stream.INTERVAL_SIZE_KEY, optimalSize.toString());

		Stream<Long> stream = new Stream<>(prop);
		stream.setTimeoutDurationSecs(timeout);

		return stream;
	}


	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		System.out.println("Grid Call succeeded");
		ObjectMapper om = new ObjectMapper();

		Object request = null;

		try {
			request = om.treeToValue(message.getPayload(), PartitionerRequest.class);
		}catch(Exception e) {}

		try {
			request = om.treeToValue(message.getPayload(), RefreshIdRequest.class);
		}catch(Exception e) {}

		if(request == null)
			throw new Exception("could not parse partitioner request");
		OutputMessageBuilder omb = new OutputMessageBuilder();

		if(request instanceof PartitionerRequest) {
			PartitionerRequest req = (PartitionerRequest) request;
			omb.setPayload(om.valueToTree(aggregate(req.getSel(), req.getProp(), req.getSubPartitioning(), req.getSubPoolSize(), req.getTimeoutSecs(), req.getStart(), req.getEnd(), req.getIncrement(), req.getOptimalSize())));
		}
		if(request instanceof RefreshIdRequest) {
			RefreshIdRequest req = (RefreshIdRequest) request;
			StreamResponseWrapper refreshResutStreamForId = refreshResutStreamForId(req.getStreamId());
			omb.setPayload(om.valueToTree(refreshResutStreamForId));
		}

		return omb.build();
	}

	@SuppressWarnings("rawtypes")
	public StreamResponseWrapper refreshResutStreamForId(StreamId streamId) {

		try {
			Stream s = this.streamBroker.getStreamAndFlagForRefresh(streamId);
			Stream result = null;
			if(s.isCompositeStream())
				result = s;
			else{
				result = new MetricsManager(s.getStreamProp()).handle(s);
				result.setComplete(s.isComplete());
			}
			return new StreamResponseWrapper(result, new MeasurementStatistics(s.getStreamProp()).getMetricList());
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
