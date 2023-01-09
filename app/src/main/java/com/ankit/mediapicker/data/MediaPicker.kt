package com.ankit.mediapicker.data

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.ankit.mediapicker.BuildConfig
import com.kotlin.mvvm.structure.utils.AppPermissions
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest


/*   Author: Ankit Mishra
 /   Date: 30th April, 2021
 /   Description: This class is used for doing some manipulation on File.
*/
class MediaPicker(private val activity: ComponentActivity) {

    private val provider = "${BuildConfig.APPLICATION_ID}.mediaPickerProvider"

    /**
     * This is activity result launcher for uploading pic from camera
     */
    private var activityResultLauncherForUploadFromCamera: ActivityResultLauncher<Intent>? = null


    /**
     * This is activity result launcher for uploading pic from gallery
     */
    private var activityResultLauncherForUploadFromGallery: ActivityResultLauncher<Intent>? = null

    /**
     * This is activity result launcher for uploading video from gallery
     */
    private var activityResultLauncherForUploadVideoFromGallery: ActivityResultLauncher<Intent>? =
        null

    /**
     * This is activity result launcher for uploading video from camera
     */
    private var activityResultLauncherForUploadVideoFromCamera: ActivityResultLauncher<Intent>? =
        null

    /**
     * This is success callback which will be called on successful pic selection from Camera.
     */
    private var onPicSelectedFromCamera: ((path: String) -> Unit)? = null

    /**
     * This is success callback which will be called on successful pic selection from Gallery.
     */
    private var onPicSelectedFromGallery: ((path: String) -> Unit)? = null

    /**
     * This is success callback which will be called on successful video selection from Camera.
     */
    private var onVideoSelectedFromCamera: ((path: String) -> Unit)? = null

    /**
     * This is success callback which will be called on successful video selection from Gallery.
     */
    private var onVideoSelectedFromGallery: ((path: String) -> Unit)? = null

    /**
     * This is temp pic path variable in which path will be stored temporarily.
     */
    private var tempFilePathOfCameraImage: String = String()

    /**
     * This is temp pic path variable in which path will be stored temporarily.
     */
    private var tempFilePathOfCameraVideo: String = String()


    private var appPermissions: AppPermissions = AppPermissions(activity)

    init {
        ifThrowsPrintStackTrace {
            activityResultLauncherForUploadFromCamera =
                activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        try {
                            onPicSelectedFromCamera?.invoke(tempFilePathOfCameraImage)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }



            activityResultLauncherForUploadFromGallery =
                activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        try {
                            val data = result.data
                            data?.let {
                                val inputStream =
                                    activity.contentResolver.openInputStream(it.data!!)
                                inputStream?.let { inputStreamWhichIsNotNull ->
                                    val file = createImageFileFromInputStream(
                                        inputStreamWhichIsNotNull
                                    )
                                    onPicSelectedFromGallery?.invoke(file.absolutePath)

                                }
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }


            activityResultLauncherForUploadVideoFromCamera =
                activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {

                        try {
                            onVideoSelectedFromCamera?.invoke(tempFilePathOfCameraVideo)


                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            activityResultLauncherForUploadVideoFromGallery =
                activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        try {
                            val data = result.data
                            data?.let {
                                val inputStream =
                                    activity.contentResolver.openInputStream(it.data!!)
                                inputStream?.let { inputStreamWhichIsNotNull ->
                                    val videoFile =
                                        createVideoFileFromInputStream(
                                            inputStreamWhichIsNotNull
                                        )
                                    onVideoSelectedFromGallery?.invoke(videoFile.absolutePath)
                                }
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }
        }
    }

    fun getBitmapFromImageView(imageView: ImageView): Bitmap? {
        var bmp: Bitmap? = null
        ifThrowsPrintStackTrace {
            val layoutParams: ViewGroup.LayoutParams = imageView.layoutParams

            imageView.isDrawingCacheEnabled = true

            // this is the important code :)
            // Without it the view will have a dimension of 0,0 and the bitmap will be null

            // this is the important code :)
            // Without it the view will have a dimension of 0,0 and the bitmap will be null
            imageView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            imageView.layout(
                0,
                0,
                imageView.measuredWidth,
                imageView.measuredHeight
            )

            imageView.buildDrawingCache(
                true
            )
            bmp = Bitmap.createBitmap(imageView.drawingCache)
            imageView.isDrawingCacheEnabled = false // clear drawing cache


            imageView.layoutParams = layoutParams

        }
        return bmp
    }

    /**
     * Gets the bitmap from the URI.
     * @param selectedPhotoUri Provide Uri
     * @param contentResolver Provide Content Resolved
     * @return this returns Bitmap from the provided uri
     */
    private fun getBitmapFromUri(selectedPhotoUri: Uri, contentResolver: ContentResolver): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src: ImageDecoder.Source = ImageDecoder.createSource(
                contentResolver,
                selectedPhotoUri
            )
            ImageDecoder.decodeBitmap(src)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
        }
    }

    /**
     * Gets the new temporary file to with .jpg extension.
     *
     * @param file File in which data will be written.
     * @param bytesToBeWritten Data which will be written.
     * @return this returns nothing.
     */
    private fun writeBytesIntoFile(file: File, bytesToBeWritten: ByteArray) {
        try {
            val fos = FileOutputStream(file, false)
            fos.write(bytesToBeWritten)
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * Gets the new temporary file to with .jpg extension.
     *
     * @param file Image File which will be compressed.
     * @param compressWhenSizeLargerThan Provide size in kb, file will be compressed only if the size is larger than this parameter.
     * @return this returns nothing.
     */
    fun compressImageFile(
        file: File,
        compressWhenSizeLargerThan: Int = 500
    ) {
        try {
            if (file.length() / 1024 > compressWhenSizeLargerThan) {
                //Compress picture
                val bitmap = getBitmapFromUri(Uri.fromFile(file), activity.contentResolver)
                val scaleWidth: Int = bitmap.width / 4
                val scaleHeight: Int = bitmap.height / 4
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    scaleWidth,
                    scaleHeight,
                    true
                )
                // 2. Instantiate the downsized image content as a byte[]
                val baos = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val compressImageBytes = baos.toByteArray()

                writeBytesIntoFile(file, compressImageBytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @SuppressLint("SetTextI18n")
            /**
             * Gets the new bitmap file to cropped in square.
             *
             * @param file Image File which will be cropped.
             * @return this returns nothing.
             */
    fun cropToSquare(file: File) {
        try {
            val bitmap = getBitmapFromUri(Uri.fromFile(file), activity.contentResolver)
            val width = bitmap.width
            val height = bitmap.height
            val newWidth = if (height > width) width else height
            val newHeight = if (height > width) height - (height - width) else height
            var cropW = (width - height) / 2
            cropW = if (cropW < 0) 0 else cropW
            var cropH = (height - width) / 2
            cropH = if (cropH < 0) 0 else cropH
            val croppedBitmap = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight)

            //Saving the cropped image
            val baos = ByteArrayOutputStream()
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val croppedImageBytes = baos.toByteArray()
            writeBytesIntoFile(file, croppedImageBytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * Gets the new temporary file to with .jpg extension.
     *
     * @return this returns new image file
     */
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val storageDir: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }


    /**
     * Gets the new temporary file to with .mp4 extension.
     *
     * @return this returns new video file
     */
    fun createVideoFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val storageDir: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "MP4_${timeStamp}_", /* prefix */
            ".mp4", /* suffix */
            storageDir /* directory */
        )
    }

    /**
     * Gets the new temporary file to with .jpg extension.
     *
     * @param inputStream
     * @return this returns new image file from the received input stream.
     */
    private fun createImageFileFromInputStream(inputStream: InputStream): File {
        val file = createImageFile()
        val outputStream: OutputStream = FileOutputStream(file)
        try {
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                outputStream.write(buf, 0, len)
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            outputStream.close()
            inputStream.close()
        }
        return file
    }

    /**
     * Gets the new temporary file to with .mp4 extension.
     *
     * @param inputStream
     * @return this returns new video file from the received input stream.
     */
    private fun createVideoFileFromInputStream(inputStream: InputStream): File {
        val file = createVideoFile()
        val outputStream: OutputStream = FileOutputStream(file)
        try {
            val buf = ByteArray(2048)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                outputStream.write(buf, 0, len)
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            outputStream.close()
            inputStream.close()
        }
        return file
    }

    fun takePictureFromCamera(
        onSuccessListener: (picPath: String) -> Unit
    ) {
        appPermissions.checkIfPermissionsGranted(cameraPermissions) {
            onPicSelectedFromCamera = onSuccessListener
            val mIntent =
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    // Ensure that there's a camera activity to handle the intent

                    takePictureIntent.resolveActivity(activity.packageManager)
                        ?.also {
                            // Create the File where the photo should go
                            val photoFile: File? = try {
                                val file = createImageFile()
//                            cameraImageFilePathCallback(file.absolutePath)
                                tempFilePathOfCameraImage = file.absolutePath
                                file
                            } catch (ex: IOException) {
                                // Error occurred while creating the File
                                ex.printStackTrace()
                                null
                            }
                            // Continue only if the File was successfully created
                            photoFile?.also {
                                val photoURI: Uri = FileProvider.getUriForFile(
                                    activity,
                                    provider, //Change the authority according to application
                                    it
                                )
                                takePictureIntent.putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    photoURI
                                )
                            }
                        }

                }
            activityResultLauncherForUploadFromCamera?.launch(mIntent)
        }
    }

    fun takeVideoFromCamera(
        onSuccessListener: (picPath: String) -> Unit
    ) {
        appPermissions.checkIfPermissionsGranted(cameraPermissions) {
            onVideoSelectedFromCamera = onSuccessListener
            ifThrowsPrintStackTrace {
                val mIntent =
                    Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                        // Ensure that there's a video activity to handle the intent

                        takeVideoIntent.resolveActivity(activity.packageManager)
                            ?.also {
                                // Create the File where the photo should go
                                val videoFile: File? = try {
                                    val file = createVideoFile()
//                            cameraVideoFilePathCallback(file.absolutePath)
                                    tempFilePathOfCameraVideo = file.absolutePath

                                    file
                                } catch (ex: IOException) {
                                    // Error occurred while creating the File
                                    ex.printStackTrace()
                                    null
                                }
                                // Continue only if the File was successfully created
                                videoFile?.also {
                                    val photoURI: Uri = FileProvider.getUriForFile(
                                        activity,
                                        provider, //Change the authority according to application
                                        it
                                    )

                                    takeVideoIntent.putExtra(
                                        MediaStore.EXTRA_OUTPUT,
                                        photoURI
                                    )
                                }
                            }

                    }
                activityResultLauncherForUploadVideoFromCamera?.launch(mIntent)
            }
        }
    }


    fun takePictureFromGallery(
        onSuccessListener: (picPath: String) -> Unit
    ) {
        onPicSelectedFromGallery = onSuccessListener
        val mIntent =
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

        activityResultLauncherForUploadFromGallery?.launch(mIntent)

    }

    fun takeVideoFromGallery(
        onSuccessListener: (picPath: String) -> Unit
    ) {
        onVideoSelectedFromGallery = onSuccessListener
        val mIntent = Intent(
            Intent.ACTION_PICK
        )
        mIntent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")

        activityResultLauncherForUploadVideoFromGallery?.launch(mIntent)
    }

    /**
     * Description: This method is used for saving the image from imageview temporarily and provide the image path.
     *
     * @author Ankit Mishra
     * @since 10/01/22
     *
     * @param imageView Provide the image view.
     */
    @Suppress("DEPRECATION")
    fun savePicTemporarilyAndGetUrl(imageView: ImageView): String {
        var filePathAfterSavingSuccessfully = String()
        ifThrowsPrintStackTrace {
            val layoutParams: ViewGroup.LayoutParams = imageView.layoutParams
            imageView.isDrawingCacheEnabled = true

            imageView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            imageView.layout(
                0,
                0,
                imageView.measuredWidth,
                imageView.measuredHeight
            )

            imageView.buildDrawingCache(
                true
            )
            val bmp: Bitmap =
                Bitmap.createBitmap(imageView.drawingCache)
            imageView.isDrawingCacheEnabled = false

            imageView.layoutParams = layoutParams

            val filePath = createImageFile().absolutePath
            filePathAfterSavingSuccessfully = try {
                val fileOutputStream = FileOutputStream(filePath)
                val bos = BufferedOutputStream(fileOutputStream)
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                bos.flush()
                bos.close()
                filePath
            } catch (e: FileNotFoundException) {
                Log.w("FileAndCameraUtils", "Error saving image file: " + e.message)
                String()
            } catch (e: IOException) {
                Log.w("FileAndCameraUtils", "Error saving image file: " + e.message)
                String()
            }
        }
        return filePathAfterSavingSuccessfully
    }

    private val cameraPermissions get() = listOf(android.Manifest.permission.CAMERA)

}