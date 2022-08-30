package com.rinoarias.scannerbarcode

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rinoarias.scannerbarcode.dataadapters.ListaProductosSubDetailAdapter
import com.rinoarias.scannerbarcode.models.Producto

class MainActivity : AppCompatActivity() {

    lateinit  var layoutManager: RecyclerView.LayoutManager
    lateinit public var adapter: ListaProductosSubDetailAdapter

    var getResult  =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == RESULT_OK){
                var listArrayNewProductos = it.data?.getSerializableExtra("data") as ArrayList<ArrayList<String>>
                listArrayNewProductos.forEach{
                      adapter.addItem(Producto(it.get(0).toString(),it.get(1).toString(),
                          it.get(2).toString(),it.get(3).toString(),it.get(4).toString()))
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
        listArrayProductos.add(Producto("001","1","10","Producto1","10"))
        listArrayProductos.add(Producto("002","1","10","Producto2","10"))
        listArrayProductos.add(Producto("003","1","10","Producto3","10"))
        listArrayProductos.add(Producto("004","1","10","Producto4","10"))
        listArrayProductos.add(Producto("005","1","10","Producto5","10"))

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
}