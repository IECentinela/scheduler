package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class ReportsId {
	
	private String resourceId;
	private CreateLiveReport queryreport;
	private Integer fk_project;
	private String startDate;
	private String endDate;
	private Integer fk_source;
	private String startDateProject;
	private String endDateProject;
	
	@Override
	public String toString() {
		return "ReportsId [resourceId=" + resourceId + ", queryreport=" + queryreport + ", fk_project=" + fk_project
				+ ", startDate=" + startDate + ", endDate=" + endDate + ", fk_source=" + fk_source
				+ ", startDateProject=" + startDateProject + ", endDateProject=" + endDateProject + "]";
	}

}