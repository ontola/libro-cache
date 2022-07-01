package tools.empathy.vocabularies

import io.ontola.empathy.web.Value

object Ontola : Vocab {
    override val vocab: String = "https://ns.ontola.io/core#"

    val Banner by Term()
    val Collection by Term()
    val CollectionFilterClass = Value.Id.Global("${vocab}CollectionFilter")
    val CollectionSortingClass = Value.Id.Global("${vocab}CollectionSorting")
    val CollectionView by Term()
    val Condition by Term()
    val Confirmation by Term()
    val ConfirmedUser by Term()
    val CreateAuthAccessToken = Value.Id.Global("${vocab}Create::Auth::AccessToken")
    val CreateAuthConfirmation = Value.Id.Global("${vocab}Create::Auth::Confirmation")
    val CreateAuthPassword = Value.Id.Global("${vocab}Create::Auth::Password")
    val CreateAuthSession = Value.Id.Global("${vocab}Create::Auth::Session")
    val CreateAuthUnlock = Value.Id.Global("${vocab}Create::Auth::Unlock")
    val CreateFollowUp = Value.Id.Global("${vocab}Create::FollowUp")
    val CreateMediaObject = Value.Id.Global("${vocab}Create::MediaObject")
    val CreateOpinion = Value.Id.Global("${vocab}Create::Opinion")
    val CreateUser = Value.Id.Global("${vocab}Create::User")
    val CreateVote = Value.Id.Global("${vocab}Create::Vote")
    val CreateVoteAction by Term()
    val DestroyVoteAction by Term()
    val DisabledActionStatus by Term()
    val ExpiredActionStatus by Term()
    val Filter by Term()
    val FilterField by Term()
    val FilterOption by Term()
    val FormOption by Term()
    val FormStep by Term()
    val GuestUser by Term()
    val InfiniteView by Term()
    val LottieAnimation by Term()
    val MenuItem by Term()
    val Ontology by Term()
    val PaginatedView by Term()
    val PictureSet by Term()
    val PropertyQuery by Term()
    val SearchResult by Term()
    val Sorting by Term()
    val UnconfirmedUser by Term()
    val VideoSet by Term()
    val Widget by Term()
    val destroy = Value.Id.Global("${vocab}_destroy")
    val action by Term()
    val actionDialog by Term()
    val actionsMenu by Term()
    val activeFilters by Term()
    val actor by Term()
    val actorType by Term()
    val alt by Term()
    val ariaLabel by Term()
    val baseCollection by Term()
    val breadcrumb by Term()
    val callToAction by Term()
    val claimRewardAction by Term()
    val collectionDisplay by Term()
    val collectionDisplayCard = Value.Id.Global("${vocab}collectionDisplay/card")
    val collectionDisplayDefault = Value.Id.Global("${vocab}collectionDisplay/default")
    val collectionDisplayGrid = Value.Id.Global("${vocab}collectionDisplay/grid")
    val collectionDisplaySettingsTable = Value.Id.Global("${vocab}collectionDisplay/settingsTable")
    val collectionDisplayTable = Value.Id.Global("${vocab}collectionDisplay/table")
    val collectionFilter by Term()
    val collectionFrame by Term()
    val collectionSorting by Term()
    val collectionType by Term()
    val collectionTypeInfinite = Value.Id.Global("${vocab}collectionType/infinite")
    val collectionTypePaginated = Value.Id.Global("${vocab}collectionType/paginated")
    val columns by Term()
    val contains by Term()
    val coverPhoto by Term()
    val createAction by Term()
    val createSubmissionAction by Term()
    val defaultPagination by Term()
    val defaultType by Term()
    val destroyAction by Term()
    val dismissAction by Term()
    val dismissButton by Term()
    val dismissedAt by Term()
    val fail by Term()
    val favoriteAction by Term()
    val filterCount by Term()
    val filterFields by Term()
    val filterKey by Term()
    val filterOptions by Term()
    val filterOptionsIn by Term()
    val filterValue by Term()
    val followMenu by Term()
    val formSteps by Term()
    val formatApng = Value.Id.Global("${vocab}format/apng")
    val formatAvif = Value.Id.Global("${vocab}format/avif")
    val formatGif = Value.Id.Global("${vocab}format/gif")
    val formatJpg = Value.Id.Global("${vocab}format/jpg")
    val formatMov = Value.Id.Global("${vocab}format/mov")
    val formatMp4 = Value.Id.Global("${vocab}format/mp4")
    val formatPng = Value.Id.Global("${vocab}format/png")
    val formatSvg = Value.Id.Global("${vocab}format/svg")
    val formatWebm = Value.Id.Global("${vocab}format/webm")
    val formatWebp = Value.Id.Global("${vocab}format/webp")
    val formsInputsSelectDisplayProp = Value.Id.Global("${vocab}forms/inputs/select/displayProp")
    val geometryType by Term()
    val groupBy by Term()
    val header by Term()
    val hideHeader by Term()
    val href by Term()
    val imagePositionY by Term()
    val imgUrl64x64 by Term()
    val imgUrl256x256 by Term()
    val imgUrl568x400 by Term()
    val imgUrl1500x2000 by Term()
    val infinitePagination by Term()
    val invalidate by Term()
    val iriTemplate by Term()
    val iriTemplateOpts by Term()
    val makePrimaryAction by Term()
    val maxColumns = Value.Id.Global("${vocab}grid/maxColumns")
    val maxCount by Term()
    val maxInclusive by Term()
    val maxInclusiveLabel by Term()
    val memberships by Term()
    val menuItems by Term()
    val menus by Term()
    val minCount by Term()
    val minInclusive by Term()
    val minInclusiveLabel by Term()
    val minLength by Term()
    val mountAction by Term()
    val moveDownAction by Term()
    val moveUpAction by Term()
    val navigationsMenu by Term()
    val oneClick by Term()
    val organization by Term()
    val pages by Term()
    val parentMenu by Term()
    val pass by Term()
    val password by Term()
    val pluralLabel by Term()
    val profileMenu by Term()
    val publishAction by Term()
    val query by Term()
    val readAction by Term()
    val redirectUrl by Term()
    val relevance by Term()
    val remove by Term()
    val replace by Term()
    val resource by Term()
    val sendConfirmationAction by Term()
    val settingsMenu by Term()
    val shIn by Term()
    val shareMenu by Term()
    val signUpAction by Term()
    val sortDirection by Term()
    val sortKey by Term()
    val sortOptions by Term()
    val startedAction by Term()
    val submitAction by Term()
    val svg by Term()
    val tabsMenu by Term()
    val topology by Term()
    val trashAction by Term()
    val updateAction by Term()
    val view by Term()
    val visible by Term()
    val void by Term()
    val widgetResource by Term()
    val widgetSize by Term()
    val widgets by Term()
    val wrapper by Term()
    val zoomLevel by Term()
}
