package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * "Does this activation change anything?" — the question behind
 * [AiProfile.refuseEmptyPayoffs][com.wingedsheep.ai.engine.AiProfile.refuseEmptyPayoffs], and the
 * generalisation of [DeadSearch] from "a search that finds nothing" to any activation whose whole
 * payoff provably lands on nothing.
 *
 * Three shapes, each from the 2026-10-10 `live`-profile AI-vs-AI logs:
 *
 *  - **A group effect over an empty group.** Oakhollow Village's "{G}, {T}: put a +1/+1 counter on
 *    each Frog, Rabbit, Raccoon, or Squirrel you control that entered this turn" activated on turns
 *    when none had (game 1, turns 19–25). Read as: every leaf is a battlefield gather or an effect
 *    that acts only on what those gathers stored (counters, tap/untap), and no permanent on the
 *    battlefield matches any gather's filter.
 *  - **An attach to the current host.** Equip on the creature already wearing the Equipment
 *    (Mandibular Kite, game 3 turn 19; Dúnedain Blade, game 1 turn 30). The attach does nothing
 *    (CR 701.3b), so the mana buys nothing.
 *  - **Moving an Equipment again after combat.** Dúnedain Blade moved three times in one
 *    postcombat main phase (game 1 turn 32). Each move is a reallocation between two of our own
 *    creatures that the rollouts score as a near-tie, so noise alone picks a "best" move again and
 *    again. Once the Equipment has been activated this turn and our combat is over, another move
 *    is floored. This is the one shape that is a judgement rather than a certainty: it also holds a
 *    "precombat equip the attacker, postcombat move it onto a blocker" line. That reallocation is
 *    small (it costs the old host what it gives the new one) and the log shows the noisy version
 *    far more often than the deliberate one.
 *
 * Like [DeadSearch], every shape declines unless the cost is plain resources (mana, tap, life), so
 * a cost that is a payoff of its own (a sacrifice, a discard) is never second-guessed here.
 */
object EmptyPayoff {

    /** Whether [ability], activated as [activation], provably buys nothing. */
    fun holds(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
        activation: ActivateAbility,
        predicates: PredicateEvaluator?,
    ): Boolean {
        if (!costsOnlyResources(ability.cost)) return false
        val leaves = EffectWalker.leaves(ability.effect)
        if (leaves.isEmpty()) return false
        if (leaves.all { it is AttachEquipmentEffect }) {
            return attachesToCurrentHost(state, activation) ||
                movesAgainAfterCombat(state, playerId, ability, activation)
        }
        return predicates != null && gathersNothing(state, playerId, leaves, activation, predicates)
    }

    /** An attach whose one target is the permanent the source is already attached to. */
    private fun attachesToCurrentHost(state: GameState, activation: ActivateAbility): Boolean {
        val target = activation.targets.singleOrNull() as? ChosenTarget.Permanent ?: return false
        val host = state.getEntity(activation.sourceId)?.get<AttachedToComponent>()?.targetId ?: return false
        return host == target.entityId
    }

    /**
     * An equip, in our own postcombat main phase, of an Equipment that is already attached and
     * already had an ability activated this turn — the second (or third) move of the same turn.
     */
    private fun movesAgainAfterCombat(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
        activation: ActivateAbility,
    ): Boolean {
        if (!ability.isEquipAbility) return false
        if (state.activePlayerId != playerId || state.step != Step.POSTCOMBAT_MAIN) return false
        val source = state.getEntity(activation.sourceId) ?: return false
        if (source.get<AttachedToComponent>() == null) return false
        return source.get<AbilityActivatedThisTurnComponent>()?.anyActivated == true
    }

    /**
     * Every leaf is a battlefield gather or a per-member effect over a collection one of them
     * stored, and every gather comes back empty. Any other leaf — a draw, a life gain, an effect
     * that reads something else — declines, because it is a payoff of its own.
     */
    private fun gathersNothing(
        state: GameState,
        playerId: EntityId,
        leaves: List<Effect>,
        activation: ActivateAbility,
        predicates: PredicateEvaluator,
    ): Boolean {
        val gathers = leaves.filterIsInstance<GatherCardsEffect>()
        if (gathers.isEmpty()) return false
        val groups = gathers.map { battlefieldGroup(it) ?: return false }
        val stored = gathers.map { it.storeAs }.toSet()
        val consumersOnlyReadThem = leaves.all { leaf ->
            when (leaf) {
                is GatherCardsEffect -> true
                is AddCountersToCollectionEffect -> leaf.collectionName in stored
                is TapUntapCollectionEffect -> leaf.collectionName in stored
                else -> false
            }
        }
        if (!consumersOnlyReadThem) return false

        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId, sourceId = activation.sourceId)
        return groups.none { group ->
            state.getBattlefield().any { id ->
                val controller = projected.getController(id)
                val inScope = when (group.player) {
                    Player.You -> controller == playerId
                    Player.EachOpponent, Player.AnOpponent -> controller != playerId
                    // Anything else is read as the whole battlefield: wider can only decline.
                    else -> true
                }
                inScope && !(group.excludeSelf && id == activation.sourceId) &&
                    // An unreadable filter is "it might match", never a veto.
                    runCatching { predicates.matches(state, projected, id, group.filter, context) }.getOrDefault(true)
            }
        }
    }

    private data class Group(val filter: GameObjectFilter, val player: Player, val excludeSelf: Boolean)

    /** What [gather] collects from the battlefield, or null when it reads anything else. */
    private fun battlefieldGroup(gather: GatherCardsEffect): Group? {
        if (gather.search || gather.revealed) return null
        return when (val source = gather.source) {
            is CardSource.BattlefieldMatching -> Group(source.filter, source.player, source.excludeSelf)
            is CardSource.FromZone ->
                if (source.zone == Zone.BATTLEFIELD) Group(source.filter, source.player, excludeSelf = false) else null
            else -> null
        }
    }

    private fun costsOnlyResources(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.Free, AbilityCost.Tap -> true
        is AbilityCost.Atom -> cost.atom is CostAtom.Mana || cost.atom is CostAtom.PayLife
        is AbilityCost.Composite -> cost.costs.all(::costsOnlyResources)
        else -> false
    }
}
