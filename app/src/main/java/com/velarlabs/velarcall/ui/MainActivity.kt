package com.velarlabs.velarcall.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {

    /** Muda sempre que a tela volta ao primeiro plano, para recarregar estado. */
    private val refreshTick = mutableIntStateOf(0)
    private val startTab = mutableIntStateOf(TAB_HOME)

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshTick.intValue++
    }
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshTick.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startTab.intValue = intent?.getIntExtra(EXTRA_TAB, TAB_HOME) ?: TAB_HOME
        setContent {
            VelarTheme {
                VelarApp(
                    refreshTick = refreshTick.intValue,
                    initialTab = startTab.intValue,
                    isRoleHeld = ::isRoleHeld,
                    requestRole = ::requestRole,
                    hasPermission = ::hasPermission,
                    requestPermissions = ::askPermissions,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick.intValue++
    }

    private fun isRoleHeld(): Boolean =
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

    private fun requestRole() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        }
    }

    private fun hasPermission(p: String): Boolean =
        checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    private fun askPermissions() {
        val wanted = buildList {
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filterNot(::hasPermission)
        if (wanted.isNotEmpty()) permLauncher.launch(wanted.toTypedArray())
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_HOME = 0
        const val TAB_HISTORY = 1
    }
}
