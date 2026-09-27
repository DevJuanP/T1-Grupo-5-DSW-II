package com.paygo.recargas.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private Long idRecarga;
    private Long idTarjeta;
    private Double saldoDisponible;
    private Double montoRecarga;
    private LocalDateTime fechaRecarga;
}