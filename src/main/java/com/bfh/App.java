package com.bfh;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class App {

    record GenerateReq(String name, String regNo, String email) {
    }

    record GenerateRes(String webhook, String accessToken) {
    }

    record SubmitReq(String finalQuery) {
    }

    private static final String GENERATE_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    private static final String FINAL_SQL = """
            SELECT
              p.AMOUNT AS SALARY,
              CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
              TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE,
              d.DEPARTMENT_NAME
            FROM PAYMENTS p
            JOIN EMPLOYEE e   ON e.EMP_ID = p.EMP_ID
            JOIN DEPARTMENT d ON d.DEPARTMENT_ID = e.DEPARTMENT
            WHERE DAY(p.PAYMENT_TIME) <> 1
            ORDER BY p.AMOUNT DESC, p.PAYMENT_TIME DESC
            LIMIT 1;
            """;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    ApplicationRunner run() {
        return args -> {
            WebClient http = WebClient.create();

            GenerateRes gen = http.post().uri(GENERATE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new GenerateReq(
                            "Chepuri Sri Venkata Adithya",
                            "22bce9605",
                            "chsvadithya123@gmail.com"))
                    .retrieve()
                    .bodyToMono(GenerateRes.class)
                    .block();

            if (gen == null)
                throw new RuntimeException("No response from generate API");

            String response = http.post().uri(gen.webhook())
                    .header(HttpHeaders.AUTHORIZATION, gen.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SubmitReq(FINAL_SQL))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Submission response: " + response);
        };
    }
}