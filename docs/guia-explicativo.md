# Guia Explicativo — Projeto Biblioteca

Este guia foi escrito para quem está começando com Java e Spring e quer entender, linha a linha, como o projeto Biblioteca funciona. Todas as informações foram confirmadas na leitura do código-fonte e, quando possível, na execução real da aplicação (requisições somente leitura ao banco).

> **Antes de ler:** se você nunca usou Spring Boot, comece pela seção [10. Conceitos de Java e Spring para estudar](#10-conceitos-de-java-e-spring-para-estudar) e volte ao início. Este guia acompanha o README; lá estão as instruções de instalação e execução.

---

## 1. O que este projeto faz

É uma API REST de biblioteca com três recursos principais:

- **Livros** — título, preço, quantidade em estoque, uma categoria, um ou mais autores e um bloco de "detalhes" (número de páginas, editora, ISBN, resumo).
- **Autores** — apenas nome.
- **Categorias** — apenas nome.

Além da API, existe uma interface web HTML/CSS/JavaScript que consome a API e permite fazer tudo pela tela (criar, editar, excluir e visualizar em tabelas).

Não existem: busca, filtros, paginação, login ou cadastro de usuários.

---

## 2. Estrutura do projeto e responsabilidade de cada camada

```
biblioteca/
├── pom.xml                          ← dependências e configuração do Maven
├── mvnw / mvnw.cmd / .mvn/          ← Maven Wrapper (não é preciso instalar Maven)
├── README.md                        ← apresentação e instruções de uso
├── docs/
│   └── guia-explicativo.md          ← este arquivo
└── src/
    ├── main/
    │   ├── java/br/com/biblioteca/biblioteca/
    │   │   ├── BibliotecaApplication.java   ← ponto de entrada (main)
    │   │   ├── controller/                  ← recebe requisições HTTP
    │   │   ├── service/                     ← regras de negócio e transações
    │   │   ├── repository/                  ← acesso ao banco (Spring Data JPA)
    │   │   ├── entity/                      ← classes JPA (tabelas do banco)
    │   │   ├── dto/                         ← objetos de entrada/saída da API
    │   │   └── exception/                   ← erros e tratamento global
    │   └── resources/
    │       ├── application.properties       ← configuração (banco, Hibernate)
    │       └── static/                      ← frontend (index.html, app.js, styles.css)
    └── test/java/br/com/biblioteca/biblioteca/   ← testes automatizados
```

Cada pacote tem um papel claro (padrão em camadas — *layers*):

| Camada (pasta) | Arquivos | Responsabilidade | Análoga a... |
|---|---|---|---|
| `controller` | `LivroController`, `AutorController`, `CategoriaController` | Expor rotas HTTP, receber JSON, chamar o service e devolver a resposta com o status adequado. **Não contém regra de negócio.** | "recepcionista" da aplicação |
| `service` | `LivroService`, `AutorService`, `CategoriaService` | Regras de negócio: validar se categoria/autores existem, montar entidades a partir dos DTOs, definir transações. | "gerente" que toma decisões |
| `repository` | `LivroRepository`, `AutorRepository`, `CategoriaRepository`, `DetalheRepository` | Interfaces do Spring Data JPA. Falam com o banco. O Spring gera a implementação em tempo de execução. | "caixa do banco de dados" |
| `entity` | `Livro`, `Autor`, `Categoria`, `Detalhe` | Classes Java mapeadas para tabelas (`@Entity`). Representam os dados persistidos. | "modelo" do dados |
| `dto` | `LivroRequestDTO`, `LivroResponseDTO`, `AutorDTO`, `CategoriaDTO`, `DetalheDTO` | Formato do JSON que entra e sai da API. Desacoplam a API do banco (a entidade nunca é exposta diretamente). | "envelope" do correio |
| `exception` | `ResourceNotFoundException`, `ErrorResponse`, `GlobalExceptionHandler` | Tratamento centralizado de erros: qualquer exceção lançada vira uma resposta JSON padronizada. | "central de atendimento de erros" |

**Por que separar assim?** Se amanhã o banco mudar (ou você trocar de banco), só a camada de persistência e as entidades mudam; se a API mudar o formato do JSON, só os DTOs mudam; o restante continua funcionando.

---

## 3. Fluxo de uma requisição: Controller → Service → Repository → banco

Vamos seguir uma requisição real, passo a passo: **criar um livro** via `POST /api/livros`.

### Passo 1 — O cliente envia o JSON

No Postman (ou no formulário da interface web):

```json
{
  "titulo": "Dom Casmurro",
  "preco": 49.90,
  "quantidade": 10,
  "categoriaId": 40,
  "autoresIds": [37],
  "detalhe": {
    "numPaginas": 256,
    "editora": "Record",
    "isbn": "9788508000001",
    "resumo": "Clássico da literatura brasileira."
  }
}
```

### Passo 2 — O Spring encontra o Controller

A classe `LivroController` está anotada com:

```java
@RestController                      // "esta classe atende requisições HTTP e devolve JSON"
@RequestMapping("/api/livros")       // todo método dela começa em /api/livros
public class LivroController {

    @PostMapping                     // POST /api/livros cai aqui
    public ResponseEntity<LivroResponseDTO> criar(@Valid @RequestBody LivroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(livroService.criar(dto));
    }
}
```

- `@RequestBody` mapeia o JSON para um objeto `LivroRequestDTO`.
- `@Valid` **antes** do service ativa a validação (seção 5). Se algo estiver errado, o Spring lança `MethodArgumentNotValidException` e o `GlobalExceptionHandler` responde `400` — o método `criar` nem chega a ser executado.

### Passo 3 — O Controller delega ao Service

`LivroService.criar(dto)` faz o trabalho pesado:

1. Busca a categoria pelo `categoriaId` no `CategoriaRepository`. Se não existir → lança `ResourceNotFoundException("Categoria não encontrada com ID: ...")`.
2. Busca cada autor do `autoresIds` no `AutorRepository`. Se algum não existir → mesma exceção, com a mensagem do autor.
3. Cria a entidade `Livro`, preenche os campos, cria a entidade `Detalhe` e associa categoria e autores.
4. Chama `livroRepository.save(livro)` — o Hibernate insere o registro (e o detalhe, por causa da cascata) no banco.
5. Converte a entidade salva para `LivroResponseDTO` (incluindo o `id` gerado) e devolve.

### Passo 4 — O Repository conversa com o banco

`LivroRepository` é uma interface:

```java
public interface LivroRepository extends JpaRepository<Livro, Long> { ... }
```

Ao estender `JpaRepository<Livro, Long>`, o Spring Data **implementa automaticamente** métodos como `findAll()`, `findById(id)`, `save(entidade)` e `delete(entidade)`. Você escreve a interface; o Spring escreve o código SQL. Por isso não existe nenhuma classe "LivroRepositoryImpl" no projeto.

O SQL correspondente aparece no console da aplicação porque `application.properties` tem `spring.jpa.show-sql=true`.

### Passo 5 — A resposta volta

O controller envolve o DTO no `ResponseEntity`:

- Sucesso na criação → `201 Created` com o JSON do livro (com `id` preenchido).
- Qualquer exceção não tratada no caminho → capturada pelo `GlobalExceptionHandler` (seção 5) e convertida em JSON de erro com o status apropriado.

### Diagrama do fluxo

```
Cliente (Postman / app.js)
        │  POST /api/livros (JSON)
        ▼
┌───────────────────┐   @Valid aciona a validação do DTO
│  LivroController  │──────────────▶ 400 (erro de validação, se houver)
└─────────┬─────────┘
          │ livroService.criar(dto)
          ▼
┌───────────────────┐   não achou categoria/autor?
│   LivroService    │──────────────▶ ResourceNotFoundException → 404
└─────────┬─────────┘
          │ categoriaRepository.findById(...) / autorRepository.findById(...) / livroRepository.save(...)
          ▼
┌───────────────────┐
│ LivroRepository   │   (Spring Data gera o SQL)
└─────────┬─────────┘
          ▼
     PostgreSQL   (tb_livro, tb_categoria, tb_autor, tb_detalhe_livro, tb_livro_autor)
```

---

## 4. Entidades e relacionamentos

### 4.1 As quatro entidades

**`Livro`** (`tb_livro`) — a entidade central:

```java
@Entity
public class Livro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)      private String titulo;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal preco;
    @Column(nullable = false)                    private Integer quantidade;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY) @JoinColumn(name = "detalhe_id", unique = true)
    private Detalhe detalhe;

    @ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "tb_livro_autor", ...)
    private Set<Autor> autores = new HashSet<>();
}
```

**`Autor`** (`tb_autor`) e **`Categoria`** (`tb_categoria`) — simples: `id` + `nome` (obrigatório; 150 e 100 caracteres respectivamente).

**`Detalhe`** (`tb_detalhe_livro`) — `numPaginas` (Integer), `editora` (até 100), `isbn` (até 20) e `resumo` (texto longo, coluna `TEXT`). No banco, essas colunas são anuláveis; a obrigatoriedade existe apenas na validação do DTO (seção 5).

### 4.2 Os três tipos de relacionamento

```
Categoria 1 ──────< Livro >────── 1 Detalhe
                    │
                    │ N:N (tabela tb_livro_autor)
                    ▼
                  Autor
```

| Relacionamento | Anotação | Significado | Exemplo prático |
|---|---|---|---|
| Livro → Categoria | `@ManyToOne` | Vários livros apontam para a mesma categoria. FK obrigatória (`nullable = false`). | "Dom Casmurro" e "O Cortiço" podem ser ambos "Romance". |
| Livro → Detalhe | `@OneToOne` | Cada livro tem no máximo um detalhe (e vice-versa); a coluna `detalhe_id` tem restrição de unicidade. `cascade = ALL` significa: ao salvar/apagar o livro, o detalhe é salvo/apagado junto. | Não há endpoint próprio de Detalhe; ele entra e sai junto com o livro. |
| Livro ↔ Autor | `@ManyToMany` | Um livro tem vários autores; um autor pode ter vários livros. A tabela de junção `tb_livro_autor` guarda os pares (`livro_id`, `autor_id`). | Machado de Assis assina vários livros; um livro coletivo tem vários autores. |

### 4.3 O schema real no banco (verificado no PostgreSQL)

| Tabela | Colunas | Restrições |
|---|---|---|
| `tb_categoria` | `id` (bigint identity), `nome` (varchar 100, not null) | PK em `id` |
| `tb_autor` | `id` (bigint identity), `nome` (varchar 150, not null) | PK em `id` |
| `tb_detalhe_livro` | `id` (bigint identity), `editora` (varchar 100), `isbn` (varchar 20), `num_paginas` (integer), `resumo` (text) | PK em `id` |
| `tb_livro` | `id` (bigint identity), `titulo` (varchar 200, not null), `preco` (numeric(10,2), not null), `quantidade` (integer, not null), `categoria_id` (not null), `detalhe_id` | PK em `id`; FK `categoria_id` → `tb_categoria`; FK `detalhe_id` → `tb_detalhe_livro`; **unique** em `detalhe_id` |
| `tb_livro_autor` | `livro_id`, `autor_id` | PK composta (`livro_id`, `autor_id`); FKs para `tb_livro` e `tb_autor` |

Essas FKs têm efeito prático: tentar excluir uma categoria que ainda está vinculada a um livro gera violação de integridade no banco, que a aplicação converte em HTTP `409` (seção 5.4).

### 4.4 `FetchType.LAZY` e as queries com `JOIN FETCH`

Todos os relacionamentos usam `fetch = FetchType.LAZY` (carregamento "preguiçoso"): o Hibernate só busca categoria/autores/detalhe quando você acessá-los. Se o service simplesmente fizesse `findAll()`, cada livro acessado dispararia novas queries para a categoria (o famoso problema **N+1**).

Para resolver isso, `LivroRepository` tem duas queries JPQL escritas à mão:

```java
@Query("SELECT DISTINCT l FROM Livro l LEFT JOIN FETCH l.categoria LEFT JOIN FETCH l.detalhe LEFT JOIN FETCH l.autores")
List<Livro> findAllWithRelations();

@Query("SELECT l FROM Livro l LEFT JOIN FETCH l.categoria LEFT JOIN FETCH l.detalhe LEFT JOIN FETCH l.autores WHERE l.id = :id")
Optional<Livro> findByIdWithRelations(@Param("id") Long id);
```

- `JOIN FETCH` traz tudo em uma única consulta com junções.
- `DISTINCT` no `findAll` evita linhas duplicadas causadas pelo cruzamento com a coleção de autores (o Hibernate ainda deduplica a coleção em memória).
- `findByIdWithRelations` é usado nos métodos de busca e atualização; o `excluir` usa o `findById` simples de propósito (só precisa do `id` e da coleção de autores para limpá-la antes de apagar).

> **Observação:** `DetalheRepository` existe no projeto, mas nenhuma classe o injeta ou utiliza — o detalhe é gerenciado junto com o livro, via cascata.

---

## 5. DTOs, validações e tratamento global de exceções

### 5.1 Por que existem DTOs separados das entidades

As entidades JPA carregam detalhes internos (proxy do Hibernate, coleções) e podem ter campos que não devem ser expostos. Os DTOs definem exatamente o contrato da API:

| DTO | Uso | Campos |
|---|---|---|
| `AutorDTO` | entra (request) e sai (response) de Autores | `id`, `nome` |
| `CategoriaDTO` | entra e sai de Categorias; também aparece embutido na resposta de Livros | `id`, `nome` |
| `DetalheDTO` | entra embutido no request de Livro e sai embutido no response | `id`, `numPaginas`, `editora`, `isbn`, `resumo` |
| `LivroRequestDTO` | **apenas entrada** em POST/PUT de Livros | `titulo`, `preco`, `quantidade`, `categoriaId`, `autoresIds`, `detalhe` |
| `LivroResponseDTO` | **apenas saída** de Livros | `id`, `titulo`, `preco`, `quantidade`, `categoria`, `detalhe`, `autores` |

Repare na assimetria do Livro: o request manda **IDs** (`categoriaId: 40`, `autoresIds: [37]`) e a resposta devolve **objetos completos** (`"categoria": {"id": 40, "nome": "Romance"}`). O `id` do `DetalheDTO` é ignorado quando você envia (o servidor cria ou atualiza o detalhe) e é retornado na resposta.

Os services contêm os métodos de conversão (`toDTO`, `toResponseDTO`, `toDetalhe`) que mapeiam entidade ⇄ DTO — é código manual, sem bibliotecas de mapeamento.

### 5.2 As validações (Bean Validation)

As anotações ficam nos DTOs e são ativadas pelos `@Valid` nos parâmetros do controller.

**`AutorDTO` / `CategoriaDTO`:**

| Campo | Anotação | Mensagem retornada |
|---|---|---|
| `nome` | `@NotBlank` | "O nome do autor é obrigatório" / "O nome da categoria é obrigatório" |

**`LivroRequestDTO`:**

| Campo | Anotação | Mensagem |
|---|---|---|
| `titulo` | `@NotBlank`, `@Size(max = 200)` | "O título é obrigatório" / "O título deve ter no máximo 200 caracteres" |
| `preco` | `@NotNull`, `@DecimalMin("0.01")` | "O preço é obrigatório" / "O preço deve ser maior que zero" |
| `quantidade` | `@NotNull`, `@Min(0)` | "A quantidade é obrigatória" / "A quantidade em estoque não pode ser negativa" |
| `categoriaId` | `@NotNull` | "A categoria é obrigatória" |
| `autoresIds` | `@NotEmpty` | "O livro deve ter pelo menos um autor associado" |
| `detalhe` | `@NotNull` + `@Valid` | "Os detalhes do livro são obrigatórios" — o `@Valid` faz o cascate para dentro do `DetalheDTO` |

**`DetalheDTO`:**

| Campo | Anotação | Mensagem |
|---|---|---|
| `numPaginas` | `@NotNull`, `@Min(1)` | "O número de páginas é obrigatório" / "O livro deve ter no mínimo 1 página" |
| `editora` | `@NotBlank`, `@Size(max = 100)` | "A editora é obrigatória" / "A editora deve ter no máximo 100 caracteres" |
| `isbn` | `@NotBlank`, `@Size(max = 20)` | "O ISBN é obrigatório" / "O ISBN deve ter no máximo 20 caracteres" |
| `resumo` | `@Size(max = 2000)` (opcional) | "O resumo deve ter no máximo 2000 caracteres" |

Quando há erros, o Spring lança `MethodArgumentNotValidException` e o handler devolve o mapa campo → mensagem (verificado em execução real):

```json
{
  "error": "Erro de validação",
  "errosValidacao": {
    "autoresIds": "O livro deve ter pelo menos um autor associado",
    "detalhe.numPaginas": "O livro deve ter no mínimo 1 página",
    "detalhe.isbn": "O ISBN é obrigatório",
    "detalhe.editora": "A editora é obrigatória"
  },
  "message": "Um ou mais campos contêm erros de validação",
  "path": "/api/livros",
  "status": 400,
  "timestamp": "2026-10-19T23:40:32.253718Z"
}
```

Perceba os campos aninhados com ponto (`detalhe.editora`) e que, havendo vários erros no mesmo campo, fica registrado apenas o primeiro (`putIfAbsent` no handler).

### 5.3 A ordem importa: validação antes da regra de negócio

Para `POST /api/livros`, a ordem de verificação é:

1. **Validação do DTO** (`@Valid`) → 400 se algum campo estiver inválido (nada é consultado no banco).
2. **Existência da categoria** → 404 se o `categoriaId` não existir.
3. **Existência dos autores** → 404 se algum ID de autor não existir.

Por isso, um JSON com título vazio E categoria inexistente retorna **400** (a validação falha primeiro).

### 5.4 Tratamento global de exceções (`GlobalExceptionHandler`)

A classe `@RestControllerAdvice` intercepta exceções lançadas em qualquer controller e monta o corpo `ErrorResponse`:

| Exceção capturada | HTTP | `error` | Quando acontece |
|---|---|---|---|
| `ResourceNotFoundException` | `404` | "Recurso não encontrado" | Livro/autor/categoria não encontrado por ID; categoria ou autor de um Livro inexistente |
| `MethodArgumentNotValidException` | `400` | "Erro de validação" | Falha nas anotações de validação (corpo com `errosValidacao`) |
| `DataIntegrityViolationException` | `409` | "Violação de integridade" | O banco recusou a operação (FK ou unicidade) — ex.: excluir categoria/em autor ainda vinculado a livros |
| `HttpMessageNotReadableException` | `400` | "Requisição inválida" | JSON malformado ou tipo de campo incompatível |

`ErrorResponse` tem os campos `timestamp`, `status`, `error`, `message`, `path` e `errosValidacao` (só presente em erro de validação; os nulos são omitidos da resposta). Exemplo real de 404, verificado com a aplicação em execução:

```json
{
  "error": "Recurso não encontrado",
  "message": "Livro não encontrado com ID: 99999",
  "path": "/api/livros/99999",
  "status": 404,
  "timestamp": "2026-10-19T23:40:06.352452Z"
}
```

O `timestamp` vem de `Instant.now()` e é serializado em **UTC**, formato ISO-8601 com microssegundos. A ordem das chaves do JSON não faz parte do contrato e pode variar.

---

## 6. Endpoints e métodos principais

Os três controllers têm exatamente a mesma estrutura de 5 operações. Abaixo, o que cada método do **service** faz internamente.

### 6.1 Livros — `/api/livros` (`LivroController` → `LivroService`)

| HTTP | Rota | Método do service | O que faz | Status de sucesso |
|---|---|---|---|---|
| GET | `/api/livros` | `listarTodos()` | `findAllWithRelations()` (uma query com JOIN FETCH) e converte cada entidade para `LivroResponseDTO`. | 200 |
| GET | `/api/livros/{id}` | `buscarPorId(id)` | `findByIdWithRelations(id)`; se vazio, lança `ResourceNotFoundException`. | 200 / 404 |
| POST | `/api/livros` | `criar(dto)` | Valida categoria e autores, monta `Livro` + `Detalhe` e salva (cascata persiste o detalhe; a tabela `tb_livro_autor` recebe os pares). | 201 / 400 / 404 |
| PUT | `/api/livros/{id}` | `atualizar(id, dto)` | Carrega o livro com relações; substitui título, preço, quantidade e categoria; **atualiza o detalhe existente** mantendo o mesmo `id` (se o livro não tiver detalhe, cria um novo); troca os autores via `clear()` + `addAll()`. | 200 / 400 / 404 |
| DELETE | `/api/livros/{id}` | `excluir(id)` | Limpa a coleção de autores (remove as linhas da tabela de junção) e deleta o livro. Autores e categorias **não** são apagados. | 204 / 404 |

Detalhes importantes, confirmados pelos testes:

- Atualizar um livro **não** cria um novo `Detalhe`: o teste `deveAtualizarDetalheDoLivroExistente` garante que o `id` do detalhe permanece o mesmo.
- Excluir um livro **não** exclui seus autores nem sua categoria (teste `deveExcluirLivroSemExcluirAutoresECategorias`).

### 6.2 Autores — `/api/autores` e Categorias — `/api/categorias`

Mesmo padrão (`AutorService` / `CategoriaService`):

| Método do service | O que faz |
|---|---|
| `listarTodos()` | `findAll()` + conversão para DTO (`@Transactional(readOnly = true)`) |
| `buscarPorId(id)` | `findById(id)` ou `ResourceNotFoundException` |
| `criar(dto)` | `new Autor(dto.getNome())` / `new Categoria(dto.getNome())` e `save()` — o `id` do request é ignorado |
| `atualizar(id, dto)` | Busca, altera apenas o `nome` e salva |
| `excluir(id)` | Busca e `delete()` — se houver FK pendente (ex.: categoria usada por livro), o banco gera `DataIntegrityViolationException` → 409 |

### 6.3 Transações

Todos os métodos dos services são anotados com `@Transactional` (escritas) ou `@Transactional(readOnly = true)` (leituras). Isso garante que cada operação seja atômica: ou tudo é gravado, ou nada é — e as leituras otimizam o acesso.

---

## 7. Comunicação entre frontend e API

O frontend está em `src/main/resources/static/` e é HTML/CSS/JavaScript puro, sem frameworks e sem etapa de build. O Spring Boot serve esses arquivos automaticamente em `http://localhost:8080/`.

### 7.1 Como o `app.js` conversa com a API

```javascript
const API = '';   // caminhos relativos — frontend e API na mesma origem
```

- **Helper `apiFetch(url, options)`**: adiciona `Content-Type: application/json`, trata o status `204` (sucesso sem corpo), tenta ler o JSON e, em caso de erro, devolve `{ sucesso: false, erro: data }`. Falha de rede vira a mensagem "Falha de conexão com a API".
- **`extrairErro(erro)`**: se a resposta tem `errosValidacao`, junta as mensagens com `; `; senão mostra `message`. É assim que a interface exibe, por exemplo, "O título é obrigatório".
- **Caches**: `categoriasCache`, `autoresCache` e `livrosCache` guardam as últimas listas recebidas para preencher tabelas e `<select>` sem novas requisições.

### 7.2 O que acontece em cada ação da tela

| Ação na interface | Requisição |
|---|---|
| Carregar a página (`carregarLivros()` no fim do `app.js`) | `GET /api/livros` |
| Abrir "+ Novo Livro" | `GET /api/categorias` + `GET /api/autores` (para preencher os selects) |
| Salvar livro novo | `POST /api/livros` com `{titulo, preco, quantidade, categoriaId, autoresIds, detalhe}` |
| Salvar livro existente | `PUT /api/livros/{id}` com o mesmo formato |
| Editar livro | apenas preenche o formulário a partir do `livrosCache` (requisição só ao salvar) |
| Excluir livro/categoria/autor | `DELETE /api/.../{id}` (com `confirm()` antes) |
| Salvar/excluir categoria | `POST/PUT/DELETE /api/categorias...` e recarrega a lista |

O formulário de livro envia os autores como `autoresIds` (IDs inteiros lidos do `<select multiple>`) e o detalhe como objeto aninhado; `resumo` vazio é enviado como `null` (campo opcional).

> **Comportamento observado (e merece atenção):** no carregamento da página, só `carregarLivros()` é chamado. As abas "Categorias" e "Autores" ficam vazias até que o usuário abra o formulário de livro (que dispara a carga) ou faça alguma operação de criação/edição/exclusão nelas. Ou seja: a tabela só se preenche depois que os dados são buscados — não há busca automática ao clicar na aba. Não é um problema da API; é o fluxo escolhido no `app.js`.

---

## 8. Testando a API no Postman

### 8.1 Configuração inicial

1. Inicie a aplicação (`./mvnw spring-boot:run`) com o PostgreSQL rodando.
2. Em cada requisição, configure:
   - **Body → raw → JSON**
   - Header `Content-Type: application/json` (o Postman adiciona automaticamente ao escolher JSON)
3. Não há autenticação: nenhum token ou header extra é necessário.
4. Base: `http://localhost:8080`

> Os números de `id` dos exemplos variam conforme os dados já existentes no seu banco. Os exemplos abaixo usam IDs observados em uma execução real (`categoria 40`, `autor 37`); ajuste conforme as respostas que você receber.

### 8.2 Roteiro recomendado (nesta ordem)

Um livro precisa de uma categoria e de um autor já cadastrados, então comece por eles.

---

#### 1) Criar categoria — `POST http://localhost:8080/api/categorias`

Body:

```json
{
  "nome": "Romance"
}
```

Resposta esperada — `201 Created`:

```json
{
  "id": 40,
  "nome": "Romance"
}
```

**Cenário de erro (400):** `{"nome": ""}` →

```json
{
  "error": "Erro de validação",
  "errosValidacao": { "nome": "O nome da categoria é obrigatório" },
  "message": "Um ou mais campos contêm erros de validação",
  "path": "/api/categorias",
  "status": 400,
  "timestamp": "2026-10-19T23:40:32.205829Z"
}
```

*(resposta real capturada; o mesmo formato vale para `POST /api/autores` com "O nome do autor é obrigatório")*

---

#### 2) Criar autor — `POST http://localhost:8080/api/autores`

Body:

```json
{
  "nome": "Machado de Assis"
}
```

Resposta — `201 Created`:

```json
{
  "id": 37,
  "nome": "Machado de Assis"
}
```

---

#### 3) Criar livro — `POST http://localhost:8080/api/livros`

Body (use os IDs que você acabou de criar):

```json
{
  "titulo": "Dom Casmurro",
  "preco": 49.90,
  "quantidade": 10,
  "categoriaId": 40,
  "autoresIds": [37],
  "detalhe": {
    "numPaginas": 256,
    "editora": "Record",
    "isbn": "9788508000001",
    "resumo": "Clássico da literatura brasileira."
  }
}
```

Resposta esperada — `201 Created` (formato verificado em execução real via `GET /api/livros`):

```json
{
  "id": 19,
  "titulo": "Dom Casmurro",
  "preco": 49.90,
  "quantidade": 10,
  "categoria": { "id": 40, "nome": "Romance" },
  "detalhe": {
    "id": 19,
    "numPaginas": 256,
    "editora": "Record",
    "isbn": "9788508000001",
    "resumo": "Clássico da literatura brasileira."
  },
  "autores": [ { "id": 37, "nome": "Machado de Assis" } ]
}
```

Ou, com vários autores, `autoresIds` aceita vários IDs:

```json
"autoresIds": [37, 38]
```

> Atenção: o campo `autores` é um `Set` (conjunto) do Java — a **ordem dos autores na resposta não é garantida**.

**Cenários de erro deste mesmo endpoint (todos verificados em execução real):**

| Situação | Status | Detalhe |
|---|---|---|
| `titulo: ""` | 400 | `"errosValidacao": { "titulo": "O título é obrigatório" }` |
| `categoriaId: 99999` (não existe) | 404 | `"message": "Categoria não encontrada com ID: 99999"` |
| `autoresIds: [99999]` | 404 | `"message": "Autor não encontrado com ID: 99999"` |
| `autoresIds: []` | 400 | `"errosValidacao": { "autoresIds": "O livro deve ter pelo menos um autor associado" }` |
| `detalhe.numPaginas: 0`, `editora: ""`, `isbn: ""` | 400 | várias mensagens em `errosValidacao`, com chaves aninhadas `detalhe.*` |
| `detalhe` omitido ou `null` | 400 | `"errosValidacao": { "detalhe": "Os detalhes do livro são obrigatórios" }` |
| `preco: 0` | 400 | `"errosValidacao": { "preco": "O preço deve ser maior que zero" }` |
| `quantidade: -1` | 400 | `"errosValidacao": { "quantidade": "A quantidade em estoque não pode ser negativa" }` |
| JSON malformado (`{invalid json`) | 400 | `"error": "Requisição inválida"`, `"message": "O corpo da requisição é inválido ou contém formato incorreto."` |

---

#### 4) Listar livros — `GET http://localhost:8080/api/livros`

Resposta — `200 OK` (real):

```json
[
  {
    "id": 19,
    "titulo": "Depois dos 15",
    "preco": 50.00,
    "quantidade": 2,
    "categoria": { "id": 40, "nome": "Romance" },
    "detalhe": { "id": 19, "numPaginas": 255, "editora": "Record", "isbn": "6585855888000", "resumo": null },
    "autores": [ { "id": 37, "nome": "Paula Pimenta" } ]
  }
]
```

Sem dados, retorna `[]`. Repare que `resumo` veio `null` porque não foi informado — o campo é opcional.

---

#### 5) Buscar livro por ID — `GET http://localhost:8080/api/livros/19`

- `200 OK` com o objeto do livro (mesmo formato acima).
- `404 Not Found` para ID inexistente:

```json
{
  "error": "Recurso não encontrado",
  "message": "Livro não encontrado com ID: 99999",
  "path": "/api/livros/99999",
  "status": 404,
  "timestamp": "2026-10-19T23:40:06.352452Z"
}
```

O mesmo vale para `GET /api/autores/{id}` e `GET /api/categorias/{id}` (mensagem com "Autor não encontrado com ID: ..." e "Categoria não encontrada com ID: ...").

---

#### 6) Atualizar livro — `PUT http://localhost:8080/api/livros/19`

Envie o corpo **completo** (o PUT substitui os dados; não existe PATCH parcial):

```json
{
  "titulo": "Dom Casmurro (edição especial)",
  "preco": 59.90,
  "quantidade": 5,
  "categoriaId": 40,
  "autoresIds": [37],
  "detalhe": {
    "numPaginas": 300,
    "editora": "Nova Editora",
    "isbn": "9788508000001",
    "resumo": "Edição revisada."
  }
}
```

Resposta — `200 OK`, com o mesmo formato do response de criação. O `detalhe.id` **continua o mesmo** de antes (o service atualiza a entidade existente em vez de criar outra) — comportamento garantido por teste automatizado.

**Erros possíveis:** `404` (livro, categoria ou autor inexistente) e os mesmos `400` de validação do POST.

---

#### 7) Excluir livro — `DELETE http://localhost:8080/api/livros/19`

- `204 No Content`, **sem corpo**.
- `404` se o ID não existir.
- Autores e categorias do livro permanecem no banco (confirmado por teste automatizado).

---

#### 8) Autores e Categorias — operações equivalentes

| Operação | Autor | Categoria |
|---|---|---|
| Listar | `GET /api/autores` | `GET /api/categorias` |
| Buscar | `GET /api/autores/{id}` | `GET /api/categorias/{id}` |
| Criar | `POST /api/autores` — `{"nome": "Clarice Lispector"}` → 201 | `POST /api/categorias` — `{"nome": "Ficção"}` → 201 |
| Atualizar | `PUT /api/autores/{id}` — `{"nome": "Novo Nome"}` → 200 | `PUT /api/categorias/{id}` → 200 |
| Excluir | `DELETE /api/autores/{id}` → 204 | `DELETE /api/categorias/{id}` → 204 |

**Cenário de 409 (integridade):** excluir uma categoria que ainda está associada a algum livro, ou um autor que ainda está em `tb_livro_autor`, viola uma restrição de chave estrangeira. O `GlobalExceptionHandler` traduz isso em:

```json
{
  "error": "Violação de integridade",
  "message": "Operação não permitida: o registro está vinculado a outros dados ou violou uma restrição de unicidade.",
  "path": "/api/categorias/40",
  "status": 409,
  "timestamp": "..."
}
```

> **Sinalização:** as restrições de FK foram confirmadas no banco e o handler de `409` existe e é testado unitariamente; porém esse cenário **não foi executado ponta a ponta** durante a elaboração deste guia (para não alterar dados). Se você testar no Postman, registre o resultado.

### 8.3 Resumo de status codes

| Código | Quando |
|---|---|
| `200` | GET, PUT com sucesso |
| `201` | POST com sucesso |
| `204` | DELETE com sucesso (corpo vazio) |
| `400` | Falha de validação (`errosValidacao`) ou JSON inválido |
| `404` | ID não encontrado (livro, autor, categoria) ou referência inexistente no request |
| `409` | Violação de integridade no banco (FK/unicidade) |

---

## 9. Testes automatizados

| Classe de teste | Estilo | Cobre |
|---|---|---|
| `LivroControllerTest` (12 testes) | `@SpringBootTest` + `MockMvc` + `@Transactional` | criar livro com relacionamentos; 404 para categoria/autor/livro inexistente; 400 para título vazio, autores vazio e JSON inválido; listar com relacionamentos; buscar por ID; atualizar livro e manter o `id` do detalhe; excluir sem excluir autores/categorias |
| `AutorControllerTest` (6 testes) | idem | criar, listar, buscar, atualizar, excluir; 400 com nome vazio; 404 com ID inexistente |
| `CategoriaControllerTest` (6 testes) | idem | mesmos cenários de Categoria |
| `GlobalExceptionHandlerTest` (4 testes) | Unitário com **Mockito** (`@ExtendWith(MockitoExtension.class)`) | garante status, `error`, `message`, `path` e `errosValidacao` para 404, 400 (validação), 409 e 400 (JSON inválido) |
| `BibliotecaApplicationTests` (1 teste) | `@SpringBootTest` | apenas garante que o contexto Spring carrega |

Como executar:

```bash
./mvnw test                              # todos
./mvnw test -Dtest=LivroControllerTest   # uma classe
```

Observações:

- Os testes de controller **usam o PostgreSQL real** (mesmo `application.properties`; não há perfil de teste nem H2). O banco precisa estar acessível.
- A anotação `@Transactional` nas classes de teste faz o Maven/Spring reverter cada teste ao final — os dados do banco não ficam alterados.
- Os testes usam `tools.jackson.databind.ObjectMapper` (a biblioteca Jackson 3, empacotada no Spring Boot 4) para montar e ler JSON.
- Em `target/surefire-reports` ficam os relatórios XML/HTML de cada execução.

---

## 10. Conceitos de Java e Spring para estudar

Um caminho de estudos que espelha a ordem em que o código os usa:

### Java (fundamentos)

1. **POO**: classes, objetos, atributos, métodos, construtores, encapsulamento (`Livro`, `Autor`...).
2. **Anotações** (`@Entity`, `@Override`): metadados lidos por frameworks.
3. **`List`, `Set`, `HashSet`**: por que `autores` é um `Set<Autor>` (sem duplicatas, sem ordem garantida).
4. **`Optional`**: `findById` devolve `Optional<Autor>` — o método `orElseThrow` escolhe entre valor ou exceção.
5. **Streams e `Collectors`**: `findAll().stream().map(this::toDTO).collect(Collectors.toList())` é o padrão de conversão repetido nos services.
6. **`BigDecimal`** para dinheiro (`preco`): evita erros de ponto flutuante.
7. **Herança de interfaces e generics**: `interface LivroRepository extends JpaRepository<Livro, Long>`.
8. **Exceções**: `throw`/`try-catch`, exceções checked vs unchecked (`RuntimeException`).
9. **Lombok**: as anotações `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor` e `@EqualsAndHashCode(of = "id")` geram código durante a compilação — por isso não existem métodos `getNome()` escritos à mão.

### Spring e ecossistema

1. **Inversão de Controle (IoC) e injeção de dependência**: os services recebem os repositories "de graça" via `@RequiredArgsConstructor` + campos `final`; o Spring monta os objetos (beans).
2. **`@SpringBootApplication` e o arranque da aplicação**: o que o `main` faz e o que é autoconfigurado.
3. **Spring MVC**: `@RestController`, `@RequestMapping`, `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`, `@PathVariable`, `@RequestBody`, `ResponseEntity` e os códigos HTTP.
4. **Spring Data JPA**: interfaces `JpaRepository`, métodos derivados (`findById`, `save`, `delete`) e **queries JPQL** com `@Query`, `@Param` e `JOIN FETCH` (problema N+1).
5. **JPA/Hibernate**: mapeamento objeto-relacional — `@Entity`, `@Table`, `@Id`, `@GeneratedValue(IDENTITY)`, `@Column`, `@ManyToOne`, `@OneToOne`, `@ManyToMany`, `@JoinTable`, `FetchType.LAZY` e `CascadeType.ALL`.
6. **Bean Validation**: `@Valid`, `@NotBlank`, `@NotNull`, `@NotEmpty`, `@Min`, `@Size`, `@DecimalMin` e como o cascate com `@Valid` valida objetos aninhados (`detalhe`).
7. **Tratamento de exceções**: `@RestControllerAdvice`, `@ExceptionHandler` e por que um handler global evita `try-catch` em cada controller.
8. **Transações**: `@Transactional` (leitura vs escrita) e rollback automático quando uma exceção estoura.
9. **DTO vs entidade**: por que a API nunca expõe a entidade JPA diretamente.
10. **Jackson (serialização)**: como objetos Java viram JSON e o que significa `Content-Type: application/json`.
11. **Configuração do Spring Boot**: `application.properties`, `ddl-auto` (`update`, `none`, `validate`), `show-sql`, portas e perfis.
12. **Maven**: `pom.xml`, dependências, plugins, o wrapper (`./mvnw`) e os ciclos `compile`, `test`, `package`.
13. **Testes**: JUnit 5, `MockMvc` (simula HTTP sem subir servidor), `@Transactional` em testes e **Mockito** (`@Mock`, `@InjectMocks`, `when(...).thenReturn(...)`).

### Frontend (o mínimo usado aqui)

- **DOM e eventos**: `document.getElementById`, `addEventListener('submit')`.
- **`fetch` e async/await** para chamar a API, incluindo leitura de resposta com status `204`.
- **Templates de string** para renderizar as linhas das tabelas.

---

## 11. Limitações e informações não confirmadas

Para transparência, o que **não existe** hoje (confirmado por análise do código):

- Busca, filtro, paginação e ordenação de livros/autores/categorias.
- Autenticação, autorização e configuração de CORS.
- Endpoints próprios para `Detalhe` (o `DetalheRepository` está declarado, mas não é usado).
- Variáveis de ambiente para configuração (credenciais do banco estão fixas no `application.properties`).

Informações que **não puderam ser confirmadas por completo** nesta análise:

- O cenário de `409` (excluir categoria/autor em uso) foi inferido das restrições de FK existentes no banco + handler testado unitariamente, mas não foi executado ponta a ponta.
- O comportamento da interface em navegadores diferentes do usado na verificação não foi testado (a análise do frontend é estática, pelo código).
- Compatibilidade com bancos diferentes de PostgreSQL e com outras portas/ambientes não foi testada (só existe a configuração única em `application.properties`).
