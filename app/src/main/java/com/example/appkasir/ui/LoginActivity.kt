package com.example.appkasir.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appkasir.R

class LoginActivity : AppCompatActivity() {
    private lateinit var edtUsername: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Check if user is already logged in
        val sharedPref = getSharedPreferences("AppKasir", MODE_PRIVATE)
        if (sharedPref.getBoolean("isLoggedIn", false)) {
            navigateToPOS()
            return
        }

        setupViews()
    }

    private fun setupViews() {
        edtUsername = findViewById(R.id.edtUsername)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtSignUp = findViewById(R.id.txtSignUp)

        btnLogin.setOnClickListener {
            performLogin()
        }

        txtSignUp.setOnClickListener {
            Toast.makeText(this, "Sign up feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val username = edtUsername.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        // Simple validation
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Basic login logic - for demo purposes
        // In production, this should validate against a backend or local database
        if (isValidCredentials(username, password)) {
            val userRole = getUserRole(username)
            // Save login state with role
            val sharedPref = getSharedPreferences("AppKasir", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("isLoggedIn", true)
                putString("currentUser", username)
                putString("userRole", userRole)
                apply()
            }

            Toast.makeText(this, "Login successful! Welcome $username", Toast.LENGTH_SHORT).show()
            navigateToPOS()
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            edtPassword.text.clear()
        }
    }

    private fun isValidCredentials(username: String, password: String): Boolean {
        // Demo credentials - replace with actual authentication
        val validUsers = mapOf(
            "admin" to "admin123",
            "operator" to "operator123",
            "demo" to "demo123"
        )
        
        return validUsers[username] == password
    }

    private fun getUserRole(username: String): String {
        return when (username) {
            "admin" -> "admin"
            "operator" -> "operator"
            "demo" -> "demo"
            else -> "operator"
        }
    }

    private fun navigateToPOS() {
        startActivity(Intent(this, POSActivity::class.java))
        finish()
    }
}
