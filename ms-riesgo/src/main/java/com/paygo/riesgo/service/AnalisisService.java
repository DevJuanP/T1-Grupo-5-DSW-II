package com.paygo.riesgo.service;

import com.paygo.riesgo.dto.RecargaMessage;
import com.paygo.riesgo.entity.Analisis;
import com.paygo.riesgo.repository.AnalisisRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

// Regla del examen: Aprobada si el monto no excede el 70% del saldo disponible,
// Observada si lo supera. Espejo de ProductService (lógica de negocio en @Service).
@Service
public class AnalisisService {

    private final AnalisisRepository analisisRepository;

    public AnalisisService(AnalisisRepository analisisRepository) {
        this.analisisRepository = analisisRepository;
    }

    public Analisis registrar(RecargaMessage message) {
        return evaluar(message);
    }

    // Idempotente por idRecarga: el modo dual (Kafka + Rabbit) entrega el mismo
    // evento dos veces. Sin este guard, la segunda insercion fallaria por PK
    // duplicada. Se conserva la regla del examen: Aprobada <= 70%, Observada > 70%.
    // OJO: se usa findById (entidad inicializada), NO getReferenceById (proxy
    // lazy que revienta con LazyInitializationException en el log del consumer).
    public Analisis evaluar(RecargaMessage message) {
        var existente = analisisRepository.findById(message.idRecarga());
        if (existente.isPresent()) {
            return existente.get();
        }
        String situacion = message.montoRecarga() <= 0.7 * message.saldoDisponible()
                ? "Aprobada"
                : "Observada";
        try {
            return analisisRepository.save(new Analisis(
                    message.idRecarga(),
                    message.idTarjeta(),
                    message.saldoDisponible(),
                    message.montoRecarga(),
                    message.fechaRecarga(),
                    situacion
            ));
        } catch (DataIntegrityViolationException e) {
            // Carrera Kafka vs Rabbit: el otro consumer inserto primero.
            return analisisRepository.findById(message.idRecarga()).orElseThrow(() -> e);
        }
    }

    public List<Analisis> listar() {
        return analisisRepository.findAll();
    }
}
