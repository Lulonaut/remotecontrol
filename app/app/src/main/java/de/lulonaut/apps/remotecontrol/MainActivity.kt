package de.lulonaut.apps.remotecontrol

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
    private var leftToggle: Boolean = false
    private var rightToggle: Boolean = false
    private var upToggle: Boolean = false
    private var downToggle: Boolean = false
    private var sendQueue: MutableList<String> = ArrayList()
    private var connected: Boolean = false
    private lateinit var connectionStatusText: TextView
    private var changing: Boolean = false
    private var speed: Int = 0

    private fun sendMessage(message: String) {
        if (!connected) return
        try {
            outputStream.write((message + "\n").toByteArray())
            outputStream.flush()
        } catch (e: SocketException) {
            Toast.makeText(applicationContext, "Connection broken!", Toast.LENGTH_LONG).show()
            connected = false
            connectionStatusText.text = getString(R.string.status_disconnected)
        }
    }

    private fun stopMouse() {
        sendQueue.add("mouse stop _ _")
        leftToggle = false
        rightToggle = false
        upToggle = false
        downToggle = false
    }

    private fun startMouseSetup() {
        leftToggle = false
        rightToggle = false
        upToggle = false
        downToggle = false
    }

    private fun startMouse(message: String) {
        sendQueue.add("$message $speed")
    }

    private fun connect() {
        Thread {
            Looper.prepare()
            try {
                socket = Socket("192.168.178.61", 7926)
                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()
                Toast.makeText(applicationContext, "Connected!", Toast.LENGTH_SHORT).show()
                connected = true
                connectionStatusText.text = getString(R.string.status_connected)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "Could not connect to PC", Toast.LENGTH_SHORT)
                    .show()
                return@Thread
            }
            //keep alive loop
            var counter = 0
            while (true) {
                counter++
                if (counter == 200) {
                    sendQueue.add("__KEEP_ALIVE")
                    counter = 0
                }
                if (!changing) {
                    for (item in sendQueue) {
                        sendMessage(item)
                    }
                    sendQueue.clear()
                }
                TimeUnit.MICROSECONDS.sleep(10000)
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val ipTextField: EditText = findViewById(R.id.text_ip_address)
        val speedTextField: EditText = findViewById(R.id.text_pixel_speed)

        val sharedPreferences: SharedPreferences =
            getSharedPreferences(getString(R.string.preference_key), Context.MODE_PRIVATE)

        ipTextField.setText(sharedPreferences.getString("remoteIP", "192.168.178.61"))
        speedTextField.setText(sharedPreferences.getInt("pixel_speed", 2).toString())
        speed = speedTextField.text.toString().toInt()

        connectionStatusText = findViewById(R.id.text_connection_status)
        val buttonLeft: Button = findViewById(R.id.button_left)
        val buttonRight: Button = findViewById(R.id.button_right)
        val buttonUp: Button = findViewById(R.id.button_up)
        val buttonDown: Button = findViewById(R.id.button_down)

        val buttonRetry: Button = findViewById(R.id.button_retry)
        val buttonSubmit: Button = findViewById(R.id.button_submit)
        val buttonClick: Button = findViewById(R.id.button_click)
        val sendTextView: EditText = findViewById(R.id.text_send_to_pc)

        connect()

        buttonRetry.setOnClickListener {
            sharedPreferences.edit().putString("remoteIP", ipTextField.text.toString()).apply()
            connect()
        }

        buttonClick.setOnClickListener {
            speed = speedTextField.text.toString().toInt()
            sharedPreferences.edit().putInt("pixel_speed", speedTextField.text.toString().toInt())
                .apply()
            sendQueue.add("click")
        }


        buttonSubmit.setOnClickListener {
            val text: String = sendTextView.text.toString()
            sendTextView.setText("")
            changing = true
            text.chars().forEach { c ->
                if (c.toChar() == ' ') {
                    sendQueue.add("keyboard space 0")
                }
                sendQueue.add("keyboard " + c.toChar() + " 0")
                TimeUnit.MILLISECONDS.sleep(50)
            }
            changing = false
        }

        buttonLeft.setOnClickListener {
            leftToggle = !leftToggle
            if (!leftToggle) {
                stopMouse()
            } else {
                startMouseSetup()
                leftToggle = true
                startMouse("mouse start left")
            }
        }

        buttonRight.setOnClickListener {
            rightToggle = !rightToggle
            if (!rightToggle) {
                stopMouse()
            } else {
                startMouseSetup()
                rightToggle = true
                startMouse("mouse start right")
            }
        }

        buttonUp.setOnClickListener {
            upToggle = !upToggle
            if (!upToggle) {
                stopMouse()
            } else {
                startMouseSetup()
                upToggle = true
                startMouse("mouse start up")
            }
        }

        buttonDown.setOnClickListener {
            downToggle = !downToggle
            if (!downToggle) {
                stopMouse()
            } else {
                startMouseSetup()
                downToggle = true
                startMouse("mouse start down")
            }
        }
    }
}
