package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.combat.installPreventAndReactShield
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId

class CombatContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(BlockerPilesContinuation::class) { state, continuation, response, _ ->
            if (response !is PilesSplitResponse) ExecutionResult.error(state, "Expected piles")
            else finishPileDeclaration(services.combatManager.resolveBlockerPiles(state, continuation, response))
        },
        resumer(BlockerPileRestrictionChoiceContinuation::class) { state, continuation, response, _ ->
            if (response !is PilesSplitResponse) ExecutionResult.error(state, "Expected piles")
            else finishPileDeclaration(services.combatManager.resolvePileRestrictions(state, continuation, response))
        },
        resumer(DamageAssignmentContinuation::class) { state, continuation, response, _ ->
            resumeDamageAssignment(state, continuation, response)
        },
        questionResumer(CombatResolutionContinuation::class) { state, continuation, question, response, _ ->
            resumeCombatResolution(state, continuation, question, response)
        },
        resumer(AssignAsUnblockedContinuation::class) { state, continuation, response, _ ->
            resumeAssignAsUnblocked(state, continuation, response)
        },
        questionResumer(AssignUnblockedToCreatureContinuation::class) { state, continuation, question, response, _ ->
            resumeAssignUnblockedToCreature(state, continuation, question, response)
        },
        resumer(DamagePreventionContinuation::class, ::resumeDamagePrevention),
        resumer(DistributeDamageContinuation::class, ::resumeDistributeDamage),
        resumer(DeflectDamageSourceChoiceContinuation::class, ::resumeDeflectDamageSourceChoice),
        resumer(PreventDamageFromChosenSourceContinuation::class, ::resumePreventDamageFromChosenSource),
        resumer(PreventNextDamageLeavingAmountContinuation::class, ::resumePreventNextDamageLeavingAmount),
        resumer(RedirectDamageSourceContinuation::class, ::resumeRedirectDamageSource),
        resumer(CombatOptionalRedirectContinuation::class) { state, continuation, response, _ ->
            resumeCombatOptionalRedirect(state, continuation, response)
        },
        resumer(OptionalRedirectEffectContinuation::class, ::resumeOptionalRedirectEffect)
    )

    private fun finishPileDeclaration(result: ExecutionResult): ExecutionResult {
        if (result.error != null || result.pendingDecision != null) return result
        val next = CombatDefenders.defendingPlayersInApnapOrder(result.state).firstOrNull {
            result.state.getEntity(it)?.has<com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent>() != true
        }
        val priorityPlayer = next ?: result.state.activePlayerId ?: return result
        return ExecutionResult.success(result.state.withPriority(priorityPlayer), result.events)
    }

    /**
     * Record one "you may have that damage dealt to you instead" answer and re-run the combat damage
     * step, which then asks about the next instance the shield covers (or deals the damage).
     */
    fun resumeCombatOptionalRedirect(
        state: GameState,
        continuation: CombatOptionalRedirectContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for optional damage redirection")
        }
        val recorded = com.wingedsheep.engine.handlers.effects.damage.OptionalDamageRedirect
            .record(state, continuation.choiceKey, response.choice)
        return services.combatManager.applyCombatDamage(recorded, firstStrike = continuation.firstStrike)
    }

    /** The non-combat counterpart: record the answer, then re-run the damage effect that asked. */
    fun resumeOptionalRedirectEffect(
        state: GameState,
        continuation: OptionalRedirectEffectContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for optional damage redirection")
        }
        val recorded = com.wingedsheep.engine.handlers.effects.damage.OptionalDamageRedirect
            .record(state, continuation.choiceKey, response.choice)
        val result = services.effectExecutorRegistry.execute(
            recorded,
            continuation.effect,
            continuation.effectContext
        )
        if (result.outcome is Outcome.Paused) {
            return ExecutionResult.propagatePause(result.state, result.events)
        }
        return checkForMore(result.state, result.events)
    }

    fun resumeDamageAssignment(
        state: GameState,
        continuation: DamageAssignmentContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        val assignments = when (response) {
            is DistributionResponse -> response.distribution
            is DamageAssignmentResponse -> response.assignments
            else -> return ExecutionResult.error(state, "Expected distribution or damage assignment response")
        }

        val newState = state.updateEntity(continuation.attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(
                    assignments
                )
            )
        }

        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    fun resumeAssignAsUnblocked(
        state: GameState,
        continuation: AssignAsUnblockedContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for assign-as-unblocked decision")
        }

        val newState = if (response.choice) {
            // Player chose to assign damage to the defending player — store a manual assignment
            // The amount it assigns, not its raw power: Doran-style "assigns equal to toughness"
            // and "assigns no combat damage" riders still apply to the redirected assignment.
            val power = com.wingedsheep.engine.mechanics.combat.CombatDamageUtils.getAssignedCombatDamage(
                state, state.projectedState, continuation.attackerId, services.cardRegistry,
                predicateEvaluator = services.predicateEvaluator
            )
            state.updateEntity(continuation.attackerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(
                        mapOf(continuation.defendingPlayerId to power)
                    )
                )
            }
        } else {
            // Declining suppresses the bypass question, but leaves normal assignment open:
            // the combat board still asks when blockers allow a choice, otherwise it auto-distributes.
            state.updateEntity(continuation.attackerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(emptyMap())
                )
            }
        }

        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    /**
     * Record the chosen creature as the unblocked attacker's whole assignment, or an empty
     * assignment (assign to what it's attacking, as normal) when the player declined.
     */
    fun resumeAssignUnblockedToCreature(
        state: GameState,
        continuation: AssignUnblockedToCreatureContinuation,
        question: PendingDecision,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection for unblocked damage assignment")
        }
        val options = (question as? SelectCardsDecision)?.options.orEmpty()
        val chosen = response.selectedCards.singleOrNull()
        if (response.selectedCards.size > 1 || (chosen != null && chosen !in options)) {
            return ExecutionResult.error(state, "Invalid creature for unblocked damage assignment")
        }
        val assignments = if (chosen == null) emptyMap() else {
            val amount = com.wingedsheep.engine.mechanics.combat.CombatDamageUtils.getAssignedCombatDamage(
                state, state.projectedState, continuation.attackerId, services.cardRegistry,
                predicateEvaluator = services.predicateEvaluator
            )
            mapOf(chosen to amount)
        }
        val newState = state.updateEntity(continuation.attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(assignments)
            )
        }
        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    /**
     * Apply a [CombatResolutionResponse] (the combat-damage board).
     *
     * The current chooser is `continuation.pendingChoosers.first()`. We honor only the edges they
     * own (filtered by [DamageEdge.editableBy] on the paired question), bake those amounts
     * on top of the shape's current amounts, and:
     *
     * - if more choosers remain (CR 510.1c sequencing, or the CR 702.22j/k two-actor banding case),
     *   re-pause via [com.wingedsheep.engine.mechanics.combat.CombatManager.repauseCombatResolution]
     *   for the next chooser with the locked-in amounts shown;
     * - otherwise fold every edge into a per-source [DamageAssignmentComponent] (read straight off
     *   the cached edge objects — no edge-id parsing), apply any row-order overrides, and re-enter
     *   `applyCombatDamage` to run the damage pipeline.
     */
    fun resumeCombatResolution(
        state: GameState,
        continuation: CombatResolutionContinuation,
        question: PendingDecision,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is CombatResolutionResponse) {
            return ExecutionResult.error(state, "Expected combat resolution response for combat resolution decision")
        }

        val shape = question as? CombatResolutionDecision
            ?: return ExecutionResult.error(state, "Expected paired combat resolution question")
        val submittingPlayer = continuation.pendingChoosers.firstOrNull()
        val remainingChoosers = continuation.pendingChoosers.drop(1)

        val edgeById = shape.edges.associateBy { it.id }
        val submittedByEdge = response.edges.associate { it.edgeId to it.amount }
        // Keep only edges this chooser owns; unknown ids and other-owner edges are dropped.
        val honoredByEdge = submittedByEdge.filter { (edgeId, _) ->
            submittingPlayer != null && edgeById[edgeId]?.editableBy == submittingPlayer
        }
        // Bake onto the shape's current amounts so the next chooser (if any) sees locked-in values.
        val accumulatedAmounts: Map<String, Int> = shape.edges.associate { it.id to it.amount } + honoredByEdge

        if (remainingChoosers.isNotEmpty()) {
            return services.combatManager.repauseCombatResolution(
                state = state,
                previous = shape,
                remainingChoosers = remainingChoosers,
                latestAmounts = accumulatedAmounts,
                firstStrike = continuation.firstStrike,
            )
        }

        // All choosers confirmed — write per-source DamageAssignmentComponents from the edge objects.
        val assignmentsBySource = mutableMapOf<EntityId, MutableMap<EntityId, Int>>()
        for ((edgeId, amount) in accumulatedAmounts) {
            val edge = edgeById[edgeId] ?: continue
            assignmentsBySource.getOrPut(edge.sourceId) { mutableMapOf() }[edge.targetId] = amount
        }

        var newState = state
        for ((sourceId, assignments) in assignmentsBySource) {
            newState = newState.updateEntity(sourceId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(assignments)
                )
            }
        }
        for ((attackerId, order) in response.orderedBlockers) {
            newState = newState.updateEntity(attackerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent(order)
                )
            }
        }
        for ((blockerId, order) in response.orderedAttackers) {
            newState = newState.updateEntity(blockerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.AttackerOrderComponent(order)
                )
            }
        }

        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    fun resumeDamagePrevention(
        state: GameState,
        continuation: DamagePreventionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for damage prevention")
        }

        val updatedEffects = state.floatingEffects.toMutableList()
        val shieldIndex = updatedEffects.indexOfFirst { it.id == continuation.shieldEffectId }
        val originalShield = if (shieldIndex >= 0) updatedEffects.removeAt(shieldIndex) else null

        val timestamp = originalShield?.timestamp ?: state.timestamp
        var workingState = state
        val gainsLife = (originalShield?.effect?.modification as? SerializableModification.PreventNextDamage)
            ?.controllerGainsLife ?: false
        for ((sourceId, preventionAmount) in response.distribution) {
            if (preventionAmount <= 0) continue
            val (effectId, advanced) = workingState.newEntity()
            workingState = advanced
            val splitEffectData = FloatingEffectData(
                layer = Layer.ABILITY,
                modification = SerializableModification.PreventNextDamage(
                    preventionAmount,
                    onlyFromSource = sourceId,
                    controllerGainsLife = gainsLife
                ),
                affectedEntities = setOf(continuation.recipientId)
            )
            updatedEffects.add(
                // Copy the original shield so every field carries — including duration-specific
                // bookkeeping such as `expiresAfterTurn`. Rebuilding field-by-field silently drops
                // whatever the next duration adds. `timestamp` is restated rather than left to the
                // copy: it is the same value either way (it is derived from the original shield
                // above), and saying so keeps the split pieces pinned to the shield's own Rule 613
                // ordering if that derivation ever changes.
                originalShield?.copy(id = effectId, effect = splitEffectData, timestamp = timestamp)
                    ?: ActiveFloatingEffect(
                        id = effectId,
                        effect = splitEffectData,
                        duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
                        sourceId = null,
                        sourceName = null,
                        controllerId = continuation.recipientId,
                        timestamp = timestamp
                    )
            )
        }

        val newState = workingState.copy(floatingEffects = updatedEffects)

        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    fun resumeDistributeDamage(
        state: GameState,
        continuation: DistributeDamageContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for divided damage")
        }

        val distribution = response.distribution
        val events = mutableListOf<GameEvent>()
        var newState = state

        for ((targetId, damageAmount) in distribution) {
            if (damageAmount > 0) {
                val result = DamageUtils.dealDamageToTarget(
                    services.zones,
                    newState,
                    targetId,
                    damageAmount,
                    continuation.sourceId, damageSourceRef = continuation.objectReferences.origin ?: continuation.sourceId?.let(state::objectRef)
                )

                // Dealing damage never asks a question, so the only other outcome is a rejection.
                if (result.outcome is Outcome.Rejected) {
                    return ExecutionResult(newState, events, result.outcome)
                }

                newState = result.state
                events.addAll(result.events)
            }
        }

        return checkForMore(newState, events)
    }

    fun resumeDeflectDamageSourceChoice(
        state: GameState,
        continuation: DeflectDamageSourceChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected cards selected response for source selection")
        }

        val chosenSourceId = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No source selected")

        val newState = state.installPreventAndReactShield(
            damageSourceId = chosenSourceId,
            protectedEntityId = continuation.controllerId,
            controllerId = continuation.controllerId,
            effectSourceId = continuation.sourceId,
            effectSourceName = continuation.sourceName,
            onPrevented = continuation.onPrevented,
            preventDamage = continuation.preventDamage,
            objectReferences = continuation.objectReferences
        )

        return checkForMore(newState, emptyList())
    }

    fun resumeRedirectDamageSource(
        state: GameState, continuation: RedirectDamageSourceContinuation,
        response: DecisionResponse, checkForMore: CheckForMore
    ): ExecutionResult {
        val chosen = when (response) {
            is CardsSelectedResponse -> continuation.choices.singleOrNull { it.reference.entityId == response.selectedCards.singleOrNull() }
            is OptionChosenResponse -> continuation.choices.getOrNull(response.optionIndex)
            else -> null
        } ?: return ExecutionResult.error(state, "Expected a legal damage source choice")
        val next = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RedirectNextDamage(
                redirectToId = continuation.redirectToId, chosenSource = chosen,
                protectedRef = continuation.protectedRef, redirectToRef = continuation.redirectToRef
            ),
            affectedEntities = setOf(continuation.protectedId), duration = continuation.duration,
            context = EffectContext(sourceId = continuation.sourceId, controllerId = continuation.controllerId,
                objectReferences = continuation.objectReferences)
        )
        return checkForMore(next, emptyList())
    }

    private fun resumePreventNextDamageLeavingAmount(
        state: GameState,
        continuation: PreventNextDamageLeavingAmountContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse || response.selectedCards.size != 1) {
            return ExecutionResult.error(state, "Choose exactly one damage source")
        }
        val choice = continuation.choices.singleOrNull { it.reference.entityId == response.selectedCards.single() }
            ?: return ExecutionResult.error(state, "Invalid damage source")
        if (!state.isCurrentObject(choice.reference)) return checkForMore(state, emptyList())
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.PreventNextDamageLeavingAmount(
                damageSourceId = choice.reference.entityId,
                sourceName = com.wingedsheep.engine.state.nameVisibleToAll(state, choice.reference.entityId, choice.name),
                amountToLeave = continuation.amountToLeave,
                eligibleSource = continuation.eligibleSource,
                combatOnly = continuation.scope == com.wingedsheep.sdk.scripting.effects.PreventionScope.CombatOnly,
                permanentSpell = choice.permanentSpell
            ),
            affectedEntities = setOf(continuation.targetId),
            duration = continuation.duration,
            context = continuation.context
        )
        return checkForMore(newState, listOf(DamagePreventionShieldCreatedEvent(
            continuation.targetId, newState.floatingEffects.last().id
        )))
    }

    fun resumePreventDamageFromChosenSource(
        state: GameState,
        continuation: PreventDamageFromChosenSourceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected cards selected response for source selection")
        }

        val chosenSourceId = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No source selected")

        val context = EffectContext(
            sourceId = continuation.sourceId,
            objectReferences = continuation.objectReferences,
            controllerId = continuation.controllerId,
        )
        // "Prevent all damage that would be dealt this turn by a source of your choice", with no
        // recipient clause (Mourner's Shield): the shield belongs on the *source*, not on a
        // protected recipient, so it reuses the same `PreventAllDamageDealtBy` silence shield that a
        // targeted `PreventionDirection.FromTarget` installs — and is honored for combat and
        // noncombat damage alike by the same two read sites.
        if (continuation.silenceChosenSource && continuation.amount == null) {
            val silenced = state.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.PreventAllDamageDealtBy,
                affectedEntities = setOf(chosenSourceId),
                duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
                context = context
            )
            return checkForMore(silenced, emptyList())
        }

        val modification = if (continuation.amount == null && continuation.nextInstanceOnly) {
            // "The next time that source would deal damage to you this turn, prevent that damage"
            // (Circle of Protection family) — single instance, then consumed.
            SerializableModification.PreventNextDamageInstanceFromSource(
                damageSourceId = chosenSourceId,
                halveRoundedDown = continuation.halvePreventedDamage
            )
        } else if (continuation.amount == null) {
            // Prevent all damage from the chosen source for the rest of the turn (Samite Ministration)
            SerializableModification.PreventAllDamageFromSource(
                damageSourceId = chosenSourceId,
                gainLifeFromColors = continuation.gainLifeFromColors
            )
        } else {
            SerializableModification.PreventNextDamage(
                remainingAmount = continuation.amount,
                onlyFromSource = chosenSourceId
            )
        }
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = modification,
            affectedEntities = setOf(continuation.targetId),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            context = context
        )

        return checkForMore(newState, emptyList())
    }
}
