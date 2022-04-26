package io.ontola.cache.health

import io.ktor.server.application.ApplicationCall

abstract class Check {
    lateinit var name: String
    lateinit var result: CheckResult
    lateinit var message: String
    lateinit var error: Exception
    lateinit var debug: String

    suspend fun run(call: ApplicationCall) {
        try {
            val output = this.runTest(call)

            if (output is Exception) {
                fail(output)
            } else {
                result = CheckResult.Pass
            }
        } catch (e: Exception) {
            fail(e)
        }
    }

    private fun fail(e: Exception) {
        result = if (e is Warning) CheckResult.Warn else CheckResult.Fail
        error = e
        message = error.message!!
    }

    abstract suspend fun runTest(call: ApplicationCall): Exception?
}
