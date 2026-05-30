package com.akprojects.copyclipper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akprojects.copyclipper.data.ContactDetails
import com.akprojects.copyclipper.data.FormPreset
import com.akprojects.copyclipper.data.PersonalDetails
import com.akprojects.copyclipper.data.PresetDatabase
import com.akprojects.copyclipper.service.OverlayService
import com.akprojects.copyclipper.service.AutofillAccessibilityService
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6366F1),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClipperDashboardScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipperDashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { PresetDatabase.getDatabase(context) }
    
    var presets by remember { mutableStateOf<List<FormPreset>>(emptyList()) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var accessibilityActive by remember { mutableStateOf(false) }
    
    var showEditor by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<FormPreset?>(null) }

    val reloadPresets = {
        scope.launch {
            presets = db.presetDao().getAllPresets()
        }
    }

    LaunchedEffect(Unit) {
        reloadPresets()
    }

    // Polling / updating check status for accessibility and overlays
    LaunchedEffect(key1 = hasOverlayPermission) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        accessibilityActive = AutofillAccessibilityService.sharedService != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FlashOn, "Logo", tint = Color(0xFF818CF8), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ak Copy Clipper ✦", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPreset = null
                    showEditor = true
                },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, "New Preset")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Permissions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Service Status Control Center", fontWeight = FontWeight.Black, color = Color(0xFF818CF8))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Draw Over Other Apps", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Overlay permission for clipboard bubble", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = hasOverlayPermission,
                            onCheckedChange = {
                                if (!hasOverlayPermission) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                        context.startActivity(intent)
                                    }
                                } else {
                                    Toast.makeText(context, "Bypass: Permission can only be toggled inside system settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Keystroke Injection Service", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Bypass onPaste block secure filters", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = accessibilityActive,
                            onCheckedChange = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                                Toast.makeText(context, "Enable Ak Copy Clipper under accessibility settings", Toast.LENGTH_LONG).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (!Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, "Please configure Overlay permission first!", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(context, OverlayService::class.java)
                                context.startService(intent)
                                Toast.makeText(context, "Floating overlay launched successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Active Service")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Launch Floating Bubble View", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Presets Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Form Presets DB List (${presets.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Presets list
            if (presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Empty DB", tint = Color(0xFF475569), modifier = Modifier.size(48.dp))
                        Text("No active profiles loaded inside SQLite Room.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { preset ->
                        PresetCardRow(
                            preset = preset,
                            onEdit = {
                                editingPreset = preset
                                showEditor = true
                            },
                            onDelete = {
                                scope.launch {
                                    db.presetDao().deletePreset(preset)
                                    reloadPresets()
                                    Toast.makeText(context, "Preset deleted from database", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Preset Editor Modal Screen Dialog
    if (showEditor) {
        PresetEditorDialog(
            preset = editingPreset,
            onDismiss = { showEditor = false },
            onSave = { savedPreset ->
                scope.launch {
                    db.presetDao().insertPreset(savedPreset)
                    reloadPresets()
                    showEditor = false
                    Toast.makeText(context, "Preset successfully saved inside SQLite Room", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun PresetCardRow(
    preset: FormPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = when (preset.colorTheme) {
        "indigo" -> Color(0xFF6366F1)
        "emerald" -> Color(0xFF10B981)
        "amber" -> Color(0xFFF59E0B)
        "rose" -> Color(0xFFF43F5E)
        "sky" -> Color(0xFF0EA5E9)
        else -> Color(0xFF64748B)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(themeColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Theme Indicator", tint = themeColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(preset.presetName, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Legal Name: ${preset.personal.fullName}", fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Profile", tint = Color(0xFFF43F5E), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun PresetEditorDialog(
    preset: FormPreset?,
    onDismiss: () -> Unit,
    onSave: (FormPreset) -> Unit
) {
    var presetName by remember { mutableStateOf(preset?.presetName ?: "") }
    var colorTheme by remember { mutableStateOf(preset?.colorTheme ?: "indigo") }
    
    // Personal state variables
    var fullName by remember { mutableStateOf(preset?.personal?.fullName ?: "") }
    var idNumber by remember { mutableStateOf(preset?.personal?.idNumber ?: "") }
    var dob by remember { mutableStateOf(preset?.personal?.dob ?: "") }
    
    // Contact state variables
    var email by remember { mutableStateOf(preset?.contact?.email ?: "") }
    var phoneNumber by remember { mutableStateOf(preset?.contact?.phoneNumber ?: "") }
    var address by remember { mutableStateOf(preset?.contact?.address ?: "") }
    var city by remember { mutableStateOf(preset?.contact?.city ?: "") }
    var zipCode by remember { mutableStateOf(preset?.contact?.zipCode ?: "") }
    
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (preset == null) "Create Profile Preset" else "Edit Profile Preset",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Profile Nickname") },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Color Theme Presets", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colors = listOf("indigo", "emerald", "amber", "rose", "sky")
                    colors.forEach { theme ->
                        val themeColor = when(theme) {
                            "indigo" -> Color(0xFF6366F1)
                            "emerald" -> Color(0xFF10B981)
                            "amber" -> Color(0xFFF59E0B)
                            "rose" -> Color(0xFFF43F5E)
                            "sky" -> Color(0xFF0EA5E9)
                            else -> Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(themeColor, CircleShape)
                                .clickable { colorTheme = theme }
                                .padding(2.dp)
                        ) {
                            if (colorTheme == theme) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF334155)))
                Text("Personal Attributes Details", color = Color(0xFF818CF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Legal Name") },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = idNumber,
                    onValueChange = { idNumber = it },
                    label = { Text("ID Number / SSN") },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Birth Date (YYYY-MM-DD)") },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF334155)))
                Text("Contact Details", color = Color(0xFF818CF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address") },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = zipCode,
                        onValueChange = { zipCode = it },
                        label = { Text("Zip Code") },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (presetName.trim().isEmpty() || fullName.trim().isEmpty()) {
                        return@Button
                    }
                    val newPreset = FormPreset(
                        id = preset?.id ?: UUID.randomUUID().toString(),
                        presetName = presetName,
                        iconName = "user",
                        colorTheme = colorTheme,
                        personal = PersonalDetails(
                            fullName = fullName,
                            idNumber = idNumber,
                            dob = dob,
                            gender = preset?.personal?.gender ?: ""
                        ),
                        contact = ContactDetails(
                            email = email,
                            phoneNumber = phoneNumber,
                            address = address,
                            city = city,
                            zipCode = zipCode,
                            country = preset?.contact?.country ?: "United States"
                        ),
                        customFieldsJson = "[]",
                        createdAt = preset?.createdAt ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
                    )
                    onSave(newPreset)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Save Profile", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}