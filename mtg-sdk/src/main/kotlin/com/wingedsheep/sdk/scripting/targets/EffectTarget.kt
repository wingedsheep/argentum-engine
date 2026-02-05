package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Defines who/what an effect targets.
 */
@Serializable
sealed interface EffectTarget {
    val description: String

    /** The controller of the source ability */
    @Serializable
    data object Controller : EffectTarget {
        override val description: String = "you"
    }

    /** The source permanent itself */
    @Serializable
    data object Self : EffectTarget {
        override val description: String = "this creature"
    }

    /** The creature enchanted by this aura */
    @Serializable
    data object EnchantedCreature : EffectTarget {
        override val description: String = "enchanted creature"
    }

    /** The controller of the target (used for effects like "its controller gains 4 life") */
    @Serializable
    data object TargetController : EffectTarget {
        override val description: String = "its controller"
    }

    /**
     * TARGET BINDING: Refers to a specific target selection from the declaration phase.
     * This solves the ambiguity of which target applies to which effect.
     * @property index The index of the TargetRequirement in the CardScript.
     */
    @Serializable
    data class ContextTarget(val index: Int) : EffectTarget {
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
    @Serializable
    data class GroupRef(val filter: GroupFilter) : EffectTarget {
        override val description: String = filter.description
    }

    /**
     * FILTERED TARGET: Refers to a target matching a composable filter.
     * For cases where ContextTarget isn't appropriate (e.g., dynamic effect targets
     * not bound at cast time).
     */
    @Serializable
    data class FilteredTarget(val filter: TargetFilter) : EffectTarget {
        override val description: String = "target ${filter.description}"
    }
}
