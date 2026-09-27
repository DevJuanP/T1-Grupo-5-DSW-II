package com.paygo.tarjetas.service;
import com.paygo.tarjetas.entity.Tarjeta;
import com.paygo.tarjetas.repository.TarjetaRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TarjetaService {

    private final TarjetaRepository tarjetaRepository;
    public TarjetaService(TarjetaRepository tarjetaRepository) {
        this.tarjetaRepository = tarjetaRepository;
    }
    public Tarjeta registrar(Tarjeta tarjeta) {
        return tarjetaRepository.save(tarjeta);
    }
    public List<Tarjeta> listar() {
        return tarjetaRepository.findAll();
    }
    public Optional<Tarjeta> buscarPorId(Long id) {
        return tarjetaRepository.findById(id);
    }
}