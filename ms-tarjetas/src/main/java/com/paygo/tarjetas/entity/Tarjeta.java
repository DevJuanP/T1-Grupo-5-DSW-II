package com.paygo.tarjetas.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tarjetas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tarjeta {

    @Id
    @NotNull(message = "idTarjeta es obligatorio")
    private Long idTarjeta;
    @NotBlank(message = "nomTitular es obligatorio")
    private String nomTitular;
    @NotNull(message = "saldoAsignado es obligatorio")
    @PositiveOrZero(message = "saldoAsignado no puede ser negativo")
    private Double saldoAsignado;
    @NotNull(message = "saldoDisponible es obligatorio")
    @PositiveOrZero(message = "saldoDisponible no puede ser negativo")
    private Double saldoDisponible;
}