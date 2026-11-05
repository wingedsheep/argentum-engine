package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * "Why tap it now?" — a creature's `{T}: deal N damage to each opponent`, held for the window
 * where the tap is free.
 *
 * Production game `3d30821e` (Ixalan Sealed, turn 15): at 2 life, facing a 6/5 lifelinking Wanted
 * Scoundrels, the AI attacked with both Emissaries of Sunrise and then tapped Lightning-Rig Crew
 * ("{T}: This creature deals 1 damage to each opponent") — its one remaining blocker, the one the
 * attack plan had kept home. The leaf priced a point of damage and had no term for the block the
 * tap gave up (the board's threat term reads a 6-power attacker as lethal through a 0/5 whether
 * the 0/5 is untapped or not), and the swing back killed it.
 *
 * The claim is the same dominance [SelfTapGrowthWindow] makes for counters: activated in the
 * opponent's end step instead, the damage lands before our next turn and the creature untaps in
 * our untap step, so every earlier window costs an attack or a block for damage that waiting
 * delivers anyway. What waiting gives up is the damage arriving a turn earlier, which matters
 * only when it ends the game — so a ping that kills an opponent now is never held.
 *
 * The narrow shape: an instant-speed ability whose cost taps its source creature and whose every
 * payoff is a fixed amount of damage or life loss to **each opponent** — untargeted, so it cannot
 * be a combat answer aimed at a creature. Never with something on the stack.
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.tapToPingAtTheirEndStep].
 */
internal object SelfTapPingWindow {

    /** Whether activating [ability] here is dominated by activating it once the tap costs nothing. */
    fun holds(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
        activation: ActivateAbility,
    ): Boolean {
        if (ability.isManaAbility || ability.timing != TimingRule.InstantSpeed) return false
        if (!SelfTapGrowthWindow.tapsItself(ability.cost)) return false
        val amount = facePingAmount(ability) ?: return false
        val projected = state.projectedState
        val source = activation.sourceId
        if (!projected.isCreature(source) || projected.getController(source) != playerId) return false
        if (state.stack.isNotEmpty()) return false
        if (state.getOpponents(playerId).any { state.lifeTotal(it) <= amount }) return false
        return !SelfTapGrowthWindow.tapIsFree(state, playerId)
    }

    /** The total fixed damage/life loss the ability deals each opponent, or null if that is not all it does. */
    private fun facePingAmount(ability: ActivatedAbility): Int? {
        val leaves = EffectWalker.leaves(ability.effect)
        if (leaves.isEmpty()) return null
        var total = 0
        for (leaf in leaves) {
            val (amount, target) = when (leaf) {
                is DealDamageEffect -> leaf.amount to leaf.target
                is LoseLifeEffect -> leaf.amount to leaf.target
                else -> return null
            }
            if (target != EffectTarget.PlayerRef(Player.EachOpponent)) return null
            total += (amount as? DynamicAmount.Fixed)?.amount ?: return null
        }
        return total
    }
}
