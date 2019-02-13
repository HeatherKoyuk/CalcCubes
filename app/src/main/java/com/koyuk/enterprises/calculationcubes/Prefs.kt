package com.koyuk.enterprises.calculationcubes

import android.content.Context
import android.content.SharedPreferences


class Prefs(context: Context) {
    private val prefsFilename = "general_prefs"

    private val editModePref = "edit_mode_pref"
    private var editModeDefault = false
    private val numOfDicePref = "num_dice_pref"
    private val numOfDiceDefault = 3
    private val numOfSidesPref = "num_sides_pref"
    private var numOfSidesDefault = 6
    private val allowPowersPref = "allow_powers_pref"
    private var allowPowersDefault = true
    private val hideSolutionsPref = "hide_solutions_pref"
    private var hideSolutionsDefault = true
    private val solutionsCheckedPref = "solutions_checked_pref"
    private var solutionsCheckedDefault = false

    private var diePref = "die_"
    private var dieDefault = 0

    private val targetPref = "target_pref"
    private var targetDefault = 0

    // max target stuff
    private val useCustomMaxTargetPref = "use_custom_max_target"
    private var useCustomMaxTargetDefault = true
    private val manualMaxTargetPref = "manual_max_target"
    private val manualMaxTargetDefault = 144
    private var multiplyNumDicePref = "multiply_num_dice"
    private var multiplyNumDiceDefault = 2
    private var multiplyDiceSidesPref = "multiply_dice_sides"
    private var multiplyDiceSidesDefault = 12

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsFilename, 0)

    fun saveDice(die: MutableList<Die>) {
        numOfDice = die.size

        // TODO
        for (i in 0 until 5) {
            prefs.edit().remove(diePref + i).apply()
        }
        for (i in 0 until die.size) {
            setDie(die[i], i)
        }
    }

    fun allDieSet(): Boolean {
        for (i in 0 until numOfDice) {
            if (getDie(i) == 0) {
                return false
            }
        }
        return target > 0
    }

    fun getDie(i: Int): Int {
        return prefs.getInt(diePref + i, dieDefault)
    }

    private fun setDie(die: Die, i: Int) {
        prefs.edit().putInt(diePref + i, die.pip).apply()
    }

    var editMode: Boolean
        get() = prefs.getBoolean(editModePref, editModeDefault)
        set(value) = prefs.edit().putBoolean(editModePref, value).apply()

    var numOfDice: Int
        get() = prefs.getInt(numOfDicePref, numOfDiceDefault)
        // TODO: This is fragile with setting the dice dependant on this then using the die again here
        set(value) = prefs.edit().putInt(numOfDicePref, value).apply()

    var numOfSides: Int
        get() = prefs.getInt(numOfSidesPref, numOfSidesDefault)
        set(value) = prefs.edit().putInt(numOfSidesPref, value).apply()

    var manualMaxTarget: Int
        get() = prefs.getInt(manualMaxTargetPref, manualMaxTargetDefault)
        set(value) = prefs.edit().putInt(manualMaxTargetPref, value).apply()

    var allowPowers: Boolean
        get() = prefs.getBoolean(allowPowersPref, allowPowersDefault)
        set(value) = prefs.edit().putBoolean(allowPowersPref, value).apply()

    var hideSolutions: Boolean
        get() = prefs.getBoolean(hideSolutionsPref, hideSolutionsDefault)
        set(value) = prefs.edit().putBoolean(hideSolutionsPref, value).apply()

    var solutionsChecked: Boolean
        get() = !hideSolutions && prefs.getBoolean(solutionsCheckedPref, solutionsCheckedDefault)
        set(value) = prefs.edit().putBoolean(solutionsCheckedPref, !hideSolutions && value).apply()

    var useCustomMaxTarget: Boolean
        get() = prefs.getBoolean(useCustomMaxTargetPref, useCustomMaxTargetDefault)
        set(value) = prefs.edit().putBoolean(useCustomMaxTargetPref, value).apply()

    var multiplyNumDice: Int
        get() = prefs.getInt(multiplyNumDicePref, multiplyNumDiceDefault)
        set(value) = prefs.edit().putInt(multiplyNumDicePref, value).apply()

    var multiplyDiceSides: Int
        get() = prefs.getInt(multiplyDiceSidesPref, multiplyDiceSidesDefault)
        set(value) = prefs.edit().putInt(multiplyDiceSidesPref, value).apply()

    var target: Int
        get() = prefs.getInt(targetPref, targetDefault)
        set(value) = prefs.edit().putInt(targetPref, value).apply()
}