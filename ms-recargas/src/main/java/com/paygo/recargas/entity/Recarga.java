package com.paygo.recargas.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Entity
@Table(name = "recargas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recarga {

    @Id
    @NotNull(message = "idRecarga es obligatorio")
    private Long idRecarga;
    @NotNull(message = "idTarjeta es obligatorio")
    private Long idTarjeta;
    private Double saldoDisponible;
    @NotNull(message = "montoRecarga es obligatorio")
    @Positive(message = "montoRecarga debe ser mayor a 0")
    private Double montoRecarga;
    private LocalDateTime fechaRecarga;
}