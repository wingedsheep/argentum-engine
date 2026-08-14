package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This creature can't be blocked.
 * Used for cards with unconditional unblockability or conditional via ConditionalStaticAbility.
 */
@SerialName("CantBeBlocked")
@Serializable
data class CantBeBlocked(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "This creature can't be blocked."
}

/**
 * This creature can't be blocked by creatures matching the filter.
 * Unified type replacing CantBeBlockedByColor, CantBeBlockedByPower,
 * CantBeBlockedByPowerOrLess, and CantBeBlockedBySubtype.
 *
 * Examples:
 * - Can't be blocked by Walls: `CantBeBlockedBy(GameObjectFilter.Creature.withSubtype("Wall"))`
 * - Can't be blocked by creatures with power 2+: `CantBeBlockedBy(GameObjectFilter.Creature.powerAtLeast(2))`
 * - Can't be blocked by black/red creatures: `CantBeBlockedBy(GameObjectFilter.Creature.withAnyColor(BLACK, RED))`
 *
 * @property blockerFilter Filter describing which creatures cannot block this creature
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedBy")
@Serializable
data class CantBeBlockedBy(
    val blockerFilter: GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked by ${blockerFilter.description}"
}

/**
 * This creature can't be blocked except by creatures matching the filter.
 * Unified type replacing CantBeBlockedExceptByKeyword.
 *
 * Examples:
 * - Can't be blocked except by flyers: `CantBeBlockedExceptBy(GameObjectFilter.Creature.withKeyword(FLYING))`
 *
 * @property blockerFilter Filter describing which creatures CAN block this creature
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedExceptBy")
@Serializable
data class CantBeBlockedExceptBy(
    val blockerFilter: GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked except by ${blockerFilter.description}"
}

/**
 * This creature can't be blocked except by [minBlockers] or more creatures — a generalization of
 * menace (which is the [minBlockers] = 2 case). It may still be left unblocked; the restriction only
 * applies once at least one creature blocks it. Enforced at block declaration, like menace.
 *
 * Example: Troll of Khazad-dûm — `CantBeBlockedByFewerThan(3)` ("can't be blocked except by three or
 * more creatures").
 *
 * @property minBlockers The minimum number of creatures required to block (if blocked at all).
 * @property filter What this ability applies to (default: the source creature).
 */
@SerialName("CantBeBlockedByFewerThan")
@Serializable
data class CantBeBlockedByFewerThan(
    val minBlockers: Int,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked except by $minBlockers or more creatures"
}

/**
 * This creature can only block creatures matching a filter.
 * Generalized "can block only X creatures" — used for the Spirit token from Realm of Koh
 * ("can't block or be blocked by non-Spirit creatures") and any analogous restriction.
 *
 * Examples:
 * - Spirits only: `CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.withSubtype("Spirit"))`
 * - Power 3 or less: `CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.powerAtMost(3))`
 *
 * Applied via projected state, so it works on tokens as well as registered cards.
 *
 * @property blockerFilter Filter describing which creatures this creature is allowed to block
 * @property filter The group of creatures this restriction applies to (default: source)
 */
@SerialName("CanOnlyBlockCreaturesWith")
@Serializable
data class CanOnlyBlockCreaturesWith(
    val blockerFilter: GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can block only ${blockerFilter.description}"
}


/**
 * This creature can't block creatures with power greater than this creature's power.
 * Used for cards like Spitfire Handler.
 *
 * The comparison uses projected power (accounts for buffs/debuffs).
 *
 * @property filter What this ability applies to (typically SourceCreature)
 */
@SerialName("CantBlockCreaturesWithGreaterPower")
@Serializable
data class CantBlockCreaturesWithGreaterPower(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't block creatures with power greater than this creature's power"
}

/**
 * This creature can't be blocked by creatures with power less than this creature's power.
 * Used for cards like Formation Breaker. The attacker-side dual of
 * [CantBlockCreaturesWithGreaterPower].
 *
 * The comparison uses projected power for both the attacker (source) and each potential
 * blocker, so it accounts for buffs/debuffs that change power.
 *
 * @property filter What this ability applies to (typically [GroupFilter.source])
 */
@SerialName("CantBeBlockedByCreaturesWithLessPower")
@Serializable
data class CantBeBlockedByCreaturesWithLessPower(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked by creatures with power less than this creature's power"
}

/**
 * This creature can't be blocked by more than N creatures.
 * Used for Charging Rhino: "can't be blocked by more than one creature."
 *
 * @property maxBlockers The maximum number of creatures that can block this creature
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedByMoreThan")
@Serializable
data class CantBeBlockedByMoreThan(
    val maxBlockers: Int,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked by more than ${
        if (maxBlockers == 1) "one creature" else "$maxBlockers creatures"
    }"
}

/**
 * Creatures matching [filter] can't be blocked while every stat in [properties] is at most
 * [maxValue] — the "small creatures slip through" evasion, in both the scopes it is printed in:
 *
 * - **Tetsuko Umezawa, Fugitive** — "Creatures you control with power or toughness 1 or less can't
 *   be blocked": `CantBeBlockedWhilePropertyAtMost(1)`, the defaults.
 * - **Stature, Size Shifter** — "Stature can't be blocked if her power is 1 or less":
 *   `CantBeBlockedWhilePropertyAtMost(1, setOf(EntityNumericProperty.Power), GroupFilter.source())`.
 *
 * **Do not spell this as `ConditionalStaticAbility(CantBeBlocked(), <power comparison>)`** — it
 * silently never switches off. A `CantBeBlocked` grant is a Layer 6 ability modification, but power
 * is settled in Layer 7; at the moment the Layer 6 effect is applied the projection has no power for
 * the permanent yet and the condition falls back to its *printed* P/T, so the gate answers "yes"
 * forever however big the creature gets. This ability is instead resolved in a post-layer pass in
 * `StateProjector` that runs after every P/T layer, so it reads final projected stats and is re-asked
 * on each projection — the evasion comes back if the creature shrinks again.
 *
 * @property maxValue The highest value at which the evasion still applies
 * @property properties Which stats to test; the evasion applies when *any* of them is at most
 *   [maxValue] ("power **or** toughness"). Only [EntityNumericProperty.Power] and
 *   [EntityNumericProperty.Toughness] are meaningful — no other property is settled in the layers.
 * @property filter Whose creatures it applies to. Defaults to the ability's controller's creatures
 *   (a lord); use [GroupFilter.source] for a creature that only grants it to itself. [description]
 *   renders those two printed shapes; a third filter would need a wording branch here.
 */
@SerialName("CantBeBlockedWhilePropertyAtMost")
@Serializable
data class CantBeBlockedWhilePropertyAtMost(
    val maxValue: Int,
    val properties: Set<EntityNumericProperty> =
        setOf(EntityNumericProperty.Power, EntityNumericProperty.Toughness),
    val filter: GroupFilter = GroupFilter.AllCreaturesYouControl
) : StaticAbility {
    /** "power", "toughness", or "power or toughness" — always in that printed order. */
    val propertyDescription: String
        get() = listOfNotNull(
            EntityNumericProperty.Power.description.takeIf { EntityNumericProperty.Power in properties },
            EntityNumericProperty.Toughness.description.takeIf { EntityNumericProperty.Toughness in properties }
        ).joinToString(" or ")

    override val description: String =
        if (filter.scope is Scope.Self)
            "This creature can't be blocked if its $propertyDescription is $maxValue or less"
        else
            "Creatures you control with $propertyDescription $maxValue or less can't be blocked"
}

/**
 * This creature can't be blocked if its controller has cast a spell matching
 * the given filter this turn. Used for Relic Runner: "can't be blocked if you've
 * cast a historic spell this turn."
 *
 * The engine tracks spell records per player per turn in
 * `GameState.spellsCastThisTurnByPlayer`. The block evasion rule evaluates the filter
 * against those records.
 *
 * @property spellFilter The filter that cast spells must match to grant unblockability
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedIfCastSpellType")
@Serializable
data class CantBeBlockedIfCastSpellType(
    val spellFilter: GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked if you've cast a ${spellFilter.description} spell this turn"
}

/**
 * This creature can block any number of creatures.
 * Used for Ironfist Crusher and similar cards.
 *
 * @property filter What this ability applies to
 */
@SerialName("CanBlockAnyNumber")
@Serializable
data class CanBlockAnyNumber(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can block any number of creatures"
}

/**
 * Grants "can block an additional N creatures" to a group of creatures.
 * Used for Brave the Sands and similar cards.
 * Cumulative: multiple instances stack (e.g., two Brave the Sands = block 3 creatures).
 *
 * @property count Number of additional creatures that can be blocked (default 1)
 * @property filter The group of creatures that gain the ability
 */
@SerialName("CanBlockAdditionalForCreatureGroup")
@Serializable
data class CanBlockAdditionalForCreatureGroup(
    val count: Int = 1,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} can block an additional $count creature${if (count > 1) "s" else ""} each combat"
}

/**
 * This creature can't be blocked unless defending player controls N or more
 * creatures that share a creature type.
 * Used for Graxiplon: "can't be blocked unless defending player controls
 * three or more creatures that share a creature type."
 *
 * @property minSharedCount The minimum number of creatures sharing a type required to allow blocking
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedUnlessDefenderSharesCreatureType")
@Serializable
data class CantBeBlockedUnlessDefenderSharesCreatureType(
    val minSharedCount: Int,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked unless defending player controls $minSharedCount or more creatures that share a creature type"
}

/**
 * This creature can't be blocked as long as the **defending player** controls at least
 * [minCount] permanents matching [permanentFilter] — the landwalk shape generalized past land
 * types. Neurok Spy: "can't be blocked as long as defending player controls an artifact."
 *
 * Defender-relative, not "any opponent": the count is taken against the player (or the controller
 * of the planeswalker/battle) this creature is attacking, so in a multiplayer pod attacking a
 * player with no artifact leaves the Spy blockable even while another opponent has ten.
 * `Conditions.OpponentControls(...)` is the existential mirror and is *not* a substitute here.
 *
 * The basic landwalk keywords ([com.wingedsheep.sdk.core.Keyword.FORESTWALK] and friends) keep
 * their own keyword-driven fast path in the engine rather than desugaring to this; use this type
 * for the non-land members of the family.
 *
 * @property permanentFilter What the defending player must control for the evasion to switch on.
 * @property minCount How many matching permanents the defending player needs (default 1, "an X").
 * @property filter What this ability applies to (default: the source creature).
 */
@SerialName("CantBeBlockedIfDefenderControls")
@Serializable
data class CantBeBlockedIfDefenderControls(
    val permanentFilter: GameObjectFilter,
    val minCount: Int = 1,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = if (minCount == 1) {
        "can't be blocked as long as defending player controls ${permanentFilter.description}"
    } else {
        "can't be blocked as long as defending player controls $minCount or more ${permanentFilter.description}"
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
