package com.rinoarias.scannerbarcode

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.rinoarias.scannerbarcode.models.Producto
import com.rinoarias.scannerbarcode.utils.BeepManager
import com.rinoarias.scannerbarcode.utils.CameraXViewModel
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class actCapturaProductos : AppCompatActivity() {

    private var api_productos : String = "https://my-json-server.typicode.com/rinoarias/ScannerBarcode/Productos"

    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    private lateinit var txtDesc:TextView
    private lateinit var txtCant:TextView
    private lateinit var txtPVP:TextView
    private lateinit var txtSubTotal:TextView

    var listArrayProductos = ArrayList<ArrayList<String>>()

    lateinit var beepManager: BeepManager
    private var lastScan = Date()
    private var lastCode: String = ""

    private val screenAspectRatio: Int
        get() {
            // Get screen metrics used to setup camera for full screen resolution
            val metrics = DisplayMetrics().also { previewView?.display?.getRealMetrics(it) }
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_act_captura_productos)

        setupCamera()

        beepManager = BeepManager(this)


        txtDesc = findViewById(R.id.lblDesc);
        txtCant = findViewById(R.id.txtCantidad);
        txtPVP = findViewById(R.id.txtPVP);
        txtSubTotal = findViewById(R.id.txtSubT);

        txtDesc.text = "ESCANEAR CÓDIGO DE PRODUCTO"
        txtCant.text ="0"
        txtPVP.text="0"
        txtSubTotal.text="0"


        txtCant.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {   }
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                try {
                    txtSubTotal.text =  (s.toString().toFloat() *
                                        txtPVP.text.toString().toFloat()).toString()
                } catch (e:Exception){
                    txtSubTotal.text = "0"
                }

            }
        })


    }


    fun  clickAddProductBt(view: View){
        listArrayProductos.add(ArrayList<String>(Arrays.asList(
            txtDesc.text.toString(), txtCant.text.toString(), txtPVP.text.toString(),
            txtDesc.text.toString())))

        txtDesc.text = "Scanear Código"
        txtCant.text ="0"
        txtPVP.text="0"
        txtSubTotal.text="0"

    }

    fun  clickBackFactura(view: View){

        val data = Intent()
        data.putExtra ("data", listArrayProductos as Serializable)
        setResult(RESULT_OK, data)
        finish()

    }

    private fun setupCamera() {
        previewView = findViewById(R.id.preview_view)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(this) { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                if (isCameraPermissionGranted()) {
                    bindCameraUseCases()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        PERMISSION_CAMERA_REQUEST
                    )
                }
            }
    }

    private fun bindCameraUseCases() {
        bindPreviewUseCase()
        bindAnalyseUseCase()
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(previewView!!.display.rotation)
            .build()
        previewUseCase!!.setSurfaceProvider(previewView!!.surfaceProvider)

        try {
            cameraProvider!!.bindToLifecycle(
                /* lifecycleOwner= */this,
                cameraSelector!!,
                previewUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    private fun bindAnalyseUseCase() {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        // BarcodeScannerOptions.Builder()
        //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        //     .build();
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }

        analysisUseCase = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(previewView!!.display.rotation)
            .build()

        // Initialize our background executor
        val cameraExecutor = Executors.newSingleThreadExecutor()

        analysisUseCase?.setAnalyzer(
            cameraExecutor,
            ImageAnalysis.Analyzer { imageProxy ->
                processImageProxy(barcodeScanner, imageProxy)
            }
        )

        try {
            cameraProvider!!.bindToLifecycle(
                /* lifecycleOwner= */this,
                cameraSelector!!,
                analysisUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if(barcodes.size>0) {

                    if(lastCode != barcodes.get(0).rawValue) {
                        val current = Date()
                        val diff = current.time - lastScan.time
                        if (diff >= 1000 ) {
                            lastScan = current
                            lastCode = barcodes.get(0).rawValue.toString()

                            var producto: Producto = obtenerProducto(lastCode)

                            txtDesc.setText(producto.descripcion)
                            txtCant.text = "1"
                            txtPVP.text = producto.pvp
                            txtSubTotal.text = producto.pvp

                            beepManager.playBeepSound()
                        }
                    }
                }
            }
            .addOnFailureListener {
                txtDesc.text = "Scanear Código"
                txtCant.text ="0"
                txtPVP.text="0"
                txtSubTotal.text="0"
            }.addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                imageProxy.close()
            }
    }

    private fun obtenerProducto(codigoBarra: String): Producto {
        var producto : Producto = Producto("0", "0", "0", "Not found")
        var requestJson : JsonArrayRequest = JsonArrayRequest(
            Request.Method.GET,
            api_productos,
            null,
            { response ->
                try {
                    for (i in 0 until response.length()){
                        var item = response.getJSONObject(i)
                        if(item.get("codigo").toString() == codigoBarra){
                            producto = Producto(item)
                            Toast.makeText(this, "Producto: " + producto.descripcion, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al obtener los datos: $e", Toast.LENGTH_LONG).show()
                    System.out.println(e.toString())
                }
            },
            {
                Toast.makeText(this, "Error al obtener los datos: $it", Toast.LENGTH_LONG).show()
                System.out.println(it.toString())
            })
        var requestQueue : RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(requestJson)
        return producto
    }

    /**
     *  [androidx.camera.core.ImageAnalysis], [androidx.camera.core.Preview] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (isCameraPermissionGranted()) {
                bindCameraUseCases()
            } else {
                Log.e(TAG, "no camera permission")
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSION_CAMERA_REQUEST = 1

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }


}