package com.centinela.scheduler.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class CreateLiveReport {
	
	private Query query;
    private Stream stream;
    private Integer idProyecto;
    private Date in_startDate;
    private Date in_endDate;

}
