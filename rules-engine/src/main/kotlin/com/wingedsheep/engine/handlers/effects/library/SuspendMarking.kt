package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SuspendedComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * Mark the exiled card [cardId] suspended (CR 702.62) and pre-arm its haste — the one tail shared
 * by every way a card becomes suspended: a granted suspend ([GrantSuspendExecutor]), the special
 * action from hand (`SuspendCardFromHandHandler`), and a spell that exiles itself with time
 * counters as it resolves (`spell { selfExile(timeCounters = n) }`).
 *
 * - The [SuspendedComponent] marker is what makes the engine grant the exile-side
 *   countdown-and-cast triggered ability (see [com.wingedsheep.sdk.scripting.Suspend]).
 * - CR 702.62a: a creature played via suspend "gains haste until you lose control of the spell
 *   or the permanent it becomes." A normal `GrantKeyword(HASTE)` can't be used here because the
 *   card is in exile (not a projected creature), so this adds a self-sourced floating haste effect
 *   keyed to the card. It lies dormant while the card sits in exile and takes effect once the
 *   card is played onto the battlefield (the permanent reuses the same entity id). Harmless for
 *   a non-creature card, which never becomes a creature on the battlefield. The duration is
 *   [Duration.WhileControlledByController]: [context]'s controller — the player who will play the
 *   card — is captured as the gate the projector checks against.
 *
 * Neither moves the card nor adds time counters; callers do both first.
 */
internal fun GameState.markSuspended(cardId: EntityId, context: EffectContext): GameState =
    updateEntity(cardId) { it.with(SuspendedComponent) }
        .addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(Keyword.HASTE.name),
            affectedEntities = setOf(cardId),
            duration = Duration.WhileControlledByController,
            // Self-sourced so the haste survives whatever suspended the card leaving play.
            context = context.copy(sourceId = cardId),
        )
