package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.sidesFor
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import java.util.concurrent.ConcurrentHashMap

/**
 * A board whose attackers end the game on contact — [com.wingedsheep.ai.engine.AiProfile.finishersEndTheGame].
 *
 * Vraska, Scheming Gorgon's −10 gives every creature of ours "whenever this creature deals damage
 * to an opponent, that player loses the game" until end of turn. The grant moves nothing but a
 * keyword, so the evaluator saw a planeswalker spent for deathtouch and took the +2 instead, with
 * three attackers into one blocker. The life race can't see it either: the opponent's life total is
 * no longer the thing the attack has to get through.
 *
 * So: on our own turn, before combat, count the creatures that can attack now and carry such a
 * trigger (printed, as on Phage the Untouchable, or granted), against the creatures the defending
 * side can block with. More strikers than blockers means one of them connects — the game, short of
 * an instant-speed answer — and scores [WIN_VALUE] in life units. Fewer is no claim at all: every
 * striker can be blocked, and the race terms already price the attack.
 */
class FinisherStrike(private val intents: IntentCatalog) : BoardFeature {

    /** Per ability: whether it is a self-bound damage trigger whose payoff is a [IntentTag.GAME_WIN]. */
    private val finishers = ConcurrentHashMap<TriggeredAbility, Boolean>()

    override fun score(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        if (!intents.isEnabled || state.step !in BEFORE_ATTACKS) return 0.0
        val sides = state.sidesFor(playerId) ?: return 0.0
        val active = state.activePlayerId ?: return 0.0
        return when {
            active in sides.mine -> if (sides.opponents.any { wins(state, projected, sides.mine, it) }) WIN_VALUE else 0.0
            else -> if (sides.opponents.any { active in it && wins(state, projected, it, sides.mine) }) -WIN_VALUE else 0.0
        }
    }

    private fun wins(state: GameState, projected: ProjectedState, attackers: List<EntityId>, defenders: List<EntityId>): Boolean {
        val strikers = attackers.sumOf { player ->
            projected.getBattlefieldControlledBy(player).count { canAttackNow(state, projected, it) && strikesForTheGame(state, projected, it) }
        }
        if (strikers == 0) return false
        val blockers = defenders.sumOf { player ->
            projected.getBattlefieldControlledBy(player).count {
                projected.isCreature(it) && !projected.cantBlock(it) && state.getEntity(it)?.has<TappedComponent>() != true
            }
        }
        return strikers > blockers
    }

    private fun canAttackNow(state: GameState, projected: ProjectedState, id: EntityId): Boolean {
        if (!projected.isCreature(id) || projected.cantAttack(id)) return false
        val entity = state.getEntity(id) ?: return false
        if (entity.has<TappedComponent>()) return false
        return !entity.has<SummoningSicknessComponent>() ||
            projected.hasKeyword(id, Keyword.HASTE) || projected.canAttackAsThoughHasty(id)
    }

    private fun strikesForTheGame(state: GameState, projected: ProjectedState, id: EntityId): Boolean {
        if (projected.hasLostAllAbilities(id)) return false
        val entity = state.getEntity(id) ?: return false
        val granted = state.grantedTriggeredAbilities.filter { it.entityId == id }.map { it.ability }
        if (granted.any(::isFinisher)) return true
        if (entity.has<FaceDownComponent>()) return false
        val name = entity.get<CardComponent>()?.name ?: return false
        return intents.definition(name)?.script?.triggeredAbilities.orEmpty().any(::isFinisher)
    }

    private fun isFinisher(ability: TriggeredAbility): Boolean = finishers.getOrPut(ability) {
        ability.binding == TriggerBinding.SELF &&
            ability.trigger is EventPattern.DealsDamageEvent &&
            IntentTag.GAME_WIN in CardIntentAnalyzer.analyzeEffect(ability.effect).tags
    }

    companion object {
        /**
         * What a connecting strike is worth, in life units: more than any board the AI could build
         * instead, short of the game-over the search scores itself. Measured against Vraska's +2
         * on three attackers into one blocker, which out-scored the −10 by about 16 weighted points.
         */
        const val WIN_VALUE = 40.0

        /** Our turn, attackers not yet declared: the strikers can still all be sent in. */
        private val BEFORE_ATTACKS = setOf(Step.UPKEEP, Step.DRAW, Step.PRECOMBAT_MAIN, Step.BEGIN_COMBAT)
    }
}
