package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.MustAttackGroupEffect

/**
 * "Does casting this forced attack now make an opponent attack this turn?" — the one window an
 * instant-speed [IntentTag.FORCED_ATTACK] spell is worth its card in, read for
 * [com.wingedsheep.ai.engine.AiProfile.forcedAttacksOnTheirTurn].
 *
 * "Creatures target opponent controls attack this turn if able" (Imaginary Threats) moves no
 * permanent, so the board score sees only a card leaving our hand. What it buys is an attack the
 * opponent didn't choose — into our blockers, leaving their team tapped for the turn after — and
 * that only exists on the opponent's own turn, before attackers are declared: cast on our turn, or
 * after their attack, "this turn" holds no attack of theirs at all.
 *
 * So the window is the opponent's turn from upkeep through the beginning of combat, and only while
 * they control a creature that could attack now and isn't already required to. Only a
 * "this turn" requirement ([Duration.EndOfTurn]) on the card's own spell reads; a trigger's
 * (Kardur's entry) is priced by the body it comes with.
 */
object ForcedAttackWindow {

    /** Whether casting [definition]'s spell now, as [playerId], forces an opponent's attack this turn. */
    fun forcesAnAttack(state: GameState, playerId: EntityId, definition: CardDefinition): Boolean {
        val spell = definition.script.spellEffect ?: return false
        val forces = EffectWalker.leaves(spell).filterIsInstance<MustAttackGroupEffect>().any {
            it.duration == Duration.EndOfTurn && IntentTag.FORCED_ATTACK in CardIntentAnalyzer.analyzeEffect(it).tags
        }
        if (!forces) return false
        if (state.isActiveTurnFor(playerId) || state.step !in BEFORE_THEIR_ATTACK) return false
        val attacker = state.activePlayerId ?: return false
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(attacker).any { couldAttackUnforced(state, projected, it) }
    }

    /** A creature that could be declared as an attacker now and isn't already required to attack. */
    private fun couldAttackUnforced(state: GameState, projected: ProjectedState, id: EntityId): Boolean {
        if (!projected.isCreature(id) || projected.cantAttack(id) || projected.mustAttack(id)) return false
        val entity = state.getEntity(id) ?: return false
        if (entity.has<TappedComponent>()) return false
        return !entity.has<SummoningSicknessComponent>() ||
            projected.hasKeyword(id, Keyword.HASTE) || projected.canAttackAsThoughHasty(id)
    }

    /** The steps of an opponent's turn before attackers are declared. */
    private val BEFORE_THEIR_ATTACK = setOf(Step.UPKEEP, Step.DRAW, Step.PRECOMBAT_MAIN, Step.BEGIN_COMBAT)
}
