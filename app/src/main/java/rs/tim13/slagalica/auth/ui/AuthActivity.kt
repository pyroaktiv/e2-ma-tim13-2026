package rs.tim13.slagalica.auth.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import rs.tim13.slagalica.core.ui.GameActivity
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Trajni login (spec 11): ako postoji važeći token, preskoči login i idi pravo na Home
        // kako bi uređaj „znao" nalog i mogao da prima notifikacije. Istekao token -> ostani na loginu.
        val tokenManager = TokenManager(this)
        if (tokenManager.isLoggedIn()) {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
            return
        } else {
            tokenManager.clearToken()
        }

        enableEdgeToEdge()
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}