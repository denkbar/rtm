package org.rtm.pipeline;

import java.util.concurrent.Executors;

import org.rtm.pipeline.pull.PullPipeline;
import org.rtm.stream.Stream;

public class PipelineExecutionHelper {

	public static void executeAndsetListeners(PullPipeline pp, Stream<Long> stream) {
		Runnable waiter = new Runnable() {

			@Override
			public void run() {
				try {
					pp.execute();
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					stream.setComplete(true);
				}
			}
		};

		Executors.newSingleThreadExecutor().submit(waiter);
	}

}