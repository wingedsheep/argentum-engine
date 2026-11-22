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
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MustAttackGroupEffect

/**
 * "Does casting this forced attack now make an opponent attack this turn?" — the one window an
 * instant-speed [IntentTag.FORCED_ATTACK] spell is worth its card in, read for
 * [com.wingedsheep.ai.engine.AiProfile.forcedAttacksOnTheirTurn].
 *
 * "Creatures target opponent controls attack this turn if able" (Imaginary Threats) moves no
 * permanent, so the board score sees only a card leaving our hand. What it buys is an attack the
 * opponent didn't choose (with the untap lock beside it, their attackers stay tapped through our
 * turn), and that only exists on the opponent's own turn, before attackers are declared: cast on
 * our turn, or after their attack, "this turn" holds no attack of theirs at all.
 *
 * So the window is the opponent's turn from upkeep through the beginning of combat, and only while
 * they control a creature that could attack now and isn't already required to. Only a
 * "this turn" requirement ([Duration.EndOfTurn]) on the card's own spell or on its activated ability
 * (Goblin Diplomats) reads; a trigger's (Kardur's entry) is priced by the body it comes with.
 */
object ForcedAttackWindow {

    /**
     * Whether casting [definition]'s spell now, as [playerId], forces an opponent's attack this turn.
     * [targetedPlayers] are the players the cast targets: aimed at an opponent whose turn it isn't
     * (a multiplayer table), "this turn" binds no attack of theirs.
     */
    fun forcesAnAttack(
        state: GameState,
        playerId: EntityId,
        definition: CardDefinition,
        targetedPlayers: List<EntityId> = emptyList(),
    ): Boolean {
        val spell = definition.script.spellEffect ?: return false
        return forcesAnAttack(state, playerId, spell, targetedPlayers)
    }

    /**
     * The same question for any resolving [effect] — an activated ability's ("{T}: Each creature
     * attacks this turn if able", Goblin Diplomats) as well as a spell's.
     */
    fun forcesAnAttack(
        state: GameState,
        playerId: EntityId,
        effect: Effect,
        targetedPlayers: List<EntityId> = emptyList(),
    ): Boolean {
        if (!forcesThisTurn(effect)) return false
        if (state.isActiveTurnFor(playerId) || state.step !in BEFORE_THEIR_ATTACK) return false
        val attacker = state.activePlayerId ?: return false
        if (targetedPlayers.isNotEmpty() && attacker !in targetedPlayers) return false
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(attacker).any { couldAttackUnforced(state, projected, it) }
    }

    /** Whether [effect] carries a "this turn" forced attack the intent reader calls one. */
    fun forcesThisTurn(effect: Effect): Boolean =
        EffectWalker.leaves(effect).filterIsInstance<MustAttackGroupEffect>().any {
            it.duration == Duration.EndOfTurn && IntentTag.FORCED_ATTACK in CardIntentAnalyzer.analyzeEffect(it).tags
        }

    /**
     * Whether [effect], resolved on our own turn, puts an attack requirement on our creatures
     * ("each creature attacks this turn if able" binds only the active player's creatures): there
     * it is a price, forcing our team into whatever blocks wait, and buys nothing of theirs.
     */
    fun bindsOwnAttackNow(state: GameState, playerId: EntityId, effect: Effect): Boolean =
        state.isActiveTurnFor(playerId) &&
            EffectWalker.leaves(effect).filterIsInstance<MustAttackGroupEffect>().any {
                it.duration == Duration.EndOfTurn && it.filter.baseFilter.controllerPredicate == null
            }

    /**
     * Whether an attack requirement for the turn on [creature] ("target creature attacks this turn
     * if able", Hustle) would still force an attack of an opponent's: it is their turn, before
     * attackers are declared, and the creature is theirs and could attack but isn't required to.
     */
    fun canStillBeForcedToAttack(state: GameState, projected: ProjectedState, playerId: EntityId, creature: EntityId): Boolean {
        if (state.isActiveTurnFor(playerId) || state.step !in BEFORE_THEIR_ATTACK) return false
        if (projected.getController(creature) != state.activePlayerId) return false
        return couldAttackUnforced(state, projected, creature)
    }

    /** A creature that could be declared as an attacker now and isn't already required to attack. */
    private fun couldAttackUnforced(state: GameState, projected: ProjectedState, id: EntityId): Boolean {
        if (!projected.isCreature(id) || projected.cantAttack(id) || projected.mustAttack(id)) return false
        val entity = state.getEntity(id) ?: return false
        if (entity.has<TappedComponent>()) return false
        return !entity.has<SummoningSicknessComponent>() ||
            projected.hasKeyword(id, Keyword.HASTE) || projected.canAttackAsThoughHasty(id)
    }

    /**
     * Whether [playerId] keeps an untapped creature other than [source] to block the attack it would
     * force: tapping the only blocker to make them attack (Goblin Diplomats) forces damage onto us.
     */
    fun keepsABlocker(state: GameState, playerId: EntityId, source: EntityId): Boolean {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).any { id ->
            id != source && projected.isCreature(id) && state.getEntity(id)?.has<TappedComponent>() == false
        }
    }

    /** The steps of an opponent's turn before attackers are declared. */
    private val BEFORE_THEIR_ATTACK = setOf(Step.UPKEEP, Step.DRAW, Step.PRECOMBAT_MAIN, Step.BEGIN_COMBAT)
}
