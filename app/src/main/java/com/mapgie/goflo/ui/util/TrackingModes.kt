package com.mapgie.goflo.ui.util

/**
 * A category that a tracking mode suggests adding. The [modeKey] is a stable,
 * unique string used to deduplicate across modes — if a category with that key
 * already exists it will not be created again.
 *
 * [defaultChecked] controls whether the checkbox is pre-ticked in the activation
 * sheet; all items can still be opted in or out freely.
 */
data class SuggestedCategory(
    val modeKey: String,
    val name: String,
    val description: String,
    val iconName: String = "category",
    val colorToken: String = "secondary",
    val categoryType: String = "default",
    val values: List<String> = emptyList(),
    val numericMin: Float = 0f,
    val numericMax: Float = 10f,
    val allowDecimals: Boolean = false,
    val numericUnit: String = "",
    val defaultChecked: Boolean = true,
)

/** UI features that a mode unlocks beyond its category bundle. */
enum class ModeFeature(val label: String) {
    BBT_CHART("BBT temperature chart in Stats (coming in a future update)"),
    PREGNANCY_COUNTER("Pregnancy week counter on the home screen"),
}

enum class AppMode(
    val id: String,
    val displayName: String,
    val description: String,
    val suggestedCategories: List<SuggestedCategory>,
    val features: List<ModeFeature> = emptyList(),
) {

    FERTILITY(
        id = "FERTILITY",
        displayName = "Fertility",
        description = "Track basal body temperature, cervical fluid, and ovulation to understand your cycle in detail.",
        features = listOf(ModeFeature.BBT_CHART),
        suggestedCategories = listOf(
            SuggestedCategory(
                modeKey      = "bbt_temperature",
                name         = "BBT Temperature",
                description  = "Daily basal body temperature reading",
                iconName     = "thermometer",
                colorToken   = "tertiary",
                categoryType = "numeric_slider",
                numericMin   = 35.0f,
                numericMax   = 42.0f,
                allowDecimals = true,
                numericUnit  = "°C",
            ),
            SuggestedCategory(
                modeKey      = "cervical_fluid",
                name         = "Cervical Fluid",
                description  = "Daily cervical mucus observation",
                iconName     = "water_drop",
                colorToken   = "secondary",
                categoryType = "default",
                values       = listOf("Dry", "Sticky", "Creamy", "Watery", "Egg white"),
            ),
        )
    ),

    PREGNANCY(
        id = "PREGNANCY",
        displayName = "Pregnancy",
        description = "See your pregnancy week and trimester on the home screen, and track how you're feeling.",
        features = listOf(ModeFeature.PREGNANCY_COUNTER),
        suggestedCategories = listOf(
            SuggestedCategory(
                modeKey      = "nausea",
                name         = "Nausea",
                description  = "Morning sickness severity (0 = none, 10 = severe)",
                iconName     = "sick",
                colorToken   = "secondary",
                categoryType = "numeric_slider",
                numericMin   = 0f,
                numericMax   = 10f,
            ),
            SuggestedCategory(
                modeKey       = "weight",
                name          = "Weight",
                description   = "Body weight",
                iconName      = "monitor_weight",
                colorToken    = "tertiary",
                categoryType  = "numeric_free",
                numericMin    = 30f,
                numericMax    = 200f,
                allowDecimals = true,
                numericUnit   = "kg",
                defaultChecked = false,
            ),
        )
    ),

    WEIGHT(
        id = "WEIGHT",
        displayName = "Weight",
        description = "Log your body weight over time and see trends alongside your cycle.",
        suggestedCategories = listOf(
            SuggestedCategory(
                modeKey       = "weight",
                name          = "Weight",
                description   = "Body weight",
                iconName      = "monitor_weight",
                colorToken    = "primary",
                categoryType  = "numeric_free",
                numericMin    = 30f,
                numericMax    = 200f,
                allowDecimals = true,
                numericUnit   = "kg",
            ),
        )
    ),

    ENDO(
        id = "ENDO",
        displayName = "Endometriosis",
        description = "Track pain, bloating, fatigue, and bowel symptoms to identify patterns and flare-ups.",
        suggestedCategories = listOf(
            SuggestedCategory(
                modeKey      = "pelvic_pain",
                name         = "Pelvic Pain",
                description  = "Pain severity (0 = none, 10 = severe)",
                iconName     = "healing",
                colorToken   = "primary",
                categoryType = "numeric_slider",
                numericMin   = 0f,
                numericMax   = 10f,
            ),
            SuggestedCategory(
                modeKey      = "bloating",
                name         = "Bloating",
                description  = "Abdominal bloating level",
                iconName     = "category",
                colorToken   = "secondary",
                categoryType = "default",
                values       = listOf("None", "Mild", "Moderate", "Severe"),
            ),
            SuggestedCategory(
                modeKey      = "fatigue",
                name         = "Fatigue",
                description  = "Energy and fatigue level (0 = none, 10 = exhausted)",
                iconName     = "bedtime",
                colorToken   = "tertiary",
                categoryType = "numeric_slider",
                numericMin   = 0f,
                numericMax   = 10f,
            ),
            SuggestedCategory(
                modeKey      = "bowel_symptoms",
                name         = "Bowel Symptoms",
                description  = "Bowel changes that may be linked to your cycle",
                iconName     = "category",
                colorToken   = "primary",
                categoryType = "default",
                values       = listOf("Normal", "Loose", "Constipated", "Painful"),
            ),
        )
    ),

    PCOS(
        id = "PCOS",
        displayName = "PCOS",
        description = "Log symptoms linked to PCOS including acne, hair growth, weight, and energy.",
        suggestedCategories = listOf(
            SuggestedCategory(
                modeKey      = "acne",
                name         = "Acne",
                description  = "Skin breakouts",
                iconName     = "face",
                colorToken   = "primary",
                categoryType = "default",
                values       = listOf("None", "Mild", "Moderate", "Severe"),
            ),
            SuggestedCategory(
                modeKey      = "hair_growth",
                name         = "Hair Growth",
                description  = "Unwanted or excess hair (hirsutism)",
                iconName     = "category",
                colorToken   = "secondary",
                categoryType = "default",
                values       = listOf("None", "Light", "Moderate", "Heavy"),
            ),
            SuggestedCategory(
                modeKey       = "weight",
                name          = "Weight",
                description   = "Body weight",
                iconName      = "monitor_weight",
                colorToken    = "tertiary",
                categoryType  = "numeric_free",
                numericMin    = 30f,
                numericMax    = 200f,
                allowDecimals = true,
                numericUnit   = "kg",
                defaultChecked = false,
            ),
            SuggestedCategory(
                modeKey      = "energy",
                name         = "Energy",
                description  = "Daily energy level (0 = exhausted, 10 = full energy)",
                iconName     = "bolt",
                colorToken   = "primary",
                categoryType = "numeric_slider",
                numericMin   = 0f,
                numericMax   = 10f,
            ),
        )
    ),

    HRT(
        id = "HRT",
        displayName = "HRT",
        description = "Monitor symptoms while on hormone replacement therapy: hot flashes, sleep, and mood.",
        suggestedCategories = listOf(
            SuggestedCategory(
                modeKey      = "hot_flashes",
                name         = "Hot Flashes",
                description  = "Frequency and intensity of hot flashes",
                iconName     = "thermostat",
                colorToken   = "primary",
                categoryType = "default",
                values       = listOf("None", "Mild", "Moderate", "Severe"),
            ),
            SuggestedCategory(
                modeKey      = "night_sweats",
                name         = "Night Sweats",
                description  = "Night sweating severity",
                iconName     = "bedtime",
                colorToken   = "secondary",
                categoryType = "default",
                values       = listOf("None", "Mild", "Severe"),
            ),
            SuggestedCategory(
                modeKey      = "sleep_quality",
                name         = "Sleep Quality",
                description  = "How well you slept (0 = very poor, 10 = excellent)",
                iconName     = "nights_stay",
                colorToken   = "tertiary",
                categoryType = "numeric_slider",
                numericMin   = 0f,
                numericMax   = 10f,
            ),
            SuggestedCategory(
                modeKey      = "mood",
                name         = "Mood",
                description  = "Overall mood (0 = very low, 10 = great)",
                iconName     = "sentiment_satisfied",
                colorToken   = "primary",
                categoryType = "numeric_slider",
                numericMin   = 0f,
                numericMax   = 10f,
            ),
        )
    ),

    PERIMENOPAUSE(
        id = "PERIMENOPAUSE",
        displayName = "Perimenopause",
        description = "Track the symptoms of perimenopause including hot flashes, brain fog, joint pain, and sleep changes.",
        suggestedCategories = listOf(
            SuggestedCategory(
                modeKey      = "hot_flashes",
                name         = "Hot Flashes",
                description  = "Frequency and intensity of hot flashes",
                iconName     = "thermostat",
                colorToken   = "primary",
                categoryType = "default",
                values       = listOf("None", "Mild", "Moderate", "Severe"),
            ),
            SuggestedCategory(
                modeKey      = "night_sweats",
                name         = "Night Sweats",
                description  = "Night sweating severity",
                iconName     = "bedtime",
                colorToken   = "secondary",
                categoryType = "default",
                values       = listOf("None", "Mild", "Severe"),
            ),
            SuggestedCategory(
                modeKey      = "brain_fog",
                name         = "Brain Fog",
                description  = "Difficulty concentrating or memory issues",
                iconName     = "psychology",
                colorToken   = "tertiary",
                categoryType = "default",
                values       = listOf("None", "Mild", "Moderate", "Severe"),
            ),
            SuggestedCategory(
                modeKey      = "joint_pain",
                name         = "Joint Pain",
                description  = "Joint or muscle discomfort",
                iconName     = "accessibility",
                colorToken   = "primary",
                categoryType = "default",
                values       = listOf("None", "Mild", "Moderate", "Severe"),
            ),
            SuggestedCategory(
                modeKey      = "sleep_quality",
                name         = "Sleep Quality",
                description  = "How well you slept (0 = very poor, 10 = excellent)",
                iconName     = "nights_stay",
                colorToken   = "secondary",
                categoryType = "numeric_slider",
                numericMin   = 0f,
                numericMax   = 10f,
            ),
        )
    ),

    HORMONE(
        id = "HORMONE",
        displayName = "Hormone Tracking",
        description = "Log hormone type, dose, and delivery method. Useful for tracking testosterone, oestrogen, progesterone, or any other hormone.",
        suggestedCategories = listOf(
            SuggestedCategory(
                modeKey      = "hormone_type",
                name         = "Hormone",
                description  = "Which hormone you took or measured",
                iconName     = "science",
                colorToken   = "primary",
                categoryType = "default",
                values       = listOf("Oestrogen", "Testosterone", "Progesterone", "Androgen blocker", "Other"),
            ),
            SuggestedCategory(
                modeKey       = "hormone_dose",
                name          = "Dose",
                description   = "Amount taken (in mg, mcg, or ml depending on your prescription)",
                iconName      = "medication",
                colorToken    = "secondary",
                categoryType  = "numeric_free",
                numericMin    = 0f,
                numericMax    = 500f,
                allowDecimals = true,
                numericUnit   = "mg",
            ),
            SuggestedCategory(
                modeKey      = "hormone_route",
                name         = "Delivery Method",
                description  = "How you took or applied the hormone",
                iconName     = "category",
                colorToken   = "tertiary",
                categoryType = "default",
                values       = listOf("Oral", "Injection", "Patch", "Gel", "Cream", "Other"),
            ),
        )
    );

    companion object {
        fun fromId(id: String): AppMode? = entries.firstOrNull { it.id == id }
    }
}

/** Parses the comma-separated activeModes string from DataStore into a set of [AppMode]. */
fun String.toActiveModeSet(): Set<AppMode> =
    split(",")
        .filter { it.isNotEmpty() }
        .mapNotNull { AppMode.fromId(it) }
        .toSet()

/** Serialises a set of [AppMode] back to the DataStore storage format. */
fun Set<AppMode>.toActiveModeString(): String =
    joinToString(",") { it.id }
