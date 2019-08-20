package org.sdrc.datum19.repository;

import java.util.Date;
import java.util.List;

import org.sdrc.datum19.document.TimePeriod;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimePeriodRepository extends MongoRepository<TimePeriod, String> {
	
	@Query("{'startDate':{'$eq':{'$date': :#{#sd}}},'endDate':{'$eq':{'$date': :#{#ed}}}}")
	public TimePeriod getTimePeriod(@Param("sd")String sd, @Param("ed")String ed);

	public TimePeriod findByEndDateBetween(Date date, Date date2);

	@Query("{'startDate':{'$lte':'?0'}}, 'endDate':{'$gte':'?0'}")
	public TimePeriod getTimePeriod(String currentDate);

	public TimePeriod findByFinancialYearAndTimePeriodAndPeriodicity(String financialYear, String quarterTimePeriod, String string);
	
	public TimePeriod findByTimePeriodId(Integer tid);

	public TimePeriod findByFinancialYearAndPeriodicity(String financialYearforYearly, String string);

//	public List<TimePeriod> findTop5ByPeriodicityAndFinancialYearOrderByCreatedDateDesc(String string, String monthlyFinancialYear);

	public List<TimePeriod> findTop5ByPeriodicityOrderByCreatedDateDesc(String periodicity);

	public List<TimePeriod> findAllByPeriodicityOrderByCreatedDateDesc(String periodicity);

	@Query("{'startDate':{$lte:?0},'endDate':{$gte:?0},periodicity:?1}")
	TimePeriod getCurrentTimePeriod(Date createdDate, String periodicity);
}
