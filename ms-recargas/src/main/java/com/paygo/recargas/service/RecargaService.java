package com.paygo.recargas.service;
import com.paygo.recargas.client.TarjetaClient;
import com.paygo.recargas.dto.RecargaMessage;
import com.paygo.recargas.dto.TarjetaResponse;
import com.paygo.recargas.entity.Recarga;
import com.paygo.recargas.messaging.RecargaProducer;
import com.paygo.recargas.messaging.RecargaRabbitProducer;
import com.paygo.recargas.repository.RecargaRepository;
import feign.FeignException;
import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class RecargaService {
    private static final Logger log = LoggerFactory.getLogger(RecargaService.class);
    private final RecargaRepository recargaRepository;
    private final TarjetaClient tarjetaClient;
    private final RecargaProducer recargaProducer;
    private final RecargaRabbitProducer recargaRabbitProducer;
    private final String messagingMode;
    public RecargaService(
            RecargaRepository recargaRepository,
            TarjetaClient tarjetaClient,
            RecargaProducer recargaProducer,
            RecargaRabbitProducer recargaRabbitProducer,
            @Value("${app.messaging.mode:both}") String messagingMode) {
        this.recargaRepository = recargaRepository;
        this.tarjetaClient = tarjetaClient;
        this.recargaProducer = recargaProducer;
        this.recargaRabbitProducer = recargaRabbitProducer;
        this.messagingMode = messagingMode;
    }
    public Recarga registrar(Recarga recarga) {
        if (recarga.getIdTarjeta() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idTarjeta es obligatorio");
        }
        if (recarga.getMontoRecarga() == null || recarga.getMontoRecarga() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montoRecarga debe ser mayor a 0");
        }
        TarjetaResponse tarjeta;
        try {
            tarjeta = tarjetaClient.buscarPorId(recarga.getIdTarjeta());
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarjeta no existe");
        } catch (RetryableException | FeignException.ServiceUnavailable e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de tarjetas no disponible");
        }
        recarga.setSaldoDisponible(
                tarjeta.getSaldoDisponible()
        );
        recarga.setFechaRecarga(
                LocalDateTime.now()
        );
        Recarga guardada = recargaRepository.save(recarga);
        RecargaMessage event = new RecargaMessage(
                guardada.getIdRecarga(),
                guardada.getIdTarjeta(),
                guardada.getSaldoDisponible(),
                guardada.getMontoRecarga(),
                guardada.getFechaRecarga()
        );
        String mode = messagingMode == null ? "both" : messagingMode.toLowerCase();
        if (mode.equals("kafka") || mode.equals("both")) {
            try {
                recargaProducer.publish(event);
            } catch (Exception e) {
                log.error("[ms-recargas] Fallo publicacion Kafka idRecarga={}: {}", guardada.getIdRecarga(), e.toString());
                if (mode.equals("kafka")) throw e;
            }
        }
        if (mode.equals("rabbit") || mode.equals("both")) {
            try {
                recargaRabbitProducer.publish(event);
            } catch (Exception e) {
                log.error("[ms-recargas] Fallo publicacion Rabbit idRecarga={}: {}", guardada.getIdRecarga(), e.toString());
                if (mode.equals("rabbit")) throw e;
            }
        }
        return guardada;
    }
    public List<Recarga> listar() {
        return recargaRepository.findAll();
    }
}
