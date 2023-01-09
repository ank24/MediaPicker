package com.ankit.mediapickerdemo

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImagePainter.State.Empty.painter
import coil.compose.rememberImagePainter
import com.ankit.mediapickerdemo.ui.theme.MediaPickerTheme
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import java.io.File

class MainActivity : ComponentActivity() {

    enum class MediaOptions {
        Photo,
        Video
    }

    enum class PickerOptions {
        Camera,
        Gallery
    }

    private lateinit var mediaPicker: com.ankit.mediapicker.MediaPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPicker = com.ankit.mediapicker.MediaPicker(this)
        setContent {
            MediaPickerTheme {
                ActivityComposables(mediaPicker)
            }
        }
    }
}

@Composable
fun ActivityComposables(mediaPicker: com.ankit.mediapicker.MediaPicker? = null) {
    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(horizontal = 20.dp)
        ) {

            var currentMediaState by remember {
                mutableStateOf(MainActivity.MediaOptions.Photo)
            }
            var currentPickerState by remember {
                mutableStateOf(MainActivity.PickerOptions.Camera)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Select Media Type:")
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                RadioTextField(
                    isSelected = currentMediaState == MainActivity.MediaOptions.Photo,
                    onSelected = {
                        currentMediaState = MainActivity.MediaOptions.Photo
                    },
                    text = "Photo"
                )
                Spacer(modifier = Modifier.width(20.dp))
                RadioTextField(
                    isSelected = currentMediaState == MainActivity.MediaOptions.Video,
                    onSelected = {
                        currentMediaState = MainActivity.MediaOptions.Video
                    },
                    text = "Video"
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Select Picker Type:")
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                RadioTextField(
                    isSelected = currentPickerState == MainActivity.PickerOptions.Camera,
                    onSelected = {
                        currentPickerState = MainActivity.PickerOptions.Camera
                    },
                    text = "Camera"
                )
                Spacer(modifier = Modifier.width(20.dp))
                RadioTextField(
                    isSelected = currentPickerState == MainActivity.PickerOptions.Gallery,
                    onSelected = {
                        currentPickerState = MainActivity.PickerOptions.Gallery
                    },
                    text = "Gallery"
                )
            }
            Spacer(modifier = Modifier.height(20.dp))


            var imagePath by remember {
                mutableStateOf("")
            }
            var videoPath by remember {
                mutableStateOf("")
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = {
                    Log.d("Ankit", "Picked options: $currentMediaState $currentPickerState")

                    when (currentMediaState) {
                        MainActivity.MediaOptions.Photo -> {
                            videoPath=String()
                            when (currentPickerState) {
                                MainActivity.PickerOptions.Camera -> mediaPicker?.takePictureFromCamera {
                                    imagePath = it
                                }
                                MainActivity.PickerOptions.Gallery -> mediaPicker?.takePictureFromGallery {
                                    imagePath = it
                                }
                            }
                        }
                        MainActivity.MediaOptions.Video -> {
                            imagePath=String()
                            when (currentPickerState) {
                                MainActivity.PickerOptions.Camera -> mediaPicker?.takeVideoFromCamera {
                                    videoPath = it
                                }
                                MainActivity.PickerOptions.Gallery -> mediaPicker?.takeVideoFromGallery {
                                    videoPath = it
                                }
                            }
                        }
                    }
                }) {

                    Text("Pick Media")


                }
            }
            if (imagePath.isNotBlank()) {

                AsyncImage(
                    model = imagePath,
                    contentDescription = null
                )


            }
            if(videoPath.isNotBlank()){
                Box(Modifier.fillMaxWidth()) {
                    VideoView(videoUri = videoPath)
                }''
            }
        }
    }
}

@Composable
fun RadioTextField(isSelected: Boolean, text: String, onSelected: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = isSelected,
            onClick = {
                onSelected()
            })
        Text(text)
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MediaPickerTheme {
        ActivityComposables()
    }
}

@Composable
fun VideoView(videoUri: String) {
    val context = LocalContext.current

    val exoPlayer = ExoPlayer.Builder(LocalContext.current)
        .build()
        .also { exoPlayer ->
            val mediaItem = MediaItem.Builder()
                .setUri(videoUri)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

    DisposableEffect(
        AndroidView(factory = {
            StyledPlayerView(context).apply {
                player = exoPlayer
            }
        })
    ) {
        onDispose { exoPlayer.release() }
    }
}