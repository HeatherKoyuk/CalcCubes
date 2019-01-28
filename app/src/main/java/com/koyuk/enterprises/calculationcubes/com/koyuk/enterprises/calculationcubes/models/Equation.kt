package com.koyuk.enterprises.calculationcubes.com.koyuk.enterprises.calculationcubes.models

import java.util.ArrayList
import kotlin.math.pow

sealed class Equation: Comparable<Equation> {

    object GlobalVariable {
        @Volatile
        var lookup: MutableMap<String, Equation> = mutableMapOf()
        var combinations: MutableMap<String, List<IntArray>> = mutableMapOf()
    }
    abstract val operator: String
    var equationString: String = ""
    var solution: Double = 0.0
    var isAllPlus: Boolean = false
    var isAllTimes: Boolean = false
    var allList: ArrayList<Equation> = arrayListOf()

    constructor(a: Int) {
        equationString = a.toString()
        solution = a.toDouble()
        isAllPlus = true
        isAllTimes = true
        allList.add(this)
    }

    constructor(a: Equation, b: Equation, c: String){
        equationString = c
        solution = this.computeSolution(a, b)
    }

    constructor(a: Equation, b: Equation, c: String, list: ArrayList<Equation>){
        equationString = c
        solution = this.computeSolution(a, b)
        this.allList = list
    }

    protected abstract fun computeSolution(a: Equation, b: Equation) : Double

    companion object {
        fun computeText(a: Equation, b: Equation, thisOperator: String): String {
            return if (a is SimpleNumber) {
                if (b is SimpleNumber) {
                    a.equationString + thisOperator + b.equationString
                } else {
                    a.equationString + thisOperator + "(" + b.equationString + ")"
                }
            } else {
                if (b is SimpleNumber) {
                    "(" + a.equationString + ")" + thisOperator + b.equationString
                } else {
                    "(" + a.equationString + ")" + thisOperator + "(" + b.equationString + ")"
                }
            }
        }
    }
    override operator fun compareTo(other: Equation): Int {
        return equationString.compareTo(other.equationString)
    }
}

data class SimpleNumber(val a: Int) : Equation(a){
    override val operator: String
        get() =  ""
    // this never actually gets called so fake it
    override fun computeSolution(a: Equation, b: Equation) : Double {
        return solution
    }
}

data class Add(val a: Equation, val b: Equation,val c: String, val list: ArrayList<Equation>) : Equation(a, b, c, list) {
    override val operator: String
        get() =  "+"
    init {
        this.isAllPlus = true
    }

    override fun computeSolution(a: Equation, b: Equation) : Double {
        return a.solution + b.solution
    }

    companion object {

        fun getOrCreate(a: Equation, b: Equation): Equation {
            var list: ArrayList<Equation> = arrayListOf()
            if (a.isAllPlus) {
                list.addAll(a.allList)
            } else {
                list.add(a)
            }
            if (b.isAllPlus) {
                list.addAll(b.allList)
            } else {
                list.add(b)
            }
            list.sort()
            var equationString = list[0].equationString
            if (!list[0].isAllPlus) {
                equationString = "($equationString)"
            }
            for (i in 1 until list.size) {
                equationString = "$equationString+"
                equationString += if (list[i].isAllPlus) {
                    list[i].equationString
                } else {
                    "(" + list[i].equationString + ")"
                }
            }
            val eq = GlobalVariable.lookup[equationString]
            if (eq != null) {
                return eq as Add
            }
            var add = Add(a, b, equationString, list)
            try {
                GlobalVariable.lookup[equationString] = add
            }
            catch(e: Exception){}
            return add
        }
    }
}

data class Subtract(val a: Equation, val b: Equation, val c: String) : Equation(a, b, c) {
    override val operator: String
        get() =  "-"

    override fun computeSolution(a: Equation, b: Equation) : Double {
        return a.solution - b.solution
    }
    companion object {
        private fun computeText(a: Equation, b: Equation): String {
            return Equation.computeText(a, b, "-")
        }

        fun getOrCreate(a: Equation, b: Equation): Equation {
            val equationString = Subtract.computeText(a, b)
            val eq = GlobalVariable.lookup[equationString]
            if (eq != null) {
                return eq as Subtract
            }
            var sub = Subtract(a, b, equationString)
            try {
                GlobalVariable.lookup[equationString] = sub
            }
            catch(e: Exception){}
            return sub
        }
    }
}

data class Multiply(val a: Equation, val b: Equation, val c: String, val list: ArrayList<Equation>) : Equation(a, b, c, list) {
    override val operator: String
        get() =  "*"

    init {
        this.isAllTimes = true
    }

    override fun computeSolution(a: Equation, b: Equation) : Double {
        return a.solution * b.solution
    }
    companion object {
        fun getOrCreate(a: Equation, b: Equation): Equation {
            var list: ArrayList<Equation> = arrayListOf()
            if (a.isAllTimes) {
                list.addAll(a.allList)
            } else {
                list.add(a)
            }
            if (b.isAllTimes) {
                list.addAll(b.allList)
            } else {
                list.add(b)
            }
            list.sort()
            var equationString = list[0].equationString
            if (!list[0].isAllTimes) {
                equationString = "($equationString)"
            }
            for (i in 1 until list.size) {
                equationString = "$equationString*"
                equationString += if (list[i].isAllTimes) {
                    list[i].equationString
                } else {
                    "(" + list[i].equationString + ")"
                }
            }
            val eq = GlobalVariable.lookup.get(equationString)
            if (eq != null) {
                return eq as Multiply
            }
            var mult = Multiply(a, b, equationString, list)
            try {
                GlobalVariable.lookup[equationString] = mult
            }
            catch(e: Exception){}
            return mult
        }
    }
}

data class Divide(val a: Equation, val b: Equation, val c: String) : Equation(a, b, c) {
    override val operator: String
        get() =  "/"

    override fun computeSolution(a: Equation, b: Equation) : Double {
        return a.solution / b.solution
    }

    companion object {
        private fun computeText(a: Equation, b: Equation): String {
            return Equation.computeText(a, b, "/")
        }

        fun getOrCreate(a: Equation, b: Equation): Equation {
            val equationString = Divide.computeText(a, b)
            val eq = GlobalVariable.lookup[equationString]
            if (eq != null) {
                return eq as Divide
            }
            var div = Divide(a, b, equationString)
            try {
                GlobalVariable.lookup[equationString] = div
            }
            catch(e: Exception){}
            return div
        }
    }
}

data class PowerOf(val a: Equation, val b: Equation, val c: String) : Equation(a, b, c) {
    override val operator: String
        get() =  "^"

    override fun computeSolution(a: Equation, b: Equation) : Double {
        return a.solution.pow(b.solution)
    }

    companion object {
        private fun computeText(a: Equation, b: Equation): String {
            return if (a is SimpleNumber) {
                if (b is SimpleNumber) {
                    a.equationString + "<sup>" + b.equationString + "</sup>"
                } else {
                    a.equationString + "<sup>" + "(" + b.equationString + ")" + "</sup>"
                }
            } else {
                if (b is SimpleNumber) {
                    "(" + a.equationString + ")" + "<sup>" + b.equationString + "</sup>"
                } else {
                    "(" + a.equationString + ")" + "<sup>" + "(" + b.equationString + ")" + "</sup>"
                }
            }
        }

        fun getOrCreate(a: Equation, b: Equation): Equation {
            val equationString = PowerOf.computeText(a, b)
            val eq = GlobalVariable.lookup.get(equationString)
            if (eq != null) {
                return eq
            }
            var pow = PowerOf(a, b, equationString)
            try {
                GlobalVariable.lookup[equationString] = pow
            }
            catch(e: Exception){}
            return pow
        }
    }
}

data class RootOf(val a: Equation, val b: Equation, val c: String) : Equation(a, b, c) {
    override val operator: String
        get() =  "v"

    override fun computeSolution(a: Equation, b: Equation) : Double {
        return a.solution.pow(1 / b.solution)
    }

    companion object {
        private fun computeText(a: Equation, b: Equation): String {
            return if (a is SimpleNumber) {
                if (b is SimpleNumber) {
                    a.equationString + "<sup>1/" + b.equationString + "</sup>"
                } else {
                    a.equationString + "<sup>1/" + "(" + b.equationString + ")" + "</sup>"
                }
            } else {
                if (b is SimpleNumber) {
                    "(" + a.equationString + ")" + "<sup>1/" + b.equationString + "</sup>"
                } else {
                    "(" + a.equationString + ")" + "<sup>1/" + "(" + b.equationString + ")" + "</sup>"
                }
            }
        }

        fun getOrCreate(a: Equation, b: Equation): Equation {
            val equationString = RootOf.computeText(a, b)
            val eq = GlobalVariable.lookup[equationString]
            if (eq != null) {
                return eq
            }
            var root = RootOf(a, b, equationString)
            try {
                GlobalVariable.lookup[equationString] = root
            }
            catch(e: Exception){}
            return root
        }
    }
}
