package com.paygo.recargas.repository;
import com.paygo.recargas.entity.Recarga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RecargaRepository extends JpaRepository<Recarga, Long> {
}