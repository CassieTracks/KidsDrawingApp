package com.example.kidsdrawingapp

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import com.larswerkman.holocolorpicker.ColorPicker

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    //define variables
    private var mDrawPath : CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()


    //need to initialize the variables
    init{
        setUpDrawing()
    }

    fun onClickUndo (){
        if(mPaths.size>0){
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
        }
    }
    fun onClickRedo (){
        if(mUndoPaths.size>0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }
    }

    fun onDelete (){
        mPaths.clear()
        invalidate()

    }
    private fun setUpDrawing(){

        mDrawPaint = Paint()
        mDrawPath = CustomPath(color,mBrushSize)
        mDrawPaint!!.color = color //know this isn't null because we defined it, but can add an if check in
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)

    }

    // Change Canvas to Canvas? if fails
    override fun onDraw(canvas: Canvas?){
        super.onDraw(canvas)
        mCanvasBitmap.let {
            canvas?.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)//start at top left corner
        for(path in mPaths){ //This is what makes the lines persist on the screen
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas?.drawPath(path,mDrawPaint!!)
        }
        }
        if(!mDrawPath!!.isEmpty) { //This code is what shows you what you are drawing while you are drawing
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas?.drawPath(mDrawPath!!, mDrawPaint!!)
            Log.i("CassieCheckPaint",mDrawPaint!!.strokeWidth.toString())
            Log.i("CassieCheckPaint",mDrawPaint!!.color.toString())
        }


    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {  //when we press on the screen
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()
                if (touchX != null) { //null check otherwise use touchX?
                    if (touchY != null) {

                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }

                //return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchX != null) {
                    if (touchY != null) {

                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
                //return true
            }
            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath!!) //add drawings to an array so it keeps them
                mDrawPath= CustomPath(color, mBrushSize)

            //return true
            }
            else -> return false //don't do anything for rest of the events


        }

        invalidate()
        return true

    }

    fun setSizeForBrush(newSize : Float) {//public so you can use this in mainAcitivity
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize, resources.displayMetrics) //COMPLEX_UNIT_DIP is specified in TypedValue
        mDrawPaint!!.strokeWidth = mBrushSize

    }

    fun setColor(newColor: String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color




    }

    fun setCustomColor(newColor: Int){
        color = newColor
        mDrawPaint!!.color = color
        Log.i("troubles",newColor.toString())
        Log.i("troubles2",mDrawPaint!!.color.toString())

    }
    internal inner class CustomPath(var color: Int,
                                    var brushThickness: Float) : Path() {


    }


}