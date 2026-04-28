package com.smartcardscanner.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

object Routes {
    const val HOME = "home"
    const val SCAN_BACK = "scan_back"
    const val SCAN_NFC = "scan_nfc"
    const val RESULTS = "results"
    const val IMPORT_DB = "import_db"
    const val RECORDS = "records"
    const val RECORD_DETAIL = "record_detail/{recordId}"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    onNavigate: (String) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            com.smartcardscanner.presentation.home.HomeScreen(
                onStartScan = { navController.navigate(Routes.SCAN_BACK) },
                onImportDatabase = { navController.navigate(Routes.IMPORT_DB) },
                onViewRecords = { navController.navigate(Routes.RECORDS) }
            )
        }

        composable(Routes.SCAN_BACK) {
            com.smartcardscanner.presentation.scan.BackScanScreen(
                onScanComplete = { navController.navigate(Routes.SCAN_NFC) },
                onSkip = { navController.navigate(Routes.SCAN_NFC) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SCAN_NFC) {
            com.smartcardscanner.presentation.scan.NfcScanScreen(
                onScanComplete = { navController.navigate(Routes.RESULTS) },
                onSkip = { navController.navigate(Routes.RESULTS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RESULTS) {
            com.smartcardscanner.presentation.result.ResultScreen(
                onSave = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onNewScan = {
                    navController.navigate(Routes.SCAN_BACK) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.IMPORT_DB) {
            com.smartcardscanner.presentation.database.ImportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RECORDS) {
            com.smartcardscanner.presentation.records.RecordsScreen(
                onBack = { navController.popBackStack() },
                onRecordClick = { /* detail view */ }
            )
        }
    }
}
