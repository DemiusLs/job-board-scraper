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
            // 1. Costruzione dell'URL Base
            StringBuilder urlBuilder = new StringBuilder("https://api.adzuna.com/v1/api/jobs/it/search/1?");
            
            // 2. Credenziali e numero di risultati fissi
            urlBuilder.append("app_id=").append(appId)
                      .append("&app_key=").append(appKey)
                      .append("&results_per_page=50");

            // 3. Parola chiave (what)
            if (criteria.title() != null && !criteria.title().trim().isEmpty()) {
                urlBuilder.append("&what=").append(URLEncoder.encode(criteria.title().trim(), StandardCharsets.UTF_8));
            }

            // 4. LA TUA SCOPERTA: Se remoteOnly è true, forziamo "smart working" con what_and!
            if (criteria.remoteOnly()) {
                urlBuilder.append("&what_and=").append(URLEncoder.encode("smart working", StandardCharsets.UTF_8));
            }

            // 5. Località e Raggio (where & distance)
            if (criteria.location() != null && !criteria.location().trim().isEmpty()) {
                String loc = criteria.location().trim();
                // Ignoriamo "Italy" per fare la ricerca su base nazionale
                if (!loc.equalsIgnoreCase("Italy") && !loc.equalsIgnoreCase("Italia")) {
                    urlBuilder.append("&where=").append(URLEncoder.encode(loc, StandardCharsets.UTF_8))
                              .append("&distance=100");
                }
            }

            // 6. Giorni massimi (max_days_old)
            if (criteria.daysOld() > 0) {
                urlBuilder.append("&max_days_old=").append(criteria.daysOld());
            }

            // 7. Ordinamento fisso per data
            urlBuilder.append("&sort_by=date");

            // --- CHIAMATA HTTP CON WEB AGENT ---
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // --- CONTROLLO ERRORI API ---
            if (response.statusCode() != 200) {
                System.err.println(">>> ATTENZIONE: Errore API per '" + criteria.title() + "'. Codice HTTP: " + response.statusCode());
                System.err.println("URL Tentato: " + urlBuilder.toString());
                return foundJobs; // Passa alla prossima ricerca senza bloccare il programma
            }

            // --- PARSING DEL JSON ---
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode resultsArray = root.path("results");

            if (resultsArray != null && resultsArray.isArray()) {
                for (JsonNode node : resultsArray) {
                    JobPost job = new JobPost();
                    
                    job.setTitle(node.path("title").asText(""));
                    job.setCompany(node.path("company").path("display_name").asText(""));
                    job.setLink(node.path("redirect_url").asText(""));
                    job.setDescription(node.path("description").asText(""));
                    job.setLocation(node.path("location").path("display_name").asText(""));
                    job.setCategory(node.path("category").path("label").asText(""));

                    // --- Logica Data di Pubblicazione ---
                    String dateString = node.path("created").asText("");
                    if (!dateString.isEmpty()) {
                        try {
                            java.time.LocalDateTime publishDate = java.time.ZonedDateTime.parse(dateString).toLocalDateTime();
                            job.setPublishDate(publishDate);
                        } catch (Exception e) {
                            // Data non valida, proseguiamo
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

                    foundJobs.add(job);
                }
            }

        } catch (Exception e) { 
            throw new RuntimeException("Errore critico durante lo scraping di '" + criteria.title() + "': " + e.getMessage(), e);
        }

        return foundJobs;
    }
}