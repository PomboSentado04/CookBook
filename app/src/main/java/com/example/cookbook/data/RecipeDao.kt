package com.example.cookbook.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM receitas ORDER BY nome COLLATE NOCASE")
    fun observarTodas(): Flow<List<ReceitaComIngredientes>>

    @Transaction
    @Query("SELECT * FROM receitas WHERE id = :id")
    suspend fun buscarPorId(id: Long): ReceitaComIngredientes?

    @Insert
    suspend fun inserirReceita(receita: Receita): Long

    @Insert
    suspend fun inserirIngredientes(ingredientes: List<Ingrediente>)

    @Update
    suspend fun atualizarReceita(receita: Receita)

    @Query("DELETE FROM ingredientes WHERE receitaId = :receitaId")
    suspend fun excluirIngredientesDaReceita(receitaId: Long)

    @Query("DELETE FROM receitas WHERE id = :receitaId")
    suspend fun excluirReceitaPorId(receitaId: Long): Int

    // Remove primeiro os filhos e depois a receita, na mesma transação.
    @Transaction
    suspend fun excluirCompleta(receitaId: Long): Int {
        excluirIngredientesDaReceita(receitaId)
        return excluirReceitaPorId(receitaId)
    }

    @Transaction
    suspend fun salvar(receita: Receita, ingredientes: List<Pair<String, String>>) {
        val novoId = inserirReceita(receita)
        inserirIngredientes(ingredientes.map { Ingrediente(receitaId = novoId, nome = it.first, quantidade = it.second) })
    }

    @Transaction
    suspend fun atualizarCompleta(receita: Receita, ingredientes: List<Pair<String, String>>) {
        atualizarReceita(receita)
        excluirIngredientesDaReceita(receita.id)
        if (ingredientes.isNotEmpty()) {
            inserirIngredientes(ingredientes.map { Ingrediente(receitaId = receita.id, nome = it.first, quantidade = it.second) })
        }
    }
}
