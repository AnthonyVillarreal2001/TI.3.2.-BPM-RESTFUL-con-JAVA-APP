package com.eurekabank.bpm.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("auditoriaDelegate")
public class PersistirAuditoriaDelegate implements JavaDelegate {
    
    private static final Logger log = LoggerFactory.getLogger(PersistirAuditoriaDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String tipoOperacion = (String) execution.getVariable("tipoOperacion");
        Integer httpStatus = (Integer) execution.getVariable("httpStatus");
        
        String resultado = "EXITO";
        if (execution.hasVariable("mensajeError") && execution.getVariable("mensajeError") != null) {
            resultado = (String) execution.getVariable("mensajeError");
        }
        
        log.info("==========================================");
        log.info("         REGISTRO DE AUDITORÍA BPM        ");
        log.info("==========================================");
        log.info("Fecha       : {}", LocalDateTime.now());
        log.info("Operación   : {}", tipoOperacion);
        log.info("Status HTTP : {}", httpStatus);
        log.info("Resultado   : {}", resultado);
        log.info("==========================================");
        
        // Aquí se puede persistir en la BDD del Sistema de Auditoría
    }
}
