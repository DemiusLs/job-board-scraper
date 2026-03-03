package com.demiusls.job_board_scraper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.demiusls.job_board_scraper.model.JobPost;

@Repository
public interface JobRepository extends JpaRepository<JobPost,Integer>{

    boolean existsByLink(String link);    
} 
