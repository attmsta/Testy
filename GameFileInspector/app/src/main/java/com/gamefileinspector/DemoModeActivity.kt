package com.gamefileinspector

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gamefileinspector.adapters.PossibleValueAdapter
import com.gamefileinspector.databinding.ActivityDemoModeBinding
import com.gamefileinspector.models.DataType
import com.gamefileinspector.models.PossibleValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Demo mode activity that showcases the app's capabilities with sample data
 */
class DemoModeActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDemoModeBinding
    private lateinit var possibleValueAdapter: PossibleValueAdapter
    private var currentStep = 0
    
    private val demoSteps = listOf(
        DemoStep(
            title = "Welcome to Game File Inspector Demo",
            description = "This demo will show you how to analyze and modify game files safely.",
            values = emptyList()
        ),
        DemoStep(
            title = "Step 1: File Analysis",
            description = "The app automatically detects game values in your files. Here are some examples:",
            values = listOf(
                PossibleValue(
                    key = "player_gold",
                    value = "12500",
                    dataType = DataType.INTEGER,
                    confidence = 0.95,
                    description = "Player's gold currency",
                    location = "player.gold",
                    category = "currency"
                ),
                PossibleValue(
                    key = "player_level",
                    value = "25",
                    dataType = DataType.INTEGER,
                    confidence = 0.90,
                    description = "Player's current level",
                    location = "player.level",
                    category = "progress"
                ),
                PossibleValue(
                    key = "experience_points",
                    value = "15750",
                    dataType = DataType.INTEGER,
                    confidence = 0.88,
                    description = "Total experience points",
                    location = "player.experience",
                    category = "experience"
                )
            )
        ),
        DemoStep(
            title = "Step 2: Value Categories",
            description = "Values are categorized by type with confidence scores:",
            values = listOf(
                PossibleValue(
                    key = "premium_gems",
                    value = "150",
                    dataType = DataType.INTEGER,
                    confidence = 0.92,
                    description = "Premium currency (gems)",
                    location = "player.gems",
                    category = "gems"
                ),
                PossibleValue(
                    key = "energy",
                    value = "100",
                    dataType = DataType.INTEGER,
                    confidence = 0.85,
                    description = "Player energy/stamina",
                    location = "player.energy",
                    category = "energy"
                ),
                PossibleValue(
                    key = "high_score",
                    value = "98750",
                    dataType = DataType.INTEGER,
                    confidence = 0.80,
                    description = "Player's highest score",
                    location = "stats.highScore",
                    category = "score"
                )
            )
        ),
        DemoStep(
            title = "Step 3: Different Data Types",
            description = "The app handles various data types safely:",
            values = listOf(
                PossibleValue(
                    key = "sound_enabled",
                    value = "true",
                    dataType = DataType.BOOLEAN,
                    confidence = 0.95,
                    description = "Sound effects enabled",
                    location = "settings.soundEnabled",
                    category = "settings"
                ),
                PossibleValue(
                    key = "music_volume",
                    value = "0.8",
                    dataType = DataType.FLOAT,
                    confidence = 0.90,
                    description = "Music volume level",
                    location = "settings.musicVolume",
                    category = "settings"
                ),
                PossibleValue(
                    key = "player_name",
                    value = "DemoPlayer",
                    dataType = DataType.STRING,
                    confidence = 0.75,
                    description = "Player's display name",
                    location = "player.name",
                    category = "profile"
                )
            )
        ),
        DemoStep(
            title = "Step 4: Modification Example",
            description = "Watch as we safely modify a value (this is just a demo):",
            values = listOf(
                PossibleValue(
                    key = "player_gold",
                    value = "99999",
                    dataType = DataType.INTEGER,
                    confidence = 0.95,
                    description = "Modified gold amount",
                    location = "player.gold",
                    category = "currency",
                    originalValue = "12500"
                )
            )
        ),
        DemoStep(
            title = "Demo Complete!",
            description = "You've seen how Game File Inspector works. Key features:\n\n" +
                    "• Automatic game value detection\n" +
                    "• Safe modification with backups\n" +
                    "• Multiple file format support\n" +
                    "• Confidence scoring\n" +
                    "• Type validation\n\n" +
                    "Ready to try it with real game files?",
            values = emptyList()
        )
    )
    
    data class DemoStep(
        val title: String,
        val description: String,
        val values: List<PossibleValue>
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoModeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        showCurrentStep()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Demo Mode"
        }
    }
    
    private fun setupRecyclerView() {
        possibleValueAdapter = PossibleValueAdapter { possibleValue ->
            // In demo mode, show explanation instead of modification
            showValueExplanation(possibleValue)
        }
        
        binding.recyclerViewValues.apply {
            layoutManager = LinearLayoutManager(this@DemoModeActivity)
            adapter = possibleValueAdapter
        }
    }
    
    private fun setupButtons() {
        binding.buttonPrevious.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                showCurrentStep()
            }
        }
        
        binding.buttonNext.setOnClickListener {
            if (currentStep < demoSteps.size - 1) {
                currentStep++
                showCurrentStep()
            } else {
                // Demo complete, return to main activity
                finish()
            }
        }
        
        binding.buttonSkipDemo.setOnClickListener {
            finish()
        }
    }
    
    private fun showCurrentStep() {
        val step = demoSteps[currentStep]
        
        binding.apply {
            textStepTitle.text = step.title
            textStepDescription.text = step.description
            textStepCounter.text = "${currentStep + 1} / ${demoSteps.size}"
            
            // Update button states
            buttonPrevious.isEnabled = currentStep > 0
            buttonNext.text = if (currentStep == demoSteps.size - 1) "Finish Demo" else "Next"
            
            // Update progress
            progressBar.progress = ((currentStep + 1) * 100) / demoSteps.size
        }
        
        // Animate value appearance
        if (step.values.isNotEmpty()) {
            lifecycleScope.launch {
                possibleValueAdapter.updateValues(emptyList())
                delay(300)
                
                step.values.forEachIndexed { index, value ->
                    delay(500)
                    val currentValues = possibleValueAdapter.getCurrentValues().toMutableList()
                    currentValues.add(value)
                    possibleValueAdapter.updateValues(currentValues)
                }
            }
        } else {
            possibleValueAdapter.updateValues(step.values)
        }
        
        // Special animations for certain steps
        when (currentStep) {
            4 -> animateModification()
        }
    }
    
    private fun animateModification() {
        lifecycleScope.launch {
            delay(1000)
            Toast.makeText(this@DemoModeActivity, "Creating backup...", Toast.LENGTH_SHORT).show()
            delay(1000)
            Toast.makeText(this@DemoModeActivity, "Modifying value...", Toast.LENGTH_SHORT).show()
            delay(1000)
            Toast.makeText(this@DemoModeActivity, "Modification complete!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showValueExplanation(value: PossibleValue) {
        val explanation = when (value.category) {
            "currency", "gold" -> {
                "Currency values like gold and coins are commonly found in game files. " +
                "They usually have high confidence scores because they're frequently modified by players."
            }
            "gems" -> {
                "Premium currencies like gems are often stored separately from regular currency. " +
                "Be careful when modifying these as some games validate them server-side."
            }
            "progress" -> {
                "Progress values include levels, stages, and ranks. These are usually small integers " +
                "and are safe to modify in most single-player games."
            }
            "experience" -> {
                "Experience points are often stored as cumulative totals. Some games calculate " +
                "level from experience, so modifying XP might be more effective than modifying level directly."
            }
            "score" -> {
                "Score values are typically large numbers. High scores are usually safe to modify " +
                "unless the game has online leaderboards."
            }
            "settings" -> {
                "Game settings are usually safe to modify and can improve your gaming experience. " +
                "Boolean settings control features on/off, while numeric settings control levels."
            }
            else -> {
                "This value was detected with ${(value.confidence * 100).toInt()}% confidence. " +
                "Higher confidence values are more likely to be actual game values."
            }
        }
        
        val title = "About ${value.key}"
        val message = "$explanation\n\nValue: ${value.value}\nType: ${value.dataType.name}\nConfidence: ${(value.confidence * 100).toInt()}%"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}