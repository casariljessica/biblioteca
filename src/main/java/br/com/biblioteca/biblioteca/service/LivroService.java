package br.com.biblioteca.biblioteca.service;

import br.com.biblioteca.biblioteca.dto.*;
import br.com.biblioteca.biblioteca.entity.*;
import br.com.biblioteca.biblioteca.exception.ResourceNotFoundException;
import br.com.biblioteca.biblioteca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LivroService {

    private final LivroRepository livroRepository;
    private final CategoriaRepository categoriaRepository;
    private final AutorRepository autorRepository;

    @Transactional(readOnly = true)
    public List<LivroResponseDTO> listarTodos() {
        return livroRepository.findAllWithRelations().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LivroResponseDTO buscarPorId(Long id) {
        Livro livro = livroRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + id));
        return toResponseDTO(livro);
    }

    @Transactional
    public LivroResponseDTO criar(LivroRequestDTO dto) {
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + dto.getCategoriaId()));

        Set<Autor> autores = buscarAutores(dto.getAutoresIds());

        Livro livro = new Livro();
        livro.setTitulo(dto.getTitulo());
        livro.setPreco(dto.getPreco());
        livro.setQuantidade(dto.getQuantidade());
        livro.setCategoria(categoria);
        livro.setDetalhe(toDetalhe(dto.getDetalhe()));
        livro.setAutores(autores);

        return toResponseDTO(livroRepository.save(livro));
    }

    @Transactional
    public LivroResponseDTO atualizar(Long id, LivroRequestDTO dto) {
        Livro livro = livroRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + id));

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + dto.getCategoriaId()));

        Set<Autor> autores = buscarAutores(dto.getAutoresIds());

        livro.setTitulo(dto.getTitulo());
        livro.setPreco(dto.getPreco());
        livro.setQuantidade(dto.getQuantidade());
        livro.setCategoria(categoria);

        if (livro.getDetalhe() != null) {
            Detalhe d = livro.getDetalhe();
            d.setNumPaginas(dto.getDetalhe().getNumPaginas());
            d.setEditora(dto.getDetalhe().getEditora());
            d.setIsbn(dto.getDetalhe().getIsbn());
            d.setResumo(dto.getDetalhe().getResumo());
        } else {
            livro.setDetalhe(toDetalhe(dto.getDetalhe()));
        }

        livro.getAutores().clear();
        livro.getAutores().addAll(autores);

        return toResponseDTO(livroRepository.save(livro));
    }

    @Transactional
    public void excluir(Long id) {
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + id));
        livro.getAutores().clear();
        livroRepository.delete(livro);
    }

    private Set<Autor> buscarAutores(Set<Long> autoresIds) {
        return autoresIds.stream()
                .map(autorId -> autorRepository.findById(autorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Autor não encontrado com ID: " + autorId)))
                .collect(Collectors.toSet());
    }

    private Detalhe toDetalhe(DetalheDTO dto) {
        Detalhe detalhe = new Detalhe();
        detalhe.setNumPaginas(dto.getNumPaginas());
        detalhe.setEditora(dto.getEditora());
        detalhe.setIsbn(dto.getIsbn());
        detalhe.setResumo(dto.getResumo());
        return detalhe;
    }

    private LivroResponseDTO toResponseDTO(Livro l) {
        LivroResponseDTO response = new LivroResponseDTO();
        response.setId(l.getId());
        response.setTitulo(l.getTitulo());
        response.setPreco(l.getPreco());
        response.setQuantidade(l.getQuantidade());

        CategoriaDTO catDTO = new CategoriaDTO();
        catDTO.setId(l.getCategoria().getId());
        catDTO.setNome(l.getCategoria().getNome());
        response.setCategoria(catDTO);

        if (l.getDetalhe() != null) {
            DetalheDTO detDTO = new DetalheDTO();
            detDTO.setId(l.getDetalhe().getId());
            detDTO.setNumPaginas(l.getDetalhe().getNumPaginas());
            detDTO.setEditora(l.getDetalhe().getEditora());
            detDTO.setIsbn(l.getDetalhe().getIsbn());
            detDTO.setResumo(l.getDetalhe().getResumo());
            response.setDetalhe(detDTO);
        }

        Set<AutorDTO> autorDTOs = l.getAutores().stream()
                .map(a -> {
                    AutorDTO ad = new AutorDTO();
                    ad.setId(a.getId());
                    ad.setNome(a.getNome());
                    return ad;
                })
                .collect(Collectors.toSet());
        response.setAutores(autorDTOs);

        return response;
    }
}
