package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReturnSelfFromExileTransformedEffect] (CR 702.167a).
 *
 * The source has been exiled by the paired [com.wingedsheep.sdk.scripting.AbilityCost.Craft]
 * cost. This executor:
 *  1. Flips the source's [DoubleFacedComponent] to its back face and rewrites
 *     [CardComponent] to the back face's printed characteristics.
 *  2. Re-attaches the [CraftedFromExiledComponent] recorded on the source during cost
 *     payment — preserved across the exile sojourn — so it survives
 *     [com.wingedsheep.engine.handlers.effects.ZoneTransitionService.applyBattlefieldEntry]'s
 *     general battlefield-entry strip (Rule 400.7).
 *  3. Moves the source from EXILE → BATTLEFIELD under its owner's control via the standard
 *     zone-transition pipeline (ETB triggers, static-ability registration, etc.).
 *
 * Does **not** emit a `TransformedEvent` — CR 701.27a defines transforming as turning over
 * a permanent that is already on the battlefield. Craft's "return ... transformed" produces
 * a new battlefield object with its back face up; no transform action occurs, so triggers
 * keyed on `Triggers.TransformsToBack` / `TransformsToFront` must not fire. ETB triggers on
 * the back face are dispatched normally by the `ZoneChangeEvent` emitted from the move.
 */
class ReturnSelfFromExileTransformedExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<ReturnSelfFromExileTransformedEffect> {

    override val effectType: KClass<ReturnSelfFromExileTransformedEffect> =
        ReturnSelfFromExileTransformedEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnSelfFromExileTransformedEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "Craft effect has no source")
        val container = state.getEntity(sourceId)
            ?: return EffectResult.error(state, "Craft source not found")

        val dfc = container.get<DoubleFacedComponent>()
            ?: return EffectResult.error(state, "Craft source is not a double-faced permanent")

        val backDef = cardRegistry.getCard(dfc.backCardDefinitionId)
            ?: return EffectResult.error(state, "Back face not registered: ${dfc.backCardDefinitionId}")

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: return EffectResult.error(state, "Craft source has no owner")

        val materials = container.get<CraftedFromExiledComponent>()

        // Snapshot the front-face CardComponent (the entity is currently in exile, so per
        // CR 712.8a the component on it is the front face's). Stashing it on the DFC lets
        // ZoneTransitionService restore Saheeli's Lattice when Mastercraft Raptor later
        // leaves the battlefield to a non-battlefield/non-stack zone — same path
        // TransformEffectExecutor uses for transforming DFCs.
        val frontFaceCard = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Craft source has no CardComponent")

        // Build the back face's CardComponent via the same helper TransformEffectExecutor uses.
        val backCard = buildCardComponentForDfcFace(frontFaceCard, backDef)

        // Flip the DFC face + swap CardComponent while the entity is still in exile so the
        // zone transition picks up the back face's static abilities cleanly. Save the front
        // face on the DFC so Rule 712.8a's restore-on-leave path can swap back to it.
        var newState = state.updateEntity(sourceId) { c ->
            c.with(backCard).with(
                dfc.copy(
                    currentFace = DoubleFacedComponent.Face.BACK,
                    frontFaceCard = frontFaceCard
                )
            )
        }

        // Move EXILE → BATTLEFIELD under owner's control.
        val transition = ZoneTransitionService.moveToZone(
            newState,
            sourceId,
            Zone.BATTLEFIELD,
            options = ZoneEntryOptions(controllerId = ownerId)
        )
        newState = transition.state

        // Re-attach CraftedFromExiledComponent: ZoneTransitionService.applyBattlefieldEntry
        // strips it as part of the Rule 400.7 "new object" cleanup (alongside
        // LinkedExileComponent), so this is the deliberate re-establishment of the materials
        // link on the craft-return entry path.
        if (materials != null && newState.getEntity(sourceId) != null) {
            newState = newState.updateEntity(sourceId) { c -> c.with(materials) }
        }

        return EffectResult.success(newState, transition.events)
    }
}
