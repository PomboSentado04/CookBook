package com.example.cookbook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.cookbook.data.Receita
import com.example.cookbook.data.ReceitaComIngredientes
import com.example.cookbook.network.MealDto
import com.example.cookbook.ui.RecipeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: RecipeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CookBookApp(vm) }
    }
}

private enum class Tela { LISTA, FORMULARIO, DETALHE, BUSCA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookBookApp(vm: RecipeViewModel) {
    val receitas by vm.receitas.collectAsStateWithLifecycle()
    var tela by remember { mutableStateOf(Tela.LISTA) }
    var selecionada by remember { mutableStateOf<ReceitaComIngredientes?>(null) }
    var editando by remember { mutableStateOf<ReceitaComIngredientes?>(null) }
    var termo by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<MealDto>>(emptyList()) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF795548),
        onPrimary = Color(0xFFFFFAF5),
        primaryContainer = Color(0xFFE9D7C5),
        onPrimaryContainer = Color(0xFF402C22),
        secondary = Color(0xFFA98264),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF1E2D2),
        background = Color(0xFFFFF9F2),
        onBackground = Color(0xFF3E3029),
        surface = Color(0xFFFFFDF9),
        onSurface = Color(0xFF3E3029),
        surfaceVariant = Color(0xFFF2E7DA),
        onSurfaceVariant = Color(0xFF69584D),
        outline = Color(0xFFB8A496),
        error = Color(0xFFAA443C)
    )) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("CookBook ☕", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    navigationIcon = {
                        if (tela != Tela.LISTA) IconButton(onClick = { tela = Tela.LISTA }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        if (tela == Tela.LISTA) {
                            IconButton(onClick = { tela = Tela.BUSCA }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar na internet")
                            }
                            IconButton(onClick = { editando = null; tela = Tela.FORMULARIO }) {
                                Icon(Icons.Default.Add, contentDescription = "Nova receita")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                when (tela) {
                    Tela.LISTA -> {
                        Text("Suas receitas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 16.dp))
                        if (receitas.isEmpty()) {
                            Text("Ainda não há receitas salvas. Toque em + para criar ou na lupa para importar da TheMealDB.")
                        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(receitas, key = { it.receita.id }) { item ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth().clickable { selecionada = item; tela = Tela.DETALHE },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (item.receita.imagemUrl.isNotBlank()) {
                                            AsyncImage(model = item.receita.imagemUrl, contentDescription = item.receita.nome, modifier = Modifier.size(76.dp))
                                        }
                                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                            Text(item.receita.nome, fontWeight = FontWeight.SemiBold)
                                            Text(listOf(item.receita.categoria, item.receita.origem).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Tela.FORMULARIO -> RecipeForm(
                        initial = editando,
                        onCancel = { tela = Tela.LISTA },
                        onSave = { nome, cat, origem, imagem, preparo, ingredientes ->
                            val atual = editando
                            if (atual == null) vm.salvar(nome, cat, origem, imagem, preparo, ingredientes)
                            else vm.atualizar(atual.receita.id, nome, cat, origem, imagem, preparo, ingredientes)
                            tela = Tela.LISTA
                        }
                    )
                    Tela.DETALHE -> selecionada?.let { item ->
                        RecipeDetail(
                            item = item,
                            onEdit = { editando = item; tela = Tela.FORMULARIO },
                            onDelete = {
                                vm.excluir(item.receita.id) { sucesso, erro ->
                                    if (sucesso) {
                                        selecionada = null
                                        tela = Tela.LISTA
                                    } else {
                                        mensagem = erro
                                    }
                                }
                            },
                            erro = mensagem,
                            onShare = {
                                val texto = buildString {
                                    append(item.receita.nome).append("\n\n")
                                    append("Ingredientes:\n")
                                    item.ingredientes.forEach { append("• ").append(it.quantidade).append(" ").append(it.nome).append("\n") }
                                    append("\nModo de preparo:\n").append(item.receita.modoPreparo)
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, item.receita.nome)
                                    putExtra(Intent.EXTRA_TEXT, texto)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartilhar receita"))
                            }
                        )
                    }
                    Tela.BUSCA -> {
                        Text("Explorar TheMealDB", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp))
                        OutlinedTextField(value = termo, onValueChange = { termo = it }, label = { Text("Nome do prato") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                scope.launch {
                                    carregando = true
                                    val result = vm.pesquisarOnline(termo)
                                    resultados = result.getOrElse { mensagem = "Não foi possível consultar a API. Verifique sua conexão."; emptyList() }
                                    if (result.isSuccess && resultados.isEmpty()) mensagem = "Nenhuma receita encontrada."
                                    carregando = false
                                }
                            },
                            enabled = termo.isNotBlank() && !carregando,
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text(if (carregando) "Buscando..." else "Buscar receitas") }
                        mensagem?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(resultados) { meal ->
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(model = meal.strMealThumb, contentDescription = null, modifier = Modifier.size(76.dp))
                                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                            Text(meal.strMeal.orEmpty(), fontWeight = FontWeight.SemiBold)
                                            Text("${meal.strCategory.orEmpty()} • ${meal.strArea.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                                            TextButton(onClick = { vm.importar(meal); mensagem = "Receita importada para sua coleção." }) { Text("Importar para minhas receitas") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun RecipeForm(
    initial: ReceitaComIngredientes?,
    onCancel: () -> Unit,
    onSave: (String, String, String, String, String, List<Pair<String,String>>) -> Unit
) {
    var nome by remember(initial) { mutableStateOf(initial?.receita?.nome.orEmpty()) }
    var categoria by remember(initial) { mutableStateOf(initial?.receita?.categoria.orEmpty()) }
    var origem by remember(initial) { mutableStateOf(initial?.receita?.origem.orEmpty()) }
    var imagem by remember(initial) { mutableStateOf(initial?.receita?.imagemUrl.orEmpty()) }
    var preparo by remember(initial) { mutableStateOf(initial?.receita?.modoPreparo.orEmpty()) }
    var ingredientes by remember(initial) {
        mutableStateOf(initial?.ingredientes?.joinToString("\n") { "${it.nome}|${it.quantidade}" }.orEmpty())
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (initial == null) "Nova receita" else "Editar receita", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp))
        OutlinedTextField(nome, { nome = it }, label = { Text("Nome *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(categoria, { categoria = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(origem, { origem = it }, label = { Text("Origem") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(imagem, { imagem = it }, label = { Text("URL da imagem (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(preparo, { preparo = it }, label = { Text("Modo de preparo") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
        Text("Ingredientes (um por linha: nome|quantidade)", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(ingredientes, { ingredientes = it }, modifier = Modifier.fillMaxWidth(), minLines = 4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = {
                val lista = ingredientes.lines().mapNotNull { linha ->
                    val partes = linha.split("|", limit = 2)
                    partes.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { it to partes.getOrNull(1).orEmpty().trim() }
                }
                onSave(nome.trim(), categoria.trim(), origem.trim(), imagem.trim(), preparo.trim(), lista)
            }, enabled = nome.isNotBlank()) { Text("Salvar") }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RecipeDetail(
    item: ReceitaComIngredientes,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    erro: String?,
    onShare: () -> Unit
) {
    var confirmar by remember(item.receita.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (item.receita.imagemUrl.isNotBlank()) AsyncImage(
            model = item.receita.imagemUrl, contentDescription = item.receita.nome,
            modifier = Modifier.fillMaxWidth().height(210.dp)
        )
        Text(item.receita.nome, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        Text(listOf(item.receita.categoria, item.receita.origem).filter { it.isNotBlank() }.joinToString(" • "))
        // Duas linhas evitam que os textos dos botões quebrem em telas estreitas.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) { Text("Compartilhar", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Editar", maxLines = 1, softWrap = false)
            }
        }
        OutlinedButton(
            onClick = { confirmar = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Excluir receita", maxLines = 1, softWrap = false)
        }
        Text("Ingredientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        item.ingredientes.forEach { Text("• ${it.quantidade} ${it.nome}".trim()) }
        Text("Modo de preparo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(item.receita.modoPreparo.ifBlank { "Modo de preparo não informado." })
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false },
        title = { Text("Excluir receita?") },
        text = { Text("Essa ação não poderá ser desfeita.") },
        confirmButton = { TextButton(onClick = { confirmar = false; onDelete() }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
    )
}
