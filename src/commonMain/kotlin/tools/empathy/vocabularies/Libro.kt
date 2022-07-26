package tools.empathy.vocabularies

object Libro : Vocab {
    override val vocab: String = "https://ns.ontola.io/libro/"

    /** The result of the health check */
    val result by Term()

    /** The message of the health check */
    val message by Term()
    val target by Term()

    object Actions : Vocab {
        override val vocab = Libro.vocab

        val copyToClipboard by Term("actions/copyToClipboard")
        val expireSession by Term("actions/expireSession")
        val logout by Term("actions/logout")
        val playAudio by Term("actions/playAudio")
        val redirect by Term("actions/redirect")
        val refresh by Term("actions/refresh")
        val reload by Term("actions/reload")
        val search by Term("actions/search")

        object Dialog : Vocab {
            override val vocab = Libro.vocab

            val alert by Term("actions/dialog/alert")
            val close by Term("actions/dialog/close")
        }

        object Navigation : Vocab {
            override val vocab = Libro.vocab

            val pop by Term("acions/navigation/pop")
            val push by Term("acions/navigation/push")
        }
    }

    object Boostrap : Vocab {
        override val vocab = Libro.vocab

        val Browser by Term("bootstrap/Browser")
        val TopologiesList by Term("bootstrap/TopologiesList")
        val ModulesList by Term("bootstrap/ModulesList")
    }

    object Module : Vocab {
        override val vocab = Libro.vocab

        val topologiesCount by Term("module/topologiesCount")
        val type by Term("module/type")
        val viewsCount by Term("module/viewsCount")
    }

    object Targets : Vocab {
        override val vocab = Libro.vocab

        val blank by Term("_blank")
        val parent by Term("_parent")
        val self by Term("_self")
        val top by Term("_top")
        val modal by Term("modal")
    }

    object Topologies : Vocab {
        override val vocab = Libro.vocab

        val actionsBar by Term("topologies/actionsBar")
        val appMenu by Term("topologies/appMenu")
        val attributeList by Term("topologies/attributeList")
        val card by Term("topologies/card")
        val cardAppendix by Term("topologies/cardAppendix")
        val cardFixed by Term("topologies/cardFixed")
        val cardFloat by Term("topologies/cardFloat")
        val cardMain by Term("topologies/cardMain")
        val cardMicroRow by Term("topologies/cardMicroRow")
        val cardRow by Term("topologies/cardRow")
        val container by Term("topologies/container")
        val containerFloat by Term("topologies/containerFloat")
        val containerHeader by Term("topologies/containerHeader")
        val contentDetails by Term("topologies/contentDetails")
        val detail by Term("topologies/detail")
        val flow by Term("topologies/flow")
        val footer by Term("topologies/footer")
        val fullResource by Term("topologies/fullResource")
        val grid by Term("topologies/grid")
        val hoverBox by Term("topologies/hoverBox")
        val inline by Term("topologies/inline")
        val list by Term("topologies/list")
        val mainBody by Term("topologies/mainBody")
        val menu by Term("topologies/menu")
        val navbar by Term("topologies/navbar")
        val page by Term("topologies/page")
        val pageHeader by Term("topologies/pageHeader")
        val parent by Term("topologies/parent")
        val sideBarTopology by Term("topologies/sideBarTopology")
        val tabBar by Term("topologies/tabBar")
        val tabPane by Term("topologies/tabPane")
    }
}
