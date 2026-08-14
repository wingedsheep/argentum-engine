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

    /**
     * The permanent whose static ability granted the currently-resolving ability — the
     * Equipment/Aura/permanent bearing the `GrantActivatedAbility` (or gained-abilities) static,
     * as opposed to [Self], which in a granted ability is the *host* that received it.
     *
     * Use for granted abilities that reference the granting object by name, e.g. an Equipment that
     * gives its bearer "...Return [this Equipment] to its owner's hand" (Trusty Boomerang), or
     * "Attach [this Equipment] to target creature" (Cranial Plating). Resolves to the granter
     * captured when the ability was put on the stack, so it survives the granter leaving play
     * (CR 113.7a last-known information — the effect no-ops if the granter is gone). For an ability
     * whose source already *is* the granter (Territory Forge / Sharkey-style gains), this resolves
     * to the same entity as [Self].
     */
    @SerialName("GrantingSource")
    @Serializable
    data object GrantingSource : EffectTarget {
        override val description: String = "the source that granted this ability"
    }

    /**
     * The controller of the target (used for effects like "its controller gains 4 life").
     *
     * Resolves through the projected controller (Layer-2 control changes are honored) and, once
     * the target has left the battlefield — typically because an earlier step of the same effect
     * destroyed or exiled it ("Destroy target creature. Its controller creates two Map tokens.")
     * — through the controller it last had on the battlefield (CR 608.2h last-known information),
     * before finally falling back to the card's owner.
     */
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
     * Equipment) became attached to — or, for the unattach mirror, came off of. Only meaningful
     * inside a [com.wingedsheep.sdk.scripting.EventPattern.BecomesAttachedEvent] or
     * [com.wingedsheep.sdk.scripting.EventPattern.BecomesUnattachedEvent] trigger, where the
     * triggering entity is the attachment and this resolves to the host on the other end.
     *
     * Used by Eriette, the Beguiler ("gain control of that permanent"), Assimilation Aegis
     * ("that creature becomes a copy …"), and Stitcher's Graft ("sacrifice that permanent"). On the
     * unattach side the host may already have left the battlefield — it then resolves to nothing and
     * the payoff is a no-op, which is exactly what Stitcher's Graft's ruling calls for.
     */
    @SerialName("AttachedToTriggeringPermanent")
    @Serializable
    data object AttachedToTriggeringPermanent : EffectTarget {
        override val description: String = "that permanent"
    }

    /**
     * DISCARDED AS COST: a card discarded to pay this spell/ability's additional cost, by index.
     * Mirrors the cost-referencing roles [com.wingedsheep.sdk.scripting.values.EntityReference.Sacrificed]
     * / [com.wingedsheep.sdk.scripting.values.EntityReference.TappedAsCost], but for the discard
     * cost (`Costs.additional.DiscardCards(...)`). Resolves to the discarded card's entity id —
     * the card is in its owner's graveyard by resolution (CR 608.2), so an
     * [com.wingedsheep.sdk.scripting.conditions.EntityMatches] reads that card's graveyard
     * characteristics (land vs nonland, type, color, …).
     *
     * **Resolution-only** (no projection meaning). Used by Grab the Prize: "Draw two cards. If the
     * discarded card wasn't a land card, ~ deals 2 damage to each opponent" — built via
     * `Conditions.DiscardedCardMatches(filter)`.
     *
     * @property index Which discarded card to reference (defaults to the first/only one).
     */
    @SerialName("DiscardedAsCost")
    @Serializable
    data class DiscardedAsCost(val index: Int = 0) : EffectTarget {
        override val description: String = "the discarded card"
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

/**
 * Placeholder token emitted where an effect refers to its own source permanent generically. The SDK
 * renders [EffectTarget.Self] as this token inside a
 * [com.wingedsheep.sdk.scripting.effects.SelfReferentialDescription]'s `descriptionTemplate`; the
 * type-aware render layer (the server's `ClientStateTransformer`) substitutes the noun matching the
 * host permanent's actual type ("this creature" for a creature, "this land"/"this artifact"/… for a
 * non-creature). [DEFAULT_SELF_NOUN] is the type-safe fallback applied wherever no host permanent is
 * in hand, so the raw token never leaks to an un-type-aware consumer.
 */
const val SELF_NOUN_TOKEN = "{self}"

/** Noun [SELF_NOUN_TOKEN] resolves to when the host permanent's type is unknown (tests, logs,
 * non-type-aware contexts). "this permanent" is correct for a permanent of any card type. */
const val DEFAULT_SELF_NOUN = "this permanent"

/** Replace every [SELF_NOUN_TOKEN] in [text] with [noun]. */
fun resolveSelfNoun(text: String, noun: String): String = text.replace(SELF_NOUN_TOKEN, noun)

/**
 * Type-neutral phrasing for the source permanent. For [EffectTarget.Self] this is the
 * [SELF_NOUN_TOKEN] placeholder (resolved to the permanent's actual-type noun downstream); every
 * non-[Self] target falls through to its own [EffectTarget.description] unchanged. Effects that
 * legitimately apply to non-creature permanents — transforming a double-faced artifact/land,
 * granting an ability to a Vehicle or DFC land — must build their `descriptionTemplate` through
 * this so the generated text never calls a land or artifact "this creature".
 */
val EffectTarget.selfNounToken: String
    get() = if (this is EffectTarget.Self) SELF_NOUN_TOKEN else description
