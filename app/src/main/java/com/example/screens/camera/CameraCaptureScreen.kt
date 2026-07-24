package com.example.screens.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.models.MEDIA_TYPE_PHOTO
import com.example.models.MEDIA_TYPE_VIDEO
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureDialog(
    onDismiss: () -> Unit,
    onMediaCaptured: (File, String, String) -> Unit // File, FileType (PHOTO/VIDEO), MimeType
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (hasCameraPermission) {
                CameraContent(
                    onDismiss = onDismiss,
                    onMediaCaptured = onMediaCaptured
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Please grant camera permission to capture photos and videos for your event.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                        Button(
                            onClick = {
                                launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                            }
                        ) {
                            Text("Grant Permission")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraContent(
    onDismiss: () -> Unit,
    onMediaCaptured: (File, String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isVideoMode by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) } // OFF, ON, AUTO
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableIntStateOf(0) }

    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    // Preview capture result state
    var capturedFile by remember { mutableStateOf<File?>(null) }
    var capturedType by remember { mutableStateOf<String?>(null) } // PHOTO or VIDEO
    var capturedMime by remember { mutableStateOf<String?>(null) }

    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }

    // Timer for recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingDurationSec++
            }
        }
    }

    if (capturedFile != null && capturedType != null) {
        // PREVIEW SCREEN (Retake vs Use)
        CameraPreviewScreen(
            file = capturedFile!!,
            fileType = capturedType!!,
            onRetake = {
                capturedFile?.delete()
                capturedFile = null
                capturedType = null
                capturedMime = null
            },
            onUse = {
                onMediaCaptured(capturedFile!!, capturedType!!, capturedMime!!)
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        val imgCapture = ImageCapture.Builder()
                            .setFlashMode(flashMode)
                            .build()

                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                            .build()
                        val vidCapture = VideoCapture.withOutput(recorder)

                        imageCapture = imgCapture
                        videoCapture = vidCapture

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                if (isVideoMode) vidCapture else imgCapture
                            )
                            activeCamera = cam
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        val imgCapture = ImageCapture.Builder()
                            .setFlashMode(flashMode)
                            .build()

                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                            .build()
                        val vidCapture = VideoCapture.withOutput(recorder)

                        imageCapture = imgCapture
                        videoCapture = vidCapture

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                if (isVideoMode) vidCapture else imgCapture
                            )
                            activeCamera = cam
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // Top Bar Controls (Close, Flash, Front/Back)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
                }

                if (isRecording) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Red.copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            val minutes = recordingDurationSec / 60
                            val seconds = recordingDurationSec % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Flash Button
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            val icon = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            }
                            Icon(icon, contentDescription = "Flash", tint = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White else Color.Yellow)
                        }

                        // Front/Back Camera Switch
                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
                        }
                    }
                }
            }

            // Zoom Control
            activeCamera?.let { cam ->
                if (!isRecording) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                zoomRatio = if (zoomRatio >= 2f) 1f else 2f
                                cam.cameraControl.setZoomRatio(zoomRatio)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1fx", zoomRatio),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Capture Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Selector (Photo / Video)
                if (!isRecording) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PHOTO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isVideoMode) Color.Yellow else Color.White.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.clickable { isVideoMode = false }
                        )
                        Text(
                            text = "VIDEO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isVideoMode) Color.Yellow else Color.White.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.clickable { isVideoMode = true }
                        )
                    }
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .background(if (isVideoMode) (if (isRecording) Color.Red else Color.Red.copy(alpha = 0.8f)) else Color.White)
                        .clickable {
                            if (!isVideoMode) {
                                // TAKE PHOTO
                                val imgCap = imageCapture ?: return@clickable
                                val photoFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                imgCap.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            capturedFile = photoFile
                                            capturedType = MEDIA_TYPE_PHOTO
                                            capturedMime = "image/jpeg"
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                        }
                                    }
                                )
                            } else {
                                // RECORD VIDEO
                                if (!isRecording) {
                                    val vidCap = videoCapture ?: return@clickable
                                    val videoFile = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
                                    val outputOptions = FileOutputOptions.Builder(videoFile).build()

                                    val pendingRecording = vidCap.output.prepareRecording(context, outputOptions)
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        pendingRecording.withAudioEnabled()
                                    }

                                    isRecording = true
                                    activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                                        if (event is VideoRecordEvent.Finalize) {
                                            isRecording = false
                                            if (!event.hasError()) {
                                                capturedFile = videoFile
                                                capturedType = MEDIA_TYPE_VIDEO
                                                capturedMime = "video/mp4"
                                            } else {
                                                videoFile.delete()
                                            }
                                        }
                                    }
                                } else {
                                    // STOP RECORDING
                                    activeRecording?.stop()
                                    activeRecording = null
                                    isRecording = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewScreen(
    file: File,
    fileType: String,
    onRetake: () -> Unit,
    onUse: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (fileType == MEDIA_TYPE_PHOTO) {
            AsyncImage(
                model = file,
                contentDescription = "Captured Photo",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(Uri.fromFile(file))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                }
            )
        }

        // Bottom Action Controls: Retake vs Use
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f).padding(end = 10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake")
            }

            Button(
                onClick = onUse,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Media")
            }
        }
    }
}
