package com.demiusls.job_board_scraper.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.demiusls.job_board_scraper.service.RssScraperService;

@Controller
@RequestMapping("/jobs")
public class JobController {
    @Autowired
    private RssScraperService scraperService;

    @GetMapping
    public String listJobs(Model model) {
        model.addAttribute("jobs", scraperService.getAllSavedJobs());
        return "job-list"; 
    }

    @GetMapping("/refresh")
    public String refreshJobs() {
        scraperService.fetchAndSaveJobs();
        return "redirect:/jobs"; 
    }
    
}
