package ml.komarov.markscanner.db

import androidx.room.*


@Dao
interface HistoryDao {
    @get:Query("SELECT * FROM History")
    val all: List<History>

    @get:Query("SELECT COUNT(*) FROM History")
    val count: Int

    @Query("SELECT * FROM History WHERE id = (:id)")
    fun getHistory(id: Long): History?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistory(history: History): Long

    @Delete
    fun delete(history: History?)
}