package org.sdrc.datum19.service;

import static org.springframework.data.mongodb.core.aggregation.ConditionalOperators.when;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sdrc.datum19.document.AllChecklistFormData;
import org.sdrc.datum19.document.DataValue;
import org.sdrc.datum19.document.Indicator;
import org.sdrc.datum19.repository.DataDomainRepository;
import org.sdrc.datum19.repository.IndicatorRepository;
import org.sdrc.datum19.util.AreaMapObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators.Sum;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Divide;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Multiply;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;


@Service
public class MongoAggregationService {
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private IndicatorRepository indicatorRepository;
	
	@Autowired
	private AreaService areaService;
	
	@Autowired
	private ConfigurableEnvironment configurableEnvironment;
	
	@Autowired
	private DataDomainRepository dataDomainRepository;
	
	List<Map> submissionList;
	List<DataValue> dataValueList;
	Integer timePeriodId=null;
	public String aggregate(Integer tp, String periodicity){
		timePeriodId=tp;
		submissionList=new ArrayList<>();
		dataValueList=new ArrayList<>();
		List<Indicator> indicatorList = indicatorRepository.getIndicatorByPeriodicity(periodicity);
		indicatorList.forEach(indicator->{
			switch (String.valueOf(indicator.getIndicatorDataMap().get("parentType"))) {
			case "dropdown":
				List<Integer> tdlist=new ArrayList<>();
				Arrays.asList(String.valueOf(indicator.getIndicatorDataMap().get("typeDetailId")).split("#")).stream().forEach(i->{tdlist.add(Integer.parseInt(i));});
				List<Map> dataList= mongoTemplate.aggregate(getDropdownAggregationResults(
						Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
						 String.valueOf(indicator.getIndicatorDataMap().get("area")),
						String.valueOf(indicator.getIndicatorDataMap().get("collection")),
						String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
						tdlist,
						String.valueOf(indicator.getIndicatorDataMap().get("indicatorName"))),AllChecklistFormData.class, Map.class).getMappedResults();
				dataList.forEach(data->{
					DataValue datadoc=new DataValue();
					datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
					datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
					datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("value"))));
					datadoc.setTp(tp);
					datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
					dataValueList.add(datadoc);
				});
				break;
				
			case "table":
				List<Map> tableDataList= mongoTemplate.aggregate(getTableAggregationResults(
						Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
						 String.valueOf(indicator.getIndicatorDataMap().get("area")),
						String.valueOf(indicator.getIndicatorDataMap().get("collection")),
						String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
						 String.valueOf(indicator.getIndicatorDataMap().get("parentColumn")),
						 String.valueOf(indicator.getIndicatorDataMap().get("indicatorName"))),AllChecklistFormData.class, Map.class).getMappedResults();
				tableDataList.forEach(data->{
					DataValue datadoc=new DataValue();
					datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
					datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
					datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("value"))));
					datadoc.setTp(tp);
					datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
					dataValueList.add(datadoc);
				});
				
				break;

			case "numeric":
				List<Map> numericDataList= mongoTemplate.aggregate(getNumericAggregationResults(
						Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
						 String.valueOf(indicator.getIndicatorDataMap().get("area")),
						String.valueOf(indicator.getIndicatorDataMap().get("collection")),
						String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
						String.valueOf(indicator.getIndicatorDataMap().get("indicatorName"))),AllChecklistFormData.class, Map.class).getMappedResults();
				
				numericDataList.forEach(data->{
					DataValue datadoc=new DataValue();
					datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
					datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
					datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("value"))));
					datadoc.setTp(tp);
					datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
					dataValueList.add(datadoc);
				});
				break;
				
			case "form":
				switch (String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")).split(":")[0]) {
				case "unique":
					List<Map> uniqueCountData=mongoTemplate.aggregate(getUniqueCount(
							Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")), 
							String.valueOf(indicator.getIndicatorDataMap().get("area")), 
							String.valueOf(indicator.getIndicatorDataMap().get("collection")), 
							String.valueOf(indicator.getIndicatorDataMap().get("indicatorName")),
							String.valueOf(indicator.getIndicatorDataMap().get("numerator"))), AllChecklistFormData.class,Map.class).getMappedResults();
					System.out.println("uniqueCountData :: "+uniqueCountData);
					uniqueCountData.forEach(data->{
						DataValue datadoc=new DataValue();
						datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
						datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
						datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
						datadoc.setTp(tp);
						datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
						dataValueList.add(datadoc);
					});
					break;
					
				case "total":
				List<Map> visitCountData=mongoTemplate.aggregate(getTotalVisitCount(
						Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")), 
						String.valueOf(indicator.getIndicatorDataMap().get("area"))), AllChecklistFormData.class,Map.class).getMappedResults();
				visitCountData.forEach(data->{
					DataValue datadoc=new DataValue();
					datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
					datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
					datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
					datadoc.setTp(tp);
					datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
					dataValueList.add(datadoc);
				});
					break;
					
				case "gte":
				case "lte":
				case "eq":
				Integer value=Integer.parseInt(String.valueOf(indicator.getIndicatorDataMap().get("typeDetailId")));
				List<Map> gteCountData=mongoTemplate.aggregate(getGreaterThanCount(
						Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")), 
						String.valueOf(indicator.getIndicatorDataMap().get("area")),
						String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
						value,String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule"))),AllChecklistFormData.class,Map.class).getMappedResults();
				gteCountData.forEach(data->{
					DataValue datadoc=new DataValue();
					datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
					datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
					datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
					datadoc.setTp(tp);
					datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
					dataValueList.add(datadoc);
				});
					break;
				case "repeatCount":
					Integer value1=Integer.parseInt(String.valueOf(indicator.getIndicatorDataMap().get("typeDetailId")));
					List<Integer> valueList=new ArrayList<>();
					Arrays.asList(String.valueOf(indicator.getIndicatorDataMap().get("typeDetailId")).split("#")).stream().forEach(i->{valueList.add(Integer.parseInt(i));});
					List<Map> repeatCountData=mongoTemplate.aggregate(getRepeatCountQuery(
							Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")), 
							String.valueOf(indicator.getIndicatorDataMap().get("area")),
							String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
							valueList,String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule"))),AllChecklistFormData.class,Map.class).getMappedResults();
					repeatCountData.forEach(data->{
						DataValue datadoc=new DataValue();
						datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
						datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get(String.valueOf(indicator.getIndicatorDataMap().get("area")).split("\\.")[1]))));
						datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
						datadoc.setTp(tp);
						datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
						dataValueList.add(datadoc);
						System.out.println(data);
					});
					break;

				default:
					break;
				}
				
				break;
				
			default:
				break;
			}
			
		});
		dataDomainRepository.saveAll(dataValueList);
		try {
			int areaLevel = Integer.parseInt(configurableEnvironment.getProperty("spark.aggregation.arealevel"));
			for (int i = areaLevel; i >0; i--) {
				AreaMapObject amb = areaService.getAreaForAggregation(i);
				Map<Integer, Integer> areaMap= amb.getAreaMap();
				List<Integer> areaList=amb.getAreaList();
				System.out.println(areaList);
			List<Map> areaDataMap=mongoTemplate.aggregate(aggregateAreaTree(tp,areaList),DataValue.class,Map.class).getMappedResults();
			List<DataValue> datalist2=new ArrayList<>();
			areaDataMap.forEach(data->{
				DataValue datavalue=new DataValue();
				datavalue.setInid(Integer.valueOf(String.valueOf(data.get("inid"))));
				datavalue.setAreaId(Integer.valueOf(String.valueOf(data.get("parentAreaId"))));
				datavalue.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
				datavalue.setTp(Integer.valueOf(String.valueOf(data.get("tp"))));
				datavalue.set_case(String.valueOf(data.get("_case")));
				
				datalist2.add(datavalue);
			});
			dataDomainRepository.saveAll(datalist2);
			}
			aggregateFinalIndicators(periodicity,"indicator");
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("error encountered :: ");
			e.printStackTrace();
		}
		return "aggregation complete";
	}
	List<DataValue> percentDataMap=new ArrayList<>();
	List<DataValue> percentDataMapAll=new ArrayList<>();
	public List<DataValue> aggregateFinalIndicators(String periodicity, String indicatorType) {
		List<Indicator> indicatorList = indicatorRepository.getPercentageIndicators(periodicity,indicatorType);
		indicatorList.forEach(indicator->{
			List<Integer> dependencies=new ArrayList<>();
			List<Integer> numlist=new ArrayList<>();
			String[] numerators=String.valueOf(indicator.getIndicatorDataMap().get("numerator")).split(",");
			Integer inid=Integer.parseInt(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid")));
			for (int i = 0; i < numerators.length; i++) {
				numlist.add(Integer.parseInt(numerators[i]));
				dependencies.add(Integer.parseInt(numerators[i]));
			}
			List<Integer> denolist=new ArrayList<>();
			String[] denominators=String.valueOf(indicator.getIndicatorDataMap().get("denominator")).split(",");
			for (int i = 0; i < denominators.length; i++) {
				denolist.add(Integer.parseInt(denominators[i]));
				dependencies.add(Integer.parseInt(denominators[i]));
			}
			try {
				percentDataMap=mongoTemplate.aggregate(getPercentData(dependencies,numlist,denolist),DataValue.class,DataValue.class).getMappedResults();
				percentDataMap.forEach(dv->{
					dv.setInid(inid);
//					percentDataMapAll.add(dv);
				});
				percentDataMapAll.addAll(percentDataMap);
				System.out.println(percentDataMap);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		});
		dataDomainRepository.saveAll(percentDataMapAll);
		return percentDataMapAll;
	}
	
	
	private TypedAggregation<DataValue> aggregateAreaTree(Integer tp, List<Integer> areaList) {
		
		MatchOperation matchOperation=Aggregation.match(Criteria.where("tp").is(tp).and("areaId").in(areaList));
		LookupOperation lookupOperation=Aggregation.lookup("area", "areaId", "areaId", "parent");
		GroupOperation groupOperation=Aggregation.group("parent.parentAreaId","inid","tp","_case").sum("dataValue").as("dataValue");
		UnwindOperation unwindOperation=Aggregation.unwind("$_id.parentAreaId");
		
		return Aggregation.newAggregation(DataValue.class,matchOperation,lookupOperation,groupOperation,unwindOperation);
	}
	
	private TypedAggregation<DataValue> getPercentData(List<Integer> dep,List<Integer> num,List<Integer> deno){
		MatchOperation matchOperation = Aggregation.match(Criteria.where("inid").in(dep));
		GroupOperation groupOperation=Aggregation.group("areaId","tp").sum(when(where("inid").in(num)).thenValueOf("$dataValue").otherwise(0)).as("numerator")
				.sum(when(where("inid").in(deno)).thenValueOf("$dataValue").otherwise(0)).as("denominator");
		ProjectionOperation projectionOperation=Aggregation.project().and("_id.areaId").as("areaId")
				.and("_id.tp").as("tp").and("numerator").as("numerator").and("denominator").as("denominator")
				.andExclude("_id")
				.and(when(where("denominator").gt(0)).thenValueOf(Divide.valueOf(Multiply.valueOf("numerator")
				.multiplyBy(100)).divideBy("denominator")).otherwise(0)).as("dataValue");
		return Aggregation.newAggregation(DataValue.class,matchOperation,groupOperation,projectionOperation);
	}
	
	
	
	public Aggregation getDropdownAggregationResults(Integer formId, String area, String collection, String path, List<Integer> tdlist,String name) {
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).in(tdlist));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation= Aggregation.group(area).count().as("value");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getTableAggregationResults(Integer formId, String area, String collection, String path, String table,String name) {
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		UnwindOperation unwindOperation = Aggregation.unwind("data."+table);
		GroupOperation groupOperation= Aggregation.group(area).sum("data."+table+"."+path).as("value");
		return Aggregation.newAggregation(matchOperation,projectionOperation,unwindOperation,groupOperation);
	}
	
	public Aggregation getNumericAggregationResults(Integer formId, String area, String collection, String path,String name) {
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation= Aggregation.group(area).sum("data."+path).as("value");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getUniqueCount(Integer formId, String area, String collection, String name,String childArea) {
		MatchOperation matchOperation=Aggregation.match(Criteria.where("formId").is(formId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area).addToSet("data."+childArea).as("childArea");
		UnwindOperation unwindOperation=Aggregation.unwind("childArea");
		GroupOperation groupOperation2=Aggregation.group("$_id").count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation,unwindOperation,groupOperation2);
	}
	
	public Aggregation getTotalVisitCount(Integer formId, String area) {
		MatchOperation matchOperation=Aggregation.match(Criteria.where("formId").is(formId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area).count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getGreaterThanCount(Integer formId, String area, String path, Integer value, String rule) {
		MatchOperation matchOperation=null;
		switch (rule) {
		case "eq":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).is(value));
			break;
		case "lte":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).lte(value));
			break;
		case "gte":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).gte(value));
			break;
		case "gt":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).gt(value));
			break;
		case "lt":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).lt(value));
			break;

		default:
			break;
		}
		
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area).count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getRepeatCountQuery(Integer formId, String area, String path, List<Integer> valueList,String query) {
		MatchOperation matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).in(valueList).and("timePeriod.timePeriodId").is(timePeriodId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(query.split(":")[1],"data."+path,area).count().as("totalcount");
		ProjectionOperation projectionOperation2=Aggregation.project(path,area.split("\\.")[1]).and(when(where("totalcount").gt(1))
				.thenValueOf(Sum.sumOf("totalcount")).otherwise(0)).as("repeatCount");
		GroupOperation groupOperation2=Aggregation.group(path,area.split("\\.")[1]).count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation,projectionOperation2,groupOperation2);
	}
}
