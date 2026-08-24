package com.wingedsheep.sdk.scripting.targets

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who chooses the target(s) for a [TargetRequirement].
 *
 * Defaults to [Controller] — the player casting the spell or activating the ability picks the
 * target, as in the overwhelming majority of cards. [Opponent] models the rare "… of an
 * opponent's choice" wording (Cuombajj Witches): the target is still a real target of the
 * controller's spell/ability — announced at the same time, equally respondable, and its legality
 * (hexproof/protection/shroud) is evaluated relative to the *controller*, per CR 115 — but it is
 * an opponent who selects which legal object/player it is. In a multiplayer game the controller
 * first chooses which opponent makes the selection (CR 601.6a / 602.3a), and that selection happens
 * after the controller's own choices (CR 601.6b / 602.3b: the controller goes first, then the other
 * player).
 *
 * [TriggeringPlayer] and [ControllerOfTriggeringEntity] are the triggered-ability cases: a trigger
 * whose printed text hands the choice to somebody other than the ability's controller. They are two
 * cases rather than one because a trigger names its player in two different ways, exactly as
 * [com.wingedsheep.sdk.scripting.targets.EffectTarget] already splits `TriggeringEntity` from
 * `ControllerOfTriggeringEntity`: a step trigger's "that player" *is* the triggering entity, while
 * an enters trigger's "its controller" has to be read off the permanent that entered. Collapsing
 * them would make the right answer depend on the trigger's event shape rather than on the card.
 *
 * The chooser is orthogonal to legality: target-finding and validation ignore it (they always run
 * relative to the controller). Only the announcement layer reads it, to route the selection
 * decision to the right player.
 */
@Serializable
enum class TargetChooser {
    Controller,
    Opponent,

    /**
     * The player the trigger names decides — "that player … of their choice" (Quicksilver
     * Fountain). Resolves the way every other reader of the triggering player does
     * (`triggeringPlayerId ?: triggeringEntityId`), so a step trigger's active player and a
     * trigger that names a distinct player both land on the same case.
     */
    TriggeringPlayer,

    /**
     * The controller of the permanent that caused the trigger decides — "its controller chooses
     * target permanent …" (Confusion in the Ranks). Distinct from [TriggeringPlayer], which treats
     * the triggering entity itself as the deciding player.
     */
    ControllerOfTriggeringEntity
}

/**
 * Defines what can be targeted by a spell or ability.
 * Each TargetRequirement specifies the valid targets and any restrictions.
 *
 * TargetRequirements are data objects - validation is handled by TargetValidator
 * which checks targets against GameState.
 *
 * Target count semantics:
 * - count = maximum number of targets
 * - minCount = minimum number of targets (defaults to count if not specified)
 * - optional = if true, minCount becomes 0 ("up to X" targets)
 *
 * Examples:
 * - "target creature": count=1, minCount=1
 * - "up to two target creatures": count=2, optional=true (minCount becomes 0)
 * - "one or two target creatures": count=2, minCount=1
 */
@Serializable
sealed interface TargetRequirement : TextReplaceable<TargetRequirement> {
    val description: String
    val count: Int get() = 1  // Maximum targets
    val minCount: Int get() = count  // Minimum targets (defaults to count)
    val optional: Boolean get() = false  // If true, minCount becomes 0
    /**
     * Who selects this requirement's target(s). Defaults to [TargetChooser.Controller]; set to
     * [TargetChooser.Opponent] for "… of an opponent's choice" wording. See [TargetChooser].
     * Currently honored at announcement for activated abilities (the only printed use, Cuombajj
     * Witches); a requirement whose chooser is an opponent should appear after the
     * controller-chosen requirements in a script.
     */
    val chooser: TargetChooser get() = TargetChooser.Controller
    /**
     * If true, the target count has no upper bound — "any number of target ...".
     * The practical maximum is the number of legal targets, which the engine surfaces
     * to the client; validation imposes no cap. Implies a minimum of 0, so [count] is
     * left at its default and ignored. Use this instead of a large placeholder [count].
     */
    val unlimited: Boolean get() = false
    /** Named identifier for this target requirement. When set, enables BoundVariable resolution. */
    val id: String? get() = null

    /** Effective minimum after considering optional/unlimited flags */
    val effectiveMinCount: Int get() = if (optional || unlimited) 0 else minCount
}

// =============================================================================
// Player Targeting
// =============================================================================

/**
 * Target player (any player).
 *
 * [restriction], when non-null, is a [Condition] that each candidate player must satisfy to be a
 * legal target ("target player who lost life this turn", "target player with 10 or less life").
 * It is evaluated against each player with [Player.Candidate] bound to that player, both when
 * enumerating legal targets and — per CR 608.2b — re-checked at resolution, so a target whose
 * restriction stopped holding is removed. Author it through the `Conditions.candidate*` facade.
 * Because condition descriptions don't read as English relative clauses, pass
 * [descriptionOverride] (e.g. "target player who lost life this turn") whenever [restriction] is set.
 */
@SerialName("TargetPlayer")
@Serializable
data class TargetPlayer(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val unlimited: Boolean = false,
    override val id: String? = null,
    val restriction: Condition? = null,
    private val descriptionOverride: String? = null
) : TargetRequirement {
    override val description: String = descriptionOverride
        ?: when {
            unlimited -> "any number of target players"
            count == 1 -> "target player"
            else -> "target $count players"
        }

    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newRestriction = restriction?.applyTextReplacement(replacer)
        return if (newRestriction !== restriction) copy(restriction = newRestriction) else this
    }
}

/**
 * Target opponent only.
 *
 * See [TargetPlayer.restriction] — [restriction] applies the same per-candidate gate, scoped to
 * opponents of the controller.
 */
@SerialName("TargetOpponent")
@Serializable
data class TargetOpponent(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val unlimited: Boolean = false,
    override val id: String? = null,
    val restriction: Condition? = null,
    private val descriptionOverride: String? = null
) : TargetRequirement {
    override val description: String = descriptionOverride
        ?: when {
            unlimited -> "any number of target opponents"
            count == 1 -> "target opponent"
            else -> "target $count opponents"
        }

    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newRestriction = restriction?.applyTextReplacement(replacer)
        return if (newRestriction !== restriction) copy(restriction = newRestriction) else this
    }
}

// =============================================================================
// Creature Targeting (factory function — returns TargetObject)
// =============================================================================

/**
 * Target creature (any creature on the battlefield).
 * Factory function that returns a TargetObject with appropriate defaults.
 */
fun TargetCreature(
    count: Int = 1,
    minCount: Int = count,
    optional: Boolean = false,
    unlimited: Boolean = false,
    filter: TargetFilter = TargetFilter.Creature,
    id: String? = null,
    dynamicMaxCount: DynamicAmount? = null,
    sameController: Boolean = false,
    differentControllers: Boolean = false,
    sameCreatureType: Boolean = false,
    chooser: TargetChooser = TargetChooser.Controller
): TargetObject = TargetObject(
    count = count,
    minCount = minCount,
    optional = optional,
    unlimited = unlimited,
    filter = filter,
    id = id,
    dynamicMaxCount = dynamicMaxCount,
    sameController = sameController,
    differentControllers = differentControllers,
    sameCreatureType = sameCreatureType,
    chooser = chooser
)

// =============================================================================
// Permanent Targeting (factory function — returns TargetObject)
// =============================================================================

/**
 * Target permanent (any permanent on the battlefield).
 * Factory function that returns a TargetObject with appropriate defaults.
 */
fun TargetPermanent(
    count: Int = 1,
    optional: Boolean = false,
    unlimited: Boolean = false,
    filter: TargetFilter = TargetFilter.Permanent,
    id: String? = null,
    dynamicMaxCount: DynamicAmount? = null,
    sameCardType: Boolean = false
): TargetObject = TargetObject(
    count = count,
    optional = optional,
    unlimited = unlimited,
    filter = filter,
    id = id,
    dynamicMaxCount = dynamicMaxCount,
    sameCardType = sameCardType
)

// =============================================================================
// Combined Targeting
// =============================================================================

/**
 * "Any target" - can target any creature, player, or planeswalker.
 */
@SerialName("AnyTarget")
@Serializable
data class AnyTarget(
    override val count: Int = 1,
    override val minCount: Int = count,
    override val optional: Boolean = false,
    override val id: String? = null,
    override val chooser: TargetChooser = TargetChooser.Controller,
    private val descriptionOverride: String? = null
) : TargetRequirement {
    override val description: String = descriptionOverride
        ?: buildString {
            append(if (count == 1) "any target" else "$count targets")
            if (chooser == TargetChooser.Opponent) append(" of an opponent's choice")
        }
}

/**
 * "Target creature or player" - classic burn spell targeting.
 */
@SerialName("TargetCreatureOrPlayer")
@Serializable
data class TargetCreatureOrPlayer(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target creature or player" else "$count targets (creatures or players)"
}

/**
 * "Target permanent or player" — any permanent on the battlefield, or any player.
 *
 * The general member of the "object or player" family: [permanentFilter] decides which permanents
 * qualify, so the same type covers "target permanent or player" (Powerful Broker, the default),
 * "target artifact or player", and so on. The narrower [TargetCreatureOrPlayer] predates it and is
 * kept as-is because it is the serialized form of already-shipped cards; new "… or player" wordings
 * that aren't exactly "creature or player" should use this requirement.
 *
 * [TargetCreatureOrPlayer] is a strict special case (`permanentFilter = TargetFilter.Creature`) and
 * the engine currently validates it through a parallel implementation, which can drift. The
 * intended cleanup is to keep its `@SerialName` and type but have its validator delegate here —
 * anti-drift at zero serialization cost — once its face-down special case (CR 708.2) is confirmed
 * to survive the general path. Not done in this change.
 *
 * **Before adding a sixth "… or player" type, read `backlog/target-union-with-arms.md`.**
 * That scoping doc proposes collapsing this whole family — [AnyTarget], [TargetCreatureOrPlayer],
 * [TargetPlayerOrPlaneswalker], [TargetOpponentOrPlaneswalker] and this type — into one
 * `TargetUnion(arms)` where each arm carries its own criteria. This requirement is the closest
 * existing type to that shape (it is the only member of the family with a criteria slot, and the
 * proposal's central complaint about the others is that they have none), so it is the intended
 * migration target: a new "X or player" wording should be `TargetPermanentOrPlayer(permanentFilter
 * = …)` rather than another bespoke type, and if it genuinely cannot be, that is the signal to build
 * `TargetUnion` instead of extending the family again.
 *
 * Legality is the union of the two halves — a permanent target is checked for
 * hexproof/shroud/protection like any permanent, a player target like any player — and, being a
 * target, it is chosen on announcement (CR 601.2c) and re-checked on resolution (CR 608.2b).
 */
@SerialName("TargetPermanentOrPlayer")
@Serializable
data class TargetPermanentOrPlayer(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null,
    val permanentFilter: TargetFilter = TargetFilter.Permanent,
    private val descriptionOverride: String? = null
) : TargetRequirement {
    override val description: String = descriptionOverride
        ?: run {
            val noun = permanentFilter.description
            when {
                count == 1 -> "target $noun or player"
                // Suffixing "s" only reads correctly for a bare noun; a longer filter
                // description ("artifact creature you control") would come out as
                // "... you controls". Leave those singular and let a card pass a
                // descriptionOverride if it needs better.
                !noun.contains(' ') -> "$count targets (${noun}s or players)"
                else -> "$count targets ($noun or player)"
            }
        }

    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newFilter = permanentFilter.applyTextReplacement(replacer)
        return if (newFilter !== permanentFilter) copy(permanentFilter = newFilter) else this
    }
}

/**
 * "Target opponent or planeswalker" - can target an opponent or any planeswalker.
 */
@SerialName("TargetOpponentOrPlaneswalker")
@Serializable
data class TargetOpponentOrPlaneswalker(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target opponent or planeswalker" else "$count targets (opponents or planeswalkers)"
}

/**
 * "Target player or planeswalker" - can target any player or any planeswalker.
 */
@SerialName("TargetPlayerOrPlaneswalker")
@Serializable
data class TargetPlayerOrPlaneswalker(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target player or planeswalker" else "$count targets (players or planeswalkers)"
}

/**
 * "Target creature or planeswalker" - modern burn spell targeting.
 */
@SerialName("TargetCreatureOrPlaneswalker")
@Serializable
data class TargetCreatureOrPlaneswalker(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target creature or planeswalker" else "$count targets (creatures or planeswalkers)"
}

// =============================================================================
// Spell or Permanent Targeting
// =============================================================================

/**
 * "Target spell or permanent" - can target spells on the stack or permanents
 * on the battlefield.
 *
 * Used by text-changing effects like Artificial Evolution and bounce-to-library
 * effects like Swat Away ("target spell or creature").
 *
 * The [permanentFilter] restricts which permanents are valid. When null, any
 * permanent may be targeted. For "target spell or creature", pass
 * [GameObjectFilter.Creature].
 */
@SerialName("TargetSpellOrPermanent")
@Serializable
data class TargetSpellOrPermanent(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null,
    val permanentFilter: GameObjectFilter? = null
) : TargetRequirement {
    override val description: String = run {
        val permanentNoun = permanentFilter?.description ?: "permanent"
        if (count == 1) "target spell or $permanentNoun"
        else "$count target spells or ${permanentNoun}s"
    }
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newFilter = permanentFilter?.applyTextReplacement(replacer)
        return if (newFilter !== permanentFilter) copy(permanentFilter = newFilter) else this
    }
}

// =============================================================================
// Card Targeting (other zones)
// =============================================================================

// =============================================================================
// Spell Targeting (factory function — returns TargetObject)
// =============================================================================

/**
 * Target spell on the stack.
 * Factory function that returns a TargetObject with appropriate defaults.
 */
fun TargetSpell(
    count: Int = 1,
    optional: Boolean = false,
    filter: TargetFilter = TargetFilter.SpellOnStack,
    id: String? = null,
    unlimited: Boolean = false
): TargetObject = TargetObject(
    count = count, optional = optional, filter = filter, id = id, unlimited = unlimited
)

// =============================================================================
// Generic Object Targeting
// =============================================================================

/**
 * Target any game object matching a filter.
 * Generalizes zone-specific targeting — the TargetFilter's zone field
 * determines which zone to look in.
 *
 * @param count Maximum number of targets when [dynamicMaxCount] is null. Ignored
 *   otherwise — the resolved dynamic value is authoritative.
 * @param optional If true, allows 0 targets ("up to X" style targeting)
 * @param filter Determines what can be targeted and in which zone
 * @param dynamicMaxCount When non-null, the engine evaluates this at the moment the
 *   spell/ability goes on the stack and uses the resolved value as the maximum number
 *   of targets. Used for "up to X target ..." where X is determined by board state,
 *   like Prismabasher's Vivid trigger ("up to X target creatures you control",
 *   X = colors among permanents you control).
 */
@SerialName("TargetObject")
@Serializable
data class TargetObject(
    override val count: Int = 1,
    override val minCount: Int = count,
    override val optional: Boolean = false,
    override val unlimited: Boolean = false,
    val filter: TargetFilter,
    override val id: String? = null,
    val dynamicMaxCount: DynamicAmount? = null,
    /**
     * When true and more than one target is chosen for this requirement, every chosen
     * target must be controlled by the same player ("two target creatures controlled by
     * the same player"). Enforced cross-target by `TargetValidator` at cast time; a no-op
     * for single-target requirements. Defaults to false.
     */
    val sameController: Boolean = false,
    /**
     * When true and more than one target is chosen for this requirement, every chosen
     * card target must be owned by the same player — i.e. drawn "from a single graveyard"
     * (Arashin Sunshield: "exile up to two target cards from a single graveyard").
     * Enforced cross-target by `TargetValidator` against each `ChosenTarget.Card`'s owner;
     * a no-op for single-target requirements and for non-card targets. Defaults to false.
     */
    val sameOwner: Boolean = false,
    /**
     * When true and more than one target is chosen for this requirement, the chosen permanent
     * targets must all share at least one creature type with one another — "two target creatures
     * you control that share a creature type" (Secret Tunnel). Enforced cross-target by
     * `TargetValidator` using each permanent's *projected* creature subtypes (so granted/changed
     * types via continuous effects count); a no-op for single-target requirements and for
     * non-permanent targets. Defaults to false.
     */
    val sameCreatureType: Boolean = false,
    /**
     * When true and more than one target is chosen for this requirement, the chosen permanent
     * targets must all share at least one **card type** (CR 205.2a — artifact, creature,
     * enchantment, planeswalker, …) with one another — "two target nonland permanents that share a
     * card type" (Burglar's Plot). The card-type sibling of [sameCreatureType]: enforced
     * cross-target by `TargetValidator` using each permanent's *projected* types (so an animated
     * land counts as a creature) with supertypes sieved out (two legendary permanents don't share
     * a card type by being legendary). A no-op for single-target requirements and for non-permanent
     * targets. Defaults to false.
     */
    val sameCardType: Boolean = false,
    /**
     * When non-null, the chosen card targets for this requirement must have a **combined mana
     * value no greater than this amount** — "any number of target creature cards with total mana
     * value X or less" (Fire Lord Sozin). The [DynamicAmount] is resolved once the ability is
     * being put on the stack (so `DynamicAmount.XValue` reads the X just paid) and enforced
     * cross-target against the summed `manaValue` of the chosen cards by both `TargetValidator`
     * (authoritative) and the interactive `DecisionValidators` (which sees the resolved integer
     * cap baked into the decision). Pair with `unlimited = true` for the "any number … with total
     * mana value N or less" shape. `null` (the default) imposes no aggregate cap. Distinct from
     * `dynamicMaxCount`, which caps the *count* of targets, not their summed mana value.
     */
    val totalManaValueAtMost: DynamicAmount? = null,
    /**
     * When true and more than one target is chosen for this requirement, every chosen target must
     * have a **different name** from the others — "up to six target creature cards with different
     * names" (Behold the Sinister Six!). Enforced cross-target by `TargetValidator` (authoritative)
     * and the interactive `DecisionValidators`, grouping by each target's *projected* card name; a
     * no-op for single-target requirements. Defaults to false.
     */
    val differentNames: Boolean = false,
    /**
     * When true and more than one target is chosen for this requirement, every chosen target must
     * be controlled by a **different player** — the "one per player" distribution wording, whose
     * canonical shape is "for each other player, exile **up to one** target creature that player
     * controls" (Kaya, Spirits' Justice). The exact inverse of [sameController], and it composes
     * with `optional = true` + `dynamicMaxCount = DynamicAmount.PlayerCount(Player.EachOpponent)`
     * to spell that clause completely: the count says *how many* players are in scope, this says
     * *at most one each*, and `optional` is the "up to". Enforced cross-target by `TargetValidator`
     * (authoritative) and the interactive `DecisionValidators`, grouping by each permanent's
     * *projected* controller so a control-change effect is respected; a no-op for single-target
     * requirements and for non-permanent targets. Defaults to false.
     */
    val differentControllers: Boolean = false,
    /**
     * Who picks which legal object this requirement lands on. See [TargetChooser] — the target
     * stays the *controller's* target either way (legality, respondability and CR 115 all still
     * run relative to the controller); only the selection decision is routed elsewhere.
     * [TargetChooser.TriggeringPlayer] and [TargetChooser.ControllerOfTriggeringEntity] are honored
     * on triggered abilities, [TargetChooser.Opponent] on activated ones; `CardLinter` fails a card
     * that puts one in a context the engine doesn't route.
     */
    override val chooser: TargetChooser = TargetChooser.Controller
) : TargetRequirement {
    override val description: String = run {
        val base = if (id != null) {
            buildString {
                // The author-supplied id is often the complete phrase already (e.g. Elrond,
                // Master of Healing's "up to X target creatures"). Only add the quantifier when
                // the id doesn't already begin with it, so we don't render "up to up to …".
                val quantifier = when {
                    unlimited -> "any number of "
                    optional -> "up to "
                    minCount < count -> "$minCount to "
                    else -> ""
                }
                if (quantifier.isNotEmpty() && !id.startsWith(quantifier)) append(quantifier)
                append(id)
            }
        } else {
            buildString {
                if (unlimited) {
                    append("any number of target ")
                    append("${filter.description}s")
                } else {
                    if (optional) append("up to ")
                    else if (minCount < count) append("$minCount to ")
                    append("target ")
                    append(if (count == 1) filter.description else "$count ${filter.description}s")
                }
            }
        }
        val qualified = when {
            sameController -> "$base controlled by the same player"
            differentControllers -> "$base controlled by different players"
            sameOwner -> "$base from a single graveyard"
            sameCreatureType -> "$base that share a creature type"
            sameCardType -> "$base that share a card type"
            else -> base
        }
        if (totalManaValueAtMost != null) {
            "$qualified with total mana value ${totalManaValueAtMost.description} or less"
        } else {
            qualified
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

// =============================================================================
// Special Targeting
// =============================================================================

/**
 * Marks a target as needing to be different from other targets of the same spell or
 * ability — used for "any other target" / "another target" wording. Validation
 * delegates to [baseRequirement] and additionally rejects any chosen target that
 * matches a target chosen for an earlier requirement of the same cast/activation.
 *
 * If [excludeSourceId] is set (or sourceId is provided at validation time), that id
 * is also excluded — useful for "another target spell" semantics.
 *
 * If [excludeAttachedCreature] is set, the creature the source is attached to (its
 * Aura/Equipment target) is excluded instead of the source itself — used for "enchanted
 * creature deals damage … to any other target" wording, where the dealer is the attached
 * creature rather than the ability's source permanent.
 */
@SerialName("TargetOther")
@Serializable
data class TargetOther(
    val baseRequirement: TargetRequirement,
    val excludeSourceId: EntityId? = null,
    val excludeAttachedCreature: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = baseRequirement.description
    // Every count-shaping field is delegated, not just `count`: this wrapper only adds a
    // distinctness rule, so dropping any of them silently reshapes how many targets the wrapped
    // requirement accepts — an "any number of other target …" requirement would collapse to a
    // single mandatory target (`unlimited` lost ⇒ count 1, minCount 1).
    override val count: Int = baseRequirement.count
    override val minCount: Int = baseRequirement.minCount
    override val optional: Boolean = baseRequirement.optional
    override val unlimited: Boolean = baseRequirement.unlimited
    override val chooser: TargetChooser = baseRequirement.chooser

    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newBase = baseRequirement.applyTextReplacement(replacer)
        return if (newBase !== baseRequirement) copy(baseRequirement = newBase) else this
    }
}

/**
 * Return a copy of this requirement whose maximum [count] is narrowed to [newCount], clamping
 * [minCount] down so it never exceeds the new maximum. A no-op when [newCount] already equals
 * [count].
 *
 * Used when a chosen target set is smaller than a requirement's declared maximum (a partially
 * filled "up to N" slot). The flattened target↔requirement index walks
 * (`StackResolver.getRequirementForTargetIndex`, `EffectContext.buildNamedTargets`) advance by
 * `count`, so an over-wide requirement would absorb a *later* slot's targets and validate them
 * against the wrong filter. Shrinking the requirement to the targets actually chosen keeps those
 * walks aligned.
 */
fun TargetRequirement.withCount(newCount: Int): TargetRequirement {
    if (newCount == count) return this
    val clampedMin = minOf(minCount, newCount)
    return when (this) {
        is TargetPlayer -> copy(count = newCount)
        is TargetOpponent -> copy(count = newCount)
        is AnyTarget -> copy(count = newCount, minCount = clampedMin)
        is TargetCreatureOrPlayer -> copy(count = newCount)
        is TargetPermanentOrPlayer -> copy(count = newCount)
        is TargetOpponentOrPlaneswalker -> copy(count = newCount)
        is TargetPlayerOrPlaneswalker -> copy(count = newCount)
        is TargetCreatureOrPlaneswalker -> copy(count = newCount)
        is TargetSpellOrPermanent -> copy(count = newCount)
        is TargetObject -> copy(count = newCount, minCount = clampedMin)
        is TargetOther -> copy(baseRequirement = baseRequirement.withCount(newCount))
    }
}

/**
 * Create a copy of this TargetRequirement with the given id set.
 * Used by the DSL to stamp an id onto requirements passed to target(name, requirement).
 */
fun TargetRequirement.withId(name: String): TargetRequirement = when (this) {
    is TargetPlayer -> copy(id = name)
    is TargetOpponent -> copy(id = name)
    is AnyTarget -> copy(id = name)
    is TargetCreatureOrPlayer -> copy(id = name)
    is TargetPermanentOrPlayer -> copy(id = name)
    is TargetOpponentOrPlaneswalker -> copy(id = name)
    is TargetPlayerOrPlaneswalker -> copy(id = name)
    is TargetCreatureOrPlaneswalker -> copy(id = name)
    is TargetSpellOrPermanent -> copy(id = name)
    is TargetObject -> copy(id = name)
    is TargetOther -> copy(id = name)
}
