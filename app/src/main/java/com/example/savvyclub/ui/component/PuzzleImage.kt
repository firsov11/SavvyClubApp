package com.example.savvyclub.ui.component

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.savvyclub.R
import com.example.savvyclub.data.model.PuzzleSource
import java.io.File

@Composable
fun PuzzleImageFromPath(
    filePath: String,
    source: PuzzleSource,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val imageBitmap: ImageBitmap? = remember(filePath) {
        try {
            val localFile = File(context.filesDir, "puzzles/package_7/$filePath")
            val bitmap = if (localFile.exists()) {
                BitmapFactory.decodeFile(localFile.absolutePath)
            } else {
                context.assets.open(filePath).use { inputStream ->
                    val byteArray = inputStream.readBytes()
                    BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                }
            }
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
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
                .then(
                    if (onTap != null) Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onTap() })
                    } else Modifier
                )
        )
    }
}





