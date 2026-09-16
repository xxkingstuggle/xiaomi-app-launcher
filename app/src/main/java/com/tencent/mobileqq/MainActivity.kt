package com.tencent.mobileqq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tencent.mobileqq.data.LauncherApp
import com.tencent.mobileqq.data.SortMode
import com.tencent.mobileqq.ui.AppCenterTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppCenterTheme {
                AppCenterScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppCenterScreen(viewModel: LauncherViewModel) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val hasInstalledApps by viewModel.hasInstalledApps.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用中心") },
                actions = {
                    SortMenu(sortMode, viewModel::setSortMode)
                    IconButton(onClick = viewModel::refresh, enabled = loadState !is LoadState.Loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新应用列表")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SearchBox(searchQuery, viewModel::setSearchQuery)
            when (val state = loadState) {
                LoadState.Loading -> LoadingContent()
                is LoadState.Error -> ErrorContent(state.message, viewModel::refresh)
                LoadState.Ready -> {
                    if (!hasInstalledApps) EmptyContent("设备上没有可启动的应用", "安装应用后，点击刷新即可查看")
                    else if (apps.isEmpty()) EmptyContent("无搜索结果", "试试其他名称或包名")
                    else AppList(apps, preferences.favorites, viewModel::toggleFavorite, viewModel::openApp)
                }
            }
        }
    }
}

@Composable
private fun SearchBox(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        singleLine = true,
        label = { Text("搜索应用") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                }
            }
        }
    )
}

@Composable
private fun SortMenu(sortMode: SortMode, onModeSelected: (SortMode) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "选择排序方式")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("最近打开${if (sortMode == SortMode.RECENT) " ✓" else ""}") },
                onClick = { onModeSelected(SortMode.RECENT); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("按名称${if (sortMode == SortMode.NAME) " ✓" else ""}") },
                onClick = { onModeSelected(SortMode.NAME); expanded = false }
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text("正在读取设备上的应用…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("应用列表加载失败", style = MaterialTheme.typography.titleMedium)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.Button(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun EmptyContent(title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppList(
    apps: List<LauncherApp>,
    favorites: Set<String>,
    onToggleFavorite: (LauncherApp) -> Unit,
    onOpen: (LauncherApp) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppRow(app, favorites, onToggleFavorite, onOpen)
        }
    }
}

@Composable
private fun AppRow(app: LauncherApp, favorites: Set<String>, onToggleFavorite: (LauncherApp) -> Unit, onOpen: (LauncherApp) -> Unit) {
    Surface(
        onClick = { onOpen(app) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app, Modifier.size(48.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onToggleFavorite(app) }) {
                val isFavorite = app.packageName in favorites
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (isFavorite) "取消收藏" else "收藏")
            }
        }
    }
}

@Composable
private fun AppIcon(app: LauncherApp, modifier: Modifier = Modifier) {
    val icon = app.icon
    if (icon != null) {
        val bitmap = remember(icon) { icon.toBitmap(96, 96).asImageBitmap() }
        Image(bitmap = bitmap, contentDescription = "${app.appName}图标", modifier = modifier)
    } else {
        Icon(painterResource(com.tencent.mobileqq.R.drawable.ic_launcher_legacy), contentDescription = "${app.appName}图标", modifier = modifier)
    }
}

