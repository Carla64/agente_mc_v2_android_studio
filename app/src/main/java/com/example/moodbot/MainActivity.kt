package com.example.moodbot

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodbot.databinding.ActivityMainBinding
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivity:ActivityMainBinding
    private lateinit var messageList:ArrayList<MessageClass>
    private val USER = 0
    private val BOT = 1
    private val IMAGE=2
    private lateinit var adapter: MessageAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)
        messageList = ArrayList<MessageClass>()
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        mainActivity.messageList.layoutManager = linearLayoutManager
        adapter = MessageAdapter(this,messageList)
        adapter.setHasStableIds(true)
        mainActivity.messageList.adapter = adapter
        mainActivity.sendBtn.setOnClickListener{
            val msg = mainActivity.messageBox.text.toString()
            sendMessage(msg)
            Toast.makeText(this, "Mensaje enviado", Toast.LENGTH_SHORT).show()
            mainActivity.messageBox.setText("")
        }

    }
    // Incluye el tracker
    fun sendMessage(message:String){
        var userMessage = MessageClass()
        if(message.isEmpty()){
            Toast.makeText(this,"Escribe tu mensaje",Toast.LENGTH_SHORT).show()
        }
        else{
            userMessage = MessageClass(message,USER)
            messageList.add(userMessage)
            adapter.notifyDataSetChanged()
        }
        val okHttpClient = OkHttpClient()
        val retrofit = Retrofit.Builder().baseUrl("https://80f5-201-141-108-246.ngrok.io/webhooks/rest/").client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build()
        val messagerSender = retrofit.create(MessageSender::class.java)
        val response = messagerSender.messageSender(userMessage)
        //Tracker - Esperar respuesta bot
        response.enqueue(object: Callback<ArrayList<BotResponse>>{
            override fun onResponse(
                call: Call<ArrayList<BotResponse>>,
                response: Response<ArrayList<BotResponse>>
            ) {
                if(response.body() != null || response.body()?.size != 0){
                    if(response.body()!!.size>1){
                        for(message in 0..response.body()!!.size-1) {
                            val currentMessage = response.body()!![message]
                            if(currentMessage.image.isNotEmpty()){
                                messageList.add(MessageClass(currentMessage.image,IMAGE))
                            }else if(currentMessage.text.isNotEmpty()) {
                                messageList.add(MessageClass(currentMessage.text,BOT))
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }else{
                        val currentMessage = response.body()!![0]
                        if(currentMessage.text.isNotEmpty()){
                            messageList.add(MessageClass(currentMessage.text,BOT))
                        }else if(currentMessage.image.isNotEmpty()){
                            messageList.add(MessageClass(currentMessage.image,IMAGE))
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<ArrayList<BotResponse>>, t: Throwable) {
                val message = "Verifica tu conexión"
                messageList.add(MessageClass(message,BOT))
            }

        })
    }
}
