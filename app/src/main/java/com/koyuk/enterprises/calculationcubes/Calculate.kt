package com.koyuk.enterprises.calculationcubes

import com.koyuk.enterprises.calculationcubes.com.koyuk.enterprises.calculationcubes.models.*
import com.koyuk.enterprises.calculationcubes.messages.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.util.*
import java.util.concurrent.Executors

class Calculate(private val answerListMaxSize: Int, private val allowPowers: Boolean, private val target: Int)
{

    val threadPool = Executors.newCachedThreadPool().asCoroutineDispatcher()

    public suspend fun computeAnswers(equations: ArrayList<Equation>) : ArrayList<Answer> = coroutineScope  {
        val actor = answersActor() // create the actor
        withContext(Dispatchers.Default) { compute(equations, actor, true) }
        val response = CompletableDeferred<ArrayList<Answer>>()
        actor.send(GetAnswers(response))
        actor.close()
        response.await()
    }

    private suspend fun compute(equations: ArrayList<Equation>, answerActor: SendChannel<AnswersMsg>, done: Boolean = false) : ArrayList<Equation> {
        // TODO: Cancel
       // if (!isActive)
        //    return arrayListOf(Equation)
        if (equations.size == 1) {
            return equations
        } else if (equations.size == 2) {
            try {
                var results = computeTwo(equations.elementAt(0), equations.elementAt(1));
                return if (done) {
                    withContext(threadPool) {
                        answerActor.send(AddToAnswers(results))
                    }
                    arrayListOf()
                } else{
                    results
                }
            }
            catch(e: Exception){
                var ii = e.message
                return arrayListOf()
            }
        }

        var half = equations.size / 2 + 1
        // to pull the results from the equationsActor
        var results: ArrayList<Equation> = arrayListOf()

        withContext(threadPool){
            val equationsActor = equationsActor() // create the actor
            val jobsActor = jobsActor() // create the actor
            //println(half)

            for (index in 1 until half) {
                var c = Combinations(index, equations.size)
                // if we're computing (2,4) then we get back {[0,1], [0,2], [0,3], [1,2], [1,3], [2,3]) and the second half of that list will be computed automatically below
                var upTo = if (equations.size % 2 == 0 && index == equations.size / 2) {
                    c.combos.size / 2
                } else {
                    c.combos.size
                }
                // kick off new coroutine for the part that actually does the work so they can all run in parallel
                for (i in 0 until upTo) {
                    var list = c.combos[i]
                    jobsActor.send(AddToJobs(doTheWork(list, equations, answerActor, equationsActor, done)))
                }
            }

            val jobResponse = CompletableDeferred<ArrayList<Job>>()
            jobsActor.send(GetJobs(jobResponse))
            jobsActor.close()
            val jobs = jobResponse.await()
            jobs.forEach{ it.join() }

            val response = CompletableDeferred<ArrayList<Equation>>()
            equationsActor.send(GetEquations(response))
            equationsActor.close()
            results = response.await()
        }
        return if (done) {
            arrayListOf()
        } else{
            results
        }
    }

    private fun doTheWork(list: IntArray, equations: ArrayList<Equation>, answerActor: SendChannel<AnswersMsg>,
                                  equationsActor: SendChannel<EquationsMsg>, done: Boolean) : Job {
        return GlobalScope.launch {
            try {
                var first: ArrayList<Equation> = arrayListOf()
                var second: ArrayList<Equation> = arrayListOf()
                for (j in 0 until equations.size) {
                    if (list.contains(j)) {
                        first.add(equations[j])
                    } else {
                        second.add(equations[j])
                    }
                }
                var firstList = withContext(threadPool) { compute(first, answerActor) }
                var secondList = withContext(threadPool) { compute(second, answerActor) }

                for (firstEquation in firstList) {
                    for (secondEquation in secondList) {
                        try {
                            var subResults = computeTwo(firstEquation, secondEquation)
                            if (done) {
                                answerActor.send(AddToAnswers(subResults))
                            } else {
                                equationsActor.send(AddToEquations(subResults))
                            }
                        } catch (e: Exception) {
                            var eeeee = e.message
                        }
                    }
                }
            } catch (e: Exception) {
                var iis = e.message
            }
        }
    }

    private fun computeTwo(equation0: Equation, equation1: Equation): ArrayList<Equation> {
        var results = ArrayList<Equation>(10)

        results.add(Add.getOrCreate(equation0, equation1))
        results.add(Subtract.getOrCreate(equation0, equation1))
        results.add(Multiply.getOrCreate(equation0, equation1))
        results.add(Divide.getOrCreate(equation0, equation1))
        if (allowPowers) {
            results.add(PowerOf.getOrCreate(equation0, equation1))
            results.add(RootOf.getOrCreate(equation0, equation1))
        }

        if (equation0.equationString != equation1.equationString) {
            results.add(Subtract.getOrCreate(equation0, equation1))
            results.add(Divide.getOrCreate(equation0, equation1))
            if (allowPowers) {
                results.add(PowerOf.getOrCreate(equation0, equation1))
                results.add(RootOf.getOrCreate(equation0, equation1))
            }
        }
        return ArrayList(results.filter {
            it.solution != java.lang.Double.POSITIVE_INFINITY && it.solution != Double.NEGATIVE_INFINITY
                    && it.solution != java.lang.Double.NaN
        }.distinctBy { it.equationString })
    }

    private fun CoroutineScope.answersActor() = actor<AnswersMsg> {
        var answers: ArrayList<Answer> = arrayListOf()
        var baseline = Double.MAX_VALUE

        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is AddToAnswers -> {
                    answers = addToList(answers, baseline, msg.equations)
                    if (answers.size == answerListMaxSize) {
                        baseline = answers[answerListMaxSize - 1].absoluteDiff
                    }
                }
                is GetAnswers -> msg.response.complete(answers)
            }
        }
    }
    // assuming that sorting is more expensive than calculating extra values, we are waiting and collecting lists before
    // trying to add them to the answers list (because the sorting, filtering etc can then be done in-place)
    private fun addToList(answers: ArrayList<Answer>, baseline: Double, list: ArrayList<Equation>): ArrayList<Answer> {
        // no reason to add anything to the list that is farther away than the current baseline
        // There are still going to be some duplicates, it.equation vs. answers.display -
        // Not sure that can be helped
        val cutoffList = list.map { Answer(it, target) }.filter { it.absoluteDiff <= baseline }
        when {
            cutoffList.isEmpty() -> return answers
            cutoffList.size == 1 -> answers.add(cutoffList[0])
            else -> answers.addAll(cutoffList)
        }
        // take distinct, sort by diff, and then only take the top 500
        var tryList = answers.distinctBy { it.display }.sortedWith(compareBy { it.absoluteDiff }).take(answerListMaxSize)

        if(tryList.isEmpty()){
            return arrayListOf()
        }
        return tryList as ArrayList<Answer>
    }

    private fun CoroutineScope.equationsActor() = actor<EquationsMsg> {
        var equations: ArrayList<Equation> = arrayListOf()

        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is AddToEquations -> {
                    equations.addAll(msg.equations)
                }
                is GetEquations -> msg.response.complete(equations)
            }
        }
    }

    private fun CoroutineScope.jobsActor() = actor<JobsMsg> {
        var jobs: ArrayList<Job> = arrayListOf()

        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is AddToJobs -> {
                    jobs.add(msg.job)
                    var size = jobs.size
                }
                is GetJobs -> msg.response.complete(jobs)
            }
        }
    }
}