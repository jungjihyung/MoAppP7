package com.example.opencvproject

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.opencvproject.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.util.Date

private const val TAG = "TEST_OPEN_CV_ANDROID"
private const val REQUEST_IMAGE_CAPTURE = 1

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var textViewRGB: TextView
    private lateinit var textViewColor: TextView
    private lateinit var bitmap: Bitmap

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(applicationContext, "권한 허용 필요", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermissionLauncher.launch("android.permission.CAMERA")

        // OpenCV초기화
        OpenCVLoader.initDebug()

        imageView = findViewById(R.id.imageView)
        textViewRGB = findViewById(R.id.textViewRGB)
        textViewColor = findViewById(R.id.textViewColor)
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.img1)


        imageView.setImageBitmap(bitmap)

        // 초기 색상을 저장할 변수들
        val originalRedColor = binding.red.backgroundTintList?.defaultColor
        val originalGreenColor = binding.green.backgroundTintList?.defaultColor
        val originalBlueColor = binding.blue.backgroundTintList?.defaultColor

        //버튼 클릭시 버튼 색상 변경
        val click = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        // 모든 버튼의 색상을 원래대로 돌리는 함수
        fun resetButtonColors() {
            binding.red.setBackgroundColor(originalRedColor ?: click)
            binding.green.setBackgroundColor(originalGreenColor ?: click)
            binding.blue.setBackgroundColor(originalBlueColor ?: click)
        }

        imageView.setOnTouchListener { v, event ->
            resetButtonColors()
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 이미지 뷰의 너비와 높이를 가져옵니다.
                val imageViewWidth = v.width
                val imageViewHeight = v.height

                // 이미지의 실제 너비와 높이를 가져옵니다.
                val bitmapWidth = bitmap.width
                val bitmapHeight = bitmap.height

                // 이미지 뷰와 이미지의 크기 사이의 비율을 계산합니다.
                val widthRatio = bitmapWidth.toFloat() / imageViewWidth
                val heightRatio = bitmapHeight.toFloat() / imageViewHeight

                // 터치 이벤트의 좌표를 실제 이미지의 좌표로 변환합니다.
                val x = (event.x * widthRatio).toInt()
                val y = (event.y * heightRatio).toInt()


                val hsv = getHSV(x, y)
                val rgb = HSVtoRGB(hsv)
                showRGB(rgb)
                showColorName(hsv)

                // 클릭한 부분을 표시할 원을 생성
                val circleBitmap =
                    Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(circleBitmap)
                val circlePaint = Paint().apply {
                    color = Color.WHITE // 원 이미지의 색상을 설정합니다.
                    style = Paint.Style.STROKE
                    strokeWidth = 20f
                }

                // 클릭한 부분의 좌표를 중심으로 하는 원의 반지름을 설정합니다.
                val radius = 50f

                canvas.drawCircle(
                    x.toFloat(),
                    y.toFloat(),
                    radius,
                    circlePaint
                )

                // 클릭한 부분의 이미지에 원
                val combinedBitmap =
                    Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val combinedCanvas = Canvas(combinedBitmap)
                combinedCanvas.drawBitmap(bitmap, 0f, 0f, null)
                combinedCanvas.drawBitmap(circleBitmap, 0f, 0f, null)

                imageView.setImageBitmap(combinedBitmap)
            }

            true
        }


        val lowerRed1 = Scalar(0.0, 150.0, 100.0)
        val upperRed1 = Scalar(20.0, 255.0, 255.0)
        val lowerRed2 = Scalar(160.0, 150.0, 100.0)
        val upperRed2 = Scalar(180.0, 255.0, 255.0)

        val lowerGreen = Scalar(40.0, 150.0, 100.0)
        val upperGreen = Scalar(80.0, 255.0, 255.0)

        val lowerBlue = Scalar(100.0, 150.0, 100.0)
        val upperBlue = Scalar(140.0, 255.0, 255.0)

        binding.red.setOnClickListener {
            resetButtonColors() // 다른 버튼의 색상을 원래대로 돌리기
            val mat = Mat()
            val dst = Mat()
            val dst1 = Mat()
            val dst2 = Mat()
            Utils.bitmapToMat(bitmap, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2HSV)
            Core.inRange(mat, lowerRed1, upperRed1, dst1)
            Core.inRange(mat, lowerRed2, upperRed2, dst2)
            Core.bitwise_or(dst1, dst2, dst)
            val filter = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.RGB_565)
            Utils.matToBitmap(dst, filter)
            imageView.setImageBitmap(filter)
            binding.red.setBackgroundColor(click)

        }

        binding.green.setOnClickListener {
            resetButtonColors() // 다른 버튼의 색상을 원래대로 돌리기
            val mat = Mat()
            val dst = Mat()
            Utils.bitmapToMat(bitmap, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2HSV)
            Core.inRange(mat, lowerGreen, upperGreen, dst)
            val filter = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.RGB_565)
            Utils.matToBitmap(dst, filter)
            imageView.setImageBitmap(filter)
            binding.green.setBackgroundColor(click)
        }

        binding.blue.setOnClickListener {
            resetButtonColors() // 다른 버튼의 색상을 원래대로 돌리기
            val mat = Mat()
            val dst = Mat()
            Utils.bitmapToMat(bitmap, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2HSV)
            Core.inRange(mat, lowerBlue, upperBlue, dst)
            val filter = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.RGB_565)
            Utils.matToBitmap(dst, filter)
            imageView.setImageBitmap(filter)
            binding.blue.setBackgroundColor(click)
        }

        binding.Menu.setOnClickListener {
            showPopupMenu(it)
        }
    }


    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.main_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_camera -> {
                    // 카메라로 사진 찍기 동작 수행
                    openCamera(view)
                    true
                }
                R.id.menu_gallery -> {
                    // 갤러리에서 가져오기 동작 수행
                    openGallery(view)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    var pictureUri: Uri? = null
    private val getTakePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            pictureUri?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun createImageFile(): Uri? {
        val now = SimpleDateFormat("yyMMdd_HHmmss").format(Date())
        val content = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "img_$now.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content)
    }

    fun openCamera(View: View) {
        pictureUri = createImageFile()
        getTakePicture.launch(pictureUri)
    }

    private val pickGalleryImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(bitmap)
            }
        }

    fun openGallery(view: View) {
        pickGalleryImage.launch("image/*")
    }


    private fun getHSV(x: Int, y: Int): FloatArray {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)

        val pixel = mat.get(y, x)
        val rgb = IntArray(3)
        rgb[0] = pixel[0].toInt() and 0xFF // Red
        rgb[1] = pixel[1].toInt() and 0xFF // Green
        rgb[2] = pixel[2].toInt() and 0xFF // Blue

        val hsv = FloatArray(3)
        Color.RGBToHSV(rgb[0], rgb[1], rgb[2], hsv)

        return hsv
    }

    private fun HSVtoRGB(hsv: FloatArray): IntArray {
        val rgb = IntArray(3)
        val color = Color.HSVToColor(hsv)
        rgb[0] = (color shr 16) and 0xFF
        rgb[1] = (color shr 8) and 0xFF
        rgb[2] = color and 0xFF

        return rgb
    }

    private fun showRGB(rgb: IntArray) {
        val (red, green, blue) = rgb
        val rgbText = String.format("#%02X%02X%02X", red, green, blue)
        textViewRGB.text = "$rgbText"
    }

    private fun getColorName(hsv: FloatArray): String {
        val rgb = HSVtoRGB(hsv)
        val (red, green, blue) = rgb
        val colorMap = mapOf(
            "White(흰색)" to intArrayOf(255, 255, 255),
            "Black(검정색)" to intArrayOf(0, 0, 0),
            "Red(빨간색)" to intArrayOf(255, 0, 0),
            "Blue(파랑색)" to intArrayOf(0, 0, 255),
            "Yellow(노란색)" to intArrayOf(255, 195, 0),
            "Green(초록색)" to intArrayOf(0, 255, 0),
            "Viridian(짙은녹색)" to intArrayOf(0, 100, 0),
            "Olive Green(짙은 녹두색)" to intArrayOf(0, 128, 0),
            "Brown(갈색)" to intArrayOf(165, 42, 42),
            "Vandyke Brown(고동색)" to intArrayOf(88, 70, 48),
            "Purple(자주색)" to intArrayOf(128, 0, 128),
            "Orange(주황색)" to intArrayOf(255, 165, 0),
            "Pale Orange(연주황색)" to intArrayOf(255, 178, 102),
            "Pink(분홍색)" to intArrayOf(255, 192, 203),
            "Sky Blue(하늘색)" to intArrayOf(135, 206, 235),
            "Yellow Green(연두)" to intArrayOf(129, 193, 71),
            "Gray(회색)" to intArrayOf(128, 128, 128),
            "Ultramarine(군청색)" to intArrayOf(0, 0, 128),
            "Beige(베이지색)" to intArrayOf(245, 245, 220),
            "Ivory(아이보리색)" to intArrayOf(255, 255, 240),
            "Blue Green(청록색)" to intArrayOf(0, 255, 255),
            "lime(라임색)" to intArrayOf(191, 255, 0),
            "Lemon Yellow(레몬색)" to intArrayOf(255, 247, 0),
            "Prussian Blue(남색)" to intArrayOf(0, 0, 139),
            "Violet(남보라색)" to intArrayOf(83, 32, 161),
        )

        var ColorName = ""
        var minDistance = Double.MAX_VALUE

        for ((colorName, colorValue) in colorMap) {
            // 유클리어
            val distance = Math.sqrt(
                        0.3 * Math.pow(red - colorValue[0].toDouble(), 2.0) +
                        0.59 * Math.pow(green - colorValue[1].toDouble(), 2.0) +
                        0.11 * Math.pow(blue - colorValue[2].toDouble(), 2.0)
            )
            if (distance < minDistance) {
                minDistance = distance
                ColorName = colorName
            }
        }

        return ColorName
    }

    private fun showColorName(hsv: FloatArray) {
        val rgb = HSVtoRGB(hsv)
        val (red, green, blue) = rgb
        val colorName = getColorName(hsv)
        val colorText = " $colorName"
        textViewColor.text = colorText

        val colorBox = findViewById<TextView>(R.id.colorBox)
        colorBox.setBackgroundColor(android.graphics.Color.rgb(red, green, blue))
    }

}