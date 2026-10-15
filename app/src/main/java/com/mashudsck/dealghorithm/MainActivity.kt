package com.mashudsck.dealghorithm

import android.Manifest
import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import java.time.LocalDate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val model get() = ViewModelProvider(this)[AppViewModel::class.java]
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent.getStringExtra("page")?.let(model.runtime::navigate)
        setContent { DeAlghorithmScreen(model) }
    }
    override fun onResume() { super.onResume(); model.runtime.refresh() }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("page")?.let(model.runtime::navigate)
    }
}

@Composable
private fun DeAlghorithmScreen(model: AppViewModel) {
    val ui by model.ui.collectAsStateWithLifecycle()
    val runtime = model.runtime
    var confirmDelete by remember { mutableStateOf(false) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { runtime.refresh() }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xffb45309), onPrimary = Color.White,
        background = Color(0xfffaf8f5), surface = Color.White, onBackground = Color(0xff451a03), onSurface = Color(0xff292524)),
        shapes = Shapes(small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(8.dp))) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DeAlghorithm", style = MaterialTheme.typography.titleLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { runtime.navigate("timer") }) { Text("Timer") }
                    TextButton(onClick = { runtime.navigate("activities") }) { Text("Activities") }
                    TextButton(onClick = { runtime.navigate("summary") }) { Text("Summary") }
                    TextButton(onClick = { runtime.navigate("settings") }) { Text("Settings") }
                }
                if (!ui.ready) Text("Loading local data")
                else if (ui.error != null) {
                    Text(ui.error!!)
                    ActionButton("Delete all data") { confirmDelete = true }
                } else {
                    val state = ui.state!!
                    when (ui.page) {
                        "settings" -> {
                            Text("Local data and reminders", style = MaterialTheme.typography.titleMedium)
                            Text("Automatic monitoring unavailable")
                            ObservationSetup(ui, runtime)
                            ActionButton("Disable monitoring") { runtime.disableMonitoring() }
                            Text("Your manual timer, saved activities and summaries stay on this device.")
                            Row {
                                Text("Allow reminder requests", Modifier.weight(1f).padding(top = 12.dp))
                                Switch(checked = state.preferences.remindersEnabled, onCheckedChange = { runtime.reminders(it) })
                            }
                            Text(if (!state.preferences.remindersEnabled) "Reminders disabled" else if (!ui.notificationsAllowed) "Notification permission needed" else "Reminders enabled")
                            if (!ui.notificationsAllowed && Build.VERSION.SDK_INT >= 33) ActionButton("Allow notifications") {
                                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            ActionButton("Delete all data") { confirmDelete = true }
                        }
                        "activities" -> Activities(ui, runtime)
                        "summary" -> Summary(state)
                        else -> Timer(ui, runtime)
                    }
                }
            }
            if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete all data?") },
                text = { Text("This removes your timer, activities, summaries and preferences.") },
                confirmButton = { TextButton(onClick = { confirmDelete = false; runtime.deleteAllData() }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
        }
    }
}

@Composable
private fun ObservationSetup(ui: UiState, runtime: AppRuntime) {
    val state = ui.state!!
    val context = LocalContext.current
    var pkg by remember(state.generation) { mutableStateOf(state.preferences.selectedPackage ?: trialPackages.first()) }
    var disclose by remember(state.generation) { mutableStateOf(false) }
    var settingsMissing by remember(state.generation) { mutableStateOf(false) }
    Text("Observation trial", style = MaterialTheme.typography.titleMedium)
    Text("TikTok is a test target. Automatic budgets, scores and background monitoring remain unavailable.")
    Text(ui.observation.message)
    if (!state.preferences.monitoringOptIn) {
        Text("Select the TikTok package installed on your device for the trial.")
        for (candidate in trialPackages) Row(Modifier.heightIn(min = 48.dp)
            .selectable(selected = pkg == candidate, role = Role.RadioButton, onClick = { pkg = candidate })
            .semantics(mergeDescendants = true) {}) {
            RadioButton(selected = pkg == candidate, onClick = null)
            Text(candidate, Modifier.padding(top = 12.dp))
        }
        ActionButton("Set up observation trial") { disclose = true }
    } else {
        Text("Selected trial app: ${state.preferences.selectedPackage}")
        Text("Usage Access: ${if (ui.observation.usageAccess) "granted" else "needed"}")
        if (!ui.observation.installed && ui.observation.message != "Selected trial app is not installed") Text("Selected trial app is not installed")
        if (!ui.observation.usageAccess) ActionButton("Open Usage Access settings") {
            try { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            catch (_: ActivityNotFoundException) { settingsMissing = true }
        }
        if (settingsMissing) Text("Usage Access settings are unavailable on this device. Manual features still work.")
        if (ui.observation.usageAccess && ui.observation.installed) {
            if (!ui.observation.running) ActionButton("Start observation trial") { runtime.startObservationTrial(state.generation) }
            else {
                Text("Diagnostic checks only. Lock/unlock once for a fresh lifecycle baseline; check within one minute.")
                ActionButton("Check recent observation") { runtime.checkObservation(state.generation) }
            }
        }
        ui.observation.snapshot?.let { trial ->
            Text("Trial selected-app intervals: ${durationLabel(trial.selectedMillis)}")
            Text("Repeat openings: ${trial.repeatOpenings?.toString() ?: "unavailable"}")
            Text("Observed state: ${when (trial.focus) {
                ObservedApp.SELECTED -> "Selected app"
                ObservedApp.DEALGORITHM -> "DeAlghorithm"
                ObservedApp.OTHER -> "Other app"
                ObservedApp.UNKNOWN -> "Unknown or ambiguous"
            }}")
            Text("Trial data stays transient. No measured summary or score is created.")
        }
    }
    if (disclose) AlertDialog(onDismissRequest = { disclose = false }, title = { Text("Agree to observation trial?") },
        text = { Text("After you grant Android Usage Access, explicit checks read recent app lifecycle and screen-lock events. " +
            "The selected app, DeAlghorithm, other apps and unknown states are distinguished in memory. " +
            "No feed, messages, searches or screen contents are read. Events and unrelated names are not saved or transmitted. " +
            "Manual timers remain independent. Disable monitoring stops the trial and keeps your summaries.") },
        confirmButton = { TextButton(onClick = { disclose = false; runtime.enableObservationTrial(pkg, state.generation) }) { Text("Agree to trial") } },
        dismissButton = { TextButton(onClick = { disclose = false }) { Text("Cancel") } })
}

@Composable
private fun Activities(ui: UiState, runtime: AppRuntime) {
    val state = ui.state!!
    var editing by remember(state.generation) { mutableStateOf(false) }
    var editingId by remember(state.generation) { mutableStateOf<String?>(null) }
    var name by remember(state.generation) { mutableStateOf("") }
    Text("Replacement activities", style = MaterialTheme.typography.titleMedium)
    Text("Choices are recorded; completion is not tracked.")
    if (state.activities.isEmpty()) Text("No saved activities")
    ui.activityMessage?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    state.activities.forEachIndexed { index, activity ->
        HorizontalDivider()
        Text(activity.name, style = MaterialTheme.typography.titleSmall)
        ActionButton("Choose ${activity.name}") { runtime.selectActivity(activity.id, state.generation) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { editingId = activity.id; name = activity.name; editing = true },
                modifier = Modifier.heightIn(min = 48.dp)) { Text("Rename ${activity.name}") }
        }
        TextButton(onClick = { runtime.deleteActivity(activity.id, state.generation) },
            modifier = Modifier.heightIn(min = 48.dp)) { Text("Delete ${activity.name}") }
        if (index > 0) TextButton(onClick = { runtime.moveActivity(activity.id, -1, state.generation) },
            modifier = Modifier.heightIn(min = 48.dp)) { Text("Move ${activity.name} up") }
        if (index < state.activities.lastIndex) TextButton(onClick = { runtime.moveActivity(activity.id, 1, state.generation) },
            modifier = Modifier.heightIn(min = 48.dp)) { Text("Move ${activity.name} down") }
    }
    if (state.activities.size < 10) ActionButton("Add an activity") { editingId = null; name = ""; editing = true }
    else Text("10 activities saved. Delete one to add another.")
    ActionButton("Take a break") { runtime.finish(true) }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text(if (editingId == null) "Add an activity" else "Rename activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Activity name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), isError = name.trim().length > 80)
                Text("1–80 characters")
            }
        },
        confirmButton = { TextButton(enabled = name.trim().length in 1..80, onClick = {
            val id = editingId
            if (id == null) runtime.addActivity(name, state.generation) else runtime.renameActivity(id, name, state.generation)
            editing = false
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } })
}

@Composable
private fun Summary(state: AppState) {
    val today = LocalDate.now()
    val days = state.summaries.associateBy { it.date }
    Text("Seven-day summary", style = MaterialTheme.typography.titleMedium)
    Text("Today and the previous six local dates. Monitoring unavailable; measured totals show saved observations only.")
    Text("Reminder requests do not mean reminders were read. Activity selections do not mean completion.")
    for (offset in 0L..6L) {
        val date = today.minusDays(offset)
        val day = days[date]
        HorizontalDivider()
        Text(if (offset == 0L) "$date · Today" else date.toString(), style = MaterialTheme.typography.titleSmall)
        Text(if (day != null && day.measuredMillis > 0L) "Measured app use: ${durationLabel(day.measuredMillis)}" else "Measured app use: unavailable")
        Text("Manual timer: ${durationLabel(day?.manualMillis ?: 0)}")
        Text("Reminder requests: ${day?.reminderRequests ?: 0}")
        Text("Breaks chosen: ${day?.breaksChosen ?: 0}")
        Text("Activity selections: ${day?.activitySelections ?: 0}")
    }
}

private fun durationLabel(millis: Long): String {
    val seconds = millis / 1_000
    return "${seconds / 60} min ${seconds % 60} sec"
}

@Composable
private fun Timer(ui: UiState, runtime: AppRuntime) {
    var minutes by remember { mutableIntStateOf(5) }
    var purpose by remember { mutableStateOf(Purpose.ENTERTAINMENT) }
    val intent = ui.state!!.intent
    Text("Manual timer", style = MaterialTheme.typography.titleMedium)
    Text("Elapsed time for your intention")
    Text("Measured app use unavailable")
    Text("Score unavailable")
    if (intent == null || intent.status == IntentStatus.FINISHED) {
        if (intent?.status == IntentStatus.FINISHED) Text("Finished")
        Text("Purpose")
        for (choice in Purpose.entries) Row(Modifier.heightIn(min = 48.dp)
            .selectable(selected = purpose == choice, role = Role.RadioButton, onClick = { purpose = choice })
            .semantics(mergeDescendants = true) {}) {
            RadioButton(selected = purpose == choice, onClick = null)
            Text(purposeLabel(choice), Modifier.padding(top = 12.dp))
        }
        if (purpose == Purpose.OTHER) OutlinedTextField(value = ui.otherPurpose, onValueChange = { runtime.purposeText(it, ui.state!!.generation) },
            label = { Text("Optional purpose") }, modifier = Modifier.fillMaxWidth())
        Text("Budget")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (choice in listOf(5, 15, 30)) OutlinedButton(onClick = { minutes = choice },
                modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(6.dp)) { Text(if (minutes == choice) "$choice min selected" else "$choice min") }
        }
        ActionButton("Start manual timer") { runtime.start(minutes, purpose) }
    } else {
        Text("Purpose: ${purposeLabel(intent.purpose)}")
        Text(when (intent.status) {
            IntentStatus.RUNNING -> "Running"
            IntentStatus.PAUSED -> "Paused"
            IntentStatus.EXPIRED -> "Overdue"
            IntentStatus.FINISHED -> "Finished"
        })
        val seconds = (intent.remainingMillis + 999) / 1_000
        Text("Remaining: ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}")
        when (intent.status) {
            IntentStatus.RUNNING -> ActionButton("Pause") { runtime.pause() }
            IntentStatus.PAUSED -> if (intent.remainingMillis > 0) ActionButton("Resume") { runtime.resume() }
            else -> Unit
        }
        ActionButton("Finish") { runtime.finish(false) }
        if (intent.status == IntentStatus.EXPIRED || (intent.status == IntentStatus.PAUSED && intent.remainingMillis == 0L)) {
            ActionButton("Add 5 minutes") { runtime.extend() }
            ActionButton("Choose an activity") { runtime.navigate("activities") }
        }
        ActionButton("Take a break") { runtime.finish(true) }
    }
    if (!ui.notificationsAllowed || !ui.state.preferences.remindersEnabled) Text("Reminders disabled")
}

private fun purposeLabel(purpose: Purpose) = when (purpose) {
    Purpose.FIND_SOMETHING -> "Find something"
    Purpose.REPLY -> "Reply to someone"
    Purpose.ENTERTAINMENT -> "Entertainment"
    Purpose.OTHER -> "Other"
}

@Composable
private fun ActionButton(label: String, action: () -> Unit) {
    Button(onClick = action, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(6.dp)) { Text(label) }
}
