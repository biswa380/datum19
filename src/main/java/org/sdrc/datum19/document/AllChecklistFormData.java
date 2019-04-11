
package org.sdrc.datum19.document;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sdrc.datum19.model.ChecklistSubmissionStatus;
import org.sdrc.datum19.model.FormAttachmentsModel;
import org.sdrc.datum19.model.SubmissionCompleteStatus;
import org.sdrc.datum19.usermgmt.Account;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

//import in.co.sdrc.sdrcdatacollector.models.FormAttachmentsModel;
import lombok.Data;

@Document
@Data
public class AllChecklistFormData {

	@Id
	private String id;

	private String userName;

	private String userId;
	
	private String submittedBy;

	private Date createdDate;

	private Date updatedDate;

	private Date syncDate;

	private Map<String, Object> data;

	private Integer formId;

	private String uniqueId;

	private boolean rejected = false;

	private String rejectMessage;

	private String uniqueName;

	private TimePeriod timePeriod;

	private Boolean isAggregated;

	private Boolean isValid;

	private Integer attachmentCount = 0;

	Map<String, List<FormAttachmentsModel>> attachments;

	private SubmissionCompleteStatus submissionCompleteStatus = SubmissionCompleteStatus.PC;
	
	private ChecklistSubmissionStatus checklistSubmissionStatus = ChecklistSubmissionStatus.PENDING;

	private Boolean latest;
	
	private Date rejectedApprovedDate;
	
	private Account actionBy;
}
