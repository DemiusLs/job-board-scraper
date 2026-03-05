package com.demiusls.job_board_scraper.interfaces;
import java.util.List;

import com.demiusls.job_board_scraper.model.JobPost;
import com.demiusls.job_board_scraper.model.SearchCriteria;


public interface JobScraper {
    
    List<JobPost> fetchJobs(SearchCriteria criteria);
    
    String getProviderName();
    
}
