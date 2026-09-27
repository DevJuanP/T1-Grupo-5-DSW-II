package com.paygo.riesgo.service;

import com.paygo.riesgo.dto.RecargaMessage;
import com.paygo.riesgo.entity.Analisis;
import com.paygo.riesgo.repository.AnalisisRepository;
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
        String situacion = message.montoRecarga() <= 0.7 * message.saldoDisponible()
                ? "Aprobada"
                : "Observada";
        return analisisRepository.save(new Analisis(
                message.idRecarga(),
                message.idTarjeta(),
                message.saldoDisponible(),
                message.montoRecarga(),
                message.fechaRecarga(),
                situacion
        ));
    }

    public List<Analisis> listar() {
        return analisisRepository.findAll();
    }
}
