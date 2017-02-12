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
package org.rtm.rest.aggregation;

import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.rtm.commons.Configuration;
import org.rtm.commons.MeasurementConstants;
import org.rtm.core.AggregationService;
import org.rtm.core.ComplexServiceResponse;
import org.rtm.core.ComplexServiceResponse.Status;
import org.rtm.dao.RTMMongoClient;
import org.rtm.exception.NoDataException;
import org.rtm.exception.ValidationException;

@Singleton
@Path("/aggregate")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AggregationServlet {

	@POST
	@Path("/timebased")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAggregatesPost(AggInput input) {
		
		Configuration conf = Configuration.getInstance();
		
		final String serviceDomain = conf.getProperty("aggregateService.domain");
		AggOutput response;
		int skip =0;
		/* The backend skip/limit strategy does not really apply here as the data can be handle properly through aggregation */
		/* But a limit should still be set later to prevent queries that will last hours on end */
		int limit = conf.getPropertyAsInteger("aggregateService.maxMeasurements");
		/**/
		
		try{
			new AggregateValidator().validate(input);
		}catch (ValidationException e) {
			e.printStackTrace();
			return Response.status(500).entity("Exception occured : " + e.getMessage()).build();
		}
		try{
			String separator = conf.getProperty("domainSeparator");
			Iterable it = RTMMongoClient.getInstance().selectMeasurements(input.getSelectors(), skip, limit, MeasurementConstants.BEGIN_KEY);
			AggregationService as = new AggregationService();

			Map<String,String> serviceParams = input.getServiceParams();
			ComplexServiceResponse inconsistent = as.buildAggregatesForTimeInconsistent(
					serviceParams.get(serviceDomain + separator +MeasurementConstants.SESSION_KEY), 
					it,
					Long.parseLong(serviceParams.get(serviceDomain + separator +Configuration.GRANULARITY_KEY)),
					//Configuration.TEXT_PREFIX +Configuration.SPLITTER+serviceParams.get(serviceDomain + dSep +Configuration.GROUPBY_KEY),
					serviceParams.get(serviceDomain + separator +Configuration.GROUPBY_KEY),
					MeasurementConstants.BEGIN_KEY, MeasurementConstants.END_KEY, MeasurementConstants.VALUE_KEY, MeasurementConstants.SESSION_KEY
					);

			ComplexServiceResponse consistent = AggregationService.makeDataConsistent(inconsistent, MeasurementConstants.SESSION_KEY, MeasurementConstants.BEGIN_KEY, MeasurementConstants.END_KEY, MeasurementConstants.NAME_KEY);

			response = AggregationService.convertForJson(consistent.getPayload());
			if(inconsistent.getReturnStatus() == Status.WARNING)
				response.setWarning(inconsistent.getMessage());

			return Response.ok(response).build();
		}catch(NoDataException e){
			response = new AggOutput();
			response.setShallowPayload();
			response.setWarning("No data found with the given criteria.");
			return Response.ok(response).build();
		}
		catch(Exception e){
			e.printStackTrace();
			return Response.status(500).entity("Exception occured : " + e.getMessage()).build();
		}
	}
}