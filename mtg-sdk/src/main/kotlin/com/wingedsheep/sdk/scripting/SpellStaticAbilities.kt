package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants flash to spells matching a filter.
 * Used for Quick Sliver: "Any player may cast Sliver spells as though they had flash."
 * Used for Raff Capashen: "You may cast historic spells as though they had flash."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * when determining if a non-instant spell can be cast at instant speed.
 *
 * @property filter The filter that spells must match to gain flash
 * @property controllerOnly If true, only the permanent's controller benefits (default: false = any player)
 */
@SerialName("GrantFlashToSpellType")
@Serializable
data class GrantFlashToSpellType(
    val filter: GameObjectFilter,
    val controllerOnly: Boolean = false
) : StaticAbility {
    override val description: String = if (controllerOnly) {
        "You may cast ${filter.description} spells as though they had flash"
    } else {
        "Any player may cast ${filter.description} spells as though they had flash"
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Spells matching a filter can't be countered.
 * Used for Root Sliver: "Sliver spells can't be countered."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * when a spell would be countered. If the spell matches the filter, the counter
 * attempt fails.
 *
 * @property filter The filter that spells must match to be uncounterable
 */
@SerialName("GrantCantBeCountered")
@Serializable
data class GrantCantBeCountered(
    val filter: GameObjectFilter
) : StaticAbility {
    override val description: String = "${filter.description} spells can't be countered"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants an alternative casting cost for spells cast by this permanent's controller.
 * Used for cards like Jodah, Archmage Eternal: "You may pay {W}{U}{B}{R}{G} rather than pay
 * the mana cost for spells you cast."
 *
 * When a player controls a permanent with this ability, they may choose to pay the
 * alternative cost instead of the spell's normal mana cost. Alternative costs cannot
 * be combined with other alternative costs (e.g., flashback).
 *
 * Per Rule 118.9a, additional costs, cost increases, and cost reductions still apply
 * to the alternative cost.
 *
 * @property cost The alternative mana cost string (e.g., "{W}{U}{B}{R}{G}")
 */
@SerialName("GrantAlternativeCastingCost")
@Serializable
data class GrantAlternativeCastingCost(
    val cost: String
) : StaticAbility {
    override val description: String = "You may pay $cost rather than pay the mana cost for spells you cast"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * You may cast cards exiled with this permanent (linked via LinkedExileComponent).
 * The [filter] restricts which exiled cards can be cast (e.g., GameObjectFilter.Nonland
 * for "you may cast spells from among cards exiled with ~").
 *
 * When this permanent leaves the battlefield, the static ability naturally ceases to apply
 * and the exiled cards can no longer be cast. The cards remain in exile.
 *
 * Used by Rona, Disciple of Gix and similar cards. Dawnhand Dissident uses the
 * [duringYourTurnOnly] timing restriction and the [additionalCost] gate (a distributed
 * counter-removal) to gate its reanimation ability.
 *
 * @property filter Which exiled cards can be cast
 * @property duringYourTurnOnly If true, only legal during the controller's turn
 * @property additionalCost Optional additional cost that must be paid alongside the spell's mana cost
 */
@SerialName("GrantMayCastFromLinkedExile")
@Serializable
data class GrantMayCastFromLinkedExile(
    val filter: GameObjectFilter = GameObjectFilter.Companion.Nonland,
    val duringYourTurnOnly: Boolean = false,
    val additionalCost: AdditionalCost? = null,
    /**
     * When true, restricts the permission to cards owned by the granter's controller.
     * Dawnhand Dissident's "cards you own exiled with this creature" — exiling an
     * opponent's graveyard card is still permitted, but only the exiler can ever
     * cast it, not the granter, so those stripped cards sit in exile permanently.
     */
    val ownedByYou: Boolean = false,
    /**
     * When true, the spell may be cast without paying its mana cost
     * (Maralen, Fae Ascendant). The spell still pays any other mandatory
     * additional costs.
     */
    val withoutPayingManaCost: Boolean = false,
    /**
     * When true, this permission may be used at most once per turn. A successful
     * cast marks the granter with [com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent]
     * (cleared at end of turn).
     */
    val oncePerTurn: Boolean = false,
    /**
     * Optional cap on the cast spell's mana value. The amount is evaluated each
     * time legality is checked, so dynamic counts (e.g. "the number of Elves and
     * Faeries you control") update live as the battlefield changes.
     */
    val maxManaValue: DynamicAmount? = null,
    /**
     * When true, only cards exiled this turn are eligible (Maralen). Checked via
     * [com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent].
     */
    val exiledThisTurnOnly: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        if (duringYourTurnOnly) append("During your turn, y") else append("Y")
        if (oncePerTurn) append("ou may cast a ${filter.description} ")
        else append("ou may cast ${filter.description} ")
        if (maxManaValue != null) append("with mana value less than or equal to ${maxManaValue.description} ")
        if (ownedByYou) append("cards you own ")
        else if (oncePerTurn) append("card ")
        else append("cards ")
        append("exiled with this permanent")
        if (exiledThisTurnOnly) append(" this turn")
        if (withoutPayingManaCost) append(" without paying its mana cost")
        if (additionalCost != null) append(" by ${additionalCost.description.lowercase()} in addition to paying their other costs")
        if (oncePerTurn) append(". This ability may be used only once each turn")
        append(".")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAdditional = additionalCost?.applyTextReplacement(replacer)
        val newMaxManaValue = maxManaValue?.applyTextReplacement(replacer)
        if (newFilter === filter && newAdditional === additionalCost && newMaxManaValue === maxManaValue) return this
        return copy(filter = newFilter, additionalCost = newAdditional, maxManaValue = newMaxManaValue)
    }
}

/**
 * Grants a keyword (typically a cost-modifying one like CONVOKE or DELVE) to spells cast by this
 * permanent's controller that match [spellFilter].
 *
 * Used for cards like Eirdu, Carrier of Dawn: "Creature spells you cast have convoke." The engine
 * consults battlefield permanents with this static ability when computing legal casts and alternative
 * payments, treating the granted keyword as if it were printed on the spell.
 *
 * Scoped to the controller of the source permanent — "you cast" semantics in the oracle text.
 *
 * @property keyword The keyword that matching spells gain while this permanent is in play.
 * @property spellFilter Which spells gain the keyword (defaults to creature spells).
 */
@SerialName("GrantKeywordToOwnSpells")
@Serializable
data class GrantKeywordToOwnSpells(
    val keyword: Keyword,
    val spellFilter: GameObjectFilter = GameObjectFilter.Creature
) : StaticAbility {
    override val description: String =
        "${spellFilter.description.replaceFirstChar { it.uppercase() }} spells you cast have ${keyword.displayName.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * You may cast spells matching [filter] from your graveyard, optionally by paying [lifeCost]
 * life in addition to their other costs. Only during your turn if [duringYourTurnOnly] is true.
 *
 * Normal timing rules still apply — instants any time you have priority, everything else only
 * at sorcery speed. A spell cast this way uses its normal mana cost (plus the optional life cost).
 *
 * Used for:
 *  - Festival of Embers: `MayCastFromGraveyard(InstantOrSorcery, lifeCost = 1, duringYourTurnOnly = true)`
 *  - Yawgmoth's Agenda: `MayCastFromGraveyard(Nonland)` — free, any spell type, any turn.
 *
 * @property filter The filter that spells must match (e.g., instant/sorcery, or any nonland card)
 * @property lifeCost The life cost to pay in addition to other costs (0 = free)
 * @property duringYourTurnOnly If true, only castable during your turn
 */
@SerialName("MayCastFromGraveyard")
@Serializable
data class MayCastFromGraveyard(
    val filter: GameObjectFilter,
    val lifeCost: Int = 0,
    val duringYourTurnOnly: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        if (duringYourTurnOnly) append("During your turn, y") else append("Y")
        append("ou may cast ${filter.description} spells from your graveyard")
        if (lifeCost > 0) append(" by paying $lifeCost life in addition to their other costs")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * The controller of a permanent with this static ability can't cast more than [maxPerTurn]
 * spell(s) each turn. The restriction is per controller — each player is limited only by the
 * permanents they control (Yawgmoth's Agenda: "You can't cast more than one spell each turn.").
 *
 * Spells already cast earlier in the turn count, even if cast before this permanent entered
 * (per the Yawgmoth's Agenda ruling). The engine enforces this by comparing the controller's
 * spells-cast-this-turn tally against [maxPerTurn] during cast-legality checks. Copies of
 * spells and activated/triggered abilities are not "cast" and do not count.
 *
 * @property maxPerTurn The maximum number of spells the controller may cast each turn.
 */
@SerialName("RestrictSpellsCastPerTurn")
@Serializable
data class RestrictSpellsCastPerTurn(
    val maxPerTurn: Int = 1
) : StaticAbility {
    override val description: String =
        "You can't cast more than $maxPerTurn spell${if (maxPerTurn == 1) "" else "s"} each turn"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Players can't cast a spell that shares a color with the spell most recently cast this turn.
 * Used by Mana Maze (Invasion).
 *
 * This is a global continuous restriction — it applies to every player, not just the source's
 * controller. The engine tracks the colors of the most recently cast spell on
 * [com.wingedsheep.engine.state.GameState.lastCastSpellColors] (cleared at the start of each turn)
 * and, while any permanent on the battlefield has this static ability, refuses to cast a spell
 * whose colors overlap that set. A colorless spell shares no color, so it is always castable and
 * casting one (an empty color set) lifts the restriction until the next colored spell is cast.
 */
@SerialName("CantCastSpellsSharingColorWithLastCast")
@Serializable
data object CantCastSpellsSharingColorWithLastCast : StaticAbility {
    override val description: String =
        "Players can't cast a spell that shares a color with the spell most recently cast this turn"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * A continuous prohibition on casting spells, scoped along three independent axes — each a
 * reused SDK primitive, so one type covers a whole family of "can't cast" cards:
 *
 *  - **who** — [affected], a [Player] reference interpreted *relative to this permanent's
 *    controller*: `EachOpponent` / `Opponent` (your opponents), `You` (the controller), `Each`
 *    (everyone).
 *  - **which** — [spellFilter], matched against the card being cast (card predicates work in any
 *    zone). Defaults to every spell.
 *  - **when** — [condition], evaluated in the controller's context, so `IsYourTurn` reads as
 *    "during your turn" and `IsNotYourTurn` as "during an opponent's turn". `null` = always.
 *
 * Read at cast-legality time (never on the stack), so it composes with every casting path —
 * hand, graveyard flashback/harmonize, exile, top of library. Control is read from projected
 * state, so a control-changing effect flips who is restricted.
 *
 *  - Voice of Victory: `PlayersCantCastSpells(Player.EachOpponent, condition = IsYourTurn)`
 *    ("Your opponents can't cast spells during your turn.")
 *  - Grand Abolisher's cast clause: `PlayersCantCastSpells(Player.EachOpponent)` (every turn).
 *  - Void Winnower: `PlayersCantCastSpells(Player.EachOpponent, spellFilter =
 *    GameObjectFilter(cardPredicates = listOf(CardPredicate.ManaValueIsEven)))`.
 *
 * @property affected Who is forbidden, relative to the source's controller.
 * @property spellFilter Which spells are forbidden (matched against the card being cast).
 * @property condition Optional timing/state gate, evaluated in the controller's context; null = always.
 */
@SerialName("PlayersCantCastSpells")
@Serializable
data class PlayersCantCastSpells(
    val affected: Player = Player.EachOpponent,
    val spellFilter: GameObjectFilter = GameObjectFilter.Any,
    val condition: Condition? = null
) : StaticAbility {
    override val description: String = buildString {
        when (affected) {
            is Player.You -> append("You can't cast ")
            is Player.Opponent, is Player.EachOpponent -> append("Your opponents can't cast ")
            else -> append("${affected.description.replaceFirstChar { it.uppercase() }} can't cast ")
        }
        append(if (spellFilter == GameObjectFilter.Any) "spells" else "${spellFilter.description} spells")
        when (condition) {
            is IsYourTurn -> append(" during your turn")
            is IsNotYourTurn -> append(" during your opponents' turns")
            null -> {}
            else -> append(" ${condition.description}")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newFilter === spellFilter && newCondition === condition) this
        else copy(spellFilter = newFilter, condition = newCondition)
    }
}
