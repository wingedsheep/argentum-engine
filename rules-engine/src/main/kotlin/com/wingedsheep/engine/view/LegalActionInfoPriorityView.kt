package com.wingedsheep.engine.view

import com.wingedsheep.engine.legalactions.PriorityAction
import com.wingedsheep.sdk.model.EntityId

/**
 * Adapts the client-facing [LegalActionInfo] to [PriorityAction], the shape
 * [com.wingedsheep.engine.legalactions.MeaningfulActionFilter] reads.
 *
 * A wrapper rather than `LegalActionInfo : PriorityAction`: the DTO is a wire contract serialized
 * straight to the client, and two of the interface's members (`isAffordableAction`,
 * `additionalCostType`) would have to be added as derived properties, which Jackson would then
 * publish as new JSON fields. This costs one small object per legal action, once per priority
 * window, on the server path only.
 */
fun LegalActionInfo.asPriorityAction(): PriorityAction = LegalActionInfoPriorityView(this)

private class LegalActionInfoPriorityView(private val info: LegalActionInfo) : PriorityAction {
    override val actionType: String get() = info.actionType
    override val requiresTargets: Boolean get() = info.requiresTargets
    override val validTargets: List<EntityId>? get() = info.validTargets
    override val validAttackers: List<EntityId>? get() = info.validAttackers
    override val validBlockers: List<EntityId>? get() = info.validBlockers
    override val isManaAbility: Boolean get() = info.isManaAbility
    override val holdPriority: Boolean get() = info.holdPriority
    override val isAffordableAction: Boolean get() = info.isAffordable
    override val additionalCostType: String? get() = info.additionalCostInfo?.costType
    override val hasUnfillableTargetRequirement: Boolean
        get() = info.targetRequirements?.any { it.minTargets > 0 && it.validTargets.isEmpty() } ?: false
}
