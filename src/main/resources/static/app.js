const API = '';

let categoriasCache = [];
let autoresCache = [];

// ---------- Navegação ----------
function mostrarSecao(id) {
    document.querySelectorAll('section').forEach(s => s.classList.remove('secao-visivel'));
    document.getElementById(id).classList.add('secao-visivel');
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    event.target.classList.add('active');
    fecharForm('form-livro');
    fecharForm('form-categoria');
    fecharForm('form-autor');
}

function fecharForm(id) {
    document.getElementById(id).classList.add('form-oculto');
    document.getElementById(id).reset();
}

function abrirForm(id) {
    document.getElementById(id).classList.remove('form-oculto');
}

// ---------- Mensagens ----------
function mostrarMensagem(texto, tipo) {
    const el = document.getElementById('mensagem');
    el.textContent = texto;
    el.className = `mensagem ${tipo}`;
    setTimeout(() => el.classList.add('hidden'), 4000);
}

function extrairErro(err) {
    if (err.errosValidacao) {
        return Object.values(err.errosValidacao).join('; ');
    }
    return err.message || 'Erro inesperado';
}

// ---------- Fetch helper ----------
async function apiFetch(url, options = {}) {
    try {
        const resp = await fetch(API + url, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        if (resp.status === 204) return { sucesso: true };
        const data = await resp.json().catch(() => null);
        if (!resp.ok) {
            return { sucesso: false, erro: data };
        }
        return { sucesso: true, data };
    } catch (e) {
        return { sucesso: false, erro: { message: 'Falha de conexão com a API' } };
    }
}

// ---------- Categorias ----------
async function carregarCategorias() {
    const r = await apiFetch('/api/categorias');
    if (!r.sucesso) return mostrarMensagem(extrairErro(r.erro), 'erro');
    categoriasCache = r.data;
    renderizarTabelaCategorias();
    preencherSelectCategorias();
}

function renderizarTabelaCategorias() {
    const tbody = document.querySelector('#tabela-categorias tbody');
    tbody.innerHTML = categoriasCache.map(c => `
        <tr>
            <td>${c.id}</td>
            <td>${c.nome}</td>
            <td>
                <button class="btn-acao" title="Editar" onclick="editarCategoria(${c.id})">&#9998;</button>
                <button class="btn-acao" title="Excluir" onclick="excluirCategoria(${c.id})">&#10005;</button>
            </td>
        </tr>
    `).join('');
}

function preencherSelectCategorias() {
    const sel = document.getElementById('livro-categoria');
    sel.innerHTML = '<option value="">Selecione...</option>' +
        categoriasCache.map(c => `<option value="${c.id}">${c.nome}</option>`).join('');
}

function abrirFormCategoria() {
    document.getElementById('categoria-id').value = '';
    document.getElementById('categoria-nome').value = '';
    document.getElementById('titulo-form-categoria').textContent = 'Cadastrar Categoria';
    abrirForm('form-categoria');
}

function editarCategoria(id) {
    const c = categoriasCache.find(x => x.id === id);
    document.getElementById('categoria-id').value = c.id;
    document.getElementById('categoria-nome').value = c.nome;
    document.getElementById('titulo-form-categoria').textContent = 'Editar Categoria';
    abrirForm('form-categoria');
}

async function excluirCategoria(id) {
    if (!confirm('Excluir esta categoria?')) return;
    const r = await apiFetch(`/api/categorias/${id}`, { method: 'DELETE' });
    if (r.sucesso) {
        mostrarMensagem('Categoria excluída com sucesso', 'sucesso');
        carregarCategorias();
    } else {
        mostrarMensagem(extrairErro(r.erro), 'erro');
    }
}

document.getElementById('form-categoria').addEventListener('submit', async e => {
    e.preventDefault();
    const id = document.getElementById('categoria-id').value;
    const nome = document.getElementById('categoria-nome').value.trim();
    const dto = { nome };
    const opts = id ? { method: 'PUT', body: JSON.stringify(dto) } : { method: 'POST', body: JSON.stringify(dto) };
    const url = id ? `/api/categorias/${id}` : '/api/categorias';
    const r = await apiFetch(url, opts);
    if (r.sucesso) {
        mostrarMensagem(id ? 'Categoria atualizada' : 'Categoria criada', 'sucesso');
        fecharForm('form-categoria');
        carregarCategorias();
    } else {
        mostrarMensagem(extrairErro(r.erro), 'erro');
    }
});

// ---------- Autores ----------
async function carregarAutores() {
    const r = await apiFetch('/api/autores');
    if (!r.sucesso) return mostrarMensagem(extrairErro(r.erro), 'erro');
    autoresCache = r.data;
    renderizarTabelaAutores();
    preencherSelectAutores();
}

function renderizarTabelaAutores() {
    const tbody = document.querySelector('#tabela-autores tbody');
    tbody.innerHTML = autoresCache.map(a => `
        <tr>
            <td>${a.id}</td>
            <td>${a.nome}</td>
            <td>
                <button class="btn-acao" title="Editar" onclick="editarAutor(${a.id})">&#9998;</button>
                <button class="btn-acao" title="Excluir" onclick="excluirAutor(${a.id})">&#10005;</button>
            </td>
        </tr>
    `).join('');
}

function preencherSelectAutores() {
    const sel = document.getElementById('livro-autores');
    sel.innerHTML = autoresCache.map(a => `<option value="${a.id}">${a.nome}</option>`).join('');
}

function abrirFormAutor() {
    document.getElementById('autor-id').value = '';
    document.getElementById('autor-nome').value = '';
    document.getElementById('titulo-form-autor').textContent = 'Cadastrar Autor';
    abrirForm('form-autor');
}

function editarAutor(id) {
    const a = autoresCache.find(x => x.id === id);
    document.getElementById('autor-id').value = a.id;
    document.getElementById('autor-nome').value = a.nome;
    document.getElementById('titulo-form-autor').textContent = 'Editar Autor';
    abrirForm('form-autor');
}

async function excluirAutor(id) {
    if (!confirm('Excluir este autor?')) return;
    const r = await apiFetch(`/api/autores/${id}`, { method: 'DELETE' });
    if (r.sucesso) {
        mostrarMensagem('Autor excluído com sucesso', 'sucesso');
        carregarAutores();
    } else {
        mostrarMensagem(extrairErro(r.erro), 'erro');
    }
}

document.getElementById('form-autor').addEventListener('submit', async e => {
    e.preventDefault();
    const id = document.getElementById('autor-id').value;
    const nome = document.getElementById('autor-nome').value.trim();
    const dto = { nome };
    const opts = id ? { method: 'PUT', body: JSON.stringify(dto) } : { method: 'POST', body: JSON.stringify(dto) };
    const url = id ? `/api/autores/${id}` : '/api/autores';
    const r = await apiFetch(url, opts);
    if (r.sucesso) {
        mostrarMensagem(id ? 'Autor atualizado' : 'Autor criado', 'sucesso');
        fecharForm('form-autor');
        carregarAutores();
    } else {
        mostrarMensagem(extrairErro(r.erro), 'erro');
    }
});

// ---------- Livros ----------
let livrosCache = [];

async function carregarLivros() {
    const r = await apiFetch('/api/livros');
    if (!r.sucesso) return mostrarMensagem(extrairErro(r.erro), 'erro');
    livrosCache = r.data;
    renderizarTabelaLivros();
}

function renderizarTabelaLivros() {
    const tbody = document.querySelector('#tabela-livros tbody');
    tbody.innerHTML = livrosCache.map(l => {
        const autores = (l.autores || []).map(a => a.nome).join(', ');
        return `
        <tr>
            <td>${l.id}</td>
            <td>${l.titulo}</td>
            <td>R$ ${Number(l.preco).toFixed(2)}</td>
            <td>${l.quantidade}</td>
            <td>${l.categoria ? l.categoria.nome : '-'}</td>
            <td>${autores}</td>
            <td>${l.detalhe ? l.detalhe.editora : '-'}</td>
            <td>
                <button class="btn-acao" title="Editar" onclick="editarLivro(${l.id})">&#9998;</button>
                <button class="btn-acao" title="Excluir" onclick="excluirLivro(${l.id})">&#10005;</button>
            </td>
        </tr>`;
    }).join('');
}

function abrirFormLivro() {
    document.getElementById('livro-id').value = '';
    document.getElementById('titulo-form-livro').textContent = 'Cadastrar Livro';
    abrirForm('form-livro');
    carregarCategorias();
    carregarAutores();
}

async function editarLivro(id) {
    const l = livrosCache.find(x => x.id === id);
    document.getElementById('livro-id').value = l.id;
    document.getElementById('livro-titulo').value = l.titulo;
    document.getElementById('livro-preco').value = l.preco;
    document.getElementById('livro-quantidade').value = l.quantidade;
    await carregarCategorias();
    await carregarAutores();
    document.getElementById('livro-categoria').value = l.categoria ? l.categoria.id : '';
    const selAutores = document.getElementById('livro-autores');
    Array.from(selAutores.options).forEach(o => {
        o.selected = (l.autores || []).some(a => a.id === parseInt(o.value));
    });
    if (l.detalhe) {
        document.getElementById('detalhe-paginas').value = l.detalhe.numPaginas || '';
        document.getElementById('detalhe-editora').value = l.detalhe.editora || '';
        document.getElementById('detalhe-isbn').value = l.detalhe.isbn || '';
        document.getElementById('detalhe-resumo').value = l.detalhe.resumo || '';
    }
    document.getElementById('titulo-form-livro').textContent = 'Editar Livro';
    abrirForm('form-livro');
}

async function excluirLivro(id) {
    if (!confirm('Excluir este livro?')) return;
    const r = await apiFetch(`/api/livros/${id}`, { method: 'DELETE' });
    if (r.sucesso) {
        mostrarMensagem('Livro excluído com sucesso', 'sucesso');
        carregarLivros();
    } else {
        mostrarMensagem(extrairErro(r.erro), 'erro');
    }
}

document.getElementById('form-livro').addEventListener('submit', async e => {
    e.preventDefault();
    const id = document.getElementById('livro-id').value;
    const selAutores = document.getElementById('livro-autores');
    const autoresIds = Array.from(selAutores.selectedOptions).map(o => parseInt(o.value));

    const dto = {
        titulo: document.getElementById('livro-titulo').value.trim(),
        preco: parseFloat(document.getElementById('livro-preco').value),
        quantidade: parseInt(document.getElementById('livro-quantidade').value),
        categoriaId: parseInt(document.getElementById('livro-categoria').value),
        autoresIds: autoresIds,
        detalhe: {
            numPaginas: parseInt(document.getElementById('detalhe-paginas').value),
            editora: document.getElementById('detalhe-editora').value.trim(),
            isbn: document.getElementById('detalhe-isbn').value.trim(),
            resumo: document.getElementById('detalhe-resumo').value.trim() || null
        }
    };

    const opts = id ? { method: 'PUT', body: JSON.stringify(dto) } : { method: 'POST', body: JSON.stringify(dto) };
    const url = id ? `/api/livros/${id}` : '/api/livros';
    const r = await apiFetch(url, opts);
    if (r.sucesso) {
        mostrarMensagem(id ? 'Livro atualizado' : 'Livro criado', 'sucesso');
        fecharForm('form-livro');
        carregarLivros();
    } else {
        mostrarMensagem(extrairErro(r.erro), 'erro');
    }
});

// ---------- Init ----------
carregarLivros();
