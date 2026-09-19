package com.carmangment.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Main destinations deliberately mirror the user's daily flow, not the database tables.
 *
 * The old "بیشتر" tab was a launcher for unrelated tools; it is now "تنظیمات" and holds
 * settings only. The tools it used to hide moved to the right-edge side drawer
 * ([ToolDestination]).
 */
enum class AppDestination(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("خانه", Icons.Outlined.Home, Icons.Filled.Home),
    SERVICES("ثبت سرویس", Icons.Outlined.AddCircle, Icons.Filled.AddCircle),
    CALENDAR("تقویم", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    REPORTS("گزارش", Icons.Outlined.Assessment, Icons.Filled.Assessment),
    SETTINGS("تنظیمات", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/** Secondary tools. Reached from the right-edge slide-out panel, never from a tab. */
enum class ToolDestination(val title: String, val subtitle: String, val icon: ImageVector) {
    FUEL("سوخت", "ثبت سوخت‌گیری و هزینه", Icons.Outlined.LocalGasStation),
    FINANCE("مالی، وام و اقساط", "نمای کلی، وام‌ها، خرید و درآمد شخصی", Icons.Outlined.AccountBalanceWallet),
    MAINTENANCE("تعمیرات و نگهداری", "تعویض روغن، لاستیک، لنت و…", Icons.Outlined.Build),
    RATES("نرخ‌ها", "نرخ کیلومتر، ساعت، سوخت و عوارضی", Icons.Outlined.Percent),
}
