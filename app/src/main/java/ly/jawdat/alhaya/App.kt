package ly.jawdat.alhaya

import android.app.Application
import ly.jawdat.alhaya.data.Prefs
import ly.jawdat.alhaya.data.Repo
import ly.jawdat.alhaya.reminders.Reminders
import ly.jawdat.alhaya.ui.Lang

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Repo.init(this)
        Lang.arabic = Prefs.arabic
        Reminders.createChannel(this)
        Reminders.schedule(this)
    }
}
