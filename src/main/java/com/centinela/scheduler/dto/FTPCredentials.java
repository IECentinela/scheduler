package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class FTPCredentials {
	
	private String serverFTP;

	private  int portFTP;

	private  String usernameFTP;

	private  String passwordFTP;

}
