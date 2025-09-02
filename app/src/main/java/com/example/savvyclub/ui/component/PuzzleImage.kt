package com.example.savvyclub.ui.component

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.savvyclub.data.model.PuzzleSource
import java.io.File

@Composable
fun PuzzleImageFromPath(
    filePath: String,
    source: PuzzleSource,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    tintAlpha: Float = 0.35f // прозрачность тонировки, 0..1
) {
    val context = LocalContext.current
    val isLight = !isSystemInDarkTheme() // true для светлой темы, false для тёмной

    // Цвет тонировки в зависимости от темы
    val tintColor = if (isLight) Color(0xFF808000) else Color(0xFF657E39)

    val imageBitmap: ImageBitmap? = remember(filePath, source) {
        try {
            val bitmap = when (source) {
                PuzzleSource.LOCAL -> {
                    val localFile = File(context.filesDir, "puzzles/$filePath")
                    if (localFile.exists()) BitmapFactory.decodeFile(localFile.absolutePath)
                    else null
                }
                PuzzleSource.ASSETS -> {
                    context.assets.open(filePath).use { inputStream ->
                        val byteArray = inputStream.readBytes()
                        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    }
                }
            }
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            Log.e("PuzzleImageFromPath", "Ошибка загрузки изображения: $filePath", e)
            null
        }
    }

    imageBitmap?.let {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .then(
                    if (onTap != null) Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onTap() })
                    } else Modifier
                )
        ) {
            // Картинка растягивается на всю ширину и сохраняет пропорции
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )

            // Оверлей точно совпадает с картинкой
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(tintColor.copy(alpha = tintAlpha))
            )
        }
    }
}
