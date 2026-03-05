package com.demiusls.job_board_scraper.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.demiusls.job_board_scraper.interfaces.JobScraper;
import com.demiusls.job_board_scraper.model.JobPost;
import com.demiusls.job_board_scraper.model.SearchCriteria;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AdzunaScraperService implements JobScraper {

    // Controlla che nel file properties ci sia scritto "adizuna" o "adzuna"
    @Value("${adzuna.app_id}") 
    private String appId;

    @Value("${adzuna.app_key}")
    private String appKey;

    // Inizializziamo il client HTTP e il parser JSON
    @Autowired
    private HttpClient client ;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getProviderName() {
        return "Adzuna";
    }

    @Override
    public List<JobPost> fetchJobs(SearchCriteria criteria) {
        List<JobPost> foundJobs = new ArrayList<>();

        try { 
            StringBuilder urlBuilder = new StringBuilder("https://api.adzuna.com/v1/api/jobs/it/search/1?");
            urlBuilder.append("app_id=").append(appId)
                      .append("&app_key=").append(appKey)
                      .append("&results_per_page=50")
                      .append("&full_time=1"); 

            // Gestione dei giorni
            if (criteria.daysOld() > 0) {
                urlBuilder.append("&max_days_old=").append(criteria.daysOld());
            }

            // Ricerca RIGIDA nel titolo
            if (criteria.title() != null && !criteria.title().isEmpty()) {
                urlBuilder.append("&title_only=").append(URLEncoder.encode(criteria.title(), StandardCharsets.UTF_8));
            }

            // Parole chiave extra per livello e modalità lavoro
            String extraKeywords = "";
            if (criteria.level() != null && !criteria.level().isEmpty()) {
                extraKeywords += criteria.level() + " ";
            }
            if (criteria.remoteOnly()) {
                extraKeywords += "remote smartworking"; 
            }
            if (!extraKeywords.trim().isEmpty()) {
                urlBuilder.append("&what=").append(URLEncoder.encode(extraKeywords.trim(), StandardCharsets.UTF_8));
            }

            // Gestione Località e Distanza (Se la ricerca non è nazionale)
            if (criteria.location() != null && !criteria.location().isEmpty()) {
                if (!criteria.location().equalsIgnoreCase("Italy") && !criteria.location().equalsIgnoreCase("Italia")) {
                    urlBuilder.append("&where=").append(URLEncoder.encode(criteria.location(), StandardCharsets.UTF_8))
                              .append("&distance=100"); 
                }
            }
                
            // Chiamata HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode resultsArray = root.path("results");

            // Parsing del JSON di Adzuna e Mapping
            if (resultsArray != null && resultsArray.isArray()) {
                for (JsonNode node : resultsArray) {
                    JobPost job = new JobPost();
                    
                    job.setTitle(node.path("title").asString());
                    job.setCompany(node.path("company").path("display_name").asString());
                    job.setLink(node.path("redirect_url").asString());
                    job.setDescription(node.path("description").asString());
                    job.setLocation(node.path("location").path("display_name").asString());

                    // --- Logica Data di Pubblicazione ---
                    String dateString = node.path("created").asString();
                    if (dateString != null && !dateString.isEmpty()) {
                        try {
                            // Trasformiamo la stringa nel tuo LocalDateTime
                            java.time.LocalDateTime publishDate = java.time.ZonedDateTime.parse(dateString).toLocalDateTime();
                            job.setPublishDate(publishDate);
                        } catch (Exception e) {
                            System.err.println("Impossibile parsare la data per l'annuncio: " + job.getTitle());
                        }
                    }

                    // --- Logica Modalità Lavoro ---
                    String descLower = job.getDescription().toLowerCase();
                    String titleLower = job.getTitle().toLowerCase();
                    
                    if (criteria.remoteOnly() || titleLower.contains("remote") || descLower.contains("remoto") || descLower.contains("telelavoro")) {
                        job.setWorkMode("Remote");
                    } else if (descLower.contains("hybrid") || descLower.contains("ibrido") || descLower.contains("smart working")) {
                        job.setWorkMode("Hybrid");
                    } else {
                        job.setWorkMode("On-site");
                    }

                    // Aggiungiamo il lavoro alla lista 
                    foundJobs.add(job);
                }
            }

        } catch (Exception e) { 
            System.err.println("Errore in Adzuna Scraper per la ricerca '" + criteria.title() + "': " + e.getMessage());
        }

        return foundJobs; 
    }
}