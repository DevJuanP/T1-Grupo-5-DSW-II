package com.paygo.recargas.service;
import com.paygo.recargas.client.TarjetaClient;
import com.paygo.recargas.dto.TarjetaResponse;
import com.paygo.recargas.entity.Recarga;
import com.paygo.recargas.repository.RecargaRepository;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class RecargaService {
    private final RecargaRepository recargaRepository;
    private final TarjetaClient tarjetaClient;
    public RecargaService(
            RecargaRepository recargaRepository,
            TarjetaClient tarjetaClient) {
        this.recargaRepository = recargaRepository;
        this.tarjetaClient = tarjetaClient;
    }
    public Recarga registrar(Recarga recarga) {
        TarjetaResponse tarjeta;
        try {
            tarjeta = tarjetaClient.buscarPorId(recarga.getIdTarjeta());
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarjeta no existe");
        }
        recarga.setSaldoDisponible(
                tarjeta.getSaldoDisponible()
        );
        recarga.setFechaRecarga(
                LocalDateTime.now()
        );
        return recargaRepository.save(recarga);
    }
    public List<Recarga> listar() {
        return recargaRepository.findAll();
    }
}
