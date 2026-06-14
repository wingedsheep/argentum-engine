package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.effects.BattlefieldEntry
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId

/**
 * Mints a token that is a copy of a bare [CardDefinition] — one not instantiated in any zone —
 * and puts it onto [controllerId]'s battlefield.
 *
 * The existing token-copy path
 * ([CreateTokenCopyOfChosenPermanentExecutor.createTokenCopy]) copies the [CardComponent] off an
 * entity already in a zone; this is its sibling for the case where the source is a definition the
 * engine chose (Momir's "copy of a randomly chosen creature card"). The characteristic-copying is
 * shared with game setup via [CardEntityFactory] so a definition-derived component (protection,
 * morph, toxic, …) is never silently dropped from the token.
 *
 * The token enters as a token copy with the definition's full abilities (resolved by name through
 * the registry, like any token). It carries no cast-time `{X}` — it never went on the stack — so a
 * copied creature's own `{X}` reads 0, matching CR for cards put onto the battlefield without being
 * cast. ETB triggers fire off the emitted [ZoneChangeEvent]; global "enters with counters" grants
 * are applied via [EntersWithCountersHelper], identical to the in-zone token-copy tail.
 */
object TokenFromDefinition {

    fun mint(
        state: GameState,
        cardDef: CardDefinition,
        controllerId: EntityId,
        staticAbilityHandler: StaticAbilityHandler? = null,
    ): EffectResult {
        val (tokenId, stateWithId) = state.newEntity()

        // Token is owned and controlled by the player creating it.
        var container = CardEntityFactory.create(cardDef, ownerId = controllerId)
            .with(TokenComponent)
            .with(SummoningSicknessComponent)

        if (staticAbilityHandler != null) {
            container = staticAbilityHandler.addContinuousEffectComponent(container)
            container = staticAbilityHandler.addReplacementEffectComponent(container)
        }

        var newState = stateWithId.withEntity(tokenId, container)
        newState = BattlefieldEntry.place(newState, controllerId, tokenId)

        val event = ZoneChangeEvent(
            entityId = tokenId,
            entityName = cardDef.name,
            fromZone = null,
            toZone = Zone.BATTLEFIELD,
            ownerId = controllerId
        )

        val (stateWithCounters, counterEvents) = EntersWithCountersHelper.applyGlobalEntersWithCounters(
            newState, tokenId, controllerId
        )

        return EffectResult.success(stateWithCounters, listOf(event) + counterEvents)
    }
}
