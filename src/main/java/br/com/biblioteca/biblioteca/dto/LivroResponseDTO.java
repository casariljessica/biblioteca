package br.com.biblioteca.biblioteca.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LivroResponseDTO {

    private Long id;
    private String titulo;
    private BigDecimal preco;
    private Integer quantidade;
    private CategoriaDTO categoria;
    private DetalheDTO detalhe;
    private Set<AutorDTO> autores = new HashSet<>();
}
