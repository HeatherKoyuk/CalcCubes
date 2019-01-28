package com.koyuk.enterprises.calculationcubes

import android.content.Context
import android.content.SharedPreferences

class Prefs (context: Context) {
    private val prefsFilename = "general_prefs"
    private val numOfDicePref = "num_dice_pref"
    private  val numOfDiceDefault = 3
    private val numOfSidesPref = "num_sides_pref"
    private var numOfSidesDefault = 6
    private val targetMaxPref = "target_max_pref"
    private var targetMaxDefault = 144
    private val allowPowersPref = "allow_powers_pref"
    private var allowPowersDefault = true
    private val hideSolutionsPref = "hide_solutions_pref"
    private var hideSolutionsDefault = true
    private val solutionsCheckedPref = "solutions_checked_pref"
    private var solutionsCheckedDefault = false

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsFilename, 0)

    var numOfDice: Int
        get() = prefs.getInt(numOfDicePref, numOfDiceDefault)
        set(value) = prefs.edit().putInt(numOfDicePref, value).apply()

    var numOfSides: Int
        get() = prefs.getInt(numOfSidesPref, numOfSidesDefault)
        set(value) = prefs.edit().putInt(numOfSidesPref, value).apply()

    var targetMax: Int
        get() = prefs.getInt(targetMaxPref, targetMaxDefault)
        set(value) = prefs.edit().putInt(targetMaxPref, value).apply()

    var allowPowers: Boolean
        get() = prefs.getBoolean(allowPowersPref, allowPowersDefault)
        set(value) = prefs.edit().putBoolean(allowPowersPref, value).apply()

    var hideSolutions: Boolean
        get() = prefs.getBoolean(hideSolutionsPref, hideSolutionsDefault)
        set(value) = prefs.edit().putBoolean(hideSolutionsPref, value).apply()

    var solutionsChecked: Boolean
        get() = !hideSolutions && prefs.getBoolean(solutionsCheckedPref, solutionsCheckedDefault)
        set(value) = prefs.edit().putBoolean(solutionsCheckedPref, !hideSolutions && value).apply()
}