package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.mechanics.targeting.withTargetConstraints

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.modal.ChosenModeMemory
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.engine.mechanics.targeting.DynamicTargetCount
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/** Mode and target announcement for top-level activated modals, before any costs are paid. */
internal class ActivationModalChoices(
    private val targetFinder: TargetFinder,
    private val conditions: ConditionEvaluator,
    private val complete: ((GameState, ActivateAbility) -> ExecutionResult)? = null
) {
    fun effective(state: GameState, action: ActivateAbility, modal: ModalEffect): ModalEffect {
        val text = TextChanges.of(state, action.sourceId)
        val changed = text?.let { modal.applyTextReplacement(it) as ModalEffect } ?: modal
        val ctx = EffectContext(sourceId = action.sourceId, controllerId = action.playerId, xValue = action.xValue)
        val max = changed.dynamicChooseCount?.let { conditions.amounts.evaluate(state, it, ctx) }
            ?: changed.chooseCount
        val cap = if (changed.allowRepeat) max.coerceAtLeast(0) else max.coerceIn(0, changed.modes.size)
        val min = changed.dynamicMinChooseCount?.let { conditions.amounts.evaluate(state, it, ctx) }
            ?: changed.minChooseCount
        return changed.copy(chooseCount = cap, minChooseCount = min.coerceIn(0, cap))
    }

    private fun counts(state: GameState, action: ActivateAbility, req: TargetRequirement): DynamicTargetCount.Bounds {
        val ctx = EffectContext(sourceId = action.sourceId, controllerId = action.playerId, xValue = action.xValue)
        val distributed = if (DynamicTargetCount.objectOf(req)?.ownerDistribution != null) {
            com.wingedsheep.engine.mechanics.targeting.OwnerTargetDistribution.bounds(state, req, legal(state, action, req))
        } else null
        val bounds = distributed ?: DynamicTargetCount.bounds(req, action.xValue) { conditions.amounts.evaluate(state, it, ctx) }
        return bounds.copy(max = bounds.max ?: if (req.unlimited) Int.MAX_VALUE else req.count)
    }

    private fun legal(state: GameState, action: ActivateAbility, req: TargetRequirement) =
        targetFinder.findLegalTargets(state, req, action.playerId, action.sourceId,
            targetingSourceType = TargetingSourceType.ACTIVATED_ABILITY,
            pipelineContext = PredicateContext(controllerId = action.playerId, sourceId = action.sourceId, xValue = action.xValue))

    fun sourceColors(state: GameState, action: ActivateAbility): Set<com.wingedsheep.sdk.core.Color> =
        if (action.sourceId in state.getBattlefield()) state.projectedState.getColors(action.sourceId)
            .map { com.wingedsheep.sdk.core.Color.valueOf(it) }.toSet()
        else state.getEntity(action.sourceId)?.get<CardComponent>()?.colors.orEmpty()

    fun sourceSubtypes(state: GameState, action: ActivateAbility): Set<String> =
        if (action.sourceId in state.getBattlefield()) state.projectedState.getSubtypes(action.sourceId)
        else state.getEntity(action.sourceId)?.get<CardComponent>()?.typeLine?.subtypes?.map { it.value }?.toSet().orEmpty()

    fun boundModeCounts(state: GameState, action: ActivateAbility, modal: ModalEffect): List<List<Int>> =
        action.chosenModes.orEmpty().mapIndexed { ordinal, index ->
            targetFinder.validator.bindTargetGroups(state, action.modeTargetsOrdered[ordinal],
                effective(state, action, modal).modes[index].targetRequirements, action.playerId,
                sourceColors = sourceColors(state, action), sourceSubtypes = sourceSubtypes(state, action),
                sourceId = action.sourceId, xValue = action.xValue,
                targetingSourceType = TargetingSourceType.ACTIVATED_ABILITY,
                explicitCounts = action.modeTargetGroupCountsOrdered.getOrNull(ordinal)).counts
        }

    fun available(state: GameState, action: ActivateAbility, modal: ModalEffect): List<Int> {
        val excluded = ChosenModeMemory.excludedFor(state, action.sourceId, modal)
        return modal.modes.indices.filter { index ->
            index !in excluded && modal.modes[index].targetRequirements.all { req ->
                legal(state, action, req).size >= counts(state, action, req).min
            }
        }
    }

    fun validate(state: GameState, action: ActivateAbility, effect: Effect): String? {
        val raw = (effect as? ModalEffect)
        if (raw == null) return if (action.chosenModes != null || action.modeTargetsOrdered.isNotEmpty())
            "This ability is not modal" else null
        if (action.repeatCount > 1) return "Modal activations must be announced individually"
        val modal = effective(state, action, raw)
        val available = available(state, action, modal)
        if (modal.minChooseCount > 0 && (available.isEmpty() || !modal.allowRepeat && available.size < modal.minChooseCount))
            return "No legal mode selection available"
        val chosen = action.chosenModes ?: return if (action.modeTargetsOrdered.isNotEmpty() || action.modeTargetGroupCountsOrdered.isNotEmpty())
            "Mode bindings require chosen modes" else null
        if (action.modeTargetGroupCountsOrdered.isNotEmpty() && action.modeTargetGroupCountsOrdered.size != chosen.size)
            return "Mode target groups must match chosen modes"
        if (chosen.size !in modal.minChooseCount..modal.chooseCount) return "Invalid number of modes"
        if (!modal.allowRepeat && chosen.distinct().size != chosen.size) return "A mode cannot be chosen twice"
        if (chosen.any { it !in available }) return "An unavailable mode was chosen"
        if (action.modeTargetsOrdered.size != chosen.size) return "Mode target bindings must match chosen modes"
        for ((ordinal, index) in chosen.withIndex()) {
            val reqs = modal.modes[index].targetRequirements
            val targets = action.modeTargetsOrdered[ordinal]
            if (reqs.isEmpty()) {
                if (targets.isNotEmpty() || action.modeTargetGroupCountsOrdered.getOrNull(ordinal).orEmpty().isNotEmpty())
                    return "A nontargeting mode cannot have target bindings"
            } else {
                targetFinder.validator.validateTargets(state, targets, reqs, action.playerId,
                    sourceColors = sourceColors(state, action), sourceId = action.sourceId,
                    sourceSubtypes = sourceSubtypes(state, action),
                    xValue = action.xValue, targetingSourceType = TargetingSourceType.ACTIVATED_ABILITY,
                    groupCounts = action.modeTargetGroupCountsOrdered.getOrNull(ordinal))?.let { return it }
            }
        }
        val modeTargets = action.modeTargetsOrdered.flatten()
        if (modeTargets.isNotEmpty() && action.targets.takeLast(modeTargets.size) != modeTargets)
            return "Flat targets must include the mode target bindings"
        return null
    }

    fun presentModes(state: GameState, action: ActivateAbility, modal: ModalEffect,
                     selected: List<Int> = emptyList()): ExecutionResult {
        if (selected.size == modal.chooseCount) return presentTargets(state, action, modal, selected)
        val offered = available(state, action, modal).filter { modal.allowRepeat || it !in selected }
        val done = selected.size >= modal.minChooseCount
        if (offered.isEmpty() && !done) return ExecutionResult.error(state, "No legal mode selection available")
        val name = state.getEntity(action.sourceId)?.get<CardComponent>()?.name
        return state.withPriority(action.playerId).suspendForDecision(
            question = { id -> ChooseOptionDecision(id = id, playerId = action.playerId,
                prompt = "Choose a mode for $name (${selected.size + 1} of ${modal.chooseCount})",
                context = DecisionContext(sourceId = action.sourceId, sourceName = name, phase = DecisionPhase.CASTING),
                options = offered.map { modal.modes[it].description } + if (done) listOf("Done") else emptyList(),
                canCancel = true) },
            answer = ActivationModalModeContinuation(action, modal, offered, selected, done)
        )
    }

    fun presentTargets(state: GameState, action: ActivateAbility, modal: ModalEffect, selected: List<Int>,
                       resolved: List<List<ChosenTarget>> = emptyList(), startOrdinal: Int = 0): ExecutionResult {
        var ordinal = startOrdinal
        var targets = resolved
        val groups = action.modeTargetGroupCountsOrdered.toMutableList()
        while (ordinal < selected.size) {
            val mode = modal.modes[selected[ordinal]]
            val reqs = mode.targetRequirements
            if (reqs.isEmpty()) {
                targets = targets + listOf(emptyList())
                while (groups.size <= ordinal) groups.add(emptyList())
                ordinal++; continue
            }
            val legal = reqs.mapIndexed { index, req -> index to legal(state, action, req) }.toMap()
            val infos = reqs.mapIndexed { index, req ->
                val bounds = counts(state, action, req)
                TargetRequirementInfo(index = index, description = req.description,
                    minTargets = bounds.min, maxTargets = bounds.max!!.coerceAtMost(maxOf(legal.getValue(index).size, bounds.min)))
                    .withTargetConstraints(state, req, legal.getValue(index))
            }
            if (infos.any { it.minTargets > it.maxTargets }) return ExecutionResult.error(state, "No compatible targets for mode")
            val name = state.getEntity(action.sourceId)?.get<CardComponent>()?.name
            return state.withPriority(action.playerId).suspendForDecision(
                question = { id -> ChooseTargetsDecision(id = id, playerId = action.playerId,
                    prompt = "Choose targets for $name — ${mode.description}",
                    context = DecisionContext(sourceId = action.sourceId, sourceName = name,
                        phase = DecisionPhase.CASTING, effectHint = mode.description),
                    targetRequirements = infos, legalTargets = legal, canCancel = true) },
                answer = ActivationModalTargetContinuation(action.copy(modeTargetGroupCountsOrdered = groups), modal, selected, targets, ordinal)
            )
        }
        val finalAction = action.copy(chosenModes = selected, modeTargetsOrdered = targets,
            modeTargetGroupCountsOrdered = groups, targets = action.targets + targets.flatten())
        return complete?.invoke(state, finalAction) ?: ExecutionResult.error(state, "No activation handler")
    }

}
