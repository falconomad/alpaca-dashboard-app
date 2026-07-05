package com.bloodbath.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppContainer(this)
            }
        }
    }
}

@Composable
fun MainAppContainer(activity: MainActivity) {
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var accountInfo by remember { mutableStateOf<Map<String, Any>?>(null) }
    var transactions by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    val hasCredentials = apiKey.isNotEmpty() && apiSecret.isNotEmpty()
    val isConnected = accountInfo != null && errorMsg.isEmpty()

    // Initial Credential Load
    LaunchedEffect(Unit) {
        val (k, s) = AlpacaService.getCredentials(activity)
        apiKey = k
        apiSecret = s
        if (apiKey.isNotEmpty() && apiSecret.isNotEmpty()) {
            fetchData(activity, { accountInfo = it }, { transactions = it }, { loading = it }, { errorMsg = it })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080A)) // Deep Premium iOS Black
    ) {
        if (!hasCredentials || (!isConnected && !loading && errorMsg.isNotEmpty())) {
            PreLoginScreen(
                apiKey = apiKey,
                apiSecret = apiSecret,
                errorMsg = errorMsg,
                loading = loading,
                onApiKeyChange = { apiKey = it },
                onApiSecretChange = { apiSecret = it },
                onConnect = {
                    AlpacaService.saveCredentials(activity, apiKey, apiSecret)
                    fetchData(activity, { accountInfo = it }, { transactions = it }, { loading = it }, { errorMsg = it })
                }
            )
        } else {
            PostLoginDashboard(
                accountInfo = accountInfo,
                transactions = transactions,
                loading = loading,
                onOpenSettings = { showSettings = true },
                onRefresh = {
                    fetchData(activity, { accountInfo = it }, { transactions = it }, { loading = it }, { errorMsg = it })
                }
            )
        }

        if (showSettings) {
            SettingsModalDialog(
                apiKey = apiKey,
                apiSecret = apiSecret,
                onApiKeyChange = { apiKey = it },
                onApiSecretChange = { apiSecret = it },
                onClose = { showSettings = false },
                onDisconnect = {
                    apiKey = ""
                    apiSecret = ""
                    accountInfo = null
                    transactions = null
                    AlpacaService.saveCredentials(activity, "", "")
                    showSettings = false
                },
                onSave = {
                    AlpacaService.saveCredentials(activity, apiKey, apiSecret)
                    showSettings = false
                    fetchData(activity, { accountInfo = it }, { transactions = it }, { loading = it }, { errorMsg = it })
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreLoginScreen(
    apiKey: String,
    apiSecret: String,
    errorMsg: String,
    loading: Boolean,
    onApiKeyChange: (String) -> Unit,
    onApiSecretChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF30D158), Color(0xFF1C7D33))
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🦙", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Connect your Alpaca Paper Trading account",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("Alpaca API Key ID", color = Color(0xFF8E8E93)) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF30D158),
                        unfocusedBorderColor = Color(0xFF2C2C2E)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiSecret,
                    onValueChange = onApiSecretChange,
                    label = { Text("Secret Key", color = Color(0xFF8E8E93)) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF30D158),
                        unfocusedBorderColor = Color(0xFF2C2C2E)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorMsg, color = Color(0xFFFF453A), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF30D158))
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Connect Account",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostLoginDashboard(
    accountInfo: Map<String, Any>?,
    transactions: List<Map<String, Any>>?,
    loading: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val account = accountInfo ?: emptyMap()
    val equity = account["equity"]?.toString()?.toDoubleOrNull() ?: 0.0
    val buyingPower = account["buying_power"]?.toString()?.toDoubleOrNull() ?: 0.0
    val cash = account["cash"]?.toString()?.toDoubleOrNull() ?: 0.0

    val formattedEquity = String.format(Locale.US, "$%,.2f", equity)
    val formattedBuyingPower = String.format(Locale.US, "$%,.2f", buyingPower)
    val formattedCash = String.format(Locale.US, "$%,.2f", cash)

    // Date filtering state
    var selectedDate by remember { mutableStateOf(Calendar.getInstance(TimeZone.getTimeZone("UTC"))) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedDateString = remember(selectedDate) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(selectedDate.time)
    }

    val displayDateText = remember(selectedDate) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).time)

        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateStr = formatter.format(selectedDate.time)
        if (selectedDateString == todayStr) "Today ($dateStr)" else dateStr
    }

    val filteredTransactions = remember(transactions, selectedDateString) {
        transactions?.filter { tx ->
            val rawTime = tx["transaction_time"]?.toString() ?: ""
            rawTime.startsWith(selectedDateString)
        } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Toolbar / Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Equity Balance",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedEquity,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .background(Color(0xFF1C1C1E), shape = CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.LightGray
                )
            }
        }

        // Sub Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Buying Power", color = Color(0xFF8E8E93), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = formattedBuyingPower, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Cash Balance", color = Color(0xFF8E8E93), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = formattedCash, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Date Filter Card Bar
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
            border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Filter By Date", color = Color(0xFF8E8E93), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = displayDateText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                
                Button(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                    border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Select Date", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions (${filteredTransactions.size})",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).time)
                
                if (selectedDateString != todayStr) {
                    Text(
                        text = "Show Today",
                        color = Color(0xFF8E8E93),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                selectedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            }
                            .padding(end = 16.dp)
                    )
                }
                
                if (loading) {
                    CircularProgressIndicator(color = Color(0xFF30D158), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Refresh",
                        color = Color(0xFF30D158),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onRefresh() }
                    )
                }
            }
        }

        // List content
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedDateString == SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).time))
                        "No transactions today." 
                    else 
                        "No transactions on this date.", 
                    color = Color(0xFF8E8E93), 
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTransactions) { tx ->
                    TransactionItem(tx)
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            cal.timeInMillis = millis
                            selectedDate = cal
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = Color(0xFF30D158), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color(0xFF8E8E93))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TransactionItem(tx: Map<String, Any>) {
    val symbol = tx["symbol"]?.toString() ?: "Unknown"
    val side = tx["side"]?.toString()?.lowercase() ?: "buy"
    val qty = tx["qty"]?.toString()?.toDoubleOrNull() ?: 0.0
    val price = tx["price"]?.toString()?.toDoubleOrNull() ?: 0.0
    val rawDate = tx["transaction_time"]?.toString() ?: ""
    val cleanDate = if (rawDate.length > 16) rawDate.substring(11, 16) else rawDate // Show HH:mm for selected day
    val status = tx["status"]?.toString()?.uppercase() ?: "FILLED"

    val isBuy = side == "buy"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121215)),
        border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isBuy) Color(0x1F30D158) else Color(0x1FFF453A),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBuy) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                        contentDescription = side,
                        tint = if (isBuy) Color(0xFF30D158) else Color(0xFFFF453A),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = symbol, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.1f shares @ $%,.2f", qty, price),
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = cleanDate, color = Color(0xFF8E8E93), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(
                    modifier = Modifier
                        .background(
                            color = if (status == "FILLED") Color(0xFF30D158) else Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status,
                        color = if (status == "FILLED") Color.White else Color.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModalDialog(
    apiKey: String,
    apiSecret: String,
    onApiKeyChange: (String) -> Unit,
    onApiSecretChange: (String) -> Unit,
    onClose: () -> Unit,
    onDisconnect: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        content = {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Alpaca API Keys",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text("API Key ID", color = Color(0xFF8E8E93)) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF30D158),
                            unfocusedBorderColor = Color(0xFF2C2C2E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiSecret,
                        onValueChange = onApiSecretChange,
                        label = { Text("API Secret Key", color = Color(0xFF8E8E93)) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF30D158),
                            unfocusedBorderColor = Color(0xFF2C2C2E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Disconnect", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onSave,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Cancel", color = Color(0xFF8E8E93))
                    }
                }
            }
        }
    )
}

fun fetchData(
    activity: MainActivity,
    onAccountLoaded: (Map<String, Any>?) -> Unit,
    onTransactionsLoaded: (List<Map<String, Any>>?) -> Unit,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    onLoading(true)
    onError("")
    thread {
        try {
            val account = AlpacaService.fetchAccount(activity)
            val transactions = AlpacaService.fetchTransactions(activity)

            activity.runOnUiThread {
                if (account != null) {
                    onAccountLoaded(account)
                    onTransactionsLoaded(transactions ?: emptyList())
                } else {
                    onError("Failed to fetch account info. Check your API keys.")
                }
                onLoading(false)
            }
        } catch (e: Exception) {
            activity.runOnUiThread {
                onError("Network error: ${e.message}")
                onLoading(false)
            }
        }
    }
}
