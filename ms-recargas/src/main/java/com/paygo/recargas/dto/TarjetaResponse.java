package com.paygo.recargas.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarjetaResponse {
    private Long idTarjeta;
    private String nomTitular;
    private Double saldoAsignado;
    private Double saldoDisponible;
}