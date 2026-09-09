package com.calculadoracalorias.app.data.repository

import androidx.room.withTransaction
import com.calculadoracalorias.app.data.local.AppDatabase
import com.calculadoracalorias.app.data.local.dao.MealDao
import com.calculadoracalorias.app.data.local.dao.SupplementDao
import com.calculadoracalorias.app.data.local.dao.SupplementLogDao
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity
import com.calculadoracalorias.app.data.local.entity.SupplementEntity
import com.calculadoracalorias.app.data.local.entity.SupplementLogEntity
import com.calculadoracalorias.app.data.local.relation.MealWithItems
import com.calculadoracalorias.app.data.preferences.UserPreferences
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.domain.model.MealTimeWindows
import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.model.backup.BackupMealEntry
import com.calculadoracalorias.app.domain.model.backup.BackupMealItem
import com.calculadoracalorias.app.domain.model.backup.BackupMealTimeWindows
import com.calculadoracalorias.app.domain.model.backup.BackupPreferences
import com.calculadoracalorias.app.domain.model.backup.BackupSupplement
import com.calculadoracalorias.app.domain.model.backup.BackupSupplementLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalTime

class RoomBackupRepositoryTest {

    private val appDatabase: AppDatabase = mockk(relaxed = true)
    private val mealDao: MealDao = mockk(relaxed = true)
    private val supplementDao: SupplementDao = mockk(relaxed = true)
    private val supplementLogDao: SupplementLogDao = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)

    private lateinit var repository: RoomBackupRepository

    @BeforeEach
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { appDatabase.withTransaction(any<suspend () -> Any>()) } coAnswers {
            val block = secondArg<suspend () -> Any>()
            block()
        }

        repository = RoomBackupRepository(
            appDatabase = appDatabase,
            mealDao = mealDao,
            supplementDao = supplementDao,
            supplementLogDao = supplementLogDao,
            userPreferencesRepository = userPreferencesRepository,
            appVersionName = "1.1"
        )
    }

    @Test
    @DisplayName("exportBackupPayload debe compilar todos los datos de Room y DataStore en un BackupDataPayload")
    fun testExportBackupPayload() = runBlocking {
        val mealEntry = MealEntryEntity(
            id = 1L,
            category = "DESAYUNO",
            timestamp = 1788775200000L,
            totalCalories = 300.0,
            totalProtein = 10.0,
            totalCarbs = 40.0,
            totalFat = 8.0,
            rawDescription = "Huevos con tostada"
        )
        val foodItem = MealFoodItemEntity(
            id = 10L,
            mealEntryId = 1L,
            name = "Huevos revueltos",
            servingGrams = 100.0,
            caloriesPer100g = 150.0,
            proteinPer100g = 12.0,
            carbsPer100g = 1.0,
            fatPer100g = 10.0
        )
        val mealWithItems = MealWithItems(meal = mealEntry, items = listOf(foodItem))

        val supplement = SupplementEntity(
            id = "creatina",
            name = "Creatina",
            dosageDescription = "5g",
            calories = 0.0,
            proteinGrams = 0.0,
            carbsGrams = 0.0,
            fatGrams = 0.0,
            isActive = true,
            isCustom = false
        )

        val supplementLog = SupplementLogEntity(
            date = "2026-09-08",
            supplementId = "creatina",
            takenTimestamp = 1788771000000L
        )

        val prefs = UserPreferences(
            targetCalories = 2100.0,
            targetProteinGrams = 160.0,
            targetCarbsGrams = 210.0,
            targetFatGrams = 60.0,
            mealTimeWindows = MealTimeWindows(
                breakfastStart = LocalTime.of(8, 0),
                breakfastEnd = LocalTime.of(10, 0)
            )
        )

        coEvery { mealDao.getAllMealsWithItems() } returns listOf(mealWithItems)
        coEvery { supplementDao.getAllSupplementsList() } returns listOf(supplement)
        coEvery { supplementLogDao.getAllLogs() } returns listOf(supplementLog)
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(prefs)

        val result = repository.exportBackupPayload()

        assertNotNull(result)
        assertEquals(1, result.version)
        assertEquals("1.1", result.appVersionName)
        assertEquals(1, result.meals.size)
        assertEquals("DESAYUNO", result.meals[0].category)
        assertEquals(1, result.meals[0].items.size)
        assertEquals("Huevos revueltos", result.meals[0].items[0].name)
        assertEquals(1, result.supplements.size)
        assertEquals("creatina", result.supplements[0].id)
        assertEquals(1, result.supplementLogs.size)
        assertEquals("2026-09-08", result.supplementLogs[0].date)
        assertEquals(2100.0, result.preferences.targetCalories)
        assertEquals(480, result.preferences.mealTimeWindows.breakfastStartMinute)
    }

    @Test
    @DisplayName("importBackupPayload debe restaurar comidas, suplementos y preferencias atómicamente")
    fun testImportBackupPayload() = runBlocking {
        val payload = BackupDataPayload(
            version = 1,
            exportedAt = 1788775200000L,
            appVersionName = "1.1",
            meals = listOf(
                BackupMealEntry(
                    id = 2L,
                    category = "ALMUERZO",
                    timestamp = 1788775200000L,
                    totalCalories = 600.0,
                    totalProteinGrams = 40.0,
                    totalCarbsGrams = 70.0,
                    totalFatGrams = 15.0,
                    items = listOf(
                        BackupMealItem(
                            id = 20L,
                            name = "Pollo",
                            portionGrams = 200.0,
                            calories = 330.0,
                            proteinGrams = 62.0,
                            carbsGrams = 0.0,
                            fatGrams = 7.2,
                            caloriesPer100g = 165.0,
                            proteinPer100g = 31.0,
                            carbsPer100g = 0.0,
                            fatPer100g = 3.6
                        )
                    )
                )
            ),
            supplements = listOf(
                BackupSupplement(
                    id = "omega_3",
                    name = "Omega 3",
                    dosageDescription = "2 caps",
                    calories = 18.0,
                    proteinGrams = 0.0,
                    carbsGrams = 0.0,
                    fatGrams = 2.0
                )
            ),
            supplementLogs = listOf(
                BackupSupplementLog(
                    date = "2026-09-08",
                    supplementId = "omega_3",
                    takenTimestamp = 1788771000000L
                )
            ),
            preferences = BackupPreferences(
                targetCalories = 2300.0,
                targetProteinGrams = 170.0,
                targetCarbsGrams = 230.0,
                targetFatGrams = 70.0,
                mealTimeWindows = BackupMealTimeWindows(
                    breakfastStartMinute = 420,
                    breakfastEndMinute = 660,
                    lunchStartMinute = 720,
                    lunchEndMinute = 960,
                    dinnerStartMinute = 1140,
                    dinnerEndMinute = 1380
                )
            )
        )

        repository.importBackupPayload(payload)

        coVerify(exactly = 1) { mealDao.restoreMealsWithItems(any()) }
        coVerify(exactly = 1) { supplementDao.deleteAllSupplements() }
        coVerify(exactly = 1) { supplementDao.insertOrReplaceAll(any()) }
        coVerify(exactly = 1) { supplementLogDao.deleteAllLogs() }
        coVerify(exactly = 1) { supplementLogDao.insertAllLogs(any()) }
        coVerify(exactly = 1) {
            userPreferencesRepository.restorePreferences(
                targetCalories = 2300.0,
                targetProtein = 170.0,
                targetCarbs = 230.0,
                targetFat = 70.0,
                windows = any()
            )
        }
    }
}
