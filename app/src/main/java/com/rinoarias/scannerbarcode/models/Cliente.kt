package com.rinoarias.scannerbarcode.models

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Cliente(var id: String, var identificacion: String, var razonSocial: String,
               var email: String, var placa: String, var telefono: String,
              var direccion: String) {

    constructor (a: JSONObject): this(a.getString("id").toString(),
        a.getString("identificacion").toString(), a.getString("razon_social").toString(),
        a.getString("email").toString(), a.getString("placa").toString(),
        a.getString("telefono").toString(), a.getString("direccion").toString()){

    }

    companion object {
        @Throws(JSONException::class)
        fun JsonObjectsBuild(datos: JSONArray): ArrayList<Cliente> {
            val clientes: ArrayList<Cliente> = ArrayList<Cliente>()
            var i = 0
            while (i < datos.length())
                clientes.add(Cliente(datos.getJSONObject(i++)))

            return clientes
        }
    }
}