package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ChangeSpellTargetContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect
import com.wingedsheep.sdk.scripting.targets.*
import kotlin.reflect.KClass

/**
 * Executor for ChangeTargetEffect.
 * "Change the target of target spell or ability with a single target."
 *
 * Logic:
 * 1. Get the target spell/ability from context
 * 2. A fixed new target ("…to this creature") replaces one legal slot — see redirectToFixedTarget;
 *    otherwise the spell must have exactly one target, or the effect does nothing
 * 3. Find all legal new targets based on the spell/ability's target requirement
 * 4. Present a selection decision to the controller
 * 5. Push ChangeSpellTargetContinuation (reused)
 */
class ChangeTargetExecutor(
    private val predicateEvaluator: PredicateEvaluator,
    private val targetFinder: TargetFinder
) : EffectExecutor<ChangeTargetEffect> {

    override val effectType: KClass<ChangeTargetEffect> = ChangeTargetEffect::class

    private val decisionHandler = DecisionHandler()
    override fun execute(
        state: GameState,
        effect: ChangeTargetEffect,
        context: EffectContext
    ): EffectResult {
        // 1. Get target spell/ability
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return EffectResult.error(state, "No valid spell/ability target for ChangeTarget")
        }

        val stackEntity = state.getEntity(targetSpell.spellEntityId)
            ?: return EffectResult.error(state, "Spell/ability not found on stack")

        // 2. Get the spell/ability's targets
        val targetsComponent = stackEntity.get<TargetsComponent>()
        val spellTargets = targetsComponent?.targets ?: emptyList()
        val targetRequirements = targetsComponent?.targetRequirements ?: emptyList()

        // The object's own controller judges the new target ("target creature you control"). An
        // activated/triggered ability carries no ControllerComponent, so read the stack component.
        val spellController = TargetResolutionUtils.stackObjectController(state, targetSpell.spellEntityId)
            ?: stackEntity.get<ControllerComponent>()?.playerId
            ?: context.controllerId

        // "…to this creature" (Hydroelectric Specimen, Spellskite): no choice of new target, and any
        // one of several targets may be the one changed — see [redirectToFixedTarget].
        effect.newTarget?.let { fixed ->
            return redirectToFixedTarget(
                state, context, effect, fixed, spellTargets, targetRequirements, spellController,
                targetSpell.spellEntityId
            )
        }

        // A chosen new target needs exactly one current target — if not, the effect does nothing
        if (spellTargets.size != 1) {
            return EffectResult.success(state)
        }

        val currentTarget = spellTargets.first()

        // "…if that target is you" (Reflecting Mirror). Checked at resolution, so a spell whose
        // target changed hands in response is no longer redirectable.
        if (effect.onlyIfCurrentTargetIsController &&
            getTargetEntityId(currentTarget) != context.controllerId
        ) {
            return EffectResult.success(state)
        }

        // 3. Find all legal new targets based on target requirements
        var legalNewTargets = findLegalNewTargets(
            state, currentTarget, targetRequirements, spellController, targetSpell.spellEntityId
        )

        // "The new target must be a player" (Reflecting Mirror) — narrowed *after* the spell's own
        // requirement, so the redirect can never make an otherwise-illegal choice legal.
        if (effect.newTargetMustBePlayer) {
            legalNewTargets = legalNewTargets.filter { it in state.turnOrder }
        }

        if (legalNewTargets.isEmpty()) {
            // No other legal targets to redirect to
            return EffectResult.success(state)
        }

        // 4. Present selection decision to the controller
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val continuation = ChangeSpellTargetContinuation(
            spellEntityId = targetSpell.spellEntityId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences
        )

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose a new target",
            options = legalNewTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true,
            answer = continuation
        )

        return EffectResult.propagatePause(
            decisionResult.state,
            decisionResult.events
        )
    }

    /**
     * Change one of the spell's targets to the fixed object [fixed] names. CR 115.7a — a target
     * changes only to another legal target, judged by that target's own requirement from the
     * spell's controller's side; otherwise it stays as it was. A slot qualifies when [fixed] isn't
     * already its target, is legal for its requirement, and isn't already chosen for another slot
     * of the same instance of the word "target" (CR 115.3) — the change can't make the spell's
     * other targets illegal. With several qualifying targets, the controller chooses which one
     * changes (Spellskite's ruling); "with a single target" wordings restrict at targeting instead.
     */
    private fun redirectToFixedTarget(
        state: GameState,
        context: EffectContext,
        effect: ChangeTargetEffect,
        fixed: EffectTarget,
        targets: List<ChosenTarget>,
        targetRequirements: List<TargetRequirement>,
        spellController: EntityId,
        spellEntityId: EntityId
    ): EffectResult {
        val newTargetId = context.resolveTarget(fixed, state) ?: return EffectResult.success(state)
        if (effect.newTargetMustBePlayer && newTargetId !in state.turnOrder) return EffectResult.success(state)
        if (targets.isEmpty() || targetRequirements.isEmpty()) return EffectResult.success(state)

        val aligned = SpellCopyTargets.alignedRequirements(targetRequirements, targets.size)
        val groupOfSlot = aligned.flatMapIndexed { group, requirement -> List(requirement.count) { group } }
        val legalByGroup = HashMap<Int, Boolean>()
        val candidateSlots = targets.indices.filter { slot ->
            val group = groupOfSlot.getOrNull(slot) ?: return@filter false
            val currentId = getTargetEntityId(targets[slot])
            currentId != newTargetId &&
                (!effect.onlyIfCurrentTargetIsController || currentId == context.controllerId) &&
                targets.indices.none { other ->
                    other != slot && groupOfSlot.getOrNull(other) == group &&
                        getTargetEntityId(targets[other]) == newTargetId
                } &&
                legalByGroup.getOrPut(group) {
                    newTargetId in targetFinder.findLegalTargets(
                        state, targetRequirements[group], spellController, spellEntityId
                    )
                }
        }
        if (candidateSlots.isEmpty()) return EffectResult.success(state)

        val candidateIds = candidateSlots.mapNotNull { getTargetEntityId(targets[it]) }.distinct()
        if (candidateIds.size <= 1) {
            return EffectResult.success(
                replaceTargetSlot(state, spellEntityId, candidateSlots.first(), newTargetId)
            )
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose the target to change",
            options = candidateIds,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true,
            answer = ChangeSpellTargetContinuation(
                spellEntityId = spellEntityId,
                sourceId = context.sourceId,
                objectReferences = context.objectReferences,
                fixedNewTarget = newTargetId,
                candidateSlots = candidateSlots
            )
        )
        return EffectResult.propagatePause(decisionResult.state, decisionResult.events)
    }

    /**
     * Find all legal new targets for the spell/ability, excluding the current target.
     * Uses the spell's target requirements to determine what types of entities are valid.
     */
    private fun findLegalNewTargets(
        state: GameState,
        currentTarget: ChosenTarget,
        targetRequirements: List<TargetRequirement>,
        controllerId: EntityId,
        sourceId: EntityId
    ): List<EntityId> {
        val projected = state.projectedState
        val currentTargetId = getTargetEntityId(currentTarget)

        // If we have target requirements, use the first one to determine legal targets
        val requirement = targetRequirements.firstOrNull()

        return when {
            // AnyTarget: creatures/planeswalkers on battlefield + players
            requirement is AnyTarget -> {
                targetFinder.findLegalTargets(state, requirement, controllerId, sourceId).filter { it != currentTargetId }
            }

            // TargetCreatureOrPlayer: creatures on battlefield + players
            requirement is TargetCreatureOrPlayer -> {
                val creatures = state.getBattlefield().filter { entityId ->
                    projected.hasType(entityId, "CREATURE")
                }
                val players = state.turnOrder.filter { state.hasEntity(it) }
                (creatures + players).filter { it != currentTargetId }
            }

            // TargetPermanentOrPlayer: permanents matching the filter + players
            requirement is TargetPermanentOrPlayer -> {
                val predContext = PredicateContext(controllerId = controllerId)
                val permanents = state.getBattlefield().filter { entityId ->
                    predicateEvaluator.matches(
                        state, projected, entityId, requirement.permanentFilter.baseFilter, predContext
                    )
                }
                val players = state.turnOrder.filter {
                    state.hasEntity(it) && (!requirement.opponentsOnly || state.isOpponentOf(it, controllerId))
                }
                (permanents + players).filter { it != currentTargetId }
            }

            // TargetOpponentOrPlaneswalker: opponents + planeswalkers on battlefield
            requirement is TargetOpponentOrPlaneswalker -> {
                val opponents = state.turnOrder.filter { it != controllerId && state.hasEntity(it) }
                val planeswalkers = state.getBattlefield().filter { entityId ->
                    projected.hasType(entityId, "PLANESWALKER")
                }
                (opponents + planeswalkers).filter { it != currentTargetId }
            }

            // TargetCreatureOrPlaneswalker: creatures and planeswalkers on battlefield
            requirement is TargetCreatureOrPlaneswalker -> {
                state.getBattlefield().filter { entityId ->
                    entityId != currentTargetId &&
                        (projected.hasType(entityId, "CREATURE") || projected.hasType(entityId, "PLANESWALKER"))
                }
            }

            // TargetObject with battlefield zone: use filter to match permanents
            requirement is TargetObject && requirement.filter.zone == Zone.BATTLEFIELD -> {
                val predContext = PredicateContext(controllerId = controllerId)
                state.getBattlefield().filter { entityId ->
                    entityId != currentTargetId &&
                        predicateEvaluator.matches(state, state.projectedState, entityId, requirement.filter.baseFilter, predContext)
                }
            }

            // Fallback: infer from current target type
            else -> findTargetsByCurrentType(state, currentTarget, projected)
        }
    }

    /**
     * Fallback: find targets of the same type as the current target.
     */
    private fun findTargetsByCurrentType(
        state: GameState,
        currentTarget: ChosenTarget,
        projected: ProjectedState
    ): List<EntityId> {
        val currentTargetId = getTargetEntityId(currentTarget)
        return when (currentTarget) {
            is ChosenTarget.Permanent -> {
                state.getBattlefield().filter { it != currentTargetId }
            }
            is ChosenTarget.Player -> {
                state.turnOrder.filter { it != currentTargetId && state.hasEntity(it) }
            }
            else -> emptyList()
        }
    }

    private fun getTargetEntityId(target: ChosenTarget): EntityId? {
        return when (target) {
            is ChosenTarget.Permanent -> target.entityId
            is ChosenTarget.Player -> target.playerId
            is ChosenTarget.Card -> target.cardId
            is ChosenTarget.Spell -> target.spellEntityId
        }
    }

    companion object {
        /** Rewrite one target slot of [spellEntityId] to [newTargetId], keeping the slot's target shape. */
        fun replaceTargetSlot(state: GameState, spellEntityId: EntityId, slot: Int, newTargetId: EntityId): GameState {
            val component = state.getEntity(spellEntityId)?.get<TargetsComponent>() ?: return state
            val targets = component.targets.toMutableList()
            if (slot !in targets.indices) return state
            targets[slot] = ContestedRetargetLogic.rebuildTarget(state, newTargetId, targets[slot])
            return state.updateEntity(spellEntityId) { container ->
                container.with(TargetsComponent.capture(state, targets, component.targetRequirements))
            }
        }
    }
}
