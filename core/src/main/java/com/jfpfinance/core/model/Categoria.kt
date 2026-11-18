package com.jfpfinance.core.model

enum class TipoCategoria { RECEITA, DESPESA }

/** Pessoa da família (seção 13). Serve para atribuir lançamentos e separar relatórios. */
data class Pessoa(
    val id: Long = 0,
    val nome: String,
    val ativa: Boolean = true
)

/** Categoria de receita ou despesa. [paiId] preenchido = subcategoria. */
data class Categoria(
    val id: Long = 0,
    val nome: String,
    val tipo: TipoCategoria,
    val paiId: Long? = null,
    val ativa: Boolean = true
)

/** Categorias criadas no primeiro uso. O usuário poderá personalizar depois. */
object CategoriasPadrao {
    val lista: List<Categoria> = listOf(
        "Salário", "Rendimentos", "Receitas extras", "Outras receitas"
    ).map { Categoria(nome = it, tipo = TipoCategoria.RECEITA) } + listOf(
        "Moradia", "Alimentação", "Transporte", "Saúde", "Educação", "Lazer",
        "Vestuário", "Contas e serviços", "Impostos e taxas", "Outras despesas"
    ).map { Categoria(nome = it, tipo = TipoCategoria.DESPESA) }
}
