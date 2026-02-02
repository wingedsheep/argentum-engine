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

    /** An opponent of the controller */
    @Serializable
    data object Opponent : EffectTarget {
        override val description: String = "target opponent"
    }

    /** Any player */
    @Serializable
    data object AnyPlayer : EffectTarget {
        override val description: String = "target player"
    }

    /** The source permanent itself */
    @Serializable
    data object Self : EffectTarget {
        override val description: String = "this creature"
    }

    /** Target creature */
    @Serializable
    data object TargetCreature : EffectTarget {
        override val description: String = "target creature"
    }

    /** Target creature an opponent controls */
    @Serializable
    data object TargetOpponentCreature : EffectTarget {
        override val description: String = "target creature an opponent controls"
    }

    /** Target creature you control */
    @Serializable
    data object TargetControlledCreature : EffectTarget {
        override val description: String = "target creature you control"
    }

    /** Target permanent */
    @Serializable
    data object TargetPermanent : EffectTarget {
        override val description: String = "target permanent"
    }

    /** Target nonland permanent */
    @Serializable
    data object TargetNonlandPermanent : EffectTarget {
        override val description: String = "target nonland permanent"
    }

    /** Target land */
    @Serializable
    data object TargetLand : EffectTarget {
        override val description: String = "target land"
    }

    /** Target nonblack creature */
    @Serializable
    data object TargetNonblackCreature : EffectTarget {
        override val description: String = "target nonblack creature"
    }

    /** Any target (creature or player) */
    @Serializable
    data object AnyTarget : EffectTarget {
        override val description: String = "any target"
    }

    /** Target card in a graveyard */
    @Serializable
    data object TargetCardInGraveyard : EffectTarget {
        override val description: String = "target card in a graveyard"
    }

    /** All creatures */
    @Serializable
    data object AllCreatures : EffectTarget {
        override val description: String = "all creatures"
    }

    /** All creatures you control */
    @Serializable
    data object AllControlledCreatures : EffectTarget {
        override val description: String = "creatures you control"
    }

    /** All creatures opponents control */
    @Serializable
    data object AllOpponentCreatures : EffectTarget {
        override val description: String = "creatures your opponents control"
    }

    /** Each opponent */
    @Serializable
    data object EachOpponent : EffectTarget {
        override val description: String = "each opponent"
    }

    /** Each player */
    @Serializable
    data object EachPlayer : EffectTarget {
        override val description: String = "each player"
    }

    /** Target tapped creature */
    @Serializable
    data object TargetTappedCreature : EffectTarget {
        override val description: String = "target tapped creature"
    }

    /** Target enchantment */
    @Serializable
    data object TargetEnchantment : EffectTarget {
        override val description: String = "target enchantment"
    }

    /** Target artifact */
    @Serializable
    data object TargetArtifact : EffectTarget {
        override val description: String = "target artifact"
    }

    /** Target nonland permanent an opponent controls */
    @Serializable
    data object TargetOpponentNonlandPermanent : EffectTarget {
        override val description: String = "target nonland permanent an opponent controls"
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

    /** Target creature with flying */
    @Serializable
    data object TargetCreatureWithFlying : EffectTarget {
        override val description: String = "target creature with flying"
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
}
