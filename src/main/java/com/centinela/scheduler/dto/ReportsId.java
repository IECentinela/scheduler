package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class ReportsId {
	
	String resourceId;
	CreateLiveReport queryreport;
	Integer fk_project;
	String startDate;
	String endDate;

}