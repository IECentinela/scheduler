package com.centinela.scheduler.dto;

import lombok.Data;

@Data
public class General {
	
	private Integer since;
    private Integer until;
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
    private Integer receivedRetweets;
    private Integer favorites;
    private Integer impressions;
    private Integer quotes;
    private Integer totalReplies;
    private Integer bookmarks;
    private Integer contributors;
    private Integer originalContributors;
    private Integer originals;
    private Double tweetValueMean;
    private Double influence;
    private Double engagement;
    private Double contributorValueMean;
    private Negative negative;
    private Positive positive;
    private Neutral neutral;
    private Undefined undefined;
}
