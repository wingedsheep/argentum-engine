package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.TapForManaPermanentsYouDontControlEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * "What are you going to cast with it?" — a spell whose whole payoff is a turn-scoped permission to
 * tap other players' permanents for mana that can only be spent on spells, cast where nothing we
 * could cast afterwards would spend it.
 *
 * Piracy is the card (2026-10-25 AI playtest, four-player game 1, turn 26): cast in the precombat
 * main with only two Extinguish left in hand and nothing on the stack to counter. Nothing else was
 * cast that turn, and the permission ended at cleanup. The leaf sees two Islands tapped for a grant
 * the board evaluator gives no weight to either way, which came out no worse than passing.
 *
 * The claim is the [TimingVerdict.NoWindow] kind, so the shape is narrow:
 *
 *  - an **instant or sorcery** with no faces whose spell effect is exactly one
 *    [TapForManaPermanentsYouDontControlEffect] for its controller, lasting **until end of turn**,
 *    with the [ManaRestriction.SpellsOnly] restriction — so no activated ability can spend it either;
 *  - cast in **our own main phase with an empty stack** — the position the closing paragraph's
 *    assumption needs, so an instant copy cast on an opponent's turn (or in response) is never
 *    refused;
 *  - and in the position the cast resolves to (the [leaf]: stack empty, nothing pending, priority
 *    back with us), the engine offers us **no affordable spell cast** whose targets can be filled.
 *    That enumeration already counts the borrowed sources, so a spell that becomes castable only
 *    with the borrowed mana keeps the grant.
 *
 * What this can't see is a spell an opponent casts later this turn that we would then want to
 * answer with the borrowed mana; at sorcery speed in our own main phase, with nothing on the
 * stack, that is not something to hold a card for. Carried on
 * [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs].
 */
internal object UnspendableManaGrant {

    fun holds(
        state: GameState,
        playerId: EntityId,
        definition: CardDefinition,
        leaf: GameState?,
        legalActions: ((GameState, EntityId) -> List<LegalAction>)?,
    ): Boolean {
        if (leaf == null || legalActions == null) return false
        val type = definition.typeLine
        if (!(type.isInstant || type.isSorcery)) return false
        if (definition.cardFaces.isNotEmpty() || definition.backFace != null || definition.flipSide != null) return false
        val grant = definition.script.spellEffect as? TapForManaPermanentsYouDontControlEffect ?: return false
        if (grant.target != EffectTarget.Controller || grant.restriction != ManaRestriction.SpellsOnly ||
            grant.duration != Duration.EndOfTurn
        ) {
            return false
        }
        // The assumption below only holds where the cast happens: our main phase, nothing on the stack.
        if (state.activePlayerId != playerId || !state.step.isMainPhase || state.stack.isNotEmpty()) return false

        // Only a fully resolved leaf, back in our hands, says what we could do with the grant.
        if (leaf.gameOver || leaf.pendingDecision != null || leaf.stack.isNotEmpty() ||
            leaf.priorityPlayerId != playerId
        ) {
            return false
        }
        val actions = runCatching { legalActions(leaf, playerId) }.getOrNull() ?: return false
        return actions.none { action ->
            action.action is CastSpell && action.affordable && !action.hasUnfillableTargetRequirement
        }
    }
}
