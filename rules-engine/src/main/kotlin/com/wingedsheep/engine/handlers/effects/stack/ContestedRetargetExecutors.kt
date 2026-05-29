package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ContestedRetargetContinuation
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChangeTriggeringObjectTargetsEffect
import com.wingedsheep.sdk.scripting.effects.RetargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlin.reflect.KClass

/**
 * Executor for [ChangeTriggeringObjectTargetsEffect] — the chosen player may change the triggering
 * spell/ability's targets. Delegates the interactive slot-by-slot retargeting to
 * [ContestedRetargetLogic] (shared with the resumer).
 */
class ChangeTriggeringObjectTargetsExecutor :
    EffectExecutor<ChangeTriggeringObjectTargetsEffect> {

    override val effectType: KClass<ChangeTriggeringObjectTargetsEffect> =
        ChangeTriggeringObjectTargetsEffect::class

    override fun execute(
        state: GameState,
        effect: ChangeTriggeringObjectTargetsEffect,
        context: EffectContext
    ): EffectResult {
        val stackObjectId = context.triggeringEntityId ?: return EffectResult.success(state)
        if (!state.stack.contains(stackObjectId)) return EffectResult.success(state)
        val targetsComponent = state.getEntity(stackObjectId)?.get<TargetsComponent>()
            ?: return EffectResult.success(state)
        if (targetsComponent.targets.isEmpty()) return EffectResult.success(state)

        val chooserId = when (val chooser = effect.chooser) {
            is RetargetChooser.Controller -> context.controllerId
            is RetargetChooser.OwnerOfStored -> {
                // The owner of the single stored card; if there isn't exactly one (a tie left
                // several, or none were stored), there is no chooser and we skip the retarget.
                val stored = context.pipeline.storedCollections[chooser.collectionName].orEmpty()
                stored.singleOrNull()?.let { state.getEntity(it)?.get<CardComponent>()?.ownerId }
            }
        } ?: return EffectResult.success(state)

        return ContestedRetargetLogic.start(state, stackObjectId, chooserId, context.sourceId)
    }
}

/**
 * Shared logic for a chosen player's optional retargeting of the triggering spell/ability, used by
 * [ChangeTriggeringObjectTargetsExecutor] (which sets up the first decision) and the continuation
 * resumer (which drives subsequent slot decisions).
 *
 * Targets are reselected one slot at a time. For each slot the chooser is offered the original
 * spell/ability's legal targets (legality is judged from the spell's controller's perspective,
 * never the chooser's), including the current target so they may keep it. A slot with no
 * alternative target is kept automatically without a prompt; if no slot has an alternative, the
 * targets are left unchanged. A target may not be chosen for two slots (CR).
 */
object ContestedRetargetLogic {

    private val targetFinder = TargetFinder()
    private val decisionHandler = DecisionHandler()

    fun start(
        state: GameState,
        stackObjectId: EntityId,
        chooserId: EntityId,
        sourceId: EntityId?
    ): EffectResult {
        val targetsComponent = state.getEntity(stackObjectId)?.get<TargetsComponent>()
            ?: return EffectResult.success(state)
        val ownerControllerId = controllerOfStackObject(state, stackObjectId)
            ?: return EffectResult.success(state)
        val perSlot = expandRequirements(targetsComponent.targetRequirements, targetsComponent.targets)

        return advance(
            state = state,
            stackObjectId = stackObjectId,
            chooserId = chooserId,
            ownerControllerId = ownerControllerId,
            perSlotRequirements = perSlot,
            originalTargets = targetsComponent.targets,
            newTargets = emptyList(),
            startSlot = 0,
            sourceId = sourceId
        )
    }

    /**
     * Resolve slots from [startSlot] onward, auto-keeping any slot without an alternative and
     * pausing for a decision on the first slot that has one. When all slots are resolved, writes
     * the new targets onto the triggering object. Returns an [EffectResult] (paused or success).
     */
    fun advance(
        state: GameState,
        stackObjectId: EntityId,
        chooserId: EntityId,
        ownerControllerId: EntityId,
        perSlotRequirements: List<TargetRequirement>,
        originalTargets: List<ChosenTarget>,
        newTargets: List<ChosenTarget>,
        startSlot: Int,
        sourceId: EntityId?
    ): EffectResult {
        var acc = newTargets
        var slot = startSlot
        while (slot < originalTargets.size) {
            val current = originalTargets[slot]
            val requirement = perSlotRequirements.getOrNull(slot)
            val options = legalOptions(state, requirement, ownerControllerId, stackObjectId, current, acc)

            if (options.size <= 1) {
                // No alternative target for this slot — keep the current one.
                acc = acc + current
                slot++
                continue
            }

            val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = state,
                playerId = chooserId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = if (originalTargets.size == 1) {
                    "Choose this spell or ability's target (or keep the current one)"
                } else {
                    "Choose target ${slot + 1} of ${originalTargets.size} (or keep the current one)"
                },
                options = options,
                minSelections = 1,
                maxSelections = 1,
                useTargetingUI = true
            )

            val continuation = ContestedRetargetContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                stackObjectId = stackObjectId,
                chooserId = chooserId,
                ownerControllerId = ownerControllerId,
                perSlotRequirements = perSlotRequirements,
                originalTargets = originalTargets,
                newTargets = acc,
                currentSlot = slot,
                sourceId = sourceId
            )

            return EffectResult.paused(
                decisionResult.state.pushContinuation(continuation),
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }

        // All slots resolved — write the (possibly unchanged) targets back onto the object.
        val requirements = state.getEntity(stackObjectId)?.get<TargetsComponent>()?.targetRequirements
            ?: emptyList()
        val updatedState = state.updateEntity(stackObjectId) { container ->
            container.with(TargetsComponent(acc, requirements))
        }
        return EffectResult.success(updatedState)
    }

    /**
     * Legal target options for a slot: the original spell/ability's legal targets (from its
     * controller's perspective), minus targets already chosen for other slots, plus the current
     * target (always a legal "keep" option).
     */
    private fun legalOptions(
        state: GameState,
        requirement: TargetRequirement?,
        ownerControllerId: EntityId,
        stackObjectId: EntityId,
        current: ChosenTarget,
        alreadyChosen: List<ChosenTarget>
    ): List<EntityId> {
        val currentId = entityIdOf(current)
        if (requirement == null) return listOfNotNull(currentId)

        val legal = targetFinder.findLegalTargets(
            state = state,
            requirement = requirement,
            controllerId = ownerControllerId,
            sourceId = stackObjectId
        )
        val excluded = alreadyChosen.mapNotNull { entityIdOf(it) }.toSet() - setOfNotNull(currentId)
        val filtered = legal.filter { it !in excluded }
        return if (currentId != null && currentId !in filtered) filtered + currentId else filtered
    }

    private fun controllerOfStackObject(state: GameState, id: EntityId): EntityId? {
        val container = state.getEntity(id) ?: return null
        container.get<SpellOnStackComponent>()?.let { return it.casterId }
        container.get<ActivatedAbilityOnStackComponent>()?.let { return it.controllerId }
        container.get<TriggeredAbilityOnStackComponent>()?.let { return it.controllerId }
        return null
    }

    /**
     * Expand the spell/ability's target requirements into one requirement per target slot, so a
     * multi-target requirement (count > 1) maps each of its targets to itself.
     */
    private fun expandRequirements(
        requirements: List<TargetRequirement>,
        targets: List<ChosenTarget>
    ): List<TargetRequirement> {
        if (requirements.isEmpty()) return emptyList()
        val out = ArrayList<TargetRequirement>(targets.size)
        for (requirement in requirements) {
            val n = if (requirement.unlimited) targets.size else requirement.count.coerceAtLeast(1)
            repeat(n) { if (out.size < targets.size) out.add(requirement) }
            if (out.size >= targets.size) break
        }
        while (out.size < targets.size) out.add(requirements.last())
        return out
    }

    fun entityIdOf(target: ChosenTarget): EntityId? = when (target) {
        is ChosenTarget.Permanent -> target.entityId
        is ChosenTarget.Player -> target.playerId
        is ChosenTarget.Card -> target.cardId
        is ChosenTarget.Spell -> target.spellEntityId
    }

    /**
     * Rebuild a [ChosenTarget] for a newly selected entity, preserving the slot's target shape:
     * a player id becomes [ChosenTarget.Player]; otherwise mirror the prior target's kind.
     */
    fun rebuildTarget(state: GameState, selectedId: EntityId, template: ChosenTarget): ChosenTarget = when {
        state.turnOrder.contains(selectedId) -> ChosenTarget.Player(selectedId)
        template is ChosenTarget.Spell -> ChosenTarget.Spell(selectedId)
        template is ChosenTarget.Card -> ChosenTarget.Card(
            cardId = selectedId,
            ownerId = state.getEntity(selectedId)?.get<CardComponent>()?.ownerId ?: template.ownerId,
            zone = template.zone
        )
        else -> ChosenTarget.Permanent(selectedId)
    }
}
