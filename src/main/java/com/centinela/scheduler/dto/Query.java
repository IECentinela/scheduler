package com.centinela.scheduler.dto;

import java.util.List;

import lombok.Data;

@Data
public class Query {
	
	private List<String> or;
	private List<String> must;
	private List<String> nor;
//	private int limit;

}
