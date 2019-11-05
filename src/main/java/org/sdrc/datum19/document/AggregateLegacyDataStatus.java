package org.sdrc.datum19.document;

import java.io.Serializable;
import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * @author Sarita
 * This table contains log of aggregate legacy data
 *
 */
@Document
@Data
public class AggregateLegacyDataStatus implements Serializable {

	private static final long serialVersionUID = 5744360198339831822L;
	
	private String id;
	private String timePeriod;
	private String periodicity;
	private Integer tpId;
	private String status = "INPROGRESS";
	private Date startTime;
	private Date endTime;
	private boolean legacy = false;
}
