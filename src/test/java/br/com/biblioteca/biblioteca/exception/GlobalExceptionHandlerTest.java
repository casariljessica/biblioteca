package br.com.biblioteca.biblioteca.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @Test
    void deveRetornarStatus404QuandoResourceNotFoundExceptionLancada() {
        when(request.getRequestURI()).thenReturn("/api/livros/999");

        ResourceNotFoundException ex = new ResourceNotFoundException("Livro não encontrado com ID: 999");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Recurso não encontrado", response.getBody().getError());
        assertEquals("Livro não encontrado com ID: 999", response.getBody().getMessage());
        assertEquals("/api/livros/999", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
        assertNull(response.getBody().getErrosValidacao());
    }

    @Test
    void deveRetornarStatus400ComDetalhesDosCamposQuandoErroDeValidacao() {
        when(request.getRequestURI()).thenReturn("/api/livros");
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);

        FieldError fieldError1 = new FieldError("livroRequestDTO", "titulo", "O título é obrigatório");
        FieldError fieldError2 = new FieldError("livroRequestDTO", "preco", "O preço deve ser maior que zero");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(methodArgumentNotValidException, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Erro de validação", response.getBody().getError());
        assertEquals("Um ou mais campos contêm erros de validação", response.getBody().getMessage());
        assertEquals("/api/livros", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getErrosValidacao());
        assertEquals(2, response.getBody().getErrosValidacao().size());
        assertEquals("O título é obrigatório", response.getBody().getErrosValidacao().get("titulo"));
        assertEquals("O preço deve ser maior que zero", response.getBody().getErrosValidacao().get("preco"));
    }

    @Test
    void deveRetornarStatus409QuandoDataIntegrityViolationExceptionLancada() {
        when(request.getRequestURI()).thenReturn("/api/categorias/1");

        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolationException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Violação de integridade", response.getBody().getError());
        assertEquals("/api/categorias/1", response.getBody().getPath());
    }

    @Test
    void deveRetornarStatus400QuandoHttpMessageNotReadableExceptionLancada() {
        when(request.getRequestURI()).thenReturn("/api/livros");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON", (org.springframework.http.HttpInputMessage) null);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadableException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Requisição inválida", response.getBody().getError());
        assertEquals("/api/livros", response.getBody().getPath());
    }
}
