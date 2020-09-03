package com.edt.ut3.backend.note

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.ThumbnailUtils
import android.os.Environment
import android.os.Parcelable
import com.edt.ut3.misc.toDp
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * This class is used to store, generate and load picture
 * from the internal phone's memory.
 *
 * @property thumbnail The picture's thumbnail
 * @property picture The actual picture
 */
@Parcelize
data class Picture(
    val thumbnail: String,
    val picture: String
) : Parcelable {
    companion object {
        /**
         * This function prepare a temp file that will be stored
         * with the provided name and a ".jpg" extension.
         *
         * @param context A valid context
         * @param filename The filename to store the picture
         * @param format The format (.jpg by default)
         */
        fun prepareImageFile(context: Context, filename: String, format: String = ".jpg"): File =
            File(getStorageDir(context), "$filename.jpg")
//            File.createTempFile(
//                filename,
//                format,
//                getStorageDir(context)
//            )

        /**
         * This function will generate and store a thumbnail
         * for a given picture.
         * The picture provided must exists because the image
         * will be load in memory to perform the operation.
         *
         * It will add "_thumbnail" to the filename
         *
         * @param context A valid context
         * @param filename How you want to name the picture
         * @param pictureLocation The actual picture location that will be
         * edited into a thumbnail.
         *
         * @return The Picture containing all the information
         * to retrieve the picture and the thumbnail.
         */
        suspend fun generateFromPictureUri(context: Context, filename: String, pictureLocation: String): Picture = withContext(IO)
        {
            // We prepare the picture into a File
            File(pictureLocation).let {
                // The file is loaded and decoded by the BitmapFactory
                val bitmap = BitmapFactory.decodeFileDescriptor(it.inputStream().fd)

                // The thumbnail location is generated with
                // the provided filename
                val thumbnailLocation = "${filename}_thumbnail"

                // The thumbnail is generated asynchronously
                // with a size of 64dps
                val thumbnail = withContext(Default) {
                    val thumbSize = 64.toDp(context).toInt()
                    ThumbnailUtils.extractThumbnail(bitmap, thumbSize, thumbSize)
                }

                // Finally the thumbnail is saved in memory
                withContext(Default) {
                    val thumbnailFile = prepareImageFile(context, thumbnailLocation)
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, thumbnailFile.outputStream())

                    // And we return the Picture
                    Picture(picture = pictureLocation, thumbnail =  thumbnailFile.absolutePath)
                }
            }
        }

        /**
         * Returns the default app directory pictures
         * @param context A valid context
         */
        fun getStorageDir(context: Context) =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        /**
         * Generate a filename which contains the provided prefix
         * and the current timestamp
         *
         * @param prefix The filename prefix
         */
        @SuppressLint("SimpleDateFormat")
        fun generateFilename(prefix: String) =
            "$prefix${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}"
    }

    /**
     * @return Retrieve the current thumbnail
     */
    @Throws(IllegalStateException::class)
    suspend fun loadThumbnail(): Bitmap = withContext(IO) {
        println("Loading thumbnail $thumbnail")
        val file = File(thumbnail)
        if (!file.exists()) {
            throw IllegalStateException("The thumbnail doesn't exist")
        }

        BitmapFactory.decodeFileDescriptor(file.inputStream().fd)
    }

    suspend fun loadPicture(): Bitmap = withContext(IO) {
        println("Loading picture: $picture")
        val file = File(picture)
        if (!file.exists()) {
            throw IllegalStateException("The picture doesn't exist")
        }

        BitmapFactory.decodeFile(file.absolutePath)
    }

    suspend fun loadPictureForBounds(reqWidth: Int, reqHeight: Int): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        return withContext(IO) {
            BitmapFactory.Options().run {
                val file = File(picture)
                inJustDecodeBounds = true
                BitmapFactory.decodeFile(file.absolutePath, this)

                // Calculate inSampleSize
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

                // Decode bitmap with inSampleSize set
                inJustDecodeBounds = false

                BitmapFactory.decodeFile(file.absolutePath, this)
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}