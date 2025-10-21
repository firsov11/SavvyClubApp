package com.example.savvyclub.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savvyclub.data.model.PuzzlePack
import com.example.savvyclub.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    onBack: () -> Unit,
    viewModel: StoreViewModel = viewModel()
) {
    val packs by viewModel.packs.collectAsState()

    // Показываем только платные пакеты
    val paidPacks = packs.filter { !it.isFree }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Магазин") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(paidPacks) { pack ->
                    PuzzlePackItem(
                        pack = pack,
                        onDownloadClick = { context -> viewModel.downloadPack(context, pack) },
                        onBuyClick = { viewModel.buyPack(pack) }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzlePackItem(
    pack: PuzzlePack,
    onDownloadClick: (Context) -> Unit,
    onBuyClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = pack.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${pack.price} $",
                    fontSize = 14.sp
                )
            }

            when {
                pack.isDownloaded -> {
                    Text(
                        text = "Скачано",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
                pack.isPurchased -> {
                    Button(onClick = { onDownloadClick(context) }) {
                        Text("Скачать")
                    }
                }
                else -> {
                    Button(onClick = onBuyClick) {
                        Text("Купить")
                    }
                }
            }
        }
    }
}


