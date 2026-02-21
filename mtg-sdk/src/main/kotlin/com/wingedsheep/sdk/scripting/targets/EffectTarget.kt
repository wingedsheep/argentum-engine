package com.wingedsheep.sdk.scripting.targets

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Defines who/what an effect targets.
 */
@Serializable
sealed interface EffectTarget {
    val description: String

    /** The controller of the source ability */
    @SerialName("Controller")
    @Serializable
    data object Controller : EffectTarget {
        override val description: String = "you"
    }

    /** The source permanent itself */
    @SerialName("Self")
    @Serializable
    data object Self : EffectTarget {
        override val description: String = "this creature"
    }

    /** The creature enchanted by this aura */
    @SerialName("EnchantedCreature")
    @Serializable
    data object EnchantedCreature : EffectTarget {
        override val description: String = "enchanted creature"
    }

    /** The controller of the target (used for effects like "its controller gains 4 life") */
    @SerialName("TargetController")
    @Serializable
    data object TargetController : EffectTarget {
        override val description: String = "its controller"
    }

    /**
     * TARGET BINDING: Refers to a specific target selection from the declaration phase.
     * This solves the ambiguity of which target applies to which effect.
     * @property index The index of the TargetRequirement in the CardScript.
     */
    @SerialName("ContextTarget")
    @Serializable
    data class ContextTarget(val index: Int) : EffectTarget {
        override val description: String = "target"
    }

    /**
     * NAMED TARGET BINDING: Refers to a cast-time target by name rather than positional index.
     * Safer and more self-documenting than ContextTarget(index).
     *
     * The name must match the `id` field of a TargetRequirement in the card script.
     * For multi-target requirements (count > 1), use indexed names: "creature[0]", "creature[1]".
     *
     * @property name The name of the target binding (matches TargetRequirement.id)
     */
    @SerialName("BoundVariable")
    @Serializable
    data class BoundVariable(val name: String) : EffectTarget {
        override val description: String = "target"
    }

    /**
     * VARIABLE BINDING: Refers to an entity stored in a variable during effect execution.
     *
     * This enables Oblivion Ring-style effects:
     * - First trigger exiles a creature and stores it: `StoreResultEffect(exile, "exiledCard")`
     * - Second trigger returns it: `ReturnFromExileEffect(StoredEntityTarget("exiledCard"))`
     *
     * @property variableName The name of the variable holding the entity reference.
     */
    @SerialName("StoredEntityTarget")
    @Serializable
    data class StoredEntityTarget(val variableName: String) : EffectTarget {
        override val description: String = "the stored $variableName"
    }

    /**
     * PLAYER REFERENCE: Refers to a player or set of players.
     *
     * Usage:
     * - PlayerRef(Player.Each) → "each player"
     * - PlayerRef(Player.EachOpponent) → "each opponent"
     * - PlayerRef(Player.TargetOpponent) → "target opponent"
     * - PlayerRef(Player.TargetPlayer) → "target player"
     */
    @SerialName("PlayerRef")
    @Serializable
    data class PlayerRef(val player: Player) : EffectTarget {
        override val description: String = player.description
    }

    /**
     * GROUP REFERENCE: Refers to a group of permanents for mass effects.
     *
     * Usage:
     * - GroupRef(GroupFilter.AllCreatures) → "all creatures"
     * - GroupRef(GroupFilter.AllCreaturesYouControl) → "creatures you control"
     * - GroupRef(GroupFilter(GameObjectFilter.Creature.withColor(Color.RED))) → "all red creatures"
     */
    @SerialName("GroupRef")
    @Serializable
    data class GroupRef(val filter: GroupFilter) : EffectTarget {
        override val description: String = filter.description
    }

    /**
     * FILTERED TARGET: Refers to a target matching a composable filter.
     * For cases where ContextTarget isn't appropriate (e.g., dynamic effect targets
     * not bound at cast time).
     */
    @SerialName("FilteredTarget")
    @Serializable
    data class FilteredTarget(val filter: TargetFilter) : EffectTarget {
        override val description: String = "target ${filter.description}"
    }

    /**
     * SPECIFIC ENTITY: Refers to a specific entity by ID.
     * Used by delayed triggers to return a specific exiled card.
     */
    @SerialName("SpecificEntity")
    @Serializable
    data class SpecificEntity(val entityId: EntityId) : EffectTarget {
        override val description: String = "specific entity"
    }

    /**
     * PIPELINE TARGET: Refers to a target selected during effect resolution via SelectTargetEffect.
     *
     * Resolves to `context.storedCollections[collectionName][index]`.
     * @property collectionName The name of the stored collection containing the target IDs
     * @property index Which target in the collection (defaults to 0 for single-target)
     */
    @SerialName("PipelineTarget")
    @Serializable
    data class PipelineTarget(val collectionName: String, val index: Int = 0) : EffectTarget {
        override val description: String = "the chosen target"
    }

    /**
     * TRIGGERING ENTITY: Refers to the entity that caused the trigger to fire.
     * Used for effects like Aurification: "put a gold counter on it" where "it"
     * refers to the creature that dealt damage.
     */
    @SerialName("TriggeringEntity")
    @Serializable
    data object TriggeringEntity : EffectTarget {
        override val description: String = "that creature"
    }

    /**
     * CONTROLLER OF TRIGGERING ENTITY: Refers to the controller/owner of the
     * entity that caused the trigger to fire.
     * Used for effects like Tephraderm: "deals that much damage to that spell's controller"
     * where the triggering entity is the spell and we need its controller.
     */
    @SerialName("ControllerOfTriggeringEntity")
    @Serializable
    data object ControllerOfTriggeringEntity : EffectTarget {
        override val description: String = "that spell's controller"
    }
}
