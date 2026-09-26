package com.wingedsheep.sdk.scripting.targets

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The one vocabulary for "which entity": who or what an effect acts on, and which object a value
 * read (a characteristic comparison, a `DynamicAmount.EntityProperty`, the colors a loop iterates)
 * is taken from.
 *
 * References are late-bound — resolved against the resolution context when the instruction runs.
 * The engine resolves every member through one path, which keeps the two rules that tell an
 * *action* from a *value read* in one place: an action on the source, the triggering object, or a
 * loop's current object does nothing once that object has changed zones and become a new object
 * (CR 400.7), while a value read of it falls back to last-known information (CR 608.2h).
 */
@Serializable
sealed interface EffectTarget {
    val description: String

    /**
     * A reference that names exactly one entity — the source, a chosen target, the triggering
     * object, a permanent paid as a cost, the object a loop is visiting, and so on — as opposed to
     * a player role ([Controller], [PlayerRef], the `ControllerOf…` family) or a set of objects
     * ([GroupRef], [FilteredTarget], [EachDamagedBySourceThisGame]).
     *
     * Slots that read one object's characteristics take this type, so a group or a player role is
     * unrepresentable there: `CardPredicate.SharesColorWith(entity)`,
     * `DynamicAmount.EntityProperty(entity, …)`, `IterationSpace.ColorsOf(source)`.
     */
    @Serializable
    sealed interface SingleEntity : EffectTarget

    /** The current top card of a player's library; absent when that library is empty. */
    @SerialName("LibraryTop")
    @Serializable
    data class LibraryTop(val player: Player = Player.You) : SingleEntity {
        override val description: String = "the top card of ${player.possessive} library"
    }

    /** The controller of the source ability */
    @SerialName("Controller")
    @Serializable
    data object Controller : EffectTarget {
        override val description: String = "you"
    }

    /**
     * The source of the resolving ability — the permanent (or spell) it belongs to. For a granted
     * ability that is the permanent that received it (see [GrantingSource] for the granter).
     *
     * Always the source, including inside a `ForEach` loop body: the object the loop is visiting
     * is [IterationEntity].
     */
    @SerialName("Self")
    @Serializable
    data object Self : SingleEntity {
        override val description: String = "this creature"
    }

    /** The creature enchanted by this aura */
    @SerialName("EnchantedCreature")
    @Serializable
    data object EnchantedCreature : SingleEntity {
        override val description: String = "enchanted creature"
    }

    /** The creature this equipment is attached to */
    @SerialName("EquippedCreature")
    @Serializable
    data object EquippedCreature : SingleEntity {
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
    data object EnchantedPermanent : SingleEntity {
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
    data object GrantingSource : SingleEntity {
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
    data class ContextTarget(val index: Int) : SingleEntity {
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
    data class BoundVariable(val name: String) : SingleEntity {
        override val description: String = "target"

        /**
         * This target read as a player, for the slots typed as [Player] — "the cards in *that
         * player's* hand" (`CardSource.FromZone(Zone.HAND, opponent.asPlayer)`).
         */
        val asPlayer: Player get() = Player.BoundVariable(name)
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
     * MULTI-ENTITY REFERENCE: every **opponent and planeswalker the effect's source has dealt
     * damage to this game** — The Fallen's "each opponent and planeswalker it has dealt damage to
     * this game".
     *
     * Backed by the source's accumulating `DealtDamageToThisGameComponent`, filtered at resolution
     * to recipients still in the game: players always are, a planeswalker must still be on the
     * battlefield. Players are narrowed to the source's *opponents* because that is what the
     * printed line says — a source that somehow damaged its own controller does not come back
     * around on them.
     *
     * Like [PlayerRef] with `Player.EachOpponent`, this resolves to a *set*, so it is only
     * meaningful to effects that iterate their target; `DealDamageEffect` does.
     */
    @SerialName("EachDamagedBySourceThisGame")
    @Serializable
    data object EachDamagedBySourceThisGame : EffectTarget {
        override val description: String =
            "each opponent and planeswalker it has dealt damage to this game"
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
    data class SpecificEntity(val entityId: EntityId) : SingleEntity {
        override val description: String = "specific entity"
    }

    /**
     * PIPELINE TARGET: an entity recorded in the resolution pipeline's stored collections — a
     * target selected mid-resolution by `SelectTargetEffect`, or an entity an additional-cost step
     * recorded under its `storeAs` key (`AdditionalCost.ChooseEntity`).
     *
     * Resolves to `context.storedCollections[collectionName][index]`. A value read of an entity a
     * cost step chose falls back to the snapshot taken when the cost was paid if the entity has
     * since left the battlefield (CR 608.2h), like [SacrificedAsCost] / [TappedAsCost].
     *
     * @property collectionName The name of the stored collection containing the target IDs
     * @property index Which target in the collection (defaults to 0 for single-target)
     */
    @SerialName("PipelineTarget")
    @Serializable
    data class PipelineTarget(val collectionName: String, val index: Int = 0) : SingleEntity {
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
    data object ChosenCreature : SingleEntity {
        override val description: String = "the chosen creature"
    }

    /**
     * TRIGGERING ENTITY: Refers to the entity that caused the trigger to fire.
     * Used for effects like Aurification: "put a gold counter on it" where "it"
     * refers to the creature that dealt damage.
     */
    @SerialName("TriggeringEntity")
    @Serializable
    data object TriggeringEntity : SingleEntity {
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
    data object AttachedToTriggeringPermanent : SingleEntity {
        override val description: String = "that permanent"
    }

    /**
     * DISCARDED AS COST: a card discarded to pay this spell's additional cost
     * (`Costs.additional.DiscardCards(...)`) or this activated ability's cost (`Costs.Discard(...)`),
     * by index. The discard counterpart of [SacrificedAsCost] / [TappedAsCost]. Resolves to the discarded card's entity id —
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
    data class DiscardedAsCost(val index: Int = 0) : SingleEntity {
        override val description: String = "the discarded card"
    }

    /**
     * SACRIFICED AS COST: a permanent sacrificed to pay this spell/ability's cost, by index —
     * "the sacrificed creature". A value read of it ("damage equal to the sacrificed creature's
     * power") uses the snapshot taken as the cost was paid (CR 608.2h), since the permanent is
     * gone by resolution.
     */
    @SerialName("SacrificedAsCost")
    @Serializable
    data class SacrificedAsCost(val index: Int = 0) : SingleEntity {
        override val description: String = "the sacrificed creature"
    }

    /**
     * TAPPED AS COST: a permanent tapped to pay this activation's cost — the tap counterpart of
     * [SacrificedAsCost], reading `EffectContext.tappedPermanents`. A value read of it falls back
     * to the snapshot taken as the cost was paid if it has since left the battlefield.
     *
     * Vodalian War Machine (FEM) is the card that needs it: each of its abilities taps an untapped
     * Merfolk, and its death trigger destroys the Merfolk that paid. Each activation schedules its
     * own delayed trigger naming *its* Merfolk, so the reference is baked into a concrete id when
     * the trigger is created and the whole set is destroyed when the War Machine dies.
     *
     * @property index Which tapped permanent to reference; every printed use taps exactly one.
     */
    @SerialName("TappedAsCost")
    @Serializable
    data class TappedAsCost(val index: Int = 0) : SingleEntity {
        override val description: String = "the tapped permanent"
    }

    /**
     * LINKED EXILED CARD: a card exiled *with* the source permanent — its `LinkedExileComponent`,
     * the pile every imprint / "exiled with this" mechanic writes (CR 607 linked abilities), which
     * on Mirrodin's *Imprint* cards holds "the exiled card". This is the read-side companion to the
     * `linkToSource = true` exile that puts a card there: every `…With(entity)` predicate and
     * `DynamicAmount.EntityProperty` reads the imprinted card through it without new vocabulary —
     * `CardPredicate.SharesColorWith(LinkedExiledCard())` for Thought Prison.
     *
     * **Dual-mode** — resolvable at resolution *and* during static-ability projection, because it
     * is anchored to the source permanent exactly like [Self] and needs no resolution context. That
     * is what makes it usable as the entity of an
     * [com.wingedsheep.sdk.scripting.conditions.EntityMatches] gating a
     * [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] — Duplicant's "as long as a card
     * exiled with this creature is a creature card", built via
     * `Conditions.LinkedExiledCardMatches(filter)`.
     *
     * Resolution walks the pile in exile order and skips ids that have since left exile, so
     * [index] `0` is the *oldest card still exiled*. An empty (or fully departed) pile resolves to
     * nothing, so a condition over it reads false and an amount over it reads zero — the correct
     * reading of an imprint the controller declined.
     *
     * @property index Which exiled card to reference. Imprint exiles exactly one card, so this is a
     *   completeness parameter rather than something Mirrodin's cards need.
     */
    @SerialName("LinkedExiledCard")
    @Serializable
    data class LinkedExiledCard(val index: Int = 0) : SingleEntity {
        override val description: String = "the exiled card"
    }

    /**
     * RING-BEARER: a player's designated Ring-bearer (CR 701.54) — the creature carrying that
     * player's Ring-bearer designation, on the battlefield under their control. Resolves to
     * nothing when the player has no Ring-bearer, so `EntityProperty(RingBearer(), Power)` reads 0.
     *
     * The reference always names the *referenced* player's Ring-bearer, independent of any
     * per-player rebinding (e.g. inside a `ForEachPlayerEffect`), so "each player mills cards equal
     * to **your** Ring-bearer's power" (The One Ring to Rule Them All) measures the spell
     * controller's Ring-bearer for every player.
     */
    @SerialName("RingBearer")
    @Serializable
    data class RingBearer(val player: Player = Player.You) : SingleEntity {
        override val description: String = "${player.possessive} Ring-bearer"
    }

    /**
     * AFFECTED ENTITY: the permanent a continuous effect is being applied to during state
     * projection — the creature an Aura's static bonus lands on, one member of a lord's group. Only
     * meaningful inside a projected value ("gets +1/+1 for each other creature that shares a
     * creature type with it" — Alpha Status); empty at resolution.
     */
    @SerialName("AffectedEntity")
    @Serializable
    data object AffectedEntity : SingleEntity {
        override val description: String = "it"
    }

    /**
     * ITERATION ENTITY: the object a `ForEach` loop over a group or a collection is currently
     * visiting — "each creature you control gets +2/+1" is `ForEachInGroup(creatures you control,
     * ModifyStats(2, 1, IterationEntity))`. [Self] keeps meaning the source inside the loop body, so
     * one body can relate the two ("each other creature deals damage equal to its power to this
     * creature").
     *
     * Bound with the visited object's identity: once that object changes zones it is a new object
     * (CR 400.7) and an action aimed at it does nothing, exactly as for [Self]. A delayed trigger
     * created inside the loop captures the binding, so its effect can act on "that creature" later.
     */
    @SerialName("IterationEntity")
    @Serializable
    data object IterationEntity : SingleEntity {
        override val description: String = ITERATION_ENTITY_NOUN
    }

    /**
     * AMASSED ARMY: the Army chosen by the most recent Amass step in the resolving pipeline — "the
     * amassed Army", whether or not it received counters (CR 701.47c). Lets a follow-up effect read
     * it: "Amass Orcs 2, then ~ deals damage to any target equal to the amassed Army's power"
     * (Foray of Orcs).
     *
     * The engine writes the chosen Army's id into the pipeline under [STORAGE_KEY] after Amass
     * resolves and carries it across composite sub-effects and the multi-Army choice.
     */
    @SerialName("AmassedArmy")
    @Serializable
    data object AmassedArmy : SingleEntity {
        override val description: String = "the amassed Army"

        /**
         * Reserved pipeline-storage key — engine writers and SDK readers must agree on this string
         * so neither side has to import the other's module.
         */
        const val STORAGE_KEY: String = "__amassed_army"
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
 * How [EffectTarget.IterationEntity] reads in generated text. A group loop's description splices
 * the group back in over this phrase ("destroy that permanent" + "creatures you control" →
 * "destroy each creature you control").
 */
const val ITERATION_ENTITY_NOUN = "that permanent"

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
