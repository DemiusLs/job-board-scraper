package com.demiusls.job_board_scraper.service;


import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.demiusls.job_board_scraper.model.JobPost;
import com.demiusls.job_board_scraper.repository.JobRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


@Service
public class RssScraperService {

    
    @Autowired
    private  JobRepository jobRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rapidapi.key}")
    private String apiKey;

    @Value("${rapidapi.host}")
    private String apiHost;

   

    record SearchCriteria(String title, String location, boolean remoteOnly) {}

    public void fetchAndSaveJobs() {
        List<SearchCriteria> searches = List.of(
            new SearchCriteria("Java Developer", "Italy", true)
            // new SearchCriteria("Full Stack", "Italy", true),
            // new SearchCriteria("Data Engineer", "Italy", true),
            // new SearchCriteria("Java Developer", "Milano", false),
            // new SearchCriteria("Backend Developer", "Brescia", false),
            // new SearchCriteria("Full Stack", "Lombardia", false),
            // new SearchCriteria("DevOps", "Milano", false)
        );

        // Creiamo il client UNA VOLTA sola
        HttpClient client = HttpClient.newHttpClient();

        for (SearchCriteria criteria : searches) {
            try {
                String encodedTitle = URLEncoder.encode(criteria.title(), StandardCharsets.UTF_8);
                String encodedLocation = URLEncoder.encode(criteria.location(), StandardCharsets.UTF_8);
                
                StringBuilder urlBuilder = new StringBuilder("https://");
                urlBuilder.append(apiHost)
                        .append("/active-jb-24h?limit=50&title_filter=")
                        .append(encodedTitle)
                        .append("&location_filter=")
                        .append(encodedLocation)
                        .append("&description_type=text&seniority_filter=Entry%20level");

                if (criteria.remoteOnly()) {
                    urlBuilder.append("&remote=true");
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlBuilder.toString()))
                        .header("x-rapidapi-key", apiKey)
                        .header("x-rapidapi-host", apiHost)
                        .GET().build();

                System.out.println("Avvio ricerca: " + criteria.title() + " in " + criteria.location());

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("RAW RESPONSE per " + criteria.title() + ": " + response.body());
                
                JsonNode root = objectMapper.readTree(response.body());

                if (root.isArray() && !root.isEmpty()) {
                    int count = 0;
                    for (JsonNode node : root) {
                        String link = node.path("url").asString();

                        if (link != null && !link.isEmpty() && !jobRepository.existsByLink(link)) {
                            JobPost job = new JobPost();
                            job.setTitle(node.path("title").asString());
                            job.setCompany(node.path("organization").asString());
                            job.setLink(link);
                            job.setDescription(node.path("description_text").asString());
                            
                            if (node.path("locations_derived").isArray() && !node.path("locations_derived").isEmpty()) {
                                job.setLocation(node.path("locations_derived").get(0).asString());
                            }
                            boolean isRemote = node.path("remote_derived").asBoolean();
                            String desc = job.getDescription().toLowerCase();
                            if (isRemote) {
                                job.setWorkMode("Remote");
                            } else if (desc.contains("hybrid") || desc.contains("ibrido") || desc.contains("smart")) {
                                job.setWorkMode("Hybrid");
                            } else {
                                job.setWorkMode("On-site");
                            }

                            jobRepository.save(job);
                            count++;
                        }
                    }
                    System.out.println(">>> Completato " + criteria.title() + ": " + count + " nuovi lavori salvati.");
                } else {
                    System.out.println("Nessun risultato trovato per: " + criteria.title() + " in " + criteria.location());
                }

                Thread.sleep(1500); 

            } catch (Exception e) {
                System.err.println("Errore per " + criteria.title() + ": " + e.getMessage());
            }
        }
    }

    // Metodo per il Controller
    public List<JobPost> getAllSavedJobs() {
        return jobRepository.findAll();
    }
    

    
}
