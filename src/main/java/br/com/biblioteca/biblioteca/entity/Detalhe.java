package br.com.biblioteca.biblioteca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_detalhe_livro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Detalhe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer numPaginas;

    @Column(length = 100)
    private String editora;

    @Column(length = 20)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    public Detalhe(Integer numPaginas, String editora, String isbn, String resumo) {
        this.numPaginas = numPaginas;
        this.editora = editora;
        this.isbn = isbn;
        this.resumo = resumo;
    }
}
