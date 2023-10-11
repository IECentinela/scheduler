package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class Query {
	
	private Object or;
	private Object must;
	private Object nor;
	private int limit;

}
