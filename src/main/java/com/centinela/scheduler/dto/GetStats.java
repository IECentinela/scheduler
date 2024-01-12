package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class GetStats {
	
	private int idProyecto;
	private String reportId;
	private String url;
}
