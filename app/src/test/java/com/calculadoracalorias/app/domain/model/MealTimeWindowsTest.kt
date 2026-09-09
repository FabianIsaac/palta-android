package com.calculadoracalorias.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class MealTimeWindowsTest {

    private val windows = MealTimeWindows()

    @Test
    fun `detectCategory detects breakfast within morning window`() {
        assertEquals(MealCategory.DESAYUNO, windows.detectCategory(LocalTime.of(6, 0)))
        assertEquals(MealCategory.DESAYUNO, windows.detectCategory(LocalTime.of(8, 30)))
        assertEquals(MealCategory.DESAYUNO, windows.detectCategory(LocalTime.of(11, 29)))
    }

    @Test
    fun `detectCategory detects lunch within midday window`() {
        assertEquals(MealCategory.ALMUERZO, windows.detectCategory(LocalTime.of(11, 30)))
        assertEquals(MealCategory.ALMUERZO, windows.detectCategory(LocalTime.of(13, 45)))
        assertEquals(MealCategory.ALMUERZO, windows.detectCategory(LocalTime.of(15, 59)))
    }

    @Test
    fun `detectCategory detects once cena within evening window`() {
        assertEquals(MealCategory.ONCE_CENA, windows.detectCategory(LocalTime.of(16, 0)))
        assertEquals(MealCategory.ONCE_CENA, windows.detectCategory(LocalTime.of(19, 30)))
        assertEquals(MealCategory.ONCE_CENA, windows.detectCategory(LocalTime.of(21, 59)))
    }

    @Test
    fun `detectCategory detects colaciones outside primary meal windows`() {
        assertEquals(MealCategory.COLACIONES, windows.detectCategory(LocalTime.of(22, 0)))
        assertEquals(MealCategory.COLACIONES, windows.detectCategory(LocalTime.of(23, 45)))
        assertEquals(MealCategory.COLACIONES, windows.detectCategory(LocalTime.of(0, 0)))
        assertEquals(MealCategory.COLACIONES, windows.detectCategory(LocalTime.of(3, 15)))
        assertEquals(MealCategory.COLACIONES, windows.detectCategory(LocalTime.of(5, 59)))
    }

    @Test
    fun `detectCategory respects custom meal time windows`() {
        val customWindows = MealTimeWindows(
            breakfastStart = LocalTime.of(7, 0),
            breakfastEnd = LocalTime.of(10, 0),
            lunchStart = LocalTime.of(12, 0),
            lunchEnd = LocalTime.of(15, 0),
            dinnerStart = LocalTime.of(18, 0),
            dinnerEnd = LocalTime.of(21, 0)
        )

        // En el rango previo al desayuno personalizado
        assertEquals(MealCategory.COLACIONES, customWindows.detectCategory(LocalTime.of(6, 30)))
        // Desayuno personalizado
        assertEquals(MealCategory.DESAYUNO, customWindows.detectCategory(LocalTime.of(7, 30)))
        // Intermedio (entre desayuno y almuerzo)
        assertEquals(MealCategory.COLACIONES, customWindows.detectCategory(LocalTime.of(10, 30)))
        // Almuerzo personalizado
        assertEquals(MealCategory.ALMUERZO, customWindows.detectCategory(LocalTime.of(13, 0)))
        // Intermedio tarde
        assertEquals(MealCategory.COLACIONES, customWindows.detectCategory(LocalTime.of(16, 30)))
        // Once/Cena personalizada
        assertEquals(MealCategory.ONCE_CENA, customWindows.detectCategory(LocalTime.of(19, 0)))
        // Noche
        assertEquals(MealCategory.COLACIONES, customWindows.detectCategory(LocalTime.of(21, 30)))
    }
}
