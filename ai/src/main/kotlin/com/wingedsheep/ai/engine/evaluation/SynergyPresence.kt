package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.OpponentAggregate
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.SynergyProfile
import com.wingedsheep.ai.engine.sidesFor
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import java.util.EnumMap

/**
 * What a **payoff on the battlefield is worth given the fuel beside it** — the board term that tells
 * Blood Artist next to four tokens from Blood Artist on an empty board.
 *
 * [BoardPresence] prices a non-creature permanent by a static prior and a creature by its body.
 * Neither moves with the rest of the board, so a payoff whose value is all in what feeds it — "whenever
 * a creature you control dies", "whenever you gain life", "whenever you cast an instant or sorcery" —
 * is worth the same with nothing to feed it as beside an engine. The anthem term is the one place
 * that already scales with the board (a lord is worth the creatures it pumps); this is the same idea
 * for every [SynergyAxis], read by [SynergyAxes].
 *
 * Per payoff, per axis it rewards: [PAIR_VALUE] per unit of fuel, capped at [FUEL_CAP]. Fuel is
 *
 *  - every **other** permanent its controller has that feeds the axis — a sacrifice outlet, a
 *    lifelinker, a token maker, a creature for "whenever a creature enters" — plus the bodies the
 *    axis spends: any creature is fodder for [SynergyAxis.DEATH], any token already made counts
 *    toward [SynergyAxis.TOKENS]; and
 *  - [HAND_SHARE] per card in hand that would feed it — **own seat only**: the AI reads its own hand
 *    and never anyone else's.
 *
 * A feeder counts on the battlefield whether or not it will feed *again*: the term prices how much
 * of the payoff's material is assembled, not how many triggers are left. That is what keeps casting
 * a feeder a gain (half a unit in hand becomes a whole one in play) rather than a loss for the
 * half-unit it stops being in hand.
 *
 * What this moves, all of it measured on the leaf:
 *
 *  - **Removal and blocks aim at engines.** An opponent's payoff with fuel is worth more than its
 *    body, so killing it beats killing an equal body, and [payoffValue] lifts it over the bar
 *    [com.wingedsheep.ai.engine.knowledge.RemovalPatience] holds a removal spell to; the AI's own
 *    engine is worth protecting.
 *  - **Payoff before fuel.** A payoff cast into a hand of feeders gains their share at once, so the
 *    lord-before-tokens ordering is a gain of its own even before the rollout reaches the triggers.
 *  - **Enablers are worth keeping.** Losing the only sacrifice outlet costs every payoff its fuel.
 *
 * It does **not** price the triggers themselves: a Blood Artist trigger that drains for one is
 * already in the leaf's life totals. This is the value of the engine *still standing*.
 *
 * Face-down permanents are skipped (their definition is hidden information), and so is a permanent
 * that has lost all its abilities as a payoff — it still pays nothing out.
 */
class SynergyPresence(private val intents: IntentCatalog) : BoardFeature {

    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val sides = state.sidesFor(playerId) ?: return 0.0
        val mine = sides.mine.sumOf { value(state, projected, it, ownSeat = it == playerId) }
        return sides.against(OpponentAggregate.THREAT) { opponent ->
            mine - opponent.sumOf { value(state, projected, it, ownSeat = false) }
        }
    }

    /**
     * What the one permanent [payoff] adds to its controller's term, read from the board alone —
     * what removing it takes away. Zero for a permanent that rewards nothing.
     * [com.wingedsheep.ai.engine.knowledge.RemovalPatience] adds it to the target's worth.
     */
    fun payoffValue(state: GameState, projected: ProjectedState, payoff: EntityId): Double {
        val controller = projected.getController(payoff) ?: return 0.0
        return value(state, projected, controller, ownSeat = false, only = payoff)
    }

    /** [player]'s synergy value, reading their hand only when [ownSeat]; [only] restricts the payoffs. */
    internal fun value(
        state: GameState,
        projected: ProjectedState,
        player: EntityId,
        ownSeat: Boolean,
        only: EntityId? = null,
    ): Double {
        val payoffs = mutableListOf<Set<SynergyAxis>>()
        val ownFuel = mutableListOf<Set<SynergyAxis>>()
        val inPlay = EnumMap<SynergyAxis, Int>(SynergyAxis::class.java)
        for (id in projected.getBattlefieldControlledBy(player)) {
            val container = state.getEntity(id) ?: continue
            val profile = profileOf(state, id)
            val fuel = buildSet {
                profile?.let { addAll(it.feeds) }
                if (projected.isCreature(id)) add(SynergyAxis.DEATH)
                if (container.has<TokenComponent>()) add(SynergyAxis.TOKENS)
            }
            fuel.forEach { inPlay.merge(it, 1, Int::plus) }
            val counted = only == null || only == id
            if (counted && profile != null && profile.rewards.isNotEmpty() && !projected.hasLostAllAbilities(id)) {
                payoffs += profile.rewards
                ownFuel += fuel
            }
        }
        if (payoffs.isEmpty()) return 0.0
        // The opponent-exile axis's fuel is the resource itself, not a feeder: every card an
        // opponent owns in exile is one more payment a processor can make.
        if (payoffs.any { SynergyAxis.OPPONENT_EXILE in it }) {
            val exiled = state.turnOrder.filter { it != player }.sumOf { state.getZone(it, Zone.EXILE).size }
            if (exiled > 0) inPlay.merge(SynergyAxis.OPPONENT_EXILE, exiled, Int::plus)
        }

        val inHand = EnumMap<SynergyAxis, Int>(SynergyAxis::class.java)
        if (ownSeat) {
            for (id in state.getZone(player, Zone.HAND)) {
                profileOf(state, id)?.feeds?.forEach { inHand.merge(it, 1, Int::plus) }
            }
        }

        var total = 0.0
        for ((index, rewards) in payoffs.withIndex()) {
            for (axis in rewards) {
                val itself = if (axis in ownFuel[index]) 1 else 0
                val fuel = ((inPlay[axis] ?: 0) - itself) + HAND_SHARE * (inHand[axis] ?: 0)
                total += PAIR_VALUE * fuel.coerceIn(0.0, FUEL_CAP)
            }
        }
        return total
    }

    private fun profileOf(state: GameState, id: EntityId): SynergyProfile? {
        val container = state.getEntity(id) ?: return null
        if (container.has<FaceDownComponent>()) return null
        val card = container.get<CardComponent>() ?: return null
        if (card.isLand) return null
        val definition = intents.definition(card.cardDefinitionId) ?: return null
        return SynergyAxes.of(definition).takeUnless { it.isEmpty }
    }

    companion object {
        /**
         * Board value of one unit of fuel beside one payoff. A payoff at [FUEL_CAP] is then worth
         * 3.0 on top of its body — about a 2/2's (2.8): a fully fed Blood Artist is the kill over
         * the 1/1s feeding it, while a real body beside it (a Grizzly Bears, whose two power
         * `ThreatAssessment` prices as well) still outranks it. Deliberately modest until the arena
         * measures it.
         */
        const val PAIR_VALUE = 0.75

        /** A feeder still in hand is this share of one already feeding from the battlefield. */
        const val HAND_SHARE = 0.5

        /** Fuel past this many units adds nothing more to one payoff. */
        const val FUEL_CAP = 4.0
    }
}
