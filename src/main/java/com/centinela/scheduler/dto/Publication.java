package com.centinela.scheduler.dto;


public class Publication {
	
	private Pagination pagination;
	private Data data[];
	
	public Pagination getPagination() {
		return pagination;
	}
	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}
	public Data[] getData() {
		return data;
	}
	public void setData(Data[] data) {
		this.data = data;
	}

}
