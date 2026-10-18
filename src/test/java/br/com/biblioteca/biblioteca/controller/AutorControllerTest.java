package br.com.biblioteca.biblioteca.controller;

import tools.jackson.databind.ObjectMapper;
import br.com.biblioteca.biblioteca.dto.AutorDTO;
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
class AutorControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    private AutorDTO novoAutor(String nome) {
        AutorDTO dto = new AutorDTO();
        dto.setNome(nome);
        return dto;
    }

    @Test
    void deveCriarAutor() throws Exception {
        mvc.perform(post("/api/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoAutor("Machado de Assis"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Machado de Assis"));
    }

    @Test
    void deveRetornar400QuandoNomeVazio() throws Exception {
        mvc.perform(post("/api/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosValidacao.nome").exists());
    }

    @Test
    void deveListarAutores() throws Exception {
        mvc.perform(post("/api/autores").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoAutor("Clarice Lispector"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/autores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveRetornar404QuandoIdInexistente() throws Exception {
        mvc.perform(get("/api/autores/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarAutor() throws Exception {
        String resp = mvc.perform(post("/api/autores").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoAutor("Autor Original"))))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(resp).get("id").asLong();

        mvc.perform(put("/api/autores/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoAutor("Autor Novo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Autor Novo"));
    }

    @Test
    void deveExcluirAutor() throws Exception {
        String resp = mvc.perform(post("/api/autores").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(novoAutor("Temp"))))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(resp).get("id").asLong();

        mvc.perform(delete("/api/autores/" + id))
                .andExpect(status().isNoContent());
    }
}
