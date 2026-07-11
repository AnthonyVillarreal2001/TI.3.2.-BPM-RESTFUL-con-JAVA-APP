package com.eurekabank.bpm.delegates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component("procesarTransferenciaDelegate")
public class ProcesarTransferenciaDelegate implements JavaDelegate {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${eurekabank.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // 1. Obtener variables del proceso BPM
        String cuentaOrigen = (String) execution.getVariable("cuentaOrigen");
        String cuentaDestino = (String) execution.getVariable("cuentaDestino");
        Double monto = (Double) execution.getVariable("monto");

        // 2. Primero validamos saldo (GET)
        String saldoUrl = apiBaseUrl + "/api/v1/cuentas/" + cuentaOrigen + "/saldo";
        ResponseEntity<String> saldoResponse = restTemplate.getForEntity(saldoUrl, String.class);
        
        if (saldoResponse.getStatusCode().isError()) {
            manejarErrorHttp(execution, saldoResponse);
        }

        JsonNode saldoJson = objectMapper.readTree(saldoResponse.getBody());
        Double saldoActual = saldoJson.get("saldo").asDouble();

        if (saldoActual < monto) {
            // Regla de negocio: Saldo insuficiente. Lanzamos BPMN Error
            execution.setVariable("httpStatus", 422);
            execution.setVariable("mensajeError", "Saldo insuficiente en la cuenta de origen.");
            throw new BpmnError("API_ERROR"); 
        }

        // 3. Proceder con el POST de la transferencia
        String transferenciaUrl = apiBaseUrl + "/api/v1/transacciones/transferencia";
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("cuentaOrigen", cuentaOrigen);
        payload.put("cuentaDestino", cuentaDestino);
        payload.put("monto", monto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // Invocar API
        ResponseEntity<String> response = restTemplate.postForEntity(transferenciaUrl, request, String.class);

        // 4. Evaluar resultado y Manejo de Errores HTTP
        if (response.getStatusCode().isError()) {
            manejarErrorHttp(execution, response);
        } else {
            // Operación Exitosa
            JsonNode respuestaExitosa = objectMapper.readTree(response.getBody());
            execution.setVariable("resultadoOperacion", respuestaExitosa.toString());
            execution.setVariable("httpStatus", response.getStatusCode().value());
        }
    }

    private void manejarErrorHttp(DelegateExecution execution, ResponseEntity<String> response) throws Exception {
        execution.setVariable("httpStatus", response.getStatusCode().value());
        
        try {
            JsonNode errorJson = objectMapper.readTree(response.getBody());
            String mensaje = errorJson.has("mensaje") ? errorJson.get("mensaje").asText() : "Error desconocido en API";
            execution.setVariable("mensajeError", mensaje);
        } catch (Exception e) {
            execution.setVariable("mensajeError", "Error procesando la respuesta del servidor.");
        }
        
        // Desencadena el Boundary Error Event en Camunda
        throw new BpmnError("API_ERROR");
    }
}
