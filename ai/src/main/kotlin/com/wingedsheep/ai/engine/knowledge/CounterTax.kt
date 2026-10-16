package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.isOpponentTo
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * "Can they just pay it?" — the question a **tax counter** has to ask before it is cast, and the
 * one a one-ply leaf cannot.
 *
 * A Divert Disaster ("counter target spell unless its controller pays {2}") cast into an opponent
 * with two mana open does not counter anything: they pay, the spell resolves, and we get the
 * Lander the rider hands out for our trouble. Two Divert Disasters into the same two mana *do*
 * counter it — the first one empties their pool and the second one is a hard counter. Both halves
 * showed up in one game (2026-10-15, Edge of Eternities sealed, game `2f830698`):
 *
 *  - **Turns 13 and 15** — Tannuk and Nova Hellkite cast with exactly two lands left over, the AI
 *    holding two Divert Disasters and four open Islands. Each single Divert scored as a card spent
 *    for a Lander, so neither was cast, and the line that counters the Hellkite — one Divert to
 *    drain them, the second to counter — was never seen: the leaf scores one cast at a time, and
 *    the first cast of that line, scored alone, is the bad one.
 *  - **Turn 21** — both Diverts thrown at a Bombard cast off ten lands. The opponent paid both
 *    taxes and the Bombard resolved.
 *
 * So the question is not "can they pay this tax" but "can they pay every tax we can stack on this
 * spell": the taxes of the tax counters in our hand, cheapest first, that the mana we have left
 * after this one can still cast. Three answers:
 *
 *  - **They cannot pay this one.** It is an ordinary counter, and the leaf already sees it counter
 *    the spell — [verdict] says nothing.
 *  - **They can pay this one but not the stack.** The cast is the first half of a two-cast line,
 *    and is credited with what the line buys: the spell's worth, on the same
 *    [BoardPresence.spellValue] scale [CounterPatience] prices it on. Once it resolves and they
 *    have paid, the second counter's own leaf shows the spell countered, so the line completes
 *    without any memory between decisions.
 *  - **They can pay the whole stack.** The counter cannot counter. What is left — the rider and
 *    the mana they spend paying — is not what the card was printed for, and the floor says so.
 *
 * Their mana is read with the engine's own solver ([IntentCatalog.availableMana]), so a mana
 * creature or a Treasure counts as the mana it is: undercounting it is exactly how a tax gets
 * paid. Our side compares mana values, not colours, like [AmbushWindow]'s contention release.
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.taxCountersReadTheirMana].
 */
internal object CounterTax {

    /** What a tax counter cast as [cast] makes of the spell it points at. */
    sealed interface Reading {
        /** Not a tax counter, not pointed at an opposing spell, or they cannot pay: no opinion. */
        data object Silent : Reading

        /** They can pay this tax, but not every tax we can stack — worth [spellWorth] of board value. */
        data class FirstOfAStack(val spellWorth: Double) : Reading

        /** They can pay every tax we can put on the spell. */
        data object Futile : Reading
    }

    fun read(
        state: GameState,
        playerId: EntityId,
        intent: CardIntent,
        cast: CastSpell,
        intents: IntentCatalog,
    ): Reading {
        val tax = intent.counterTax ?: return Reading.Silent

        // One opposing spell, as [CounterPatience] insists: the tax is paid by that spell's caster.
        val spellId = cast.targets.filterIsInstance<ChosenTarget.Spell>().singleOrNull()?.spellEntityId
            ?: return Reading.Silent
        val spell = state.getEntity(spellId) ?: return Reading.Silent
        val casterId = spell.get<SpellOnStackComponent>()?.casterId ?: return Reading.Silent
        if (!state.isOpponentTo(casterId, playerId)) return Reading.Silent

        val theirMana = intents.availableMana(state, casterId) ?: return Reading.Silent
        if (theirMana < tax) return Reading.Silent

        val stacked = tax + stackableTaxes(state, playerId, cast.cardId, intents)
        if (theirMana >= stacked) return Reading.Futile

        val card = spell.get<CardComponent>() ?: return Reading.Silent
        // An instant or sorcery has no board value to read; its mana value at the fair-trade rate is
        // "a spell that size", the same stand-in [Patience] gives the removal bar.
        val worth = BoardPresence.spellValue(state.projectedState, card, casterId, intents)
            ?: (Patience.FAIR_TRADE_VALUE_PER_MANA * card.manaCost.cmc)
        return Reading.FirstOfAStack(worth)
    }

    /**
     * The taxes of the other tax counters in our hand that the mana left after casting [castCardId]
     * can still pay for, cheapest first — the most tax we can stack behind this one.
     */
    private fun stackableTaxes(
        state: GameState,
        playerId: EntityId,
        castCardId: EntityId,
        intents: IntentCatalog,
    ): Int {
        val ourMana = intents.availableMana(state, playerId) ?: return 0
        val thisCost = state.getEntity(castCardId)?.get<CardComponent>()?.manaCost?.cmc ?: return 0
        var left = ourMana - thisCost
        var taxes = 0
        val others = state.getZone(playerId, Zone.HAND)
            .filter { it != castCardId }
            .mapNotNull { id ->
                val card = state.getEntity(id)?.get<CardComponent>() ?: return@mapNotNull null
                val other = intents.forName(card.name) ?: return@mapNotNull null
                val otherTax = other.counterTax ?: return@mapNotNull null
                if (other.speed != Speed.INSTANT) return@mapNotNull null
                card.manaCost.cmc to otherTax
            }
            .sortedBy { it.first }
        for ((cost, otherTax) in others) {
            if (cost > left) break
            left -= cost
            taxes += otherTax
        }
        return taxes
    }
}
