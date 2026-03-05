package com.demiusls.job_board_scraper.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.demiusls.job_board_scraper.service.ScraperManager;

@Controller
@RequestMapping("/jobs")
public class JobController {
    @Autowired
    private ScraperManager scraperManager;

    @GetMapping
    public String listJobs(Model model) {
        model.addAttribute("jobs", scraperManager.getAllSavedJobs());
        return "job-list"; 
    }

    @PostMapping("/scrape")
    public String runScraper() {
        scraperManager.runAllScrapers();
        return "redirect:/"; 
    }
    
}
