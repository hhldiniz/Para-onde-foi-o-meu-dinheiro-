package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hhldiniz.praondefoiomeudinheiro.platform.rememberAppInstaller
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoButton
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.DataClearedHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_back_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_currency_label
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_danger_cancel_button
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_danger_confirm_button
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_danger_confirm_text
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_danger_confirm_title
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_danger_delete_button
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_danger_description
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_danger_title
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_install_button
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_install_description
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_install_title
import com.hhldiniz.praondefoiomeudinheiro.resources.settings_title

/** Settings screen allowing the user to change the preferred currency. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val selectedCurrency by CurrencyHolder.selectedCurrency.collectAsState()
    val importRepository = koinInject<ImportRepository>()
    val categoryDao = koinInject<CategoryDao>()
    val coroutineScope = rememberCoroutineScope()
    val appInstaller = rememberAppInstaller()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.settings_back_content_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrutalYellow,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CurrencySelector(
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { currency ->
                    CurrencyHolder.setCurrency(currency)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Only the web build ever offers this, and only until the app is
            // installed — see AppInstaller.canInstall.
            if (appInstaller.canInstall) {
                Spacer(modifier = Modifier.height(32.dp))

                InstallAppSection(
                    onInstallRequested = { appInstaller.install() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            DangerZone(
                onDeleteRequested = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = stringResource(Res.string.settings_danger_confirm_title),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.settings_danger_confirm_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        coroutineScope.launch {
                            importRepository.clearAllData(categoryDao)
                            DataClearedHolder.markCleared()
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.settings_danger_confirm_button),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        text = stringResource(Res.string.settings_danger_cancel_button),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

/**
 * Offer to install the app to the device, shown only where the platform can
 * actually do it — in practice the web build, in a browser that has an install
 * prompt ready (see [rememberAppInstaller]).
 */
@Composable
private fun InstallAppSection(
    onInstallRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.settings_install_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.settings_install_description),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        NeoButton(
            onClick = onInstallRequested,
            text = stringResource(Res.string.settings_install_button),
            backgroundColor = BrutalCyan,
            textColor = MaterialTheme.colorScheme.onSurface,
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Danger section with a destructive action to wipe all internal data. */
@Composable
private fun DangerZone(
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.settings_danger_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.settings_danger_description),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        HardShadowBox(
            offsetX = 3.dp,
            offsetY = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.error, RectangleShape)
                    .background(MaterialTheme.colorScheme.errorContainer, RectangleShape)
                    .clickable { onDeleteRequested() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.settings_danger_delete_button),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/** Dropdown selector for picking a [CurrencyOption] from the available list. */
@Composable
private fun CurrencySelector(
    selectedCurrency: CurrencyOption,
    onCurrencySelected: (CurrencyOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        HardShadowBox(
            offsetX = 3.dp,
            offsetY = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .background(MaterialTheme.colorScheme.surface, RectangleShape)
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        Res.string.settings_currency_label,
                        selectedCurrency.code,
                        selectedCurrency.symbol,
                    ),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CurrencyOption.entries.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${currency.code} (${currency.symbol})",
                            fontWeight = if (currency == selectedCurrency) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}