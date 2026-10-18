package br.com.biblioteca.biblioteca.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import br.com.biblioteca.biblioteca.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LivroControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    private Long criarCategoria(String nome) throws Exception {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNome(nome);
        String resp = mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resp).get("id").asLong();
    }

    private Long criarAutor(String nome) throws Exception {
        AutorDTO dto = new AutorDTO();
        dto.setNome(nome);
        String resp = mvc.perform(post("/api/autores").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resp).get("id").asLong();
    }

    private LivroRequestDTO novoLivroReq(Long catId, Long autId) {
        LivroRequestDTO dto = new LivroRequestDTO();
        dto.setTitulo("Dom Casmurro");
        dto.setPreco(new java.math.BigDecimal("49.90"));
        dto.setQuantidade(10);
        dto.setCategoriaId(catId);
        dto.setAutoresIds(Set.of(autId));
        DetalheDTO det = new DetalheDTO();
        det.setNumPaginas(256);
        det.setEditora("Editora Ática");
        det.setIsbn("978-85-08-00000-1");
        det.setResumo("Clássico da literatura brasileira.");
        dto.setDetalhe(det);
        return dto;
    }

    @Test
    void deveCriarLivroComRelacionamentos() throws Exception {
        Long catId = criarCategoria("Romance");
        Long autId = criarAutor("Machado de Assis");

        mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoLivroReq(catId, autId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Dom Casmurro"))
                .andExpect(jsonPath("$.categoria.nome").value("Romance"))
                .andExpect(jsonPath("$.detalhe.editora").value("Editora Ática"))
                .andExpect(jsonPath("$.autores[0].nome").value("Machado de Assis"));
    }

    @Test
    void deveRetornar404QuandoCategoriaNaoExiste() throws Exception {
        Long autId = criarAutor("Autor");
        mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoLivroReq(99999L, autId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Categoria")));
    }

    @Test
    void deveRetornar404QuandoAutorNaoExiste() throws Exception {
        Long catId = criarCategoria("Cat");
        mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoLivroReq(catId, 99999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Autor")));
    }

    @Test
    void deveRetornar400QuandoTituloVazio() throws Exception {
        Long catId = criarCategoria("Cat");
        Long autId = criarAutor("Aut");
        LivroRequestDTO dto = novoLivroReq(catId, autId);
        dto.setTitulo("");

        mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosValidacao.titulo").exists());
    }

    @Test
    void deveRetornar400QuandoAutoresVazio() throws Exception {
        Long catId = criarCategoria("Cat");
        LivroRequestDTO dto = novoLivroReq(catId, catId);
        dto.setAutoresIds(Set.of());

        mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosValidacao.autoresIds").exists());
    }

    @Test
    void deveListarLivrosComRelacionamentos() throws Exception {
        Long catId = criarCategoria("Romance");
        Long autId = criarAutor("Machado de Assis");
        mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoLivroReq(catId, autId))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/livros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].categoria.nome").value("Romance"));
    }

    @Test
    void deveBuscarLivroPorId() throws Exception {
        Long catId = criarCategoria("Cat");
        Long autId = criarAutor("Aut");
        String resp = mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoLivroReq(catId, autId))))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(resp).get("id").asLong();

        mvc.perform(get("/api/livros/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Dom Casmurro"));
    }

    @Test
    void deveRetornar404QuandoLivroNaoExiste() throws Exception {
        mvc.perform(get("/api/livros/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarLivro() throws Exception {
        Long catId = criarCategoria("Cat");
        Long autId = criarAutor("Aut");
        String resp = mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoLivroReq(catId, autId))))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(resp).get("id").asLong();

        LivroRequestDTO dto = novoLivroReq(catId, autId);
        dto.setTitulo("Memórias Póstumas de Brás Cubas");

        mvc.perform(put("/api/livros/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Memórias Póstumas de Brás Cubas"));
    }

    @Test
    void deveAtualizarDetalheDoLivroExistente() throws Exception {
        Long catId = criarCategoria("Cat");
        Long autId = criarAutor("Aut");
        String resp = mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoLivroReq(catId, autId))))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(resp);
        Long id = node.get("id").asLong();
        Long detalheId = node.get("detalhe").get("id").asLong();

        LivroRequestDTO dto = novoLivroReq(catId, autId);
        dto.setTitulo("Livro Atualizado");
        dto.getDetalhe().setEditora("Nova Editora");
        dto.getDetalhe().setNumPaginas(300);

        String respUpdate = mvc.perform(put("/api/livros/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Livro Atualizado"))
                .andExpect(jsonPath("$.detalhe.editora").value("Nova Editora"))
                .andReturn().getResponse().getContentAsString();

        JsonNode updateNode = mapper.readTree(respUpdate);
        org.junit.jupiter.api.Assertions.assertEquals(detalheId, updateNode.get("detalhe").get("id").asLong(),
                "O ID do detalhe deve ser o mesmo após atualização (não criar novo)");
    }

    @Test
    void deveExcluirLivroSemExcluirAutoresECategorias() throws Exception {
        Long catId = criarCategoria("Cat");
        Long autId = criarAutor("Aut");
        String resp = mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoLivroReq(catId, autId))))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(resp).get("id").asLong();

        mvc.perform(delete("/api/livros/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/livros/" + id))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/autores/" + autId))
                .andExpect(status().isOk());

        mvc.perform(get("/api/categorias/" + catId))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar400QuandoJSONInvalido() throws Exception {
        mvc.perform(post("/api/livros").contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }
}
