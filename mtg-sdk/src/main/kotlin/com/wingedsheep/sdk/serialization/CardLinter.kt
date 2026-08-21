package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.StaticAbility
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Structural lint for card definitions (sdk-analysis §1.1): catches the "silent no-op" bug class
 * where a reference doesn't resolve to anything, so the ability compiles and serializes cleanly
 * while doing nothing at all. Two flavors:
 *
 * - **Name-based** — a pipeline collection, a stored number, a chosen value, a cast-time target
 *   binding, a [com.wingedsheep.sdk.scripting.ChoiceSlot] that misses because of a typo, a missing
 *   writer, or a reference into the wrong ability's scope.
 * - **Shape-based** — a reference that can't resolve given what the card *is*: a
 *   `TargetChooser.Opponent` outside an activated ability ([checkOpponentChoosers]), an
 *   `EntityMatches` role the evaluator doesn't dispatch, or an attach-scope filter on a card that
 *   can never be attached ([checkAttachedScope]).
 *
 * ## How it works
 *
 * The card is serialized to its JSON tree (the same machinery as the snapshot test) and the lint
 * walks that tree, so every effect container — composites, gates, modes, granted abilities, class
 * levels, saga chapters, card faces — is covered automatically, including containers added after
 * this linter was written. Two registries supply the semantics the tree alone doesn't carry:
 *
 * - [dataflowFields]: which `(type, field)` pairs *write* a named pipeline variable
 *   (`GatherCards.storeAs`) and which *read* one (`MoveCollection.from`), per namespace
 *   (collections / numbers / chosen values / string lists / subtype groups / cast flags).
 * - Slot readers/declarers: which node types read a [com.wingedsheep.sdk.scripting.ChoiceSlot]
 *   (`CastChoiceMade`, `HasChosenColor`, …) and which declare one (`EntersWithChoice`, kicker,
 *   blight, sneak).
 *
 * Both registries are kept honest by hygiene checks that run on every card: a string field whose
 * *name* looks like a dataflow reference (`storeAs`, `from`, `collectionName`, …) but whose
 * `(type, field)` is not classified fails with [CardValidationError.UnclassifiedDataflowField] —
 * the same "classify it or the build breaks" contract as the executor-coverage test.
 *
 * ## Scopes
 *
 * Pipeline variables live in an `EffectContext`, which exists per resolution. Each top-level
 * effect tree (the spell effect, each triggered/activated ability, each saga chapter) is its own
 * scope; abilities nested inside effects (granted triggered/activated abilities, token abilities)
 * start fresh scopes because they resolve later with a fresh context. Nested scopes are detected
 * *structurally*: a JSON object without a polymorphic `type` discriminator that has an `effect`
 * member plus a `trigger` / `cost` / target-requirement member is an ability. Modal `Mode`s are
 * target-scopes only — their effects share the parent resolution's collections, but their
 * `ContextTarget` indices are sliced per mode.
 *
 * Severities: a read whose name is written *nowhere* on the card is an [LintSeverity.ERROR]
 * (it can only be a typo); a read satisfied only later in the same scope or only in a different
 * scope is a [LintSeverity.WARNING] (cross-trigger flows are legal but worth eyeballing), as are
 * writes that are never read.
 */
object CardLinter {

    /**
     * JSON encoder with defaults materialized: a defaulted writer (`ChooseOption.storeAs =
     * "chosenOption"`) must still connect to an explicit reader of that name, and vice versa.
     * [CardSerialization.json] encodes with `encodeDefaults = false`, which would hide them.
     */
    private val lintJson = Json(from = CardSerialization.json) { encodeDefaults = true }

    /** `Scope.AttachedTo`'s serial name. */
    private const val ATTACHED_TO = "AttachedTo"

    /**
     * Fortifications attach to lands (CR 301.6-adjacent; the Fortify keyword). No card in the
     * corpus uses one yet and there is no `Subtype` constant, but they are an attachment type, so
     * the attachability check honors the subtype rather than mis-flagging the first one added.
     */
    private val FORTIFICATION = Subtype("Fortification")

    fun lint(card: CardDefinition): List<CardValidationError> {
        val findings = mutableListOf<CardValidationError>()
        val fullTree = lintJson.encodeToJsonElement(CardDefinition.serializer(), card) as JsonObject
        val explicitTree =
            CardSerialization.json.encodeToJsonElement(CardDefinition.serializer(), card) as JsonObject

        // Choice slots persist on the physical card (a DFC's back face reads what the front
        // declared), so declarations and reads are collected across all faces before any check.
        val slots = SlotUsage()
        collectSlots(fullTree, slots)

        lintDefinition(card.name, fullTree, explicitTree, slots, findings)
        checkSlots(card.name, slots, findings)
        checkOpponentChoosers(card.name, explicitTree, withinActivatedAbility = false, findings)
        checkAttachedScope(card, findings)
        checkManaAbilityClassification(card.name, fullTree, findings)
        return findings
    }

    /** Serial names of the effects that put mana into a pool (CR 605.1a's "could add mana"). */
    private val MANA_ADDING_EFFECTS = setOf(
        "AddMana",
        "AddColorlessMana",
        "AddManaOfChoice",
        "AddDynamicMana",
        "AddAnyColorManaSpendOnChosenType",
        "AddOneManaOfEachColorAmong",
    )

    /**
     * Check every activated ability's `isManaAbility` flag against CR 605.1a, in both directions.
     *
     * CR 605.1a makes the classification a *consequence* of the ability, not a choice: an activated
     * ability that could add mana, doesn't target, isn't a loyalty ability and whose **cost and
     * effect** move no card to or from a library **is** a mana ability — and one that fails any of
     * those tests is not, however much mana it makes. Getting the flag wrong is invisible on
     * inspection and changes four things at once: whether the ability can be activated while paying
     * a cost (CR 605.3a), whether it uses the stack and can be responded to (CR 605.3b), whether
     * `ManaAbilityEnumerator` surfaces it to the auto-tap solver, and whether anything keying off
     * "tapping a permanent for mana" (Badgermole Cub, Lavaleaper, Overabundance) fires.
     *
     * **Unflagged** — the `activatedAbility { manaAbility = true }` builder derives
     * [TimingRule.ManaAbility] from the flag, but a raw `ActivatedAbility(...)` — the only way to
     * build one *inside* a `GrantActivatedAbility` — has no builder to do it, and its
     * `isManaAbility` defaults to false. That is how Cryptolith Rite, Joiner Adept and Citanul
     * Hierophants shipped unflagged, and then Abundant Growth, Nature's Embrace, New Horizons,
     * Huatli, Enduring Vitality and Great Divide Guide after them. Documenting it twice didn't stop
     * the second batch; this does.
     *
     * **Misflagged** — the mirror image, and the reason this check runs both ways. The library
     * clause was only added to 605.1a in the August 7, 2026 rules update, so seven cards that were
     * mana abilities when they were written stopped being them on that date: Chromatic Sphere and
     * the five Odyssey Eggs (mana plus "Draw a card") and Deranged Assistant (a mill *cost*). A
     * one-directional check silently keeps every one of them resolving off the stack.
     *
     * Structural on purpose: an `isManaAbility` member identifies an `ActivatedAbility` wherever it
     * sits — printed, inside a static grant, or nested in a `GrantActivatedAbilityEffect` — so a
     * new grant shape is covered the day it is added.
     */
    private fun checkManaAbilityClassification(
        cardName: String,
        tree: JsonElement,
        findings: MutableList<CardValidationError>
    ) {
        forEachActivatedAbility(tree) { ability ->
            val flagged = (ability["isManaAbility"] as? JsonPrimitive)?.booleanOrNull ?: return@forEachActivatedAbility
            val isLoyalty = (ability["isPlaneswalkerAbility"] as? JsonPrimitive)?.booleanOrNull == true
            val targets = (ability["targetRequirements"] as? JsonArray)?.isNotEmpty() == true
            val effect = ability["effect"] ?: return@forEachActivatedAbility
            val movesLibraryCard = movesCardToOrFromLibrary(ability)

            if (!flagged) {
                if (isLoyalty || targets || movesLibraryCard) return@forEachActivatedAbility
                if (!ownEffectContains(effect, MANA_ADDING_EFFECTS)) return@forEachActivatedAbility
                findings.add(
                    CardValidationError.UnflaggedManaAbility(
                        cardName = cardName,
                        message = "'$cardName' has an activated ability that adds mana but is not " +
                            "flagged `isManaAbility = true`. By CR 605.1a it IS a mana ability, so the " +
                            "engine must resolve it off the stack: as written it uses the stack, can't " +
                            "be activated while paying a cost, is invisible to the auto-tap solver, and " +
                            "never triggers a \"whenever you tap a permanent for mana\" static. Set " +
                            "`isManaAbility = true` and `timing = TimingRule.ManaAbility` on the raw " +
                            "ActivatedAbility (the `activatedAbility { manaAbility = true }` builder " +
                            "sets both for you). If the ability genuinely isn't one — it targets, or " +
                            "moves a card to or from a library — say so in lint-allowlist.txt."
                    )
                )
                return@forEachActivatedAbility
            }

            val disqualifier = when {
                movesLibraryCard -> "its cost or effect moves a card to or from a library"
                targets -> "it requires a target"
                isLoyalty -> "it is a loyalty ability"
                else -> return@forEachActivatedAbility
            }
            findings.add(
                CardValidationError.MisflaggedManaAbility(
                    cardName = cardName,
                    message = "'$cardName' has an activated ability flagged `isManaAbility = true` " +
                        "that CR 605.1a disqualifies, because $disqualifier. It is an ordinary " +
                        "activated ability: it must use the stack, be respondable, and be " +
                        "unavailable while paying a cost — but as flagged the engine resolves it " +
                        "off the stack and offers it to the auto-tap solver. Drop `manaAbility = " +
                        "true` (and any explicit `timing = TimingRule.ManaAbility`) so the ability " +
                        "keeps the default instant-speed timing."
                )
            )
        }
    }

    /**
     * True if [ability]'s **cost or effect** moves a card to or from a library — the CR 605.1a
     * disqualifier ("its cost and effect don't move any card to or from a library").
     *
     * Checked by serial name against the *real* vocabulary, because there is no single `Mill` or
     * `SearchLibrary` node to look for: mill, search, exile-the-top and every other library
     * operation is a `GatherCards` → `MoveCollection` pipeline over a `CardSource` /
     * `CardDestination`, so the library-ness lives in a **zone field** on those nodes rather than
     * in the effect's own name. Three tests, in order of how much they have to know:
     *
     * - [LIBRARY_MOVING_NODES] — nodes that move a library card whatever their arguments:
     *   `DrawCards`, `Surveil` (library → graveyard), `Cascade`/`Discover`/`ExileFromTopRepeating`/
     *   `ExileLibraryUntilManaValue`, `PutOnLibraryPositionOfChoice`, and the `AtomMill` cost.
     * - The `AtomExileFrom` cost, which is a move only when its zone is the library — it exiles
     *   from a graveyard or a hand just as often (Molt Tender).
     * - [crossesLibraryBoundary] — the pipeline shapes, where whether a card moves *to or from* a
     *   library is a property of the whole pipeline rather than of any one node.
     *
     * `Scry` is deliberately in none of them: it reorders cards *within* a library and never moves
     * one to or from it, so a scry rider leaves a mana ability a mana ability (Path of Ancestry).
     */
    private fun movesCardToOrFromLibrary(ability: JsonObject): Boolean =
        listOfNotNull(ability["effect"], ability["cost"]).any { tree ->
            ownEffectContains(tree, LIBRARY_MOVING_NODES) ||
                exilesFromLibraryAsCost(tree) ||
                crossesLibraryBoundary(tree)
        }

    /**
     * True if a card *crosses* the library boundary somewhere in [tree] — the pipeline half of
     * [movesCardToOrFromLibrary], and the reason `TopOfLibrary` and `ToZone(Library)` can't simply
     * be listed as library-moving nodes.
     *
     * `CardSource.TopOfLibrary` is the shared gather for mill, exile-the-top, surveil, scry **and
     * look-at-top**, and `CardDestination.ToZone(Library)` is the shared put-back for shuffle-in,
     * put-on-top *and* the same reorders. Either one alone says only that a library was *touched*.
     *
     * So the default is that touching a library at all counts, and exactly one shape is carved out:
     * a **reorder**, where cards come off a library and every one of them goes straight back into
     * it. That is `LibraryPatterns.lookAtTopAndReorder` and the pipeline the compact `Scry` node
     * expands to — flagging those would make the check contradict its own `Scry` carve-out
     * depending only on which spelling the card used. Anything else that leaves the library
     * (mill, `lookAtTopAndKeep`, a search that casts what it finds) and anything that enters one
     * without having come from it (shuffle a graveyard in, put a card from hand on top) crosses.
     *
     * Reading destinations tree-wide rather than per-`MoveCollection` keeps this independent of
     * collection-key bookkeeping: `scryPipeline` re-keys its gather through `SelectFromCollection`
     * into two separate moves, and both still land in the library.
     */
    private fun crossesLibraryBoundary(tree: JsonElement): Boolean {
        val fromLibrary = anyNode(tree) { node, type ->
            type == "TopOfLibrary" || (type in ZONE_SOURCE_NODES && namesLibrary(node))
        }
        val toLibrary = anyNode(tree) { node, type -> type == "ToZone" && namesLibrary(node) }
        if (!fromLibrary && !toLibrary) return false

        val toElsewhere = anyNode(tree) { node, type -> type == "ToZone" && !namesLibrary(node) }
        // A cast takes the card to the stack, so a gather it consumes has left the library even
        // though no `ToZone` says so.
        val castsFromCollection = anyNode(tree) { _, type -> type == CAST_FROM_COLLECTION }
        val reorder = fromLibrary && toLibrary && !toElsewhere && !castsFromCollection
        return !reorder
    }

    /** True if [tree] pays an `AtomExileFrom` cost out of a library. */
    private fun exilesFromLibraryAsCost(tree: JsonElement): Boolean =
        anyNode(tree) { node, type -> type == "AtomExileFrom" && namesLibrary(node) }

    /**
     * True if any node in [tree] satisfies [predicate], which receives the node and its serial
     * `type`. Stops at the same boundary as [ownEffectContains]: a granted ability or an embedded
     * token definition that reaches a library is not something *this* resolution does.
     */
    private fun anyNode(tree: JsonElement, predicate: (JsonObject, String?) -> Boolean): Boolean =
        when (tree) {
            is JsonObject ->
                if (isNestedAbilityOrDefinition(tree)) false
                else predicate(tree, (tree["type"] as? JsonPrimitive)?.contentOrNull) ||
                    tree.values.any { anyNode(it, predicate) }
            is JsonArray -> tree.any { anyNode(it, predicate) }
            else -> false
        }

    /** True if [node] names the library in a scalar `zone` member or in a `zones` list. */
    private fun namesLibrary(node: JsonObject): Boolean =
        (node["zone"] as? JsonPrimitive)?.contentOrNull == LIBRARY_ZONE ||
            (node["zones"] as? JsonArray)
                ?.any { (it as? JsonPrimitive)?.contentOrNull == LIBRARY_ZONE } == true

    /** Nodes that move a card to or from a library. See [movesCardToOrFromLibrary]. */
    private val LIBRARY_MOVING_NODES = setOf(
        "DrawCards",
        "Surveil",
        "Cascade",
        "Discover",
        "ExileFromTopRepeating",
        "ExileLibraryUntilManaValue",
        "PutOnLibraryPositionOfChoice",
        "AtomMill",
    )

    /** `CardSource` shapes that name the zone they gather from. See [crossesLibraryBoundary]. */
    private val ZONE_SOURCE_NODES = setOf(
        "FromZone",
        "FromMultipleZones",
    )

    /** Serial name of the effect that casts a gathered card, taking it out of wherever it was. */
    private const val CAST_FROM_COLLECTION = "CastFromCollectionWithoutPayingCost"

    /** `Zone.LIBRARY`'s serial name. */
    private const val LIBRARY_ZONE = "Library"

    /** Every serialized `ActivatedAbility` anywhere in [tree] — the `isManaAbility` member marks one. */
    private fun forEachActivatedAbility(tree: JsonElement, visit: (JsonObject) -> Unit) {
        when (tree) {
            is JsonObject -> {
                if (tree.containsKey("isManaAbility")) visit(tree)
                tree.values.forEach { forEachActivatedAbility(it, visit) }
            }
            is JsonArray -> tree.forEach { forEachActivatedAbility(it, visit) }
            else -> {}
        }
    }

    /**
     * True if any polymorphic `"type"` discriminator in [tree] — or the tree itself, when a
     * no-argument effect encodes as the bare serial name — is one of [names], counting only what
     * *this* resolution does.
     *
     * The recursion stops at a nested ability or embedded card definition, because an effect that
     * merely carries one doesn't perform it: Avatar Roku's "{8}: Create a … Dragon with firebending
     * 4" embeds a token whose attack trigger adds {R}{R}{R}{R}, and the {8} ability adds no mana at
     * all. The same guard keeps a `GrantActivatedAbilityEffect` from being read as though it
     * produced the mana its granted ability will — [forEachActivatedAbility] visits that one on its
     * own terms.
     */
    private fun ownEffectContains(tree: JsonElement, names: Set<String>): Boolean = when (tree) {
        is JsonPrimitive -> tree.isString && tree.contentOrNull in names
        is JsonObject ->
            if (isNestedAbilityOrDefinition(tree)) false
            else (tree["type"] as? JsonPrimitive)?.contentOrNull in names ||
                tree.values.any { ownEffectContains(it, names) }
        is JsonArray -> tree.any { ownEffectContains(it, names) }
        else -> false
    }

    /** A serialized ability or card definition rather than a step of the effect being scanned. */
    private fun isNestedAbilityOrDefinition(tree: JsonObject): Boolean =
        tree.containsKey("script") || tree.containsKey("trigger") ||
            tree.containsKey("isManaAbility") || tree.containsKey("chapter")

    /**
     * Flag a `Scope.AttachedTo` filter on a card that can never host an attachment.
     *
     * Attach-scope means "the permanent this Aura/Equipment is attached to". The engine reaches it
     * only by walking a host's attachments — `TriggerAbilityResolver` iterates the permanents
     * attached to the entity, and the projection layer resolves the scope the same way. So on a
     * card that is nobody's Aura or Equipment there is no permanent to resolve to and the filter
     * silently matches nothing: the ability compiles, serializes, and does nothing at all.
     *
     * This is how Harmonious Grovestrider shipped with no ward. Its printed "Ward {2}" was authored
     * as `GrantWard(WardCost.Mana("{2}"))`, and `GrantWard.filter` *defaults* to
     * `GroupFilter.attachedCreature()` — the Aura/Equipment shape. Nothing was attached to the
     * Beast, so no ward trigger was ever generated and no `WARD` keyword was ever projected. A
     * creature's own printed ward is `keywordAbility(KeywordAbility.ward("{N}"))`; the attach-scope
     * default only makes sense on a card that attaches.
     *
     * The default filter is exactly what makes this invisible on inspection: it doesn't appear in
     * the card source *or* the serialized snapshot, so each ability is re-encoded with defaults
     * materialized ([lintJson]) rather than read off the explicit tree.
     *
     * ## What is deliberately *not* flagged
     *
     * Only the card's **printed** static abilities are checked — `script.staticAbilities` and the
     * per-level `classLevels`, on every face. Attach scope nested inside an *effect* is legitimate,
     * because an effect can change what the card is before the filter is ever read: The Irencrag is
     * a Legendary Artifact whose trigger turns it into an Equipment and hands it
     * `ModifyStats(3, 3)` on the default attached scope, which is correct precisely because the
     * grant only applies once it *is* an Equipment. The same goes for abilities an effect gives to
     * some other object — an Aura token minted by `CreateTokenEffect`, say. A printed static
     * ability has no such escape: it describes the card as printed, so its scope has to make sense
     * for the card as printed.
     *
     * Attachability is judged across the whole physical card — either side of a DFC and any
     * [com.wingedsheep.sdk.model.CardFace] counts — so a creature that transforms into an Aura is
     * not flagged for the attach-scope abilities on its other face.
     */
    private fun checkAttachedScope(
        card: CardDefinition,
        findings: MutableList<CardValidationError>
    ) {
        if (canEverBeAttached(card)) return
        val offends = printedStaticAbilities(card).any { ability ->
            hasAttachedScope(lintJson.encodeToJsonElement(StaticAbility.serializer(), ability))
        }
        if (!offends) return
        findings.add(
            CardValidationError.AttachedScopeGrantOnNonAttachment(
                cardName = card.name,
                message = "'${card.name}' uses a GroupFilter scoped to Scope.AttachedTo " +
                    "(\"enchanted/equipped creature\"), but it is not an Aura, Equipment, or " +
                    "Fortification and has no auraTarget or equipCost — nothing can ever be " +
                    "attached to it, so the filter matches no permanent and the ability is a " +
                    "silent no-op. Note that attach-scope is the *default* filter on the Grant* " +
                    "static abilities, so an omitted filter argument is the usual cause: for a " +
                    "card's own printed keyword use the keyword ability (e.g. " +
                    "keywordAbility(KeywordAbility.ward(\"{2}\"))), and for a lord-style grant to " +
                    "other permanents pass an explicit battlefield-scoped GroupFilter."
            )
        )
    }

    /**
     * The card's printed static abilities across every face — the ones that describe the card as
     * printed, and so must make sense for the card as printed. Class levels count: an unlocked
     * level's statics are printed on the Class, just gated behind the level.
     */
    private fun printedStaticAbilities(card: CardDefinition): List<StaticAbility> = buildList {
        fun addFrom(script: CardScript) {
            addAll(script.staticAbilities)
            script.classLevels.forEach { addAll(it.staticAbilities) }
        }
        addFrom(card.script)
        card.cardFaces.forEach { addFrom(it.script) }
        card.backFace?.let { addAll(printedStaticAbilities(it)) }
    }

    /** True if any face of the physical card can host an attachment. */
    private fun canEverBeAttached(card: CardDefinition): Boolean {
        fun attaches(typeLine: TypeLine, script: CardScript) =
            typeLine.isAura || typeLine.isEquipment || typeLine.hasSubtype(FORTIFICATION) ||
                // A card can carry the target/cost without the subtype; CardValidator reports that
                // mismatch on its own, and this check has no business double-reporting it.
                script.auraTarget != null

        if (attaches(card.typeLine, card.script) || card.equipCost != null) return true
        if (card.cardFaces.any { attaches(it.typeLine, it.script) }) return true
        return card.backFace?.let { canEverBeAttached(it) } ?: false
    }

    /**
     * True if any `"scope"` member anywhere in [tree] is `AttachedTo`. `Scope` is a sealed
     * hierarchy of `data object`s, which encode as the bare string `"AttachedTo"`; the object form
     * is accepted too so a future `Scope` variant carrying data doesn't slip past.
     */
    private fun hasAttachedScope(tree: JsonElement): Boolean = when (tree) {
        is JsonObject -> {
            val scope = tree["scope"]
            val isAttached = when (scope) {
                is JsonPrimitive -> scope.contentOrNull == ATTACHED_TO
                is JsonObject -> (scope["type"] as? JsonPrimitive)?.contentOrNull == ATTACHED_TO
                else -> false
            }
            isAttached || tree.values.any { hasAttachedScope(it) }
        }
        is JsonArray -> tree.any { hasAttachedScope(it) }
        else -> false
    }

    /**
     * Flag a [com.wingedsheep.sdk.scripting.targets.TargetChooser] requirement in a context the
     * engine doesn't route to that chooser. Each chooser is honored by exactly one announcement
     * path, and in the wrong context the controller would silently choose the target instead — so
     * catch it at card load rather than mis-resolve:
     *
     * - `Opponent` ("… of an opponent's choice") — activated abilities only, because routing it
     *   needs the controller to first pick *which* opponent decides.
     * - `TriggeringPlayer` / `ControllerOfTriggeringEntity` — triggered abilities only, because
     *   both are read off a trigger context that nothing else has.
     *
     * Walks the JSON tree carrying which ability subtree we're inside, so the check covers every
     * container structurally (granted abilities, class levels, token abilities) regardless of which
     * field name holds the requirement (`targetRequirement`, `targetRequirements`,
     * `additionalTargetRequirements`, …). The match is anchored on the type discriminator of the
     * [TargetRequirement]s that carry a `TargetChooser`, so this can't collide with the unrelated
     * `chooser` field on pipeline selection steps (`Chooser`, which has its own honored `Opponent`
     * value).
     */
    private fun checkOpponentChoosers(
        cardName: String,
        element: JsonElement,
        withinActivatedAbility: Boolean,
        findings: MutableList<CardValidationError>,
        withinTriggeredAbility: Boolean = false
    ) {
        when (element) {
            is JsonObject -> {
                val type = (element["type"] as? JsonPrimitive)?.contentOrNull
                val chooser = (element["chooser"] as? JsonPrimitive)?.contentOrNull
                if (type in CHOOSER_BEARING_TARGET_TYPES) {
                    val misplaced = when (chooser) {
                        "Opponent" -> !withinActivatedAbility
                        "TriggeringPlayer", "ControllerOfTriggeringEntity" -> !withinTriggeredAbility
                        else -> false
                    }
                    if (misplaced) {
                        val expected =
                            if (chooser == "Opponent") "an activated ability" else "a triggered ability"
                        // Name the printed wording, not just the enum: that is what lets an author
                        // recognise the clause they were modelling.
                        val wording = when (chooser) {
                            "Opponent" -> "… of an opponent's choice"
                            "TriggeringPlayer" -> "that player … of their choice"
                            else -> "its controller chooses target …"
                        }
                        findings.add(
                            CardValidationError.UnsupportedOpponentChooser(
                                cardName = cardName,
                                message = "'$cardName' uses a TargetChooser.$chooser " +
                                    "(\"$wording\") target outside $expected. Only $expected " +
                                    "routes that selection to the named player; here the " +
                                    "controller would silently choose the target instead. Move it " +
                                    "there or drop the chooser."
                            )
                        )
                    }
                }
                for ((key, value) in element) {
                    checkOpponentChoosers(
                        cardName,
                        value,
                        withinActivatedAbility || key == "activatedAbilities",
                        findings,
                        withinTriggeredAbility ||
                            key == "triggeredAbilities" ||
                            key == "stateTriggeredAbilities"
                    )
                }
            }
            is JsonArray -> element.forEach {
                checkOpponentChoosers(
                    cardName, it, withinActivatedAbility, findings, withinTriggeredAbility
                )
            }
            else -> {}
        }
    }

    /**
     * The [com.wingedsheep.sdk.scripting.targets.TargetRequirement] type discriminators that carry
     * a `chooser` field. Keep in step with the SDK: a requirement that gains a `chooser` and is
     * missing here is silently exempt from [checkOpponentChoosers].
     */
    private val CHOOSER_BEARING_TARGET_TYPES = setOf("AnyTarget", "TargetObject")

    // =========================================================================================
    // Namespaces and registries
    // =========================================================================================

    internal enum class Space(val displayName: String) {
        COLLECTION("collection"),
        NUMBER("stored number"),
        CHOSEN("chosen value"),
        STRING_LIST("stored string list"),
        SUBTYPE_GROUPS("stored subtype groups"),
        CAST_FLAG("cast-time flag"),
    }

    private enum class Kind { READ, WRITE, IGNORE }

    private data class Classification(val kind: Kind, val space: Space)

    private fun read(space: Space) = Classification(Kind.READ, space)
    private fun write(space: Space) = Classification(Kind.WRITE, space)
    private val ignored = Classification(Kind.IGNORE, Space.COLLECTION)

    /**
     * `(type discriminator | null, field name)` → how that field participates in dataflow.
     * A `null` type matches any node (for fields on non-polymorphic classes, which carry no
     * discriminator — e.g. `CastTimeCapture.flag`, `GroupFilter.chosenSubtypeKey`). Type-keyed
     * entries win over field-keyed ones.
     */
    private val dataflowFields: Map<Pair<String?, String>, Classification> = buildMap {
        // --- Writers -------------------------------------------------------------------------
        put("GatherCards" to "storeAs", write(Space.COLLECTION))
        put("CaptureControllers" to "storeAs", write(Space.COLLECTION))
        put("GatherSubtypes" to "storeAs", write(Space.SUBTYPE_GROUPS))
        put("GatherUntilMatch" to "storeMatch", write(Space.COLLECTION))
        put("GatherUntilMatch" to "storeRevealed", write(Space.COLLECTION))
        put("SelectFromCollection" to "storeSelected", write(Space.COLLECTION))
        put("SelectFromCollection" to "storeRemainder", write(Space.COLLECTION))
        put("ChoosePile" to "storeChosenAs", write(Space.COLLECTION))
        put("ChoosePile" to "storeOtherAs", write(Space.COLLECTION))
        put("MoveCollection" to "storeMovedAs", write(Space.COLLECTION))
        put("SelectTarget" to "storeAs", write(Space.COLLECTION))
        put("FilterCollection" to "storeMatching", write(Space.COLLECTION))
        put("FilterCollection" to "storeNonMatching", write(Space.COLLECTION))
        put("ExileLibraryUntilManaValue" to "storeAs", write(Space.COLLECTION))
        // The contest publishes the winning *player* as a one-entry collection and every card it
        // exiled, in every round, as another.
        put("ExileTopCardContest" to "storeWinnerAs", write(Space.COLLECTION))
        put("ExileTopCardContest" to "storeExiledAs", write(Space.COLLECTION))
        put("Discover" to "storeDiscoveredAs", write(Space.COLLECTION))
        put("CopyCardIntoCollection" to "storeAs", write(Space.COLLECTION))
        put("CopyCollectionIntoCollection" to "storeAs", write(Space.COLLECTION))
        put("CastFromCollectionWithoutPayingCost" to "storeCastTo", write(Space.COLLECTION))
        put("CounterAllOnStack" to "storeCountAs", write(Space.COLLECTION))
        put("Behold" to "storeAs", write(Space.COLLECTION))
        put("ChooseEntity" to "storeAs", write(Space.COLLECTION))
        put("ChooseOnePerCategory" to "storeAs", write(Space.COLLECTION))
        put("StoreNumber" to "name", write(Space.NUMBER))
        put("FlipCoins" to "storeHeadsAs", write(Space.NUMBER))
        put("FlipCoinsUntilLoss" to "storeWinsAs", write(Space.NUMBER))
        put("PlayerGuessesConditionEffect" to "storeGuessedRightAs", write(Space.NUMBER))
        put("ForEachCapturedController" to "countVariable", write(Space.NUMBER))
        put("DrawUpTo" to "storeNotDrawnAs", write(Space.NUMBER))
        put("Fight" to "excessDamageVariable", write(Space.NUMBER))
        put("PayCounters" to "storeAmountAs", write(Space.NUMBER))
        put("CollectEvidenceChosenAmount" to "storeAmountAs", write(Space.NUMBER))
        put("PayManaCostRepeatedly" to "storeCountAs", write(Space.NUMBER))
        put("ChooseOption" to "storeAs", write(Space.CHOSEN))
        put("NoteCreatureType" to "storeAs", write(Space.CHOSEN))
        put("StoreCardName" to "storeAs", write(Space.CHOSEN))
        put("EachPlayerChoosesCreatureType" to "storeAs", write(Space.STRING_LIST))
        put(null to "flag", write(Space.CAST_FLAG)) // CastTimeCapture (no discriminator)

        // --- Readers -------------------------------------------------------------------------
        for (type in listOf(
            "CaptureControllers", "GatherSubtypes", "RevealCollection", "SelectFromCollection",
            "ChoosePile", "MoveCollection", "GrantMayPlayFromExile", "GrantPlayWithoutPayingCost",
            "MakePlotted",
            "GrantPlayWithAdditionalCost", "GrantPlayWithCostIncrease", "FilterCollection",
            "ChooseOnePerCategory",
            "StoreCardName", "CastFromCollectionWithoutPayingCost", "PlayFromCollectionWithoutPayingCost",
            "CastAnyNumberFromCollectionWithoutPayingCost", "ExileFromStorage",
            "CopyCollectionIntoCollection", "RecordChosenLinkedExile",
            "PairWithSource",
        )) put(type to "from", read(Space.COLLECTION))
        put("ChoosePile" to "pileA", read(Space.COLLECTION))
        put("ChoosePile" to "pileB", read(Space.COLLECTION))
        put("ForEachCapturedController" to "collection", read(Space.COLLECTION))
        put("ForEachCapturedController" to "originalCollection", read(Space.COLLECTION))
        put("ForEachCapturedController" to "controllerSnapshot", read(Space.COLLECTION))
        put("IterationSpace.Collection" to "collection", read(Space.COLLECTION))
        put("ConditionalOnCollection" to "collection", read(Space.COLLECTION))
        put("CollectionContainsMatch" to "collection", read(Space.COLLECTION))
        put("CollectionSharesCardType" to "collection", read(Space.COLLECTION))
        put("SuccessCriterion.CollectionNonEmpty" to "name", read(Space.COLLECTION))
        put("FromVariable" to "variableName", read(Space.COLLECTION))
        put("PipelineTarget" to "collectionName", read(Space.COLLECTION))
        put("ControllerOfPipelineTarget" to "collectionName", read(Space.COLLECTION))
        put("StoredCardManaValue" to "collectionName", read(Space.COLLECTION))
        put("ManaValueSumOfCollection" to "collectionName", read(Space.COLLECTION))
        put("FromCostStorage" to "collectionName", read(Space.COLLECTION))
        put("RetargetChooser.OwnerOfStored" to "collectionName", read(Space.COLLECTION))
        put("TapUntapCollection" to "collectionName", read(Space.COLLECTION))
        put("AddCountersToCollection" to "collectionName", read(Space.COLLECTION))
        put("DealDamagePerEntityInZone" to "collectionName", read(Space.COLLECTION))
        put("DistinctEntitiesInCollections" to "collections", read(Space.COLLECTION))
        put("DistinctCardTypesInCollections" to "collections", read(Space.COLLECTION))
        put("ExcludeOtherCollection" to "otherCollectionName", read(Space.COLLECTION))
        put("VariableReference" to "variableName", read(Space.NUMBER))
        put("NameEqualsChosen" to "variableName", read(Space.CHOSEN))
        put("HasSubtypeFromVariable" to "variableName", read(Space.CHOSEN))
        put("YouControlMostOfChosenType" to "chosenValueKey", read(Space.CHOSEN))
        put(null to "chosenSubtypeKey", read(Space.CHOSEN)) // GroupFilter (no discriminator)
        put("HasSubtypeInStoredList" to "listName", read(Space.STRING_LIST))
        put("ExcludeSubtypesFromStored" to "storedKey", read(Space.STRING_LIST))
        put("HasSubtypeInEachStoredGroup" to "groupName", read(Space.SUBTYPE_GROUPS))
        put("CastTimeFlagSet" to "flag", read(Space.CAST_FLAG))
        // "becomes the chosen type/color" effects reading a ChooseOption result.
        for (type in listOf("SetCreatureSubtypes", "AddCreatureType", "AddSubtype", "SetLandType")) {
            put(type to "fromChosenValueKey", read(Space.CHOSEN))
        }
    }

    /**
     * Field names that *look like* dataflow references. A string-valued field with one of these
     * names on a node whose `(type, field)` is not in [dataflowFields] is a hygiene error: either
     * classify it (READ/WRITE) or list it as IGNORE. Deliberately narrow — generic names (`name`,
     * `key`, `id`) are classified per-type only.
     */
    private val candidateFieldNames = setOf(
        "from", "collection", "collectionName", "collections", "originalCollection",
        "controllerSnapshot", "pileA", "pileB", "variableName", "otherCollectionName",
        "storedKey", "groupName", "listName", "chosenValueKey", "chosenSubtypeKey",
        "countVariable", "flag",
    )

    private fun isStoreField(field: String) =
        field == "storeAs" || (field.startsWith("store") && field.length > 5 && field[5].isUpperCase())

    /** Known non-dataflow uses of candidate names — `(type, field)` pairs that are fine as-is. */
    private val hygieneExempt: Set<Pair<String, String>> = setOf(
        // EventPattern zone fields ("from"/"to" zones on zone-change triggers) are Zone enums.
        "ZoneChangeEvent" to "from",
    )

    /** Implicit accesses keyed by node type — participation not visible as a string field. */
    private fun implicitAccesses(type: String?, obj: JsonObject): List<Pair<Kind, Pair<Space, String>>> =
        when {
            type == "ChooseCreatureType" ->
                listOf(Kind.WRITE to (Space.CHOSEN to "chosenCreatureType"))
            type == "SelectFromCollection" &&
                (obj["matchChosenCreatureType"] as? JsonPrimitive)?.contentOrNull == "true" ->
                listOf(Kind.READ to (Space.CHOSEN to "chosenCreatureType"))
            // Token executors publish the created tokens' ids under this well-known name so
            // sibling steps can address them via PipelineTarget(CREATED_TOKENS, i).
            type == "CreateToken" || type == "CreatePredefinedToken" ||
                type == "CreateTokenCopyOfTarget" || type == "CreateTokenCopyOfSource" ->
                listOf(Kind.WRITE to (Space.COLLECTION to "createdTokens"))
            // Amass publishes the Army it chose under this well-known name (CR 701.47c), so a
            // sibling step can address "the amassed Army" — either as
            // DynamicAmount.EntityProperty(EntityReference.AmassedArmy, …) or, when it needs it
            // as a target, EffectTarget.PipelineTarget(AmassedArmy.STORAGE_KEY) (Goblin Plate
            // Mail's "then attach this Equipment to the amassed Army").
            type == "Amass" ->
                listOf(
                    Kind.WRITE to (
                        Space.COLLECTION to com.wingedsheep.sdk.scripting.values
                            .EntityReference.AmassedArmy.STORAGE_KEY
                        ),
                )
            // The scry / surveil macros are opaque nodes on the card, but the engine expands each
            // into a Gather → Select → Move pipeline (LibraryPatterns.scryPipeline /
            // surveilPipeline) whose selected/remainder collections seed the *same* EffectContext.
            // Sibling effects can therefore read the cards kept on top / put below — e.g. Starving
            // Revenant draws and drains per card left on top via
            // DistinctEntitiesInCollections("toTop"). Surface those writes so such reads resolve
            // instead of looking unwired. (Collection names mirror the two pipelines above.)
            type == "Scry" ->
                listOf(
                    Kind.WRITE to (Space.COLLECTION to "toBottom"),
                    Kind.WRITE to (Space.COLLECTION to "toTop"),
                )
            type == "Surveil" ->
                listOf(
                    Kind.WRITE to (Space.COLLECTION to "toGraveyard"),
                    Kind.WRITE to (Space.COLLECTION to "toTop"),
                )
            type == "ForEach" -> {
                val reducers = obj["collectCollections"] as? JsonObject
                if (reducers == null) emptyList() else reducers.flatMap { (localName, aggregateValue) ->
                    val aggregateName = (aggregateValue as? JsonPrimitive)?.contentOrNull
                    if (aggregateName == null) emptyList() else listOf(
                        Kind.READ to (Space.COLLECTION to localName),
                        Kind.WRITE to (Space.COLLECTION to aggregateName),
                    )
                }
            }
            else -> emptyList()
        }

    // =========================================================================================
    // Choice slots
    // =========================================================================================

    /** Node types that declare one or more slots just by being present. */
    private val slotDeclarers: Map<String, List<String>> = mapOf(
        "Sneak" to listOf("SNEAK"),
        "Ninjutsu" to listOf("SNEAK"),
        // Web-slinging (CR 702.188): the engine stamps both the "was web-slung" flag and the
        // returned creature's mana value when the web-slinging cost is paid (see StackResolver).
        "WebSlinging" to listOf("WEB_SLUNG", "WEB_SLUNG_RETURNED_MV"),
        // Mayhem (CR 702.187): the engine stamps the "mayhem cost was paid" flag on a resolved
        // permanent / into the resolution context when the Mayhem cost is paid (see StackResolver).
        "Mayhem" to listOf("MAYHEM_CAST"),
        "BlightVariable" to listOf("BLIGHT_AMOUNT"),
        "BlightOrPay" to listOf("BLIGHT_AMOUNT"),
        // Resolution-time color choices: ChooseColorThen sets EffectContext.chosenColor for its
        // wrapped effect; ChooseColorForTarget stamps a ChosenColorComponent on the permanent.
        // Both are what HasChosenColor / GrantChosenColor-style readers consume.
        "ChooseColorThen" to listOf("COLOR"),
        "ChooseColorForTarget" to listOf("COLOR"),
        // Resolution-time opponent choice: writes ChoiceSlot.OPPONENT on the source entity,
        // read back by Player.ChosenOpponent (gift recipient).
        "ChooseOpponentForSource" to listOf("OPPONENT"),
        // Gift (CR 702.174a): the cast-time promise declares both the "was it promised" flag and
        // the promised opponent, read back by Conditions.GiftWasPromised and Player.ChosenOpponent.
        "Gift" to listOf("GIFT_PROMISED", "OPPONENT"),
    )

    /** [com.wingedsheep.sdk.scripting.ReplacementEffect] `EntersWithChoice.choiceType` → slot. */
    private val choiceTypeToSlot: Map<String, String> = mapOf(
        "COLOR" to "COLOR",
        "CREATURE_TYPE" to "CREATURE_TYPE",
        "BASIC_LAND_TYPE" to "LAND_TYPE",
        "MODE" to "MODE",
        "CREATURE_ON_BATTLEFIELD" to "CREATURE",
        "OPPONENT" to "OPPONENT",
        "NUMBER" to "CHOSEN_NUMBER",
    )

    /** Node types that read a slot without naming it in a field. */
    private val slotReaders: Map<String, String> = mapOf(
        "ChosenOpponent" to "OPPONENT",
        "ChosenCreature" to "CREATURE",
        "HasChosenColor" to "COLOR",
        "SharesChosenColorWithSource" to "COLOR",
        "GrantChosenColor" to "COLOR",
        "GrantChosenSubtype" to "CREATURE_TYPE",
        "GrantProtectionFromChosenColorToGroup" to "COLOR",
        "GrantLandwalkOfChosenType" to "LAND_TYPE",
        "NotOfSourceChosenType" to "CREATURE_TYPE",
        "SneakCostWasPaid" to "SNEAK",
        "SourceChosenModeIs" to "MODE",
        "CardTypeEqualsChosenComponent" to "CARD_TYPE",
    )

    /** Node types whose `slot` field names the slot they read. */
    private val slotFieldReaders = setOf("CastChoice", "CastChoiceMade", "CastChoiceIs")

    /**
     * Node types whose `slot` field names the slot they *declare* (write durably on the source).
     * [com.wingedsheep.sdk.scripting.effects.ChooseNumberForSourceEffect] records a number under the
     * named slot (e.g. `CHOSEN_NUMBER` for Shapeshifter), which a `CastChoice` read then resolves.
     */
    private val slotFieldDeclarers = setOf("ChooseNumberForSource", "ChooseCardTypeForSource")

    private class SlotUsage {
        val declared = mutableSetOf<String>()
        val declaredModeIds = mutableSetOf<String>()
        val reads = mutableListOf<Pair<String, String>>() // slot to nodeType
        val modeIdReads = mutableListOf<String>()
    }

    /** One pass over the whole card (all faces) collecting slot declarations and reads. */
    private fun collectSlots(element: JsonElement, slots: SlotUsage) {
        when (element) {
            is JsonObject -> {
                val type = element.typeName()
                slotDeclarers[type]?.let { slots.declared.addAll(it) }
                if (type == "EntersWithChoice") {
                    val choiceType = (element["choiceType"] as? JsonPrimitive)?.contentOrNull
                    choiceTypeToSlot[choiceType]?.let { slots.declared.add(it) }
                    (element["modeOptions"] as? JsonArray)?.forEach { option ->
                        ((option as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull
                            ?.let { slots.declaredModeIds.add(it) }
                    }
                }
                slotReaders[type]?.let { slots.reads.add(it to type.orEmpty()) }
                if (type in slotFieldReaders) {
                    (element["slot"] as? JsonPrimitive)?.contentOrNull
                        ?.let { slots.reads.add(it to type.orEmpty()) }
                }
                if (type in slotFieldDeclarers) {
                    (element["slot"] as? JsonPrimitive)?.contentOrNull
                        ?.let { slots.declared.add(it) }
                }
                // The optional-additional-cost keyword (serial name "Kicker") declares whichever
                // slot its own mechanic uses: KICKED for kicker/multikicker/offspring, BARGAINED
                // for bargain (CR 702.166b).
                if (type == "Kicker") {
                    val declaredSlot = (element["declaredSlot"] as? JsonPrimitive)?.contentOrNull
                    slots.declared.add(declaredSlot ?: "KICKED")
                }
                if (type == "SourceChosenModeIs") {
                    (element["modeId"] as? JsonPrimitive)?.contentOrNull
                        ?.let { slots.modeIdReads.add(it) }
                }
                // A kicked resolution implies the KICKED slot even without the keyword object.
                if (element["kickerSpellEffect"] != null && element["kickerSpellEffect"] !is JsonNull) {
                    slots.declared.add("KICKED")
                }
                element.values.forEach { collectSlots(it, slots) }
            }
            is JsonArray -> element.forEach { collectSlots(it, slots) }
            else -> {}
        }
    }

    // =========================================================================================
    // Scopes
    // =========================================================================================

    private data class Access(
        val pos: Int,
        val space: Space,
        val name: String,
        val nodeType: String?,
        val field: String,
    )

    private data class TargetRef(
        val nodeType: String,
        val index: Int?, // ContextTarget / EntityReference.Target
        val boundName: String?, // BoundVariable
    )

    private class Scope(
        val label: String,
        val targetCount: Int,
        val targetIds: Set<String>,
        /** Non-null for Mode scopes: collections resolve against this enclosing scope. */
        val collectionParent: Scope?,
    ) {
        val reads = mutableListOf<Access>()
        val writes = mutableListOf<Access>()
        val targetRefs = mutableListOf<TargetRef>()

        /** The scope whose pipeline context this scope's collection accesses belong to. */
        val collectionScope: Scope get() = collectionParent?.collectionScope ?: this
    }

    private class LintState(val cardName: String, val findings: MutableList<CardValidationError>) {
        var pos = 0
        val scopes = mutableListOf<Scope>()

        fun newScope(
            label: String,
            targetCount: Int = 0,
            targetIds: Set<String> = emptySet(),
            collectionParent: Scope? = null,
        ): Scope = Scope(label, targetCount, targetIds, collectionParent).also { scopes.add(it) }
    }

    // =========================================================================================
    // Definition walk
    // =========================================================================================

    private fun lintDefinition(
        cardName: String,
        defObj: JsonObject,
        explicitDefObj: JsonObject?,
        slots: SlotUsage,
        findings: MutableList<CardValidationError>,
    ) {
        val state = LintState(cardName, findings)
        walkDefinitionScopes(defObj, state)
        checkDataflow(state, explicitDefObj, slots)
        checkTargets(state)

        // Faces are separate lint units: their scripts resolve in their own resolutions.
        (defObj["backFace"] as? JsonObject)?.let { back ->
            lintDefinition(
                "$cardName (back face)",
                back,
                explicitDefObj?.get("backFace") as? JsonObject,
                slots,
                findings,
            )
        }
        (defObj["cardFaces"] as? JsonArray)?.forEachIndexed { i, face ->
            val faceObj = face as? JsonObject ?: return@forEachIndexed
            val explicitFace = (explicitDefObj?.get("cardFaces") as? JsonArray)?.getOrNull(i) as? JsonObject
            // CardFace wraps its behavior in a `script`; reuse the definition walk on a
            // synthetic object so the same scope assembly applies.
            lintDefinition(
                "$cardName (face ${i + 1})",
                JsonObject(faceObj.filterKeys { it == "script" || it == "keywordAbilities" }),
                explicitFace?.let { JsonObject(it.filterKeys { k -> k == "script" || k == "keywordAbilities" }) },
                slots,
                findings,
            )
        }
    }

    /** Assembles the top-level scopes for one definition (card or face). */
    private fun walkDefinitionScopes(defObj: JsonObject, state: LintState) {
        val script = defObj["script"] as? JsonObject ?: return

        val baseReqs = script["targetRequirements"] as? JsonArray ?: JsonArray(emptyList())
        val kickerReqs = script["kickerTargetRequirements"] as? JsonArray ?: JsonArray(emptyList())
        val cleaveReqs = script["cleaveTargetRequirements"] as? JsonArray ?: JsonArray(emptyList())
        val spellScope = state.newScope(
            label = "spell effect",
            targetCount = maxOf(
                requirementSlotCount(baseReqs),
                requirementSlotCount(kickerReqs),
                requirementSlotCount(cleaveReqs),
            ),
            targetIds = requirementIds(baseReqs) + requirementIds(kickerReqs) + requirementIds(cleaveReqs),
        )

        // Spell-resolution scope, in execution order: cast-time writers (captures, additional
        // costs, alternative-cost riders) come before the spell effect that reads them.
        val abilityListFields = setOf(
            "triggeredAbilities", "stateTriggeredAbilities", "activatedAbilities",
            "staticAbilities", "replacementEffects", "sagaChapters", "classLevels",
        )
        val orderedSpellFields = listOf("castTimeCaptures", "additionalCosts", "selfAlternativeCost")
        val deferredSpellFields = listOf("spellEffect", "kickerSpellEffect", "cleaveSpellEffect")

        // A declared cast-time creature-type choice writes the chosen type before resolution.
        if (script["castTimeCreatureTypeChoice"]?.takeIf { it !is JsonNull } != null) {
            spellScope.writes.add(
                Access(state.pos++, Space.CHOSEN, "chosenCreatureType", null, "castTimeCreatureTypeChoice")
            )
        }
        defObj["keywordAbilities"]?.let { walk(it, spellScope, state) }
        for (field in orderedSpellFields) script[field]?.let { walk(it, spellScope, state) }
        for ((field, value) in script) {
            if (field in abilityListFields || field in orderedSpellFields || field in deferredSpellFields) continue
            walk(value, spellScope, state)
        }
        for (field in deferredSpellFields) script[field]?.let { walk(it, spellScope, state) }

        // Each ability is its own resolution.
        (script["triggeredAbilities"] as? JsonArray)?.forEachIndexed { i, ability ->
            walkAbilityScope(ability, "triggered ability ${i + 1}", state)
        }
        (script["stateTriggeredAbilities"] as? JsonArray)?.forEachIndexed { i, ability ->
            walkAbilityScope(ability, "state-triggered ability ${i + 1}", state)
        }
        (script["activatedAbilities"] as? JsonArray)?.forEachIndexed { i, ability ->
            walkAbilityScope(ability, "activated ability ${i + 1}", state)
        }
        (script["sagaChapters"] as? JsonArray)?.forEachIndexed { i, chapter ->
            walkAbilityScope(chapter, "saga chapter ${i + 1}", state)
        }
        (script["staticAbilities"] as? JsonArray)?.forEachIndexed { i, ability ->
            walkInto(ability, state.newScope("static ability ${i + 1}"), state)
        }
        (script["replacementEffects"] as? JsonArray)?.forEachIndexed { i, replacement ->
            walkInto(replacement, state.newScope("replacement effect ${i + 1}"), state)
        }
        (script["classLevels"] as? JsonArray)?.forEachIndexed { i, level ->
            // The level object itself is just a holder; its nested ability objects are
            // ability-shaped and start their own scopes via the structural rule.
            walkInto(level, state.newScope("class level ${i + 1}"), state)
        }
    }

    /** Starts a scope for an ability-shaped object and walks its members. */
    private fun walkAbilityScope(
        element: JsonElement,
        label: String,
        state: LintState,
        collectionParent: Scope? = null,
    ) {
        val obj = element as? JsonObject ?: return
        val reqs = JsonArray(
            listOfNotNull(obj["targetRequirement"]?.takeIf { it !is JsonNull }) +
                (obj["targetRequirements"] as? JsonArray ?: emptyList()) +
                (obj["additionalTargetRequirements"] as? JsonArray ?: emptyList())
        )
        // A Mode with no target requirements of its own indexes into the card-level
        // requirements (the engine slices the flat target list per mode only when modes
        // declare their own).
        val scope = if (reqs.isEmpty() && collectionParent != null) {
            state.newScope(label, collectionParent.targetCount, collectionParent.targetIds, collectionParent)
        } else {
            state.newScope(label, requirementSlotCount(reqs), requirementIds(reqs), collectionParent)
        }
        walkInto(obj, scope, state)
    }

    private fun requirementIds(reqs: JsonArray): Set<String> =
        reqs.mapNotNull { ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull }.toSet()

    /**
     * Number of `ContextTarget` indices a requirement list spans. `ContextTarget(i)` indexes the
     * *flattened* chosen-target list, so a requirement with `count = 2` ("two target creatures")
     * contributes two indices; `unlimited` / `dynamicMaxCount` requirements contribute an
     * unbounded number.
     */
    private fun requirementSlotCount(reqs: JsonArray): Int {
        var total = 0
        for (req in reqs) {
            val obj = req as? JsonObject ?: continue
            val unlimited = (obj["unlimited"] as? JsonPrimitive)?.contentOrNull == "true"
            val dynamicMax = obj["dynamicMaxCount"]?.takeIf { it !is JsonNull } != null
            if (unlimited || dynamicMax) return Int.MAX_VALUE
            total += (obj["count"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 1
        }
        return total
    }

    /**
     * Structural ability detection. Abilities ([com.wingedsheep.sdk.scripting.TriggeredAbility],
     * [com.wingedsheep.sdk.scripting.ActivatedAbility], modal `Mode`s, saga chapters) are concrete
     * classes, so unlike effects they serialize *without* a `type` discriminator — an object with
     * an `effect` member, no `type`, plus a trigger / cost / condition+id / target-requirement
     * member is an embedded ability wherever it appears (granted abilities, token abilities).
     */
    private enum class AbilityShape { FULL, TARGETS_ONLY }

    private fun abilityShape(obj: JsonObject): AbilityShape? {
        if (obj.containsKey("type") || !obj.containsKey("effect")) return null
        return when {
            obj.containsKey("trigger") || obj.containsKey("cost") ||
                (obj.containsKey("condition") && obj.containsKey("id")) -> AbilityShape.FULL
            obj.containsKey("targetRequirement") || obj.containsKey("targetRequirements") ->
                AbilityShape.TARGETS_ONLY
            else -> null
        }
    }

    /** Walks an object's members in declaration order without re-testing the object itself. */
    private fun walkInto(element: JsonElement, scope: Scope, state: LintState) {
        val obj = element as? JsonObject ?: return walk(element, scope, state)
        visitNode(obj, scope, state)
        obj.values.forEach { walk(it, scope, state) }
    }

    private fun walk(element: JsonElement, scope: Scope, state: LintState) {
        when (element) {
            is JsonObject -> {
                when (abilityShape(element)) {
                    AbilityShape.FULL ->
                        return walkAbilityScope(element, "ability granted by ${scope.label}", state)
                    AbilityShape.TARGETS_ONLY ->
                        // A modal Mode: own target slice, parent's pipeline context.
                        return walkAbilityScope(element, "mode of ${scope.label}", state, collectionParent = scope)
                    null -> {}
                }
                when (element.typeName()) {
                    "ReflexiveTrigger" -> return walkDeferredEffect(
                        element, scope, state,
                        effectField = "reflexiveEffect",
                        reqFields = listOf("reflexiveTargetRequirements"),
                        label = "reflexive trigger of ${scope.label}",
                    )
                    "CreateDelayedTrigger" -> return walkDeferredEffect(
                        element, scope, state,
                        effectField = "effect",
                        reqFields = listOf("targetRequirement", "additionalTargetRequirements"),
                        label = "delayed trigger of ${scope.label}",
                    )
                }
                visitNode(element, scope, state)
                element.values.forEach { walk(it, scope, state) }
            }
            is JsonArray -> element.forEach { walk(it, scope, state) }
            else -> {}
        }
    }

    /**
     * Effects that *defer* a sub-effect into its own future trigger resolution with its own
     * target requirements: a `ReflexiveTriggerEffect`'s reflexive effect targets via
     * `reflexiveTargetRequirements` (chosen when the reflexive trigger goes on the stack —
     * Foray of Orcs et al.), and a `CreateDelayedTriggerEffect`'s effect targets via its
     * `targetRequirement` plus `additionalTargetRequirements` (chosen each time the delayed trigger
     * fires — Rediscover the Way for one, Feral Encounter for two).
     * `ContextTarget` indices inside the deferred effect are scoped to those requirements;
     * when none are declared, they inherit the outer ability's targets ("exile target card …
     * when you do, return it"). Pipeline collections flow through — the engine snapshots them
     * at creation time.
     */
    private fun walkDeferredEffect(
        obj: JsonObject,
        scope: Scope,
        state: LintState,
        effectField: String,
        reqFields: List<String>,
        label: String,
    ) {
        visitNode(obj, scope, state)
        for ((field, value) in obj) {
            if (field == effectField) continue
            walk(value, scope, state)
        }
        val reqs = JsonArray(
            reqFields.flatMap { field ->
                when (val value = obj[field]) {
                    is JsonArray -> value
                    is JsonObject -> listOf(value)
                    else -> emptyList()
                }
            }
        )
        val child = if (reqs.isEmpty()) {
            state.newScope(label, scope.targetCount, scope.targetIds, scope)
        } else {
            state.newScope(label, requirementSlotCount(reqs), requirementIds(reqs), collectionParent = scope)
        }
        obj[effectField]?.let { walk(it, child, state) }
    }

    /**
     * Entity roles `ConditionEvaluator.evaluateEntityMatches` dispatches. Any other
     * `EffectTarget` inside an `EntityMatches` evaluates to a constant `false`, so the linter
     * rejects it at card load. Extending the evaluator to a new role must extend this set.
     */
    private val supportedEntityMatchesRoles = setOf(
        "Self",
        "EnchantedPermanent",
        "EnchantedCreature",
        "EquippedCreature",
        "ContextTarget",
        "TriggeringEntity",
        "DiscardedAsCost",
    )

    /** Records this node's dataflow accesses and target references (not its children). */
    private fun visitNode(obj: JsonObject, scope: Scope, state: LintState) {
        val type = obj.typeName()

        when (type) {
            "ContextTarget" -> (obj["index"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
                ?.let { scope.targetRefs.add(TargetRef(type, it, null)) }
            "Target" -> (obj["index"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
                ?.let { scope.targetRefs.add(TargetRef(type, it, null)) }
            "BoundVariable" -> (obj["name"] as? JsonPrimitive)?.contentOrNull
                ?.let { scope.targetRefs.add(TargetRef(type, null, it)) }
            "EntityMatches" -> {
                val role = when (val entity = obj["entity"]) {
                    is JsonPrimitive -> entity.contentOrNull
                    is JsonObject -> entity.typeName()
                    else -> null
                }
                if (role !in supportedEntityMatchesRoles) {
                    state.findings.add(
                        CardValidationError.UnsupportedEntityMatchesRole(
                            cardName = state.cardName,
                            message = "'${state.cardName}': EntityMatches names entity role " +
                                "'${role ?: "(none)"}', which the ConditionEvaluator doesn't dispatch — " +
                                "the condition would silently evaluate to false. Supported roles: " +
                                supportedEntityMatchesRoles.joinToString(", ") + ".",
                        )
                    )
                }
            }
        }

        for ((kind, spaceAndName) in implicitAccesses(type, obj)) {
            val (space, name) = spaceAndName
            val access = Access(state.pos++, space, name, type, "(implicit)")
            if (kind == Kind.READ) scope.collectionScope.reads.add(access)
            else scope.collectionScope.writes.add(access)
        }

        for ((field, value) in obj) {
            val names: List<String> = when {
                value is JsonPrimitive && value.isString -> listOf(value.content)
                value is JsonArray && dataflowFields[type to field]?.kind == Kind.READ ->
                    value.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
                else -> continue
            }
            val classification = dataflowFields[type to field] ?: dataflowFields[null to field]
            if (classification == null) {
                val suspicious = field in candidateFieldNames || isStoreField(field)
                if (suspicious && (type.orEmpty() to field) !in hygieneExempt) {
                    state.findings.add(
                        CardValidationError.UnclassifiedDataflowField(
                            cardName = state.cardName,
                            message = "'${state.cardName}': field '$field' on node type " +
                                "'${type ?: "(no discriminator)"}' looks like a pipeline-variable reference " +
                                "but is not classified in CardLinter.dataflowFields. Classify it as a " +
                                "READ/WRITE (with its namespace) or list it as a known non-dataflow field.",
                        )
                    )
                }
                continue
            }
            if (classification.kind == Kind.IGNORE) continue
            for (name in names) {
                val access = Access(state.pos++, classification.space, name, type, field)
                if (classification.kind == Kind.READ) scope.collectionScope.reads.add(access)
                else scope.collectionScope.writes.add(access)
            }
        }
    }

    // =========================================================================================
    // Checks
    // =========================================================================================

    /** `x_count` numeric reads are satisfied by a collection write named `x`. */
    private fun matches(read: Access, write: Access): Boolean {
        if (write.space == read.space && write.name == read.name) return true
        return read.space == Space.NUMBER && write.space == Space.COLLECTION &&
            read.name == "${write.name}_count"
    }

    private fun checkDataflow(state: LintState, explicitDefObj: JsonObject?, slots: SlotUsage) {
        val allWrites = state.scopes.flatMap { it.writes }

        /**
         * The default `chosenCreatureType` key doubles as the read path for the CREATURE_TYPE
         * choice slot: statics like Cover of Darkness pair `EntersWithChoice(CREATURE_TYPE)` with
         * a `chosenSubtypeKey = "chosenCreatureType"` group filter, no pipeline write involved.
         */
        fun satisfiedBySlot(read: Access): Boolean =
            read.space == Space.CHOSEN && read.name == "chosenCreatureType" &&
                "CREATURE_TYPE" in slots.declared

        /**
         * Collections the engine seeds for the ability before its effect runs, so a read with no
         * in-card writer is correct, not a silent no-op. `trigger.captured` is the batch-trigger
         * capture (the matching members of a `PermanentsEnteredEvent` batch — Kambal); the engine
         * populates it when the triggered ability resolves.
         */
        fun engineSeeded(read: Access): Boolean =
            read.space == Space.COLLECTION &&
                read.name == com.wingedsheep.sdk.scripting.effects.IterationSpace.TRIGGER_CAPTURED_COLLECTION

        for (scope in state.scopes) {
            if (scope.collectionParent != null) continue // merged into parent already
            for (read in scope.reads) {
                if (satisfiedBySlot(read)) continue
                if (engineSeeded(read)) continue
                val inScope = scope.writes.filter { matches(read, it) }
                when {
                    inScope.any { it.pos <= read.pos } ||
                        (read.nodeType == "ForEach" && read.field == "(implicit)" && inScope.isNotEmpty()) -> {}
                    inScope.isNotEmpty() -> state.findings.add(
                        CardValidationError.PipelineReadBeforeWrite(
                            cardName = state.cardName,
                            message = "'${state.cardName}' (${scope.label}): ${read.nodeType}.${read.field} " +
                                "reads ${read.space.displayName} '${read.name}' before any step writes it " +
                                "in the same resolution. Verify the pipeline ordering.",
                        )
                    )
                    allWrites.any { matches(read, it) } -> state.findings.add(
                        CardValidationError.CrossScopePipelineRead(
                            cardName = state.cardName,
                            message = "'${state.cardName}' (${scope.label}): ${read.nodeType}.${read.field} " +
                                "reads ${read.space.displayName} '${read.name}', which is only written in a " +
                                "different ability's resolution. Cross-resolution flows work only when the " +
                                "engine snapshots the value (e.g. delayed-trigger creation) — verify this one.",
                        )
                    )
                    else -> state.findings.add(
                        CardValidationError.UnresolvedPipelineRead(
                            cardName = state.cardName,
                            message = "'${state.cardName}' (${scope.label}): ${read.nodeType}.${read.field} " +
                                "reads ${read.space.displayName} '${read.name}', but nothing on this card " +
                                "writes it — the step would silently no-op. " +
                                suggestion(read, allWrites),
                        )
                    )
                }
            }
        }

        // Orphan writes: only explicitly-authored names (a defaulted storeAs nobody reads is
        // just an unused convenience default, not a smell).
        if (explicitDefObj != null) {
            val explicitNames = mutableSetOf<Pair<String, String>>() // (field, name)
            collectExplicitStrings(explicitDefObj, explicitNames)
            val allReads = state.scopes.flatMap { it.reads }
            for (write in allWrites) {
                if ((write.field to write.name) !in explicitNames) continue
                val isRead = allReads.any { read -> matches(read, write) }
                if (!isRead) {
                    state.findings.add(
                        CardValidationError.OrphanPipelineWrite(
                            cardName = state.cardName,
                            message = "'${state.cardName}': ${write.nodeType}.${write.field} stores " +
                                "${write.space.displayName} '${write.name}', but nothing reads it. " +
                                "Drop the store or wire up the consumer.",
                        )
                    )
                }
            }
        }
    }

    private fun suggestion(read: Access, writes: List<Access>): String {
        val sameSpace = writes.filter { it.space == read.space }.map { it.name }.distinct()
        return if (sameSpace.isEmpty()) {
            "No ${read.space.displayName} is written anywhere on the card."
        } else {
            "Written ${read.space.displayName}s on this card: ${sameSpace.joinToString(", ") { "'$it'" }}."
        }
    }

    /** Collects every `(field, value)` string pair present in the defaults-omitted tree. */
    private fun collectExplicitStrings(element: JsonElement, into: MutableSet<Pair<String, String>>) {
        when (element) {
            is JsonObject -> for ((field, value) in element) {
                if (value is JsonPrimitive && value.isString) into.add(field to value.content)
                collectExplicitStrings(value, into)
            }
            is JsonArray -> element.forEach { collectExplicitStrings(it, into) }
            else -> {}
        }
    }

    private fun checkTargets(state: LintState) {
        for (scope in state.scopes) {
            for (ref in scope.targetRefs) {
                if (ref.index != null && ref.index >= scope.targetCount) {
                    state.findings.add(
                        CardValidationError.InvalidTargetIndex(
                            cardName = state.cardName,
                            index = ref.index,
                            maxIndex = scope.targetCount - 1,
                            message = "'${state.cardName}' (${scope.label}): ${ref.nodeType} references " +
                                "target index ${ref.index} but the owning ability declares " +
                                "${scope.targetCount} target requirement(s).",
                        )
                    )
                }
                if (ref.boundName != null) {
                    val base = ref.boundName.substringBefore('[')
                    if (base !in scope.targetIds) {
                        state.findings.add(
                            CardValidationError.UnknownTargetBinding(
                                cardName = state.cardName,
                                message = "'${state.cardName}' (${scope.label}): BoundVariable('${ref.boundName}') " +
                                    "doesn't match any target requirement id in the owning ability " +
                                    (if (scope.targetIds.isEmpty()) "(none are named)."
                                    else "(named: ${scope.targetIds.joinToString(", ") { "'$it'" }})."),
                            )
                        )
                    }
                }
            }
        }
    }

    private fun checkSlots(cardName: String, slots: SlotUsage, findings: MutableList<CardValidationError>) {
        for ((slot, nodeType) in slots.reads) {
            if (slot !in slots.declared) {
                findings.add(
                    CardValidationError.UndeclaredChoiceSlotRead(
                        cardName = cardName,
                        message = "'$cardName': $nodeType reads choice slot $slot, but nothing on the card " +
                            "declares it (EntersWithChoice / kicker / blight / sneak). The read would " +
                            "always come back empty.",
                    )
                )
            }
        }
        for (modeId in slots.modeIdReads) {
            if (modeId !in slots.declaredModeIds) {
                findings.add(
                    CardValidationError.UnknownModeId(
                        cardName = cardName,
                        message = "'$cardName': SourceChosenModeIs('$modeId') doesn't match any " +
                            "EntersWithChoice mode option id " +
                            (if (slots.declaredModeIds.isEmpty()) "(no mode options declared)."
                            else "(declared: ${slots.declaredModeIds.joinToString(", ") { "'$it'" }})."),
                    )
                )
            }
        }
    }

    private fun JsonObject.typeName(): String? = (this["type"] as? JsonPrimitive)?.contentOrNull
}
