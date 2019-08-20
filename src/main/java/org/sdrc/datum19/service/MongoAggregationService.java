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
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
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
	
	//consider those for aggregation where valid = true and latest = true
	public String aggregate(Integer tp, String periodicity){
		dataValueList=new ArrayList<>();
		timePeriodId=tp;
		dataValueList=new ArrayList<>();
		
		//added later -- remove before production deploy
		dataDomainRepository.deleteByTp(timePeriodId);
		
		List<Indicator> indicatorList = indicatorRepository.getIndicatorByPeriodicity(periodicity);
		indicatorList.stream().filter(indicator->!indicator.getIndicatorDataMap().get("collection").equals("dataValue")).forEach(indicator->{
			Class<?> clazz=null;
			
			String[] aggAreaArray = String.valueOf(indicator.getIndicatorDataMap().get("area")).split("#");
			try {
				clazz=Class.forName(String.valueOf(indicator.getIndicatorDataMap().get("collection")));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			for (String aggArea : aggAreaArray) {
				
				switch (String.valueOf(indicator.getIndicatorDataMap().get("parentType"))) {
				case "dropdown":
					List<Integer> tdlist=new ArrayList<>();
					Arrays.asList(String.valueOf(indicator.getIndicatorDataMap().get("typeDetailId")).split("#")).stream().forEach(i->{tdlist.add(Integer.parseInt(i));});
//					System.out.println(indicator.getIndicatorDataMap().get("indicatorNid"));
					List<Map> dataList= mongoTemplate.aggregate(getDropdownAggregationResults(
							Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
	//						 String.valueOf(indicator.getIndicatorDataMap().get("area")),
							aggArea.split("!")[0],
							String.valueOf(indicator.getIndicatorDataMap().get("collection")),
							String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
							tdlist,
							String.valueOf(indicator.getIndicatorDataMap().get("indicatorName")),
							indicator.getIndicatorDataMap().get("aggregationRule")!=null ?
									String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")) : null),clazz, Map.class).getMappedResults();
					dataList.forEach(data->{
						DataValue datadoc=new DataValue();
						datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
//						datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
						
						if(data.get("_id")!=null) {
							datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
						}
						
						int c = 0;
						for(String areaType : aggArea.split("!")[0].split(",")) {
							
							areaType = areaType.replace("data.", "");
							if(c==0) {
								if(data.get(areaType)!=null) 
									datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get(areaType))));
							}else {
								//change this to get key directly when datavalue will be map<steing,object>
								if(data.get("f1FacilityType")!=null) {
									datadoc.setF1FacilityType(Integer.valueOf(String.valueOf(data.get("f1FacilityType"))));
								}
								
								if(data.get("f1FacilityLevel")!=null) {
									datadoc.setF1FacilityLevel(Integer.valueOf(String.valueOf(data.get("f1FacilityLevel"))));
								}
							}
							c++;
						}
						
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
								aggArea.split("!")[0],
//								 String.valueOf(indicator.getIndicatorDataMap().get("area")),
								String.valueOf(indicator.getIndicatorDataMap().get("collection")),
								String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
								 String.valueOf(indicator.getIndicatorDataMap().get("parentColumn")),
								 String.valueOf(indicator.getIndicatorDataMap().get("indicatorName"))),clazz, Map.class).getMappedResults();
						break;
					case "count":
						tableDataList= mongoTemplate.aggregate(getTableCountResults(
								Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
								aggArea.split("!")[0],
//								 String.valueOf(indicator.getIndicatorDataMap().get("area")),
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
//						datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
						
						if(data.get("_id")!=null) {
							datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
						}
						
						int c = 0;
						for(String areaType : aggArea.split("!")[0].split(",")) {
							
							areaType = areaType.replace("data.", "");
							if(c==0) {
								if(data.get(areaType)!=null) 
									datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get(areaType))));
							}else {
								//change this to get key directly when datavalue will be map<steing,object>
								if(data.get("f1FacilityType")!=null) {
									datadoc.setF1FacilityType(Integer.valueOf(String.valueOf(data.get("f1FacilityType"))));
								}
								
								if(data.get("f1FacilityLevel")!=null) {
									datadoc.setF1FacilityLevel(Integer.valueOf(String.valueOf(data.get("f1FacilityLevel"))));
								}
							}
							c++;
						}
						
						datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("value"))));
						datadoc.setTp(tp);
						datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
						dataValueList.add(datadoc);
					});
					
					break;
	
				case "numeric": //data.f1q_district#data.f1q_district,data.f1FacilityType#data.f1q_district,data.f1FacilityLevel#data.f1q_district,data.f1FacilityType,data.f1FacilityLevel
					
	//				for (String aggArea : aggAreaArray) {
						List<Map> numericDataList= mongoTemplate.aggregate(getNumericAggregationResults(
								Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
								aggArea.split("!")[0],
								String.valueOf(indicator.getIndicatorDataMap().get("collection")),
								String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
								String.valueOf(indicator.getIndicatorDataMap().get("indicatorName")),
								String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule"))),clazz, Map.class).getMappedResults();
						
						numericDataList.forEach(data->{
							DataValue datadoc=new DataValue();
							datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
							
							
							if(data.get("_id")!=null) {
								datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
							}
							
							int c = 0;
							for(String areaType : aggArea.split("!")[0].split(",")) {
								
								areaType = areaType.replace("data.", "");
								if(c==0) {
									if(data.get(areaType)!=null) 
										datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get(areaType))));
								}else {
									//change this to get key directly when datavalue will be map<steing,object>
									if(data.get("f1FacilityType")!=null) {
										datadoc.setF1FacilityType(Integer.valueOf(String.valueOf(data.get("f1FacilityType"))));
									}
									
									if(data.get("f1FacilityLevel")!=null) {
										datadoc.setF1FacilityLevel(Integer.valueOf(String.valueOf(data.get("f1FacilityLevel"))));
									}
								}
								c++;
							}
							
//							if(data.get("f1q_district")!=null) {
//								datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("f1q_district"))));
//							}
//							
//							if(data.get("f1FacilityType")!=null) {
//								datadoc.setF1FacilityType(Integer.valueOf(String.valueOf(data.get("f1FacilityType"))));
//							}
//							
//							if(data.get("f1FacilityLevel")!=null) {
//								datadoc.setF1FacilityLevel(Integer.valueOf(String.valueOf(data.get("f1FacilityLevel"))));
//							}
							
							datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("value"))));
							datadoc.setTp(tp);
							datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
							dataValueList.add(datadoc);
						});
	//				}
	//				List<Map> numericDataList= mongoTemplate.aggregate(getNumericAggregationResults(
	//						Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
	//						 String.valueOf(indicator.getIndicatorDataMap().get("area")),
	//						String.valueOf(indicator.getIndicatorDataMap().get("collection")),
	//						String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
	//						String.valueOf(indicator.getIndicatorDataMap().get("indicatorName")),
	//						String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule"))),clazz, Map.class).getMappedResults();
					
	//				numericDataList.forEach(data->{
	//					DataValue datadoc=new DataValue();
	//					datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
	//					datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
	//					datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("value"))));
	//					datadoc.setTp(tp);
	//					datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
	//					dataValueList.add(datadoc);
	//				});
					break;
					
				case "form":
					switch (String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")).split(":")[0]) {
					case "unique":
						List<Map> uniqueCountData=mongoTemplate.aggregate(getUniqueCount(
								Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")),
								aggArea.split("!")[0],
//								String.valueOf(indicator.getIndicatorDataMap().get("area")), 
								String.valueOf(indicator.getIndicatorDataMap().get("collection")), 
								String.valueOf(indicator.getIndicatorDataMap().get("indicatorName")),
								String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
								String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")).split(":").length>1
								?String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule")).split(":")[1]:""), clazz,Map.class).getMappedResults();
	//					System.out.println("uniqueCountData :: "+uniqueCountData);
						uniqueCountData.forEach(data->{
							DataValue datadoc=new DataValue();
							datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
							
							if(data.get("_id")!=null) {
								datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
							}
							
							int c = 0;
							for(String areaType : aggArea.split("!")[0].split(",")) {
								
								areaType = areaType.replace("data.", "");
								if(c==0) {
									if(data.get(areaType)!=null) 
										datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get(areaType))));
								}else {
									//change this to get key directly when datavalue will be map<steing,object>
									if(data.get("f1FacilityType")!=null) {
										datadoc.setF1FacilityType(Integer.valueOf(String.valueOf(data.get("f1FacilityType"))));
									}
									
									if(data.get("f1FacilityLevel")!=null) {
										datadoc.setF1FacilityLevel(Integer.valueOf(String.valueOf(data.get("f1FacilityLevel"))));
									}
								}
								c++;
							}
							
//							datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
							datadoc.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
							datadoc.setTp(tp);
							datadoc.set_case(String.valueOf(indicator.getIndicatorDataMap().get("aggregationType")));
							dataValueList.add(datadoc);
						});
						break;
						
					case "total":
					List<Map> visitCountData=mongoTemplate.aggregate(getTotalVisitCount(
							Integer.valueOf((String) indicator.getIndicatorDataMap().get("formId")), 
							aggArea.split("!")[0]
//							String.valueOf(indicator.getIndicatorDataMap().get("area"))
							), clazz,Map.class).getMappedResults();
					visitCountData.forEach(data->{
						DataValue datadoc=new DataValue();
						datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
						
						if(data.get("_id")!=null) {
							datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
						}
						
						int c = 0;
						for(String areaType : aggArea.split("!")[0].split(",")) {
							
							
							areaType = areaType.replace("data.", "");
							if(c==0) {
								if(data.get(areaType)!=null) 
									datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get(areaType))));
							}else {
								//change this to get key directly when datavalue will be map<steing,object>
								if(data.get("f1FacilityType")!=null) {
									datadoc.setF1FacilityType(Integer.valueOf(String.valueOf(data.get("f1FacilityType"))));
								}
								
								if(data.get("f1FacilityLevel")!=null) {
									datadoc.setF1FacilityLevel(Integer.valueOf(String.valueOf(data.get("f1FacilityLevel"))));
								}
							}
							c++;
						}
//						datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
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
							aggArea.split("!")[0],
//							String.valueOf(indicator.getIndicatorDataMap().get("area")),
							String.valueOf(indicator.getIndicatorDataMap().get("numerator")),
							value,String.valueOf(indicator.getIndicatorDataMap().get("aggregationRule"))),clazz,Map.class).getMappedResults();
					gteCountData.forEach(data->{
						DataValue datadoc=new DataValue();
						datadoc.setInid(Integer.valueOf(String.valueOf(indicator.getIndicatorDataMap().get("indicatorNid"))));
						
						if(data.get("_id")!=null) {
							datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
						}
						
						int c = 0;
						for(String areaType : aggArea.split("!")[0].split(",")) {
							
							areaType = areaType.replace("data.", "");
							if(c==0) {
								if(data.get(areaType)!=null) 
									datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get(areaType))));
							}else {
								//change this to get key directly when datavalue will be map<steing,object>
								if(data.get("f1FacilityType")!=null) {
									datadoc.setF1FacilityType(Integer.valueOf(String.valueOf(data.get("f1FacilityType"))));
								}
								
								if(data.get("f1FacilityLevel")!=null) {
									datadoc.setF1FacilityLevel(Integer.valueOf(String.valueOf(data.get("f1FacilityLevel"))));
								}
							}
							c++;
						}
						
//						datadoc.setAreaId(Integer.valueOf(String.valueOf(data.get("_id"))));
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
			}
		});
		dataDomainRepository.saveAll(dataValueList);
		try {
			int areaLevel = Integer.parseInt(configurableEnvironment.getProperty("spark.aggregation.arealevel"));
			for (int i = areaLevel; i >0; i--) {
				AreaMapObject amb = areaService.getAreaForAggregation(i);
//				Map<Integer, Integer> areaMap= amb.getAreaMap();
				List<Integer> areaList=amb.getAreaList();
//				System.out.println(areaList);
				
				String[] excludedFields = "f1q_district!f1FacilityType,f1FacilityLevel#f1q_district,f1FacilityType!f1FacilityLevel#f1q_district,f1FacilityLevel!f1FacilityType#f1q_district,f1FacilityType,f1FacilityLevel"
						.split("#");

				for (String excludedField : excludedFields) {
					List<Map> areaDataMap=mongoTemplate.aggregate(aggregateAreaTree(tp,areaList,excludedField),DataValue.class,Map.class).getMappedResults();
					List<DataValue> datalist2=new ArrayList<>();
					areaDataMap.forEach(data->{
						DataValue datavalue=new DataValue();
						datavalue.setInid(Integer.valueOf(String.valueOf(data.get("inid"))));
						datavalue.setAreaId(Integer.valueOf(String.valueOf(data.get("parentAreaId"))));
						datavalue.setDataValue(Double.valueOf(String.valueOf(data.get("dataValue"))));
						datavalue.setTp(Integer.valueOf(String.valueOf(data.get("tp"))));
						datavalue.set_case(String.valueOf(data.get("_case")));
						
						
						if(data.get("f1FacilityType")!=null) {
							datavalue.setF1FacilityType(Integer.valueOf(String.valueOf(data.get("f1FacilityType"))));
						}
						
						if(data.get("f1FacilityLevel")!=null) {
							datavalue.setF1FacilityLevel(Integer.valueOf(String.valueOf(data.get("f1FacilityLevel"))));
						}
						
						
						datalist2.add(datavalue);
					});
					dataDomainRepository.saveAll(datalist2);
					}
				}
				
			
			aggregateFinalIndicators(periodicity,"indicator");
			
			//after aggregating update aggregate true in submissions-----------
			
			Query query = new Query();
			query.addCriteria(Criteria.where("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
			Update update = new Update();
			update.set("isAggregated",true);
			mongoTemplate.updateMulti(query, update, AllChecklistFormData.class);
			
			//end----------------------
		} catch (Exception e) {
			System.out.println("error encountered :: ");
			e.printStackTrace();
		}
		return "aggregation complete";
	}
	
	List<DataValue> percentDataMap=null;
	List<DataValue> percentDataMapAll=null;
	public List<DataValue> aggregateFinalIndicators(String periodicity, String indicatorType) {
		percentDataMap=new ArrayList<>();
		percentDataMapAll=new ArrayList<>();
		List<Indicator> indicatorList = indicatorRepository.getPercentageIndicators(periodicity,indicatorType);
		indicatorList.forEach(indicator->{
			
//			System.out.println(indicator.get_id());
			List<Integer> dependencies=new ArrayList<>();
			List<Integer> numlist=new ArrayList<>();
			
			String[] excludedFields = "f1q_district!f1FacilityType,f1FacilityLevel#f1q_district,f1FacilityType!f1FacilityLevel#f1q_district,f1FacilityLevel!f1FacilityType#f1q_district,f1FacilityType,f1FacilityLevel"
					.split("#");

//			String[] excludedFields = String.valueOf(indicator.getIndicatorDataMap().get("area")).split("#");
			
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
				switch (String.valueOf(indicator.getIndicatorDataMap().get("aggregationType"))) {
				case "percent":
					
					for (String excludedField : excludedFields) {
						percentDataMap=mongoTemplate.aggregate(getPercentData(dependencies,numlist,denolist,aggrule,excludedField),DataValue.class,DataValue.class).getMappedResults();
						percentDataMap.forEach(dv->{
							dv.setInid(inid);
							dv.set_case("percent");
							
							if (dv.getDenominator()==0) {
								dv.setDataValue(null);
							}
						});
						percentDataMapAll.addAll(percentDataMap);
					}
					
					break;
					
				case "avg":
					
					for (String excludedField : excludedFields) {
						percentDataMap=mongoTemplate.aggregate(getAvgData(dependencies,numlist,denolist,aggrule,excludedField),DataValue.class,DataValue.class).getMappedResults();
						percentDataMap.forEach(dv->{
							dv.setInid(inid);
							dv.set_case("avg");
							if (dv.getDenominator()==0) {
								dv.setDataValue(null);
							}
						});
						percentDataMapAll.addAll(percentDataMap);
					}
					
					break;

				default:
					break;
				}
					
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		dataDomainRepository.saveAll(percentDataMapAll);
		return percentDataMapAll;
	}
	
	
	private TypedAggregation getAvgData(List<Integer> dependencies, List<Integer> numlist, List<Integer> denolist,
			String aggrule, String excludedField) {
//		MatchOperation matchOperation = Aggregation.match(Criteria.where("inid").in(dependencies).and("tp").is(timePeriodId));
		
		//new code for facility type/level wise aggregation
		Criteria criteria = new Criteria();

		List<Criteria> andExpression = new ArrayList<>();
		//ensure all the excluded fields to be be null
		if(excludedField!=null) {
			if(excludedField.split("!").length > 1) {
				for (String oneCriteria : excludedField.split("!")[1].split(",")) {
					Criteria expression = Criteria.where(oneCriteria).gt(null);
					andExpression.add(expression);
				}
			}
		}
		
		Criteria tpInidcriteria = Criteria.where("inid").in(dependencies).and("tp").is(timePeriodId).and("isValid")
				.is(true).and("latest").is(true).and("duplicate").is(false);
		andExpression.add(tpInidcriteria);
		criteria.andOperator(andExpression.toArray(new Criteria[andExpression.size()]));
		
		MatchOperation matchOperation=Aggregation.match(criteria);
		
		GroupOperation groupOperation=null;
		ProjectionOperation projectionOperation=null;
		
		List<String> groupList = new ArrayList<>(Arrays.asList("areaId,tp".split(",")));
		List<String> includeList = new ArrayList<>();
		
		//these are included fields
		if(excludedField.split("!")[0].split(",").length > 1) {
			
			excludedField = excludedField.substring(excludedField.split("!")[0].indexOf(",")+1, excludedField.split("!")[0].length());
			
			includeList = new ArrayList<>(Arrays.asList(excludedField.split(",")));
			groupList.addAll(includeList);
			
		}
		
		groupOperation=Aggregation.group(groupList.toArray(new String[groupList.size()])).sum(when(where("inid").in(numlist)).thenValueOf("$dataValue").otherwise(0)).as("numerator")
				.sum(when(where("inid").in(denolist)).thenValueOf("$dataValue").otherwise(0)).as("denominator");
		
		projectionOperation=Aggregation.project().and("_id.areaId").as("areaId")
				.and("_id.tp").as("tp").and("numerator").as("numerator").and("denominator").as("denominator")
				.andInclude(includeList.toArray(new String[includeList.size()]))
				.andExclude("_id")
				.and(when(where("denominator").gt(0)).thenValueOf(Divide.valueOf("numerator").divideBy("denominator")).otherwise(0)).as("dataValue");
		return Aggregation.newAggregation(DataValue.class,matchOperation,groupOperation,projectionOperation);
	}


	private TypedAggregation<DataValue> aggregateAreaTree(Integer tp, List<Integer> areaList, String excludedField) {
		
//		MatchOperation matchOperation=Aggregation.match(Criteria.where("tp").is(tp).and("areaId").in(areaList));
		
		//new code for facility type/level wise aggregation
		Criteria criteria =new Criteria();
		
		List<Criteria> andExpression = new ArrayList<>();
		
		//ensure all the excluded fields to be be null
		
		if(excludedField!=null) {
			if(excludedField.split("!").length > 1) {
				for (String oneCriteria : excludedField.split("!")[1].split(",")) {
//					Criteria expression = Criteria.where(oneCriteria).exists(false);
					Criteria expression = Criteria.where(oneCriteria).gt(null);
					andExpression.add(expression);
				}
			}
		}
		Criteria tpCriteria = Criteria.where("tp").is(tp).and("areaId").in(areaList);
		andExpression.add(tpCriteria);
		criteria.andOperator(andExpression.toArray(new Criteria[andExpression.size()]));
		
		MatchOperation matchOperation=Aggregation.match(criteria);
		
		LookupOperation lookupOperation=Aggregation.lookup("area", "areaId", "areaId", "parent");
		GroupOperation groupOperation;
		List<String> groupList = new ArrayList<>(Arrays.asList("parent.parentAreaId,inid,tp,_case".split(",")));
		List<String> includeList = new ArrayList<>();
		
		//these are include fields
		if(excludedField.split("!")[0].split(",").length > 1) {
			
			excludedField = excludedField.substring(excludedField.split("!")[0].indexOf(",")+1, excludedField.split("!")[0].length());
			
			includeList = new ArrayList<>(Arrays.asList(excludedField.split(",")));
			groupList.addAll(includeList);
			
		}
		
		groupOperation=Aggregation.group(groupList.toArray(new String[groupList.size()])).sum("dataValue").as("dataValue");
		UnwindOperation unwindOperation=Aggregation.unwind("$_id.parentAreaId");
		return Aggregation.newAggregation(DataValue.class,matchOperation,lookupOperation,groupOperation,unwindOperation);
		
	}
	
	private TypedAggregation<DataValue> getPercentData(List<Integer> dep,List<Integer> num,List<Integer> deno, String rule,String excludedField){
		
//		Criteria criteria = Criteria.where("inid").in(dep).and("tp").is(timePeriodId);
		
		//new code for facility type/level wise aggregation
		Criteria criteria =new Criteria();
				
		List<Criteria> andExpression = new ArrayList<>();
		
		//ensure all the excluded fields to be be null
		if(excludedField!=null) {
			if(excludedField.split("!").length > 1) {
				for (String oneCriteria : excludedField.split("!")[1].split(",")) {
//					Criteria expression = Criteria.where(oneCriteria).exists(false);
					
					Criteria expression = Criteria.where(oneCriteria).gt(null);
					andExpression.add(expression);
					
				}
			}
//			criteria.andOperator(andExpression.toArray(new Criteria[andExpression.size()]));
		}
		
		Criteria tpInidCriteria = Criteria.where("inid").in(dep).and("tp").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false);
		andExpression.add(tpInidCriteria);
		criteria.andOperator(andExpression.toArray(new Criteria[andExpression.size()]));
		
		MatchOperation matchOperation = Aggregation.match(criteria);
		
//		MatchOperation matchOperation = Aggregation.match(Criteria.where("inid").in(dep).and("tp").is(timePeriodId));
		GroupOperation groupOperation=null;
		ProjectionOperation projectionOperation=null;
		ProjectionOperation p1=null;
		ProjectionOperation p2=null;
		
		if(rule.equals("sub")) {
			List<String> groupList = new ArrayList<>(Arrays.asList("areaId,tp".split(",")));
			List<String> includeList = new ArrayList<>();
			
			//new code for type/level wise aggregation
			if(excludedField.split("!")[0].split(",").length > 1) {
				
				excludedField = excludedField.substring(excludedField.split("!")[0].indexOf(",")+1, excludedField.split("!")[0].length());
				
				includeList = new ArrayList<>(Arrays.asList(excludedField.split(",")));
				groupList.addAll(includeList);
			}
			
			groupOperation=Aggregation.group(groupList.toArray(new String[groupList.size()])).sum(when(where("inid").in(deno)).thenValueOf("$dataValue").otherwise(0)).as("denominator")
					.sum(Sum.sumOf(when(where("inid").is(num.get(0))).then("$dataValue").otherwise(0))).as("n1")
					.sum(when(where("inid").is(num.get(1))).then("$dataValue").otherwise(0)).as("n2");
			
			p1=Aggregation.project().and("areaId").as("areaId").and("tp").as("tp")
					.andInclude(includeList.toArray(new String[includeList.size()]))
					.and(Subtract.valueOf("n1").subtract("n2")).as("numerator").and("denominator").as("denominator");
			
			p2=Aggregation.project().and("areaId").as("areaId").and("tp").as("tp").and("numerator").as("numerator").and("denominator").as("denominator")
					.andInclude(includeList.toArray(new String[includeList.size()]))
					.and(when(where("denominator").gt(0)).thenValueOf(Divide.valueOf(Multiply.valueOf("numerator")
					.multiplyBy(100)).divideBy("denominator")).otherwise(0)).as("dataValue");
			
			return Aggregation.newAggregation(DataValue.class,matchOperation,groupOperation,p1,p2);
		} else {

			List<String> groupList = new ArrayList<>(Arrays.asList("areaId,tp".split(",")));
			List<String> includeList = new ArrayList<>();
			
			if (excludedField.split("!")[0].split(",").length > 1) {

				excludedField = excludedField.substring(excludedField.split("!")[0].indexOf(",") + 1,
						excludedField.split("!")[0].length());

				includeList = new ArrayList<>(Arrays.asList(excludedField.split(",")));
				groupList.addAll(includeList);

			}

			groupOperation = Aggregation.group(groupList.toArray(new String[groupList.size()]))
					.sum(when(where("inid").in(num)).thenValueOf("$dataValue").otherwise(0)).as("numerator")
					.sum(when(where("inid").in(deno)).thenValueOf("$dataValue").otherwise(0)).as("denominator");
			
			projectionOperation = Aggregation.project().and("_id.areaId").as("areaId").and("_id.tp").as("tp")
					.andInclude(includeList.toArray(new String[includeList.size()]))
					.and("numerator").as("numerator").and("denominator").as("denominator").andExclude("_id")
					.and(when(where("denominator").gt(0)).thenValueOf(
							Divide.valueOf(Multiply.valueOf("numerator").multiplyBy(100)).divideBy("denominator"))
							.otherwise(0))
					.as("dataValue");


			return Aggregation.newAggregation(DataValue.class, matchOperation, groupOperation,
					projectionOperation);
		}
	}
	
//	for count of 0 records
	public Aggregation getDropdownAggregationResults(Integer formId, String area, String collection, String path, List<Integer> tdlist,String name,String conditions) {
		List<String> condarr=new ArrayList<>();
		if(conditions!=null && !conditions.isEmpty())
			condarr=Arrays.asList(conditions.split(";"));
		Criteria matchCriteria=Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false);
		if(!condarr.isEmpty()) {
			condarr.forEach(_cond->{
				matchCriteria.andOperator(Criteria.where(_cond.split(":")[0].split("\\(")[1]).is(Integer.parseInt(_cond.split(":")[1].split("\\)")[0])));
			});
		}
		MatchOperation matchOperation = Aggregation.match(matchCriteria);
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
//		ProjectionOperation projectionOperation1=Aggregation.project()
//				.and(area).as("area")
//				.and(Sum.sumOf(when(where("data."+path).in(tdlist)).then(1).otherwise(0))).as("projectedData");
		
		ProjectionOperation projectionOperation1=Aggregation.project()
				.andInclude(area.split(","))
				.and(Sum.sumOf(when(where("data."+path).in(tdlist)).then(1).otherwise(0))).as("projectedData");
		
//		GroupOperation groupOperation= Aggregation.group("area").sum("projectedData").as("value");
		
		area = area.replaceAll("data.", "");
		
		GroupOperation groupOperation= Aggregation.group(area.split(",")).sum("projectedData").as("value");
		
		return Aggregation.newAggregation(matchOperation,projectionOperation,projectionOperation1,groupOperation);
	}
	
	public Aggregation getTableAggregationResults(Integer formId, String area, String collection, String path, String table,String name) {
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		UnwindOperation unwindOperation = Aggregation.unwind("data."+table);
//		GroupOperation groupOperation= Aggregation.group(area).sum("data."+table+"."+path).as("value");
		
		GroupOperation groupOperation= Aggregation.group(area.split(",")).sum("data."+table+"."+path).as("value");
		return Aggregation.newAggregation(matchOperation,projectionOperation,unwindOperation,groupOperation);
	}
	
	private Aggregation getTableCountResults(Integer formId, String area, String collection, String path, String table,String _rule,String name) {
		String[] rules= _rule.split(";");
		GroupOperation _groupOp=null;

		for (String rule: rules) {
			switch (rule.split("\\(")[0]) {
			case "and$eq":
				_groupOp=Aggregation.group(area.split(",")).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.is(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;
			case "and$gt":
				_groupOp=Aggregation.group(area.split(",")).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.gt(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;
			case "and$lt":
				_groupOp=Aggregation.group(area.split(",")).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.lt(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;
			case "and$gte":
				_groupOp=Aggregation.group(area.split(",")).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.gte(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;
			case "and$lte":
				_groupOp=Aggregation.group(area.split(",")).sum(when(where(rule.split("\\(")[1].split(":")[0])
						.lte(Integer.parseInt(rule.split("\\(")[1].split(":")[1].split("\\)")[0]))).then(1).otherwise(0)).as("value");
				break;

			default:
				break;
			}
		    
		}
		MatchOperation matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
		ProjectionOperation _projectOp=Aggregation.project("data");
		UnwindOperation _unwindOp=Aggregation.unwind("data."+table);
		
		return Aggregation.newAggregation(Map.class, matchOperation,_projectOp, _unwindOp,_groupOp);
	}
	
	public Aggregation getNumericAggregationResults(Integer formId, String area, String collection, String path,String name,String conditions) {
		
//		area -> data.f1q_district,data.f1FacilityType,data.f1FacilityLevel

		List<String> condarr=new ArrayList<>();
		if(!conditions.equals("null")&&!conditions.isEmpty())
			condarr=Arrays.asList(conditions.split(";"));
		Criteria matchCriteria=Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false);
		if(!condarr.isEmpty()) {
		condarr.forEach(_cond->{
			matchCriteria.andOperator(Criteria.where(_cond.split(":")[0].split("\\(")[1]).is(Integer.parseInt(_cond.split(":")[1].split("\\)")[0])));
		});
		}
		String pathString="";
		path="data."+path;
		path=path.replace("+", "+data.");
		path=path.replace("-", "-data.");
		pathString=path;
//		String[] paths=path.split(",");
//		String pathString="";
//		for (int i = 0; i < paths.length; i++) {
//			pathString=pathString+"data."+paths[i]+"+";
//		}
//		pathString=pathString.substring(0,pathString.lastIndexOf("+"));
		MatchOperation matchOperation = Aggregation.match(matchCriteria);
		ProjectionOperation projectionOperation=null;
		ProjectionOperation pop=null;
		GroupOperation groupOperation=null;
		if(pathString.contains("+")||pathString.contains("-")) {
			projectionOperation=Aggregation.project().and("data").as("data");
//			pop=Aggregation.project().and(area).as("area").andExpression(pathString).as("value1");
			
			pop=Aggregation.project().andInclude(area.split(",")).andExpression(pathString).as("value1");
			
//			groupOperation= Aggregation.group("area").sum("value1").as("value");
			
			area = area.replaceAll("data.", "");
			groupOperation= Aggregation.group(area.split(",")).sum("value1").as("value");
			
			return Aggregation.newAggregation(matchOperation,projectionOperation,pop,groupOperation);
		}else {
			projectionOperation=Aggregation.project().and("data").as("data");
			
			groupOperation= Aggregation.group(area.split(",")).sum(pathString).as("value");
			
//			groupOperation= Aggregation.group(area).sum(pathString).as("value");
			return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
		}
		
	}
	
	public Aggregation getUniqueCount(Integer formId, String area, String collection, String name,String childArea,String conditions) {
		
		List<String> condarr=new ArrayList<>();
		if(!conditions.isEmpty())
			condarr=Arrays.asList(conditions.split(","));
		Criteria criteria = Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false);
		if(!condarr.isEmpty()) {
		condarr.forEach(_cond->{
			criteria.andOperator(Criteria.where("data."+_cond.split("=")[0]).is(Integer.parseInt(_cond.split("=")[1])));
		});
		}
		
		MatchOperation matchOperation=Aggregation.match(criteria);
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area.split(",")).addToSet("data."+childArea).as("childArea");
		UnwindOperation unwindOperation=Aggregation.unwind("childArea");
		GroupOperation groupOperation2=Aggregation.group("$_id").count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation,unwindOperation,groupOperation2);
	}
	
	public Aggregation getTotalVisitCount(Integer formId, String area) {
		MatchOperation matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area.split(",")).count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getCount(Integer formId, String area, String path, Integer value, String rule) {
		MatchOperation matchOperation=null;
		switch (rule) {
		case "eq":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).is(value).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
			break;
		case "lte":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).lte(value).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
			break;
		case "gte":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).gte(value).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
			break;
		case "gt":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).gt(value).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
			break;
		case "lt":
			matchOperation=Aggregation.match(Criteria.where("formId").is(formId).and("data."+path).lt(value).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
			break;

		default:
			break;
		}
		
		ProjectionOperation projectionOperation=Aggregation.project().and("data").as("data");
		GroupOperation groupOperation=Aggregation.group(area.split(",")).count().as("dataValue");
		return Aggregation.newAggregation(matchOperation,projectionOperation,groupOperation);
	}
	
	public Aggregation getRepeatCountQuery(Integer formId, String area, String path, List<Integer> valueList,
			String query) {

		MatchOperation matchOperation = null;
		ProjectionOperation projectionOperation = null;
		GroupOperation groupOperation = null;
		ProjectionOperation projectionOperation2 = null;
//		GroupOperation groupOperation2 = null;

		ProjectionOperation projectionOperation3 = null;
		if (path.equals("")) {
			matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
			projectionOperation = Aggregation.project().and("data").as("data");
			groupOperation = Aggregation.group(query.split(":")[1], area).count().as("totalcount");
			projectionOperation2 = Aggregation.project(area.split("\\.")[1])
					.and(when(where("totalcount").gt(1)).then(1).otherwise(0)).as("repeatCount");
//				groupOperation2 = Aggregation.group(area.split("\\.")[1]).count().as("dataValue");

			projectionOperation3 = Aggregation.project().and(area.split("\\.")[1]).as(area.split("\\.")[1])
					.and(when(where("repeatCount").is(1)).then(Sum.sumOf("repeatCount")).otherwise(0)).as("dataValue");

		} else {
			matchOperation = Aggregation.match(Criteria.where("formId").is(formId).and("data." + path).in(valueList).and("timePeriod.timePeriodId").is(timePeriodId).and("isValid").is(true).and("latest").is(true).and("duplicate").is(false));
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
