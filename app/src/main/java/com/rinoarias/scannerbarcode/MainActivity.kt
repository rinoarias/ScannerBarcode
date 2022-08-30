package com.rinoarias.scannerbarcode

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.rinoarias.scannerbarcode.dataadapters.ListaProductosSubDetailAdapter
import com.rinoarias.scannerbarcode.models.Cliente
import com.rinoarias.scannerbarcode.models.Producto

class MainActivity : AppCompatActivity() {

    val URL_API_CLIENTES : String = "https://my-json-server.typicode.com/rinoarias/ScannerBarcode/Clientes"

    lateinit var requestQueue : RequestQueue
    lateinit  var layoutManager: RecyclerView.LayoutManager
    lateinit public var adapter: ListaProductosSubDetailAdapter

    var getResult  =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == RESULT_OK){
                var listArrayNewProductos = it.data?.getSerializableExtra("data") as ArrayList<ArrayList<String>>
                listArrayNewProductos.forEach{
                      adapter.addItem(Producto(it.get(0).toString(),it.get(1).toString(),
                          it.get(2).toString(),it.get(3).toString()))
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val recyclerView = findViewById<RecyclerView>(R.id.rvListaProductos)
        recyclerView.layoutManager = layoutManager

        var listArrayProductos = ArrayList<Producto>()
        //listArrayProductos.add(Producto("001","1","10","Producto1"))
        //listArrayProductos.add(Producto("002","1","10","Producto2"))
        //listArrayProductos.add(Producto("003","1","10","Producto3"))
        //listArrayProductos.add(Producto("004","1","10","Producto4"))
        //listArrayProductos.add(Producto("005","1","10","Producto5"))

        val resId = R.anim.layout_animation_down_to_up
        val animation = AnimationUtils.loadLayoutAnimation(
            applicationContext,
            resId
        )
        recyclerView.layoutAnimation = animation
        adapter = ListaProductosSubDetailAdapter(listArrayProductos)
        recyclerView.adapter = adapter

    }


    fun  clickbutton(view: View){
        val intent = Intent(this.applicationContext, actCapturaProductos::class.java)
        getResult.launch(intent)

    }

    fun clickBtnBuscarCliente(view: View){

        var txtRUC : EditText = findViewById(R.id.txtRUC)
        var txtCliente : EditText = findViewById(R.id.txtCliente)
        var txtDireccion : EditText = findViewById(R.id.txtDireccion)
        var txtTelefono : EditText = findViewById(R.id.txtTelefono)
        var txtEmail : EditText = findViewById(R.id.txtEmail)



        var requestJson : JsonArrayRequest = JsonArrayRequest(
            Request.Method.GET,
            URL_API_CLIENTES,
            null,
            { response ->
                try {
                    var clientePredeterminado : Cliente = Cliente(response.getJSONObject(0))
                    for (i in 0 until response.length()){
                        var item = response.getJSONObject(i)
                        var cliente = Cliente(item)
                        if(cliente.identificacion == txtRUC.text.toString()){

                            txtRUC.setText(cliente.identificacion)
                            txtCliente.setText(cliente.razonSocial)
                            txtDireccion.setText(cliente.direccion)
                            txtTelefono.setText(cliente.telefono)
                            txtEmail.setText(cliente.email)

                            Toast.makeText(this, "Cliente: " + cliente.razonSocial, Toast.LENGTH_SHORT).show()
                            break
                        }
                        else if(i == response.length()-1 && cliente.identificacion != txtRUC.text.toString()){
                            txtRUC.setText(clientePredeterminado.identificacion)
                            txtCliente.setText(clientePredeterminado.razonSocial)
                            txtDireccion.setText(clientePredeterminado.direccion)
                            txtTelefono.setText(clientePredeterminado.telefono)
                            txtEmail.setText(clientePredeterminado.email)
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
        requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(requestJson)
    }
}