package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.spliceKeyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * Single source of truth for "which cards in hand can be spliced onto the spell being cast, what does
 * that add to its cost, and what text does it add?" — shared by
 * [com.wingedsheep.engine.legalactions.enumerators.CastSpellEnumerator], the cast handler's
 * validate/execute paths, and [com.wingedsheep.engine.mechanics.stack.StackResolver].
 *
 * Splice (CR 702.47a) is a static ability that functions while the card is **in hand**: "You may
 * reveal this card from your hand as you cast a [quality] spell. If you do, that spell gains the text
 * of this card's rules text and you pay [cost] as an additional cost to cast that spell."
 *
 * Four consequences shape every read site:
 *
 *  - **The spliced card is never cast and never leaves hand.** It is only revealed. So it stays
 *    castable later, splice-able onto a different spell later, and — per the CR's own example — can
 *    even be discarded to pay a "discard a card" cost of the very spell it was spliced onto. Nothing
 *    here moves a card between zones.
 *  - **What the spell gains is text, not characteristics** (CR 702.47c). The spell keeps its own name,
 *    colour, and types, so a blue Arcane spell with a red splice card's damage text is still blue and
 *    can still hit a creature with protection from red. This falls out of resolving the spliced effect
 *    with the *spell's* entity as its source.
 *  - **The main spell's effects happen first** (CR 702.47b), then each spliced card's text in the
 *    order the caster chose. Splicing the same card onto one spell twice is illegal.
 *  - **The splice cost is an additional cost** (CR 702.47a → 601.2b, 601.2f–h), so it stacks on top of
 *    the spell's own cost and on top of any alternative cost paying for it.
 *
 * Splice changes are lost as soon as the spell leaves the stack (CR 702.47e) — no cleanup code, since
 * the choice rides the stack object and dies with it.
 *
 * There is no runtime-grant source: no card grants splice to another, so a printed keyword is the only
 * input.
 */
object SpliceCasts {

    /** The printed splice keyword on [cardDef], or null when it has none. */
    fun printedSplice(cardDef: CardDefinition?): KeywordAbility.Splice? = cardDef?.spliceKeyword()

    /**
     * Whether a spell with [spellSubtypes] carries the "[quality]" a splice ability splices onto
     * (CR 702.47a) — in practice always Arcane. Compared on the subtype's string value so it matches
     * however the card's type line spelled it.
     */
    fun qualityMatches(splice: KeywordAbility.Splice, spellSubtypes: Collection<String>): Boolean =
        spellSubtypes.any { it.equals(splice.onto.value, ignoreCase = true) }

    /**
     * Cards in [playerId]'s hand that could be revealed and spliced onto a spell with
     * [spellSubtypes], paired with their splice ability. [castCardId] — the card being cast — is
     * excluded: it is on its way to the stack, not in hand to be revealed.
     *
     * Hand is read from base state, not projected state: splice functions in hand, a zone the layer
     * system doesn't project.
     */
    fun candidates(
        state: GameState,
        playerId: EntityId,
        castCardId: EntityId,
        spellSubtypes: Collection<String>,
        cardRegistry: CardRegistry,
    ): List<SpliceCandidate> {
        if (spellSubtypes.isEmpty()) return emptyList()
        return state.getZone(ZoneKey(playerId, Zone.HAND))
            .filter { it != castCardId }
            .mapNotNull { cardId ->
                val name = state.getEntity(cardId)?.get<CardComponent>()?.name ?: return@mapNotNull null
                val cardDef = cardRegistry.getCard(name) ?: return@mapNotNull null
                val splice = printedSplice(cardDef) ?: return@mapNotNull null
                if (!qualityMatches(splice, spellSubtypes)) return@mapNotNull null
                SpliceCandidate(cardId = cardId, name = name, definition = cardDef, splice = splice)
            }
    }

    /**
     * [baseCost] plus the splice cost of every card in [splicedCardIds], in order — the spell's total
     * cost once the splices are announced (CR 601.2b/f). Ids that aren't splice-able are skipped here;
     * the handler's validate() is what rejects them.
     */
    fun addSpliceCosts(
        baseCost: ManaCost,
        state: GameState,
        splicedCardIds: List<EntityId>,
        cardRegistry: CardRegistry,
    ): ManaCost = splicedCardIds.fold(baseCost) { cost, cardId ->
        val splice = spliceOf(state, cardId, cardRegistry) ?: return@fold cost
        cost + splice.cost
    }

    /** The splice ability of the card [cardId] currently names, or null when it has none. */
    fun spliceOf(state: GameState, cardId: EntityId, cardRegistry: CardRegistry): KeywordAbility.Splice? {
        val name = state.getEntity(cardId)?.get<CardComponent>()?.name ?: return null
        return printedSplice(cardRegistry.getCard(name))
    }

    /**
     * The target requirements the spliced cards contribute, in splice order (CR 702.47d — "choose
     * targets for the added text normally"). Appended after the main spell's own requirements, so the
     * flat cast-time target list splits cleanly into the main spell's slice followed by one slice per
     * spliced card.
     */
    fun targetRequirementsFor(
        state: GameState,
        splicedCardIds: List<EntityId>,
        cardRegistry: CardRegistry,
    ): List<TargetRequirement> = splicedCardIds.flatMap { cardId ->
        definitionOf(state, cardId, cardRegistry)?.script?.targetRequirements ?: emptyList()
    }

    /** The card definition [cardId] currently names, or null. */
    fun definitionOf(state: GameState, cardId: EntityId, cardRegistry: CardRegistry): CardDefinition? {
        val name = state.getEntity(cardId)?.get<CardComponent>()?.name ?: return null
        return cardRegistry.getCard(name)
    }

    /**
     * How many target slots each spliced card's text occupies, in splice order.
     *
     * Counted by the requirement's **declared** `count`, not by how many targets the player actually
     * supplied — that is the same arithmetic
     * [com.wingedsheep.engine.handlers.EffectContext.buildNamedTargets] uses to walk a flat target list
     * (`targetIndex += req.count`), so a slice computed here lines up with how the effect will read it.
     * The single definition behind both [sliceSplicedTargets] and [mainTargetCount], so the two can't
     * drift into disagreeing about where the main spell's targets end.
     */
    fun splicedTargetSlotCounts(
        splicedCardNames: List<String>,
        cardRegistry: CardRegistry,
    ): List<Int> = splicedCardNames.map { name ->
        cardRegistry.getCard(name)?.script?.targetRequirements?.sumOf { it.count } ?: 0
    }

    /**
     * Split [flatTargets] — the cast's whole target list, laid out as the main spell's targets
     * followed by each spliced card's — into one slice per entry of [splicedCardNames].
     */
    fun <T> sliceSplicedTargets(
        flatTargets: List<T>,
        splicedCardNames: List<String>,
        cardRegistry: CardRegistry,
    ): List<List<T>> {
        val counts = splicedTargetSlotCounts(splicedCardNames, cardRegistry)
        var index = mainTargetCount(flatTargets.size, splicedCardNames, cardRegistry)
        return counts.map { count ->
            val slice = flatTargets.drop(index).take(count)
            index += count
            slice
        }
    }

    /** How many of a cast's flat targets belong to the main spell rather than to a spliced card. */
    fun mainTargetCount(
        totalTargets: Int,
        splicedCardNames: List<String>,
        cardRegistry: CardRegistry,
    ): Int = (totalTargets - splicedTargetSlotCounts(splicedCardNames, cardRegistry).sum())
        .coerceAtLeast(0)
}

/** A card in hand that could be revealed and spliced onto the spell being cast (CR 702.47a). */
data class SpliceCandidate(
    val cardId: EntityId,
    val name: String,
    val definition: CardDefinition,
    val splice: KeywordAbility.Splice,
)
