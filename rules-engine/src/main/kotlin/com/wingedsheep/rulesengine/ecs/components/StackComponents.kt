package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.ability.Effect
import com.wingedsheep.rulesengine.ability.TriggeredAbility
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.event.TriggerContext
import kotlinx.serialization.Serializable

/**
 * Components for tracking items on the stack.
 *
 * In MTG, the stack contains:
 * - Spells (cards being cast)
 * - Activated abilities
 * - Triggered abilities
 *
 * Each item on the stack needs to track its targets, controller,
 * and any other relevant context needed for resolution.
 */

// =============================================================================
// Spell on Stack
// =============================================================================

/**
 * Marks an entity as a spell on the stack.
 *
 * When a card is cast, it moves to the stack and gets this component
 * to track the casting context. When it resolves:
 * - Permanents move to the battlefield
 * - Non-permanents (instants/sorceries) execute their effects and go to graveyard
 *
 * @property casterId The player who cast this spell
 * @property targets The chosen targets for this spell
 * @property xValue The value of X if this spell has X in its cost (null if not applicable)
 * @property wasKicked Whether the kicker cost was paid (for kicker spells)
 * @property chosenModes The modes chosen for modal spells
 */
@Serializable
data class SpellOnStackComponent(
    val casterId: EntityId,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val wasKicked: Boolean = false,
    val chosenModes: List<Int> = emptyList()
) : Component {
    /**
     * Check if this spell has any targets.
     */
    val hasTargets: Boolean get() = targets.isNotEmpty()

    /**
     * Get all target entity IDs.
     */
    val targetEntityIds: List<EntityId>
        get() = targets.mapNotNull { target ->
            when (target) {
                is ChosenTarget.Player -> target.playerId
                is ChosenTarget.Permanent -> target.entityId
                is ChosenTarget.Card -> target.cardId
                is ChosenTarget.Spell -> target.spellEntityId
            }
        }

    /**
     * Add a target to the spell.
     */
    fun withTarget(target: ChosenTarget): SpellOnStackComponent =
        copy(targets = targets + target)

    /**
     * Set the X value.
     */
    fun withX(x: Int): SpellOnStackComponent = copy(xValue = x)

    /**
     * Mark as kicked.
     */
    fun withKicker(): SpellOnStackComponent = copy(wasKicked = true)

    /**
     * Set the chosen modes.
     */
    fun withModes(modes: List<Int>): SpellOnStackComponent = copy(chosenModes = modes)
}

// =============================================================================
// Triggered Ability on Stack
// =============================================================================

/**
 * Marks an entity as a triggered ability on the stack.
 *
 * When a triggered ability triggers, a new entity is created on the stack
 * with this component to track the trigger context.
 *
 * @property ability The triggered ability definition
 * @property sourceId The permanent that has this ability
 * @property sourceName The name of the source (for display)
 * @property controllerId The player who controls the ability
 * @property triggerContext Context from when the ability triggered
 * @property targets The chosen targets for this ability
 */
@Serializable
data class TriggeredAbilityOnStackComponent(
    val ability: TriggeredAbility,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val triggerContext: TriggerContext,
    val targets: List<ChosenTarget> = emptyList()
) : Component {
    val description: String
        get() = "$sourceName: ${ability.description}"

    val hasTargets: Boolean get() = targets.isNotEmpty()

    val targetEntityIds: List<EntityId>
        get() = targets.mapNotNull { target ->
            when (target) {
                is ChosenTarget.Player -> target.playerId
                is ChosenTarget.Permanent -> target.entityId
                is ChosenTarget.Card -> target.cardId
                is ChosenTarget.Spell -> target.spellEntityId
            }
        }

    fun withTarget(target: ChosenTarget): TriggeredAbilityOnStackComponent =
        copy(targets = targets + target)
}

// =============================================================================
// Activated Ability on Stack
// =============================================================================

/**
 * Marks an entity as an activated ability on the stack.
 *
 * Activated abilities are abilities that a player actively chooses to use,
 * typically with a cost and effect (e.g., "{T}: Add {G}").
 *
 * @property effect The effect(s) of this ability
 * @property sourceId The permanent that has this ability
 * @property sourceName The name of the source
 * @property controllerId The player who activated this ability
 * @property targets The chosen targets
 */
@Serializable
data class ActivatedAbilityOnStackComponent(
    val effect: Effect,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val targets: List<ChosenTarget> = emptyList()
) : Component {
    val hasTargets: Boolean get() = targets.isNotEmpty()

    val targetEntityIds: List<EntityId>
        get() = targets.mapNotNull { target ->
            when (target) {
                is ChosenTarget.Player -> target.playerId
                is ChosenTarget.Permanent -> target.entityId
                is ChosenTarget.Card -> target.cardId
                is ChosenTarget.Spell -> target.spellEntityId
            }
        }

    fun withTarget(target: ChosenTarget): ActivatedAbilityOnStackComponent =
        copy(targets = targets + target)
}
