package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Prevents the affected permanents from attacking.
 * Use [GroupFilter.source] for "this creature can't attack", [GroupFilter.attachedCreature]
 * for Pacifism-style auras, or any battlefield filter for "Creatures can't attack" effects.
 */
@SerialName("CantAttack")
@Serializable
data class CantAttack(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} can't attack"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Forces the affected permanents to attack each combat if able.
 * Use [GroupFilter.source] for "this creature attacks each combat", or any battlefield
 * filter for "All creatures attack each combat if able" effects (e.g. Grand Melee).
 */
@SerialName("MustAttack")
@Serializable
data class MustAttack(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} attack each combat if able"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Prevents the affected permanents from blocking.
 * Use [GroupFilter.source] for "this creature can't block", or any battlefield filter
 * for "Beasts can't block" / "Creatures can't block" effects.
 */
@SerialName("CantBlock")
@Serializable
data class CantBlock(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} can't block"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Forces the affected permanents to block each combat if able.
 * Used for enchantments like Grand Melee: "All creatures block each combat if able."
 */
@SerialName("MustBlock")
@Serializable
data class MustBlock(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} block each combat if able"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Static counterpart of [com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect]: the source
 * creature must be blocked while this ability is active. With [allCreatures] = false (default), at
 * least one creature able to block it must do so ("must be blocked if able" — Frodo Baggins); with
 * true, every creature able to block it must (Lure-style). Typically wrapped in a
 * [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] (e.g. gated on `SourceIsRingBearer`).
 */
@SerialName("MustBeBlockedStatic")
@Serializable
data class MustBeBlocked(
    val allCreatures: Boolean = false,
    val filter: GroupFilter? = null
) : StaticAbility {
    override val description: String = buildString {
        append(if (filter == null) "This creature" else filter.description)
        append(
            if (allCreatures) " must be blocked by all creatures able to block it"
            else " must be blocked each combat if able"
        )
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter?.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Creature assigns combat damage equal to its toughness rather than its power.
 * Conditional variant: only when toughness is greater than power.
 * Used for cards like Bark of Doran, Doran the Siege Tower, etc.
 *
 * The description reads off [filter], because this ability is not equipment-only: Doran and
 * Bedrock Tortoise scope it to a battlefield group, and The Kingpin of Crime grants that group form
 * to itself for a turn (see `Effects.GrantStaticAbility`). Hardcoding "equipped creature" here
 * rendered equipment flavour on every one of those.
 */
@SerialName("AssignDamageEqualToToughness")
@Serializable
data class AssignDamageEqualToToughness(
    val filter: GroupFilter = GroupFilter.attachedCreature(),
    val onlyWhenToughnessGreaterThanPower: Boolean = true
) : StaticAbility {
    override val description: String = when (filter.scope) {
        // The single-permanent scopes read naturally in the singular ("As long as X's toughness …
        // it assigns"); a battlefield group needs the plural, and takes its subject from the filter
        // exactly as CantAttack / CantBlock do.
        is Scope.Self, is Scope.AttachedTo -> {
            val subject = filter.description
            if (onlyWhenToughnessGreaterThanPower) {
                "As long as $subject's toughness is greater than its power, it assigns " +
                    "combat damage equal to its toughness rather than its power"
            } else {
                "${subject.replaceFirstChar(Char::uppercaseChar)} assigns combat damage equal to " +
                    "its toughness rather than its power"
            }
        }
        else -> buildString {
            append(filter.description.replaceFirstChar(Char::uppercaseChar))
            if (onlyWhenToughnessGreaterThanPower) {
                append(" with toughness greater than their power")
            }
            append(" assign combat damage equal to their toughness rather than their power")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Creatures this permanent's controller controls that are tapped to activate a Station
 * ability contribute their toughness (rather than their power), as long as toughness
 * is greater than power.
 *
 * Engine wiring (CR 702.184c): the station charge amount is its own node,
 * `DynamicAmount.StationCharge`, *not* a plain `EntityProperty(TappedAsCost, Power)` read — so
 * this substitution is confined to station abilities and never silently rewrites an unrelated
 * "tap a creature: do X equal to its power" ability. While a permanent with this static ability
 * is on the battlefield, the evaluator substitutes toughness for power when the tapped creature's
 * controller matches and toughness > power. The substitution is re-evaluated at resolution time
 * and uses last-known characteristics if the tapped creature has left the battlefield (Rule
 * 113.7a — Tapestry Warden 2025-07-25 rulings). A per-creature filter is not currently supported;
 * the override applies to all of the controller's creatures meeting the toughness > power condition.
 *
 * Used for Tapestry Warden: "Each creature you control with toughness greater than its
 * power stations permanents using its toughness rather than its power."
 */
@SerialName("StationUsingToughness")
@Serializable
data object StationUsingToughness : StaticAbility {
    override val description: String =
        "Each creature you control with toughness greater than its power stations permanents using its toughness rather than its power"
}

/**
 * This creature's combat damage may be divided as its controller chooses among
 * the defending player and/or any number of creatures they control.
 * Used for Butcher Orgg.
 *
 * Auto-assigns: lethal to each blocker in order, remainder to defending player.
 */
@SerialName("DivideCombatDamageFreely")
@Serializable
data class DivideCombatDamageFreely(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String =
        "You may assign this creature's combat damage divided as you choose among defending player and/or any number of creatures they control"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature may assign its combat damage as though it weren't blocked.
 * When blocked, the controller chooses whether to assign damage to blockers
 * or to the defending player/planeswalker. Used for Thorn Elemental.
 */
@SerialName("AssignCombatDamageAsUnblocked")
@Serializable
data class AssignCombatDamageAsUnblocked(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String =
        "You may have this creature assign its combat damage as though it weren't blocked"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature can't attack unless a condition is met.
 * Checked at attack declaration time when the defending player is known.
 *
 * The condition is evaluated with "you" = the creature's controller and
 * "opponent" = the defending player.
 *
 * @property condition The condition that must be met for the creature to attack
 * @property filter What this ability applies to
 */
/**
 * This creature can't attack unless its controller sacrifices [count] permanents matching
 * [sacrificeFilter], paid as attackers are declared — Leviathan's "this creature can't attack
 * unless you sacrifice two Islands".
 *
 * A **cost**, not a condition, which is why it is not [CantAttackUnless]: merely controlling two
 * Islands is not enough, they have to go. It is also not an *optional* attack cost (CR 508.1g,
 * "costs a player may pay as a creature attacks"): the clause is a restriction checked at
 * CR 508.1c, and its cost is determined and paid at CR 508.1h–j. CR 508.1d is the reason an
 * unpayable one simply keeps the creature home rather than making the whole declaration illegal. The declaration is illegal up front when the controller
 * doesn't control enough matching permanents to pay (a cost you can't pay can't be paid), and
 * otherwise the declare-attackers step pauses for the choice of which to sacrifice, in the same
 * window the generic-mana [AttackTax] pauses to be paid.
 *
 * Unlike [CantAttackOrBlockUnlessPay] this has no blocking half: the printed line is attack-only,
 * and a blocking sibling would need its own pause in the blocker step.
 */
@SerialName("CantAttackUnlessSacrifice")
@Serializable
data class CantAttackUnlessSacrifice(
    val sacrificeFilter: GameObjectFilter,
    val count: Int = 1,
) : StaticAbility {
    override val description: String =
        "can't attack unless you sacrifice $count ${sacrificeFilter.description}"

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = sacrificeFilter.applyTextReplacement(replacer)
        return if (newFilter !== sacrificeFilter) copy(sacrificeFilter = newFilter) else this
    }
}

@SerialName("CantAttackUnless")
@Serializable
data class CantAttackUnless(
    val condition: Condition,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't attack unless ${condition.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature can't attack unless another creature being declared as an attacker in the
 * same declaration matches [coAttackerFilter]. Used for cards like Scarred Puma
 * ("This creature can't attack unless a black or green creature also attacks").
 *
 * Unlike [CantAttackUnless], this restriction depends on the set of co-attackers rather than
 * on the defending player, so it is checked against the full proposed attacker group at
 * declaration time. The creature itself is never counted as its own co-attacker.
 *
 * @property coAttackerFilter The filter a *different* attacking creature must match.
 * @property filter What this ability applies to.
 */
@SerialName("CantAttackUnlessCoAttacker")
@Serializable
data class CantAttackUnlessCoAttacker(
    val coAttackerFilter: com.wingedsheep.sdk.scripting.GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't attack unless ${coAttackerFilter.description} also attacks"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature can't block unless a condition is met.
 * Checked at block declaration time when the attacking player is known.
 *
 * The condition is evaluated with "you" = the creature's controller and
 * "opponent" = the attacking player.
 *
 * @property condition The condition that must be met for the creature to block
 * @property filter What this ability applies to
 */
@SerialName("CantBlockUnless")
@Serializable
data class CantBlockUnless(
    val condition: Condition,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't block unless ${condition.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature can't block unless another creature being declared as a blocker in the same
 * declaration matches [coBlockerFilter]. The blocking sibling of [CantAttackUnlessCoAttacker];
 * together they model "can't attack or block alone" (Toby's Beast token — pass
 * [com.wingedsheep.sdk.scripting.GameObjectFilter.Creature] for the bare "alone" form, where any
 * other declared blocker satisfies it).
 *
 * Like [CantAttackUnlessCoAttacker], this restriction depends on the set of co-blockers rather
 * than on the attacking player, so it is checked against the full proposed blocker group at
 * declaration time (CR 509.1b). The co-blocker need not block the same attacker — it only has to
 * be declared as a blocker this combat. The creature itself is never counted as its own co-blocker.
 *
 * @property coBlockerFilter The filter a *different* blocking creature must match.
 * @property filter What this ability applies to.
 */
@SerialName("CantBlockUnlessCoBlocker")
@Serializable
data class CantBlockUnlessCoBlocker(
    val coBlockerFilter: com.wingedsheep.sdk.scripting.GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't block unless ${coBlockerFilter.description} also blocks"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Creatures can't attack you unless their controller pays generic mana for each
 * attacking creature. Used for Ghostly Prison, Propaganda, Windborn Muse, and
 * Domain-scaled variants like Collective Restraint.
 *
 * Only applies when attacking the controller of this permanent (not their planeswalkers).
 * Multiple AttackTax effects from different permanents stack additively.
 *
 * The per-attacker amount is a [DynamicAmount] so it can scale with game state
 * (e.g., [com.wingedsheep.sdk.dsl.DynamicAmounts.domain] for "{X} where X is your
 * domain"). Evaluated with the source permanent's controller as "you".
 *
 * An optional [condition] gates the whole tax on the source permanent's state — evaluated
 * with the source permanent as "you"/source. Used for Archangel of Tithes
 * ("As long as this creature is untapped, …") via [com.wingedsheep.sdk.dsl.Conditions.SourceIsUntapped].
 * When null (the default) the tax is always active (Ghostly Prison, Propaganda, Windborn Muse).
 *
 * @property amountPerAttacker Generic mana to pay per attacking creature.
 * @property condition Optional gate on the source's state; tax is inactive when it fails.
 */
@SerialName("AttackTax")
@Serializable
data class AttackTax(
    val amountPerAttacker: DynamicAmount,
    val condition: Condition? = null,
) : StaticAbility {
    override val description: String = buildString {
        if (condition != null) append("As long as ${condition.description}, ")
        append("creatures can't attack you")
        if (condition != null) append(" or planeswalkers you control")
        append(" unless their controller pays {${amountPerAttacker.description}} for each ")
        append(if (condition != null) "of those creatures" else "creature they control that's attacking you")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newCondition !== condition) copy(condition = newCondition) else this
    }
}

/**
 * This permanent can't attack or block unless its controller pays [amount] generic mana for it
 * (CR 508.1a / 509.1a — the payment is part of the cost of declaring it).
 *
 * The self-scoped counterpart of [AttackTax] and [BlockTax], which tax *other* players' creatures
 * from the side of the board they are aimed at. Here the taxed creature and the taxing permanent
 * are the same object, so there is no filter and no per-attacker multiplier: the amount is what
 * this one creature costs to send.
 *
 * Used for Myr Prototype ("This creature can't attack or block unless you pay {1} for each +1/+1
 * counter on it"), where [amount] counts the source's own counters — the reason the amount is a
 * [DynamicAmount] rather than an `Int`, since the price changes every upkeep.
 *
 * Attack and block are one ability rather than two flags because the printed line is one sentence.
 * A card that taxes only one half wants its own variant, not a boolean here.
 *
 * @property amount Generic mana the controller must pay to declare this creature as an attacker,
 *           and again to declare it as a blocker. Evaluated with this permanent as the source.
 */
@SerialName("CantAttackOrBlockUnlessPay")
@Serializable
data class CantAttackOrBlockUnlessPay(
    val amount: DynamicAmount,
    /**
     * Whether the tax also applies to declaring this creature as a *blocker*. True (the default) is
     * the printed "can't attack or block unless …" of Myr Prototype; set false for the attack-only
     * wording — Brainwash's "Enchanted creature can't attack unless its controller pays {3}".
     *
     * A separate flag rather than a separate ability because the two wordings differ in exactly one
     * clause and share every other rule: same per-creature charge, same payment step, same
     * evaluation source.
     */
    val appliesToBlocking: Boolean = true,
) : StaticAbility {
    override val description: String = buildString {
        append("can't attack")
        if (appliesToBlocking) append(" or block")
        append(" unless you pay {${amount.description}}")
    }
}

/**
 * Creatures can't block unless their controller pays generic mana for each blocking creature.
 * The defending side of Archangel of Tithes' second ability
 * ("creatures can't block unless their controller pays {1} for each of those creatures").
 *
 * Unlike [AttackTax] (a defender-side restriction tied to who is being attacked), this is a
 * *global* restriction: while any permanent with this ability — whose optional [condition]
 * holds — is on the battlefield, every declared blocker is taxed [amountPerBlocker]. Multiple
 * sources stack additively. Mirrors the per-creature block-tax pause already used for
 * [com.wingedsheep.sdk.scripting.effects.GrantAttackBlockTaxPerCreatureTypeEffect].
 *
 * An optional [condition] gates the tax on the source permanent's state, evaluated with the
 * source permanent as "you"/source. Archangel of Tithes uses
 * [com.wingedsheep.sdk.dsl.Conditions.SourceIsAttacking] ("As long as this creature is attacking, …").
 * When null (the default) the tax is always active.
 *
 * @property amountPerBlocker Generic mana to pay per blocking creature.
 * @property condition Optional gate on the source's state; tax is inactive when it fails.
 */
@SerialName("BlockTax")
@Serializable
data class BlockTax(
    val amountPerBlocker: DynamicAmount,
    val condition: Condition? = null,
) : StaticAbility {
    override val description: String = buildString {
        if (condition != null) append("As long as ${condition.description}, ")
        append("creatures can't block unless their controller pays {${amountPerBlocker.description}} for each ")
        append(if (condition != null) "of those creatures" else "creature they control that's blocking")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newCondition !== condition) copy(condition = newCondition) else this
    }
}

/**
 * This creature can attack as though it didn't have defender, as long as a condition is met.
 * "As long as this creature has a counter on it, it can attack as though it didn't have defender."
 *
 * Checked at attack declaration time. The condition is evaluated with "you" = the creature's
 * controller. The filter defaults to the source creature itself.
 *
 * @property condition The condition under which the defender restriction is bypassed
 * @property filter What this ability applies to
 */
@SerialName("CanAttackDespiteDefender")
@Serializable
data class CanAttackDespiteDefender(
    val condition: Condition,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can attack as though it didn't have defender as long as ${condition.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Creatures matching [attackerFilter] can't attack the controller of this permanent — the general
 * defender-side attack restriction (CR 508.1c).
 *
 * The engine checks the *defending* player's battlefield for permanents with this ability and
 * rejects any attacker that matches the filter. The filter is evaluated with this permanent as the
 * predicate source and the defending player as "you", so `youControl()` / chosen-color / chosen-
 * subtype predicates all resolve against the restriction's own side.
 *
 * The filter carries the whole restriction, positive or negative — there is no separate
 * "…without keyword" shape:
 *  - Storm, Windrider — "Creatures with flying can't attack you":
 *    `CantBeAttackedBy(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))`
 *  - Form of the Dragon — "Creatures without flying can't attack you":
 *    `CantBeAttackedBy(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))`
 *  - Teferi's Moat — "Creatures of the chosen color without flying can't attack you":
 *    `CantBeAttackedBy(GameObjectFilter.Creature.sharingChosenColorWithSource().withoutKeyword(FLYING))`
 *
 * @property attackerFilter Which attackers are restricted.
 */
@SerialName("CantBeAttackedBy")
@Serializable
data class CantBeAttackedBy(
    val attackerFilter: com.wingedsheep.sdk.scripting.GameObjectFilter
) : StaticAbility {
    override val description: String = "${pluralAttackerSubject(attackerFilter)} can't attack you"

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = attackerFilter.applyTextReplacement(replacer)
        return if (newFilter !== attackerFilter) copy(attackerFilter = newFilter) else this
    }
}

/**
 * Sentence-subject rendering of an attacker filter: "creature with flying" → "Creatures with
 * flying". This string is user-visible — it is the attack-rejection message and the label of a
 * granted static — and the clause it heads is always plural ("Creatures without flying can't
 * attack you").
 *
 * [GameObjectFilter.description] is a *singular* noun phrase whose qualifiers trail the type word
 * ("creature of the chosen color without flying"), so only the **type** word may take the "s":
 * pluralizing the last word instead would give "creature with flyings". A filter whose description
 * carries no recognizable type noun is left alone rather than mangled.
 */
private fun pluralAttackerSubject(filter: GameObjectFilter): String {
    val words = filter.description.split(" ")
    val typeIndex = words.indexOfFirst { it.lowercase() in PLURALIZABLE_TYPE_NOUNS }
    val plural = if (typeIndex < 0) {
        words
    } else {
        words.mapIndexed { index, word -> if (index == typeIndex) "${word}s" else word }
    }
    return plural.joinToString(" ").replaceFirstChar { it.uppercase() }
}

/** Type nouns a [GameObjectFilter] description can head with, all regular "+s" plurals. */
private val PLURALIZABLE_TYPE_NOUNS = setOf(
    "creature", "permanent", "artifact", "enchantment", "land", "planeswalker", "battle",
    "token", "card", "spell"
)

/**
 * The source permanent can't be chosen as an attack defender while it is attached to another
 * permanent. This is checked only when attackers are declared; becoming attached after attackers
 * have already been declared does not remove the source from combat.
 */
@SerialName("CantBeAttackedWhileAttached")
@Serializable
data object CantBeAttackedWhileAttached : StaticAbility {
    override val description: String = "This permanent can't be attacked while it's attached"
}

/**
 * Global cap on how many creatures may attack in a single combat (Dueling Grounds —
 * "No more than one creature can attack each combat").
 *
 * Unlike per-creature restrictions, this constrains the *total* declared attacker set
 * regardless of controller, so it is enforced as a whole-declaration check rather than a
 * per-attacker [AttackRestrictionRule]. While any permanent with this ability is on the
 * battlefield, an attack declaration with more than [maxAttackers] attackers is illegal.
 */
@SerialName("AttackerCountLimit")
@Serializable
data class AttackerCountLimit(
    val maxAttackers: Int
) : StaticAbility {
    override val description: String =
        "No more than $maxAttackers creature${if (maxAttackers == 1) "" else "s"} can attack each combat"
}

/**
 * Global cap on how many creatures may block in a single combat (Dueling Grounds —
 * "No more than one creature can block each combat").
 *
 * Constrains the *total* declared blocker set regardless of controller, so it is enforced as
 * a whole-declaration check rather than a per-blocker rule. While any permanent with this
 * ability is on the battlefield, a block declaration with more than [maxBlockers] blockers is
 * illegal.
 */
@SerialName("BlockerCountLimit")
@Serializable
data class BlockerCountLimit(
    val maxBlockers: Int
) : StaticAbility {
    override val description: String =
        "No more than $maxBlockers creature${if (maxBlockers == 1) "" else "s"} can block each combat"
}
