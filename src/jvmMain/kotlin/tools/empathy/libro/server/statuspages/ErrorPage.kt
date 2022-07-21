package tools.empathy.libro.server.statuspages

import io.ktor.http.HttpStatusCode
import kotlinx.css.Visibility
import kotlinx.css.visibility
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.title
import tools.empathy.libro.server.health.styleCss
import tools.empathy.serialization.Value

enum class RenderLanguage(val lexical: String) {
    EN("en"),
    NL("nl"),
    DE("de"),
}

data class LangStringSet(
    val en: String,
    val nl: String,
    val de: String,
) {
    operator fun get(language: RenderLanguage): Value.LangString {
        return when (language) {
            RenderLanguage.NL -> Value.LangString(nl, language.lexical)
            RenderLanguage.EN -> Value.LangString(en, language.lexical)
            RenderLanguage.DE -> Value.LangString(de, language.lexical)
        }
    }
}

fun titleForStatus(status: HttpStatusCode): LangStringSet = when (status) {
    HttpStatusCode.BadRequest -> LangStringSet(
        "Bad request (400 Bad Request)",
        "Fout in het verzoek (400 Bad Request)",
        "Fehlerhafte Anfrage (400 Bad Request)",
    )
    HttpStatusCode.Unauthorized -> LangStringSet(
        "Unauthorized",
        "Niet ingelogd",
        "Nicht autorisiert",
    )
    HttpStatusCode.Forbidden -> LangStringSet(
        "This item is hidden",
        "Dit item is verborgen",
        "Dieser Artikel ist versteckt",
    )
    HttpStatusCode.NotFound -> LangStringSet(
        "This item is not found",
        "Dit item is niet gevonden",
        "Dieses Element wurde nicht gefunden",
    )
    HttpStatusCode.NotAcceptable -> LangStringSet(
        "Not acceptable",
        "Dit resource kan niet worden bekeken in dit formaat.",
        "Nicht akzeptabel",
    )
    HttpStatusCode.RequestTimeout -> LangStringSet(
        "Request timeout",
        "Verzoek stilgevallen",
        "Zeitüberschreitung der Anfrage",
    )
    HttpStatusCode.Conflict -> LangStringSet(
        "Conflict",
        "Conflict",
        "Konflikt",
    )
    HttpStatusCode.Gone -> LangStringSet(
        "Gone",
        "Item verwijderd",
        "Nicht mehr vorhanden",
    )
    HttpStatusCode.PayloadTooLarge -> LangStringSet(
        "Request entity too large (413 Payload Too Large)",
        "Verzoek te groot",
        "Die angeforderte Einheit ist zu groß (413 Payload Too Large)",
    )
    HttpStatusCode.UnprocessableEntity -> LangStringSet(
        "Unprocessable Entity",
        "Het item kan niet worden verwerkt",
        "Unverarbeitbare Entität",
    )
    HttpStatusCode.TooManyRequests -> LangStringSet(
        "Too many requests",
        "Te veel verzoeken",
        "Zu viele Anfragen",
    )
    HttpStatusCode.InternalServerError -> LangStringSet(
        "Internal server error",
        "Interne serverfout",
        "Interner Serverfehler",
    )
    HttpStatusCode.NotImplemented -> LangStringSet(
        "Not implemented",
        "Deze functionaliteit is niet geïmplementeerd, probeer het later nog eens.",
        "Nicht implementiert",
    )
    HttpStatusCode.BadGateway -> LangStringSet(
        "Bad gateway",
        "Slechte doorgang",
        "Schlechtes Gateway",
    )
    HttpStatusCode.ServiceUnavailable -> LangStringSet(
        "Service unavailable",
        "Service niet beschikbaar",
        "Dienst nicht verfügbar",
    )
    HttpStatusCode.GatewayTimeout -> LangStringSet(
        "Gateway timeout",
        "Doorgang stilgevallen",
        "Gateway-Zeitüberschreitung",
    )
    else -> LangStringSet(
        "Unknown error",
        "Onbekende fout",
        "Unbekannter Fehler",
    )
}

fun bodyForStatus(status: HttpStatusCode): LangStringSet = when (status) {
    HttpStatusCode.BadRequest -> LangStringSet(
        "The request made cannot be fulfilled because it contains bad syntax, check your URL parameters or refresh the page that linked to this resource.",
        "Het verzoek bevat een fout waardoor het niet verwerkt kan worden, check de URL parameters en probeer de pagina welke naar deze verwees te herladen.",
        "Die Anfrage kann nicht erfüllt werden, da sie eine fehlerhafte Syntax enthält. Überprüfen Sie Ihre URL-Parameter oder aktualisieren Sie die Seite, die auf diese Ressource verweist.",
    )
    HttpStatusCode.Unauthorized -> LangStringSet(
        "You have to be logged in to view this resource.",
        "Je moet ingelogd zijn om dit te kunnen zien.",
        "Sie müssen eingeloggt sein, um diese Ressource zu sehen.",
    )
    HttpStatusCode.Forbidden -> LangStringSet(
        "Maybe it's visible after logging in.",
        "Mogelijk is het zichtbaar na te hebben ingelogt.",
        "Vielleicht ist es nach dem Einloggen sichtbar.",
    )
    HttpStatusCode.NotFound -> LangStringSet(
        "Maybe the item you are looking for is deleted or never existed.",
        "Misschien is het item dat je zoekt verwijderd of heeft het nooit bestaan.",
        "Vielleicht ist der gesuchte Artikel gelöscht oder hat nie existiert.",
    )
    HttpStatusCode.NotAcceptable -> LangStringSet(
        "This resource cannot be viewed in the current format.",
        "Dit resource kan niet bekeken worden in dit formaat.",
        "Diese Ressource kann im aktuellen Format nicht angezeigt werden.",
    )
    HttpStatusCode.RequestTimeout -> LangStringSet(
        "The request took too long, refresh the page or try again later.",
        "Het verzoek duurde te lang, probeer het opnieuw of later nog eens.",
        "Die Anfrage hat zu lange gedauert. Aktualisieren Sie die Seite oder versuchen Sie es später noch einmal.",
    )
    HttpStatusCode.Conflict -> LangStringSet(
        "The change could not be persisted because the resource was edited since it was opened locally.",
        "De verandering kon niet worden doorgevoerd omdat het item bewerkt is sinds deze lokaal was geopend.",
        "Die Änderung konnte nicht beibehalten werden, da die Ressource bearbeitet wurde, seit sie lokal geöffnet wurde.",
    )
    HttpStatusCode.Gone -> LangStringSet(
        "The resource has been deleted permanently.",
        "Dit item is permanent verwijderd.",
        "Die Ressource wurde dauerhaft gelöscht.",
    )
    HttpStatusCode.PayloadTooLarge -> LangStringSet(
        " Too Large)The item that you are uploading is too large. Go back and try a smaller file.",
        "Het item dat je probeert te verzenden is te groot. Ga terug en probeer een kleiner bestand.",
        "Das hochgeladene Element ist zu groß. Gehen Sie zurück und versuchen Sie es mit einer kleineren Datei.",
    )
    HttpStatusCode.UnprocessableEntity -> LangStringSet(
        "The item that you are trying to create cannot be processed.",
        "Het item dat je probeert aan te maken kan niet worden verwerkt.",
        "Das Element, das Sie zu erstellen versuchen, kann nicht verarbeitet werden.",
    )
    HttpStatusCode.TooManyRequests -> LangStringSet(
        "You're making too many request, try again in half a minute.",
        "Je maakt te veel verzoeken, probeer het over halve minuut nog eens.",
        "Sie stellen zu viele Anfragen, versuchen Sie es in einer halben Minute erneut.",
    )
    HttpStatusCode.InternalServerError -> LangStringSet(
        "An error occurred on our side, please try again later.",
        "Er ging iets aan onze kant fout, probeer het later nog eens",
        "Es ist ein Fehler auf unserer Seite aufgetreten, bitte versuchen Sie es später noch einmal.",
    )
    HttpStatusCode.NotImplemented -> LangStringSet(
        "This feature isn't implemented, please try again later.",
        "Deze functionaliteit is niet geïmplementeerd, probeer het later nog eens.",
        "Diese Funktion ist nicht implementiert, bitte versuchen Sie es später noch einmal.",
    )
    HttpStatusCode.BadGateway -> LangStringSet(
        "There was a networking issue during this request, please retry or try again later",
        "Er was een netwerkprobleem tijdens dit verzoek, probeer het opnieuw of later nog eens.",
        "Bei dieser Anfrage ist ein Netzwerkproblem aufgetreten. Bitte versuchen Sie es später noch einmal.",
    )
    HttpStatusCode.ServiceUnavailable -> LangStringSet(
        "There was a networking issue during this request, please retry or try again later",
        "Er was een netwerkprobleem tijdens dit verzoek, probeer het opnieuw of later nog eens.",
        "Bei dieser Anfrage ist ein Netzwerkproblem aufgetreten. Bitte versuchen Sie es später noch einmal.",
    )
    HttpStatusCode.GatewayTimeout -> LangStringSet(
        "There was a networking issue during this request, please retry or try again later",
        "Er was een netwerkprobleem tijdens dit verzoek, probeer het opnieuw of later nog eens.",
        "Während dieser Anfrage gab es ein Netzwerkproblem, bitte versuchen Sie es erneut oder zu einem späteren Zeitpunkt",
    )
    else -> LangStringSet(
        "An unknown error occurred, please try again later.",
        "Er is een onbekende fout opgetreden, probeer het later nog eens.",
        "Ein unbekannter Fehler ist aufgetreten, bitte versuchen Sie es später erneut.",
    )
}

fun HTML.errorPage(status: HttpStatusCode, cause: Exception, language: RenderLanguage) {
    head {
        meta(charset = "utf-8")
        title(titleForStatus(status)[language].lexical)
    }
    body {
        h1 { +titleForStatus(status)[language].lexical }
        p { +bodyForStatus(status)[language].lexical }
        span {
            styleCss {
                visibility = Visibility.hidden
            }
            +cause.javaClass.name
        }
    }
}
