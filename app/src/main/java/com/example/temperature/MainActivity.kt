package com.example.temperature

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.AdapterView.VIEW_LOG_TAG
import androidx.appcompat.app.AppCompatActivity
import com.example.temperature.manager.MQTTConnectionParams
import com.example.temperature.manager.MQTTmanager
import com.example.temperature.protocols.UIUpdaterInterface
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), UIUpdaterInterface {

    var mqttManager: MQTTmanager? = null
    var firstrun = 1
    var temp:JSONArray = JSONArray()
    var humi:JSONArray = JSONArray()
    var dato:JSONArray = JSONArray()
    var kl:JSONArray = JSONArray()
    var noData = 0


    lateinit var datePickerDialog: DatePickerDialog


    override fun update(message: String) {
        val answer = JSONObject(message)
        kl = answer.getJSONArray("klok")
        dato = answer.getJSONArray("date")
        temp = answer.getJSONArray("tempList")
        humi = answer.getJSONArray("humidityList")
        noData = 0
        if (message.count() == 40 && firstrun == 0) {
            Toast.makeText(this@MainActivity, "Der er ikke noget data på den valgte dato", Toast.LENGTH_LONG).show()
            noData = 1
        }

        findViewById<TextView>(R.id.temp_now).text = temp[temp.length()-1].toString()+ " °C"
        findViewById<TextView>(R.id.date_now).text = dato[dato.length()-1].toString()
        findViewById<TextView>(R.id.kl_now).text = kl[kl.length()-1].toString()
        findViewById<TextView>(R.id.humiTxt).text = "Humidity:  " + humi[humi.length()-1].toString() +"%"
        val n = temp.length()-1
        var maxT = 0.0f
        var minT = 1000.0f
        for (i in 0..n) {
            if (temp[i].toString().toFloat() > maxT) {
                maxT = temp[i].toString().toFloat()
            }

            if(temp[i].toString().toFloat() < minT) {
                minT = temp[i].toString().toFloat()
            }
        }
        findViewById<TextView>(R.id.tvMaxMin).text = "Min: $minT °C \t\t\t\t Max: $maxT °C"
        if(firstrun == 1) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("ddMM")
            val formatted = current.format(formatter).toString()
            mqttManager?.publish("kennyGG/reqTemp",formatted)
            firstrun = 0
        }

        setLineChartData(temp,humi,kl)
        findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.dataChart).invalidate()
        findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.dataChart2).invalidate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var host = "tcp://" + "broker.hivemq.com" + ":1883"
        var topic = "kennyGG/temp"
        var connectionParams = MQTTConnectionParams("MQTTSample",host,topic,"","")
        mqttManager = MQTTmanager(connectionParams,applicationContext,this)
        mqttManager?.connect()
        val spnLocale = findViewById<View>(R.id.interSpin) as Spinner

        spnLocale.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                setLineChartData(temp,humi,kl)
                findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.dataChart).invalidate()
                findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.dataChart2).invalidate()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
                return
            }
        }
    }


    fun setLineChartData(datas:JSONArray, humis:JSONArray, times: JSONArray) {
        var n = datas.length()-1
        var xval = ArrayList<String>()
        var yval = ArrayList<Float>()
        var yval2 = ArrayList<Float>()
        var y_temp = ArrayList<Entry>()
        var y_humi = ArrayList<Entry>()

        var count = 0
        if (findViewById<Spinner>(R.id.interSpin).selectedItemPosition == 0) {
            for(i in 0..n-1){
                if(i % 1 == 0) {
                    val flt = datas[n - i].toString().toFloat()
                    val flt2 = humis[n - i].toString().toFloat()
                    xval.add(times[n - i].toString())
                    yval.add(flt)
                    yval2.add(flt2)
                    count++
                }
            }
        }
        else if (findViewById<Spinner>(R.id.interSpin).selectedItemPosition == 1) {
            for(i in 0..n-1){
                if(i % 3 == 0) {
                    val flt = datas[n - i].toString().toFloat()
                    val flt2 = humis[n - i].toString().toFloat()
                    xval.add(times[n - i].toString())
                    yval.add(flt)
                    yval2.add(flt2)
                    count++
                }
            }
        }
        else if (findViewById<Spinner>(R.id.interSpin).selectedItemPosition == 2) {
            for(i in 0..n-1){
                if(i % 6 == 0) {
                    val flt = datas[n - i].toString().toFloat()
                    val flt2 = humis[n - i].toString().toFloat()
                    xval.add(times[n - i].toString())
                    yval.add(flt)
                    yval2.add(flt2)
                    count++
                }
            }
        }

        xval.reverse()

        for(i in 0..count-1) {
            y_temp.add(Entry(yval[count-1-i],i))
            y_humi.add(Entry(yval2[count-1-i],i))
        }

        val linedataset = LineDataSet(y_temp, "Temperatur")
        linedataset.color = resources.getColor(R.color.purple_500)
        linedataset.lineWidth = 3F
        linedataset.circleRadius = 0f
        linedataset.axisDependency = YAxis.AxisDependency.LEFT
        linedataset.setDrawValues(false)

        val linedataset2 = LineDataSet(y_humi, "Humidity")
        linedataset2.color = resources.getColor(R.color.reddo)
        linedataset2.lineWidth = 3F
        linedataset2.circleRadius = 0f
        linedataset2.axisDependency = YAxis.AxisDependency.LEFT


        linedataset2.setDrawValues(false)

        val dataSets: ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(linedataset)
        dataSets.add(linedataset2)

        val data = LineData(xval,linedataset)
        val chart = findViewById<LineChart>(R.id.dataChart)
        chart.data = data
        chart.setBackgroundColor(resources.getColor(R.color.white))
        chart.axisRight.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        val data2 = LineData(xval,linedataset2)
        val chart2 = findViewById<LineChart>(R.id.dataChart2)
        chart2.data = data2
        chart2.setBackgroundColor(resources.getColor(R.color.white))
        chart2.axisRight.isEnabled = false
        chart2.setTouchEnabled(true)
        chart2.setPinchZoom(true)
        chart2.xAxis.position = XAxis.XAxisPosition.BOTTOM
    }

    fun refreshTemps(view: View) {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("ddMM")
        val formatted = current.format(formatter).toString()
        mqttManager?.publish("kennyGG/reqTemp",formatted)
    }

    fun changeDate(view: View) {
        findViewById<DatePicker>(R.id.date1).visibility = View.VISIBLE
        findViewById<Button>(R.id.submitbtn).visibility = View.VISIBLE
    }

    fun submitdate(view: View) {
        findViewById<DatePicker>(R.id.date1).visibility = View.INVISIBLE
        findViewById<Button>(R.id.submitbtn).visibility = View.INVISIBLE
        val day_ = findViewById<DatePicker>(R.id.date1).dayOfMonth
        val month_ = findViewById<DatePicker>(R.id.date1).month +1
        var dateStr =""
        if (day_ < 10) {
           dateStr = "0$day_"
        }
        else {
            dateStr = "$day_"
        }

        if (month_ < 10) {
            dateStr += "0$month_"
        }
        else {
            dateStr += "$month_"
        }
        findViewById<TextView>(R.id.date_now).text = dateStr.substring(0,2) +"/"+dateStr.substring(2,4)
        mqttManager?.publish("kennyGG/reqTemp",dateStr)
    }

    fun nextDay(view: View) {
        if (noData == 0) {
            val prevdate = dato[0].toString()
            val month_ = prevdate.substring(prevdate.length-2).toInt()
            val day_ = prevdate.substring(0,2).toInt()
            val localDate = LocalDate.of(2021, month_, day_)
            val nextday = localDate.plusDays(1)
            val DAY = nextday.dayOfMonth
            val MONTH = nextday.monthValue

            var dateStr =""
            if (day_ < 10) {
                dateStr = "0$DAY"
            }
            else {
                dateStr = "$DAY"
            }
            if (month_ < 10) {
                dateStr += "0$MONTH"
            }
            else {
                dateStr += "$MONTH"
            }

            findViewById<TextView>(R.id.date_now).text = dateStr.substring(0,2) +"/"+dateStr.substring(2,4)
            mqttManager?.publish("kennyGG/reqTemp",dateStr)
        }
        else {
            Toast.makeText(this@MainActivity, "Brug venligst kalenderen", Toast.LENGTH_LONG).show()
        }
    }
    fun prevDay(view: View) {
        if(noData == 0) {
            val prevdate = dato[0].toString()
            val month_ = prevdate.substring(prevdate.length-2).toInt()
            val day_ = prevdate.substring(0,2).toInt()
            val localDate = LocalDate.of(2021, month_, day_)
            val nextday = localDate.minusDays(1)
            val DAY = nextday.dayOfMonth
            val MONTH = nextday.monthValue

            var dateStr =""
            if (day_ < 10) {
                dateStr = "0$DAY"
            }
            else {
                dateStr = "$DAY"
            }
            if (month_ < 10) {
                dateStr += "0$MONTH"
            }
            else {
                dateStr += "$MONTH"
            }

            findViewById<TextView>(R.id.date_now).text = dateStr.substring(0,2) +"/"+dateStr.substring(2,4)
            mqttManager?.publish("kennyGG/reqTemp",dateStr)
        }
        else {
            Toast.makeText(this@MainActivity, "Brug venligst kalenderen", Toast.LENGTH_LONG).show()
        }
    }
}


