package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class Pagination {
	
	 private int offset;
	 private int limit;
	 private int total;
	 private String nextResults;

}
