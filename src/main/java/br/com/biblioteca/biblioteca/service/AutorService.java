package br.com.biblioteca.biblioteca.service;

import br.com.biblioteca.biblioteca.dto.AutorDTO;
import br.com.biblioteca.biblioteca.entity.Autor;
import br.com.biblioteca.biblioteca.exception.ResourceNotFoundException;
import br.com.biblioteca.biblioteca.repository.AutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutorService {

    private final AutorRepository autorRepository;

    @Transactional(readOnly = true)
    public List<AutorDTO> listarTodos() {
        return autorRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AutorDTO buscarPorId(Long id) {
        Autor autor = autorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor não encontrado com ID: " + id));
        return toDTO(autor);
    }

    @Transactional
    public AutorDTO criar(AutorDTO dto) {
        Autor autor = new Autor(dto.getNome());
        return toDTO(autorRepository.save(autor));
    }

    @Transactional
    public AutorDTO atualizar(Long id, AutorDTO dto) {
        Autor autor = autorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor não encontrado com ID: " + id));
        autor.setNome(dto.getNome());
        return toDTO(autorRepository.save(autor));
    }

    @Transactional
    public void excluir(Long id) {
        Autor autor = autorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor não encontrado com ID: " + id));
        autorRepository.delete(autor);
    }

    private AutorDTO toDTO(Autor a) {
        AutorDTO dto = new AutorDTO();
        dto.setId(a.getId());
        dto.setNome(a.getNome());
        return dto;
    }
}
