package com.gnayuil.ahair

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Size
import android.view.*
import android.widget.GridView
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.mediapipe.components.CameraHelper.CameraFacing
import com.google.mediapipe.components.CameraXPreviewHelper
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.glutil.EglManager

class MainActivity : AppCompatActivity() {
    companion object {
        private val COLOR_BLOCK =
            arrayOf(
                "0000FF", "58C9B9", "DF405A", "F0F8FF", "FFD700", "3F4C77"
                , "7BCBED", "004369", "F3004B", "FEC0C1", "FFF75E"
            )
        private var BINARY_GRAPH_NAME = COLOR_BLOCK[0] + ".binarypb"

        private const val INPUT_VIDEO_STREAM_NAME = "input_video"
        private const val OUTPUT_VIDEO_STREAM_NAME = "output_video"
        private val CAMERA_FACING = CameraFacing.FRONT

        private const val FLIP_FRAMES_VERTICALLY = true

        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }
    }

    private lateinit var previewFrameTexture: SurfaceTexture

    private lateinit var previewDisplayView: SurfaceView

    private lateinit var eglManager: EglManager

    private lateinit var processor: FrameProcessor

    private lateinit var converter: ExternalTextureConverter

    private lateinit var cameraHelper: CameraXPreviewHelper

    private var initDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBottomSheet()

        AndroidAssetUtil.initializeNativeAssetManager(this)
        PermissionHelper.checkAndRequestCameraPermissions(this)
    }

    private fun initBottomSheet() {
        val bottomSheetLayout: ConstraintLayout = findViewById(R.id.bottom_sheet_layout)
        val bottomSheetArrowImageView: ImageView = findViewById(R.id.bottom_sheet_arrow)
        val gestureLayout: RelativeLayout = findViewById(R.id.gesture_layout)
        val sheetBehavior: BottomSheetBehavior<ConstraintLayout> =
            BottomSheetBehavior.from<ConstraintLayout>(bottomSheetLayout)
        val vto = gestureLayout.viewTreeObserver
        vto.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val height = gestureLayout.measuredHeight
                    sheetBehavior.peekHeight = height

                    initDone = true
                    rebuild()

                }
            })
        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetCallback() {
                override fun onStateChanged(
                    bottomSheet: View,
                    newState: Int
                ) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            bottomSheetArrowImageView.setImageResource(R.mipmap.icn_chevron_down)
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            bottomSheetArrowImageView.setImageResource(R.mipmap.icn_chevron_up)
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {
                        }
                        BottomSheetBehavior.STATE_SETTLING -> bottomSheetArrowImageView.setImageResource(
                            R.mipmap.icn_chevron_up
                        )
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        }
                    }
                }

                override fun onSlide(
                    bottomSheet: View,
                    slideOffset: Float
                ) {
                }
            })

        initColorBlock()
    }

    private fun initColorBlock() {
        val colorList = ArrayList<ColorBlock>()
        for (colorBlock in COLOR_BLOCK) {
            colorList.add(ColorBlock("#" + colorBlock))
        }

        val colorGallery = findViewById<GridView>(R.id.color_gallery)
        val adapter = ColorAdapter(this, colorList) { position ->
            colorBlockClick(position)
        }
        colorGallery.adapter = adapter
    }

    private fun colorBlockClick(position: Int) {
        if (position >= COLOR_BLOCK.size) {
            AlertDialog.Builder(this@MainActivity)
                .setMessage("More color or custom color is coming.")
                .setTitle("To Be Continued")
                .setPositiveButton("Got it", null)
                .create()
                .show()
        } else {
            BINARY_GRAPH_NAME = COLOR_BLOCK[position] + ".binarypb"
            if (PermissionHelper.cameraPermissionsGranted(this)) {
                rebuild()
            } else {
                PermissionHelper.checkAndRequestCameraPermissions(this)
            }
        }
    }

    private fun rebuild() {
        CameraX.unbindAll()

        previewDisplayView = SurfaceView(this)
        setupPreviewDisplayView()

        eglManager = EglManager(null)
        processor = FrameProcessor(
            this,
            eglManager.nativeContext,
            BINARY_GRAPH_NAME,
            INPUT_VIDEO_STREAM_NAME,
            OUTPUT_VIDEO_STREAM_NAME
        )
        processor.videoSurfaceOutput.setFlipY(FLIP_FRAMES_VERTICALLY)

        converter = ExternalTextureConverter(eglManager.context)
        converter.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter.setConsumer(processor)
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            startCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        if (initDone) {
            rebuild()
        }
    }

    override fun onPause() {
        super.onPause()
        if (initDone) {
            converter.close()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setupPreviewDisplayView() {
        previewDisplayView.visibility = View.GONE
        val viewGroup = findViewById<ViewGroup>(R.id.preview_display_layout)
        if (viewGroup.childCount > 1) {
            viewGroup.removeViews(0, viewGroup.childCount - 1)
        }
        viewGroup.addView(previewDisplayView)
        previewDisplayView
            .holder
            .addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        processor.videoSurfaceOutput.setSurface(holder.surface)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        val viewSize = Size(width, height)
                        val displaySize =
                            cameraHelper.computeDisplaySizeFromViewSize(viewSize)
                        converter.setSurfaceTextureAndAttachToGLContext(
                            previewFrameTexture, displaySize.width, displaySize.height
                        )
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        processor.videoSurfaceOutput.setSurface(null)
                    }
                })
    }

    private fun startCamera() {
        cameraHelper = CameraXPreviewHelper()
        cameraHelper.setOnCameraStartedListener { surfaceTexture: SurfaceTexture? ->
            if (surfaceTexture != null) {
                previewFrameTexture = surfaceTexture
                previewDisplayView.visibility = View.VISIBLE
            }
        }
        cameraHelper.startCamera(
            this,
            CAMERA_FACING,
            null
        )
    }
}
