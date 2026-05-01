package com.wingedsheep.engine.state.components.stack

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
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
    val wasBlightPaid: Boolean = false,  // For BlightOrPay additional cost — true if blight path was taken
    val chosenModes: List<Int> = emptyList(),  // For modal spells (700.2). Ordered; same index may repeat when allowRepeat.
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),  // Per-mode chosen targets, aligned 1:1 with chosenModes
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),  // Per-mode TargetRequirements for 608.2b re-validation at resolution
    val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),  // Per-mode DividedDamageEffect allocations (future)
    /** Snapshots of permanents sacrificed as additional cost (Rule 112.7a — last known info). */
    val sacrificedPermanents: List<PermanentSnapshot> = emptyList(),
    val castFaceDown: Boolean = false,  // For morph - creature enters face-down
    val damageDistribution: Map<EntityId, Int>? = null,  // For DividedDamageEffect - pre-chosen damage allocation
    val chosenCreatureType: String? = null,  // For spells that choose a creature type during casting (e.g., Aphetto Dredging)
    val exiledCardCount: Int = 0,  // For variable exile additional costs (e.g., Chill Haunting)
    val castFromZone: Zone? = null,  // Zone the spell was cast from (e.g., HAND for normal casting)
    val wasWarped: Boolean = false,  // For warp - permanent is exiled at end step
    val wasEvoked: Boolean = false,  // For evoke - permanent is sacrificed on ETB
    val beheldCards: List<EntityId> = emptyList(),  // Cards chosen via Behold (stored in pipeline as named collection)
    val manaSpentWhite: Int = 0,  // Mana colors spent for mana-spent-gated triggers
    val manaSpentBlue: Int = 0,
    val manaSpentBlack: Int = 0,
    val manaSpentRed: Int = 0,
    val manaSpentGreen: Int = 0,
    val manaSpentColorless: Int = 0
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
    val description: String,
    /** Optional human-readable description from `TriggeredAbility.descriptionOverride`,
     *  used when displaying the ability on the stack instead of the auto-generated effect text. */
    val descriptionOverride: String? = null,
    val triggerDamageAmount: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val xValue: Int? = null,
    val triggerCounterCount: Int? = null,
    val triggerTotalCounterCount: Int? = null,
    val targetingSourceEntityId: EntityId? = null,  // The spell/ability that targeted this permanent (for ward)
    val damageDistribution: Map<EntityId, Int>? = null,  // For DividedDamageEffect - pre-chosen damage allocation
    val copyIndex: Int? = null,    // Which copy number this is (1, 2, 3...) for storm/copy effects
    val copyTotal: Int? = null,    // Total number of copies being created
    val lastKnownPower: Int? = null,    // Power at the moment the triggering entity left the battlefield (dies/leaves)
    val lastKnownToughness: Int? = null, // Toughness at the moment the triggering entity left the battlefield (dies/leaves)
    // Modal fields — populated when this triggered ability is a copy of a modal spell (700.2g).
    // Copies inherit the original's chosen modes; targets either inherit too (StormCopy default)
    // or are re-chosen by the copy controller while modes stay fixed.
    val chosenModes: List<Int> = emptyList(),
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
    val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap()
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
    val effect: Effect,
    /** Snapshots of permanents sacrificed as additional cost (Rule 112.7a — last known info). */
    val sacrificedPermanents: List<PermanentSnapshot> = emptyList(),
    val xValue: Int? = null,
    val tappedPermanents: List<EntityId> = emptyList(),
    /** Optional human-readable description from `ActivatedAbility.descriptionOverride`,
     *  used when displaying the ability on the stack instead of the auto-generated effect text. */
    val descriptionOverride: String? = null
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
 *
 * @property targets The chosen targets
 * @property targetRequirements The original target requirements, used for re-validation
 *           on resolution (Rule 608.2b — targets must still be legal when the spell/ability resolves)
 */
@Serializable
data class TargetsComponent(
    val targets: List<ChosenTarget>,
    val targetRequirements: List<TargetRequirement> = emptyList()
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
        val zone: Zone
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

/**
 * Marks a spell or ability on the stack as having been granted extra keywords (by enum name)
 * while it remains on the stack. Used by effects like Spinerock Tyrant that grant wither to
 * a copied or original spell. The component disappears with the spell entity when it leaves
 * the stack.
 */
@Serializable
data class SpellGrantedKeywordsComponent(
    val keywords: Set<String> = emptySet()
) : Component
