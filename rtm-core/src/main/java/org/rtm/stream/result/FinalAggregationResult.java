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
package org.rtm.stream.result;

import java.util.Map;

import org.rtm.stream.Dimension;

/**
 * @author doriancransac
 *
 */

public class FinalAggregationResult<T> implements AggregationResult<T> {

	private Map<String, Dimension> dimensionsMap;
	private Identifier<T> streamPayloadIdentifier;
	
	public FinalAggregationResult(){
	}
	
	public FinalAggregationResult(Identifier<T> id){
		this.streamPayloadIdentifier = id;
	}
	
	@Override
	public Identifier<T> getStreamPayloadIdentifier() {
		return this.streamPayloadIdentifier;
	}

	@Override
	public Map<String, Dimension> getDimensionsMap() {
		return dimensionsMap;
	}
	
	@Override
	public void setDimensionsMap(Map<String, Dimension> map) {
		this.dimensionsMap = map;
		
	}

	@Override
	public void setStreamPayloadIdentifier(Identifier<T> streamPayloadIdentifier) {
		this.streamPayloadIdentifier = streamPayloadIdentifier;
	}


}
