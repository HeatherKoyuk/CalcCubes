package com.koyuk.enterprises.calculationcubes.messages

import com.koyuk.enterprises.calculationcubes.models.Equation
import kotlinx.coroutines.CompletableDeferred
import java.util.*

sealed class EquationsMsg
class AddToEquations(val equations: ArrayList<Equation>) : EquationsMsg()
class GetEquations(val response: CompletableDeferred<ArrayList<Equation>>) : EquationsMsg()
