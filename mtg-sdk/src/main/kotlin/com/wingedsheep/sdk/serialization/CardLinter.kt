package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Structural lint for card definitions (sdk-analysis §1.1): catches the "silent no-op" bug class
 * where a name-based reference — a pipeline collection, a stored number, a chosen value, a
 * cast-time target binding, a [com.wingedsheep.sdk.scripting.ChoiceSlot] — doesn't resolve to
 * anything because of a typo, a missing writer, or a reference into the wrong ability's scope.
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
        return findings
    }

    /**
     * Flag a [com.wingedsheep.sdk.scripting.targets.TargetChooser.Opponent] requirement
     * ("… of an opponent's choice") in a context the engine doesn't route to an opponent. Only
     * activated abilities (including loyalty and granted activated abilities) honor the chooser at
     * announcement; on a spell, triggered ability, kicker target, or saga chapter the controller
     * would silently choose the target instead. Catch it at card load rather than mis-resolve.
     *
     * Walks the JSON tree carrying whether we're inside an `"activatedAbilities"` subtree, so the
     * check covers every container structurally (granted abilities, class levels, token abilities)
     * regardless of which field name holds the requirement (`targetRequirement`,
     * `targetRequirements`, `additionalTargetRequirements`, …). The match is anchored on the
     * `AnyTarget` type discriminator: it's the only [TargetRequirement] that carries a
     * `TargetChooser`, so this can't collide with the unrelated `chooser` field on pipeline
     * selection steps (`Chooser`, which has its own honored `Opponent` value).
     */
    private fun checkOpponentChoosers(
        cardName: String,
        element: JsonElement,
        withinActivatedAbility: Boolean,
        findings: MutableList<CardValidationError>
    ) {
        when (element) {
            is JsonObject -> {
                val type = (element["type"] as? JsonPrimitive)?.contentOrNull
                val chooser = (element["chooser"] as? JsonPrimitive)?.contentOrNull
                if (!withinActivatedAbility && type == "AnyTarget" && chooser == "Opponent") {
                    findings.add(
                        CardValidationError.UnsupportedOpponentChooser(
                            cardName = cardName,
                            message = "'$cardName' uses a TargetChooser.Opponent (\"… of an " +
                                "opponent's choice\") target outside an activated ability. Only " +
                                "activated abilities route the selection to an opponent; here the " +
                                "controller would silently choose it. Move it to an activated " +
                                "ability or drop the chooser."
                        )
                    )
                }
                for ((key, value) in element) {
                    checkOpponentChoosers(
                        cardName, value, withinActivatedAbility || key == "activatedAbilities", findings
                    )
                }
            }
            is JsonArray -> element.forEach {
                checkOpponentChoosers(cardName, it, withinActivatedAbility, findings)
            }
            else -> {}
        }
    }

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
        put("CopyCardIntoCollection" to "storeAs", write(Space.COLLECTION))
        put("CastFromCollectionWithoutPayingCost" to "storeCastTo", write(Space.COLLECTION))
        put("CounterAllOnStack" to "storeCountAs", write(Space.COLLECTION))
        put("Behold" to "storeAs", write(Space.COLLECTION))
        put("BeholdOrPay" to "storeAs", write(Space.COLLECTION))
        put("ChooseEntity" to "storeAs", write(Space.COLLECTION))
        put("StoreNumber" to "name", write(Space.NUMBER))
        put("ForEachCapturedController" to "countVariable", write(Space.NUMBER))
        put("DrawUpTo" to "storeNotDrawnAs", write(Space.NUMBER))
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
            "StoreCardName", "CastFromCollectionWithoutPayingCost",
            "CastAnyNumberFromCollectionWithoutPayingCost", "ExileFromStorage",
        )) put(type to "from", read(Space.COLLECTION))
        put("ChoosePile" to "pileA", read(Space.COLLECTION))
        put("ChoosePile" to "pileB", read(Space.COLLECTION))
        put("ForEachCapturedController" to "collection", read(Space.COLLECTION))
        put("ForEachCapturedController" to "originalCollection", read(Space.COLLECTION))
        put("ForEachCapturedController" to "controllerSnapshot", read(Space.COLLECTION))
        put("IterationSpace.Collection" to "collection", read(Space.COLLECTION))
        put("ConditionalOnCollection" to "collection", read(Space.COLLECTION))
        put("CollectionContainsMatch" to "collection", read(Space.COLLECTION))
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
            type == "CreateToken" || type == "CreatePredefinedToken" ->
                listOf(Kind.WRITE to (Space.COLLECTION to "createdTokens"))
            else -> emptyList()
        }

    // =========================================================================================
    // Choice slots
    // =========================================================================================

    /** Node types that declare a slot just by being present. */
    private val slotDeclarers: Map<String, String> = mapOf(
        "Kicker" to "KICKED",
        "Sneak" to "SNEAK",
        "BlightVariable" to "BLIGHT_AMOUNT",
        "BlightOrPay" to "BLIGHT_AMOUNT",
        // Resolution-time color choices: ChooseColorThen sets EffectContext.chosenColor for its
        // wrapped effect; ChooseColorForTarget stamps a ChosenColorComponent on the permanent.
        // Both are what HasChosenColor / GrantChosenColor-style readers consume.
        "ChooseColorThen" to "COLOR",
        "ChooseColorForTarget" to "COLOR",
    )

    /** [com.wingedsheep.sdk.scripting.ReplacementEffect] `EntersWithChoice.choiceType` → slot. */
    private val choiceTypeToSlot: Map<String, String> = mapOf(
        "COLOR" to "COLOR",
        "CREATURE_TYPE" to "CREATURE_TYPE",
        "BASIC_LAND_TYPE" to "LAND_TYPE",
        "MODE" to "MODE",
        "CREATURE_ON_BATTLEFIELD" to "CREATURE",
        "OPPONENT" to "OPPONENT",
    )

    /** Node types that read a slot without naming it in a field. */
    private val slotReaders: Map<String, String> = mapOf(
        "ChosenOpponent" to "OPPONENT",
        "ChosenCreature" to "CREATURE",
        "HasChosenColor" to "COLOR",
        "SharesChosenColorWithSource" to "COLOR",
        "GrantChosenColor" to "COLOR",
        "GrantProtectionFromChosenColorToGroup" to "COLOR",
        "GrantLandwalkOfChosenType" to "LAND_TYPE",
        "NotOfSourceChosenType" to "CREATURE_TYPE",
        "SneakCostWasPaid" to "SNEAK",
        "SourceChosenModeIs" to "MODE",
    )

    /** Node types whose `slot` field names the slot they read. */
    private val slotFieldReaders = setOf("CastChoice", "CastChoiceMade", "CastChoiceIs")

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
                slotDeclarers[type]?.let { slots.declared.add(it) }
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
        val spellScope = state.newScope(
            label = "spell effect",
            targetCount = maxOf(requirementSlotCount(baseReqs), requirementSlotCount(kickerReqs)),
            targetIds = requirementIds(baseReqs) + requirementIds(kickerReqs),
        )

        // Spell-resolution scope, in execution order: cast-time writers (captures, additional
        // costs, alternative-cost riders) come before the spell effect that reads them.
        val abilityListFields = setOf(
            "triggeredAbilities", "stateTriggeredAbilities", "activatedAbilities",
            "staticAbilities", "replacementEffects", "sagaChapters", "classLevels",
        )
        val orderedSpellFields = listOf("castTimeCaptures", "additionalCosts", "selfAlternativeCost")
        val deferredSpellFields = listOf("spellEffect", "kickerSpellEffect")

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
                        reqFields = listOf("targetRequirement"),
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
     * `targetRequirement` (chosen each time the delayed trigger fires — Rediscover the Way).
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
                    inScope.any { it.pos <= read.pos } -> {}
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
