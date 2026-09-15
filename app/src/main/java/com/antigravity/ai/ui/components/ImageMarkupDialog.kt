package com.antigravity.ai.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigravity.ai.data.model.Attachment
import com.antigravity.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class MarkupTool {
    PEN,    // Serbest çizim
    ARROW,  // Ok işareti
    CIRCLE, // Daire / Halka
    RECT    // Dikdörtgen kutu
}

sealed class DrawnElement(val color: Color, val strokeWidth: Float) {
    class Freehand(
        val points: List<Offset>,
        color: Color,
        strokeWidth: Float
    ) : DrawnElement(color, strokeWidth)

    class Arrow(
        val start: Offset,
        val end: Offset,
        color: Color,
        strokeWidth: Float
    ) : DrawnElement(color, strokeWidth)

    class Circle(
        val start: Offset,
        val end: Offset,
        color: Color,
        strokeWidth: Float
    ) : DrawnElement(color, strokeWidth)

    class Rect(
        val start: Offset,
        val end: Offset,
        color: Color,
        strokeWidth: Float
    ) : DrawnElement(color, strokeWidth)
}

/**
 * Resimlerin üzerine hızlıca kırmızı ve diğer renklerle çizim, ok, daire ve kutu ekleyerek
 * AI asistana görsel talimat vermeyi sağlayan zengin görsel işaretleme editörü.
 */
@Composable
fun ImageMarkupDialog(
    imageSource: Any, // Uri, File path (String), or Attachment
    title: String = "Görseli İşaretle",
    onDismiss: () -> Unit,
    onSaveAnnotated: (Attachment) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Çizim Araçları State
    var selectedTool by remember { mutableStateOf(MarkupTool.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFFFF1744)) } // Varsayılan Canlı Kırmızı
    var strokeWidth by remember { mutableStateOf(8f) }

    val elements = remember { mutableStateListOf<DrawnElement>() }

    // Aktif çizilen geçici şekil
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Bitmap'i asenkron yükle
    LaunchedEffect(imageSource) {
        withContext(Dispatchers.IO) {
            try {
                val bmp: Bitmap? = when (imageSource) {
                    is Uri -> {
                        context.contentResolver.openInputStream(imageSource)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                    is String -> {
                        when {
                            imageSource.startsWith("content://") -> {
                                val u = Uri.parse(imageSource)
                                context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
                            }
                            imageSource.startsWith("file://") -> {
                                val path = imageSource.removePrefix("file://")
                                BitmapFactory.decodeFile(path)
                            }
                            imageSource.startsWith("http://") || imageSource.startsWith("https://") -> {
                                val url = java.net.URL(imageSource)
                                val conn = url.openConnection()
                                conn.connectTimeout = 8000
                                conn.readTimeout = 8000
                                conn.getInputStream().use { BitmapFactory.decodeStream(it) }
                            }
                            else -> BitmapFactory.decodeFile(imageSource)
                        }
                    }
                    is Attachment -> {
                        val path = imageSource.path
                        val localUri = imageSource.localUri
                        when {
                            !localUri.isNullOrBlank() && localUri.startsWith("content://") -> {
                                val u = Uri.parse(localUri)
                                context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
                            }
                            !path.isNullOrBlank() -> BitmapFactory.decodeFile(path)
                            else -> null
                        }
                    }
                    else -> null
                }

                if (bmp != null) {
                    loadedBitmap = bmp
                    isLoading = false
                } else {
                    errorMessage = "Görsel yüklenemedi"
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Hata: ${e.localizedMessage}"
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0D14)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121722))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantDark)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextPrimary)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "🎨 $title",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Kırmızıyla çizerek talimat verin",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Undo and Save Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (elements.isNotEmpty()) {
                                    elements.removeAt(elements.size - 1)
                                }
                            },
                            enabled = elements.isNotEmpty(),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (elements.isNotEmpty()) SurfaceVariantDark else SurfaceVariantDark.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Undo,
                                contentDescription = "Geri Al",
                                tint = if (elements.isNotEmpty()) TextPrimary else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val bmp = loadedBitmap ?: return@Button
                                coroutineScope.launch {
                                    try {
                                        val annotated = saveAnnotatedBitmap(
                                            context = context,
                                            originalBitmap = bmp,
                                            elements = elements.toList(),
                                            renderedCanvasSize = canvasSize
                                        )
                                        if (annotated != null) {
                                            onSaveAnnotated(annotated)
                                            Toast.makeText(context, "İşaretlenmiş görsel mesaja eklendi", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Görsel kaydedilemedi", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Kayıt hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = loadedBitmap != null,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ekle", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Middle Area: Image & Drawing Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = GeminiBlue)
                    } else if (errorMessage != null || loadedBitmap == null) {
                        Text(
                            text = errorMessage ?: "Görsel açılamadı",
                            color = DangerRed,
                            fontSize = 13.sp
                        )
                    } else {
                        val bmp = loadedBitmap!!
                        val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Aspect ratio matching container
                            val bmpWidth = bmp.width.toFloat()
                            val bmpHeight = bmp.height.toFloat()
                            val aspectRatio = bmpWidth / bmpHeight.coerceAtLeast(1f)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .onSizeChanged { canvasSize = it }
                                    .pointerInput(selectedTool, selectedColor, strokeWidth) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                dragStart = offset
                                                dragCurrent = offset
                                                if (selectedTool == MarkupTool.PEN) {
                                                    currentPoints = listOf(offset)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val nextOffset = (dragCurrent ?: dragStart ?: Offset.Zero) + dragAmount
                                                dragCurrent = nextOffset
                                                if (selectedTool == MarkupTool.PEN) {
                                                    currentPoints = currentPoints + nextOffset
                                                }
                                            },
                                            onDragEnd = {
                                                val start = dragStart
                                                val current = dragCurrent
                                                if (start != null && current != null) {
                                                    when (selectedTool) {
                                                        MarkupTool.PEN -> {
                                                            if (currentPoints.size >= 2) {
                                                                elements.add(
                                                                    DrawnElement.Freehand(
                                                                        points = currentPoints,
                                                                        color = selectedColor,
                                                                        strokeWidth = strokeWidth
                                                                    )
                                                                )
                                                            }
                                                        }
                                                        MarkupTool.ARROW -> {
                                                            elements.add(
                                                                DrawnElement.Arrow(
                                                                    start = start,
                                                                    end = current,
                                                                    color = selectedColor,
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            )
                                                        }
                                                        MarkupTool.CIRCLE -> {
                                                            elements.add(
                                                                DrawnElement.Circle(
                                                                    start = start,
                                                                    end = current,
                                                                    color = selectedColor,
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            )
                                                        }
                                                        MarkupTool.RECT -> {
                                                            elements.add(
                                                                DrawnElement.Rect(
                                                                    start = start,
                                                                    end = current,
                                                                    color = selectedColor,
                                                                    strokeWidth = strokeWidth
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                                dragStart = null
                                                dragCurrent = null
                                                currentPoints = emptyList()
                                            },
                                            onDragCancel = {
                                                dragStart = null
                                                dragCurrent = null
                                                currentPoints = emptyList()
                                            }
                                        )
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // 1. Orijinal Görseli Çiz
                                    drawImage(
                                        image = imageBitmap,
                                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                                    )

                                    // 2. Önceden Çizilmiş Elemanları Çiz
                                    elements.forEach { elem ->
                                        drawMarkupElement(elem)
                                    }

                                    // 3. Aktif Çizilmekte Olan Şekli Canlı Çiz
                                    val start = dragStart
                                    val current = dragCurrent
                                    if (start != null && current != null) {
                                        when (selectedTool) {
                                            MarkupTool.PEN -> {
                                                if (currentPoints.size >= 2) {
                                                    drawMarkupElement(
                                                        DrawnElement.Freehand(
                                                            points = currentPoints,
                                                            color = selectedColor,
                                                            strokeWidth = strokeWidth
                                                        )
                                                    )
                                                }
                                            }
                                            MarkupTool.ARROW -> {
                                                drawMarkupElement(
                                                    DrawnElement.Arrow(
                                                        start = start,
                                                        end = current,
                                                        color = selectedColor,
                                                        strokeWidth = strokeWidth
                                                    )
                                                )
                                            }
                                            MarkupTool.CIRCLE -> {
                                                drawMarkupElement(
                                                    DrawnElement.Circle(
                                                        start = start,
                                                        end = current,
                                                        color = selectedColor,
                                                        strokeWidth = strokeWidth
                                                    )
                                                )
                                            }
                                            MarkupTool.RECT -> {
                                                drawMarkupElement(
                                                    DrawnElement.Rect(
                                                        start = start,
                                                        end = current,
                                                        color = selectedColor,
                                                        strokeWidth = strokeWidth
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Toolbar: Tool selection, Colors, Widths & Clear
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121722))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Tool Row (Pen, Arrow, Circle, Rect, Clear)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Tool Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolIconButton(
                                icon = Icons.Default.Edit,
                                label = "Serbest",
                                isSelected = selectedTool == MarkupTool.PEN,
                                onClick = { selectedTool = MarkupTool.PEN }
                            )
                            ToolIconButton(
                                icon = Icons.Default.ArrowOutward,
                                label = "Ok",
                                isSelected = selectedTool == MarkupTool.ARROW,
                                onClick = { selectedTool = MarkupTool.ARROW }
                            )
                            ToolIconButton(
                                icon = Icons.Default.RadioButtonUnchecked,
                                label = "Daire",
                                isSelected = selectedTool == MarkupTool.CIRCLE,
                                onClick = { selectedTool = MarkupTool.CIRCLE }
                            )
                            ToolIconButton(
                                icon = Icons.Default.CropSquare,
                                label = "Kutu",
                                isSelected = selectedTool == MarkupTool.RECT,
                                onClick = { selectedTool = MarkupTool.RECT }
                            )
                        }

                        // Clear Button
                        IconButton(
                            onClick = { elements.clear() },
                            enabled = elements.isNotEmpty(),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (elements.isNotEmpty()) DangerRed.copy(alpha = 0.2f) else SurfaceVariantDark.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Temizle",
                                tint = if (elements.isNotEmpty()) DangerRed else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Color & Width Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Color Palette
                        val colors = listOf(
                            Color(0xFFFF1744), // Canlı Kırmızı (Varsayılan)
                            Color(0xFFFFD600), // Sarı
                            Color(0xFF00E676), // Yeşil
                            Color(0xFF00B0FF), // Mavi
                            Color(0xFFFFFFFF)  // Beyaz
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colors.forEach { c ->
                                val isSelected = selectedColor == c
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 28.dp else 22.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = c }
                                )
                            }
                        }

                        // Stroke Width Selector (İnce, Orta, Kalın)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Pair(4f, "İnce"),
                                Pair(8f, "Orta"),
                                Pair(14f, "Kalın")
                            ).forEach { (w, name) ->
                                val isSel = strokeWidth == w
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) GeminiBlue.copy(alpha = 0.25f) else SurfaceVariantDark,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) GeminiBlue else BorderSubtle
                                    ),
                                    modifier = Modifier.clickable { strokeWidth = w }
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) GeminiBlue else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
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
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) GeminiBlue.copy(alpha = 0.22f) else SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) GeminiBlue else BorderSubtle
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) GeminiBlue else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) GeminiBlue else TextSecondary
            )
        }
    }
}

/**
 * Compose Canvas üzerinde şekli çizer
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarkupElement(elem: DrawnElement) {
    when (elem) {
        is DrawnElement.Freehand -> {
            if (elem.points.size >= 2) {
                for (i in 0 until elem.points.size - 1) {
                    drawLine(
                        color = elem.color,
                        start = elem.points[i],
                        end = elem.points[i + 1],
                        strokeWidth = elem.strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        is DrawnElement.Arrow -> {
            drawArrow(
                start = elem.start,
                end = elem.end,
                color = elem.color,
                strokeWidth = elem.strokeWidth
            )
        }
        is DrawnElement.Circle -> {
            val left = minOf(elem.start.x, elem.end.x)
            val top = minOf(elem.start.y, elem.end.y)
            val right = maxOf(elem.start.x, elem.end.x)
            val bottom = maxOf(elem.start.y, elem.end.y)
            drawOval(
                color = elem.color,
                topLeft = Offset(left, top),
                size = Size((right - left).coerceAtLeast(1f), (bottom - top).coerceAtLeast(1f)),
                style = Stroke(width = elem.strokeWidth)
            )
        }
        is DrawnElement.Rect -> {
            val left = minOf(elem.start.x, elem.end.x)
            val top = minOf(elem.start.y, elem.end.y)
            val right = maxOf(elem.start.x, elem.end.x)
            val bottom = maxOf(elem.start.y, elem.end.y)
            drawRect(
                color = elem.color,
                topLeft = Offset(left, top),
                size = Size((right - left).coerceAtLeast(1f), (bottom - top).coerceAtLeast(1f)),
                style = Stroke(width = elem.strokeWidth)
            )
        }
    }
}

/**
 * Ok işareti çizen yardımcı fonksiyon
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    // 1. Ana Çizgi
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // 2. Ok Başı (Arrowhead)
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowHeadLength = (strokeWidth * 3.2f).coerceIn(16f, 42f)
    val arrowAngle = Math.PI / 6.0 // 30 derece

    val x1 = end.x - arrowHeadLength * cos(angle - arrowAngle).toFloat()
    val y1 = end.y - arrowHeadLength * sin(angle - arrowAngle).toFloat()

    val x2 = end.x - arrowHeadLength * cos(angle + arrowAngle).toFloat()
    val y2 = end.y - arrowHeadLength * sin(angle + arrowAngle).toFloat()

    drawLine(
        color = color,
        start = end,
        end = Offset(x1, y1),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = end,
        end = Offset(x2, y2),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

/**
 * Orijinal Bitmap'in çözünürlüğüne uygun ölçekleyerek çizimleri Android Canvas ile
 * render edip dosyaya kaydeder ve Attachment nesnesi döndürür.
 */
private suspend fun saveAnnotatedBitmap(
    context: Context,
    originalBitmap: Bitmap,
    elements: List<DrawnElement>,
    renderedCanvasSize: IntSize
): Attachment? = withContext(Dispatchers.IO) {
    try {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val outputBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(outputBitmap)

        val scaleX = if (renderedCanvasSize.width > 0) width.toFloat() / renderedCanvasSize.width else 1f
        val scaleY = if (renderedCanvasSize.height > 0) height.toFloat() / renderedCanvasSize.height else 1f
        val baseScale = (scaleX + scaleY) / 2f

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        elements.forEach { elem ->
            paint.color = elem.color.toArgb()
            paint.strokeWidth = elem.strokeWidth * baseScale

            when (elem) {
                is DrawnElement.Freehand -> {
                    if (elem.points.size >= 2) {
                        val path = AndroidPath()
                        path.moveTo(elem.points[0].x * scaleX, elem.points[0].y * scaleY)
                        for (i in 1 until elem.points.size) {
                            path.lineTo(elem.points[i].x * scaleX, elem.points[i].y * scaleY)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                is DrawnElement.Arrow -> {
                    val sX = elem.start.x * scaleX
                    val sY = elem.start.y * scaleY
                    val eX = elem.end.x * scaleX
                    val eY = elem.end.y * scaleY

                    canvas.drawLine(sX, sY, eX, eY, paint)

                    val angle = atan2((eY - sY).toDouble(), (eX - sX).toDouble())
                    val arrowHeadLength = (paint.strokeWidth * 3.2f).coerceIn(24f, 80f)
                    val arrowAngle = Math.PI / 6.0

                    val x1 = (eX - arrowHeadLength * cos(angle - arrowAngle)).toFloat()
                    val y1 = (eY - arrowHeadLength * sin(angle - arrowAngle)).toFloat()
                    val x2 = (eX - arrowHeadLength * cos(angle + arrowAngle)).toFloat()
                    val y2 = (eY - arrowHeadLength * sin(angle + arrowAngle)).toFloat()

                    canvas.drawLine(eX, eY, x1, y1, paint)
                    canvas.drawLine(eX, eY, x2, y2, paint)
                }
                is DrawnElement.Circle -> {
                    val left = minOf(elem.start.x, elem.end.x) * scaleX
                    val top = minOf(elem.start.y, elem.end.y) * scaleY
                    val right = maxOf(elem.start.x, elem.end.x) * scaleX
                    val bottom = maxOf(elem.start.y, elem.end.y) * scaleY

                    canvas.drawOval(left, top, right, bottom, paint)
                }
                is DrawnElement.Rect -> {
                    val left = minOf(elem.start.x, elem.end.x) * scaleX
                    val top = minOf(elem.start.y, elem.end.y) * scaleY
                    val right = maxOf(elem.start.x, elem.end.x) * scaleX
                    val bottom = maxOf(elem.start.y, elem.end.y) * scaleY

                    canvas.drawRect(left, top, right, bottom, paint)
                }
            }
        }

        // Termux uploads klasörüne veya app files klasörüne kaydet
        val uploadsDir = File("/data/data/com.termux/files/home/uploads")
        val targetDir = if (uploadsDir.exists() && uploadsDir.canWrite()) uploadsDir else File(context.filesDir, "uploads").apply { mkdirs() }

        val timestamp = System.currentTimeMillis()
        val fileName = "annotated_${timestamp}.png"
        val outputFile = File(targetDir, fileName)

        FileOutputStream(outputFile).use { out ->
            outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        Attachment(
            name = fileName,
            path = outputFile.absolutePath,
            localUri = Uri.fromFile(outputFile).toString(),
            relPath = "uploads/$fileName",
            type = "image",
            size = outputFile.length()
        )
    } catch (e: Exception) {
        null
    }
}
