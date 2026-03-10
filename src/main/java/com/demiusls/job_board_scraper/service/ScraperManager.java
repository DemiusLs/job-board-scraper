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


    public void runScrapingForCriteria(SearchCriteria criteria, String provider) {
        System.out.println("=== Inizio scraping personalizzato per: " + criteria.title() + " in " + criteria.location() + " su " + provider + " ===");
        
        for (JobScraper scraper : scrapers) {
            
            if ("Tutte".equalsIgnoreCase(provider) || scraper.getProviderName().equalsIgnoreCase(provider)) {
                
                System.out.println("-> Interrogo: " + scraper.getProviderName());
                try {
                    List<JobPost> jobs = scraper.fetchJobs(criteria);
                    int salvati = 0;
                    
                    for (JobPost job : jobs) {
                        if (job.getLink() != null && !job.getLink().isEmpty()) {
                            if (!jobRepository.existsByLink(job.getLink())) {
                                jobRepository.save(job);
                                salvati++;
                            }
                        }
                    }
                    System.out.println("   [OK] " + scraper.getProviderName() + ": Salvati " + salvati + " nuovi lavori.");
                } catch (Exception e) {
                    System.err.println("   [ERRORE] " + scraper.getProviderName() + " - " + e.getMessage());
                }
                
            } else {
                System.out.println("-> Salto " + scraper.getProviderName() + " (Filtro utente)");
            }
        }
        System.out.println("=== Scraping personalizzato completato! ===");
    }




    public void runAllScrapers() {
        // Definiamo le nostre ricerche usando il record SearchCriteria
        List<SearchCriteria> searches = List.of(
            new SearchCriteria("Java", "Italy", true, "", 3),
            new SearchCriteria("Backend ", "Italy", true, "", 3),
            new SearchCriteria("Frontend", "Italy", true, "", 3),
            new SearchCriteria("Full Stack", "Italy", true, "", 3),
            new SearchCriteria("Java", "Brescia", false, "", 3),
            new SearchCriteria("Backend ", "Brescia", false, "", 3),
            new SearchCriteria("Frontend", "Brescia", false, "", 3),
            new SearchCriteria("Full Stack", "Brescia", false, "", 3),
            new SearchCriteria("Java", "Milano", false, "", 3),
            new SearchCriteria("Backend ", "Milano", false, "", 3),
            new SearchCriteria("Frontend", "Milano", false, "", 3),
            new SearchCriteria("Full Stack", "Milano", false, "", 3)
            
        );

        System.out.println("=== Inizio ciclo di scraping generale ===");
        //Cicla su tutti gli scraper e tutte le ricerche, delegando il lavoro agli scraper
        for (JobScraper scraper : scrapers) {
            System.out.println("-> Affido il lavoro a: " + scraper.getProviderName());
            
            for (SearchCriteria criteria : searches) {
                System.out.println("   Cerco: " + criteria.title() + " in " + criteria.location());
                
                try {
                    // Lo scraper fa la chiamata e ci restituisce la lista di lavori
                    List<JobPost> jobs = scraper.fetchJobs(criteria);
                    int salvati = 0;
                    
                    // Il Manager controlla i duplicati e salva nel DB
                    for (JobPost job : jobs) {
                        if (job.getLink() != null && !job.getLink().isEmpty()) {
                            if (!jobRepository.existsByLink(job.getLink())) {
                                jobRepository.save(job);
                                salvati++;
                            }
                        }
                    }
                    
                    System.out.println("   [OK] Trovati e salvati " + salvati + " nuovi lavori.");
                    
                    // Pausa di 2 secondi tra una ricerca e l'altra 
                    Thread.sleep(2000);
                    
                } catch (Exception e) {
                    System.err.println("   [ERRORE] Problema con la ricerca " + criteria.title() + ": " + e.getMessage());
                    break;
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
