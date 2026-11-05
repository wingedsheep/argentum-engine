package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * "Why tap it now?" — a creature's `{T}: put a counter on this creature`, held for the window where
 * the tap is free.
 *
 * The misplay (player report, 2026-10-23): Clockwork Hydra's `{T}: Put a +1/+1 counter on this
 * creature` activated in the AI's own precombat main and again in the opponent's, turn after turn.
 * The Hydra grew every turn and never attacked or blocked. The leaf sees a creature one counter
 * bigger and has no term for the attack the tap forfeited — and on the opponent's turn the block
 * it forfeited is decided after the leaf is scored — so the counter always won.
 *
 * ## Why this is a floor and not a discount
 *
 * Because the claim is a dominance, not a trade-off. Activated in the opponent's end step instead,
 * the counter lands before our next turn and the creature untaps with it in our untap step. Every
 * window before then is strictly worse:
 *
 *  - **Our own turn**: the tap costs this turn's attack and the next turn's block, and buys a
 *    counter on a creature that is tapped for all of the time it would have carried it.
 *  - **The opponent's turn before blockers**: the tap costs the block, for a counter that could not
 *    have been used before the end step anyway.
 *
 * What waiting gives up is that the counter is on a tapped, idle creature for less time — nothing
 * the creature can spend. That is also the answer to "a big creature getting bigger matters less":
 * once the counter is free, there is no trade to weigh against the creature's size or the race.
 *
 * ## The narrow shape
 *
 * Only an instant-speed ability whose cost taps its source and whose every payoff is counters on
 * that same source. A counter on *another* creature can buy that creature's attack this turn; a
 * sorcery-speed ability has no opponent's end step to wait for; anything else the ability does is
 * a payoff this cannot price. And never with something on the stack, where the counter may be the
 * answer (a burn spell aimed at the creature) — the leaf decides there.
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.tapToGrowAtTheirEndStep].
 */
internal object SelfTapGrowthWindow {

    /** Whether activating [ability] here is dominated by activating it once the tap costs nothing. */
    fun holds(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
        activation: ActivateAbility,
    ): Boolean {
        if (ability.isManaAbility || ability.timing != TimingRule.InstantSpeed) return false
        if (!tapsItself(ability.cost) || !growsOnlyItself(ability)) return false
        val projected = state.projectedState
        val source = activation.sourceId
        if (!projected.isCreature(source) || projected.getController(source) != playerId) return false
        if (state.stack.isNotEmpty()) return false
        return !tapIsFree(state, playerId)
    }

    /**
     * An opponent's turn whose combat has reached blocks, with our own turn next: the creature can
     * no longer block this turn and untaps before it can attack. Blocks being in, rather than combat
     * being over, keeps the counter available as a combat pump for a creature that is blocking —
     * the leaf, which can see that fight, decides there.
     */
    fun tapIsFree(state: GameState, playerId: EntityId): Boolean {
        if (state.isActiveTurnFor(playerId)) return false
        val active = state.activePlayerId ?: return false
        if (!state.turnAfterIsOurs(active, playerId)) return false
        return state.step.ordinal >= Step.DECLARE_BLOCKERS.ordinal
    }

    /** Whether the turn after [after]'s is ours — a later opponent's turn could still use the block. */
    private fun GameState.turnAfterIsOurs(after: EntityId, playerId: EntityId): Boolean {
        val next = getNextTeam(after)
        return next == playerId || (format.sharesTeamTurns && teamOf(next).contains(playerId))
    }

    fun tapsItself(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.Tap -> true
        is AbilityCost.Composite -> cost.costs.any(::tapsItself)
        else -> false
    }

    private fun growsOnlyItself(ability: ActivatedAbility): Boolean {
        val leaves = EffectWalker.leaves(ability.effect)
        return leaves.isNotEmpty() && leaves.all {
            (it is AddCountersEffect && it.target == EffectTarget.Self) ||
                (it is AddDynamicCountersEffect && it.target == EffectTarget.Self)
        }
    }
}
