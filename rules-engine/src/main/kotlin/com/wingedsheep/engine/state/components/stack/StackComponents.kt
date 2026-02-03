package com.wingedsheep.engine.state.components.stack

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.Effect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Marks an entity as a spell on the stack.
 */
@Serializable
data class SpellOnStackComponent(
    val casterId: EntityId,
    val xValue: Int? = null,  // For X spells
    val wasKicked: Boolean = false,  // For kicker costs
    val chosenModes: List<Int> = emptyList(),  // For modal spells
    val sacrificedPermanents: List<EntityId> = emptyList(),  // For additional costs
    val castFaceDown: Boolean = false,  // For morph - creature enters face-down
    val damageDistribution: Map<EntityId, Int>? = null  // For DividedDamageEffect - pre-chosen damage allocation
) : Component

/**
 * Marks an entity as a triggered ability on the stack.
 */
@Serializable
data class TriggeredAbilityOnStackComponent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect,
    val description: String
) : Component {
    val hasTargets: Boolean = false  // Will be updated based on effect
}

/**
 * Marks an entity as an activated ability on the stack.
 */
@Serializable
data class ActivatedAbilityOnStackComponent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect
) : Component {
    val hasTargets: Boolean = false  // Will be updated based on effect
}

/**
 * Legacy ability on stack component (for backwards compatibility).
 */
@Serializable
data class AbilityOnStackComponent(
    val sourceId: EntityId,
    val controllerId: EntityId,
    val abilityId: AbilityId,
    val effect: Effect
) : Component

/**
 * Targets chosen for a spell or ability.
 */
@Serializable
data class TargetsComponent(
    val targets: List<ChosenTarget>
) : Component

/**
 * Represents a chosen target for a spell or ability.
 */
@Serializable
sealed interface ChosenTarget {
    @Serializable
    @SerialName("Player")
    data class Player(val playerId: EntityId) : ChosenTarget

    @Serializable
    @SerialName("Permanent")
    data class Permanent(val entityId: EntityId) : ChosenTarget

    @Serializable
    @SerialName("Card")
    data class Card(
        val cardId: EntityId,
        val ownerId: EntityId,
        val zone: ZoneType
    ) : ChosenTarget

    @Serializable
    @SerialName("Spell")
    data class Spell(val spellEntityId: EntityId) : ChosenTarget
}

/**
 * Additional context for spell resolution.
 */
@Serializable
data class SpellContextComponent(
    val additionalData: Map<String, String> = emptyMap()
) : Component
