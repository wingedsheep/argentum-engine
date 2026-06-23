package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Player Effects
// =============================================================================

/**
 * Target player skips their combat phases during their next turn.
 * Used for cards like False Peace.
 */
@SerialName("SkipCombatPhases")
@Serializable
data class SkipCombatPhasesEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} skips all combat phases of their next turn"
}

/**
 * Target player's creatures and lands don't untap during their next untap step.
 * Used for cards like Exhaustion.
 */
@SerialName("SkipUntap")
@Serializable
data class SkipUntapEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent),
    val affectsCreatures: Boolean = true,
    val affectsLands: Boolean = true
) : Effect {
    override val description: String = buildString {
        val affectedTypes = listOfNotNull(
            if (affectsCreatures) "Creatures" else null,
            if (affectsLands) "lands" else null
        ).joinToString(" and ")
        append("$affectedTypes ${target.description} controls don't untap during their next untap step")
    }
}

/**
 * Target player skips their next draw step.
 * Used for cards like Elfhame Sanctuary ("you skip your draw step this turn") and
 * Howling Mine / Abundance-style draw-replacement riders.
 *
 * Adds a one-shot marker to the target player that the draw step consumes the next time
 * it would occur (the same turn when applied during that turn's upkeep, or the player's
 * next turn otherwise).
 */
@SerialName("SkipNextDrawStep")
@Serializable
data class SkipNextDrawStepEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You skip your next draw step"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} skips their next draw step"
    }
}

/**
 * You may play additional lands this turn.
 * Used for Summer Bloom: "You may play up to three additional lands this turn."
 *
 * @param count The number of additional lands you may play
 */
@SerialName("PlayAdditionalLands")
@Serializable
data class PlayAdditionalLandsEffect(
    val count: Int
) : Effect {
    override val description: String = "You may play up to $count additional land${if (count != 1) "s" else ""} this turn"
}

/**
 * Add an additional combat phase followed by an additional main phase after the current main phase.
 * Used for Aggravated Assault: "{3}{R}{R}: Untap all creatures you control. After this main phase,
 * there is an additional combat phase followed by an additional main phase."
 */
@SerialName("AddCombatPhase")
@Serializable
data object AddCombatPhaseEffect : Effect {
    override val description: String =
        "After this main phase, there is an additional combat phase followed by an additional main phase"
}

/**
 * Give the controller additional upkeep steps after the current phase
 * (Obeka, Splitter of Seconds: "you get that many additional upkeep steps after this phase").
 *
 * Per CR 500.10, adding an upkeep step after a phase creates the beginning phase that normally
 * contains the upkeep step, with its untap and draw steps skipped. The phases are inserted after
 * the current phase (CR 500.8), and after any additional combat phases added to the same point
 * (CR 500.8: most recently created phase occurs first), so any extra combat happens before the
 * extra upkeeps. "At the beginning of [your] upkeep" abilities trigger in each (CR 503.1a).
 *
 * Always added to the controller of the effect (CR 500.10a — "you get" steps are never added to
 * another player's turn).
 *
 * @param amount The number of additional upkeep steps to add (e.g. the combat damage dealt).
 */
@SerialName("AddAdditionalUpkeepSteps")
@Serializable
data class AddAdditionalUpkeepStepsEffect(
    val amount: DynamicAmount
) : Effect {
    override val description: String = "You get ${amount.description} additional upkeep step(s) after this phase"
}

/**
 * Take an extra turn after this one, with a consequence at end of turn.
 * Used for Last Chance: "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game."
 *
 * @param loseAtEndStep If true, you lose the game at the beginning of that turn's end step
 * @param target The player who takes the extra turn. Defaults to the controller.
 */
@SerialName("TakeExtraTurn")
@Serializable
data class TakeExtraTurnEffect(
    val loseAtEndStep: Boolean = false,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = buildString {
        append("Take an extra turn after this one")
        if (loseAtEndStep) {
            append(". At the beginning of that turn's end step, you lose the game")
        }
    }
}

/**
 * Prevent the target player from playing lands for the rest of this turn.
 * Sets the player's remaining land drops to 0.
 * Defaults to the controller (e.g. Rock Jockey); pass a [EffectTarget.PlayerRef]
 * for "target player can't play lands this turn" cards like Turf Wound.
 */
@SerialName("PreventLandPlaysThisTurn")
@Serializable
data class PreventLandPlaysThisTurnEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        is EffectTarget.Controller -> "You can't play lands this turn"
        is EffectTarget.ContextTarget -> "Target player can't play lands this turn"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} can't play lands this turn"
    }
}

/**
 * Create a global triggered ability that is not attached to any specific permanent, lasting for
 * the given [duration].
 *
 * This is the single duration-parametric form: pass [Duration.EndOfTurn] for spells like False
 * Cure ("Until end of turn, whenever..."), [Duration.Permanent] for emblem-style recurring triggers
 * from non-permanent sources (e.g. Dimensional Breach, planeswalker ultimates), or any other
 * [Duration] for temporary triggers like "Until your next turn, whenever...".
 *
 * Prefer the [com.wingedsheep.sdk.dsl.Effects.CreateGlobalTriggeredAbility] facade over
 * constructing this directly.
 *
 * @property ability The triggered ability to create
 * @property duration How long the ability lasts
 * @property descriptionOverride Optional override for the auto-generated description (for emblem display)
 */
@SerialName("CreateGlobalTriggeredAbility")
@Serializable
data class CreateGlobalTriggeredAbilityEffect(
    val ability: TriggeredAbility,
    val duration: Duration = Duration.Permanent,
    val descriptionOverride: String? = null
) : Effect {
    override val description: String = descriptionOverride ?: when (duration) {
        Duration.Permanent -> ability.description
        else -> "${duration.description.replaceFirstChar { it.uppercase() }}, " +
            ability.description.replaceFirstChar { it.lowercase() }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
    }
}

/**
 * Target player skips their next [count] turns.
 * Used for cards like Lethal Vapors ("You skip your next turn." → `count = Fixed(1)`) and Ral
 * Zarek, Guest Lecturer ("Target opponent skips their next X turns." → `count` is a
 * [DynamicAmount.VariableReference] reading a previously-stored coin-flip tally).
 *
 * The count accumulates: applying this twice to the same player stacks the skipped turns
 * (CR 720.4 — each skip is tracked independently). Resolving with `count == 0` is a no-op.
 *
 * @param target The player who skips turns (default: the controller/activating player)
 * @param count How many of their upcoming turns to skip (default: one)
 */
@SerialName("SkipNextTurn")
@Serializable
data class SkipNextTurnEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val count: DynamicAmount = DynamicAmount.Fixed(1)
) : Effect {
    override val description: String = buildString {
        append(target.description.replaceFirstChar { it.uppercase() })
        append(" skips their next ")
        append(if (count == DynamicAmount.Fixed(1)) "turn" else "${count.description} turns")
    }
}

/**
 * The ability's controller controls the target player during that player's next turn.
 *
 * Used for Mindslaver-style effects (e.g., The Dominion Bracelet). PR 1 ships this effect
 * as a no-op that emits a TurnHijackedEvent only — full input/visibility routing is
 * delivered in a follow-up PR.
 */
@SerialName("HijackNextTurn")
@Serializable
data class HijackNextTurnEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)
) : Effect {
    override val description: String =
        "Controller controls ${target.description} during their next turn"
}

/**
 * Target player can't cast spells for the specified duration.
 * Used for cards like Xantid Swarm: "Whenever this creature attacks, defending player can't cast spells this turn."
 *
 * @param target The player who can't cast spells
 * @param duration How long the restriction lasts (default: EndOfTurn)
 */
@SerialName("CantCastSpells")
@Serializable
data class CantCastSpellsEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} can't cast spells ${duration.description}"
}

/**
 * Target player can't play cards from their hand for the specified [duration].
 *
 * Restricts both casting spells and playing lands, but only from the **hand** zone —
 * the player may still play cards from other zones (exile via a may-play permission,
 * graveyard via Muldrotha, etc.). This is the "they can't play cards from their hand"
 * clause of Memory Vessel, which pairs the restriction with an impulse-style grant so a
 * player swaps their hand for the top cards of their library for a turn.
 *
 * Distinct from [CantCastSpellsEffect], which forbids casting from *every* zone but
 * leaves land plays untouched. This effect is hand-scoped and covers lands too.
 *
 * @param target The player who can't play cards from their hand (default: controller).
 * @param duration How long the restriction lasts (default: until your next turn, matching
 *   the impulse window it usually accompanies).
 */
@SerialName("CantPlayCardsFromHand")
@Serializable
data class CantPlayCardsFromHandEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.UntilYourNextTurn
) : Effect {
    override val description: String =
        "${target.description.replaceFirstChar { it.uppercase() }} can't play cards from their hand ${duration.description}"
}

/**
 * Target player can't activate planeswalkers' loyalty abilities for the specified duration.
 * Sibling restriction to [CantCastSpellsEffect]; compose the two for cards that forbid both
 * (e.g. Revel in Silence: "Your opponents can't cast spells or activate planeswalkers' loyalty
 * abilities this turn.").
 *
 * @param target The player who can't activate loyalty abilities
 * @param duration How long the restriction lasts (default: EndOfTurn)
 */
@SerialName("CantActivateLoyaltyAbilities")
@Serializable
data class CantActivateLoyaltyAbilitiesEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} can't activate planeswalkers' loyalty abilities ${duration.description}"
}

/**
 * Target player loses the game.
 * Used for cards like Phage the Untouchable: "that player loses the game."
 *
 * @param target The player who loses the game
 * @param message Optional message describing why they lost (shown in game-over screen)
 */
@SerialName("LoseGame")
@Serializable
data class LoseGameEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val message: String? = null
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} loses the game"
}

/**
 * Target player wins the game.
 * Used for cards like Simic Ascendancy and Coalition Victory.
 *
 * Implemented as "all opponents of [target] lose the game" — state-based actions
 * then resolve gameOver with the remaining player as winner.
 *
 * @param target The player who wins (default: controller)
 * @param message Optional message describing the win condition (shown in game-over screen)
 */
@SerialName("WinGame")
@Serializable
data class WinGameEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val message: String? = null
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} wins the game"
}

/**
 * Grant shroud to a target entity for the specified duration.
 * Works for players, creatures, and planeswalkers.
 *
 * Used for cards like Gilded Light: "You gain shroud until end of turn."
 *
 * - For player targets: adds PlayerShroudComponent with appropriate removal timing
 * - For permanent targets: creates a floating effect granting the Shroud keyword
 *
 * @param target The entity to grant shroud to (player, creature, or planeswalker)
 * @param duration How long the shroud lasts
 */
@SerialName("GrantShroud")
@Serializable
data class GrantShroudEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} gains shroud ${duration.description}"
}

/**
 * Grants hexproof to a target entity for the specified duration.
 * "You gain hexproof until end of turn."
 *
 * Used for cards like Dawn's Truce: "You and permanents you control gain hexproof until end of turn."
 *
 * - For player targets: adds PlayerHexproofComponent with appropriate removal timing
 * - For permanent targets: creates a floating effect granting the Hexproof keyword
 *
 * @param target The entity to grant hexproof to (player, creature, or planeswalker)
 * @param duration How long the hexproof lasts
 */
@SerialName("GrantHexproof")
@Serializable
data class GrantHexproofEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} gains hexproof ${duration.description}"
}

/**
 * Grant permission to cast creature spells from your graveyard by paying the forage
 * additional cost. Creatures cast this way enter with a finality counter.
 *
 * Used for Osteomancer Adept's activated ability.
 *
 * @param duration How long the permission lasts
 */
@SerialName("GrantCastCreaturesFromGraveyardWithForage")
@Serializable
data class GrantCastCreaturesFromGraveyardWithForageEffect(
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "Until end of turn, you may cast creature spells from your graveyard by foraging in addition to paying their other costs. If you cast a spell this way, that creature enters with a finality counter on it"
}

/**
 * Grant a flat damage bonus to a player's sources for the specified duration.
 * When a source matching the filter that the player controls would deal damage,
 * it deals that much damage plus the bonus amount instead.
 *
 * Used for cards like The Flame of Keld (Chapter III): "If a red source you control
 * would deal damage to a permanent or player this turn, it deals that much damage plus 2 instead."
 *
 * @param bonusAmount The flat damage bonus to add
 * @param sourceFilter Filter for which sources get the bonus (e.g., SourceFilter.HasColor(Color.RED))
 * @param target The player who gets the damage bonus (default: controller)
 * @param duration How long the bonus lasts (default: EndOfTurn)
 */
@SerialName("GrantDamageBonus")
@Serializable
data class GrantDamageBonusEffect(
    val bonusAmount: Int,
    val sourceFilter: SourceFilter = SourceFilter.Any,
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("If ${sourceFilter.description} ${target.description} controls would deal damage to a permanent or player")
        append(", it deals that much damage plus $bonusAmount instead")
    }
}

// =============================================================================
// Gift Effects
// =============================================================================

/**
 * Signals that a gift was given (Bloomburrow gift mechanic).
 * Emits a GiftGivenEvent so that cards like Jolly Gerbils can trigger.
 * This effect has no game state change — it only produces the event.
 */
@SerialName("GiftGiven")
@Serializable
data object GiftGivenEffect : Effect {
    override val description: String = "Give a gift"
}

/**
 * Grant a keyword to spells of a certain type that the controller casts.
 * Used for emblems like Ral's "Instant and sorcery spells you cast have storm."
 *
 * @property keyword The keyword to grant (e.g., STORM)
 * @property spellFilter Which spell types get the keyword (e.g., INSTANT_OR_SORCERY)
 */
@SerialName("GrantSpellKeyword")
@Serializable
data class GrantSpellKeywordEffect(
    val keyword: Keyword,
    val spellFilter: GameObjectFilter
) : Effect {
    override val description: String =
        "${spellFilter.description} spells you cast have ${keyword.displayName.lowercase()}"
}

/**
 * [target] may cast spells matching [spellFilter] as though they had flash, for [duration].
 *
 * Player-scoped, duration-bounded counterpart to the permanent-static
 * [com.wingedsheep.sdk.scripting.GrantFlashToSpellType]: the static lives on a battlefield
 * permanent and applies as long as the source is on the battlefield, while this Effect is a
 * resolution-time one-shot that records a turn-scoped grant on the target player and survives
 * the source (sorcery/instant) leaving the stack.
 *
 * Used for Borne Upon a Wind: "You may cast spells this turn as though they had flash."
 * Composes for narrower-filter variants like "you may cast sorcery spells as though they had
 * flash until end of turn" by passing `GameObjectFilter.Sorcery`.
 *
 * Per CR 702.8a flash means "you may play this card any time you could cast an instant." The
 * permission is read at cast-legality time (CR 601.3) by `CastPermissionUtils.hasGrantedFlash`,
 * so it composes with every casting path that already honors flash.
 *
 * @property target Whose spells gain flash (default: controller).
 * @property spellFilter Which spells gain flash (defaults to every spell).
 * @property duration How long the grant lasts (default: end of turn). The "as long as ~ is on
 *   the battlefield" variant is covered by [com.wingedsheep.sdk.scripting.GrantFlashToSpellType]
 *   instead, so this effect's typical durations are `EndOfTurn`, `UntilYourNextTurn`, etc.
 */
@SerialName("GrantFlashToSpells")
@Serializable
data class GrantFlashToSpellsEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val spellFilter: GameObjectFilter = GameObjectFilter.Any,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append(target.description.replaceFirstChar { it.uppercase() })
        append(" may cast ")
        append(if (spellFilter == GameObjectFilter.Any) "spells" else "${spellFilter.description} spells")
        if (duration != Duration.Permanent) append(" ${duration.description}")
        append(" as though they had flash")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * Spells matching [spellFilter] that [target] casts can't be countered until [duration].
 *
 * Player-scoped counterpart to the permanent-static [com.wingedsheep.sdk.scripting.GrantCantBeCountered]:
 * grants the same protection but with a duration and a controller scope, so it can be
 * triggered by activated abilities like Domri, Anarch of Bolas's +1
 * ("Creature spells you cast this turn can't be countered.").
 *
 * @property target Whose spells gain the protection (default: controller).
 * @property spellFilter Which spell types are protected (defaults to creature spells).
 * @property duration How long the protection lasts (default: end of turn).
 */
@SerialName("GrantSpellsCantBeCountered")
@Serializable
data class GrantSpellsCantBeCounteredEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val spellFilter: GameObjectFilter = GameObjectFilter.Creature,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String =
        "${spellFilter.description} spells ${target.description} cast${if (duration == Duration.Permanent) "" else " ${duration.description}"} can't be countered"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * Create a permanent emblem that grants a static modification to all permanents matching a filter.
 * Used for planeswalker -X abilities that produce a static-ability emblem
 * (e.g., Oko's "Creatures you control of the chosen type get +3/+3 and have vigilance and hexproof").
 *
 * The filter may use [GroupFilter.chosenSubtypeKey] to bind the affected creatures to a creature type
 * chosen earlier in a pipeline (via [ChooseCreatureTypeEffect]). When the executor resolves, it
 * captures the chosen type so the emblem can re-evaluate the filter against future battlefield state.
 *
 * Composes with `Effects.Composite(ChooseCreatureTypeEffect, CreatePermanentEmblem(...))`.
 *
 * @property groupFilter Which permanents the emblem affects.
 * @property powerBonus Power modification applied to each affected creature.
 * @property toughnessBonus Toughness modification applied to each affected creature.
 * @property grantedKeywords Keywords granted to each affected creature.
 * @property emblemDescription Human-readable description of the emblem (without the "You get an emblem with" prefix).
 */
@SerialName("CreatePermanentEmblem")
@Serializable
data class CreatePermanentEmblemEffect(
    val groupFilter: GroupFilter,
    val powerBonus: Int = 0,
    val toughnessBonus: Int = 0,
    val grantedKeywords: List<String> = emptyList(),
    val emblemDescription: String
) : Effect {
    override val description: String = "You get an emblem with \"$emblemDescription\""
}

/**
 * Grants the city's blessing to a player (CR 702.131 / 700.5).
 *
 * Once a player has the city's blessing, they have it for the rest of the game and
 * it can never be removed. Applying this effect to a player who already has the
 * blessing is a no-op.
 *
 * Used as the post-resolution effect of Ascend triggers (typically gated by
 * [com.wingedsheep.sdk.scripting.conditions.Compare] "you control 10+ permanents").
 *
 * @param target The player to grant the city's blessing to. Defaults to the
 *   ability's controller, matching Ascend's "you get the city's blessing" wording.
 */
@SerialName("GainCitysBlessing")
@Serializable
data class GainCitysBlessingEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} gets the city's blessing"
}

/**
 * Removes the target player's maximum hand size for the rest of the game
 * ("you have no maximum hand size for the rest of the game").
 *
 * Unlike [com.wingedsheep.sdk.scripting.NoMaximumHandSize] — a permanent's *static* ability
 * that only applies while that permanent is on the battlefield (Reliquary Tower, Thought Vessel)
 * — this is a one-shot resolution effect that confers a player-scoped, permanent property. It
 * survives the source leaving any zone (e.g. Wisdom of Ages exiles itself on resolution), so it
 * must live on the player, not on a permanent. Applying it to a player who already has no maximum
 * hand size is a no-op.
 *
 * @param target The player who loses their maximum hand size. Defaults to the ability's
 *   controller, matching "you have no maximum hand size for the rest of the game".
 */
@SerialName("RemoveMaximumHandSize")
@Serializable
data class RemoveMaximumHandSizeEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String =
        "${target.description.replaceFirstChar { it.uppercase() }} has no maximum hand size for the rest of the game"
}

/**
 * Lock [target] player's life gain — they can't gain life for [duration].
 *
 * Unlike the [com.wingedsheep.sdk.scripting.PreventLifeGain] replacement effect (a static that
 * ends when its source permanent leaves the battlefield), this is a one-shot effect that tags the
 * player directly, so the lock is independent of the source. With the default [Duration.Permanent]
 * it lasts for the rest of the game (Screaming Nemesis: "If a player is dealt damage this way,
 * they can't gain life for the rest of the game"); [Duration.EndOfTurn] / [Duration.UntilYourNextTurn]
 * are also honored for turn-scoped variants.
 *
 * Non-player targets are a no-op — only players have a life total to lock — so this composes
 * safely with a "deal damage to any target" rider that may hit a creature or planeswalker.
 *
 * @param target The player whose life gain is locked.
 * @param duration How long the lock lasts (default: rest of game).
 */
@SerialName("LockLifeGain")
@Serializable
data class LockLifeGainEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer),
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = buildString {
        append(target.description.replaceFirstChar { it.uppercase() })
        append(" can't gain life")
        when (duration) {
            Duration.Permanent -> append(" for the rest of the game")
            Duration.EndOfTurn -> append(" this turn")
            Duration.UntilYourNextTurn -> append(" until your next turn")
            else -> {}
        }
    }
}

/**
 * "The Ring tempts you" (CR 701.54). The target player gets an emblem named The Ring (if they
 * don't have one) and chooses a creature they control to become their Ring-bearer. The emblem's
 * four cumulative abilities are gated by how many times that player has been tempted.
 *
 * @param target The tempted player. Defaults to the ability's controller ("the Ring tempts you").
 */
@SerialName("TheRingTemptsYou")
@Serializable
data class TheRingTemptsYouEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "the Ring tempts ${target.description}"
}

/**
 * "Choose a number, then [effect]." The controller is prompted for an integer in
 * [[minValue], [maxValue]]; the chosen number is stamped onto the effect context (as the
 * X value) and [then] is executed once. Atomic effects and filters read the chosen number
 * via [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueEqualsX] so the same
 * combinator works for any "choose a number, act on objects with that mana value" card (Void).
 *
 * Compose [then] with [CompositeEffect] when several effects key off the chosen number.
 */
@SerialName("ChooseNumberThen")
@Serializable
data class ChooseNumberThenEffect(
    val then: Effect,
    val minValue: Int = 0,
    val maxValue: Int = 16,
    val prompt: String = "Choose a number"
) : Effect {
    override val description: String = "Choose a number. ${then.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newThen = then.applyTextReplacement(replacer)
        return if (newThen !== then) copy(then = newThen) else this
    }
}

/**
 * "Choose a number between [minValue] and [maxValue]" and store it **durably on the source
 * permanent** in its cast-choices bag under [slot], replacing any prior value for that slot.
 *
 * Unlike [ChooseNumberThenEffect] (which stamps the number transiently into the resolution
 * context as the X value and runs an inner effect once), this records the number on the
 * permanent so a continuous characteristic-defining ability can read it for the rest of the
 * permanent's life via [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice]. The
 * **last** chosen value is what the CDA reads, matching "the last chosen number" wording.
 *
 * This is the *on-resolution* form, run from a triggered/activated ability (e.g. an upkeep
 * re-choice). For the *as-enters* "As ~ enters, choose a number" choice, prefer the replacement
 * effect [com.wingedsheep.sdk.scripting.EntersWithChoice]`(ChoiceType.NUMBER, minValue, maxValue)`
 * — it writes the same [com.wingedsheep.sdk.scripting.ChoiceSlot.CHOSEN_NUMBER] slot *before* the
 * permanent is on the battlefield (CR 614.1c), so the CDA never reads a default while the permanent
 * briefly sits at its printed P/T. Wrapping this effect in an
 * [com.wingedsheep.sdk.scripting.OnEnterRunEffect] also works but runs *after* placement, so avoid
 * it when the entry choice feeds a P/T-defining CDA.
 *
 * Shapeshifter: "As this enters and at the beginning of your upkeep, choose a number between 0
 * and 7. Its power is the last chosen number and its toughness is 7 minus that number." The P/T
 * is a `SetBasePowerToughnessDynamicStatic(CastChoice(CHOSEN_NUMBER), Subtract(7, CastChoice(...)))`.
 *
 * For an optional "you **may** choose a number" clause, mark the *triggered ability* that runs
 * this effect `optional = true` (declining the trigger keeps the prior value) rather than baking
 * a decline path into this effect — the choice itself is always a real number when reached.
 *
 * @property minValue Lowest number the controller may choose (inclusive).
 * @property maxValue Highest number the controller may choose (inclusive).
 * @property slot Which durable cast-choices slot to write (default
 *   [com.wingedsheep.sdk.scripting.ChoiceSlot.CHOSEN_NUMBER]).
 * @property prompt Player-facing prompt text.
 */
@SerialName("ChooseNumberForSource")
@Serializable
data class ChooseNumberForSourceEffect(
    val minValue: Int = 0,
    val maxValue: Int = 7,
    val slot: com.wingedsheep.sdk.scripting.ChoiceSlot = com.wingedsheep.sdk.scripting.ChoiceSlot.CHOSEN_NUMBER,
    val prompt: String = "Choose a number"
) : Effect {
    override val description: String = "choose a number between $minValue and $maxValue"
}
