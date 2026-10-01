package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantEmergeToOwnSpells
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "can this card be emerge-cast, which creatures can pay for it, and
 * what does it actually cost?" — used by [com.wingedsheep.engine.legalactions.enumerators.EmergeCastEnumerator]
 * and by the cast handler's validate/execute paths.
 *
 * Emerge (CR 702.119a) is two static abilities that function while the spell is on the stack:
 * "You may cast this spell by paying [cost] and sacrificing a creature rather than paying its mana
 * cost", and "if you chose to pay this spell's emerge cost, its total cost is reduced by an amount
 * of **generic** mana equal to the sacrificed creature's mana value."
 *
 * Three consequences shape every read site:
 *
 *  - The reduction is generic-only. A creature whose mana value exceeds the generic portion of the
 *    emerge cost doesn't reduce the colored pips and the excess is simply wasted — so affordability
 *    has to be recomputed *per candidate creature*, not once for the spell.
 *  - The creature is chosen as you choose to pay the emerge cost (CR 601.2b) but sacrificed as you
 *    pay the total cost (CR 601.2h), i.e. *after* mana abilities are activated. It is therefore
 *    still available to be tapped for mana toward its own emerge cost, and the handler sacrifices
 *    it only once the mana payment has gone through.
 *  - Emerge grants no timing permission of its own — the spell is cast at its normal timing, which
 *    for Elder Deep-Fiend means flash.
 *
 * Emerge is printed ([KeywordAbility.Emerge]) or granted by a battlefield static
 * ([GrantEmergeToOwnSpells] — Herigast's "each creature spell you cast has emerge", priced at the
 * spell's own mana cost). Every read site goes through [effectiveEmerge] so the two behave
 * identically.
 */
object EmergeCasts {

    /** The printed emerge keyword on [cardDef], or null when it has none. */
    fun printedEmerge(cardDef: CardDefinition?): KeywordAbility.Emerge? =
        cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Emerge>()?.firstOrNull()

    /**
     * The emerge [cardId] has when [playerId] casts it, or null when it has none. A printed emerge
     * wins; otherwise the first [GrantEmergeToOwnSpells] on a permanent [playerId] controls whose
     * filter matches the card supplies a synthetic plain emerge priced at the card's mana cost.
     *
     * Printed-first mirrors every other printed-or-granted alternative cost ([WarpGrants],
     * [MiracleGrants]). When both apply, the rules let the caster pick either emerge cost; the
     * printed one is the one offered, since a printed emerge is the card's own discount.
     */
    fun effectiveEmerge(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?,
        playerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): KeywordAbility.Emerge? {
        printedEmerge(cardDef)?.let { return it }
        if (cardDef == null) return null
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        // Controlled view, so the grant follows whoever controls the granter (CR 109.5).
        for (permanentId in state.controlledBattlefield(playerId)) {
            val source = state.getEntity(permanentId)?.get<CardComponent>() ?: continue
            val sourceDef = cardRegistry.getCard(source.cardDefinitionId) ?: continue
            for (ability in sourceDef.script.staticAbilities) {
                if (ability !is GrantEmergeToOwnSpells) continue
                if (predicateEvaluator.matches(state, projected, cardId, ability.spellFilter, context)) {
                    return KeywordAbility.Emerge(cardDef.manaCost)
                }
            }
        }
        return null
    }

    /**
     * Permanents [playerId] controls that could be sacrificed to pay [emerge] — creatures for plain
     * emerge (CR 702.119a), or permanents of the named quality for "emerge from [quality]"
     * (CR 702.119b). No further restriction: tapped and summoning-sick permanents qualify. Read
     * through projected state so animated lands and type-changing effects count.
     */
    fun sacrificeCandidates(
        state: GameState,
        playerId: EntityId,
        emerge: KeywordAbility.Emerge,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        return projected.getBattlefieldControlledBy(playerId).filter {
            predicateEvaluator.matches(state, projected, it, emerge.sacrificeFilter, context)
        }
    }

    /**
     * The mana value the emerge reduction is measured against (CR 702.119a). A permanent's mana
     * value comes from its own mana cost, so a token or a card with no mana cost contributes 0.
     */
    fun manaValueOf(state: GameState, permanentId: EntityId): Int =
        state.getEntity(permanentId)?.get<CardComponent>()?.manaValue ?: 0

    /**
     * [cost] with the sacrificed creature's mana value taken off its **generic** portion
     * (CR 702.119a). Passing a null creature (no selection made yet) leaves the cost untouched.
     */
    fun reduceForSacrifice(cost: ManaCost, state: GameState, sacrificedId: EntityId?): ManaCost {
        val manaValue = sacrificedId?.let { manaValueOf(state, it) } ?: 0
        return if (manaValue > 0) cost.reduceGeneric(manaValue) else cost
    }
}
