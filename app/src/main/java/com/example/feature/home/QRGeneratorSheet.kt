package com.example.feature.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

/**
 * QRGeneratorSheet.kt
 * 
 * বর্তমান ওয়েবসাইটের URL বা ইউজার ইনপুট থেকে ZXing দিয়ে তাৎক্ষণিক কিউআর কোড জেনারেট ও শেয়ার করার ব্যবস্থা।
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRGeneratorSheet(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSwitchToScanner: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(initialUrl.ifBlank { "https://google.com" }) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(inputText) {
        if (inputText.isNotBlank()) {
            qrBitmap = generateQRCodeBitmap(inputText)
        } else {
            qrBitmap = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("QR Generator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onSwitchToScanner) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Switch to Scanner", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("URL or Text to encode") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // QR Display Box
            Surface(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Generated QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Enter text above to generate QR code", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        qrBitmap?.let { bmp ->
                            shareQRCodeBitmap(context, bmp, "infinity_qr_${System.currentTimeMillis()}.png")
                        } ?: Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share QR Code Image", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun generateQRCodeBitmap(text: String, size: Int = 600): Bitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun shareQRCodeBitmap(context: Context, bitmap: Bitmap, fileName: String) {
    try {
        val cachePath = File(context.cacheDir, "qr_codes")
        cachePath.mkdirs()
        val file = File(cachePath, fileName)
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share QR Code via..."))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not share QR image", Toast.LENGTH_SHORT).show()
    }
}
