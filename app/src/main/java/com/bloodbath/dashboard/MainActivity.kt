package com.bloodbath.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DashboardScreen(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(activity: MainActivity) {
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var accountInfo by remember { mutableStateOf<Map<String, Any>?>(null) }
    var transactions by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val (k, s) = AlpacaService.getCredentials(activity)
        apiKey = k
        apiSecret = s
        if (apiKey.isNotEmpty() && apiSecret.isNotEmpty()) {
            fetchData(activity, { accountInfo = it }, { transactions = it }, { loading = it }, { errorMsg = it })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Text(
            text = "Alpaca Dashboard",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // API Credentials Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "API Credentials", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key ID", color = Color.Gray) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00C805),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = apiSecret,
                    onValueChange = { apiSecret = it },
                    label = { Text("API Secret Key", color = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00C805),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        AlpacaService.saveCredentials(activity, apiKey, apiSecret)
                        fetchData(activity, { accountInfo = it }, { transactions = it }, { loading = it }, { errorMsg = it })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C805)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Save & Refresh", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00C805))
            }
        } else if (errorMsg.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(text = errorMsg, color = Color.Red, fontSize = 14.sp)
            }
        } else {
            // Dashboard Balance info Card
            accountInfo?.let { account ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Account Overview", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Total Equity:", color = Color.Gray, fontSize = 14.sp)
                            Text(text = "$${account["equity"] ?: "0.00"}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Buying Power:", color = Color.Gray, fontSize = 14.sp)
                            Text(text = "$${account["buying_power"] ?: "0.00"}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Cash Balance:", color = Color.Gray, fontSize = 14.sp)
                            Text(text = "$${account["cash"] ?: "0.00"}", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Transactions list
            Text(
                text = "Recent Transactions",
                color = Color.LightGray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val txs = transactions ?: emptyList()
            if (txs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = "No recent transactions found.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(txs) { tx ->
                        TransactionRow(tx)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: Map<String, Any>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val symbol = tx["symbol"]?.toString() ?: "Unknown"
                val side = tx["side"]?.toString()?.uppercase() ?: "BUY"
                val qty = tx["qty"]?.toString() ?: "0"
                val price = tx["price"]?.toString() ?: "0.00"
                
                Text(text = "$symbol ($side)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "Qty: $qty @ $$price", color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                val rawDate = tx["transaction_time"]?.toString() ?: ""
                val cleanDate = if (rawDate.length > 19) rawDate.substring(0, 19).replace("T", " ") else rawDate
                val status = tx["status"]?.toString()?.uppercase() ?: "FILLED"
                
                Text(text = cleanDate, color = Color.Gray, fontSize = 10.sp)
                Text(
                    text = status, 
                    color = if (status == "FILLED") Color(0xFF00C805) else Color.Yellow, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun fetchData(
    context: android.content.Context,
    onAccountFetch: (Map<String, Any>?) -> Unit,
    onTransactionsFetch: (List<Map<String, Any>>?) -> Unit,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    onLoading(true)
    onError("")
    thread {
        val account = AlpacaService.fetchAccount(context)
        val txs = AlpacaService.fetchTransactions(context)
        
        onLoading(false)
        if (account == null) {
            onError("Connection failed. Check API credentials or internet connectivity.")
        } else {
            onAccountFetch(account)
            onTransactionsFetch(txs)
        }
    }
}
