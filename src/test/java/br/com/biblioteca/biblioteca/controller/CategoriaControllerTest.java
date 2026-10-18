package br.com.biblioteca.biblioteca.controller;

import tools.jackson.databind.ObjectMapper;
import br.com.biblioteca.biblioteca.dto.CategoriaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoriaControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    private CategoriaDTO novaCategoria(String nome) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNome(nome);
        return dto;
    }

    @Test
    void deveCriarCategoria() throws Exception {
        mvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novaCategoria("Ficção"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Ficção"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void deveRetornar400QuandoNomeVazio() throws Exception {
        mvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosValidacao.nome").exists());
    }

    @Test
    void deveListarCategorias() throws Exception {
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novaCategoria("Romance"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nome").value("Romance"));
    }

    @Test
    void deveRetornar404QuandoIdInexistente() throws Exception {
        mvc.perform(get("/api/categorias/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveAtualizarCategoria() throws Exception {
        String resp = mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novaCategoria("Original"))))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(resp).get("id").asLong();

        mvc.perform(put("/api/categorias/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novaCategoria("Atualizada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Atualizada"));
    }

    @Test
    void deveExcluirCategoria() throws Exception {
        String resp = mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novaCategoria("Temp"))))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(resp).get("id").asLong();

        mvc.perform(delete("/api/categorias/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/categorias/" + id))
                .andExpect(status().isNotFound());
    }
}
