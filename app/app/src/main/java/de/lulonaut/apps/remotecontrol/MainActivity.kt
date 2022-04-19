package de.lulonaut.apps.remotecontrol

import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var socket: Socket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    var leftToggle = false
    var rightToggle = false
    var upToggle = false
    var downToggle = false
    var sendQueue: MutableList<String> = ArrayList()

    private fun onClick() {

    }

    private fun sendMessage(message: String) {
        try {
            outputStream.write((message + "\n").toByteArray())
            outputStream.flush()
        } catch (e: SocketException) {
            Toast.makeText(applicationContext, "Connection broken!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Thread {
            Looper.prepare()
            try {
                socket = Socket("192.168.178.61", 7926)
                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()
                Toast.makeText(applicationContext, "Connected!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "Could not connect to PC", Toast.LENGTH_LONG)
                    .show()
                return@Thread
            }
            //keep alive loop
            var counter = 0
            while (true) {
                if (counter == 1000) {
                    sendQueue.add("__KEEP_ALIVE")
                }
                for (item in sendQueue) {
                    sendMessage(item)
                }
                sendQueue.clear()
                TimeUnit.MICROSECONDS.sleep(10000)
            }
        }.start()

        val buttonLeft: Button = findViewById(R.id.button_left)
        val buttonRight: Button = findViewById(R.id.button_right)
        val buttonUp: Button = findViewById(R.id.button_up)
        val buttonDown: Button = findViewById(R.id.button_down)

        buttonLeft.setOnClickListener {
            leftToggle = !leftToggle
            if (!leftToggle) {
                sendQueue.add("mouse stop _")
            } else {
                sendQueue.add("mouse start left")
            }
        }

        buttonRight.setOnClickListener {
            rightToggle = !rightToggle
            if (!rightToggle) {
                sendQueue.add("mouse stop _")
            } else {
                sendQueue.add("mouse start right")
            }
        }

        buttonUp.setOnClickListener {
            upToggle = !upToggle
            if (!upToggle) {
                sendQueue.add("mouse stop _")
            } else {
                sendQueue.add("mouse start up")
            }
        }

        buttonDown.setOnClickListener {
            downToggle = !downToggle
            if (!downToggle) {
                sendQueue.add("mouse stop _")
            } else {
                sendQueue.add("mouse start down")
            }
        }
    }
}
