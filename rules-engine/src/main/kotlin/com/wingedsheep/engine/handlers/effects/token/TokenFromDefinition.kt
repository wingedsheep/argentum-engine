package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldEntry
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice

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
 * cast.
 *
 * Because the token is put onto the battlefield directly (not cast), it runs the same "as-enters"
 * replacement effects (CR 614) a directly-entering permanent would — mirroring
 * [com.wingedsheep.engine.handlers.actions.land.PlayLandHandler]:
 *  - **[EntersTapped]** (Diregraf Ghoul, "enters tapped") taps the token. The shock-land
 *    `payLifeCost` form is land-only, so a minted creature only ever has the simple form.
 *  - **self + global [EntersWithCounters]/[EntersWithDynamicCounters]** add counters (a creature
 *    that "enters with N +1/+1 counters", plus grants from other permanents) via
 *    [EntersWithCountersHelper].
 *  - **[EntersWithChoice]** (Alloy Golem, "as this enters, choose a color") pauses for a player
 *    decision via [PermanentEntryReplacements]; the chosen value is recorded in
 *    `CastChoicesComponent` and the token's ETB triggers fire from the resumer afterward.
 *
 * "When ~ enters" *triggered* abilities fire off the emitted [ZoneChangeEvent] (when there is no
 * choice) or from the [EntersWithChoiceOnBattlefieldContinuation] resumer (when a choice paused —
 * we deliberately omit the [ZoneChangeEvent] in that case so the triggers aren't detected twice).
 *
 * (Rarer as-enters replacements that pause for their own decision — Amplify reveal, Devour
 * sacrifice, EntersAsCopy — are not applied to minted tokens; a minted copy of such a creature
 * simply enters with zero amplify/devour counters. They are vanishingly rare in a random-creature
 * pool and would require generalizing their spell-keyed continuations.)
 */
object TokenFromDefinition {

    private val conditionEvaluator = ConditionEvaluator()

    fun mint(
        state: GameState,
        cardDef: CardDefinition,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
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

        // As-enters: enters tapped (CR 614). The payLifeCost (shock-land) form is land-only.
        val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
        if (entersTapped != null && entersTapped.payLifeCost == null) {
            val shouldEnterTapped = entersTapped.unlessCondition?.let { condition ->
                !conditionEvaluator.evaluate(
                    newState, condition, EffectContext(sourceId = tokenId, controllerId = controllerId)
                )
            } ?: true
            if (shouldEnterTapped) {
                newState = newState.updateEntity(tokenId) { c -> c.with(TappedComponent) }
            }
        }

        // As-enters: the token's own + global "enters with counters" (CR 614).
        val (stateWithCounters, counterEvents) = EntersWithCountersHelper.applyEntersWithCounters(
            newState, tokenId, controllerId, cardRegistry
        )
        newState = stateWithCounters

        // As-enters: "choose X as this enters" (CR 614.12). Pauses for a player decision; the
        // resumer records the choice and fires the token's ETB triggers off a synthesized
        // ZoneChangeEvent — so we deliberately do NOT emit the entry ZoneChangeEvent here when we
        // pause (the resolution path would otherwise detect those triggers now AND again in the
        // resumer, firing them twice). Counters already added ride along as carryEvents.
        val firstChoice = cardDef.script.replacementEffects
            .filterIsInstance<EntersWithChoice>()
            .sortedBy { it.choiceType.ordinal }
            .firstOrNull()
        if (firstChoice != null) {
            val cardComponent = newState.getEntity(tokenId)?.get<CardComponent>()
            if (cardComponent != null) {
                val paused = PermanentEntryReplacements.pauseForEntersWithChoice(
                    state = newState,
                    entityId = tokenId,
                    controllerId = controllerId,
                    cardComponent = cardComponent,
                    choice = firstChoice,
                    fromZone = null,
                    carryEvents = counterEvents
                )
                if (paused != null) return EffectResult.from(paused)
            }
        }

        val event = ZoneChangeEvent(
            entityId = tokenId,
            entityName = cardDef.name,
            fromZone = null,
            toZone = Zone.BATTLEFIELD,
            ownerId = controllerId
        )

        return EffectResult.success(newState, listOf(event) + counterEvents)
    }
}
