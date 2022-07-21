package tools.empathy.studio

import io.ktor.server.application.Application
import tools.empathy.libro.server.document.PageConfiguration
import tools.empathy.libro.server.plugins.StudioConfig
import tools.empathy.libro.server.plugins.cacheConfig
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

    fun complete(application: Application) {
        studioConfig = application.cacheConfig.studio
        distributionRepo = DistributionRepo(application.persistentStorage)
        publicationRepo = PublicationRepo(application.persistentStorage)
    }
}
