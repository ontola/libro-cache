package tools.empathy.libro.server.health

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.css.Color
import kotlinx.css.CssBuilder
import kotlinx.css.backgroundColor
import kotlinx.css.thead
import kotlinx.css.tr
import kotlinx.html.FlowOrMetaDataContent
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.unsafe

fun humanStatus(result: CheckResult): String = when (result) {
    CheckResult.Pass -> "🟩 pass"
    CheckResult.Fail -> "🟥 fail"
    CheckResult.Warn -> "🟨 warn"
}

fun Routing.mountHealth() {
    get("/d/health") {
        val checks = listOf(
            BackendCheck(),
            EnvironmentCheck(),
            RedisCheck(),
            HeadRequestCheck(),
            BulkCheck(),
        )

        checks.forEach { it.run(call) }

        if (checks.any { it.result == CheckResult.Fail }) {
            call.response.status(HttpStatusCode.ServiceUnavailable)
        }

        call.respondHtml {
            head {
                styleCss {
                    thead {
                        backgroundColor = Color.turquoise
                    }
                    nthChild(tr.tagName) {
                        backgroundColor = Color.aliceBlue
                    }
                }
            }
            body {
                table {
                    thead {
                        tr {
                            td { +"Check" }
                            td { +"Result" }
                            td { +"Message" }
                        }
                    }
                    tbody {
                        for (check in checks) {
                            tr {
                                id = check.name.lowercase().replace(' ', '-')

                                td { +check.name }
                                td { +humanStatus(check.result) }
                                td {
                                    if (check.result == CheckResult.Pass)
                                        +"N/A"
                                    else
                                        +check.message
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun FlowOrMetaDataContent.styleCss(builder: CssBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        unsafe {
            raw(CssBuilder().apply(builder).toString())
        }
    }
}
