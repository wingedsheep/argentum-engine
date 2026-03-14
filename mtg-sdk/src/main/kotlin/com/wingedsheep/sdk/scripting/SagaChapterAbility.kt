package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Represents a single chapter ability of a Saga enchantment.
 *
 * Each chapter triggers when the number of lore counters on the Saga
 * reaches or exceeds the chapter number. Chapter abilities use the stack
 * and can be responded to.
 *
 * @param chapter The chapter number (1, 2, 3, etc.)
 * @param effect The effect that happens when this chapter triggers
 * @param targetRequirement Optional targeting requirement for this chapter
 * @param additionalTargetRequirements Additional target requirements for multi-target chapters
 */
@Serializable
data class SagaChapterAbility(
    val chapter: Int,
    val effect: Effect,
    val targetRequirement: TargetRequirement? = null,
    val additionalTargetRequirements: List<TargetRequirement> = emptyList()
)
