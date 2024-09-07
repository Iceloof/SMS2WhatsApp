package com.iceloof.sms2whatsapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var phoneNumberEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var checkPermissionsButton: Button
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()
        Log.d("SmsReceiver", "app start")

        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        saveButton = findViewById(R.id.saveButton)
        checkPermissionsButton = findViewById(R.id.checkPermissionsButton)

        saveButton.setOnClickListener {
            savePhoneNumber()
        }

        checkPermissionsButton.setOnClickListener {
            checkPermissions()
        }

        // Load saved phone number
        val sharedPref = getSharedPreferences("SMS2WhatsAppPreferences", Context.MODE_PRIVATE)
        val savedPhoneNumber = sharedPref.getString("phoneNumber", null)
        phoneNumberEditText.setText(savedPhoneNumber)

    }

    companion object {
        const val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 1234
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
    }

    private fun savePhoneNumber() {
        val phoneNumber = phoneNumberEditText.text.toString()
        val sharedPref = getSharedPreferences("SMS2WhatsAppPreferences", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("phoneNumber", phoneNumber)
            apply()
        }
        Toast.makeText(this, "Phone number saved", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions() {
        checkAndRequestPermissions()
        if (!isAccessibilityServiceEnabled(this, AccessibilityService::class.java)) {
            promptUserToEnableAccessibilityService()
        }

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (!devicePolicyManager.isAdminActive(compName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This app require device admin permission.")
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
        }

        // Request SYSTEM_ALERT_WINDOW permission if not granted
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_SMS,
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
            Manifest.permission.BIND_DEVICE_ADMIN
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        } else {
            // All permissions are granted
            Toast.makeText(this, "All permissions are granted", Toast.LENGTH_SHORT).show()
        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isEmpty()) {
                // All permissions are granted
                Toast.makeText(this, "All permissions are granted", Toast.LENGTH_SHORT).show()
            }
//            else {
//                // Some permissions are denied
//                Toast.makeText(this, "Permissions denied: $deniedPermissions", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun promptUserToEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Please enable the accessibility service for this app.", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabledServices.isNullOrEmpty()) {
            Log.d("Debug", "No enabled accessibility services found.")
            return false
        }

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(ComponentName(context, service).flattenToString(), ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}

