package org.sdrc.datum19.controller;

import java.util.List;

import org.sdrc.datum19.document.DataValue;
import org.sdrc.datum19.service.MongoAggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MongoAggregationController {

	@Autowired
	private MongoAggregationService mongoAggregationService;
	
	@GetMapping("/mongoAggregate")
	public String getMongoAggregatedData(@RequestParam("tp") Integer tp,@RequestParam("periodicity") String periodicity) {
		return mongoAggregationService.aggregate(tp,periodicity);
	}
	
	@GetMapping("/aggregatepercent")
	public List<DataValue> aggregatepercent(@RequestParam("periodicity") String periodicity, @RequestParam("type") String type){
		return mongoAggregationService.aggregateFinalIndicators(periodicity, type);
	}
}
