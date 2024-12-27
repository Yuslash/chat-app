package com.example.anonymous2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : ComponentActivity() {

    private lateinit var userId: EditText
    private lateinit var password: EditText
    private lateinit var loginbtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        userId = findViewById(R.id.identity)
        password = findViewById(R.id.input2)
        loginbtn = findViewById(R.id.loginbtn)

        val scrollView = findViewById<HorizontalScrollView>(R.id.horizotanlMan)
        val container = findViewById<LinearLayout>(R.id.imageAnimationContainer)

        startSeamlessScrolling(scrollView, container)

        loginbtn.setOnClickListener {
            val id = userId.text.toString()
            val pass = password.text.toString()

            if (id.isBlank() || pass.isBlank()) {
                Toast.makeText(this, "Please enter userid and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(id, pass)

            RetrofitClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val intent = Intent(this@LoginActivity, ChatRoomActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "App call failed ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun startSeamlessScrolling(scrollView: HorizontalScrollView, container: LinearLayout) {
        scrollView.post {
            val totalWidth = container.measuredWidth
            val initialX = 0
            val speed = 5

            scrollView.scrollTo(initialX, 0)

            scrollView.post(object : Runnable {
                override fun run() {
                    val currentX = scrollView.scrollX
                    if (currentX >= totalWidth / 2) {
                        scrollView.scrollTo(0, 0)
                    } else {
                        scrollView.scrollBy(speed, 0)
                    }
                    scrollView.postDelayed(this, 16)
                }
            })
        }
    }
}
