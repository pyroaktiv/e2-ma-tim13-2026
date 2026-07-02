package rs.tim13.slagalica.core.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import rs.tim13.slagalica.core.NotificationHelper
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.notifications.data.FcmTokenRegistrar
import rs.tim13.slagalica.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannels(this)
        requestNotificationPermissionIfNeeded()
        // Poveži FCM token uređaja sa nalogom (spec 11) — i za sveži login i za trajni login.
        FcmTokenRegistrar.syncIfLoggedIn(this)
        // Drži ulogovanog korisnika „online" od otvaranja app-a: backend tada zna da je u aplikaciji
        // (WS live notifikacije/pozivi), a FCM push ide samo kad zaista nije povezan. Idempotentno.
        if (TokenManager(this).isLoggedIn()) SocketManager.connect(this)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}