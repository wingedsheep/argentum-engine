package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Step
import kotlinx.serialization.Serializable

/**
 * Tells the server what to do with the undo checkpoint after an action is processed.
 *
 * The engine computes this policy based on game rules (which actions are undo-eligible,
 * which events reveal information, etc.) so the server can follow it mechanically
 * without encoding game knowledge.
 */
@Serializable
enum class UndoCheckpointAction {
    /** Create a new undo checkpoint (undo-eligible actions like PlayLand, DeclareAttackers) */
    SET_CHECKPOINT,
    /** Create checkpoint + save pre-combat state (passing priority in precombat main) */
    SET_PRECOMBAT_CHECKPOINT,
    /** First mana ability creates checkpoint; subsequent ones preserve existing */
    SET_IF_NO_EXISTING_CHECKPOINT,
    /** Don't touch the checkpoint (PassPriority, ChooseManaColor, CastSpell) */
    PRESERVE,
    /** Invalidate any existing checkpoint (information-revealing events, non-neutral actions, etc.) */
    CLEAR
}

/**
 * Extension properties for [GameEvent] that classify events for undo policy decisions.
 *
 * These are defined here rather than on the sealed interface to keep GameEvent
 * focused on its data contract while centralizing undo-relevant classification.
 */
val GameEvent.revealsInformation: Boolean get() = when (this) {
    is CardsDrawnEvent, is CardRevealedFromDrawEvent,
    is CardsRevealedEvent, is LookedAtCardsEvent,
    is HandLookedAtEvent, is HandRevealedEvent,
    is CoinFlipEvent, is DieRolledEvent, is LibraryReorderedEvent -> true
    else -> false
}

val GameEvent.isStackResolution: Boolean get() = when (this) {
    is ResolvedEvent, is AbilityResolvedEvent -> true
    else -> false
}

/**
 * Computes the [UndoCheckpointAction] for a given action and its execution result.
 *
 * This encapsulates all game-rule knowledge about undo eligibility:
 * - Which actions can be undone (PlayLand, DeclareAttackers, etc.)
 * - Which events invalidate undo (draws, reveals, stack resolution, turn changes)
 * - Which actions are "checkpoint-neutral" (PassPriority, mana abilities, CastSpell)
 */
object UndoPolicyComputer {

    fun compute(
        action: GameAction,
        originalState: GameState,
        result: ExecutionResult,
        cardRegistry: CardRegistry
    ): UndoCheckpointAction {
        // Errors don't change checkpoint state
        if (result.error != null) return UndoCheckpointAction.PRESERVE

        // Events take priority: information revelation, stack resolution, or turn changes invalidate undo
        if (result.events.any { it.revealsInformation || it.isStackResolution || it is TurnChangedEvent }) {
            return UndoCheckpointAction.CLEAR
        }

        return when {
            // Declaring attackers/blockers commits the declaration and reveals it to the
            // opponent, so it can't be taken back once real creatures are involved. An *empty*
            // declaration is the auto-pass-through-combat case — it stays undoable so a player
            // who skipped into combat by accident can step back.
            action is DeclareAttackers ->
                if (action.attackers.isEmpty()) UndoCheckpointAction.SET_CHECKPOINT
                else UndoCheckpointAction.CLEAR

            action is DeclareBlockers ->
                if (action.blockers.isEmpty()) UndoCheckpointAction.SET_CHECKPOINT
                else UndoCheckpointAction.CLEAR

            isUndoEligibleAction(action) -> UndoCheckpointAction.SET_CHECKPOINT

            // Active player passing priority in precombat main -> save pre-combat state
            action is PassPriority
                && originalState.step == Step.PRECOMBAT_MAIN
                && action.playerId == originalState.activePlayerId ->
                UndoCheckpointAction.SET_PRECOMBAT_CHECKPOINT

            // Defending player passing at declare attackers with empty stack -> checkpoint for blocker undo
            action is PassPriority
                && originalState.step == Step.DECLARE_ATTACKERS
                && action.playerId != originalState.activePlayerId
                && originalState.stack.isEmpty() ->
                UndoCheckpointAction.SET_CHECKPOINT

            // Mana abilities: first creates checkpoint, subsequent preserve existing
            action is ActivateAbility && isManaAbilityActivation(action, originalState, cardRegistry) ->
                UndoCheckpointAction.SET_IF_NO_EXISTING_CHECKPOINT

            // Checkpoint-neutral actions: don't change checkpoint state
            isCheckpointNeutralAction(action) -> UndoCheckpointAction.PRESERVE

            // CastSpell goes on stack - don't clear checkpoint (opponent will clear when they act)
            action is CastSpell -> UndoCheckpointAction.PRESERVE

            // Everything else: clear checkpoint
            else -> UndoCheckpointAction.CLEAR
        }
    }

    /**
     * Non-respondable actions that the player should be able to undo
     * before the opponent has a chance to respond.
     */
    private fun isUndoEligibleAction(action: GameAction): Boolean = when (action) {
        is PlayLand, is OrderBlockers, is TurnFaceUp -> true
        else -> false
    }

    /**
     * Actions that should preserve the existing checkpoint without creating or clearing it.
     */
    private fun isCheckpointNeutralAction(action: GameAction): Boolean = when {
        action is PassPriority -> true
        action is ChooseManaColor -> true
        else -> false
    }

    /**
     * Checks if an ActivateAbility action is activating a mana ability.
     */
    private fun isManaAbilityActivation(
        action: ActivateAbility,
        state: GameState,
        cardRegistry: CardRegistry
    ): Boolean {
        val container = state.getEntity(action.sourceId) ?: return false
        val cardComponent = container.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return false
        val ability = cardDef.script.activatedAbilities.find { it.id == action.abilityId }
        return ability?.isManaAbility == true
    }
}
