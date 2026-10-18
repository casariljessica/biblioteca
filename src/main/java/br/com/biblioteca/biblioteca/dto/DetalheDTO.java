package br.com.biblioteca.biblioteca.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalheDTO {

    private Long id;

    @NotNull(message = "O número de páginas é obrigatório")
    @Min(value = 1, message = "O livro deve ter no mínimo 1 página")
    private Integer numPaginas;

    @NotBlank(message = "A editora é obrigatória")
    @Size(max = 100, message = "A editora deve ter no máximo 100 caracteres")
    private String editora;

    @NotBlank(message = "O ISBN é obrigatório")
    @Size(max = 20, message = "O ISBN deve ter no máximo 20 caracteres")
    private String isbn;

    @Size(max = 2000, message = "O resumo deve ter no máximo 2000 caracteres")
    private String resumo;
}
