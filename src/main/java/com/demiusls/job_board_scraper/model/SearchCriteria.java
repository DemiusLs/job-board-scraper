package com.demiusls.job_board_scraper.model;

public record SearchCriteria(
    String title, 
    String location, 
    boolean remoteOnly,
    String level,
    int daysOld
) {
}
