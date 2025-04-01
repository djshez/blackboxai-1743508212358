package com.example.ht2000obd.data

import androidx.room.*

@Entity(tableName = "diagnostic_codes")
data class DiagnosticCode(
    @PrimaryKey val code: String,
    val description: String,
    val possibleCauses: String,
    val solutions: String,
    val severity: Int, // 1-5, with 5 being most severe
    val category: String // Engine, Transmission, etc.
)

@Entity(tableName = "vehicle_profiles")
data class VehicleProfile(
    @PrimaryKey val id: Int,
    val make: String,
    val model: String,
    val year: Int,
    val engineCode: String,
    val transmissionType: String,
    val specificPids: String // JSON string of supported PIDs
)

@Dao
interface DiagnosticDao {
    @Query("SELECT * FROM diagnostic_codes WHERE code = :code")
    suspend fun getCodeDetails(code: String): DiagnosticCode?

    @Query("SELECT * FROM diagnostic_codes WHERE category = :category")
    suspend fun getCodesByCategory(category: String): List<DiagnosticCode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCode(code: DiagnosticCode)

    @Query("SELECT * FROM vehicle_profiles WHERE make = :make AND model = :model AND year = :year")
    suspend fun getVehicleProfile(make: String, model: String, year: Int): VehicleProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleProfile(profile: VehicleProfile)
}

@Database(entities = [DiagnosticCode::class, VehicleProfile::class], version = 1)
abstract class DiagnosticDatabase : RoomDatabase() {
    abstract fun diagnosticDao(): DiagnosticDao

    companion object {
        @Volatile
        private var INSTANCE: DiagnosticDatabase? = null

        fun getDatabase(context: android.content.Context): DiagnosticDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiagnosticDatabase::class.java,
                    "diagnostic_database"
                )
                .createFromAsset("databases/diagnostic_codes.db")
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}