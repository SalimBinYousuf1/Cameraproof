package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppTheme
import com.example.data.model.ReportSettings
import com.example.data.model.StampPosition
import com.example.data.model.StampSettings
import com.example.data.model.StampTextSize
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val stampSettings by viewModel.stampSettings.collectAsState()
    val reportSettings by viewModel.reportSettings.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Live Sample Preview Card
            LiveStampPreviewCard(stampSettings)

            // Pro Membership Card
            ProStatusCard(
                isPro = isPro,
                onUpgradeClicked = { viewModel.openPaywall() }
            )

            // Stamp Fields Section
            SectionHeader(title = stringResource(R.string.settings_stamp_section))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggleRow(
                        title = stringResource(R.string.settings_show_datetime),
                        checked = stampSettings.showDateTime,
                        onCheckedChange = { viewModel.updateStampSettings(stampSettings.copy(showDateTime = it)) }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_time_format),
                        checked = stampSettings.use24Hour,
                        onCheckedChange = { viewModel.updateStampSettings(stampSettings.copy(use24Hour = it)) }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_show_coords),
                        checked = stampSettings.showCoordinates,
                        onCheckedChange = { viewModel.updateStampSettings(stampSettings.copy(showCoordinates = it)) }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_show_address),
                        checked = stampSettings.showAddress,
                        onCheckedChange = { viewModel.updateStampSettings(stampSettings.copy(showAddress = it)) }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_show_project),
                        checked = stampSettings.showProjectName,
                        onCheckedChange = { viewModel.updateStampSettings(stampSettings.copy(showProjectName = it)) }
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_show_note),
                        checked = stampSettings.showNote,
                        onCheckedChange = { viewModel.updateStampSettings(stampSettings.copy(showNote = it)) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.settings_stamp_position),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionChip(
                            label = "Bottom L",
                            selected = stampSettings.position == StampPosition.BOTTOM_LEFT,
                            onClick = { viewModel.updateStampSettings(stampSettings.copy(position = StampPosition.BOTTOM_LEFT)) }
                        )
                        PositionChip(
                            label = "Bottom R",
                            selected = stampSettings.position == StampPosition.BOTTOM_RIGHT,
                            onClick = { viewModel.updateStampSettings(stampSettings.copy(position = StampPosition.BOTTOM_RIGHT)) }
                        )
                        PositionChip(
                            label = "Top L",
                            selected = stampSettings.position == StampPosition.TOP_LEFT,
                            onClick = { viewModel.updateStampSettings(stampSettings.copy(position = StampPosition.TOP_LEFT)) }
                        )
                        PositionChip(
                            label = "Top R",
                            selected = stampSettings.position == StampPosition.TOP_RIGHT,
                            onClick = { viewModel.updateStampSettings(stampSettings.copy(position = StampPosition.TOP_RIGHT)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.settings_text_size),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionChip(
                            label = stringResource(R.string.settings_size_small),
                            selected = stampSettings.textSize == StampTextSize.SMALL,
                            onClick = { viewModel.updateStampSettings(stampSettings.copy(textSize = StampTextSize.SMALL)) }
                        )
                        PositionChip(
                            label = stringResource(R.string.settings_size_medium),
                            selected = stampSettings.textSize == StampTextSize.MEDIUM,
                            onClick = { viewModel.updateStampSettings(stampSettings.copy(textSize = StampTextSize.MEDIUM)) }
                        )
                        PositionChip(
                            label = stringResource(R.string.settings_size_large),
                            selected = stampSettings.textSize == StampTextSize.LARGE,
                            onClick = { viewModel.updateStampSettings(stampSettings.copy(textSize = StampTextSize.LARGE)) }
                        )
                    }
                }
            }

            // PDF Report Defaults Section
            SectionHeader(title = stringResource(R.string.settings_report_section))
            ReportSettingsSection(
                reportSettings = reportSettings,
                isPro = isPro,
                onSave = { updated -> viewModel.updateReportSettings(updated) },
                onUnlockPro = { viewModel.openPaywall() }
            )

            // Appearance Section
            SectionHeader(title = stringResource(R.string.settings_appearance_section))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PositionChip(
                        label = stringResource(R.string.settings_theme_system),
                        selected = appTheme == AppTheme.SYSTEM,
                        onClick = { viewModel.setAppTheme(AppTheme.SYSTEM) }
                    )
                    PositionChip(
                        label = stringResource(R.string.settings_theme_light),
                        selected = appTheme == AppTheme.LIGHT,
                        onClick = { viewModel.setAppTheme(AppTheme.LIGHT) }
                    )
                    PositionChip(
                        label = stringResource(R.string.settings_theme_dark),
                        selected = appTheme == AppTheme.DARK,
                        onClick = { viewModel.setAppTheme(AppTheme.DARK) }
                    )
                }
            }

            // Privacy & About Section
            SectionHeader(title = stringResource(R.string.settings_about_section))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_privacy_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_privacy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.settings_version_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.settings_version_val),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPaywall) {
        ProPaywallSheet(
            billingManager = viewModel.billingManager,
            onDismiss = { viewModel.dismissPaywall() }
        )
    }
}

@Composable
private fun LiveStampPreviewCard(stampSettings: StampSettings) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Stamp Preview",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xD01E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (stampSettings.showDateTime) {
                        Text(
                            text = DateFormatter.formatDateTime(System.currentTimeMillis(), stampSettings.use24Hour),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    if (stampSettings.showProjectName) {
                        Text(
                            text = "PROJECT: Sample Site Inspection",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = Color(0xFF38BDF8)
                        )
                    }
                    if (stampSettings.showCoordinates) {
                        Text(
                            text = "GPS: 37.77490° N, 122.41940° W  ±5m",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }
                    if (stampSettings.showAddress) {
                        Text(
                            text = "LOC: 100 Market St, San Francisco, CA",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    if (stampSettings.showNote) {
                        Text(
                            text = "NOTE: Foundation integrity verified",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProStatusCard(
    isPro: Boolean,
    onUpgradeClicked: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isPro) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isPro) stringResource(R.string.pro_title) else "Free Version",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPro) "All features unlocked" else "3 projects limit • Watermark on PDF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isPro) {
                Button(
                    onClick = onUpgradeClicked,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("settings_upgrade_pro_btn")
                ) {
                    Text(stringResource(R.string.pro_buy_button))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun PositionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun ReportSettingsSection(
    reportSettings: ReportSettings,
    isPro: Boolean,
    onSave: (ReportSettings) -> Unit,
    onUnlockPro: () -> Unit
) {
    var company by remember(reportSettings) { mutableStateOf(reportSettings.companyName) }
    var inspector by remember(reportSettings) { mutableStateOf(reportSettings.inspectorName) }
    var license by remember(reportSettings) { mutableStateOf(reportSettings.inspectorLicense) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!isPro) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onUnlockPro)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Custom report headers require Salim Pro. Tap to upgrade.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = company,
                onValueChange = {
                    company = it
                    onSave(reportSettings.copy(companyName = it))
                },
                label = { Text(stringResource(R.string.settings_company_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = inspector,
                onValueChange = {
                    inspector = it
                    onSave(reportSettings.copy(inspectorName = it))
                },
                label = { Text(stringResource(R.string.settings_inspector_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = license,
                onValueChange = {
                    license = it
                    onSave(reportSettings.copy(inspectorLicense = it))
                },
                label = { Text(stringResource(R.string.settings_inspector_license)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
