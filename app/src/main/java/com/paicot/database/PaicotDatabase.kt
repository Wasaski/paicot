package com.paicot.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val runCount: Int = 0,
    val lastRunAt: Long = 0,
    val lastStatus: String = ""
)

@Entity(tableName = "executions")
data class ExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val execId: String,
    val source: String,
    val state: String,
    val stdout: String,
    val stderr: String,
    val error: String?,
    val startedAt: Long,
    val finishedAt: Long,
    val durationMs: Long
)

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    suspend fun all(): List<ScriptEntity>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(script: ScriptEntity): Long

    @Update
    suspend fun update(script: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ExecutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExecutionEntity)

    @Query("SELECT * FROM executions ORDER BY startedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 50): List<ExecutionEntity>

    @Query("DELETE FROM executions WHERE id NOT IN (SELECT id FROM executions ORDER BY startedAt DESC LIMIT 100)")
    suspend fun prune()
}

@Database(entities = [ScriptEntity::class, ExecutionEntity::class], version = 1, exportSchema = false)
abstract class PaicotDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
    abstract fun executionDao(): ExecutionDao
}
