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
import com.wingedsheep.engine.state.permissions.removeMayPlayPermission
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

        // "Cast it transformed" needs a face to turn over to. A card with no back face — a token,
        // or a single-faced card that became a copy of a transforming one — simply isn't cast, and
        // per the CR 310.11b ruling it stays where it is (in exile, for the Siege defeat trigger)
        // rather than being cast front face up.
        if (effect.castTransformed && !hasBackFace(state, cardId)) {
            return EffectResult.success(state)
        }

        // Check targeting *before* granting: a grant made ahead of a cast that never happens
        // would follow the card out of exile and stay live until end-of-turn cleanup.
        val prep = prepareTargetSelection(
            state, cardId, controllerId, cardRegistry, targetFinder, effect.storeCastTo,
            castTransformed = effect.castTransformed,
        )
        if (prep is TargetPrep.NoLegalTargets) {
            // CR 601.2c — if no legal targets exist for a required slot, the cast can't
            // initiate; the chosen card simply stays where it is.
            return EffectResult.success(state)
        }

        // payManaCost casts route through the normal cost (Kaervek, the Punisher — "you may cast
        // the copy"); only the free-cast path stamps PlayWithoutPayingCostComponent. Both grant a
        // MayPlayPermission so the card is castable from its current (e.g. exile) zone.
        val (permId, newState) = grantFreeCast(
            state = state,
            cardId = cardId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            withoutPayingCost = !effect.payManaCost,
            castTransformed = effect.castTransformed,
        )

        if (prep is TargetPrep.NeedsTargets) {
            val pausedState = newState
                .pushContinuation(prep.continuation.copy(grantedPermissionId = permId))
                .withPendingDecision(prep.decision)
                .withPriority(controllerId)
            return EffectResult.paused(pausedState, prep.decision, listOf(prep.event))
        }

        // No targets needed (or modal — CastSpellHandler will handle per-mode targets).
        return invokeCast(newState, controllerId, cardId, permId, emptyList(), effect.storeCastTo)
    }

    /** True when [cardId]'s definition has a back face to be cast transformed as. */
    private fun hasBackFace(state: GameState, cardId: EntityId): Boolean {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        return cardRegistry.getCard(cardComponent.cardDefinitionId)?.backFace != null
    }

    private fun invokeCast(
        state: GameState,
        casterId: EntityId,
        cardId: EntityId,
        grantedPermissionId: EntityId,
        targets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        storeCastTo: String?,
    ): EffectResult {
        val stateForCast = state.copy(priorityPlayerId = casterId)
        val castResult = castSpellHandlerProvider().execute(
            stateForCast,
            CastSpell(casterId, cardId, targets),
        )

        if (castResult.error != null) {
            return EffectResult.success(revokeFreeCast(state, cardId, grantedPermissionId))
        }

        // The cast initiated (synchronously or pausing for X / further input). Publish the cast
        // card so an enclosing IfYouDoEffect can gate a follow-up on "if you do" (Kaervek).
        val castCollections = storeCastTo?.let { mapOf(it to listOf(cardId)) } ?: emptyMap()

        if (castResult.pendingDecision != null) {
            return EffectResult.paused(
                castResult.state,
                castResult.pendingDecision,
                castResult.events,
            ).copy(
                updatedCollections = castCollections,
                triggersAlreadyProcessed = castResult.triggersAlreadyProcessed,
            )
        }

        // CastSpellHandler already detected + stacked this cast's triggers; propagate the flag so a
        // resuming caller (e.g. the gated MayEffect resumer -> SubmitDecisionHandler) doesn't re-scan
        // the SpellCastEvent and double-fire "whenever you cast a spell" triggers.
        return EffectResult.success(castResult.state, castResult.events)
            .copy(
                updatedCollections = castCollections,
                triggersAlreadyProcessed = castResult.triggersAlreadyProcessed,
            )
    }

    /** Outcome of [prepareTargetSelection] for a synthesized free/granted cast. */
    sealed interface TargetPrep {
        /** Targetless or modal spell — invoke `CastSpellHandler` directly; it drives any further prompts. */
        data object NotNeeded : TargetPrep

        /** A required target slot has no legal targets — the cast can't initiate (CR 601.2c). */
        data object NoLegalTargets : TargetPrep

        /** Pause with [decision] and push [continuation]; the resumer performs the cast with the picks. */
        data class NeedsTargets(
            val decision: ChooseTargetsDecision,
            val continuation: CastFromCollectionTargetsContinuation,
            val event: DecisionRequestedEvent,
        ) : TargetPrep
    }

    companion object {
        /**
         * Grant [controllerId] a one-shot cast of [cardId] from its current zone: a
         * [MayPlayPermission] covering the card and — unless [withoutPayingCost] is false
         * (Kaervek's "you may cast the copy", paying its cost) — a [PlayWithoutPayingCostComponent]
         * making the cast free. Returns the permission id so a cast that never happens can revoke
         * exactly this grant via [revokeFreeCast].
         *
         * Callers must only grant once the cast is known to be attemptable
         * ([prepareTargetSelection] first): the component is zone-agnostic and lives until
         * end-of-turn cleanup, so a leftover grant would let the card be cast for free from
         * wherever it lands (hand, library, exile).
         */
        fun grantFreeCast(
            state: GameState,
            cardId: EntityId,
            controllerId: EntityId,
            sourceId: EntityId?,
            withoutPayingCost: Boolean = true,
            castTransformed: Boolean = false,
        ): Pair<EntityId, GameState> {
            val stamped = if (!withoutPayingCost) state else state.updateEntity(cardId) { container ->
                container.with(PlayWithoutPayingCostComponent(controllerId = controllerId))
            }
            val (permId, stateWithPerm) = stamped.newEntity()
            val granted = stateWithPerm.addMayPlayPermission(
                MayPlayPermission(
                    id = permId,
                    cardIds = setOf(cardId),
                    controllerId = controllerId,
                    sourceId = sourceId,
                    castTransformed = castTransformed,
                    timestamp = stateWithPerm.timestamp,
                )
            )
            return permId to granted
        }

        /** Undo [grantFreeCast] when the synthesized cast didn't happen after all. */
        fun revokeFreeCast(state: GameState, cardId: EntityId, permissionId: EntityId?): GameState {
            val withoutPermission = permissionId?.let { state.removeMayPlayPermission(it) } ?: state
            return withoutPermission.updateEntity(cardId) { container ->
                container.without<PlayWithoutPayingCostComponent>()
            }
        }

        /**
         * Determine whether a synthesized cast of [cardId] must pause for target selection first.
         *
         * Synthesized casts (Cascade, Discover, Sunbird's Invocation, …) can't carry targets
         * through the `CastSpell` action — there is no client action to supply them — and
         * `CastSpellHandler.validate` rejects `targets = []` for required slots (and `execute`,
         * invoked directly, would silently put the spell on the stack untargeted). Non-modal
         * spells with target requirements therefore need a [ChooseTargetsDecision] surfaced
         * before the cast. Modal spells fall through to [TargetPrep.NotNeeded] because
         * `CastSpellHandler` has its own per-mode target-selection pause after mode choice.
         *
         * Includes the aura's enchant target: an Aura carries its target in `auraTarget`, not in
         * `targetRequirements`, so a free-cast Aura (Pacifism) would otherwise reach
         * CastSpellHandler with no target. Mirrors CastSpellHandler's own
         * `targetRequirements + auraTarget` union.
         *
         * [castTransformed] reads all of that off the **back** face instead, because that is the
         * face the spell will have on the stack (CR 712.8c) — the same rule that makes a disturb
         * cast's targets come from the back face.
         */
        fun prepareTargetSelection(
            state: GameState,
            cardId: EntityId,
            casterId: EntityId,
            cardRegistry: CardRegistry,
            targetFinder: TargetFinder,
            storeCastTo: String? = null,
            castTransformed: Boolean = false,
        ): TargetPrep {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
            val printedDef = cardComponent?.let { cardRegistry.getCard(it.cardDefinitionId) }
            val cardDef = if (castTransformed) printedDef?.backFace ?: printedDef else printedDef
            val isModalSpell = cardDef?.script?.spellEffect is ModalEffect
            val targetRequirements = buildList {
                addAll(cardDef?.script?.targetRequirements.orEmpty())
                cardDef?.script?.auraTarget?.let { add(it) }
            }
            if (isModalSpell || targetRequirements.isEmpty()) {
                return TargetPrep.NotNeeded
            }

            val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
            val requirementInfos = targetRequirements.mapIndexed { index, requirement ->
                val legal = targetFinder.findLegalTargets(
                    state = state,
                    requirement = requirement,
                    controllerId = casterId,
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
                return TargetPrep.NoLegalTargets
            }

            // Name the face being cast — a transformed cast prompts for "Deluge of the Dead",
            // not for the front face the player exiled.
            val cardName = (if (castTransformed) cardDef?.name else null) ?: cardComponent?.name ?: "spell"
            val decisionId = UUID.randomUUID().toString()
            val decision = ChooseTargetsDecision(
                id = decisionId,
                playerId = casterId,
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
                casterId = casterId,
                storeCastTo = storeCastTo,
            )
            val event = DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = casterId,
                decisionType = "CHOOSE_TARGETS",
                prompt = decision.prompt,
            )
            return TargetPrep.NeedsTargets(decision, continuation, event)
        }

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
