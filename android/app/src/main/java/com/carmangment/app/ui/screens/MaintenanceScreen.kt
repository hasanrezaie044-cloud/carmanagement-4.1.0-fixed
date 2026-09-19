package com.carmangment.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carmangment.app.core.finance.MaintenanceType
import com.carmangment.app.core.jalali.Jalali
import com.carmangment.app.core.analytics.Analytics
import com.carmangment.app.core.rates.CarType
import com.carmangment.app.core.rates.RatesByYear
import com.carmangment.app.data.db.MaintenanceEntity
import com.carmangment.app.data.prefs.AppSettings
import com.carmangment.app.data.repo.AppRepository
import com.carmangment.app.ui.components.*
import com.carmangment.app.ui.theme.AppDimens
import kotlinx.coroutines.launch

/**
 * Maintenance. The seven legacy preset categories are intact — they are now inline
 * selectable cards instead of a chip strip that scrolled off the edge, and the form is a
 * full-screen panel rather than a bottom sheet. Cost uses the thousands-separated money
 * field.
 */
@Composable
fun MaintenanceScreen(repository: AppRepository) {
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    val year = remember { Jalali.currentYear() }
    val records by repository.maintenances().collectAsState(initial = emptyList())
    val cost by repository.maintenanceCost("$year/").collectAsState(initial = 0.0)
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MaintenanceEntity?>(null) }
    val settings by repository.settings.collectAsState(initial = repository.settingsNow)

    if (showForm) {
        MaintenanceForm(repository) { showForm = false }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = AppDimens.screenPadding, vertical = 8.dp)) {
            MetricPair(
                first = { m ->
                    MetricCard("تعداد رکورد", records.size.toString(), Icons.Outlined.Build, m, compact = true)
                },
                second = { m ->
                    MetricCard(
                        "هزینه سال $year", amount(cost), Icons.Outlined.Payments, m,
                        compact = true, accent = MaterialTheme.colorScheme.secondary,
                    )
                },
            )
            Spacer(Modifier.height(AppDimens.gap))
            OilChangeTargetCard(repository, settings)
        }
        if (records.isEmpty()) {
            EmptyState(
                "سابقه‌ای ثبت نشده",
                "تعویض روغن و تعمیرات را ثبت کنید تا یادآور کیلومتری کار کند.",
                Icons.Outlined.Build,
                Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(AppDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.gap),
            ) {
                items(records, key = { it.id }) { item ->
                    Surface(
                        shape = FieldShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = AppDimens.rowMinHeight),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Build, null, tint = MaterialTheme.colorScheme.secondary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    MaintenanceType.fromWire(item.type).label.let {
                                        if (item.typeText.isNotBlank() && item.type == MaintenanceType.OTHER.wire) item.typeText else it
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    listOf(
                                        item.date,
                                        if (item.km > 0) "کیلومتر ${money(item.km)}" else "",
                                        item.description,
                                    ).filter { it.isNotBlank() }.joinToString(" · "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                amount(item.cost),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                            IconButton(onClick = { pendingDelete = item }) {
                                Icon(Icons.Outlined.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { showForm = true },
            modifier = Modifier
                .padding(horizontal = AppDimens.screenPadding, vertical = 10.dp)
                .fillMaxWidth()
                .height(AppDimens.buttonHeight),
            shape = FieldShape,
        ) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("ثبت تعمیرات") }
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "حذف سابقه تعمیر",
            message = "رکورد ${item.date} حذف شود؟",
            confirmLabel = "حذف",
            destructive = true,
            onConfirm = {
                scope.launch {
                    repository.deleteMaintenance(item.id)
                    toast.show("سابقه تعمیر حذف شد")
                    pendingDelete = null
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun MaintenanceForm(repository: AppRepository, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    val ratesByYear by repository.ratesByYear.collectAsState(initial = RatesByYear.empty())

    var date by remember { mutableStateOf(Jalali.todayString()) }
    var type by remember { mutableStateOf(MaintenanceType.OIL_CHANGE) }
    var typeText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var kmText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var carType by remember { mutableStateOf(CarType.SOREN) }

    val year = Jalali.parse(date)?.year ?: Jalali.currentYear()
    val rates = remember(ratesByYear, year) { ratesByYear.forYear(year) }
    LaunchedEffect(rates.defaultCarType) { carType = rates.defaultCarType }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .clearFocusOnTap()
                .padding(horizontal = AppDimens.screenPadding)
                .padding(top = AppDimens.gutter, bottom = AppDimens.gutter),
            verticalArrangement = Arrangement.spacedBy(AppDimens.gutter),
        ) {
            FormSection("نوع تعمیر", Icons.Outlined.Build) {
                OptionSelector(
                    label = "",
                    options = MaintenanceType.entries.toList(),
                    selected = type,
                    optionLabel = { it.label },
                    onSelect = { type = it },
                    columns = 2,
                )
                if (type == MaintenanceType.OTHER) {
                    Spacer(Modifier.height(AppDimens.gap))
                    TextFieldR("عنوان تعمیر", typeText, { typeText = it })
                }
            }
            FormSection("تاریخ، هزینه و کیلومتر", Icons.Outlined.Event) {
                JalaliDateField("تاریخ", date, { date = it })
                Spacer(Modifier.height(AppDimens.gap))
                MoneyField("هزینه", costText, { costText = it })
                Spacer(Modifier.height(AppDimens.gap))
                NumberField("کیلومتر", kmText, { kmText = it }, suffix = "کیلومتر فعلی خودرو")
                Spacer(Modifier.height(AppDimens.gap))
                TextFieldR("توضیحات", description, { description = it }, singleLine = false, imeAction = ImeAction.Done)
            }
            FormSection("خودرو", Icons.Outlined.DirectionsCar) {
                SegmentedSelector(
                    options = CarType.entries.toList(),
                    selected = carType,
                    label = { it.label },
                    onSelect = { carType = it },
                )
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.screenPadding, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f).height(AppDimens.buttonHeight),
                    shape = FieldShape,
                ) { Text("انصراف") }
                Button(
                    onClick = {
                        scope.launch {
                            repository.addMaintenance(
                                date = date,
                                type = type.wire,
                                typeText = typeText.ifBlank { type.label },
                                cost = costText.asDouble(),
                                km = kmText.asDouble(),
                                carType = carType.wire,
                                description = description.trim(),
                            )
                            toast.show("سابقه تعمیر ثبت شد")
                            onClose()
                        }
                    },
                    modifier = Modifier.weight(1f).height(AppDimens.buttonHeight),
                    shape = FieldShape,
                ) { Text("ذخیره") }
            }
        }
    }
}

/**
 * Oil-change target: the user types the odometer reading at the last change and the
 * odometer they want the next change to happen at, directly — no interval math to think
 * about. The app tracks the gap itself from the km already logged on services dated
 * after that was set (see AppRepository.kmSince / Analytics.oilChangeDue).
 */
@Composable
private fun OilChangeTargetCard(repository: AppRepository, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val toast = LocalToast.current
    var currentKmText by remember(settings.oilChangeCurrentKm) {
        mutableStateOf(if (settings.oilChangeCurrentKm > 0) money(settings.oilChangeCurrentKm) else "")
    }
    var nextKmText by remember(settings.oilChangeNextKm) {
        mutableStateOf(if (settings.oilChangeNextKm > 0) money(settings.oilChangeNextKm) else "")
    }
    var kmSince by remember { mutableStateOf(0.0) }
    LaunchedEffect(settings.oilChangeSetAt) {
        kmSince = if (settings.oilChangeSetAt.isNotBlank()) repository.kmSince(settings.oilChangeSetAt) else 0.0
    }
    val due = remember(settings.oilChangeCurrentKm, settings.oilChangeNextKm, kmSince) {
        Analytics.oilChangeDue(settings.oilChangeCurrentKm, settings.oilChangeNextKm, kmSince)
    }

    SurfaceCard {
        Text("تعویض روغن", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(AppDimens.gap))
        NumberField("کیلومتر فعلی", currentKmText, { currentKmText = it }, suffix = "کیلومتر در زمان آخرین تعویض")
        Spacer(Modifier.height(AppDimens.gap))
        NumberField("کیلومتر بعدی", nextKmText, { nextKmText = it }, suffix = "هدف کیلومتر برای تعویض بعدی")
        Spacer(Modifier.height(AppDimens.gap))
        Button(
            onClick = {
                val cur = currentKmText.asDouble()
                val next = nextKmText.asDouble()
                if (next <= cur) {
                    toast.show("کیلومتر بعدی باید بیشتر از کیلومتر فعلی باشد")
                    return@Button
                }
                scope.launch {
                    repository.setOilChangeTarget(cur, next, Jalali.todayString())
                    toast.show("هدف تعویض روغن ذخیره شد")
                }
            },
            modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
            shape = FieldShape,
        ) { Text("ذخیره") }

        if (due.lastKm != null) {
            Spacer(Modifier.height(AppDimens.gap))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(AppDimens.gap))
            Text(
                if (due.isOverdue) {
                    "از موعد ${money(-(due.remainingKm ?: 0.0))} کیلومتر گذشته است"
                } else {
                    "تا تعویض بعدی ${money(due.remainingKm ?: 0.0)} کیلومتر — " +
                        "از سرویس‌های ثبت‌شده تاکنون ${money(kmSince)} کیلومتر طی شده"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (due.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
