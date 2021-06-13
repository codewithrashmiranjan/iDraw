package com.rashmiranjan.idraw

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    lateinit var drawing_view: DrawingView
    lateinit var ib_brush: ImageButton
    lateinit var ib_imagePicker: ImageButton
    lateinit var ib_undo: ImageButton
    lateinit var ib_save: ImageButton
    lateinit var ib_share: ImageButton
    lateinit var ll_colorPanel: LinearLayout
    lateinit var iv_background: ImageView
    lateinit var fl_drawingViewContainer: FrameLayout

    var theResult: String = ""


    private var mImageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view = findViewById(R.id.drawing_view)
        ib_brush = findViewById(R.id.ib_brush)
        ll_colorPanel = findViewById(R.id.ll_colorPanel)
        ib_imagePicker = findViewById(R.id.ib_imagePicker)
        ib_undo = findViewById(R.id.ib_undo)
        ib_save = findViewById(R.id.ib_save)
        ib_share = findViewById(R.id.ib_share)
        iv_background = findViewById(R.id.iv_background)
        fl_drawingViewContainer = findViewById(R.id.fl_drawingViewContainer)


        iv_background.visibility = View.GONE

        mImageButtonCurrentPaint = ll_colorPanel[1] as ImageButton

        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        ib_imagePicker.setOnClickListener {
            if (isStorageAccess()) {

                val pickPhotoIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)

            } else {
                requestStoragePermission()
            }
        }

        drawing_view.setSizeofBrush(5.toFloat())
        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }

        ib_save.setOnClickListener {
            if (isStorageAccess()) {
                BitMapAsyncTask(getBitmapFromView(fl_drawingViewContainer)).execute()
            } else {
                requestStoragePermission()
            }
        }

        ib_share.setOnClickListener {
            shareDrawing()
        }


    }

    private fun showBrushSizeChooserDialog() {
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallButton = brushDialog.findViewById<ImageButton>(R.id.ib_smallBrush)
        val mediumButton = brushDialog.findViewById<ImageButton>(R.id.ib_mediumBrush)
        val largeButton = brushDialog.findViewById<ImageButton>(R.id.ib_largeBrush)

        smallButton.setOnClickListener {
            drawing_view.setSizeofBrush(5.toFloat())
            brushDialog.dismiss()
        }
        mediumButton.setOnClickListener {
            drawing_view.setSizeofBrush(15.toFloat())
            brushDialog.dismiss()
        }
        largeButton.setOnClickListener {
            drawing_view.setSizeofBrush(25.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()

    }

    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton

            var colorTag = imageButton.tag.toString()
            drawing_view.setBrushColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )

            mImageButtonCurrentPaint = view

        }
    }


    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(this, "Need permission to add Background", Toast.LENGTH_LONG).show()

        }
        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@MainActivity,
                    "Permission granted,You can access your storage..",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "You need to give the permission..",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isStorageAccess(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error in parse the Image..",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background

        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }


    private inner class BitMapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>() {


        private lateinit var mProgressBar: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if (result!!.isNotEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully:   $result",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file..",
                    Toast.LENGTH_LONG
                ).show()
            }

            theResult = result

        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""

            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f =
                        File(externalCacheDir!!.absoluteFile.toString() + File.separator + "iDrawApp_" + System.currentTimeMillis() / 1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath


                } catch (e: java.lang.Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        private fun showProgressDialog() {
            mProgressBar = Dialog(this@MainActivity)
            mProgressBar.setContentView(R.layout.dialog_custom_progress)
            mProgressBar.show()
        }

        private fun cancelProgressDialog() {
            mProgressBar.dismiss()
        }

    }

    private fun shareDrawing() {
        if (theResult != "") {
            MediaScannerConnection.scanFile(
                this@MainActivity,
                arrayOf(theResult),
                null
            ) { path, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"

                startActivity(
                    Intent.createChooser(shareIntent, "Share")
                )
            }
        } else {
            Toast.makeText(this, "Please download before sharing..", Toast.LENGTH_LONG).show()
        }

    }


    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2

    }


}