package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ReplacementEffect

/**
 * A replacement effect that is currently in force, together with the player its
 * controller filters ("you" / "an opponent") are measured against and the object it
 * hangs off.
 *
 * @property effect The replacement effect itself
 * @property controllerId The player who controls it — a permanent's *projected* controller
 *   for a printed ability, or the recorded granting player for a durational grant
 * @property sourceId The object it is anchored to. For a grant this is the anchor the
 *   grant was recorded against, which may already have left the battlefield
 * @property granted True when this came from [GameState.grantedReplacementEffects] rather
 *   than a permanent's printed [ReplacementEffectSourceComponent]
 */
data class ActiveReplacement(
    val effect: ReplacementEffect,
    val controllerId: EntityId,
    val sourceId: EntityId,
    val granted: Boolean
)

/**
 * Enumerates the replacement effects in force for read sites that resolve a replacement
 * *at the point of use* rather than through [ReplacementEffectProcessor]'s
 * `PendingGameEvent` pipeline — today the token-creation helpers.
 *
 * Two sources, and **both matter**:
 *
 *  1. **Printed** — battlefield permanents carrying a [ReplacementEffectSourceComponent]
 *     (Anointed Procession, Worldwalker Helm). Their controller is read from *projected*
 *     state, because control change is a layer-2 effect: a stolen Doubling Season still
 *     names its original controller in its base `ControllerComponent`.
 *  2. **Granted** — the durational riders in [GameState.grantedReplacementEffects] recorded
 *     by `GrantReplacementEffectExecutor` (Kaya, Geist Hunter's "until end of turn, if one
 *     or more tokens would be created under your control, twice that many … instead").
 *
 * A grant deliberately survives its anchor leaving the battlefield — the one-shot ability
 * that created it is done, and the resulting continuous effect stands on its own until its
 * [com.wingedsheep.sdk.scripting.Duration] expires in the cleanup step. So grants are read
 * straight off [GameState], never re-derived from the anchor entity.
 *
 * This is deliberately *not* a match: callers still apply their own event-pattern and
 * filter tests. It only guarantees that a read site which forgets to look at grants can't
 * silently make `Effects.GrantReplacementEffect` a no-op.
 */
object ActiveReplacements {

    /** Every printed-on-the-battlefield and granted replacement effect currently in force. */
    fun all(state: GameState): List<ActiveReplacement> {
        val results = mutableListOf<ActiveReplacement>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementSource = container.get<ReplacementEffectSourceComponent>() ?: continue
            val controllerId = state.projectedState.getController(entityId)
                ?: container.get<ControllerComponent>()?.playerId
                ?: continue
            for (effect in replacementSource.replacementEffects) {
                results.add(
                    ActiveReplacement(
                        effect = effect,
                        controllerId = controllerId,
                        sourceId = entityId,
                        granted = false
                    )
                )
            }
        }

        for (grant in state.grantedReplacementEffects) {
            results.add(
                ActiveReplacement(
                    effect = grant.replacement,
                    controllerId = grant.controllerId,
                    sourceId = grant.entityId,
                    granted = true
                )
            )
        }

        return results
    }

    /**
     * Drop every one-shot ([com.wingedsheep.sdk.scripting.Duration.NextUse]) grant that has
     * modified an event. Called at event boundaries — after each effect instruction finishes and
     * when an action settles — so a grant spent by the first of several objects entering
     * simultaneously still applies to the rest of them, and never to a later event. The boundary
     * is any completed instruction, nested ones included: an executor that enters several objects
     * simultaneously must do so within one instruction (as the token and mass-return executors do),
     * not through per-object nested instructions. Only the enters-with read path spends grants
     * today; a NextUse grant of another replacement kind is never spent and lasts the turn. Returns the
     * same instance when nothing is spent (the common case: this runs on every instruction).
     */
    fun purgeSpent(state: GameState): GameState {
        if (state.grantedReplacementEffects.none { it.spent }) return state
        return state.copy(grantedReplacementEffects = state.grantedReplacementEffects.filterNot { it.spent })
    }
}
