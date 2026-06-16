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

    /** The creature this equipment is attached to */
    @SerialName("EquippedCreature")
    @Serializable
    data object EquippedCreature : EffectTarget {
        override val description: String = "equipped creature"
    }

    /**
     * The permanent this aura/equipment is attached to, regardless of its type.
     * Use for auras that enchant non-creature permanents (e.g., Wellspring enchants
     * a land: "gain control of enchanted land"). Resolves via the source's
     * attachment relationship, exactly like [EnchantedCreature]/[EquippedCreature].
     */
    @SerialName("EnchantedPermanent")
    @Serializable
    data object EnchantedPermanent : EffectTarget {
        override val description: String = "enchanted permanent"
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
     * CONTROLLER OF PIPELINE TARGET: Refers to the controller of an entity stored in
     * a pipeline collection. Used for effects like "Exile target nonland permanent.
     * Its controller draws a card." where the target was selected via SelectTargetEffect.
     *
     * @property collectionName The name of the stored collection containing the target
     * @property index Which target in the collection (defaults to 0 for single-target)
     */
    @SerialName("ControllerOfPipelineTarget")
    @Serializable
    data class ControllerOfPipelineTarget(val collectionName: String, val index: Int = 0) : EffectTarget {
        override val description: String = "its controller"
    }

    /**
     * CHOSEN CREATURE: Refers to the creature chosen when this permanent entered the battlefield.
     * Used by cards like Dauntless Bodyguard that store a chosen creature reference.
     */
    @SerialName("ChosenCreature")
    @Serializable
    data object ChosenCreature : EffectTarget {
        override val description: String = "the chosen creature"
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

    /**
     * ATTACHED-TO TRIGGERING PERMANENT: the permanent that the triggering attachment (Aura/
     * Equipment) became attached to. Only meaningful inside a
     * [com.wingedsheep.sdk.scripting.EventPattern.BecomesAttachedEvent] trigger, where the
     * triggering entity is the attachment and this resolves to the thing it attached to.
     *
     * Used by Eriette, the Beguiler ("gain control of that permanent") and Assimilation Aegis
     * ("that creature becomes a copy …").
     */
    @SerialName("AttachedToTriggeringPermanent")
    @Serializable
    data object AttachedToTriggeringPermanent : EffectTarget {
        override val description: String = "that permanent"
    }

    /**
     * CONTROLLER OF DAMAGE SOURCE: the controller of the source dealing the damage
     * currently being processed. Only meaningful inside a damage replacement
     * (e.g. [com.wingedsheep.sdk.scripting.RedirectDamage]); resolved by the damage
     * pipeline from the source of the in-flight damage.
     *
     * Used by Harsh Judgment ("If an instant or sorcery spell of the chosen color
     * would deal damage to you, it deals that damage to its controller instead").
     */
    @SerialName("ControllerOfDamageSource")
    @Serializable
    data object ControllerOfDamageSource : EffectTarget {
        override val description: String = "its controller"
    }
}
