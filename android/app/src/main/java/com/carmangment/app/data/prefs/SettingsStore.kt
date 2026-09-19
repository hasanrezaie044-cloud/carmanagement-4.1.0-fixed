package com.carmangment.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Non-record app settings: appearance, privacy mode, monthly income goal, reminder
 * switches and the expiry dates the reminders are derived from.
 *
 * These lived in AsyncStorage keys in the legacy app (appearance / goals /
 * reminderPrefs / settings). They are intentionally kept OUT of Room: a handful of
 * scalars, read synchronously at first composition, and no schema migration needed.
 *
 * The PIN is NOT stored here — it stays in EncryptedSharedPreferences
 * (security/PinStore) and is never exported, matching legacy behaviour.
 */
data class AppSettings(
    val themeMode: String = THEME_SYSTEM,
    val privacyMode: Boolean = false,
    val monthlyIncomeGoal: Double = 0.0,
    // reminder switches — each independently switchable
    val remindDailySummary: Boolean = false,
    val remindWeeklySummary: Boolean = false,
    val remindMonthlySummary: Boolean = false,
    val remindInstallments: Boolean = true,
    val remindOilChange: Boolean = true,
    val remindBodyInsurance: Boolean = true,
    val remindVehicleInsurance: Boolean = true,
    val remindInspection: Boolean = true,
    val remindBackup: Boolean = true,
    // expiry dates driving the 14-day / 3-day reminders, Jalali "YYYY/MM/DD"
    val bodyInsuranceDate: String = "",
    val vehicleInsuranceDate: String = "",
    val inspectionDate: String = "",
    val backupIntervalDays: Int = 14,
    val lastBackupAt: Long = 0L,
    // نمایش‌داده‌شده فقط یک‌بار، اولین باری که کاربر وارد تب «ثبت سرویس» می‌شود.
    val hasSeenServiceTutorial: Boolean = false,
    // تعویض روغن: کاربر مستقیماً کیلومتر فعلی (در زمان آخرین تعویض) و کیلومتر
    // هدف برای تعویض بعدی را وارد می‌کند؛ کیلومتر طی‌شده از سرویس‌های ثبت‌شده
    // از تاریخ oilChangeSetAt به بعد به کیلومتر فعلی اضافه می‌شود تا برنامه
    // خودش بفهمد کی به کیلومتر بعدی رسیده‌ایم.
    val oilChangeCurrentKm: Double = 0.0,
    val oilChangeNextKm: Double = 0.0,
    val oilChangeSetAt: String = "",
    // فلگ‌های «آموزش اولین‌بار» برای هر تب — هرکدام فقط یک‌بار نمایش داده می‌شود.
    val hasSeenHomeTutorial: Boolean = false,
    val hasSeenCalendarTutorial: Boolean = false,
    val hasSeenFinanceTutorial: Boolean = false,
    val hasSeenReportsTutorial: Boolean = false,
    val hasSeenSettingsTutorial: Boolean = false,
    // زبان برنامه: "fa" یا "en"؛ کاربر از تنظیمات تغییر می‌دهد.
    val language: String = LANG_FA,
) {
    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val LANG_FA = "fa"
        const val LANG_EN = "en"
    }

    val anyReminderEnabled: Boolean
        get() = remindDailySummary || remindWeeklySummary || remindMonthlySummary ||
            remindInstallments || remindOilChange || remindBodyInsurance ||
            remindVehicleInsurance || remindInspection || remindBackup
}

enum class TutorialTab { HOME, SERVICES, CALENDAR, FINANCE, REPORTS, SETTINGS }

class SettingsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("car_manager_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    val current: AppSettings get() = _state.value

    private fun read(): AppSettings = AppSettings(
        themeMode = prefs.getString(K_THEME, AppSettings.THEME_SYSTEM) ?: AppSettings.THEME_SYSTEM,
        privacyMode = prefs.getBoolean(K_PRIVACY, false),
        monthlyIncomeGoal = prefs.getFloat(K_GOAL, 0f).toDouble(),
        remindDailySummary = prefs.getBoolean(K_R_DAILY, false),
        remindWeeklySummary = prefs.getBoolean(K_R_WEEKLY, false),
        remindMonthlySummary = prefs.getBoolean(K_R_MONTHLY, false),
        remindInstallments = prefs.getBoolean(K_R_INSTALL, true),
        remindOilChange = prefs.getBoolean(K_R_OIL, true),
        remindBodyInsurance = prefs.getBoolean(K_R_BODY, true),
        remindVehicleInsurance = prefs.getBoolean(K_R_VEHICLE, true),
        remindInspection = prefs.getBoolean(K_R_INSPECT, true),
        remindBackup = prefs.getBoolean(K_R_BACKUP, true),
        bodyInsuranceDate = prefs.getString(K_D_BODY, "") ?: "",
        vehicleInsuranceDate = prefs.getString(K_D_VEHICLE, "") ?: "",
        inspectionDate = prefs.getString(K_D_INSPECT, "") ?: "",
        backupIntervalDays = prefs.getInt(K_BACKUP_DAYS, 14),
        lastBackupAt = prefs.getLong(K_BACKUP_AT, 0L),
        hasSeenServiceTutorial = prefs.getBoolean(K_TUT_SERVICE, false),
        oilChangeCurrentKm = prefs.getFloat(K_OIL_CUR, 0f).toDouble(),
        oilChangeNextKm = prefs.getFloat(K_OIL_NEXT, 0f).toDouble(),
        oilChangeSetAt = prefs.getString(K_OIL_SET_AT, "") ?: "",
        hasSeenHomeTutorial = prefs.getBoolean(K_TUT_HOME, false),
        hasSeenCalendarTutorial = prefs.getBoolean(K_TUT_CALENDAR, false),
        hasSeenFinanceTutorial = prefs.getBoolean(K_TUT_FINANCE, false),
        hasSeenReportsTutorial = prefs.getBoolean(K_TUT_REPORTS, false),
        hasSeenSettingsTutorial = prefs.getBoolean(K_TUT_SETTINGS, false),
        language = prefs.getString(K_LANG, AppSettings.LANG_FA) ?: AppSettings.LANG_FA,
    )

    private fun write(s: AppSettings) {
        prefs.edit()
            .putString(K_THEME, s.themeMode)
            .putBoolean(K_PRIVACY, s.privacyMode)
            .putFloat(K_GOAL, s.monthlyIncomeGoal.toFloat())
            .putBoolean(K_R_DAILY, s.remindDailySummary)
            .putBoolean(K_R_WEEKLY, s.remindWeeklySummary)
            .putBoolean(K_R_MONTHLY, s.remindMonthlySummary)
            .putBoolean(K_R_INSTALL, s.remindInstallments)
            .putBoolean(K_R_OIL, s.remindOilChange)
            .putBoolean(K_R_BODY, s.remindBodyInsurance)
            .putBoolean(K_R_VEHICLE, s.remindVehicleInsurance)
            .putBoolean(K_R_INSPECT, s.remindInspection)
            .putBoolean(K_R_BACKUP, s.remindBackup)
            .putString(K_D_BODY, s.bodyInsuranceDate)
            .putString(K_D_VEHICLE, s.vehicleInsuranceDate)
            .putString(K_D_INSPECT, s.inspectionDate)
            .putInt(K_BACKUP_DAYS, s.backupIntervalDays)
            .putLong(K_BACKUP_AT, s.lastBackupAt)
            .putBoolean(K_TUT_SERVICE, s.hasSeenServiceTutorial)
            .putFloat(K_OIL_CUR, s.oilChangeCurrentKm.toFloat())
            .putFloat(K_OIL_NEXT, s.oilChangeNextKm.toFloat())
            .putString(K_OIL_SET_AT, s.oilChangeSetAt)
            .putBoolean(K_TUT_HOME, s.hasSeenHomeTutorial)
            .putBoolean(K_TUT_CALENDAR, s.hasSeenCalendarTutorial)
            .putBoolean(K_TUT_FINANCE, s.hasSeenFinanceTutorial)
            .putBoolean(K_TUT_REPORTS, s.hasSeenReportsTutorial)
            .putBoolean(K_TUT_SETTINGS, s.hasSeenSettingsTutorial)
            .putString(K_LANG, s.language)
            .apply()
        _state.value = s
    }

    fun update(transform: (AppSettings) -> AppSettings) = write(transform(_state.value))

    fun markBackupDone() = update { it.copy(lastBackupAt = System.currentTimeMillis()) }

    fun markServiceTutorialSeen() = update { it.copy(hasSeenServiceTutorial = true) }

    fun markTutorialSeen(tab: TutorialTab) = update {
        when (tab) {
            TutorialTab.HOME -> it.copy(hasSeenHomeTutorial = true)
            TutorialTab.SERVICES -> it.copy(hasSeenServiceTutorial = true)
            TutorialTab.CALENDAR -> it.copy(hasSeenCalendarTutorial = true)
            TutorialTab.FINANCE -> it.copy(hasSeenFinanceTutorial = true)
            TutorialTab.REPORTS -> it.copy(hasSeenReportsTutorial = true)
            TutorialTab.SETTINGS -> it.copy(hasSeenSettingsTutorial = true)
        }
    }

    /** Sets the oil-change baseline and stamps today's date as the km-tracking anchor. */
    fun setOilChangeTarget(currentKm: Double, nextKm: Double, todayJalali: String) = update {
        it.copy(oilChangeCurrentKm = currentKm, oilChangeNextKm = nextKm, oilChangeSetAt = todayJalali)
    }

    fun setLanguage(lang: String) = update { it.copy(language = lang) }

    /** JSON fragment embedded in the backup file. No PIN, no security material. */
    fun toBackupJson(): String {
        val s = _state.value
        fun b(v: Boolean) = if (v) "true" else "false"
        return "{" +
            "\"themeMode\":\"" + s.themeMode + "\"," +
            "\"privacyMode\":" + b(s.privacyMode) + "," +
            "\"monthlyIncomeGoal\":" + s.monthlyIncomeGoal + "," +
            "\"remindDailySummary\":" + b(s.remindDailySummary) + "," +
            "\"remindWeeklySummary\":" + b(s.remindWeeklySummary) + "," +
            "\"remindMonthlySummary\":" + b(s.remindMonthlySummary) + "," +
            "\"remindInstallments\":" + b(s.remindInstallments) + "," +
            "\"remindOilChange\":" + b(s.remindOilChange) + "," +
            "\"remindBodyInsurance\":" + b(s.remindBodyInsurance) + "," +
            "\"remindVehicleInsurance\":" + b(s.remindVehicleInsurance) + "," +
            "\"remindInspection\":" + b(s.remindInspection) + "," +
            "\"remindBackup\":" + b(s.remindBackup) + "," +
            "\"bodyInsuranceDate\":\"" + s.bodyInsuranceDate + "\"," +
            "\"vehicleInsuranceDate\":\"" + s.vehicleInsuranceDate + "\"," +
            "\"inspectionDate\":\"" + s.inspectionDate + "\"," +
            "\"backupIntervalDays\":" + s.backupIntervalDays + "," +
            "\"oilChangeCurrentKm\":" + s.oilChangeCurrentKm + "," +
            "\"oilChangeNextKm\":" + s.oilChangeNextKm + "," +
            "\"oilChangeSetAt\":\"" + s.oilChangeSetAt + "\"," +
            "\"language\":\"" + s.language + "\"" +
            "}"
    }

    /** Applies a restored settings block. Missing keys keep their current value. */
    fun applyBackup(
        themeMode: String?, privacyMode: Boolean?, monthlyIncomeGoal: Double?,
        remindDailySummary: Boolean?, remindWeeklySummary: Boolean?, remindMonthlySummary: Boolean?,
        remindInstallments: Boolean?, remindOilChange: Boolean?, remindBodyInsurance: Boolean?,
        remindVehicleInsurance: Boolean?, remindInspection: Boolean?, remindBackup: Boolean?,
        bodyInsuranceDate: String?, vehicleInsuranceDate: String?, inspectionDate: String?,
        backupIntervalDays: Int?,
        oilChangeCurrentKm: Double? = null, oilChangeNextKm: Double? = null, oilChangeSetAt: String? = null,
        language: String? = null,
    ) = update { c ->
        c.copy(
            themeMode = themeMode ?: c.themeMode,
            privacyMode = privacyMode ?: c.privacyMode,
            monthlyIncomeGoal = monthlyIncomeGoal ?: c.monthlyIncomeGoal,
            remindDailySummary = remindDailySummary ?: c.remindDailySummary,
            remindWeeklySummary = remindWeeklySummary ?: c.remindWeeklySummary,
            remindMonthlySummary = remindMonthlySummary ?: c.remindMonthlySummary,
            remindInstallments = remindInstallments ?: c.remindInstallments,
            remindOilChange = remindOilChange ?: c.remindOilChange,
            remindBodyInsurance = remindBodyInsurance ?: c.remindBodyInsurance,
            remindVehicleInsurance = remindVehicleInsurance ?: c.remindVehicleInsurance,
            remindInspection = remindInspection ?: c.remindInspection,
            remindBackup = remindBackup ?: c.remindBackup,
            bodyInsuranceDate = bodyInsuranceDate ?: c.bodyInsuranceDate,
            vehicleInsuranceDate = vehicleInsuranceDate ?: c.vehicleInsuranceDate,
            inspectionDate = inspectionDate ?: c.inspectionDate,
            backupIntervalDays = backupIntervalDays ?: c.backupIntervalDays,
            oilChangeCurrentKm = oilChangeCurrentKm ?: c.oilChangeCurrentKm,
            oilChangeNextKm = oilChangeNextKm ?: c.oilChangeNextKm,
            oilChangeSetAt = oilChangeSetAt ?: c.oilChangeSetAt,
            language = language ?: c.language,
        )
    }

    companion object {
        private const val K_THEME = "themeMode"
        private const val K_PRIVACY = "privacyMode"
        private const val K_GOAL = "monthlyIncomeGoal"
        private const val K_R_DAILY = "remindDailySummary"
        private const val K_R_WEEKLY = "remindWeeklySummary"
        private const val K_R_MONTHLY = "remindMonthlySummary"
        private const val K_R_INSTALL = "remindInstallments"
        private const val K_R_OIL = "remindOilChange"
        private const val K_R_BODY = "remindBodyInsurance"
        private const val K_R_VEHICLE = "remindVehicleInsurance"
        private const val K_R_INSPECT = "remindInspection"
        private const val K_R_BACKUP = "remindBackup"
        private const val K_D_BODY = "bodyInsuranceDate"
        private const val K_D_VEHICLE = "vehicleInsuranceDate"
        private const val K_D_INSPECT = "inspectionDate"
        private const val K_BACKUP_DAYS = "backupIntervalDays"
        private const val K_BACKUP_AT = "lastBackupAt"
        private const val K_TUT_SERVICE = "hasSeenServiceTutorial"
        private const val K_OIL_CUR = "oilChangeCurrentKm"
        private const val K_OIL_NEXT = "oilChangeNextKm"
        private const val K_OIL_SET_AT = "oilChangeSetAt"
        private const val K_TUT_HOME = "hasSeenHomeTutorial"
        private const val K_TUT_CALENDAR = "hasSeenCalendarTutorial"
        private const val K_TUT_FINANCE = "hasSeenFinanceTutorial"
        private const val K_TUT_REPORTS = "hasSeenReportsTutorial"
        private const val K_TUT_SETTINGS = "hasSeenSettingsTutorial"
        private const val K_LANG = "language"

        @Volatile private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore = instance ?: synchronized(this) {
            instance ?: SettingsStore(context).also { instance = it }
        }
    }
}
