package com.eurekabank.bpm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Evitamos que RestTemplate lance excepción por defecto en errores 4xx y 5xx
        // para poder manejarlos manualmente en el Delegate y extraer el JSON de error.
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError();
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // No lanzamos excepción aquí, dejamos que el Delegate capture la respuesta y extraiga el cuerpo JSON
            }
        });
        return restTemplate;
    }
}
