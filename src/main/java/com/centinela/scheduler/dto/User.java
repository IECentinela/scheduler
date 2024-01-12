package com.centinela.scheduler.dto;

import java.util.Date;
import lombok.Data;

@Data
public class User {
	
	private String id;
	private String name;
	private String alias;
	private String picture;
	private Integer followers;
	private Integer following;
    private boolean verified;
    private String bio;
    private Date age;
    private Counts counts;
	private String location;
	private String gender;
	private Double value;
	private Integer totalTweets;
	private Integer textTweets;
	private Integer impacts;

}
