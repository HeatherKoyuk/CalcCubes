package com.koyuk.enterprises.calculationcubes.com.koyuk.enterprises.calculationcubes.models

import kotlin.math.roundToInt

class Answer {
    var display: String = ""
    var absoluteDiff: Double = 0.0
    var absoluteDiffDisplay: String = ""
    var plusMinus: Int = -1

    constructor(eq: Equation, target: Int) {
        // not sure if it's worth this to try to filter out duplicates first, or not
        //equation = eq.equation
        display = display(eq)
        var diff = eq.solution - target
        absoluteDiff = Math.abs(diff)
        if (diff != 0.0) {
            absoluteDiffDisplay = tryRoundSolution(absoluteDiff)
            if (absoluteDiff < 0.01) {
                plusMinus = 0
            } else if (diff > 0) {
                plusMinus = 1
            }
        } else {
            absoluteDiffDisplay = "0"
            plusMinus = 0
        }
    }

    fun display(eq: Equation) = eq.equationString + " = " + tryRoundSolution(eq.solution)

    private fun tryRoundSolution(sol: Double): String {
        if (sol.isNaN() || sol.isInfinite()) {
            return sol.toString()
        }
        var rounded = sol.roundToInt()
        if (sol - rounded != 0.0) {
            return "%.2f".format(sol)
        }
        return rounded.toString()
    }
}