package br.com.biblioteca.biblioteca.repository;

import br.com.biblioteca.biblioteca.entity.Detalhe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalheRepository extends JpaRepository<Detalhe, Long> {
}
