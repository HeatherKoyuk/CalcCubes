package com.koyuk.enterprises.calculationcubes.messages

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import java.util.*

sealed class JobsMsg
class AddToJobs(val job: Job) : JobsMsg()
class GetJobs(val response: CompletableDeferred<ArrayList<Job>>) : JobsMsg()
