package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * Target legality belongs to the prospective copy, not the effect making it or the original.
 * The scratch entity is never put on the stack or returned to gameplay: new targets are chosen
 * before placement (CR 707.10c). In particular, the original keeps its own colors and remains a
 * possible target; a red copy can target something protected from the original's blue color.
 */
internal object SpellCopyTargets {
    fun legalTargets(
        state: GameState,
        finder: TargetFinder,
        sourceId: EntityId,
        controllerId: EntityId,
        requirements: List<TargetRequirement>,
        exceptions: CopyExceptions,
    ): Map<Int, List<EntityId>> {
        val source = state.getEntity(sourceId)
        val card = source?.get<CardComponent>() ?: return emptyMap()
        val spell = source.get<SpellOnStackComponent>() ?: return emptyMap()
        val (copyId, scratch) = state.newEntity()
        val copiedCard = CopyExceptionApplier.apply(card, exceptions).copy(ownerId = controllerId)
        val preview = scratch.withEntity(copyId, ComponentContainer.of(
            copiedCard,
            spell.copy(casterId = controllerId),
        ))
        val sourceSubtypes = copiedCard.typeLine.subtypes.mapTo(mutableSetOf()) { it.value }
        return requirements.mapIndexed { index, requirement ->
            index to finder.findLegalTargets(
                preview, requirement, controllerId, copyId, targetingSourceType = TargetingSourceType.SPELL
            ).filter { candidate ->
                finder.validator.validateSingleTarget(
                    state = preview,
                    target = com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget(preview, candidate),
                    requirement = requirement,
                    casterId = controllerId,
                    sourceColors = copiedCard.colors,
                    sourceSubtypes = sourceSubtypes,
                    sourceId = copyId,
                    xValue = spell.xValue,
                    targetingSourceType = TargetingSourceType.SPELL,
                ) == null
            }
        }.toMap()
    }
}
