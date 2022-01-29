package com.example.kidsdrawingapp

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.larswerkman.holocolorpicker.ColorPicker
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    //set a value to buttons pressed that need permissions
    private var clickedButton: Int = 0


    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackground: ImageView =
                    findViewById(R.id.iv_background)//assign it to your image background
                imageBackground.setImageURI(result.data?.data)//URI is location on your device. Not pulling in the picture
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    proceedWithPermissions()

                } else {
                    if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        Toast.makeText(
                            this,
                            "Change permissions in settings to use this functionality",
                            Toast.LENGTH_LONG
                        ).show()

                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())



        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint =
            linearLayoutPaintColors[2] as ImageButton // ImageButton at position 1 which is the... what was I saying? lol
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            clickedButton = 2
            shareImagePermissions()
        }

        val ibPickColor: ImageButton = findViewById(R.id.ib_palette)
        ibPickColor.setOnClickListener {
            showCustomColorPicker()
        }

        val ibImage: ImageButton = findViewById(R.id.ib_image)
        ibImage.setOnClickListener {

            val popupMenu = PopupMenu(this, ibImage)
            popupMenu.inflate(R.menu.background_menu)
            popupMenu.show()

            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {

                R.id.it_set_background -> {
                    clickedButton = 1
                    setBackgroundPermissions()
                    }
                R.id.it_clear_background -> {
                    clearBackground()
                }
            }
                true
            }
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }

        val ibDelete: ImageButton = findViewById(R.id.ib_clear)
        ibDelete.setOnClickListener{
            clearDrawing()
        }    }
    private fun showBrushSizeChooserDialog() { //use show and dialog on purpose in name
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }//good
    fun paintClicked(view: View) { //this ties to the onClick in xml file
        if (view !== mImageButtonCurrentPaint) {//if the button clicked is not the one already selected
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString() //using this rather than ID... very cool
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }

    }//good
    private fun showCustomColorPicker() { //use show and dialog on purpose in name
        val colorDialog = Dialog(this)
        colorDialog.setContentView(R.layout.custom_picker)
        colorDialog.setTitle("Custom Color: ")
        val picker = colorDialog.findViewById<ColorPicker>(R.id.picker)

        picker.setOnColorChangedListener {
            drawingView?.setCustomColor(picker.color)
        }

        colorDialog.show()


    }//good
    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(
            view.width,
            view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap

    }//good
    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "KidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png"
                    )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saving: $result", Toast.LENGTH_SHORT
                            ).show()

                            shareImage(result)

                        }
                    }

                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }//good
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.progress_dialog)
        customProgressDialog?.show()

    }//good
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }//good
    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()

    }
    private fun shareImage(result: String) {

                    /*MediaScannerConnection.scanFile(this, arrayOf(result), null) { path, uri ->

                        //val imageUri: Uri = result
                        //Log.i("check", uri.toString())
                        val shareIntent = Intent().apply{
                        action = Intent.ACTION_SEND
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uri) //uri is what we are sharing
                        type = "image/png"

                        }
                        startActivity(Intent.createChooser(shareIntent, "Share"))
                    }*/

        }
    private fun shareImagePermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                //proceed with activity
                proceedWithPermissions()
                }

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                showRationaleDialog(
                    "Kids Drawing App Write Access Required to Share Image", "Update permissions in " +
                            "setting to share images")
            }
            else -> {
                //ask for permission here
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ))

            }
        }

    }
    private fun setBackground() {

            val pickIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            openGalleryLauncher.launch(pickIntent)

    }
    private fun clearBackground(){
        val imageBackground: ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageDrawable(null)
    }
    private fun setBackgroundPermissions(){
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                proceedWithPermissions()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                showRationaleDialog(
                    "Kids Drawing App Read Access Required to Share Image", "Update permissions in " +
                            "setting to change the background")
            }
            else -> {
                //ask for permission here
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ))
            }
        }

    }
    private fun saveImage(){
        showProgressDialog()
        lifecycleScope.launch {
            val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
            saveBitmapFile(getBitmapFromView(flDrawingView))
        }
    }


    private fun proceedWithPermissions(){
        if (clickedButton==1){
            setBackground()
        }else if (clickedButton ==2){
            saveImage()
        }

    }

    private fun clearDrawing(){


        val builder = android.app.AlertDialog.Builder(this)
        //set title
        builder.setTitle("Kids Drawing App")
            .setMessage("Are you sure you want to delete your drawing?")
            //performing positive action
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, _ ->
                dialogInterface.dismiss()
                clearBackground()
                drawingView?.onDelete()
            })
            //performing negative action
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialogInterface, _ ->
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                dialogInterface.dismiss()
            })

            .setCancelable(false) //can't exit dialog by clicking outside of it
            .create()
            .show()
    }


}



