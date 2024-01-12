package com.centinela.scheduler.service;

import java.sql.Date;

import com.centinela.scheduler.dto.CreateLiveReport;

public interface TweetService {

	public String createLiveReport(CreateLiveReport liveReport, String url);
	public String getStats (int idProyecto, String reportId, String url);
	public String cargarGraphextPorRangoDeFecha(Date in_startDate, Date in_endDate, Integer idProyecto);
}
