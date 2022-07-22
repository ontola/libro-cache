package tools.empathy.studio

import io.ktor.server.application.Application
import tools.empathy.libro.server.configuration.StudioConfig
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.document.PageConfiguration
import tools.empathy.libro.server.plugins.persistentStorage

class StudioConfiguration {
    /**
     * List of path prefixes which should not be intercepted.
     */
    var blacklist: List<String> = emptyList()

    lateinit var studioConfig: StudioConfig
    lateinit var distributionRepo: DistributionRepo
    lateinit var publicationRepo: PublicationRepo
    lateinit var pageConfig: PageConfiguration

    /**
     * Circumvents a circular initialization problem where [StudioConfiguration] needs
     * access to the [Application], which in turn needs [StudioConfiguration] to be
     * initialized.
     */
    fun complete(application: Application) {
        studioConfig = application.libroConfig.studio
        distributionRepo = DistributionRepo(application.persistentStorage)
        publicationRepo = PublicationRepo(application.persistentStorage)
    }
}
