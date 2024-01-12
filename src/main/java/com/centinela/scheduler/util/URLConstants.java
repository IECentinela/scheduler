package com.centinela.scheduler.util;

public class URLConstants {
	
	public static final String URL_LOGIN = "/authorize/email/login"; 
	public static final String URL_LIVE_REPORT = "/search/twitter/live";
	public static final String URL_GET_PUBLICATIONS = "/reports/";
	public static final String URL_7_DAYS = "/search/twitter/7-day";
	public static final String URL_GET_STATS = "/stats.json";
	
	public String GET_URL_PUBLICATIONS (String reportId) {
		return URL_GET_PUBLICATIONS + reportId + "/output";
	}
	
}
