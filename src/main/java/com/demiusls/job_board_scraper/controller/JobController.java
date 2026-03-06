package com.demiusls.job_board_scraper.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/scrape") 
    public String runScraper() {
        
        // 1. Stampiamo questo per confermare che il bottone ha comunicato col backend!
        System.out.println(">>> [WEB] Ricevuta richiesta di aggiornamento offerte dal browser!");
        
        try {
            // 2. Facciamo partire il manager
            scraperManager.runAllScrapers();
            System.out.println(">>> [WEB] Scraping terminato, ricarico la pagina.");
        } catch (Exception e) {
            System.err.println(">>> [ERRORE WEB] Qualcosa è andato storto: " + e.getMessage());
        }
        
        // 3. Ricarichiamo la pagina principale (cambia "/jobs" con "/" se la tua home è la root)
        return "redirect:/jobs"; 
    }
    
}
