package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CastFromCollectionTargetsContinuation
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [CastFromCollectionWithoutPayingCostEffect].
 *
 * Reads the (typically 0..1) cards stored under [CastFromCollectionWithoutPayingCostEffect.from],
 * grants the first one free-cast permission, then casts it through the normal pipeline.
 *
 * Two paths after granting permission:
 *  - **Targetless or modal spell**: invoke `CastSpellHandler` directly. The handler pauses
 *    for X / mode prompts as needed; targetless spells (Goblin Sledder, Cultivate) resolve
 *    inline.
 *  - **Targeted spell** (`script.targetRequirements.isNotEmpty()`): build a
 *    [ChooseTargetsDecision] over the legal target pool, push a
 *    [CastFromCollectionTargetsContinuation], and pause. The resumer in
 *    `LibraryAndZoneContinuationResumer` consumes the player's picks, builds the
 *    `CastSpell` action with chosen targets, and invokes the handler.
 *
 * Empty collection → no-op. No legal targets for a required requirement → no-op (the card
 * stays where it is). Cast fails to initiate → no-op.
 *
 * Modal spells with per-mode target requirements fall back to the direct path because
 * `CastSpellHandler` already has its own modal target-selection pause flow
 * (`presentCastModalTargetDecision`) that triggers after mode selection.
 *
 * The handler is provided lazily so the executor can be constructed before
 * [com.wingedsheep.engine.core.EngineServices] has finished wiring the rest of the rules
 * engine.
 */
class CastFromCollectionWithoutPayingCostExecutor(
    private val castSpellHandlerProvider: () -> CastSpellHandler,
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder = TargetFinder(),
) : EffectExecutor<CastFromCollectionWithoutPayingCostEffect> {

    override val effectType: KClass<CastFromCollectionWithoutPayingCostEffect> =
        CastFromCollectionWithoutPayingCostEffect::class

    override fun execute(
        state: GameState,
        effect: CastFromCollectionWithoutPayingCostEffect,
        context: EffectContext,
    ): EffectResult {
        val cards: List<EntityId> = context.pipeline.storedCollections[effect.from].orEmpty()
        if (cards.isEmpty()) return EffectResult.success(state)
        val cardId = cards.first()
        val controllerId = context.controllerId

        // payManaCost casts route through the normal cost (Kaervek, the Punisher — "you may cast
        // the copy"); only the free-cast path stamps PlayWithoutPayingCostComponent. Both grant a
        // MayPlayPermission so the card is castable from its current (e.g. exile) zone.
        var newState = if (effect.payManaCost) state else state.updateEntity(cardId) { container ->
            container.with(PlayWithoutPayingCostComponent(controllerId = controllerId))
        }
        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(cardId),
                controllerId = controllerId,
                sourceId = context.sourceId,
                timestamp = newState.timestamp,
            )
        )

        val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val isModalSpell = cardDef?.script?.spellEffect is ModalEffect
        // Include the aura's enchant target: an Aura carries its target in `auraTarget`, not in
        // `targetRequirements`, so a free-cast Aura (Pacifism) would otherwise reach CastSpellHandler
        // with no target — which it rejects ("No valid targets available"), stranding the card in
        // exile. Mirror CastSpellHandler's own `targetRequirements + auraTarget` union here.
        val targetRequirements = buildList {
            addAll(cardDef?.script?.targetRequirements.orEmpty())
            cardDef?.script?.auraTarget?.let { add(it) }
        }

        // Modal spells route through CastSpellHandler's own target-pause flow after mode
        // selection. Non-modal targeted spells (including Auras) need us to surface a
        // ChooseTargetsDecision first because CastSpellHandler.validate rejects `targets = []`
        // for required slots.
        if (!isModalSpell && targetRequirements.isNotEmpty()) {
            val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
            val requirementInfos = targetRequirements.mapIndexed { index, requirement ->
                val legal = targetFinder.findLegalTargets(
                    state = newState,
                    requirement = requirement,
                    controllerId = controllerId,
                    sourceId = cardId,
                )
                legalTargetsMap[index] = legal
                TargetRequirementInfo(
                    index = index,
                    description = requirement.description,
                    minTargets = requirement.effectiveMinCount,
                    maxTargets = requirement.count,
                )
            }
            val mandatoryRequirementHasNoTargets = requirementInfos.any { info ->
                info.minTargets > 0 && legalTargetsMap[info.index].isNullOrEmpty()
            }
            if (mandatoryRequirementHasNoTargets) {
                // CR 601.2c — if no legal targets exist for a required slot, the cast can't
                // initiate; the chosen card simply stays where it is.
                return EffectResult.success(newState)
            }

            val cardName = cardComponent?.name ?: "spell"
            val decisionId = UUID.randomUUID().toString()
            val decision = ChooseTargetsDecision(
                id = decisionId,
                playerId = controllerId,
                prompt = "Choose targets for $cardName",
                context = DecisionContext(
                    sourceId = cardId,
                    sourceName = cardName,
                    phase = DecisionPhase.CASTING,
                ),
                targetRequirements = requirementInfos,
                legalTargets = legalTargetsMap,
                canCancel = false,
            )
            val continuation = CastFromCollectionTargetsContinuation(
                decisionId = decisionId,
                cardId = cardId,
                casterId = controllerId,
                storeCastTo = effect.storeCastTo,
            )
            val pausedState = newState
                .pushContinuation(continuation)
                .withPendingDecision(decision)
                .withPriority(controllerId)

            return EffectResult.paused(
                pausedState,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = controllerId,
                        decisionType = "CHOOSE_TARGETS",
                        prompt = decision.prompt,
                    )
                ),
            )
        }

        // No targets needed (or modal — CastSpellHandler will handle per-mode targets).
        return invokeCast(newState, controllerId, cardId, emptyList(), effect.storeCastTo)
    }

    private fun invokeCast(
        state: GameState,
        casterId: EntityId,
        cardId: EntityId,
        targets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        storeCastTo: String?,
    ): EffectResult {
        val stateForCast = state.copy(priorityPlayerId = casterId)
        val castResult = castSpellHandlerProvider().execute(
            stateForCast,
            CastSpell(casterId, cardId, targets),
        )

        if (castResult.error != null) {
            return EffectResult.success(state)
        }

        // The cast initiated (synchronously or pausing for X / further input). Publish the cast
        // card so an enclosing IfYouDoEffect can gate a follow-up on "if you do" (Kaervek).
        val castCollections = storeCastTo?.let { mapOf(it to listOf(cardId)) } ?: emptyMap()

        if (castResult.pendingDecision != null) {
            return EffectResult.paused(
                castResult.state,
                castResult.pendingDecision,
                castResult.events,
            ).copy(updatedCollections = castCollections)
        }

        return EffectResult.success(castResult.state, castResult.events)
            .copy(updatedCollections = castCollections)
    }

    companion object {
        /**
         * Shared `state + targets → ExecutionResult` shim used by both the inline
         * (no-targets) path and the targets-continuation resumer.
         */
        fun castWithTargets(
            state: GameState,
            casterId: EntityId,
            cardId: EntityId,
            targets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
            castSpellHandler: CastSpellHandler,
        ): com.wingedsheep.engine.core.ExecutionResult {
            val stateForCast = state.copy(priorityPlayerId = casterId)
            return castSpellHandler.execute(stateForCast, CastSpell(casterId, cardId, targets))
        }
    }
}
