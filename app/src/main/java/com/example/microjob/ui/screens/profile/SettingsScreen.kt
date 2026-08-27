package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.microjob.data.AppPreferences
import com.example.microjob.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: ProfileViewModel,
    onBack: () -> Unit,
    onNavigateToUserDetails: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val user = state.user ?: return
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { AppPreferences(context) }
    val theme by preferences.theme.collectAsStateWithLifecycle()
    val language by preferences.language.collectAsStateWithLifecycle()
    val copy = settingsCopy(language)
    val scope = rememberCoroutineScope()
    var showPasswordDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = { Text(copy.settings) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(copy.yourAccount, style = MaterialTheme.typography.labelLarge)
                        Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("@${user.username}", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f))
                    }
                    Icon(Icons.Filled.Settings, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            SettingsSection(copy.account, Icons.Filled.Person) {
                SettingsAction(copy.userDetails, copy.editProfile, Icons.Filled.Person, onNavigateToUserDetails)
                SettingsValue(copy.email, user.email.ifBlank { copy.notAdded }, Icons.Filled.Email)
            }

            SettingsSection(copy.privacy, Icons.Filled.PrivacyTip) {
                PrivacyRow(copy.showEmail, copy.allowEmail, user.showEmail) {
                    vm.updatePrivacy(it, user.showBirthdate, user.showPhoneNumber, user.showAvatar)
                }
                PrivacyRow(copy.showPhone, copy.allowPhone, user.showPhoneNumber) {
                    vm.updatePrivacy(user.showEmail, user.showBirthdate, it, user.showAvatar)
                }
                PrivacyRow(copy.showBirthdate, copy.allowBirthdate, user.showBirthdate) {
                    vm.updatePrivacy(user.showEmail, it, user.showPhoneNumber, user.showAvatar)
                }
                PrivacyRow(copy.showPhoto, copy.displayPhoto, user.showAvatar) {
                    vm.updatePrivacy(user.showEmail, user.showBirthdate, user.showPhoneNumber, it)
                }
            }

            SettingsSection(copy.appearance, Icons.Filled.DarkMode) {
                SettingsDropdown(copy.theme, theme, listOf(AppPreferences.THEME_SYSTEM, AppPreferences.THEME_LIGHT, AppPreferences.THEME_DARK)) {
                    preferences.setTheme(it)
                }
                SettingsDropdown(copy.language, language, listOf(AppPreferences.LANGUAGE_ENGLISH, AppPreferences.LANGUAGE_CHINESE, AppPreferences.LANGUAGE_MALAY)) {
                    preferences.setLanguage(it)
                }
                Text(copy.languageSaved, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SettingsSection(copy.security, Icons.Filled.Lock) {
                SettingsAction(copy.changePassword, copy.updatePassword, Icons.Filled.Lock) { showPasswordDialog = true }
            }

            SettingsSection(copy.about, Icons.Filled.Info) {
                SettingsValue(copy.version, "1.0", Icons.Filled.Info)
                SettingsValue("MicroJob", copy.tagline, Icons.Filled.Work)
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(vm, copy, onDismiss = { showPasswordDialog = false }, onSuccess = {
            showPasswordDialog = false
            scope.launch { /* reserved for a future snackbar host */ }
        })
    }
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun SettingsAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        TextButton(onClick = onClick) { Text("Open") }
    }
}

@Composable
private fun SettingsValue(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(12.dp))
        Column { Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun PrivacyRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
        OutlinedTextField(selected, {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true))
        ExposedDropdownMenu(expanded, { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) } }
    }
}

@Composable
private fun ChangePasswordDialog(vm: ProfileViewModel, copy: SettingsCopy, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy.changePassword) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(current, { current = it; error = null }, label = { Text(copy.currentPassword) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            OutlinedTextField(next, { next = it; error = null }, label = { Text(copy.newPassword) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            OutlinedTextField(confirm, { confirm = it; error = null }, label = { Text(copy.confirmPassword) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button(onClick = { error = when { next != confirm -> copy.passwordMismatch; else -> vm.changePassword(current, next) }; if (error == null) onSuccess() }) { Text(copy.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(copy.cancel) } }
    )
}

private data class SettingsCopy(
    val settings: String, val yourAccount: String, val account: String, val userDetails: String,
    val editProfile: String, val email: String, val notAdded: String, val privacy: String,
    val showEmail: String, val allowEmail: String, val showPhone: String, val allowPhone: String,
    val showBirthdate: String, val allowBirthdate: String, val showPhoto: String, val displayPhoto: String,
    val appearance: String, val theme: String, val language: String, val languageSaved: String,
    val security: String, val changePassword: String, val updatePassword: String, val about: String,
    val version: String, val tagline: String, val currentPassword: String, val newPassword: String,
    val confirmPassword: String, val passwordMismatch: String, val save: String, val cancel: String
)

private fun settingsCopy(language: String): SettingsCopy = when (language) {
    AppPreferences.LANGUAGE_CHINESE -> SettingsCopy(
        "设置", "您的账户", "账户", "用户详情", "编辑您的个人资料", "电子邮件", "未添加", "隐私",
        "显示电子邮件", "允许其他用户查看您的电子邮件", "显示电话号码", "允许其他用户查看您的电话",
        "显示出生日期", "允许其他用户查看您的出生日期", "显示个人照片", "在公开资料中显示您的照片",
        "外观", "主题", "语言", "语言设置已保存", "安全", "更改密码", "更新您的账户密码", "关于",
        "版本", "小任务，创造更多机会", "当前密码", "新密码", "确认密码", "密码不匹配", "保存", "取消"
    )
    AppPreferences.LANGUAGE_MALAY -> SettingsCopy(
        "Tetapan", "Akaun anda", "Akaun", "Butiran pengguna", "Edit maklumat profil", "Alamat e-mel", "Belum ditambah", "Privasi",
        "Tunjukkan e-mel", "Benarkan pengguna lain melihat e-mel", "Tunjukkan nombor telefon", "Benarkan pengguna lain melihat telefon",
        "Tunjukkan tarikh lahir", "Benarkan pengguna lain melihat tarikh lahir", "Tunjukkan foto profil", "Papar foto di profil awam",
        "Penampilan", "Tema", "Bahasa", "Pilihan bahasa telah disimpan", "Keselamatan", "Tukar kata laluan", "Kemas kini kata laluan akaun", "Tentang",
        "Versi", "Kerja kecil, peluang bermakna", "Kata laluan semasa", "Kata laluan baharu", "Sahkan kata laluan", "Kata laluan tidak sepadan", "Simpan", "Batal"
    )
    else -> SettingsCopy(
        "Settings", "Your account", "Account", "User Details", "Edit your profile information", "Email address", "Not added", "Privacy",
        "Show email address", "Allow other users to see your email", "Show phone number", "Allow other users to see your phone",
        "Show birthdate", "Allow other users to see your birthdate", "Show profile photo", "Display your photo on public profiles",
        "Appearance", "Theme", "Language", "Language changes are saved", "Security", "Change password", "Update your account password", "About",
        "Version", "Small jobs, meaningful opportunities", "Current password", "New password", "Confirm password", "Passwords do not match", "Save", "Cancel"
    )
}
