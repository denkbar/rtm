package org.rtm.stream;

import java.util.HashMap;
import java.util.Map;

import org.rtm.dao.RTMMongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Wraps a regular HM which represents a simple datapoint (containing multiple metrics)
 * for a given time bucket (pointed by Stream's key)
 * for a given dimension (pointed by TimeValue's key)
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Dimension extends HashMap<String, Object>{
	private static final long serialVersionUID = 5989391368060961616L;
	private static final Logger logger = LoggerFactory.getLogger(RTMMongoClient.class);

	private LongAccumulationHelper helper;

	private String dimensionValue;

	public Dimension(String name){
		super();
		this.helper = new LongAccumulationHelper();
		this.setDimensionValue(name);
	}

	public LongAccumulationHelper getAccumulationHelper(){
		return helper;
	}

	public void copyAndFlush() {
		if(helper != null){
			helper.entrySet().stream().forEach(
							e -> {
								Map metrics = (Map)super.get(this.dimensionValue);
								if(metrics == null){
									super.put(this.dimensionValue, new HashMap<>());
									metrics = (Map)super.get(this.dimensionValue);
								}
								metrics.put(e.getKey(), e.getValue().longValue());
							});
		}
		else
			logger.error("Null helper");
		flush();
	}

	public void flush(){
		helper.clear();
	}

	/**
	 * @return the name
	 */
	public String getDimensionValue() {
		return dimensionValue;
	}

	/**
	 * @param name the name to set
	 */
	public void setDimensionValue(String value) {
		this.dimensionValue = value;
	}

}
