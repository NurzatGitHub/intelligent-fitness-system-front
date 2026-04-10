package com.example.fitnesscoachai.ui.workout.shoulderpress

/**
 * Накапливает кадры и собирает 28-мерный вектор для модели:
 *   [down_feats(14) | up_feats(14)]
 *
 * Пороги из анализа датасета:
 *   DOWN_THRESHOLD = 40.7  (min_elbow <= порог → DOWN фаза)
 *   UP_THRESHOLD   = 151.7 (min_elbow >= порог → UP фаза)
 *
 * Алгоритм:
 *   - Каждый кадр добавляем в скользящее окно (WINDOW = 5)
 *   - Отдельно храним лучший DOWN кадр (наименьший min_elbow)
 *     и лучший UP кадр (наибольший min_elbow)
 *   - Когда оба захвачены → возвращаем concat(down, up)
 *   - После подсчёта репа — сбрасываем для следующего цикла
 */
class ShoulderPressRepBuffer {

    companion object {
        const val DOWN_THRESHOLD = 40.7f
        const val UP_THRESHOLD   = 151.7f
        private const val WINDOW = 5
    }

    // Лучший DOWN кадр: наименьший min_elbow среди кадров <= DOWN_THRESHOLD
    private var bestDownFeats: FloatArray? = null
    private var bestDownElbow = Float.MAX_VALUE

    // Лучший UP кадр: наибольший min_elbow среди кадров >= UP_THRESHOLD
    private var bestUpFeats: FloatArray? = null
    private var bestUpElbow = Float.MIN_VALUE

    // Скользящее окно для сглаживания
    private val window = ArrayDeque<FloatArray>(WINDOW)

    fun reset() {
        bestDownFeats = null
        bestDownElbow = Float.MAX_VALUE
        bestUpFeats   = null
        bestUpElbow   = Float.MIN_VALUE
        window.clear()
    }

    /**
     * Добавляет кадр. Возвращает 28-мерный вектор если оба (DOWN + UP)
     * уже захвачены, иначе null.
     */
    fun push(features: FloatArray): FloatArray? {
        // Скользящее среднее
        if (window.size >= WINDOW) window.removeFirst()
        window.addLast(features)
        val smoothed = smoothed()

        val me = ShoulderPressFeatureExtractor.minElbow(smoothed)

        // Обновляем DOWN
        if (me <= DOWN_THRESHOLD && me < bestDownElbow) {
            bestDownElbow = me
            bestDownFeats = smoothed.copyOf()
        }

        // Обновляем UP
        if (me >= UP_THRESHOLD && me > bestUpElbow) {
            bestUpElbow = me
            bestUpFeats = smoothed.copyOf()
        }

        // Если оба захвачены — вернуть вектор
        val down = bestDownFeats
        val up   = bestUpFeats
        if (down != null && up != null) {
            return down + up  // FloatArray(28)
        }

        return null
    }

    fun hasDown() = bestDownFeats != null
    fun hasUp()   = bestUpFeats   != null

    private fun smoothed(): FloatArray {
        if (window.size == 1) return window.first()
        val n    = window.size
        val size = window.first().size
        return FloatArray(size) { i -> window.sumOf { it[i].toDouble() }.toFloat() / n }
    }
}
