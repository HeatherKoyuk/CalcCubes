package com.koyuk.enterprises.calculationcubes.models

import kotlin.math.roundToInt

class Answer// not sure if it's worth this to try to filter out duplicates first, or not
//equation = eq.equation
(eq: Equation, target: Int) {
    var display: String = ""
    var absoluteDiff: Double = 0.0
    var absoluteDiffDisplay: String = ""
    var plusMinus: Int = -1

    init {
        display = display(eq)
        val diff = eq.solution - target
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

    private fun display(eq: Equation) = eq.equationString + " = " + tryRoundSolution(eq.solution)

    private fun tryRoundSolution(sol: Double): String {
        if (sol.isNaN() || sol.isInfinite()) {
            return sol.toString()
        }
        val rounded = sol.roundToInt()
        if (sol - rounded != 0.0) {
            return "%.2f".format(sol)
        }
        return rounded.toString()
    }
}