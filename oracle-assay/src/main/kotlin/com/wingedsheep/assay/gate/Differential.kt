package com.wingedsheep.assay.gate

import com.wingedsheep.assay.corpus.ImplementedCard
import com.wingedsheep.assay.corpus.ImplementedCorpus
import com.wingedsheep.assay.corpus.OracleCard
import com.wingedsheep.assay.corpus.OracleCorpus
import com.wingedsheep.assay.corpus.OracleFace
import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.grammar.CardFragment
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.serialization.CardSerialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

/** The keys whose string *values* can be a target-slot reference. See `Differential.renameSlots`. */
private val SLOT_REFERENCE_KEYS = setOf("id", "name")

/** The `CardScript` fields holding a list of abilities with a generated id. See `canonicalizeAbilities`. */
private val ABILITY_LISTS = listOf("triggeredAbilities", "activatedAbilities")

/**
 * The keys under which a requirement-owning object declares its target slots, in the order
 * `ContextTarget`'s index counts them — `TriggeredAbility.allTargetRequirements` is
 * `targetRequirement` then `additionalTargetRequirements`, and `CardScript` /`ActivatedAbility`
 * each keep one plural list. See `Differential.normalizeOwner`.
 *
 * `auraTarget` is last because that is where the engine puts it: `CastSpellHandler` builds a spell's
 * flat target list as `targetRequirements` followed by the Aura's own restriction, so a positional
 * reference on an Aura counts the spell's requirements first. It is here at all because it is a
 * `TargetRequirement` like the others — the golden declares it with no `id` and the grammar always
 * mints one, so without this entry every Aura would diverge over a name in neither model.
 */
private val REQUIREMENT_KEYS =
    listOf("targetRequirement", "targetRequirements", "additionalTargetRequirements", "auraTarget")

/**
 * Gate 2 — the **differential**: Assay's reading of a card against the definition a human wrote
 * from the same text.
 *
 * The touchstone ([Touchstone]) proves a parse is reversible. It structurally cannot prove the parse
 * is *right*: a rule that reads "Elves" as `Elve`, or reads a negative condition as its positive,
 * round-trips perfectly and means the wrong thing. This gate is the general answer to that class,
 * and it runs on an asset the incumbent pipeline never had — [ImplementedCorpus], the hand-written
 * cards themselves.
 *
 * ## Fail-closed scoping
 *
 * A comparison is only run where Assay has a **complete** reading of the card: every ability line
 * either round-trips or is a normalized variant ([CardResult.covered]). Comparing a partially-read
 * card would be fail-open — a keyword Assay never saw because its line declined would look like
 * agreement, and the gate would report confidence it has not earned. Everything else is counted
 * into a named [Population] bucket instead, so the denominator is always visible.
 *
 * ## What is compared, at this stage
 *
 * The card's own printed **keyword abilities** — which is exactly the class Phase 1's grammar reads
 * whole, and needs no new grammar to start finding bugs. The SDK spells them two ways
 * ([CardDefinition.keywords] for the parameterless ones, [CardDefinition.keywordAbilities] for the
 * rest), so the hand-written side is unified into one set before comparing; see [printedKeywords].
 *
 * As the grammar grows, the comparison grows with it — triggered abilities, then spell effects —
 * and the scoping rule above is what keeps each addition honest.
 */
class Differential(private val touchstone: Touchstone = Touchstone()) {

    /**
     * Run the gate over every hand-written card.
     *
     * The Scryfall side is indexed in memory (thin [OracleCard]s, ~35k of them) and the hand-written
     * side is streamed, so only one [CardDefinition] is resident at a time — the goldens are 20 MB
     * of JSON and inflating all of them at once is the one way this gate could need real heap.
     */
    fun run(refresh: Boolean = false, limit: Int? = null, setFilter: String? = null): DifferentialReport {
        val oracle = indexOracle(refresh)
        val builder = DifferentialReport.builder()
        var seen = 0
        for (implemented in ImplementedCorpus.cards()) {
            if (setFilter != null && !implemented.setCode.equals(setFilter, ignoreCase = true)) continue
            builder.add(compare(implemented, oracle))
            seen++
            if (limit != null && seen >= limit) break
        }
        return builder.build()
    }

    /**
     * Index by Oracle ID *and* by name. The ID is the reliable join — it is what Scryfall considers
     * one card across every printing — and the name is the fallback for goldens minted before the id
     * was recorded, or for the setless `custom/` cards that have no Scryfall entry at all.
     */
    private fun indexOracle(refresh: Boolean): Map<String, OracleCard> {
        val index = HashMap<String, OracleCard>()
        for (card in OracleCorpus.cards(refresh = refresh)) {
            card.oracleId?.let { index.putIfAbsent("id:$it", card) }
            index.putIfAbsent("name:${card.name.lowercase(Locale.ROOT)}", card)
            // Split and DFC names join on either half; the front face is what a golden is named for.
            card.name.substringBefore(" // ").takeIf { it != card.name }
                ?.let { index.putIfAbsent("name:${it.lowercase(Locale.ROOT)}", card) }
        }
        return index
    }

    fun compare(implemented: ImplementedCard, oracle: Map<String, OracleCard>): CardComparison {
        val definition = implemented.definition
            ?: return CardComparison(implemented, null, Population.UNDECODABLE)

        val card = definition.oracleId?.let { oracle["id:$it"] }
            ?: oracle["name:${implemented.name.lowercase(Locale.ROOT)}"]
            ?: return CardComparison(implemented, null, Population.NO_ORACLE_TEXT)

        // Multi-face cards split their model across `backFace` / `cardFaces`, which the keyword
        // comparison would have to mirror face by face. Excluded and counted rather than compared
        // against the front face alone, which would report a divergence for every back-face keyword.
        if (card.faces.size > 1) return CardComparison(implemented, card, Population.MULTI_FACE)

        // The comparison is only meaningful if both sides are talking about the same text. A golden
        // carries the Oracle text it was authored from, so disagreeing with Scryfall means either
        // the name join found the wrong card or the card was written against wording that has since
        // changed — and in both cases Assay would be reading one card and diffing another. Found by
        // the gate itself: three cards joined to an entry with *no* text, which Assay covers
        // trivially, and then "diverged" against a fully-implemented script.
        //
        // Compared *normalized*, not raw. Goldens include printed reminder text inconsistently —
        // "Flying" in one and "Flying (This creature can't be blocked…)" in another — and that is
        // authoring noise, not a difference in what the card says. Normalization strips reminders as
        // an invertible pass, so reusing it here compares the two texts on the only terms the
        // grammar ever sees them.
        if (!sameText(card.faces.single(), definition)) {
            return CardComparison(implemented, card, Population.ORACLE_TEXT_DIFFERS)
        }

        val result = touchstone.assay(card)
        if (!result.covered) return CardComparison(implemented, card, Population.NOT_COVERED)

        // The other half of fail-closed scoping. Assay reading every *line* is not the same as Assay
        // modelling every *slot*: a keyword the SDK lowers to a triggered ability at authoring time
        // (rampage, modular) leaves content in a slot the grammar cannot produce, and
        // confirming such a card would be claiming to have checked a lowering nobody compared.
        // Stated as "everything outside the modelled slots is still default", so widening the
        // grammar is one edit here and the check tightens with it.
        if (unmodelledSlots(definition.script) != CardScript.EMPTY) {
            return CardComparison(implemented, card, Population.SCRIPT_NOT_MODELLED)
        }

        // A card whose lines cannot be folded into one model — two effect paragraphs where the
        // `CardScript` has one `spellEffect` — is counted rather than approximated. See
        // `CardFragment.merge`.
        val fromText = result.lines.mapNotNull { it.model }
            .fold(CardFragment.EMPTY as CardFragment?) { acc, fragment -> acc?.merge(fragment) }
            // CR 607's linked abilities are a whole-card fact, so they are resolved once the fold
            // has every line — see `CardFragment.deriveExileLinkage`.
            ?.deriveExileLinkage()
            ?: return CardComparison(implemented, card, Population.LINES_DO_NOT_FOLD)
        val fromCard = CardFragment(
            keywordAbilities = printedKeywords(definition).toList(),
            script = modelledSlots(definition.script),
            // The one compared field outside the script and outside the keyword set. "Equip {1}"
            // lowers into this *and* an activated ability, so comparing only the ability would
            // confirm an Equipment that can never be equipped: `CardValidator` and `CardLinter` both
            // read this field, and a card carrying the ability without the cost is a different card.
            equipCost = definition.equipCost,
            // The second compared field outside the script, and the first that is not behaviour: a
            // `*` in the stat box is a characteristic-defining ability (CR 604.3), and the SDK puts
            // it here rather than in an ability list. Only the *dynamic* halves are compared — a
            // fixed 2/2 is the printed header, which no line says and Assay never reads — so the
            // pair is null on both sides for every card without a CDA.
            dynamicPower = definition.creatureStats?.power?.takeIf { it !is CharacteristicValue.Fixed },
            dynamicToughness = definition.creatureStats?.toughness?.takeIf { it !is CharacteristicValue.Fixed },
        )

        // The other half of the modelled-slot guard, now that whole *ability lists* are slots the
        // grammar reaches. A keyword the SDK lowers at authoring time puts an ability in the script
        // that *no text line prints*: the printed line is the keyword, which Assay reads as a
        // keyword. Triggers get this from prowess, provoke, rampage, training and mobilize;
        // activated abilities get it from cycling, equip, morph and level up. So a card carrying
        // more abilities than Assay read is carrying content nobody printed, and comparing its
        // script would report the lowering as a divergence on every such card.
        //
        // One-directional on purpose. The card having more is the lowering; **Assay** having more
        // would mean the grammar invented an ability, which must diverge loudly and not be excused
        // by this guard.
        if (carriesUnreadAbilities(fromCard.script, fromText.script)) {
            return CardComparison(implemented, card, Population.SCRIPT_NOT_MODELLED)
        }

        val textKeywords = Folds.apply(fromText.keywordAbilities.toSet())
        val cardKeywords = Folds.apply(fromCard.keywordAbilities.toSet())
        val scriptsAgree = normalizeSlotNames(fromText.script) == normalizeSlotNames(fromCard.script)
        val equipCostsAgree = fromText.equipCost == fromCard.equipCost
        val statsAgree = fromText.dynamicPower == fromCard.dynamicPower &&
            fromText.dynamicToughness == fromCard.dynamicToughness

        return if (textKeywords == cardKeywords && scriptsAgree && equipCostsAgree && statsAgree) {
            CardComparison(implemented, card, Population.COMPARED, Verdict.CONFIRMED)
        } else {
            CardComparison(
                implemented = implemented,
                oracle = card,
                population = Population.COMPARED,
                verdict = Verdict.DIVERGENT,
                onlyInText = (textKeywords - cardKeywords).toList(),
                onlyInCard = (cardKeywords - textKeywords).toList(),
                textScript = fromText.script.takeUnless { scriptsAgree },
                cardScript = fromCard.script.takeUnless { scriptsAgree },
                textEquipCost = fromText.equipCost.takeUnless { equipCostsAgree },
                cardEquipCost = fromCard.equipCost.takeUnless { equipCostsAgree },
                equipCostsAgree = equipCostsAgree,
                textStats = statLine(fromText).takeUnless { statsAgree },
                cardStats = statLine(fromCard).takeUnless { statsAgree },
                statsAgree = statsAgree,
            )
        }
    }

    /**
     * A card's two characteristic-defining halves as one printable line, `power/toughness`, with a
     * dash where the half is not defined by an ability. Structural for [structural]'s reason: two
     * `DynamicAmount`s that describe themselves identically are exactly the pair worth reading.
     */
    private fun statLine(fragment: CardFragment): String =
        "${fragment.dynamicPower ?: "—"} / ${fragment.dynamicToughness ?: "—"}"

    /**
     * Do the Scryfall face and the golden say the same thing, once reminder text is out of the way?
     *
     * The golden's `oracleText` is run through the same [Normalizer] as the printed face, so the
     * two are compared as ability lines rather than as bytes. Only the text matters here, but the
     * face is built with the golden's own name so that self-reference abstraction lines up.
     */
    private fun sameText(face: OracleFace, definition: CardDefinition): Boolean {
        val golden = OracleFace(name = face.name, oracleText = definition.oracleText, typeLine = face.typeLine)
        return Normalizer.normalize(golden).lines == Normalizer.normalize(face).lines
    }

    /**
     * Does the hand-written card carry abilities in a modelled list that no printed line produced?
     *
     * By count rather than by content, because the check runs *before* the comparison and its job
     * is only to spot a lowering. A card and a text that carry the same number of abilities but
     * different ones is exactly what the comparison is for.
     */
    private fun carriesUnreadAbilities(card: CardScript, text: CardScript): Boolean =
        card.triggeredAbilities.size > text.triggeredAbilities.size ||
            card.activatedAbilities.size > text.activatedAbilities.size ||
            // Statics get the lowering too, and the corpus already has one: affinity is spelled as
            // `KeywordAbility.Affinity` on Frogmite and hand-rolled as a `ModifySpellCost` static on
            // Qumulox and the Darksteel golems, whose printed line is the bare keyword either way.
            // Without this count those cards would report as divergent on the day `staticAbilities`
            // opened, over a second SDK spelling nobody's text said twice.
            card.staticAbilities.size > text.staticAbilities.size ||
            card.replacementEffects.size > text.replacementEffects.size

    /**
     * The `CardScript` slots the grammar can produce — [CardFragment.MODELLED_SLOTS_NOTE] — and its
     * complement. Written as a `copy` pair rather than a field list so the two stay exhaustive
     * between them however many fields `CardScript` grows.
     */
    private fun modelledSlots(script: CardScript) = CardScript(
        spellEffect = script.spellEffect,
        targetRequirements = script.targetRequirements,
        triggeredAbilities = script.triggeredAbilities,
        activatedAbilities = script.activatedAbilities,
        staticAbilities = script.staticAbilities,
        replacementEffects = script.replacementEffects,
        auraTarget = script.auraTarget,
        castRestrictions = script.castRestrictions,
        additionalCosts = script.additionalCosts,
        cantBeCountered = script.cantBeCountered,
        conditionalFlash = script.conditionalFlash,
    )

    private fun unmodelledSlots(script: CardScript) = script.copy(
        spellEffect = null,
        targetRequirements = emptyList(),
        triggeredAbilities = emptyList(),
        activatedAbilities = emptyList(),
        staticAbilities = emptyList(),
        replacementEffects = emptyList(),
        auraTarget = null,
        castRestrictions = emptyList(),
        additionalCosts = emptyList(),
        cantBeCountered = false,
        conditionalFlash = null,
    )

    /**
     * Rename target slots to their position, on both sides, before comparing.
     *
     * The string linking a `TargetRequirement` to the `EffectTarget` reading it is arbitrary — see
     * [com.wingedsheep.assay.grammar.Targets] — so two models differing only in that name are the
     * same model, and a differential that reported them as divergent would be measuring a naming
     * convention. Hand-written cards name the slot after the card ("target creature to destroy",
     * "creature or enchantment"); the grammar always mints one name; neither is more correct.
     *
     * **Rewritten over the JSON tree rather than the JSON text.** A plain string replacement of the
     * declared id — which is what this did, following `CardDefinitionSnapshotTest.normalizeAbilityIds`
     * — rewrites *keys* as well as values, and the grammar's slot is called `target`, which is also
     * the name of a field on every targeted effect. So `"target":{…}` became `"slot_0":{…}` on
     * Assay's side and stayed `"target":{…}` on a card that had named its slot anything else, and six
     * cards reported as divergent over a difference that was not in either model. Walking the tree
     * and rewriting only string *values* under `id` / `name` keeps the original intent — rename the
     * declared slots, touch nothing else — without the collision.
     *
     * **Scoped per requirement-owner, because that is what a slot number means.** A `CardScript`,
     * a `TriggeredAbility` and an `ActivatedAbility` each declare their own requirements, and
     * `ContextTarget(0)` inside an ability indexes *that ability's* list — so numbering has to
     * restart at each owner rather than run across the card. A global counter happened to agree
     * while the grammar only produced top-level requirements; Trench Wurm, whose whole script is
     * one activated ability with a positional target, is what a card looks like when it stops
     * agreeing.
     */
    internal fun normalizeSlotNames(script: CardScript): String {
        val json = CardSerialization.json.encodeToString(CardScript.serializer(), script)
        val tree = CardSerialization.json.parseToJsonElement(json)
        return CardSerialization.json.encodeToString(
            JsonElement.serializer(),
            sortKeys(
                Folds.dropModeDescriptions(
                    Folds.dropPresentation(
                        Folds.flattenComposites(
                            Folds.renamePipelineKeys(
                                canonicalizeGrantedAbilities(canonicalizeAbilities(normalizeSlots(tree))),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    /**
     * Compare objects as objects, not as the byte order their fields happened to serialize in.
     *
     * A JSON object is unordered, and both sides here come out of the same serializer, so sorting is
     * information-free — but *not* sorting is not. Two passes above add a key rather than rewrite
     * one: [normalizeOwner] stamps a requirement's `id` and [canonicalizeAbilities] stamps an
     * ability's, and `Map + Pair` appends when the key is absent and rewrites in place when it is
     * present. So a model whose id the serializer emitted and an identical model whose id it did not
     * produced the same fields in two orders, and the string comparison called that a divergence.
     *
     * Whether the serializer emitted an `AbilityId` at all used to be **unstable across calls**,
     * which is what made this present as a phantom: `encodeDefaults` is false and the id's default
     * was a global counter, so kotlinx re-evaluated the default to decide whether to skip the field
     * and a golden holding `ability_2` was omitted exactly when the counter next returned
     * `ability_2`. Two encodes of the *same* card therefore differed, and Blasting Station — whose
     * models are equal field for field — reported as divergent on some runs and confirmed on others.
     * (Ids are now minted per card and always encoded, but the sort still closes the class.)
     *
     * Sorting is the fix rather than "always stamp in position", because it closes the whole class:
     * any later pass that adds a key, and any field the serializer omits on one side, is now
     * compared on what it says instead of on where it sits.
     */
    private fun sortKeys(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.entries.sortedBy { it.key }.associate { it.key to sortKeys(it.value) })
        is JsonArray -> JsonArray(element.map(::sortKeys))
        else -> element
    }

    private fun slotName(index: Int) = "slot_$index"

    /**
     * Find every requirement-owning object in the tree and normalize each one's slots in isolation.
     *
     * An owner is any object that declares requirements under one of [REQUIREMENT_KEYS] — the root
     * script, a triggered ability, an activated ability. Objects that declare none are walked
     * through, because a card whose only requirements live inside an ability has a root that owns
     * nothing.
     */
    private fun normalizeSlots(element: JsonElement): JsonElement = when {
        element is JsonObject && element.keys.any { it in REQUIREMENT_KEYS } -> normalizeOwner(element)
        element is JsonObject -> JsonObject(element.mapValues { normalizeSlots(it.value) })
        element is JsonArray -> JsonArray(element.map(::normalizeSlots))
        else -> element
    }

    /**
     * Stamp one owner's requirements with their positional names and rewrite every reference to
     * them inside it.
     *
     * Stamping rather than only renaming, because the declared `id` is optional in the SDK — a card
     * whose effects refer to their target positionally never needs one — so renaming alone would
     * leave one side carrying `"id": "slot_0"` and the other no `id` at all, over a difference that
     * is not in either model.
     */
    private fun normalizeOwner(owner: JsonObject): JsonObject {
        var position = 0
        val names = mutableMapOf<String, String>()
        // Positions are assigned in REQUIREMENT_KEYS order rather than in the object's own key
        // order, because that is the order `ContextTarget`'s index counts them in.
        val stamped = REQUIREMENT_KEYS.mapNotNull { key ->
            val declared = owner[key] ?: return@mapNotNull null
            val requirements = (declared as? JsonArray) ?: JsonArray(listOf(declared))
            val slots = requirements.map { requirement ->
                val slot = slotName(position++)
                val fields = requirement as? JsonObject ?: return@map requirement
                (fields["id"] as? JsonPrimitive)?.takeIf { it.isString }?.let { names[it.content] = slot }
                JsonObject(fields + ("id" to JsonPrimitive(slot)))
            }
            key to if (declared is JsonArray) JsonArray(slots) else slots.single()
        }.toMap()

        return JsonObject(
            owner.mapValues { (key, value) ->
                stamped[key] ?: rewriteReferences(value, names)
            }
        )
    }

    /**
     * Rewrite references to this owner's slots, stopping at any nested owner so its own numbering
     * applies inside it.
     */
    private fun rewriteReferences(element: JsonElement, names: Map<String, String>): JsonElement = when {
        element is JsonObject && element.keys.any { it in REQUIREMENT_KEYS } -> normalizeSlots(element)
        element is JsonObject -> positionalReference(element) ?: JsonObject(
            element.mapValues { (key, value) ->
                val renamed = (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.let(names::get)
                if (key in SLOT_REFERENCE_KEYS && renamed != null) JsonPrimitive(renamed)
                else rewriteReferences(value, names)
            }
        )

        element is JsonArray -> JsonArray(element.map { rewriteReferences(it, names) })
        else -> element
    }

    /**
     * Give an ability the id its position implies — the one thing about an ability that no printed
     * text determines. (An authored `descriptionOverride` is the other, and it now belongs to
     * [Folds.dropPresentation], which drops the same class of field wherever it is nested.)
     *
     * An `AbilityId` is arbitrary in exactly the way a target slot's name is, and more obviously so:
     * the DSL numbers them per card in construction order, which is why Kavu Climber's golden says
     * `"Kavu Climber:1"`. Comparing it would measure the order the card's abilities happened to be
     * written in.
     *
     * Both lists get the same treatment, because a card's activated abilities are numbered from the
     * same per-card counter its triggered ones are — Blasting Station's golden numbers its trigger
     * `:1` and its activated ability `:2` purely because that is the order they were constructed
     * in, and the grammar mints a fixed constant for each.
     */
    private fun canonicalizeAbilities(script: JsonElement): JsonElement {
        val root = script as? JsonObject ?: return script
        return JsonObject(root + ABILITY_LISTS.mapNotNull { key ->
            val abilities = root[key] as? JsonArray ?: return@mapNotNull null
            key to JsonArray(abilities.mapIndexed { index, ability ->
                val fields = (ability as? JsonObject) ?: return@mapIndexed ability
                JsonObject(fields + ("id" to JsonPrimitive("${key}_$index")))
            })
        })
    }

    /**
     * …and the ability a **static hands out**, which is nested one level further in.
     *
     * `GrantTriggeredAbility` and `GrantActivatedAbility` carry a whole ability inside the static,
     * and its id is generated exactly as a top-level one is — `AbilityId.next()` on the card, a
     * fixed constant in the grammar. Leaving it out of [canonicalizeAbilities] made the gate report
     * six Sliver lords as divergent over a counter: a difference in neither model, and the same
     * class of self-deception as the slot-name and per-owner numbering bugs before it. Found the way
     * all of those were, by running the gate on a card class it had never reached.
     *
     * One fixed token rather than a position, because a static holds exactly one granted ability —
     * there is no list to index into.
     *
     * Stamped whether or not the ability *has* an `id` in the JSON, for the reason [sortKeys]
     * records: the serializer's decision to emit an `AbilityId` was not always stable across calls,
     * so keying this on the field's presence would canonicalize one side of a pair and not the other.
     * A `GrantStaticAbility` carries an ability with no id at all and picks up a stamp it does not
     * need — harmless, because both sides get the same one and no printed word ever determined it.
     */
    private fun canonicalizeGrantedAbilities(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (key, value) ->
                if (key == "ability" && value is JsonObject) {
                    JsonObject(
                        (canonicalizeGrantedAbilities(value) as JsonObject) +
                            ("id" to JsonPrimitive("granted")),
                    )
                } else {
                    canonicalizeGrantedAbilities(value)
                }
            }
        )

        is JsonArray -> JsonArray(element.map(::canonicalizeGrantedAbilities))
        else -> element
    }

    /**
     * **Fold: a positional target reference is a named one.**
     *
     * `ContextTarget(i)` and `BoundVariable(id-of-requirement-i)` are the SDK's two ways of saying
     * "the target this spell declared", and the SDK says so itself — `BoundVariable` is documented as
     * "safer and more self-documenting than `ContextTarget(index)`", the same link written by name
     * instead of by position. Hand-written cards use both; the grammar always mints a name, because a
     * rule that spelled the index would have to know how many requirements the *card* ends up with.
     *
     * Rewriting the positional form into the named one — paired with the stamping in
     * [normalizeOwner], which gives every requirement its positional name whether or not the author
     * wrote an `id` — makes the comparison about *which requirement an effect reads*, which is the
     * thing that carries meaning. It stays closed on the case that matters: `ContextTarget(1)`
     * normalizes to `slot_1` and still diverges from anything reading `slot_0`.
     *
     * `Player.ContextPlayer(i)` is the same positional reference in a player-typed slot, and its
     * named form — `Player.BoundVariable`, what a card's `handle.asPlayer` builds — deliberately
     * shares `BoundVariable`'s serial name, so it folds the same way.
     */
    private fun positionalReference(element: JsonObject): JsonElement? {
        if ((element["type"] as? JsonPrimitive)?.content !in setOf("ContextTarget", "ContextPlayer")) return null
        val index = (element["index"] as? JsonPrimitive)?.content?.toIntOrNull() ?: return null
        return JsonObject(
            mapOf("type" to JsonPrimitive("BoundVariable"), "name" to JsonPrimitive(slotName(index)))
        )
    }

    /**
     * The hand-written side, as one set.
     *
     * `CardDefinition` carries a card's printed keywords in two fields — [CardDefinition.keywords]
     * holds the parameterless ones as bare `Keyword` constants and [CardDefinition.keywordAbilities]
     * holds the parameterized ones — while Assay parses everything into `KeywordAbility`. Lifting
     * the first into `KeywordAbility.Simple` is what makes the two sides comparable at all.
     *
     * That the SDK needs the lift is itself worth noticing: two spellings of one concept is the same
     * shape as the `PROTECTION_FROM_EACH_OPPONENT` finding Phase 1 reported.
     */
    internal fun printedKeywords(definition: CardDefinition): Set<KeywordAbility> =
        definition.keywords.map { KeywordAbility.Simple(it) }.toSet() + definition.keywordAbilities.toSet()
}

/**
 * The **fold list**: representations that are known to be equivalent, normalized away before the two
 * sides are compared.
 *
 * The design is explicit that this list is reviewed and never grown silently, because every entry is
 * a divergence the gate stops reporting — a fold added carelessly is how a semantic gate quietly
 * turns into a formality. Each one therefore has to say *why* it is not a difference, and the bar is
 * that both spellings are already agreed to mean the same thing somewhere outside this file.
 */
internal object Folds {

    fun apply(abilities: Set<KeywordAbility>): Set<KeywordAbility> = dropImpliedSimpleMarkers(abilities)

    /**
     * **Fields no printed line determines: art, and the strings a client shows a human.**
     *
     * The rule for this list is the one `Differential.canonicalizeAbilities` already applies to
     * `descriptionOverride` — "presentation, never executed" — read one level out, because the same
     * class of field turns up nested inside effects and not only on an ability:
     *
     * - **`imageUri`** is the token's *artwork*. The SDK calls it "Optional image URI for the token
     *   artwork" and, on `AnimateEffect`, "display-only". No Oracle text names a URL, so a parser can
     *   never produce one and a card that inlines one would diverge for ever. Twelve token-making
     *   cards reported here, all of them agreeing about the token and disagreeing about its picture.
     * - **`message`** is `LoseGameEffect`/`WinGameEffect`'s "message describing why they lost (shown
     *   in game-over screen)". Phage the Untouchable carries two, each a sentence *about* its own
     *   printed line rather than a reading of it.
     * - **`prompt`** is the label a decision shows its controller. Flow of Knowledge says
     *   "Choose two cards to discard" where the grammar builds "Choose 2 cards to discard": the same
     *   decision, spelled for a human two ways.
     * - **`selectedLabel`** and **`remainderLabel`** are `prompt`'s two siblings on the same object,
     *   and the SDK says so in the same words: "Label describing where selected cards go (e.g.,
     *   'Put on bottom'). **Shown in the UI.**" They were added when the top-of-library band made
     *   `SelectFromCollectionEffect` reachable from the grammar at all, and the four cards that
     *   surfaced them — Commune with Nature, Ashe, Boughside Wanderers, Casey Jones — agreed about
     *   every destination and disagreed only about how to caption it.
     *
     * - **`inlineOnTrigger`** is `Gate.MayDecide`'s placement flag: "the yes/no is rendered inline
     *   on the triggering permanent rather than as a centered modal … flows into
     *   `DecisionContext.inlineOnTrigger`". A rendering position, and one no printed line names —
     *   the same "may" sentence carries it on Angel's Tomb and not on Sanguine Statuette. Its
     *   siblings on the same gate stay compared, and they are the reason this is safe to drop:
     *   `sourceRequiredZone` decides whether the effect happens at all and `feasibility` decides
     *   whether the player is asked, so nothing that changes the outcome rides on this name.
     *
     *   Folding them loses nothing the gate was checking, and that is a property rather than a hope:
     *   `Patterns.Library.lookAtTopAndKeep` *derives* both labels from the two destinations
     *   (`defaultDestinationLabel`), and the destinations themselves are still compared. A label
     *   that disagreed because the destination did would still be caught, by the destination.
     *
     * The bar the fold list sets is "both spellings already agreed to mean the same thing somewhere
     * outside this file", and here that agreement is the SDK's own KDoc on each field. The narrowness
     * is that this drops *these names* and nothing else — a field that changes what the effect
     * does keeps diverging, including every sibling of these inside the same object.
     * `SelectFromCollectionEffect.showAllCards` is the nearest miss and stays compared: it decides
     * which cards the player is *shown*, not what they are told about them, and on a "look at the top
     * N cards" effect the text says all of them.
     */
    fun dropPresentation(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.filterKeys { it !in PRESENTATION_KEYS }.mapValues { dropPresentation(it.value) },
        )

        is JsonArray -> JsonArray(element.map(::dropPresentation))
        else -> element
    }

    private val PRESENTATION_KEYS =
        setOf(
            "imageUri", "message", "prompt", "selectedLabel", "remainderLabel",
            "descriptionOverride", "inlineOnTrigger",
        )

    /**
     * **A mode's `description` is the same class of field, and it is scoped rather than named.**
     *
     * `Mode.description` is documented as "Human-readable description of the mode" and defaults to
     * `effect.description` — presentation, never executed, derived by the SDK from the effect beside
     * it. That is exactly what [dropPresentation] drops, and the only reason it is not a fifth entry
     * in [PRESENTATION_KEYS] is that `description` is not a rare name: `ReplacementEffect`'s
     * `AlternativeCostEffect` carries a serialized one that *is* part of what the effect does, and a
     * key-wide drop would stop the gate seeing it. So this walks to the modes of a `ModalEffect` and
     * drops the field only there.
     *
     * The two sides disagree by construction rather than by accident, which is what makes this a
     * fold rather than a bug to fix. Hand-written cards spell the printed row out with the card's
     * own name in it — Boros Charm's first mode reads "Boros Charm deals 4 damage to target player
     * or planeswalker" — and the text Assay parses has had that name abstracted to `~` before any
     * rule sees it. The grammar therefore leaves the field at its default and never invents prose;
     * see `Modal.modeFor`. Everything that decides what a mode *does* — its effect, its targets, its
     * per-mode costs — is still compared, and the count fields on the modal above it are compared
     * too, which is where Winterflame's disagreement surfaced.
     */
    fun dropModeDescriptions(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> {
            val walked = JsonObject(element.mapValues { dropModeDescriptions(it.value) })
            val modes = walked["modes"] as? JsonArray
            if (modes != null && (walked["type"] as? JsonPrimitive)?.content == MODAL_TYPE) {
                JsonObject(walked + ("modes" to JsonArray(modes.map(::dropModeDescription))))
            } else {
                walked
            }
        }

        is JsonArray -> JsonArray(element.map(::dropModeDescriptions))
        else -> element
    }

    private fun dropModeDescription(mode: JsonElement): JsonElement =
        (mode as? JsonObject)?.let { JsonObject(it - "description") } ?: mode

    /** `ModalEffect`'s `@SerialName`. The discriminator is what keeps the fold above scoped. */
    private const val MODAL_TYPE = "Modal"

    // `liftTriggerConsent` used to live here, bridging `TriggeredAbility.optional = true` (106 cards)
    // and a `Effects.May` around the effect (214 cards) — two SDK spellings of "you may" that the
    // engine already lowered one into the other on every game. It is gone because the *SDK* is: the
    // flag was deleted and the gate is the model, so both sides now produce the same value and there
    // is nothing left to fold. That is the outcome a fold entry should be aiming at; the fold list
    // shrinks when the thing it was hiding gets fixed.

    /**
     * **A parameterless marker implied by a parameterized ability of the same keyword.**
     *
     * `CardDefinition` carries a parameterized keyword twice by design, and the SDK says so itself:
     * [KeywordAbility.keyword] is documented as existing "to automatically populate
     * `CardDefinition.keywords` so that parameterized keyword abilities (e.g., Ward {1}) are visible
     * in the base keyword set". So the parameter lives in `keywordAbilities` and a bare constant
     * lives in `keywords` — Punk Frogs, Frogmite and Teeka's Dragon are all written that way. The
     * Oracle text says it once, Assay reads it once, and the second copy is an index entry rather
     * than a second ability.
     *
     * Dropping the marker *only when a parameterized ability names the same keyword* is what keeps
     * this narrow: a card carrying a bare `WARD` and nothing else still diverges, which is the case
     * that would be a real bug.
     */
    private fun dropImpliedSimpleMarkers(abilities: Set<KeywordAbility>): Set<KeywordAbility> {
        val parameterized = abilities.filter { it !is KeywordAbility.Simple }.mapNotNull { it.keyword }.toSet()
        if (parameterized.isEmpty()) return abilities
        return abilities.filterNot { it is KeywordAbility.Simple && it.keyword in parameterized }.toSet()
    }

    /**
     * **A plain `CompositeEffect` nested inside another is the same sequence as its flattening.**
     *
     * `CompositeEffect` is an ordered run of effects and nothing else: with `stopOnError` false and
     * no `descriptionOverride`, the executor runs its members in order, so `[a, [b, c]]` and
     * `[a, b, c]` are the same value written two ways. The corpus writes it both ways for one
     * sentence — Cruel Tutor nests `Patterns.Library.searchLibrary` inside its outer composite while
     * Bitter Revelation splices the same recipe's steps into a flat one, and Angelic Blessing's
     * "gets +2/+2 and gains flying" is a two-element composite on its own and three flat elements
     * when a "Scry 1." follows it.
     *
     * The fold is narrow on purpose. A composite carrying `stopOnError`, a `descriptionOverride` or
     * description amounts says something about how its members run or read, so it is left alone and
     * a difference in one still diverges. Ordering is untouched: `[a, [c, b]]` flattens to
     * `[a, c, b]` and still disagrees with `[a, b, c]`, which is the difference worth catching.
     */
    fun flattenComposites(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> {
            val walked = JsonObject(element.mapValues { flattenComposites(it.value) })
            if (isPlainComposite(walked)) {
                // A plain composite of one effect runs exactly that effect — `Effects.Pipeline { move(…) }`
                // in a branch is the bare step it wraps.
                spliceMembers(walked).singleOrNull()
                    ?: JsonObject(walked + ("effects" to JsonArray(spliceMembers(walked))))
            } else {
                walked
            }
        }

        is JsonArray -> JsonArray(element.map(::flattenComposites))
        else -> element
    }

    /**
     * **Pipeline keys are names, not meaning.** A pipeline step writes a collection (or number, or
     * chosen value) under a key and a later step reads it back by that key; which string links the
     * two says nothing about the model. Kotlin cards write pipelines with `Effects.Pipeline { }`,
     * whose keys are generated (`gathered0`, `selected1`), while the grammar mints readable ones
     * (`graveyards`, `exiled`) — the same situation [normalizeSlotNames] already folds for target
     * slots. Each write gets a fresh positional name and each read takes the name of the latest
     * write of its key, in document order (a key written twice is two slots). An optional secondary
     * output nobody reads (`storeRemainder`, `storeNonMatching`, `storeMovedAs`) is dropped: the
     * builder only emits one when a later step asks for the handle, and an unread write changes
     * nothing. Keys a step reads but nothing in the tree wrote — engine-seeded collections, a
     * pattern's fixed output — are left as they are, so a difference in *those* still diverges.
     */
    fun renamePipelineKeys(element: JsonElement): JsonElement {
        val reads = PipelineKeyWalker(consumedIds = null).also { it.walk(element) }.consumed
        return PipelineKeyWalker(consumedIds = reads).walk(element)
    }

    private val PIPELINE_WRITERS = setOf(
        "storeAs", "storeSelected", "storeRemainder", "storeMatching", "storeNonMatching", "storeMatch",
        "storeRevealed", "storeChosenAs", "storeOtherAs", "storeMovedAs", "countVariable", "storeCastTo",
        "storeCountAs", "excessDamageVariable", "storeHeadsAs", "storeGuessedRightAs", "storeWinsAs",
        "storeDestroyedAs", "storeExiledAs", "storeDiscoveredAs",
    )
    private val OPTIONAL_WRITERS = setOf("storeRemainder", "storeNonMatching", "storeMovedAs")
    private val PIPELINE_READERS = setOf(
        "from", "collection", "collectionName", "variableName", "pileA", "pileB", "otherCollectionName",
        "originalCollection", "controllerSnapshot", "collections", "carryCollections", "fromChosenValueKey",
        "chosenSubtypeKey", "groupName", "listName", "chosenValueKey", "promptNameVariable",
    )

    /** One document-order pass; the second pass (with [consumedIds]) rewrites and drops. */
    private class PipelineKeyWalker(private val consumedIds: Set<Int>?) {
        private val current = HashMap<String, Int>()
        private var written = 0
        val consumed = HashSet<Int>()
        private val names = HashMap<Int, String>()

        private fun nameOf(id: Int) = names.getOrPut(id) { "pipeline_${names.size}" }

        private fun write(key: String): Int {
            written += 1
            current[key] = written
            return written
        }

        private fun read(value: String): String {
            val (base, suffix) = if (value.endsWith("_count") && value.removeSuffix("_count") in current) {
                value.removeSuffix("_count") to "_count"
            } else {
                value to ""
            }
            val id = current[base] ?: return value
            consumed += id
            return nameOf(id) + suffix
        }

        fun walk(element: JsonElement): JsonElement = when (element) {
            is JsonObject -> walkObject(element)
            is JsonArray -> JsonArray(element.map(::walk))
            else -> element
        }

        private fun walkObject(obj: JsonObject): JsonObject {
            val isStoreNumber = (obj["type"] as? JsonPrimitive)?.content == "StoreNumber"
            val out = LinkedHashMap<String, JsonElement>()
            for ((field, value) in obj) {
                if (field == "collectCollections") continue
                val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
                when {
                    text != null && (field in PIPELINE_WRITERS || (isStoreNumber && field == "name")) -> {
                        val id = write(text)
                        if (consumedIds != null && field in OPTIONAL_WRITERS && id !in consumedIds) continue
                        out[field] = JsonPrimitive(nameOf(id))
                    }
                    text != null && field in PIPELINE_READERS -> out[field] = JsonPrimitive(read(text))
                    value is JsonArray && field in PIPELINE_READERS -> out[field] = JsonArray(
                        value.map { item ->
                            val itemText = (item as? JsonPrimitive)?.takeIf { it.isString }?.content
                            if (itemText != null) JsonPrimitive(read(itemText)) else walk(item)
                        },
                    )
                    else -> out[field] = walk(value)
                }
            }
            // A per-iteration collection unioned into an aggregate: read the inner, write the outer.
            (obj["collectCollections"] as? JsonObject)?.let { collected ->
                out["collectCollections"] = JsonObject(
                    collected.entries.associate { (inner, aggregate) ->
                        val innerName = read(inner)
                        val aggregateId = write((aggregate as JsonPrimitive).content)
                        innerName to JsonPrimitive(nameOf(aggregateId))
                    },
                )
            }
            return JsonObject(out)
        }
    }

    /** A composite with nothing said about how it runs or reads — the only shape safe to splice. */
    private fun isPlainComposite(element: JsonObject): Boolean =
        (element["type"] as? JsonPrimitive)?.content == "Composite" &&
            element["effects"] is JsonArray &&
            element.keys.none { it == "stopOnError" || it == "descriptionOverride" || it == "descriptionAmounts" }

    private fun spliceMembers(composite: JsonObject): List<JsonElement> =
        (composite.getValue("effects") as JsonArray).flatMap { member ->
            if (member is JsonObject && isPlainComposite(member)) {
                (member.getValue("effects") as JsonArray).toList()
            } else {
                listOf(member)
            }
        }
}

/** Why a hand-written card was or was not compared. The denominator is never hidden. */
enum class Population {
    /** Compared — Assay read the whole card and the golden decoded. */
    COMPARED,

    /** Assay does not yet read every line of this card. Not a bug; the grammar has not reached it. */
    NOT_COVERED,

    /** Multi-face: out of scope for the keyword comparison, see [Differential.compare]. */
    MULTI_FACE,

    /**
     * Assay reads every line, but the hand-written card puts content in a `CardScript` slot the
     * grammar cannot yet produce — typically a keyword the SDK lowers to a triggered ability at
     * authoring time. Not compared, because confirming it would claim a check nobody performed.
     */
    SCRIPT_NOT_MODELLED,

    /**
     * Assay reads every line, but the lines do not fold into one card: two of them are spell
     * effects and a `CardScript` has one. The card prints a sequence the grammar has no rule for.
     */
    LINES_DO_NOT_FOLD,

    /** No Scryfall Oracle entry joined — a `custom/` card, or a name the index does not carry. */
    NO_ORACLE_TEXT,

    /**
     * An entry joined, but its Oracle text is not the text the golden was authored from. Either the
     * name join found the wrong card or the wording has changed since. Not compared: Assay would be
     * reading one card and diffing another.
     */
    ORACLE_TEXT_DIFFERS,

    /** The golden JSON would not decode. Always a bug, in the SDK or in a stale snapshot. */
    UNDECODABLE,
}

enum class Verdict {
    /** Assay's model and the hand-written model agree. */
    CONFIRMED,

    /** They disagree. Either a parser bug or a bug in the hand-written card — both worth finding. */
    DIVERGENT,
}

data class CardComparison(
    val implemented: ImplementedCard,
    val oracle: OracleCard?,
    val population: Population,
    val verdict: Verdict? = null,
    /** Keyword abilities Assay read from the text that the hand-written card does not carry. */
    val onlyInText: List<KeywordAbility> = emptyList(),
    /** Keyword abilities the hand-written card carries that Assay did not read from the text. */
    val onlyInCard: List<KeywordAbility> = emptyList(),
    /** Set only when the scripts disagree: Assay's reading, and the hand-written one. */
    val textScript: CardScript? = null,
    val cardScript: CardScript? = null,
    /**
     * The equip costs, when they disagree. Both are nullable *and* the disagreement is a third
     * field, because "the card has one and the text does not" is the interesting case and it is the
     * one a pair of nulls cannot tell from agreement.
     */
    val textEquipCost: ManaCost? = null,
    val cardEquipCost: ManaCost? = null,
    val equipCostsAgree: Boolean = true,
    /**
     * The characteristic-defining halves of the stat box, when they disagree — same three-field
     * shape as the equip cost above, and for the same reason: "the card defines a `*` the text does
     * not" is the interesting case and a pair of nulls cannot tell it from agreement.
     */
    val textStats: String? = null,
    val cardStats: String? = null,
    val statsAgree: Boolean = true,
)

/**
 * The differential's numbers, in the shape [FinenessReport] uses: counters plus bounded examples,
 * so a whole-corpus run costs a fixed amount of memory.
 */
class DifferentialReport private constructor(
    val cards: Int,
    val byPopulation: Map<Population, Int>,
    val confirmed: Int,
    val divergent: Int,
    val divergences: List<CardComparison>,
    val undecodable: List<String>,
) {

    /** Compared cards that agreed, in parts per thousand — the differential's own fineness. */
    val agreement: Double get() = FinenessReport.permil(confirmed, confirmed + divergent)

    /**
     * The gate is red on a golden that will not decode, never on a divergence.
     *
     * A divergence is a *finding* — it has to be read and classified as parser bug, card bug, or
     * known fold, and until someone has done that, failing the build would only teach people to
     * ignore it. The MVP's acceptance is "every divergence classified", which is a human's verdict,
     * not a counter's.
     */
    val clean: Boolean get() = undecodable.isEmpty()

    fun render(topDivergences: Int = 40): String = buildString {
        appendLine(
            "Argentum Assay — differential " +
                "(${CardFragment.MODELLED_SLOTS_NOTE}, keywords, equipCost, defined P/T)"
        )
        appendLine("=".repeat(78))
        appendLine()
        appendLine(row("Hand-written cards", cards.toString()))
        appendLine(row("  compared", pop(Population.COMPARED).toString()))
        appendLine(row("  not yet covered by the grammar", pop(Population.NOT_COVERED).toString()))
        appendLine(row("  script slot not modelled yet", pop(Population.SCRIPT_NOT_MODELLED).toString()))
        appendLine(row("  lines do not fold into one card", pop(Population.LINES_DO_NOT_FOLD).toString()))
        appendLine(row("  multi-face (out of scope)", pop(Population.MULTI_FACE).toString()))
        appendLine(row("  no Scryfall Oracle entry", pop(Population.NO_ORACLE_TEXT).toString()))
        appendLine(row("  Oracle text differs from golden", pop(Population.ORACLE_TEXT_DIFFERS).toString()))
        appendLine(row("  golden would not decode", "${pop(Population.UNDECODABLE)}   ${note(undecodable)}"))
        appendLine()
        appendLine(row("Confirmed — models agree", "$confirmed   ${permilText(agreement)}"))
        appendLine(row("DIVERGENT — read every one", divergent.toString()))

        if (undecodable.isNotEmpty()) {
            appendLine()
            appendLine("GOLDENS THAT WOULD NOT DECODE (must be 0)")
            appendLine("-".repeat(78))
            undecodable.forEach { appendLine("  $it") }
        }

        appendLine()
        appendLine("DIVERGENCES — each is a parser bug, a card bug, or a fold. Classify all of them.")
        appendLine("-".repeat(78))
        if (divergences.isEmpty()) {
            appendLine("  (none)")
        } else {
            divergences.take(topDivergences).forEach { d ->
                appendLine("  ${d.implemented.name}  [${d.implemented.setCode}]")
                if (d.onlyInText.isNotEmpty()) {
                    appendLine("    text has, card lacks:  ${d.onlyInText.joinToString(", ", transform = ::structural)}")
                }
                if (d.onlyInCard.isNotEmpty()) {
                    appendLine("    card has, text lacks:  ${d.onlyInCard.joinToString(", ", transform = ::structural)}")
                }
                if (d.textScript != null || d.cardScript != null) {
                    appendLine("    script from text:      ${d.textScript?.let(::structural) ?: "(none)"}")
                    appendLine("    script on the card:    ${d.cardScript?.let(::structural) ?: "(none)"}")
                }
                if (!d.equipCostsAgree) {
                    appendLine("    equip cost from text:  ${d.textEquipCost ?: "(none)"}")
                    appendLine("    equip cost on card:    ${d.cardEquipCost ?: "(none)"}")
                }
                if (!d.statsAgree) {
                    appendLine("    defined P/T from text: ${d.textStats ?: "(none)"}")
                    appendLine("    defined P/T on card:   ${d.cardStats ?: "(none)"}")
                }
            }
            if (divergent > divergences.size) {
                appendLine("  … and ${divergent - divergences.size} more (examples are capped)")
            } else if (divergences.size > topDivergences) {
                appendLine("  … and ${divergences.size - topDivergences} more; raise --top to see them")
            }
        }
    }

    private fun pop(p: Population) = byPopulation[p] ?: 0

    /**
     * A divergence row shows the **structure**, not `KeywordAbility.description`.
     *
     * The prose is what makes a divergence unreadable exactly where it matters most: where the SDK
     * spells one concept two ways, both sides describe themselves the same way and the row looks
     * like a tool bug. That is not hypothetical — it is how the flanking finding presented, with
     * `Simple(keyword=FLANKING)` on one side and a dedicated `Flanking` object on the other, both
     * of them saying "Flanking". `toString()` on the data class is what made the difference visible.
     */
    private fun structural(ability: KeywordAbility) = ability.toString()

    /**
     * Scripts print as their serialized form rather than as `toString()`. An effect tree's
     * `toString()` is a wall of nested data-class names that is unreadable at the width a report
     * row has; the JSON is the same shape a golden shows, so a divergence row can be compared
     * against the golden directly.
     */
    private fun structural(script: CardScript) =
        CardSerialization.json.encodeToString(CardScript.serializer(), script)
    private fun note(items: List<*>) = if (items.isEmpty()) "" else "<- READ THESE"
    private fun row(label: String, value: String) = "  %-34s %s".format(Locale.ROOT, label, value).trimEnd()
    private fun permilText(value: Double) = "%.1f‰ (%.1f%%)".format(Locale.ROOT, value, value / 10.0)

    companion object {
        fun builder() = Builder()
    }

    class Builder {
        private var cards = 0
        private var confirmed = 0
        private var divergent = 0
        private val byPopulation = mutableMapOf<Population, Int>()
        private val divergences = mutableListOf<CardComparison>()
        private val undecodable = mutableListOf<String>()

        fun add(comparison: CardComparison) = apply {
            cards++
            byPopulation.merge(comparison.population, 1, Int::plus)
            when (comparison.verdict) {
                Verdict.CONFIRMED -> confirmed++
                Verdict.DIVERGENT -> {
                    divergent++
                    if (divergences.size < MAX_EXAMPLES) divergences.add(comparison)
                }

                null -> Unit
            }
            if (comparison.population == Population.UNDECODABLE && undecodable.size < MAX_EXAMPLES) {
                undecodable.add("${comparison.implemented.name} [${comparison.implemented.setCode}]")
            }
        }

        fun build() = DifferentialReport(
            cards = cards,
            byPopulation = byPopulation.toMap(),
            confirmed = confirmed,
            divergent = divergent,
            divergences = divergences.toList(),
            undecodable = undecodable.toList(),
        )

        private companion object {
            /**
             * Divergences are meant to be read one by one, so the cap is generous where the
             * fineness report's is tight — but it is still a cap, because a systematic parser bug
             * would otherwise print thousands of identical rows and bury the interesting ones.
             */
            const val MAX_EXAMPLES = 300
        }
    }
}
