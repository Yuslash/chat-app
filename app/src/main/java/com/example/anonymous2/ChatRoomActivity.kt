
package com.example.anonymous2

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class ChatRoomActivity : ComponentActivity() {

    private lateinit var socket: Socket
    private lateinit var sendButton: Button
    private lateinit var onlineUsers: TextView

    private var selectedEffect: String = "scale_sender"
    private val userColors: MutableMap<String, Int> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.chat_room)

        val messageContainer = findViewById<LinearLayout>(R.id.messageContainer)
        val messageScrollView = findViewById<ScrollView>(R.id.messageScrollView)

        socket = IO.socket()
        socket.connect()

        socket.on("broadcast_message") { args ->
            runOnUiThread {
                val data = args[0] as JSONObject
                val senderId = data.getString("id")
                val message = data.getString("message")

                val messageView = TextView(this).apply {
                    text = if (senderId == socket.id()) message else "$message"
                    setPadding(40, 40, 40, 40)
                    setTextAppearance(
                        if (senderId == socket.id()) R.style.customStyles else R.style.receiverMessageBackground
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 40, 0, 0)
                        if (senderId != socket.id()) gravity = Gravity.START
                    }

                    if (senderId == socket.id()) {
                        setBackgroundResource(R.drawable.message_background)
                    } else {
                        val backgroundDrawable = resources.getDrawable(R.drawable.receivermessage, null)
                        backgroundDrawable?.mutate()?.setTint(getColorForUser(senderId))
                        background = backgroundDrawable
                    }
                }

                applyMessageAnimation(messageView, selectedEffect)

                messageView.setOnLongClickListener {
                    val effects = arrayOf("Scale Sender", "Scale Receiver", "Shake", "Fade In", "Rotate", "Bounce")
                    val effectKeys = arrayOf("scale_sender", "scale_receiver", "shake", "fade_in", "rotate", "bounce")

                    val builder = android.app.AlertDialog.Builder(this@ChatRoomActivity)
                    builder.setTitle("Choose an Animation Effect")
                    builder.setItems(effects) { _, which ->
                        selectedEffect = effectKeys[which]
                        applyMessageAnimation(messageView, selectedEffect)
                    }
                    builder.show()

                    true
                }

                messageContainer.addView(messageView)

                messageContainer.post {
                    messageScrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }

        onlineUsers = findViewById(R.id.onlineCount)
        socket.on("online_count") { args ->
            runOnUiThread {
                val onlineCount = args[0] as Int
                onlineUsers.text = "$onlineCount"
            }
        }

        sendButton = findViewById(R.id.sendbtn)
        val messageInput = findViewById<EditText>(R.id.inputmessage)

        sendButton.setOnClickListener {
            val text = messageInput.text.toString()
            if (text.isNotEmpty()) {
                socket.emit("message", text)
                messageInput.text.clear()

                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(messageInput.windowToken, 0)
            }
        }
    }

    private fun applyMessageAnimation(messageView: TextView, effect: String) {
        val animation = when (effect) {
            "scale_sender" -> ScaleAnimation(
                0.7f, 1f,
                0.7f, 1f,
                Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 1f
            )
            "scale_receiver" -> ScaleAnimation(
                0.7f, 1f,
                0.7f, 1f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f
            )
            "shake" -> TranslateAnimation(
                -10f, 10f,
                0f, 0f
            ).apply {
                repeatCount = 5
                repeatMode = Animation.REVERSE
            }
            "fade_in" -> AlphaAnimation(0f, 1f)
            "rotate" -> RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            "bounce" -> TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f
            ).apply { interpolator = BounceInterpolator() }
            else -> null
        }

        animation?.apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            messageView.startAnimation(this)
        }
    }

    private fun getColorForUser(userId: String): Int {
        if (!userColors.containsKey(userId)) {
            val colors = listOf(
                "#FFCDD2", "#F8BBD0", "#E1BEE7", "#D1C4E9", "#C5CAE9",
                "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB", "#C8E6C9",
                "#DCEDC8", "#F0F4C3", "#FFECB3", "#FFE0B2", "#FFCCBC"
            )
            val color = colors[Math.abs(userId.hashCode() % colors.size)]
            userColors[userId] = android.graphics.Color.parseColor(color)
        }
        return userColors[userId]!!
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
    }
}
