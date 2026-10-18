package br.com.biblioteca.biblioteca.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutorDTO {

    private Long id;

    @NotBlank(message = "O nome do autor é obrigatório")
    private String nome;
}
