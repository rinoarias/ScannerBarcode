package com.rinoarias.scannerbarcode.models

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Producto(var codigo: String, var cantidad: String, var pvp: String,
               var descripcion: String) {

    constructor (a: JSONObject): this(a.getString("codigo").toString(),
        a.getString("cantidad").toString(), a.getString("pvp").toString(),
        a.getString("descripcion").toString()){

    }

    companion object {
        @Throws(JSONException::class)
        fun JsonObjectsBuild(datos: JSONArray): ArrayList<Producto> {
            val productos: ArrayList<Producto> = ArrayList<Producto>()
            var i = 0
            while (i < datos.length())
                productos.add(Producto(datos.getJSONObject(i++)))

            return productos
        }
    }
}