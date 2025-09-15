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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.savvyclub.data.model.PuzzleSource
import java.io.File
import com.example.savvyclub.R


@Composable
fun PuzzleImageFromPath(
    filePath: String,
    source: PuzzleSource,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    tintAlpha: Float = 0.27f, // прозрачность тонировки (0..1)
    grayscale: Boolean = true // включить/выключить обесцвечивание
) {
    val context = LocalContext.current
    val isLight = !isSystemInDarkTheme() // true для светлой темы, false для тёмной

    // Цвет тонировки
    val tintColor = if (isLight) Color(0xFF808000) else Color(0xFF9DA861)

    // Загружаем картинку из assets или из локальной папки puzzles/
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

    imageBitmap?.let { bitmap ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .then(
                    // Если передали обработчик onTap, вешаем жест нажатия
                    if (onTap != null) Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onTap() })
                    } else Modifier
                )
        ) {
            // Матрица для обесцвечивания (saturation = 0 делает картинку ч/б)
            val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

            // Рисуем изображение
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
                // Если включен режим grayscale — применяем фильтр
                colorFilter = if (grayscale) ColorFilter.colorMatrix(grayscaleMatrix) else null
            )

            // Полупрозрачная текстура сверху
            Image(
                painter = painterResource(id = R.drawable.kletka), // твоя текстура
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.045f, // прозрачность, чтобы текстура не перебивала изображение
//                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )

            // Рисуем полупрозрачный цветной оверлей поверх картинки
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(tintColor.copy(alpha = tintAlpha))
            )
        }
    }
}
