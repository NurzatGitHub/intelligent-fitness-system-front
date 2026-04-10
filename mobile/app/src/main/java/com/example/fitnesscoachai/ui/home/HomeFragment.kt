package com.example.fitnesscoachai.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.WeeklyPlanDay
import com.example.fitnesscoachai.data.models.WeeklyPlanResponse
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.ui.exercise.ExerciseListActivity
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var categoryAdapter: CategoryAdapter

    companion object {
        private var weeklyPlanCache: WeeklyPlanResponse? = null
    }

    data class DayCardBinding(
        val card: MaterialCardView,
        val label: TextView,
        val type: TextView,
        val title: TextView
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindUserHeader(view)

        setupCategoryRecyclerView(view)
        categoryAdapter.setCategories(MainCategory.entries)

        val cached = weeklyPlanCache
        if (cached != null) {
            bindWeeklyPlan(view, cached)
        } else {
            loadWeeklyPlan(view)
        }

        loadOverallStatus(view)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            view?.let { bindUserHeader(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { bindUserHeader(it) }
    }

    private fun bindUserHeader(view: View) {
        val authPrefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userName = authPrefs.getString("user_name", "beginner") ?: "beginner"
        view.findViewById<TextView>(R.id.tvUserName)?.text = "$userName 👋"

        val avatarUri = requireContext()
            .getSharedPreferences("user_profile", Context.MODE_PRIVATE)
            .getString("avatar_uri", null)

        if (!avatarUri.isNullOrBlank()) {
            view.findViewById<ImageView>(R.id.ivAvatar)?.let { avatar ->
                avatar.setImageURI(Uri.parse(avatarUri))
                avatar.background = null
                avatar.setPadding(0, 0, 0, 0)
                avatar.clearColorFilter()
                avatar.imageTintList = null
                avatar.invalidate()
            }
        }
    }

    private fun setupCategoryRecyclerView(view: View) {
        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategories)
        categoryAdapter = CategoryAdapter(emptyList()) { main ->
            startActivity(ExerciseListActivity.newIntent(requireContext(), main.id))
        }
        rvCategories.adapter = categoryAdapter
        rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun loadWeeklyPlan(view: View) {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val isGuest = prefs.getBoolean("isGuest", false)
        val token = prefs.getString("access_token", null)

        if (isGuest || token.isNullOrBlank()) {
            showGuestPlan(view)
            return
        }

        setLoadingState(view)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getWeeklyPlan("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val plan = response.body()!!
                    weeklyPlanCache = plan
                    bindWeeklyPlan(view, plan)
                } else {
                    showPlanError(view)
                }
            } catch (e: Exception) {
                showPlanError(view)
            }
        }
    }

    private fun setLoadingState(view: View) {
        view.findViewById<TextView>(R.id.tvAiPlanTitle)?.text = "AI Weekly Plan"
        view.findViewById<TextView>(R.id.tvAiPlanSummary)?.text = "Loading your weekly plan..."
        view.findViewById<TextView>(R.id.tvTodayPlan)?.text = "Today"
        view.findViewById<TextView>(R.id.tvTodayMeta)?.text = "Please wait..."

        dayBindings(view).forEachIndexed { index, binding ->
            binding.label.text = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]
            binding.type.text = "..."
            binding.title.text = "..."
            binding.card.setOnClickListener(null)
        }
    }

    private fun showGuestPlan(view: View) {
        view.findViewById<TextView>(R.id.tvAiPlanTitle)?.text = "AI Weekly Plan"
        view.findViewById<TextView>(R.id.tvAiPlanSummary)?.text = "Login to get a personalized weekly plan"
        view.findViewById<TextView>(R.id.tvTodayPlan)?.text = buildTodayHeader()
        view.findViewById<TextView>(R.id.tvTodayMeta)?.text = "Sign in and complete onboarding"

        dayBindings(view).forEachIndexed { index, binding ->
            binding.label.text = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]
            binding.type.text = "-"
            binding.title.text = "Login"
            binding.card.setOnClickListener(null)
        }
    }

    private fun showPlanError(view: View) {
        view.findViewById<TextView>(R.id.tvAiPlanTitle)?.text = "AI Weekly Plan"
        view.findViewById<TextView>(R.id.tvAiPlanSummary)?.text = "Could not load your plan right now"
        view.findViewById<TextView>(R.id.tvTodayPlan)?.text = buildTodayHeader()
        view.findViewById<TextView>(R.id.tvTodayMeta)?.text = "Try reopening the app"

        dayBindings(view).forEachIndexed { index, binding ->
            binding.label.text = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]
            binding.type.text = "-"
            binding.title.text = "Error"
            binding.card.setOnClickListener(null)
        }
    }

    private fun bindWeeklyPlan(view: View, plan: WeeklyPlanResponse) {
        view.findViewById<TextView>(R.id.tvAiPlanTitle)?.text =
            if (plan.title.isBlank()) "AI Weekly Plan" else plan.title

        view.findViewById<TextView>(R.id.tvAiPlanSummary)?.text =
            if (plan.goal_summary.isBlank()) "Personalized weekly training plan"
            else plan.goal_summary

        val todayIndex = getTodayIndex()
        val bindings = dayBindings(view)

        bindings.forEachIndexed { index, binding ->
            val day = plan.days.getOrNull(index)
            if (day != null) {
                bindDay(binding, day, index == todayIndex)
            } else {
                binding.label.text = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index]
                binding.type.text = "-"
                binding.title.text = "-"
                binding.card.setOnClickListener(null)
            }
        }

        val today = plan.days.getOrNull(todayIndex) ?: plan.days.firstOrNull()
        view.findViewById<TextView>(R.id.tvTodayPlan)?.text = buildTodayHeader()

        if (today != null) {
            val metaParts = buildList {
                add(today.label)
                add(today.type.replaceFirstChar { it.uppercase() })
                add("${today.duration_min} min")
                if (today.note.isNotBlank()) add(today.note)
            }
            view.findViewById<TextView>(R.id.tvTodayMeta)?.text = metaParts.joinToString(" • ")
        } else {
            view.findViewById<TextView>(R.id.tvTodayMeta)?.text = plan.today_tip
        }

        centerTodayCard(view, todayIndex)
    }

    private fun bindDay(binding: DayCardBinding, day: WeeklyPlanDay, isToday: Boolean) {
        binding.label.text = if (isToday) "${day.label} • Today" else day.label
        binding.type.text = day.type.replaceFirstChar { it.uppercase() }
        binding.title.text = day.title

        binding.card.strokeWidth = if (isToday) dp(3) else dp(1)
        binding.card.alpha = if (isToday) 1f else 0.88f
        binding.card.scaleX = if (isToday) 1.06f else 1f
        binding.card.scaleY = if (isToday) 1.06f else 1f
        binding.card.cardElevation = if (isToday) dp(6).toFloat() else 0f

        binding.label.setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)
        binding.title.setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)

        binding.card.setOnClickListener {
            openPlanDay(day)
        }
    }

    private fun openPlanDay(day: WeeklyPlanDay) {
        val json = Gson().toJson(day)
        val intent = Intent(requireContext(), WeeklyPlanDayActivity::class.java)
        intent.putExtra(WeeklyPlanDayActivity.EXTRA_DAY_JSON, json)
        startActivity(intent)
    }

    private fun centerTodayCard(view: View, todayIndex: Int) {
        val scroll = view.findViewById<HorizontalScrollView>(R.id.hsvWeekDays)
        val bindings = dayBindings(view)
        val todayCard = bindings.getOrNull(todayIndex)?.card ?: return

        scroll.post {
            val targetX = todayCard.left - (scroll.width - todayCard.width) / 2
            scroll.smoothScrollTo(targetX.coerceAtLeast(0), 0)
        }
    }

    private fun buildTodayHeader(): String {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH)
        return "Today • ${formatter.format(calendar.time)}"
    }

    private fun dayBindings(view: View): List<DayCardBinding> {
        return listOf(
            DayCardBinding(
                view.findViewById(R.id.cardDay1),
                view.findViewById(R.id.tvDay1Label),
                view.findViewById(R.id.tvDay1Type),
                view.findViewById(R.id.tvDay1Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay2),
                view.findViewById(R.id.tvDay2Label),
                view.findViewById(R.id.tvDay2Type),
                view.findViewById(R.id.tvDay2Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay3),
                view.findViewById(R.id.tvDay3Label),
                view.findViewById(R.id.tvDay3Type),
                view.findViewById(R.id.tvDay3Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay4),
                view.findViewById(R.id.tvDay4Label),
                view.findViewById(R.id.tvDay4Type),
                view.findViewById(R.id.tvDay4Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay5),
                view.findViewById(R.id.tvDay5Label),
                view.findViewById(R.id.tvDay5Type),
                view.findViewById(R.id.tvDay5Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay6),
                view.findViewById(R.id.tvDay6Label),
                view.findViewById(R.id.tvDay6Type),
                view.findViewById(R.id.tvDay6Title),
            ),
            DayCardBinding(
                view.findViewById(R.id.cardDay7),
                view.findViewById(R.id.tvDay7Label),
                view.findViewById(R.id.tvDay7Type),
                view.findViewById(R.id.tvDay7Title),
            ),
        )
    }

    private fun getTodayIndex(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }

    private fun loadOverallStatus(view: View) {
        val prefs = requireContext().getSharedPreferences("workout_history", Context.MODE_PRIVATE)
        val historyCount = prefs.getInt("history_count", 0)

        view.findViewById<TextView>(R.id.tvTotalWorkouts)?.text = historyCount.toString()

        val score = if (historyCount > 0) 78 else 0
        view.findViewById<TextView>(R.id.tvAverageFormScore)?.text = "$score%"
    }
}
