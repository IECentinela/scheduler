package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class Positive {
	
	private String _id;
    private Integer total;
    private Integer images;
    private Integer links;
    private Integer linksAndImages;
    private Integer impact;
    private Integer reach;
    private Double contributorsValue;
    private Double economicValue;
    private Integer clears;
    private Integer replies;
    private Integer retweets;
    private Integer totalReplies;
    private Integer quotes;
    private Integer impressions;
    private Integer bookmarks;
    private Integer originalContributors;
    private Integer contributors;
    private Double influence;
    private Double engagement;
    private Integer originals;
    private Double tweetValueMean;
    private Double contributorValueMean;
}
