package org.sdrc.datum19.repository;

import java.util.List;

import org.sdrc.datum19.document.AggregateLegacyDataStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AggregateLegacyDataStatusRepository extends MongoRepository<AggregateLegacyDataStatus, String> {

	List<AggregateLegacyDataStatus> findByStatus(String status);
//	AggregateLegacyDataStatus findById(String id);
	
}
