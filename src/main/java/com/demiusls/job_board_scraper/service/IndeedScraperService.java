package com.demiusls.job_board_scraper.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import com.demiusls.job_board_scraper.interfaces.JobScraper;
import com.demiusls.job_board_scraper.model.JobPost;
import com.demiusls.job_board_scraper.model.SearchCriteria;

import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class IndeedScraperService implements JobScraper {

    @Override
    public String getProviderName() {
        return "Indeed";
    }

    @Override
    public List<JobPost> fetchJobs(SearchCriteria criteria) {
        List<JobPost> foundJobs = new ArrayList<>();

        // 1. Setup di WebDriverManager (Prepara il driver per Chrome)
        WebDriverManager.chromedriver().setup();

        // 2. Configura le opzioni del browser
        ChromeOptions options = new ChromeOptions();
        // Modalità headless per non aprire la finestra del browser (opzionale ma consigliato per scraping)
        options.addArguments("--headless=new"); 
        
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        // User-Agent falso per non sembrare un bot:
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, come Gecko) Chrome/122.0.0.0 Safari/537.36");

        // 3. Inizializza il browser
        WebDriver driver = new ChromeDriver(options);

        try {
            // 4. Costruisci l'URL in base ai criteri (sostituisci spazi con '+')
            String location = criteria.location() != null ? criteria.location().replace(" ", "+") : "";
            String title = criteria.title() != null ? criteria.title().replace(" ", "+") : "";
            String url = String.format("https://it.indeed.com/jobs?q=%s&l=%s", title, location);
            
            System.out.println("Selenium sta navigando su: " + url);
            
            // 5. Apri la pagina
            driver.get(url);

            // 6. Aspetta finché i risultati non si caricano (massimo 10 secondi)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // Indeed di solito usa l'ID 'mosaic-provider-jobcards' per contenere la lista dei lavori
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("mosaic-provider-jobcards")));

            // 7. Trova tutte le singole "schede" dei lavori sulla pagina
            List<WebElement> jobCards = driver.findElements(By.cssSelector("div.job_seen_beacon"));

            for (WebElement card : jobCards) {
                try {
                    // Estraiamo il titolo
                    WebElement titleElement = card.findElement(By.cssSelector("h2.jobTitle span"));
                    String jobTitle = titleElement.getText();

                    // Estraiamo l'azienda
                    WebElement companyElement = card.findElement(By.cssSelector("span[data-testid='company-name']"));
                    String company = companyElement.getText();

                    // Estraiamo il link (attributo href)
                    WebElement linkElement = card.findElement(By.cssSelector("h2.jobTitle a"));
                    String link = linkElement.getAttribute("href");

                    // Raccogliamo la location (avvolta in un try-catch perché a volte manca)
                    String jobLocation = location; 
                    try {
                        WebElement locationElement = card.findElement(By.cssSelector("div[data-testid='text-location']"));
                        jobLocation = locationElement.getText();
                    } catch (Exception e) {}

                    // 8. Costruiamo l'oggetto e lo aggiungiamo alla lista
                    JobPost job = new JobPost();
                    job.setTitle(jobTitle);
                    job.setCompany(company);
                    job.setLocation(jobLocation);
                    job.setLink(link);
                    job.setDescription("Scaricato tramite Selenium da Indeed");
                    job.setProvider(getProviderName());

                    foundJobs.add(job);
                } catch (Exception e) {
                    
                }
            }

        } catch (Exception e) {
            System.err.println("Errore generale durante lo scraping di Indeed: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        return foundJobs;
    }
}
