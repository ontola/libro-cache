package tools.empathy.libro.server.document

import kotlinx.html.BODY
import kotlinx.html.iframe
import kotlinx.html.noScript
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.unsafe
import tools.empathy.libro.webmanifest.TrackerType
import tools.empathy.libro.webmanifest.Tracking

internal fun BODY.bodyTracking(nonce: String, tracking: List<Tracking>, isUser: Boolean) {
    tracking.forEach {
        when (it.type) {
            TrackerType.GTM -> {
                noScript {
                    iframe {
                        src = "https://www.googletagmanager.com/ns.html?id=${it.containerId}"
                        height = "0"
                        width = "0"
                        style = "display:none;visibility:hidden"
                    }
                }
            }
            TrackerType.GUA -> {
                script {
                    async = true
                    this.nonce = nonce
                    src = "https://www.googletagmanager.com/gtag/js?id=${it.containerId}"
                }
                script {
                    async = true
                    this.nonce = nonce
                    unsafe {
                        raw(
                            """
                            window.dataLayer = window.dataLayer || [];
                            function gtag(){dataLayer.push(arguments);}
                            gtag('js', new Date());
                            gtag('config', "${it.containerId}");
                            """.trimIndent()
                        )
                    }
                }
            }
            TrackerType.Matomo, TrackerType.PiwikPro -> {
                it.host ?: return

                val trackerName = when (it.type) {
                    TrackerType.Matomo -> "matomo"
                    TrackerType.PiwikPro -> "ppms"
                    else -> error(Unit)
                }
                script {
                    async = true
                    this.nonce = nonce
                    unsafe {
                        raw(
                            """
                            var _paq = window._paq || [];
                            ${if (isUser) "" else "_paq.push(['disableCookies']);"}
                            _paq.push(['trackPageView']);
                            _paq.push(['enableLinkTracking']);
                            (function() {
                              var u="https://${it.host}/";
                              _paq.push(['setTrackerUrl', u+'$trackerName.php']);
                              _paq.push(['setSiteId', ${it.containerId}]);
                              var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
                              g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'$trackerName.js'; s.parentNode.insertBefore(g,s);
                            })();
                            """.trimIndent()
                        )
                    }
                }
            }
        }
    }
}
