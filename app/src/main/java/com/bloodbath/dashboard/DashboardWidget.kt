package com.bloodbath.dashboard

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class DashboardWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        thread {
            val account = AlpacaService.fetchAccount(context)
            val equity = account?.get("equity")?.toString()?.toDoubleOrNull() ?: 0.0
            val buyingPower = account?.get("buying_power")?.toString()?.toDoubleOrNull() ?: 0.0

            val formattedEquity = String.format(Locale.US, "$%,.2f", equity)
            val formattedBuyingPower = String.format(Locale.US, "Buying Power: $%,.2f", buyingPower)
            
            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            val updateTime = "Updated: " + sdf.format(Date())

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_equity, formattedEquity)
                views.setTextViewText(R.id.widget_buying_power, formattedBuyingPower)
                views.setTextViewText(R.id.widget_update_time, updateTime)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
