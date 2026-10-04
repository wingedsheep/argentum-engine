package com.wingedsheep.assay.grammar

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.serialization.CardSerialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * What one ability line contributes to a card.
 *
 * A card is `CardDefinition`, whose two behavioural slots are `keywordAbilities` and `script`. A
 * *line* fills part of one of them, so this type is those two slots and nothing else — the whole
 * card is the [merge] of its lines' fragments.
 *
 * **This is not an Assay IR.** The design's rule is that the grammar parses straight into `mtg-sdk`
 * types, and it does: every value inside a fragment is an SDK `KeywordAbility` or an SDK
 * `CardScript`. What this class adds is not a representation of meaning but a statement of *where in
 * the card* a line's meaning goes — the same information `CardDefinition` already carries, narrowed
 * to the two fields a line can reach. Nothing here is ever translated; it is destructured.
 *
 * The unit stays the **line** rather than the card for the reason [Grammar] gives: line grouping is
 * a property of the printed text, which normalization owns, so the model must not encode it.
 */
data class CardFragment(
    val keywordAbilities: List<KeywordAbility> = emptyList(),
    val script: CardScript = CardScript.EMPTY,
    /**
     * `CardDefinition.flags` — the third behavioural slot, and the only one that is not a keyword or
     * part of the script.
     *
     * It exists because "This creature can't be blocked." is `AbilityFlag.CANT_BE_BLOCKED` rather
     * than a `StaticAbility`, so a line can reach a field neither of the other two slots covers.
     * That the SDK has two places to say a combat restriction — a flag for the unconditional form
     * and a static for every filtered one — is a finding rather than something this type should
     * paper over, and holding both is what lets the differential see it.
     */
    val flags: Set<AbilityFlag> = emptySet(),
    /**
     * `CardDefinition.equipCost` — the fourth behavioural slot, and the second one outside the
     * script.
     *
     * "Equip {1}" is a keyword ability the SDK **lowers** at authoring time rather than storing: the
     * card gets this field *and* the activated ability `ActivatedAbility.equip` builds, so one
     * printed line fills two slots in two different objects. That is the same shape
     * [Grammar.amplifyLine] has — a keyword plus a replacement effect — and the same reason this type
     * grew a field for it: the fragment is the only place a line's two contributions can meet.
     *
     * The field is not redundant with the ability beside it. `CardValidator` requires an Equipment
     * type line wherever it is set and `CardLinter` reads it to decide whether a permanent can ever
     * attach, so a card carrying the ability without the cost is a different — and worse — card than
     * one carrying both. Holding it here is what lets the differential see that.
     */
    val equipCost: ManaCost? = null,
    /**
     * `CardDefinition.creatureStats`, one characteristic at a time — the fifth behavioural slot, and
     * the first one that is not behaviour at all.
     *
     * "~'s power and toughness are each equal to the number of lands you control." is a
     * **characteristic-defining ability** (CR 604.3), and the SDK spells it where the card prints
     * it: in the stat box, as `CreatureStats(CharacteristicValue.Dynamic(…), …)`. So the line does
     * not produce an ability at all — it produces the value behind the `*` in the header, which is
     * the one direction Assay's line/header split does not otherwise run.
     *
     * Two fields rather than one `CreatureStats` because a card can print the two halves on
     * **separate lines**: Yavimaya Kavu defines its power from red creatures and its toughness from
     * green ones, in two sentences. A `CreatureStats` needs both at once and neither line has both,
     * so the fold is per characteristic and the compiler is what pairs them with the printed header.
     *
     * The SDK's *other* spelling — `SetBasePowerToughnessDynamicStatic`, a Layer 7b static — is
     * deliberately never emitted here. The corpus writes the header form 49 times against the
     * static's 12, and the static exists for the CDA a card grants to *something else* (a token's
     * quoted text, a granted body). One printed form per model: the header wins in header position,
     * and that split is a finding rather than a preference.
     */
    val dynamicPower: CharacteristicValue? = null,
    /** [dynamicPower]'s other half; see its KDoc for why the two are separate fields. */
    val dynamicToughness: CharacteristicValue? = null,
) {

    /**
     * Fold two lines' contributions together, or **null** when they cannot be one card.
     *
     * Only the slots the grammar can currently produce are combined. Two lines that both claim to be
     * *the* spell effect is the collision: a `CardScript` has one `spellEffect`, and a card printing
     * two effect paragraphs means a sequence the grammar has no rule for yet. Neither keeping the
     * first nor concatenating them is honest — the first drops meaning, the second invents an order
     * nothing checked — so the fold declines and the caller counts the card.
     *
     * It used to throw, on the reading that a collision could only be a grammar bug. It stopped
     * being one the moment [Steps] could read a second kind of sentence, and a gate that crashes on
     * a card it does not model is the one behaviour "declining is success" rules out.
     *
     * Widen this as the grammar reaches new slots; the compiler will not remind you, but
     * [Companion.MODELLED_SLOTS_NOTE] says where to look.
     */
    fun merge(other: CardFragment): CardFragment? {
        if (script.spellEffect != null && other.script.spellEffect != null) return null
        // An Aura declares one attachment restriction, so two lines both spelling "Enchant …" is the
        // same collision as two spell effects: a card the grammar has misread, or a card shape it
        // has no model for. Neither line may be dropped silently.
        if (script.auraTarget != null && other.script.auraTarget != null) return null
        // …and a card declares one equip cost, for the same reason. Two "Equip" lines on one card is
        // a shape the SDK cannot hold — `CardDefinition.equipCost` is one field — so the fold
        // declines and the card is counted rather than losing the second one silently.
        if (equipCost != null && other.equipCost != null) return null
        // …and a creature has one power and one toughness. Two lines each defining the *same*
        // characteristic is the same collision one field over: a card whose text Assay has misread,
        // since nothing in the rules lets two CDAs define one characteristic (CR 604.3 layers them,
        // and a card printing both would be a card this fold has no model for). Two lines defining
        // *different* characteristics is Yavimaya Kavu and folds normally.
        if (dynamicPower != null && other.dynamicPower != null) return null
        if (dynamicToughness != null && other.dynamicToughness != null) return null
        // …and a card declares one conditional-flash clause. Same collision, same reason: the field
        // is singular, so two lines both claiming it is a card this fold has no model for.
        if (script.conditionalFlash != null && other.script.conditionalFlash != null) return null
        return CardFragment(
            keywordAbilities = keywordAbilities + other.keywordAbilities,
            flags = flags + other.flags,
            equipCost = equipCost ?: other.equipCost,
            dynamicPower = dynamicPower ?: other.dynamicPower,
            dynamicToughness = dynamicToughness ?: other.dynamicToughness,
            script = CardScript(
                spellEffect = script.spellEffect ?: other.script.spellEffect,
                targetRequirements = script.targetRequirements + other.script.targetRequirements,
                // A spell states its casting restrictions and its additional costs on lines of their
                // own, so both accumulate across lines exactly as the ability lists do.
                castRestrictions = script.castRestrictions + other.script.castRestrictions,
                additionalCosts = script.additionalCosts + other.script.additionalCosts,
                // Triggered abilities are a list on purpose: one card, several trigger lines, in
                // printed order. Unlike the spell effect there is nothing to collide over. The same
                // holds for activated abilities — and a *single* line can contribute several of
                // them, since "{T}: Add {B} or {G}." is two — for static abilities, which is how an
                // aura's two payoff lines fold, and for replacement effects.
                triggeredAbilities = script.triggeredAbilities + other.script.triggeredAbilities,
                // CR 603.8's state triggers are a list of their own in the SDK, beside the event
                // triggers rather than inside them, and accumulate for the same reason.
                stateTriggeredAbilities = script.stateTriggeredAbilities + other.script.stateTriggeredAbilities,
                activatedAbilities = script.activatedAbilities + other.script.activatedAbilities,
                staticAbilities = script.staticAbilities + other.script.staticAbilities,
                replacementEffects = script.replacementEffects + other.script.replacementEffects,
                auraTarget = script.auraTarget ?: other.script.auraTarget,
                // "This spell can't be countered." is a line of its own on Root Sliver and Vexing
                // Beetle, so it accumulates like the ability lists rather than colliding.
                cantBeCountered = script.cantBeCountered || other.script.cantBeCountered,
                // "This spell has flash as long as …" — one field, so the fold above has already
                // refused the two-line case and this is a plain first-wins.
                conditionalFlash = script.conditionalFlash ?: other.script.conditionalFlash,
            ),
        )
    }

    /**
     * CR 607's linked abilities, resolved once the whole card is in hand — the one derivation this
     * type makes, and it is here because the evidence for it is on a *different line*.
     *
     * "Exile a card from a graveyard." says nothing about linkage. "…if it shares a card type with
     * **the exiled card**", "…cards exiled **with this creature**" is what makes the pair linked
     * (CR 607.2), and the SDK carries the fact twice: on the read
     * ([com.wingedsheep.sdk.scripting.targets.EffectTarget.LinkedExiledCard],
     * [com.wingedsheep.sdk.scripting.CostReductionSource.SharedCardTypesWithLinkedExile]) and on
     * the move that fills the pile (`MoveCollectionEffect.linkToSource`). This module's rule for a
     * value the SDK carries twice is to **derive** it rather than spell it, and no single line can:
     * the exile line has no evidence and the payoff line has no move. So the derivation runs on the
     * fold, which is the first place both are visible — the same reason
     * [Grammar]'s line/header split puts the `*` pairing in the compiler.
     *
     * The read side is a *search* and the write side is a *rewrite*, and both are deliberately
     * shallow: they descend the two wrappers a printed clause builds — a `Composite` for a sequence
     * and a `GatedEffect` for "you may" and the intervening-if — exactly as
     * [Recursion.functionsIn] does, and no further. A rule that produced an exile inside an
     * iteration would need this widened, and would be the change that widens it.
     *
     * Nothing happens on a card with no imprint read, which is every card in the corpus but the
     * imprint permanents and the cemetery cycle: [needsLinkedExile] is false and the fragment is
     * returned unchanged.
     */
    fun deriveExileLinkage(): CardFragment {
        if (!needsLinkedExile()) return this
        return copy(script = script.copy(
            spellEffect = script.spellEffect?.let(::linkExiles),
            triggeredAbilities = script.triggeredAbilities.map { it.copy(effect = linkExiles(it.effect)) },
            activatedAbilities = script.activatedAbilities.map { it.copy(effect = linkExiles(it.effect)) },
        ))
    }

    /** Whether any part of this card reads a linked-exile pile; see [deriveExileLinkage]. */
    private fun needsLinkedExile(): Boolean =
        mentionsLinkedExile(
            CardSerialization.json.parseToJsonElement(
                CardSerialization.json.encodeToString(CardScript.serializer(), script),
            )
        )

    private fun mentionsLinkedExile(element: JsonElement): Boolean = when (element) {
        is JsonObject -> element.values.any(::mentionsLinkedExile)
        is JsonArray -> element.any(::mentionsLinkedExile)
        is JsonPrimitive -> element.isString && element.content in LINKED_EXILE_READERS
        else -> false
    }

    /** Set `linkToSource` on every card-to-exile move in [effect]; see [deriveExileLinkage]. */
    private fun linkExiles(effect: Effect): Effect = when (effect) {
        is MoveCollectionEffect ->
            if ((effect.destination as? CardDestination.ToZone)?.zone == Zone.EXILE) {
                effect.copy(linkToSource = true)
            } else {
                effect
            }

        is CompositeEffect -> effect.copy(effects = effect.effects.map(::linkExiles))
        is GatedEffect -> effect.copy(then = linkExiles(effect.then))
        else -> effect
    }

    val isEmpty: Boolean
        get() = keywordAbilities.isEmpty() && flags.isEmpty() && equipCost == null &&
            dynamicPower == null && dynamicToughness == null && script == CardScript.EMPTY

    companion object {
        val EMPTY = CardFragment()

        fun of(keywords: List<KeywordAbility>) = CardFragment(keywordAbilities = keywords)

        fun of(script: CardScript) = CardFragment(script = script)

        /**
         * The `CardScript` slots the grammar can currently produce, and therefore the only ones the
         * differential is entitled to compare. Kept as one list so [merge] and
         * `Differential.compare`'s completeness check cannot drift apart — adding a slot to the
         * grammar means adding it in both places, and this note is the pointer between them.
         */
        /**
         * The `@SerialName`s of the SDK values that *read* a linked-exile pile; see
         * [deriveExileLinkage].
         *
         * Matched by discriminator rather than by walking the typed tree, for the reason the
         * differential's own folds are: the readers turn up in a `CardPredicate`, in a
         * `CostReductionSource`, in an `EffectTarget` and in a `DynamicAmount`, and a typed search
         * would be four walks over four sealed hierarchies that share nothing but this question.
         * The names are the SDK's own and a rename breaks the serialized corpus first, so this list
         * cannot drift silently.
         */
        private val LINKED_EXILE_READERS =
            setOf("LinkedExiledCard", "SharedCardTypesWithLinkedExile", "ExiledWithSource")

        const val MODELLED_SLOTS_NOTE =
            "spellEffect, targetRequirements, triggeredAbilities, stateTriggeredAbilities, activatedAbilities, " +
                "staticAbilities, replacementEffects, auraTarget, castRestrictions, additionalCosts, " +
                "cantBeCountered, conditionalFlash"
    }
}
