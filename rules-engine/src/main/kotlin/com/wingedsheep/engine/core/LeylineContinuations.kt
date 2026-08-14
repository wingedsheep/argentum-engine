package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Resume after a player decides whether to put one of their leyline cards onto the
 * battlefield from their opening hand.
 *
 * The leyline phase runs after all mulligans and bottoming complete but before the first
 * turn begins (CR 103.6 / 103.6a: once mulligans are done, the starting player and then
 * each other player in turn order may put any "begin the game with this on the battlefield"
 * card from their opening hand onto the battlefield). The engine walks players in turn order
 * starting with the active player; for each player it drains their
 * [com.wingedsheep.engine.state.components.player.MulliganStateComponent.pendingLeylineCardIds]
 * one card at a time, pausing each step with a yes/no decision and pushing a fresh
 * [LeylineDecisionContinuation] for the next card.
 *
 * The resumer (see `LeylineContinuationResumer`) handles two outcomes:
 *  - **yes**: route the card through the standard zone-change pipeline (hand → battlefield
 *    under owner's control) so any ETB replacement effects and trackers fire normally, then
 *    pause for the card's own "as this enters, choose …" replacement if it has one (a Leyline
 *    of Transformation started from the opening hand still chooses its creature type).
 *  - **no**: leave the card in hand; just drop it from the pending list.
 * After applying the outcome it either pauses with the next leyline prompt or hands the
 * state back to `SubmitDecisionHandler`, which advances the game from UNTAP into the first
 * turn via `turnManager.advanceStep`.
 *
 * @property playerId Player making this decision (the leyline card's owner)
 * @property leylineCardId The leyline card entity in [playerId]'s hand
 * @property cardName Card name for prompt rendering (avoids a registry lookup at resume time)
 */
@Serializable
data class LeylineDecisionContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val leylineCardId: EntityId,
    val cardName: String
) : ContinuationFrame

/**
 * Resume the opening-hand leyline walk after something *else* paused in the middle of it.
 *
 * Only one thing does: a leyline that answers "yes" and then has its own "as this enters,
 * choose …" replacement (Leyline of Transformation's creature type). That choice pauses with an
 * [EntersWithChoiceOnBattlefieldContinuation] of its own, so the walk over the remaining leylines
 * has to be parked underneath it. This frame is that park: it carries no decision of its own and
 * is auto-resumed (see `LeylineContinuationResumer`) once the choice above it finishes, asking the
 * next player's yes/no or handing the state back for the advance into turn 1.
 *
 * @property decisionId Synthetic — this frame is never matched against a player response.
 */
@Serializable
data class LeylinePhaseContinuation(
    override val decisionId: String
) : ContinuationFrame
