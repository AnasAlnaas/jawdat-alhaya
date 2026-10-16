package ly.jawdat.alhaya.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ly.jawdat.alhaya.R
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.reminders.Reminders
import ly.jawdat.alhaya.ui.AppCard
import ly.jawdat.alhaya.ui.AppScreen
import ly.jawdat.alhaya.ui.ChipRow
import ly.jawdat.alhaya.ui.ConfirmDialog
import ly.jawdat.alhaya.ui.Danger
import ly.jawdat.alhaya.ui.Field
import ly.jawdat.alhaya.ui.FormBody
import ly.jawdat.alhaya.ui.Ic
import ly.jawdat.alhaya.ui.Lang
import ly.jawdat.alhaya.ui.ListBody
import ly.jawdat.alhaya.ui.Nav
import ly.jawdat.alhaya.ui.Screen
import ly.jawdat.alhaya.ui.SectionTitle
import ly.jawdat.alhaya.ui.t
import ly.jawdat.alhaya.util.Files
import java.time.LocalDate

@Composable
fun MoreScreen() {
    AppScreen(title = t("المزيد", "More")) { pad ->
        ListBody(pad) {
            val entries = listOf(
                Triple(R.drawable.ic_store, t("المحلات", "Markets"), Screen.Markets),
                Triple(R.drawable.ic_money, t("المنتجات والأسعار", "Products & prices"), Screen.Products),
                Triple(R.drawable.ic_box, t("المخزون", "Stock"), Screen.Stock),
                Triple(R.drawable.ic_chart, t("المصاريف", "Expenses"), Screen.Expenses),
                Triple(R.drawable.ic_settings, t("الإعدادات والنسخ الاحتياطي", "Settings & backup"), Screen.Settings),
            )
            entries.forEach { (icon, label, screen) ->
                item {
                    AppCard(onClick = { Nav.go(screen) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Ic(icon, Modifier.size(24.dp), MaterialTheme.colorScheme.primary)
                            Text("   $label", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Ic(R.drawable.ic_chevron_forward, Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var hour by remember { mutableStateOf(Prefs.reminderHour) }
    var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            val ok = runCatching { Files.writeBackup(context, uri) }.isSuccess
            Toast.makeText(context, if (ok) t("تم حفظ النسخة الاحتياطية", "Backup saved") else t("تعذّر الحفظ", "Could not save"), Toast.LENGTH_LONG).show()
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> confirmRestore = uri }

    AppScreen(t("الإعدادات", "Settings"), onBack = { Nav.back() }) { pad ->
        FormBody(pad) {
            SectionTitle(t("اللغة", "Language"))
            ChipRow(listOf(true to "العربية", false to "English"), Lang.arabic) { Lang.set(it) }

            SectionTitle(t("مراحل الإنتاج", "Production stages"))
            OutlinedButton(onClick = { Nav.go(Screen.StageTemplate) }, modifier = Modifier.fillMaxWidth()) {
                Text(t("تعديل المراحل الافتراضية للدفعات الجديدة", "Edit the default stages for new batches"))
            }

            SectionTitle(t("التذكيرات", "Reminders"))
            Text(t("ساعة التذكير اليومي بالمراحل المستحقة", "Daily reminder time for due stages"), style = MaterialTheme.typography.bodyMedium)
            ChipRow(listOf(7, 9, 12, 16, 20).map { it to "%02d:00".format(it) }, hour) {
                hour = it
                Prefs.reminderHour = it
                Reminders.schedule(context)
            }

            SectionTitle(t("النسخ الاحتياطي", "Backup"))
            AppCard {
                Text(
                    t(
                        "كل البيانات محفوظة على هذا الهاتف فقط. احفظ نسخة احتياطية كل أسبوع في Google Drive أو أرسلها لنفسك، حتى لا تضيع إذا ضاع الهاتف.",
                        "All data is kept on this phone only. Save a backup every week to Google Drive or send it to yourself, so nothing is lost if the phone is.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = { saveBackup.launch("jawdat-backup-${LocalDate.now()}.zip") }, modifier = Modifier.fillMaxWidth()) {
                Ic(R.drawable.ic_backup, Modifier.size(18.dp)); Text("  " + t("حفظ نسخة احتياطية (Drive أو الهاتف)", "Save backup (Drive or phone)"))
            }
            OutlinedButton(onClick = { runCatching { Files.shareBackup(context) } }, modifier = Modifier.fillMaxWidth()) {
                Ic(R.drawable.ic_share, Modifier.size(18.dp)); Text("  " + t("إرسال النسخة (واتساب، تيليجرام...)", "Send backup (WhatsApp, Telegram...)"))
            }
            OutlinedButton(onClick = { openBackup.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                Text(t("استرجاع من نسخة احتياطية", "Restore from a backup"))
            }

            SectionTitle(t("عن التطبيق", "About"))
            Text(
                t("جودة الحياة — لإدارة إنتاج وبيع خل التمر.\nالخط: Readex Pro (رخصة OFL).", "Jawdat Al-Hayah — managing date vinegar production and sales.\nFont: Readex Pro (OFL licence)."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    confirmRestore?.let { uri ->
        ConfirmDialog(
            t("استرجاع النسخة؟", "Restore this backup?"),
            t("ستُستبدل كل البيانات الحالية ببيانات النسخة الاحتياطية.", "All current data will be replaced by the backup."),
            t("استرجاع", "Restore"),
            {
                val ok = runCatching { Files.restoreBackup(context, uri) }.getOrDefault(false)
                Toast.makeText(context, if (ok) t("تم الاسترجاع", "Restored") else t("هذا الملف ليس نسخة احتياطية صالحة", "This file is not a valid backup"), Toast.LENGTH_LONG).show()
                if (ok) Nav.tab(Screen.Home)
            },
            { confirmRestore = null },
        )
    }
}

@Composable
fun StageTemplateScreen() {
    val stages = remember(Lang.arabic) { mutableStateListOf<String>().apply { addAll(Prefs.stageTemplate) } }
    var newName by remember { mutableStateOf("") }
    fun save() { Prefs.stageTemplate = stages.toList() }

    AppScreen(t("المراحل الافتراضية", "Default stages"), onBack = { Nav.back() }) { pad ->
        FormBody(pad) {
            Text(
                t(
                    "هذه المراحل تُضاف لكل دفعة جديدة، ولا تؤثر على الدفعات الحالية. مرحلة \"${Prefs.herbStage}\" تُضاف تلقائياً قبل الأخيرة للأنواع بالأعشاب.",
                    "These stages are added to every new batch; existing batches don't change. \"${Prefs.herbStage}\" is added before the last stage for herb types.",
                ),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            stages.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Field("${i + 1}", s, { stages[i] = it; save() }, Modifier.weight(1f))
                    Column {
                        TextButton(enabled = i > 0, onClick = { stages.add(i - 1, stages.removeAt(i)); save() }) { Text("▲") }
                        TextButton(enabled = i < stages.lastIndex, onClick = { stages.add(i + 1, stages.removeAt(i)); save() }) { Text("▼") }
                    }
                    IconButton(onClick = { stages.removeAt(i); save() }) { Ic(R.drawable.ic_delete, Modifier.size(20.dp), Danger) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(t("مرحلة جديدة", "New stage"), newName, { newName = it }, Modifier.weight(1f))
                Button(enabled = newName.isNotBlank(), onClick = { stages.add(newName.trim()); newName = ""; save() }) { Text(t("إضافة", "Add")) }
            }
            TextButton(onClick = { stages.clear(); stages.addAll(Prefs.defaultStages(Lang.arabic)); save() }) {
                Text(t("استعادة المراحل الأصلية", "Reset to the original stages"))
            }
        }
    }
}
