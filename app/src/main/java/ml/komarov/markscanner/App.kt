package ml.komarov.markscanner

import android.app.Application
import androidx.room.Room
import ml.komarov.markscanner.db.AppDatabase


class App : Application() {
    private var database: AppDatabase? = null
    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, AppDatabase::class.java, "database")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }

    fun getDatabase(): AppDatabase? {
        return database
    }

    companion object {
        var instance: App? = null
    }
}