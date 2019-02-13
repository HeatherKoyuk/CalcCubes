package com.koyuk.enterprises.calculationcubes.messages

import com.koyuk.enterprises.calculationcubes.models.Answer
import com.koyuk.enterprises.calculationcubes.models.Equation
import kotlinx.coroutines.CompletableDeferred
import java.util.*

sealed class AnswersMsg
class AddToAnswers(val equations: ArrayList<Equation>) : AnswersMsg()
class GetAnswers(val response: CompletableDeferred<ArrayList<Answer>>) : AnswersMsg()
