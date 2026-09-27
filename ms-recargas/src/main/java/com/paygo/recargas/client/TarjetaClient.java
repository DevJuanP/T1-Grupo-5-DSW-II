package com.paygo.recargas.client;
import com.paygo.recargas.dto.TarjetaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "ms-tarjetas",
        url = "${tarjetas.service.url}"
)
public interface TarjetaClient {
    @GetMapping("/tarjetas/{id}")
    TarjetaResponse buscarPorId(@PathVariable("id") Long id);
}