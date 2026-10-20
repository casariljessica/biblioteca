# Biblioteca

API REST para gestão de livros, autores e categorias, com interface web simples servida pela própria aplicação. O projeto é uma aplicação Spring Boot com Java 17, JPA/Hibernate e PostgreSQL, acompanhada de testes automatizados (JUnit 5 + MockMvc + Mockito).

Este README foi escrito a partir da análise do código-fonte e da execução real da aplicação. A documentação complementar, voltada para aprendizagem, está em [`docs/guia-explicativo.md`](docs/guia-explicativo.md).

## Funcionalidades implementadas

Confirmadas na análise dos controllers, services e testes:

- **CRUD completo de Livros, Autores e Categorias** (criar, listar, buscar por ID, atualizar e excluir).
- **Livro com relacionamentos**: categoria (obrigatória), um ou mais autores (obrigatório, N:N) e detalhe (páginas, editora, ISBN, resumo — 1:1 com cascata).
- **Validação de entrada** com mensagens em português (Bean Validation nos DTOs).
- **Tratamento global de erros** com corpo padronizado: 404 (recurso não encontrado), 400 (validação e JSON inválido) e 409 (violação de integridade).
- **Interface web estática** (HTML/CSS/JavaScript puro) com abas de Livros, Categorias e Autores, formulários e tabelas.
- **Testes automatizados**: 29 testes (12 de Livro, 6 de Autor, 6 de Categoria, 4 do tratamento global de exceções e 1 de contexto).

**Não implementado** (confirmado por análise do código): busca/filtração, paginação, ordenação, autenticação/autorização e endpoints para gerenciar Detalhe isoladamente (`DetalheRepository` existe, mas não é injetado em nenhum service).

## Tecnologias

Confirmadas pelo `pom.xml`:

| Componente | Detalhe |
|---|---|
| Linguagem | Java 17 (`<java.version>17</java.version>`); build e execução validados com OpenJDK 21 |
| Framework | Spring Boot 4.1.1 (parent `spring-boot-starter-parent`) |
| Web | `spring-boot-starter-webmvc` (Spring MVC, embute Apache Tomcat) |
| Persistência | `spring-boot-starter-data-jpa` (Spring Data JPA + Hibernate) |
| Banco | PostgreSQL (driver `org.postgresql:postgresql`) |
| Validação | `spring-boot-starter-validation` (Bean Validation) |
| Ferramentas | Lombok, `spring-boot-devtools` |
| Build | Maven Wrapper 3.3.4 (baixa Maven 3.10.0 automaticamente) |
| Testes | `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-validation-test` (JUnit 5, MockMvc, Mockito) |
| Frontend | HTML + CSS + JavaScript puro (`src/main/resources/static/`), sem frameworks nem Node.js |

## Pré-requisitos

- **JDK 17 ou superior** (o `pom.xml` compila para Java 17).
- **PostgreSQL** rodando localmente na porta **5432**.
- **Internet na primeira execução** do `./mvnw`, para baixar a distribuição Maven e as dependências. Não é preciso instalar Maven manualmente.

## Configurar o PostgreSQL

1. Com o PostgreSQL em execução, crie o banco `biblioteca` (uma única vez):

   ```sql
   CREATE DATABASE biblioteca;
   ```

2. A aplicação usa a configuração fixa de `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/biblioteca
   spring.datasource.username=seubancoaqui
   spring.datasource.password=suasenhaaqui
   spring.datasource.driver-class-name=org.postgresql.Driver
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true
   ```

   Ou seja: usuário e senha, porta 5432. **Esses valores estão hardcoded** — não há uso de variáveis de ambiente. Altere o `application.properties` antes de rodar.

3. As tabelas são criadas/atualizadas automaticamente pelo Hibernate (`ddl-auto=update`) na primeira execução: `tb_livro`, `tb_categoria`, `tb_autor`, `tb_detalhe_livro` e `tb_livro_autor` (tabela de junção do N:N). Não é necessário rodar scripts SQL de criação de schema.

## Executar a aplicação

Na raiz do projeto:

```bash
./mvnw spring-boot:run
```

Alternativas:

```bash
# Gerar o JAR e executar
./mvnw package -DskipTests
java -jar target/biblioteca-0.0.1-SNAPSHOT.jar
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

A aplicação sobe na **porta 8080** (nenhuma `server.port` configurada; é a porta padrão do Spring Boot). O log console mostra o SQL executado (`spring.jpa.show-sql=true`).

## Acessar a interface web e a API

| O quê | URL |
|---|---|
| Interface web | http://localhost:8080/ |
| API — Livros | http://localhost:8080/api/livros |
| API — Autores | http://localhost:8080/api/autores |
| API — Categorias | http://localhost:8080/api/categorias |

A interface é um HTML estático (`src/main/resources/static/index.html`) que chama a mesma API via `fetch`, sem proxy: as requisições usam caminhos relativos (ex.: `/api/livros`). Não há login; todos os endpoints são públicos.

## Endpoints

Todos esperam e retornam `Content-Type: application/json`. Não há autenticação.

### Livros — `/api/livros`

| Método | Rota | Sucesso | Erros possíveis |
|---|---|---|---|
| GET | `/api/livros` | `200` — lista de livros com categoria, detalhe e autores | — |
| GET | `/api/livros/{id}` | `200` — livro completo | `404` se o ID não existir |
| POST | `/api/livros` | `201` — livro criado | `400` (validação/JSON inválido), `404` (categoria ou autor inexistente) |
| PUT | `/api/livros/{id}` | `200` — livro atualizado (o detalhe existente é atualizado, não recriado) | `400`, `404` (livro, categoria ou autor inexistente) |
| DELETE | `/api/livros/{id}` | `204` — sem corpo | `404` se o ID não existir |

Corpo do POST/PUT (`LivroRequestDTO`): `titulo`, `preco`, `quantidade`, `categoriaId` (ID de uma categoria já cadastrada), `autoresIds` (array de IDs de autores) e `detalhe` (`numPaginas`, `editora`, `isbn`, `resumo`).

### Autores — `/api/autores`

| Método | Rota | Sucesso | Erros possíveis |
|---|---|---|---|
| GET | `/api/autores` | `200` — lista | — |
| GET | `/api/autores/{id}` | `200` | `404` |
| POST | `/api/autores` | `201` | `400` (nome vazio) |
| PUT | `/api/autores/{id}` | `200` | `400`, `404` |
| DELETE | `/api/autores/{id}` | `204` | `404` |

Corpo do POST/PUT (`AutorDTO`): `nome` (obrigatório).

### Categorias — `/api/categorias`

| Método | Rota | Sucesso | Erros possíveis |
|---|---|---|---|
| GET | `/api/categorias` | `200` — lista | — |
| GET | `/api/categorias/{id}` | `200` | `404` |
| POST | `/api/categorias` | `201` | `400` (nome vazio) |
| PUT | `/api/categorias/{id}` | `200` | `400`, `404` |
| DELETE | `/api/categorias/{id}` | `204` | `404`; `409` se a categoria estiver em uso por algum livro (violação de chave estrangeira) |

Corpo do POST/PUT (`CategoriaDTO`): `nome` (obrigatório).

> Exemplos completos de requisição/resposta, incluindo corpo de erro, estão em [`docs/guia-explicativo.md`](docs/guia-explicativo.md).

## Testes automatizados

```bash
./mvnw test
```

Pontos importantes sobre os testes:

- Os testes de controller usam `@SpringBootTest` + `MockMvc` + `@Transactional` e **usam o mesmo PostgreSQL real** configurado no `application.properties`. **Não há perfil de teste nem H2 no projeto** — o banco precisa estar acessível em `localhost:5432/biblioteca` para os testes rodarem.
- As classes de teste são anotadas com `@Transactional`, então cada teste é revertido ao final (os dados existentes não são alterados).
- São 29 testes no total: 24 de integração via MockMvc (Livro, Autor e Categoria), 4 unitários do tratamento global de exceções (Mockito) e 1 de contexto. A última execução registrada antes desta documentação passou 100% (29/29).
- Para rodar apenas uma classe: `./mvnw test -Dtest=LivroControllerTest`. Os relatórios XML/HTML ficam em `target/surefire-reports/` após cada execução.

## Observações importantes

1. **Credenciais do banco hardcoded** em `application.properties` (postgres/postgres). Para outros ambientes, altere o arquivo ou externalize a configuração.
2. **`ddl-auto=update`**: o Hibernate cria e altera tabelas automaticamente. Conveniente em desenvolvimento; em produção, recomenda-se desativar isso e gerenciar o schema com migrations (ex.: Flyway).
3. **Sem segurança**: nenhum endpoint exige autenticação e não há configuração de CORS (o frontend é servido pela mesma origem, então não é necessário).
4. **Sem busca, filtros ou paginação**: `GET /api/livros`, `/api/autores` e `/api/categorias` retornam a lista completa. Nenhuma rota de busca existe hoje.
5. **Comportamento do frontend**: ao carregar a página, apenas a lista de livros é buscada. As listas de categorias e autores só são carregadas quando o formulário de livro é aberto ou após operações de criação/edição/exclusão nessas entidades — por isso as abas "Categorias" e "Autores" podem aparecer vazias na primeira visita, até uma dessas ações (comportamento observado em `app.js`; não é falha da API).
6. **Timestamps** nos erros são retornados em UTC no formato ISO-8601 (ex.: `"2026-10-19T23:40:06.352452Z"`).
7. **`HELP.md`** é um arquivo gerado pelo Spring Initializr com links de referência; ele está presente no diretório, mas listado no `.gitignore` (não é versionado).
8. **Se aparecer `java.lang.ClassFormatError`** ao iniciar a aplicação (classes corrompidas em `target/` após alguma build interrompida), limpe e recompile: `./mvnw clean spring-boot:run`.

## Documentação complementar

- [`docs/guia-explicativo.md`](docs/guia-explicativo.md) — guia para aprendizagem: estrutura do projeto, fluxo de uma requisição, entidades e relacionamentos, DTOs e validações, testes no Postman com exemplos reais e conceitos de Java/Spring para estudar.
