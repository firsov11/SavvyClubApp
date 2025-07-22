package com.example.savvyclub.ui.component

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.savvyclub.R

@Composable
fun PuzzleImageFromAssets(
    fileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageBitmap: ImageBitmap? = remember(fileName) {
        try {
            Log.d("PuzzleImage", "Попытка открыть файл из assets: $fileName")
            context.assets.open(fileName).use { inputStream ->
                val byteArray = inputStream.readBytes()
                Log.d("PuzzleImage", "Файл прочитан, размер: ${byteArray.size} байт")
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                if (bitmap != null) {
                    Log.d("PuzzleImage", "Картинка декодирована: ${bitmap.width}x${bitmap.height}")
                    bitmap.asImageBitmap()
                } else {
                    Log.e("PuzzleImage", "BitmapFactory вернул null — возможно, поврежденный файл.")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("PuzzleImage", "Ошибка при загрузке изображения: $fileName", e)
            null
        }
    }

    imageBitmap?.let {
        val aspectRatio = it.width.toFloat() / it.height
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .padding(8.dp)
        )
    } ?: Text(
        text = stringResource(R.string.image_not_found, fileName),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(8.dp)
    )
}
