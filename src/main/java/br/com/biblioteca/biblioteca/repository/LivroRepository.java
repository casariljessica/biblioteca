package br.com.biblioteca.biblioteca.repository;

import br.com.biblioteca.biblioteca.entity.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {

    @Query("SELECT DISTINCT l FROM Livro l LEFT JOIN FETCH l.categoria LEFT JOIN FETCH l.detalhe LEFT JOIN FETCH l.autores")
    List<Livro> findAllWithRelations();

    @Query("SELECT l FROM Livro l LEFT JOIN FETCH l.categoria LEFT JOIN FETCH l.detalhe LEFT JOIN FETCH l.autores WHERE l.id = :id")
    Optional<Livro> findByIdWithRelations(@Param("id") Long id);
}
