package com.ankit.mediapicker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ankit.mediapicker.data.MediaPicker
import com.ankit.mediapicker.ui.theme.MediaPickerTheme

class MainActivity :ComponentActivity() {

    enum class MediaOptions {
        Photo,
        Video
    }

    enum class PickerOptions {
        Camera,
        Gallery
    }
    private lateinit var mediaPicker:MediaPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPicker= MediaPicker(this)
        setContent {
            MediaPickerTheme {
                ActivityComposables(mediaPicker)
            }
        }
    }
}

@Composable
fun ActivityComposables(mediaPicker:MediaPicker?=null) {
    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 20.dp)
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
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = {
                        Log.d("Ankit","Picked options: $currentMediaState $currentPickerState")

                        when(currentMediaState){
                            MainActivity.MediaOptions.Photo -> when(currentPickerState){
                                MainActivity.PickerOptions.Camera -> mediaPicker?.takePictureFromCamera {

                                }
                                MainActivity.PickerOptions.Gallery ->  mediaPicker?.takePictureFromGallery {

                                }
                            }
                            MainActivity.MediaOptions.Video -> when(currentPickerState){
                                MainActivity.PickerOptions.Camera -> mediaPicker?.takeVideoFromCamera {

                                }
                                MainActivity.PickerOptions.Gallery ->  mediaPicker?.takeVideoFromGallery {

                                }
                            }
                        }
                    }) {

                        Text("Pick Media")


                    }
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

