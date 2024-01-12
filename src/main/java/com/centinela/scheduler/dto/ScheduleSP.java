package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class ScheduleSP {
	
	private Integer fk_project;
	private Integer IDExecution;
	private String task_description;
	private String next_execution_time;
	private Integer fk_source;
	private String projectName;
	private String resourceId;

}
