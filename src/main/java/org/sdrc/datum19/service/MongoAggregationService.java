package org.sdrc.datum19.service;

import static org.springframework.data.mongodb.core.aggregation.ConditionalOperators.when;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
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
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.*;
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

/*
 * author : Biswabhusan Pradhan
 * email : biswabhusan@sdrc.co.in
 * 
 */

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
	
	Integer timePeriodId=null;
	List<DataValue> dataValueList;
	public String aggregate(Integer tp, String periodicity){
		dataValueList=new ArrayList<>();
		timePeriodId=tp;
		dataValueList=new ArrayList<>();
		List<Indicator> indicatorList = indicatorRepository.getIndicatorByPeriodicity(periodicity);
		indicatorList.stream().filter(indicator->!indicator.getIndicatorDataMap().get("collection").equals("dataValue")).forEach(indicator->{
			Class<?> clazz=null;
			try {
				clazz=Class.forName(String.valueOf(indicator.getIndicatorDataMap().get("collection")));
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
						String.valueOf(indicator.getIndicatorDataMap().get("indicatorName"))),clazz, Map.class).getMappedResults();
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
				List<Map> tableDataList=new ArrayList<>();
				switch (String.valueOf(indicator.getIndicatorDataMap().get("aggregationType"))) {
				case "number":
					tableDataList= mongoTemplate.aggregate(getTableAggregationResults(
							Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
							 String.valueOf(indicator.getIndicatorDataMap().get("area")),
							String.valueOf(indicator.getIndicatorDataMap().get("collection")),
							String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
							 String.valueOf(indicator.getIndicatorDataMap().get("parentColumn")),
							 String.valueOf(indicator.getIndicatorDataMap().get("indicatorName"))),clazz, Map.class).getMappedResults();
					break;
				case "count":
					tableDataList= mongoTemplate.aggregate(getTableCountResults(
							Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
							 String.valueOf(indicator.getIndicatorDataMap().get("area")),
							String.valueOf(indicator.getIndicatorDataMap().get("collection")),
							String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
							 String.valueOf(indicator.getIndicatorDataMap().get("parentColumn")),
							 String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")),
							 String.valueOf(indicator.getIndicatorDataMap().get("indicatorName"))),clazz, Map.class).getMappedResults();

				default:
					break;
				}
				
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
						String.valueOf(indicator.getIndicatorDataMap().get("indicatorName"))),clazz, Map.class).getMappedResults();
				
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
							String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
							String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")).split(":").length>1
							?String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")).split(":")[1]:""), clazz,Map.class).getMappedResults();
//					System.out.println("uniqueCountData :: "+uniqueCountData);
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
						String.valueOf(indicator.getIndicatorDataMap().get("area"))), clazz,Map.class).getMappedResults();
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
				case "gt":
				case "lt":
				Integer value=Integer.parseInt(String.valueOf(indicator.getIndicatorDataMap().get("typeDetailId")));
				List<Map> gteCountData=mongoTemplate.aggregate(getCount(
						Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")), 
						String.valueOf(indicator.getIndicatorDataMap().get("area")),
						String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
						value,String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule"))),clazz,Map.class).getMappedResults();
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
					List<Integer> valueList=new ArrayList<>();
							Arrays.asList(
									String.valueOf(indicator.getIndicatorDataMap().get("typeDetailId")).split("#"))
									.stream().forEach(i -> {
										if (!i.equals(""))
											valueList.add(Integer.parseInt(i));
									});
					List<Map> repeatCountData=mongoTemplate.aggregate(getRepeatCountQuery(
							Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")), 
							String.valueOf(indicator.getIndicatorDataMap().get("area")),
							String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
							valueList,String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule"))),clazz,Map.class).getMappedResults();
					repeatCountData.forEach(data->{
						DataValue datadoc=new DataValue();
						datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
						datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get(String.valueOf(indicator.getIndicatorDataMap().get("area")).split("\\.")[1]))));
						datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
						datadoc.setTp(tp);
						datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
						dataValueList.add(datadoc);
					});
					break;

				default:
					break;
				}
				
				break;
				
			case "area":
				String[] rules= String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")).split(";");
				Integer value1=Integer.parseInt(String.valueOf(indicator.getIndicatorDataMap().get("typeDetailId")));
				List<Map> areaCountData=mongoTemplate.aggregate(getAreaCount(
						String.valueOf(indicator.getIndicatorDataMap().get("area")),
						String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
						value1,rules),clazz,Map.class).getMappedResults();
				areaCountData.forEach(data->{
					System.out.println(data);
					if(!String.valueOf(data.get("_id")).equals("null")) {
					DataValue datadoc=new DataValue();
					datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
					datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
					datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
					datadoc.setTp(tp);
					datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
					dataValueList.add(datadoc);
					}
				});
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
//				System.out.println(areaList);
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
			String aggrule=String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule"));
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
				percentDataMap=mongoTemplate.aggregate(getPercentData(dependencies,numlist,denolist,aggrule),DataValue.class,DataValue.class).getMappedResults();
				percentDataMap.forEach(dv->{
					dv.setInid(inid);
					dv.set_case("percent");
					if (dv.getDenominator()==0) {
						dv.setDataValue(null);
					}
				});
				percentDataMapAll.addAll(percentDataMap);
			} catch (Exception e) {
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
	
	private TypedAggregation<DataValue> getPercentData(List<Integer> dep,List<Integer> num,List<Integer> deno, String rule){
		MatchOperation matchOperation = Aggregation.match(Criteria.where("inid").in(dep).and("tp").is(timePeriodId));
		GroupOperation groupOperation=null;
		ProjectionOperation projectionOperation=null;
		ProjectionOperation p1=null;
		ProjectionOperation p2=null;
		if(rule.equals("sub")) {
			groupOperation=Aggregation.group("areaId","tp").sum(when(where("inid").in(deno)).thenValueOf("$dataValue").otherwise(0)).as("denominator")
					.sum(Sum.sumOf(when(where("inid").is(num.get(0))).then("$dataValue").otherwise(0))).as("n1")
					.sum(when(where("inid").is(num.get(1))).then("$dataValue").otherwise(0)).as("n2");
			
			p1=Aggregation.project().and("areaId").as("areaId").and("tp").as("tp").and(Subtract.valueOf("n1").subtract("n2")).as("numerator").and("denominator").as("denominator");
			
			p2=Aggregation.project().and("areaId").as("areaId").and("tp").as("tp").and("numerator").as("numerator").and("denominator").as("denominator")
					.and(when(where("denominator").gt(0)).thenValueOf(Divide.valueOf(Multiply.valueOf("numerator")
					.multiplyBy(100)).divideBy("denominator")).otherwise(0)).as("dataValue");
			return Aggregation.newAggregation(DataValue.class,matchOperation,groupOperation,p1,p2);
		}else {
		groupOperation=Aggregation.group("areaId","tp").sum(when(where("inid").in(num)).thenValueOf("$dataValue").otherwise(0)).as("numerator")
				.sum(when(where("inid").in(deno)).thenValueOf("$dataValue").otherwise(0)).as("denominator");
		projectionOperation=Aggregation.project().and("_id.areaId").as("areaId")
				.and("_id.tp").as("tp").and("numerator").as("numerator").and("denominator").as("denominator")
				.andExclude("_id")
				.and(when(where("denominator").gt(0)).thenValueOf(Divide.valueOf(Multiply.valueOf("numerator")
				.multiplyBy(100)).divideBy("denominator")).otherwise(0)).as("dataValue");
		return Aggregation.newAggregation(DataValue.class,matchOperation,groupOperation,projectionOperation);
		}
	}
	
//	for count of 0 records
	public Aggregation getDropdownAggregationResults(Integer formId, String area, String collection, String path, List<Integer> tdlist,String name) {
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		ProjectionOperation projectionOperation1=Aggregation.project()
				.and(area).as("area")
				.and(Sum.sumOf(when(where("data."+path).in(tdlist)).then(1).otherwise(0))).as("projectedData");
		
		GroupOperation groupOperation= Aggregation.group("area").sum("projectedData").as("value");
		
		return Aggregation.newAggregation(matchOperation,projectionOperation,projectionOperation1,groupOperation);
	}
	
	public Aggregation getTableAggregationResults(Integer formId, String area, String collection, String path, String table,String name) {
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		UnwindOperation unwindOperation = Aggregation.unwind("data."+table);
		GroupOperation groupOperation= Aggregation.group(area).sum("data."+table+"."+path).as("value");
		return Aggregation.newAggregation(matchOperation,projectionOperation,unwindOperation,groupOperation);
	}
	
	private Aggregation getTableCountResults(Integer formId, String area, String collection, String path, String table,String _rule,String name) {
		// TODO Auto-generated method stub
		String[] rules= _rule.split(";");
		GroupOperation _groupOp=null;

		for (String rule: rules) {
			switch (rule.split("\\(")[0]) {
			case "and$eq":
				_groupOp=Aggregation.group(area).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.is(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;
			case "and$gt":
				_groupOp=Aggregation.group(area).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.gt(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;
			case "and$lt":
				_groupOp=Aggregation.group(area).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.lt(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;
			case "and$gte":
				_groupOp=Aggregation.group(area).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.gte(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;
			case "and$lte":
				_groupOp=Aggregation.group(area).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.lte(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;

			default:
				break;
			}
		    
		}
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId));
		ProjectionOperation _projectOp=Aggregation.project("data");
		UnwindOperation _unwindOp=Aggregation.unwind("data."+table);
		
		return Aggregation.newAggregation(Map.class, matchOperation,_projectOp, _unwindOp,_groupOp);
	}
	
	public Aggregation getNumericAggregationResults(Integer formId, String area, String collection, String path,String name) {
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation= Aggregation.group(area).sum("data."+path).as("value");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getUniqueCount(Integer formId, String area, String collection, String name,String childArea,String conditions) {
		
		List<String> condarr=new ArrayList<>();
		if(!conditions.isEmpty())
			condarr=Arrays.asList(conditions.split(","));
		Criteria criteria = Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId);
		if(!condarr.isEmpty()) {
		condarr.forEach(_cond->{
			criteria.andOperator(Criteria.where("data."+_cond.split("=")[0]).is(Integer.parseInt(_cond.split("=")[1])));
		});
		}
		
		MatchOperation matchOperation=Aggregation.match(criteria);
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area).addToSet("data."+childArea).as("childArea");
		UnwindOperation unwindOperation=Aggregation.unwind("childArea");
		GroupOperation groupOperation2=Aggregation.group("$_id").count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation,unwindOperation,groupOperation2);
	}
	
	public Aggregation getTotalVisitCount(Integer formId, String area) {
		MatchOperation matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area).count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getCount(Integer formId, String area, String path, Integer value, String rule) {
		MatchOperation matchOperation=null;
		switch (rule) {
		case "eq":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).is(value).and("timePeriod.timePeriodId").is(timePeriodId));
			break;
		case "lte":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).lte(value).and("timePeriod.timePeriodId").is(timePeriodId));
			break;
		case "gte":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).gte(value).and("timePeriod.timePeriodId").is(timePeriodId));
			break;
		case "gt":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).gt(value).and("timePeriod.timePeriodId").is(timePeriodId));
			break;
		case "lt":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).lt(value).and("timePeriod.timePeriodId").is(timePeriodId));
			break;

		default:
			break;
		}
		
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area).count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getRepeatCountQuery(Integer formId, String area, String path, List<Integer> valueList,
			String query) {

		MatchOperation matchOperation = null;
		ProjectionOperation projectionOperation = null;
		GroupOperation groupOperation = null;
		ProjectionOperation projectionOperation2 = null;
		GroupOperation groupOperation2 = null;

		ProjectionOperation projectionOperation3 = null;
		if (path.equals("")) {
			matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId));
			projectionOperation = Aggregation.project().and("data").as("data");
			groupOperation = Aggregation.group(query.split(":")[1], area).count().as("totalcount");
			projectionOperation2 = Aggregation.project(area.split("\\.")[1])
					.and(when(where("totalcount").gt(1)).then(1).otherwise(0)).as("repeatCount");
//				groupOperation2 = Aggregation.group(area.split("\\.")[1]).count().as("dataValue");

			projectionOperation3 = Aggregation.project().and(area.split("\\.")[1]).as(area.split("\\.")[1])
					.and(when(where("repeatCount").is(1)).then(Sum.sumOf("repeatCount")).otherwise(0)).as("dataValue");

		} else {
			matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("data." + path).in(valueList).and("timePeriod.timePeriodId").is(timePeriodId));
			projectionOperation = Aggregation.project().and("data").as("data");
			groupOperation = Aggregation.group(query.split(":")[1], "data." + path, area).count().as("totalcount");
			projectionOperation2 = Aggregation.project(path, area.split("\\.")[1])
					.and(when(where("totalcount").gt(1)).then(1).otherwise(0)).as("repeatCount");
//				groupOperation2 = Aggregation.group(path, area.split("\\.")[1]).count().as("dataValue");

			projectionOperation3 = Aggregation.project().and(area.split("\\.")[1]).as(area.split("\\.")[1])
					.and(when(where("repeatCount").is(1)).then(Sum.sumOf("repeatCount")).otherwise(0)).as("dataValue");

		}

		return Aggregation.newAggregation(matchOperation, projectionOperation, groupOperation, projectionOperation2,
				projectionOperation3);

	}
	
	public Aggregation getAreaCount(String area, String path,Integer value,String[] rules) {
		Criteria criteria = Criteria.where(path).is(value);

		List<Criteria> orCriterias = new ArrayList<Criteria>();
		List<Criteria> andCriterias=new ArrayList<Criteria>();
		

		for (String rule: rules) {
			switch (rule.split("\\(")[0]) {
			case "eq":
				criteria=criteria.and(rule.split("\\(")[1].split(":")[0]).is(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]));
				break;
			case "and$in" :
				List<Integer> andtd=new ArrayList<>();
				Arrays.asList(rule.split("\\[")[1].split("\\]")[0].split(",")).forEach(v->andtd.add(Integer.parseInt(v)));
				andCriterias.add(Criteria.where(rule.split("\\(")[1].split(":")[0]).in(andtd));
				break;
			case "or$in" :
				List<Integer> ortd=new ArrayList<>();
				Arrays.asList(rule.split("\\[")[1].split("\\]")[0].split(",")).forEach(v->ortd.add(Integer.parseInt(v)));
				orCriterias.add(Criteria.where(rule.split("\\(")[1].split(":")[0]).in(ortd));
				break;

			default:
				break;
			}
		    
		}
		if (!andCriterias.isEmpty()) {
			criteria=criteria.andOperator(andCriterias.toArray(new Criteria[andCriterias.size()]));
		}
		if(orCriterias.size()!=0)
			criteria = criteria.orOperator(orCriterias.toArray(new Criteria[orCriterias.size()]));
		MatchOperation matchOperation = Aggregation.match(criteria);
		GroupOperation groupOperation=Aggregation.group(area).count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,groupOperation);
	}
}
