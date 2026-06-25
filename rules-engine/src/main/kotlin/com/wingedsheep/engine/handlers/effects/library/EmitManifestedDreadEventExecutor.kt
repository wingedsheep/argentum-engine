package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ManifestedDreadEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.EmitManifestedDreadEventEffect
import kotlin.reflect.KClass

/**
 * Tail of the [com.wingedsheep.sdk.dsl.Patterns.Library.manifestDread] composite: emits a
 * [ManifestedDreadEvent] so "Whenever you manifest dread" triggers (CR 701.60) fire exactly once
 * per manifest-dread, after the manifest/graveyard moves have all resolved.
 *
 * The carried [ManifestedDreadEvent.graveyardCardIds] are the cards put into the graveyard this
 * way — read from the named graveyard collection (`"manifestDreadGraveyard"` by default) at
 * resolution time. They are threaded onto the resolving trigger's pipeline via
 * [com.wingedsheep.engine.event.TriggerContext.capturedEntityIds] so a payoff can pull "a card you
 * put into your graveyard this way" back out (Paranormal Analyst).
 *
 * The collection (and thus the event's id list) is empty when the library held fewer than two
 * cards; the event is still emitted, because CR 701.60b fires "whenever you manifest dread"
 * triggers "even if some or all of those actions were impossible."
 */
class EmitManifestedDreadEventExecutor : EffectExecutor<EmitManifestedDreadEventEffect> {

    override val effectType: KClass<EmitManifestedDreadEventEffect> = EmitManifestedDreadEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitManifestedDreadEventEffect,
        context: EffectContext
    ): EffectResult {
        val graveyardCardIds = context.pipeline.storedCollections[effect.graveyardCollection].orEmpty()

        val playerId = context.controllerId
        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Manifest Dread"

        return EffectResult.success(
            state,
            listOf(
                ManifestedDreadEvent(
                    playerId = playerId,
                    graveyardCardIds = graveyardCardIds,
                    sourceName = sourceName
                )
            )
        )
    }
}
