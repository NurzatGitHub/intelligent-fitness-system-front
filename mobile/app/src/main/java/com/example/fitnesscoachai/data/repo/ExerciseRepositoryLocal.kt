package com.example.fitnesscoachai.data.repo

import com.example.fitnesscoachai.domain.catalog.CatalogTree
import com.example.fitnesscoachai.domain.model.Exercise
import com.example.fitnesscoachai.domain.model.ExerciseMedia
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.domain.model.SubCategory
import com.example.fitnesscoachai.domain.repo.ExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Локальная реализация репозитория.
 * Сейчас — заглушка из памяти. Позже: загрузка из assets/exercises.json или Room.
 */
class ExerciseRepositoryLocal : ExerciseRepository {

    private val exercises = mutableListOf<Exercise>().apply {
        addAll(seedExercises())
    }

    override suspend fun getExercisesBySubCategory(subId: String): List<Exercise> =
        withContext(Dispatchers.Default) {
            exercises.filter { it.sub.id == subId }
        }

    override suspend fun getExercisesByMainCategory(main: MainCategory): List<Exercise> =
        withContext(Dispatchers.Default) {
            exercises.filter { it.main == main }
        }

    override suspend fun getExerciseById(id: String): Exercise? =
        withContext(Dispatchers.Default) {
            exercises.find { it.id == id }
        }

    private fun seedExercises(): List<Exercise> {
        val subs = CatalogTree.subCategories
        return listOf(
            sub(subs, "biceps", "ex1", "Barbell Curl",
                "Curl the barbell to your shoulders, keeping elbows close to your body.",
                listOf("Grip the bar at shoulder width.", "Stand with arms extended down.", "Curl the bar toward your shoulders.", "Lower slowly to the starting position."),
                listOf("Do not swing your body.", "Keep elbows tucked to your torso."),
                "barbell", "beginner", null),
            sub(subs, "biceps", "ex2", "Hammer Curl",
                "Curl dumbbells with a neutral grip. Targets biceps and brachialis.",
                listOf("Hold dumbbells with neutral grip.", "Curl toward your shoulders.", "Lower the dumbbells under control."),
                listOf("Do not swing.", "Keep your core stable."),
                "dumbbell", "beginner", null),
            sub(subs, "triceps", "ex3", "Tricep Pushdown",
                "Extend your arms downward on the cable, keeping elbows at your sides.",
                listOf("Grasp the cable bar.", "Keep elbows at your sides.", "Extend arms downward.", "Return the bar upward."),
                listOf("Do not flare elbows out.", "Movement only at the elbow joint."),
                "cable", "beginner", null),
            sub(subs, "triceps", "ex4", "Close-Grip Bench Press",
                "Bench press with a narrow grip. Lower the bar to the lower chest.",
                listOf("Lie on the bench, grip narrower than shoulders.", "Lower the bar to your chest.", "Press up, extending your arms."),
                listOf("Elbows along your body.", "Do not bounce the bar off your chest."),
                "barbell", "intermediate", null),
            sub(subs, "forearms", "ex5", "Wrist Curl",
                "Curl your wrists with a barbell while seated. Strengthens wrist flexors.",
                listOf("Sit with forearms on your thighs.", "Let your wrists hang down.", "Curl wrists up.", "Lower back down."),
                listOf("Control the range of motion.", "Do not use momentum."),
                "barbell", "beginner", null),
            sub(subs, "quads", "ex6", "Squat",
                "Back squat. Lower until thighs are at least parallel to the floor.",
                listOf("Place the bar on your upper back.", "Feet shoulder-width apart.", "Lower by bending knees and hips.", "Stand up by extending your legs."),
                listOf("Keep your back straight.", "Knees should not cave in."),
                "barbell", "beginner", null),
            sub(subs, "quads", "ex7", "Leg Extension",
                "Extend your legs at the knee in the machine.",
                listOf("Sit in the machine, shins under the pad.", "Extend your legs at the knees.", "Hold the top briefly.", "Lower the weight."),
                listOf("Do not lift your hips off the seat.", "Move smoothly."),
                "machine", "beginner", null),
            sub(subs, "hamstrings", "ex8", "Romanian Deadlift",
                "Hinge forward with a straight back. Stretches and loads the hamstrings.",
                listOf("Stand holding the bar.", "Hinge forward, pushing hips back.", "Lower the bar along your legs.", "Return to upright by driving through the hips."),
                listOf("Keep your back straight; do not round your lower back.", "Feel the stretch in the back of your legs."),
                "barbell", "intermediate", null),
            sub(subs, "glutes", "ex9", "Hip Thrust",
                "Drive your hips up from a seated position, upper back on a bench.",
                listOf("Sit on the floor with your back to the bench, bar on your hips.", "Press your upper back into the bench.", "Drive your hips up.", "Squeeze glutes at the top and lower."),
                listOf("Tuck your chin slightly.", "Do not over-arch your lower back."),
                "barbell", "beginner", null),
            sub(subs, "calves", "ex10", "Calf Raise",
                "Standing calf raise. Rise as high as you can on your toes.",
                listOf("Stand on a platform with balls of feet on the edge.", "Lower your heels below the platform.", "Rise up on your toes as high as possible.", "Lower back down."),
                listOf("Use full range at the bottom for a stretch.", "Pause at the top."),
                "body weight", "beginner", null),
            sub(subs, "lats", "ex11", "Pull-up",
                "Hang from the bar and pull until your chin is over the bar.",
                listOf("Grip the bar at shoulder width.", "Hang with arms fully extended.", "Pull yourself up, squeezing your shoulder blades.", "Lower yourself under control."),
                listOf("Do not swing.", "Engage your core."),
                "body weight", "intermediate", null),
            sub(subs, "lats", "ex12", "Lat Pulldown",
                "Pull the bar down to your chest, squeezing your shoulder blades.",
                listOf("Sit down and secure your thighs.", "Grasp the bar with a wide grip.", "Pull to the upper chest.", "Return the bar upward."),
                listOf("Do not lean back too far.", "Elbows drive down and back."),
                "cable", "beginner", null),
            sub(subs, "lower_back", "ex13", "Deadlift",
                "Lift the bar from the floor by extending your hips and knees.",
                listOf("Stand in front of the bar, feet hip-width apart.", "Bend down and grip the bar just outside your legs.", "Keep your back straight.", "Stand up by extending legs and hips."),
                listOf("Do not round your back.", "Keep the bar close to your legs."),
                "barbell", "intermediate", null),
            sub(subs, "upper_back", "ex14", "Barbell Row",
                "Row the bar to your waist in a bent-over position. Drive elbows along your body.",
                listOf("Bend forward holding the bar.", "Pull the bar to your waist.", "Squeeze shoulder blades at the top.", "Lower the bar."),
                listOf("Back parallel or at an angle to the floor.", "Do not jerk your torso up."),
                "barbell", "beginner", null),
            sub(subs, "pectorals", "ex15", "Bench Press",
                "Press the bar up from a lying position. Lower to the middle of your chest.",
                listOf("Lie on the bench, grip wider than shoulders.", "Unrack the bar.", "Lower to the middle of your chest.", "Press the bar up."),
                listOf("Squeeze shoulder blades, feet on the floor.", "Elbows at about 45°."),
                "barbell", "beginner", null),
            sub(subs, "pectorals", "ex16", "Push-up",
                "Push-ups from the floor. Keep your body in a straight line from head to heels.",
                listOf("Start in a plank, hands shoulder-width apart.", "Lower your chest toward the floor.", "Push yourself back up to the start."),
                listOf("Do not sag your lower back.", "Keep your core tight."),
                "body weight", "beginner", null),
            sub(subs, "abs_core", "ex17", "Crunches",
                "Lying crunch: lift your shoulder blades off the floor using your abs.",
                listOf("Lie on your back, knees bent.", "Hands behind head or on chest.", "Lift your shoulder blades off the floor.", "Lower without resting your head on the floor."),
                listOf("Do not pull on your neck.", "Exhale on the way up."),
                "body weight", "beginner", null),
            sub(subs, "abs_core", "ex18", "Plank",
                "Hold a forearm and toes position. Keep your body in a straight line.",
                listOf("Support on forearms and toes.", "Body in one line.", "Hold for the set time."),
                listOf("Do not let your hips sag or pike up.", "Engage abs and glutes."),
                "body weight", "beginner", null),
            sub(subs, "hiit", "ex19", "Burpees",
                "From standing: squat → jump feet back → push-up → jump feet in → jump up.",
                listOf("From standing, squat and place hands on the floor.", "Jump feet back to a push-up position.", "Do a push-up.", "Jump feet to your hands.", "Jump up with a clap."),
                listOf("Keep a steady pace.", "Land softly."),
                "body weight", "intermediate", null),
            sub(subs, "endurance", "ex20", "Running",
                "Run at a comfortable pace. Keep heart rate in the endurance zone.",
                listOf("Warm up for 5–10 minutes.", "Maintain a steady pace.", "Finish with a cooldown and stretch."),
                listOf("Breathe steadily.", "Do not increase pace too quickly."),
                "body weight", null, null),
        )
    }

    private fun sub(
        subs: List<SubCategory>,
        subId: String,
        id: String,
        titleEn: String,
        description: String,
        steps: List<String>,
        tips: List<String>,
        equipment: String?,
        difficulty: String?,
        path: String?
    ): Exercise {
        val sub = subs.find { it.id == subId }!!
        return Exercise(
            id = id,
            titleEn = titleEn,
            description = description,
            steps = steps,
            tips = tips,
            main = sub.main,
            sub = sub,
            equipment = equipment,
            difficulty = difficulty,
            media = path?.let { ExerciseMedia.LocalAsset(it) }
        )
    }
}
