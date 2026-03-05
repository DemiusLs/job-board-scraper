package com.demiusls.job_board_scraper.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import com.demiusls.job_board_scraper.interfaces.JobScraper;
import com.demiusls.job_board_scraper.model.JobPost;
import com.demiusls.job_board_scraper.model.SearchCriteria;
import com.demiusls.job_board_scraper.repository.JobRepository;



@Service
public class ScraperManager {

    
    @Autowired
    private  JobRepository jobRepository;
    @Autowired
    private List<JobScraper> scrapers;

    public void runAllScrapers() {
        // 1. Definiamo le nostre ricerche usando il record SearchCriteria
        // (title, location, remoteOnly, level, daysOld)
        List<SearchCriteria> searches = List.of(
            new SearchCriteria("Java Developer", "Italy", true, "Junior", 3),
            new SearchCriteria("Backend Developer", "Milano", false, "", 3),
            new SearchCriteria("Frontend", "Brescia", false, "Entry Level", 3),
            new SearchCriteria("Data Engineer", "Lombardia", false, "", 3)
        );

        System.out.println("=== Inizio ciclo di scraping generale ===");

        // 2. Chiediamo a ogni operaio (scraper) di fare il suo lavoro
        for (JobScraper scraper : scrapers) {
            System.out.println("-> Affido il lavoro a: " + scraper.getProviderName());
            
            for (SearchCriteria criteria : searches) {
                System.out.println("   Cerco: " + criteria.title() + " in " + criteria.location());
                
                try {
                    // Lo scraper fa la chiamata e ci restituisce la lista di lavori
                    List<JobPost> jobs = scraper.fetchJobs(criteria);
                    int salvati = 0;
                    
                    // 3. Il Manager controlla i duplicati e salva nel DB
                    for (JobPost job : jobs) {
                        if (job.getLink() != null && !job.getLink().isEmpty()) {
                            // Se il link non esiste già nel database, lo salviamo
                            if (!jobRepository.existsByLink(job.getLink())) {
                                jobRepository.save(job);
                                salvati++;
                            }
                        }
                    }
                    
                    System.out.println("   [OK] Trovati e salvati " + salvati + " nuovi lavori.");
                    
                    // Pausa di 2 secondi tra una ricerca e l'altra per non farsi bloccare dalle API
                    Thread.sleep(2000);
                    
                } catch (Exception e) {
                    System.err.println("   [ERRORE] Problema con la ricerca " + criteria.title() + ": " + e.getMessage());
                }
            }
        }
        System.out.println("=== Scraping completato con successo! ===");
    }

    /**
     * Metodo per recuperare i lavori dal DB da passare al Controller
     */
    public List<JobPost> getAllSavedJobs() {
        return jobRepository.findAll();
    }
    

    
}
