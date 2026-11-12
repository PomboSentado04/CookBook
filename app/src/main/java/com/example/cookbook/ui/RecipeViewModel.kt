package com.example.cookbook.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookbook.data.*
import com.example.cookbook.network.MealApi
import com.example.cookbook.network.MealDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).recipeDao()
    private val api = MealApi.criar()

    val receitas = dao.observarTodas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun buscar(id: Long) = dao.buscarPorId(id)

    fun salvar(nome: String, categoria: String, origem: String, imagem: String, preparo: String, ingredientes: List<Pair<String,String>>) {
        viewModelScope.launch {
            dao.salvar(Receita(nome = nome, categoria = categoria, origem = origem, imagemUrl = imagem, modoPreparo = preparo), ingredientes)
        }
    }

    fun atualizar(id: Long, nome: String, categoria: String, origem: String, imagem: String, preparo: String, ingredientes: List<Pair<String,String>>) {
        viewModelScope.launch {
            dao.atualizarCompleta(Receita(id, nome, categoria, origem, imagem, preparo), ingredientes)
        }
    }

    fun excluir(receitaId: Long, aoConcluir: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val linhasRemovidas = dao.excluirCompleta(receitaId)
                if (linhasRemovidas > 0) {
                    aoConcluir(true, null)
                } else {
                    aoConcluir(false, "A receita não foi encontrada no banco de dados (ID: $receitaId).")
                }
            } catch (e: Exception) {
                aoConcluir(false, "Falha ao excluir: ${e.localizedMessage ?: "erro no banco de dados"}")
            }
        }
    }

    suspend fun pesquisarOnline(termo: String): Result<List<MealDto>> = runCatching {
        api.pesquisar(termo).meals.orEmpty()
    }

    fun importar(meal: MealDto) {
        viewModelScope.launch {
            val existe = dao.observarTodas()
            // Importação sempre cria uma cópia local; a chave da API fica registrada para referência.
            dao.salvar(
                Receita(
                    nome = meal.strMeal.orEmpty(),
                    categoria = meal.strCategory.orEmpty(),
                    origem = meal.strArea.orEmpty(),
                    imagemUrl = meal.strMealThumb.orEmpty(),
                    modoPreparo = meal.strInstructions.orEmpty(),
                    fonteApiId = meal.idMeal
                ),
                meal.ingredientes()
            )
        }
    }
}
