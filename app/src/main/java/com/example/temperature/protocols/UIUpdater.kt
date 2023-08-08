package com.example.temperature.protocols

import org.json.JSONArray

interface UIUpdaterInterface {

    //fun resetUIWithConnection(status: Boolean)
    fun update(message: String)
}