package org.sdrc.datum19.controller;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.sdrc.datum19.document.TimePeriod;
import org.sdrc.datum19.repository.TimePeriodRepository;
import org.sdrc.datum19.service.MongoAggregationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/*
 * author : Biswabhusan Pradhan
 * email : biswabhusan@sdrc.co.in
 * 
 */

@Controller
//@EnableScheduling
public class DAUS19JobController {
	
	@Autowired
	private TimePeriodRepository timePeriodRepository;
	
	
	@Autowired
	private MongoAggregationService mongoAggregationService;
	
	
	@GetMapping("/")
	@ResponseBody 
	public String welcome() {
		return "WELCOME TO SI-RMNCHA AGGREGATE INSTANCE";
	}
	
	private SimpleDateFormat simpleDateformater = new SimpleDateFormat("yyyy-MM-dd");
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//	@Scheduled(cron="0 0 0 11 * ?")//monthly cron
//	@Scheduled(cron="0 30 3 1/1 * ?")//daily cron
	@GetMapping("/runJob")
	@ResponseBody 
	public String runMonthlyJob() throws ParseException, InvalidFormatException, IOException {
//		TimePeriod tp=getTimePeriodForAggregation(); 
		for (TimePeriod tp : getCurrentTimePeriodForAggregation()) { //get only last tp after uat instead of 2tps
			mongoAggregationService.aggregate(tp.getTimePeriodId(), "monthly");
		}
	
		return "aggregation completed";
	}
	
	/*@Scheduled(cron="0 0 0 10 JAN,APR,JUL,OCT ?")
	@GetMapping("/runQuarterlyJob")
	public void runQuarterlyJob() throws ParseException, InvalidFormatException, IOException {
		TimePeriod tp=getQuarterForAggregation();
//		aggregationService.aggregateDependencies(tp.getTimePeriodId(), "quarterly");
		mongoAggregationService.aggregate(tp.getTimePeriodId(), "quarterly");
	}
	
	@Scheduled(cron="0 0 0 10 JAN ?")
	@GetMapping("/runYearlyJob")
	public void runYearlyJob() throws ParseException, InvalidFormatException, IOException {
		TimePeriod tp=getYearForAggregation();
//		aggregationService.aggregateDependencies(tp.getTimePeriodId(), "yearly");
		mongoAggregationService.aggregate(tp.getTimePeriodId(), "once");
	}*/
	
	public TimePeriod getYearForAggregation() throws ParseException {
		Calendar endDateCalendar = Calendar.getInstance();
		endDateCalendar.add(Calendar.MONTH, -1);
		endDateCalendar.set(Calendar.DATE, 1);
		endDateCalendar.set(Calendar.DATE, endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		

		Date eDate = endDateCalendar.getTime();
		String endDateStr = simpleDateformater.format(eDate);
		Date endDate = (Date) formatter.parse(endDateStr + " 23:59:59.000");
		
		Calendar startDateCalendar1 = Calendar.getInstance();
		startDateCalendar1.add(Calendar.MONTH, -12);
		startDateCalendar1.set(Calendar.DATE, 1);
		Date startDate1 = (Date) formatter.parse(simpleDateformater.format(startDateCalendar1.getTime()) + " 00:00:00.000");
		String sd=toISO8601UTC(new java.util.Date(startDate1.getTime()));
		String ed=toISO8601UTC(new java.util.Date(endDate.getTime()));
		
		TimePeriod timePeriod = timePeriodRepository.getTimePeriod(sd, ed);
		return timePeriod;
	}

	public TimePeriod getQuarterForAggregation() throws ParseException {
		// TODO Auto-generated method stub
		Calendar endDateCalendar = Calendar.getInstance();
		endDateCalendar.add(Calendar.MONTH, -1);
		endDateCalendar.set(Calendar.DATE, 1);
		endDateCalendar.set(Calendar.DATE, endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		

		Date eDate = endDateCalendar.getTime();
		String endDateStr = simpleDateformater.format(eDate);
		Date endDate = (Date) formatter.parse(endDateStr + " 23:59:59.000");
		
		Calendar startDateCalendar1 = Calendar.getInstance();
		startDateCalendar1.add(Calendar.MONTH, -3);
		startDateCalendar1.set(Calendar.DATE, 1);
		Date startDate1 = (Date) formatter.parse(simpleDateformater.format(startDateCalendar1.getTime()) + " 00:00:00.000");
		String sd=toISO8601UTC(new java.util.Date(startDate1.getTime()));
		String ed=toISO8601UTC(new java.util.Date(endDate.getTime()));
		
		TimePeriod timePeriod = timePeriodRepository.getTimePeriod(sd, ed);
		return timePeriod;
	}

	public TimePeriod getTimePeriodForAggregation() throws ParseException {
		Calendar endDateCalendar = Calendar.getInstance();
		endDateCalendar.add(Calendar.MONTH, -1);
		endDateCalendar.set(Calendar.DATE, 1);
		endDateCalendar.set(Calendar.DATE, endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));

		Date eDate = endDateCalendar.getTime();
		String endDateStr = simpleDateformater.format(eDate);
		Date endDate = (Date) formatter.parse(endDateStr + " 23:59:59.000");
		
		Calendar startDateCalendar1 = Calendar.getInstance();
		startDateCalendar1.add(Calendar.MONTH, -1);
		startDateCalendar1.set(Calendar.DATE, 15);
		Date startDate1 = (Date) formatter.parse(simpleDateformater.format(startDateCalendar1.getTime()) + " 00:00:00.000");
		String sd=toISO8601UTC(new java.util.Date(startDate1.getTime()));
		String ed=toISO8601UTC(new java.util.Date(endDate.getTime()));
		
		TimePeriod timePeriod = timePeriodRepository.getCurrentTimePeriod(startDate1, "1");
//		TimePeriod timePeriod = timePeriodRepository.getTimePeriod(sd, ed);
		return timePeriod;
	}
	
	//get last 2 tps
	public List<TimePeriod> getCurrentTimePeriodForAggregation() throws ParseException {
		return timePeriodRepository.findTop2ByPeriodicityOrderByStartDateDesc("1");
//		return timePeriodRepository.findAll();
	}
	
	public static String toISO8601UTC(Date date) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(tz);
		return df.format(date);
		}
		public static Date fromISO8601UTC(String dateStr) {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(tz);
		try {
		return df.parse(dateStr);
		} catch (ParseException e) {
		e.printStackTrace();
		}
		return null;
		}
}
