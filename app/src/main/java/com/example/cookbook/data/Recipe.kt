package com.example.cookbook.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "receitas")
data class Receita(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val categoria: String = "",
    val origem: String = "",
    val imagemUrl: String = "",
    val modoPreparo: String = "",
    val fonteApiId: String? = null
)

@Entity(
    tableName = "ingredientes",
    foreignKeys = [ForeignKey(
        entity = Receita::class,
        parentColumns = ["id"],
        childColumns = ["receitaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("receitaId")]
)
data class Ingrediente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receitaId: Long,
    val nome: String,
    val quantidade: String = ""
)

data class ReceitaComIngredientes(
    @Embedded val receita: Receita,
    @Relation(parentColumn = "id", entityColumn = "receitaId")
    val ingredientes: List<Ingrediente>
)
