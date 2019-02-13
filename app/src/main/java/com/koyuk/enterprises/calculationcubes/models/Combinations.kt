package com.koyuk.enterprises.calculationcubes.models


// TODO: return method from class not init new class every time
class Combinations(private val m: Int, private val n: Int) {
    var combos: MutableList<IntArray> = mutableListOf()
    private val combination = IntArray(m)
    private val key = "$m,$n"

    object GlobalVariable {
        var combinations: MutableMap<String, List<IntArray>> = mutableMapOf()
    }

    init {
        if (GlobalVariable.combinations.containsKey(key)) {
            combos = GlobalVariable.combinations[key] as MutableList<IntArray>
        } else {
            generate(0)
        }
    }

    private fun generate(k: Int) {
        if (k >= m) {
            val copy = IntArray(m)
            for (i in 0 until m) {
                copy[i] = combination[i]
            }
            combos.add(copy)
        } else {
            for (j in 0 until n) {
                if (k == 0 || j > combination[k - 1]) {
                    combination[k] = j
                    generate(k + 1)
                }
            }
        }
    }
}