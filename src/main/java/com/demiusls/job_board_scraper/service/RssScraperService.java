package com.demiusls.job_board_scraper.service;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
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

    private static final String str = "/active-jb-24h?limit=10&offset=0&title_filter=%22Data%20Engineer%22&location_filter=Italy&description_type=text&seniority_filter=Entry%20level";


    public void fetchAndSaveJobs() {

        String apiUrl = "https://" + apiHost + str;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("x-rapidapi-key", apiKey)
                    .header("x-rapidapi-host", apiHost)
                    .GET()
                    .build();
            // Invio della richiesta
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            // Parsing del JSON
            JsonNode root = objectMapper.readTree(response.body());
            

            if (root.isArray()) {
                for (JsonNode node : root) {
                    
                    String link = node.path("url").asString(); 
                    if (!link.isEmpty() && !jobRepository.existsByLink(link)) {
                        JobPost job = new JobPost();
                        job.setTitle(node.path("title").asString());
                        job.setLink(link);
                        job.setCompany(node.path("organization").asString());
                        job.setDescription(node.path("description_text").asString());
                        if (node.path("locations_derived").isArray() && !node.path("locations_derived").isEmpty()) {
                            job.setLocation(node.path("locations_derived").get(0).asString());
                        } else {
                            job.setLocation(node.path("location").asString());
                        }
                        String dateStr = node.path("date_posted").asString();
                        if (dateStr != null && !dateStr.isEmpty()) {
                            job.setPublishDate(LocalDateTime.parse(dateStr));
                        }

                        jobRepository.save(job);
                        System.out.println("Nuovo lavoro salvato: " + job.getTitle() + " presso " + job.getCompany());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Errore durante la chiamata API: " + e.getMessage());
        }
    }

    // Metodo per il Controller
    public List<JobPost> getAllSavedJobs() {
        return jobRepository.findAll();
    }
    

    
}
