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
 * Insert a single additional combat phase into the current turn — and *only* a combat phase, with
 * no trailing main phase (Aurelia, the Warleader / Combat Celebrant / Fear of Missing Out:
 * "After this phase, there is an additional combat phase").
 *
 * This is the atomic "extra combat phase" piece. The Aggravated-Assault shape ("an additional combat
 * phase followed by an additional main phase") is a composition of this with [AddMainPhaseEffect]
 * (see `Effects.AddCombatPhase` / `Effects.AddMainPhase`), so cards express exactly what they print
 * instead of always getting the bundled combat+main pair.
 *
 * Per CR 500.8 extra phases are added after the specified phase; the engine inserts the queued
 * phase(s) after the postcombat main phase.
 *
 * [attackerRestriction] optionally constrains *which creatures may be declared as attackers during
 * that inserted combat phase* (CR 508.1c — the active player checks each creature for attacking
 * restrictions, and an illegal one voids the declaration). When non-null, only creatures matching
 * the filter can attack in the added phase
 * (Bumi, Unleashed: "Only land creatures can attack during that combat phase" ⇒
 * `GameObjectFilter.Creature and GameObjectFilter.Land`). The restriction is scoped to the added
 * phase alone — the natural combat phase and any other added phase are unaffected. `null` (the
 * default, and every prior caller) means the ordinary "any creature can attack" phase.
 */
@SerialName("AddCombatPhase")
@Serializable
data class AddCombatPhaseEffect(
    val attackerRestriction: GameObjectFilter? = null
) : Effect {
    override val description: String = buildString {
        append("After this phase, there is an additional combat phase")
        if (attackerRestriction != null) {
            append(". Only ${attackerRestriction.description} can attack during that combat phase")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val replaced = attackerRestriction?.applyTextReplacement(replacer)
        return if (replaced !== attackerRestriction) copy(attackerRestriction = replaced) else this
    }
}

/**
 * Insert a single additional (postcombat) main phase into the current turn. The atomic counterpart
 * to [AddCombatPhaseEffect]; compose the two to reproduce "an additional combat phase followed by an
 * additional main phase" (Aggravated Assault, All-Out Assault). A standalone extra main phase
 * (CR 505.1a — every main phase after the first is a postcombat main phase) is also expressible.
 *
 * Per CR 500.8 the phase is added after the specified phase; the engine inserts it after the
 * postcombat main phase, in the order the queueing effects resolved.
 */
@SerialName("AddMainPhase")
@Serializable
data object AddMainPhaseEffect : Effect {
    override val description: String =
        "There is an additional main phase after this phase"
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
 * Insert additional end step(s) directly after the current end step (Y'shtola Rhul:
 * "there is an additional end step after this step").
 *
 * Per CR 500.9, an effect that adds a step inserts it directly after the specified step; if several
 * are created after the same step, the most recently created occurs first. The extra end step is a
 * full end step (CR 513) — the active player gets priority and every "at the beginning of the end
 * step" ability triggers again (CR 513.1a) — followed eventually by the single cleanup step.
 *
 * The steps are always added to the controller's own turn (CR 500.10a — "there is an additional end
 * step" riders only ever trigger on the controller's end step). Cards that must not loop guard the
 * effect behind [com.wingedsheep.sdk.scripting.conditions.IsFirstEndStepOfTurn] so only the first
 * end step spawns the extra one.
 *
 * @param amount The number of additional end steps to insert (usually a single one).
 */
@SerialName("AddAdditionalEndSteps")
@Serializable
data class AddAdditionalEndStepsEffect(
    val amount: DynamicAmount
) : Effect {
    override val description: String = "There ${if (amount.description == "1") "is" else "are"} ${amount.description} additional end step(s) after this step"
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
 * End the turn (CR 720). Used for Time Stop, Sundial of the Infinite, Discontinuity, and
 * Final Fantasy's Ultima ("Destroy all artifacts and creatures. End the turn.").
 *
 * When this resolves, in order (CR 720.1):
 *  - every spell and ability on the stack is exiled, **including the source of this effect**;
 *  - triggered abilities that would have gone on the stack from the events so far (e.g. the dies
 *    triggers from a preceding board wipe) are discarded, never put on the stack (CR 720.1c);
 *  - creatures and players are removed from combat;
 *  - the game skips straight to the cleanup step — the active player discards down to their
 *    maximum hand size, marked damage wears off, and "this turn" / "until end of turn" effects end;
 *  - then the turn ends normally and the next turn begins.
 *
 * This is a turn-structure effect: the executor only records the request (see the engine's
 * `EndTheTurnRequestedComponent`); the actual end-the-turn sequence runs after the current
 * resolution completes so it can exile the rest of the stack and suppress the pending triggers.
 * It takes no target — it always ends the current turn regardless of who controls the effect.
 */
@SerialName("EndTheTurn")
@Serializable
data object EndTheTurnEffect : Effect {
    override val description: String = "End the turn"
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
 * The ability's controller controls the target player during a scoped future window —
 * either that player's next **turn** ([HijackScope.NextTurn]) or their next **combat
 * phase** ([HijackScope.NextCombatPhase]).
 *
 * Used for Mindslaver-style effects: The Dominion Bracelet ("during their next turn")
 * and Secret of Bloodbending ("during their next combat phase", or their next turn if
 * the waterbend cost was paid). Both windows move only *input authority* to the
 * controller — resource ownership (mana, cards, life, controllership of permanents and
 * spells) stays with the affected player.
 *
 * The [SerialName] is historical ("HijackNextTurn" — the effect predates the combat-phase
 * scope). The wire tag is a stable serialization contract, so it is kept even though the
 * effect now covers more than a whole turn.
 */
@SerialName("HijackNextTurn")
@Serializable
data class HijackNextTurnEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent),
    val scope: HijackScope = HijackScope.NextTurn
) : Effect {
    override val description: String = when (scope) {
        HijackScope.NextTurn ->
            "Controller controls ${target.description} during their next turn"
        HijackScope.NextCombatPhase ->
            "Controller controls ${target.description} during their next combat phase"
    }
}

/**
 * The future window during which a [HijackNextTurnEffect] hands input authority for the
 * affected player to the ability's controller.
 *
 * A scheduled hijack of either scope waits through any skipped turns/combat phases and
 * engages on the next one the affected player actually takes.
 */
@Serializable
enum class HijackScope {
    /** The affected player's whole next turn (The Dominion Bracelet). */
    NextTurn,

    /** The affected player's next combat phase only (Secret of Bloodbending, base mode). */
    NextCombatPhase
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
 * Target player can't cast spells from anywhere other than their **hand** for the specified
 * [duration] — casts from graveyard (flashback, escape, disturb), exile (foretell, plot, a
 * may-play permission), library top, or the command zone all become illegal, while normal
 * hand casts are unaffected.
 *
 * The "your opponents can't cast spells from anywhere other than their hands" clause of
 * Avatar's Wrath. The inverse of [CantPlayCardsFromHandEffect] (which restricts *to* only the
 * hand): this restricts *away from* every zone except the hand. Enforced at the cast-legality
 * chokepoint (CastSpellHandler / the non-hand cast enumerators) — see the engine's
 * `CantCastFromNonHandZonesComponent`.
 *
 * @param target The player restricted to hand-only casting (typically each opponent).
 * @param duration How long the restriction lasts (default: until your next turn, matching
 *   Avatar's Wrath).
 */
@SerialName("CantCastSpellsFromNonHandZones")
@Serializable
data class CantCastSpellsFromNonHandZonesEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.UntilYourNextTurn
) : Effect {
    override val description: String =
        "${target.description.replaceFirstChar { it.uppercase() }} can't cast spells from anywhere " +
            "other than their hand ${duration.description}"
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
 * Grant a protection-style evasion keyword (shroud or hexproof) to a target entity for
 * the specified duration. Works for players, creatures, and planeswalkers.
 *
 * This is the player-aware counterpart to [GrantKeywordEffect]: a player can't carry a
 * keyword via the normal projection layer, so player targets get the matching player
 * protection component instead, while permanent targets get a Layer-6 floating keyword
 * (identical to what `GrantKeyword` would produce).
 *
 * Used for cards like Gilded Light ("You gain shroud until end of turn") and Dawn's Truce
 * ("You and permanents you control gain hexproof until end of turn"). Reach it through the
 * [com.wingedsheep.sdk.dsl.Effects.GrantShroud] / [com.wingedsheep.sdk.dsl.Effects.GrantHexproof]
 * facades rather than constructing it directly.
 *
 * @param keyword The protection keyword to grant — [Keyword.SHROUD] or [Keyword.HEXPROOF].
 * @param target The entity to grant the keyword to (player, creature, or planeswalker).
 * @param duration How long the grant lasts.
 */
@SerialName("GrantEvasionKeyword")
@Serializable
data class GrantEvasionKeywordEffect(
    val keyword: Keyword,
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String =
        "${target.description.replaceFirstChar { it.uppercase() }} gains ${keyword.displayName.lowercase()} ${duration.description}"
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
 * Reduces the target player's maximum hand size by [amount] for the rest of the game
 * ("your maximum hand size is reduced by three for the rest of the game" — Inspired Idea).
 *
 * Like [RemoveMaximumHandSizeEffect], this is a one-shot resolution effect that confers a
 * player-scoped, permanent property rather than a battlefield-only static
 * ([com.wingedsheep.sdk.scripting.SetMaximumHandSize]): the reduction survives the source (a
 * sorcery/instant) leaving the stack. [amount] is evaluated once at resolution and locked in —
 * "for the rest of the game" snapshots the number, and repeated applications *accumulate* (two
 * Inspired Ideas reduce your maximum by six). If the player has no maximum hand size (Reliquary
 * Tower / Wisdom of Ages) the reduction has nothing to reduce and is inert until a maximum exists.
 *
 * [amount] is a [DynamicAmount] for parity with [SetMaximumHandSize]; fixed-reduction cards pass
 * [DynamicAmount.Fixed].
 *
 * @param target The player whose maximum hand size is reduced. Defaults to the ability's
 *   controller, matching "your maximum hand size is reduced …".
 * @param amount How much to reduce the maximum hand size by (clamped to ≥ 0 by the engine).
 */
@SerialName("ReduceMaximumHandSize")
@Serializable
data class ReduceMaximumHandSizeEffect(
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String =
        "${target.description.replaceFirstChar { it.uppercase() }} maximum hand size is reduced by ${amount.description} for the rest of the game"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
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

/**
 * The controller chooses an opponent; the choice is written durably onto the source entity's
 * cast-choices bag under [com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT], where downstream
 * effects read it back through [com.wingedsheep.sdk.scripting.references.Player.ChosenOpponent].
 *
 * With a single opponent the choice is forced and resolves without a prompt, so two-player
 * games see no extra decision. The source may be a spell on the stack (gift instants and
 * sorceries — the choice lives on the spell entity for the rest of its resolution) or a
 * permanent (gift ETB triggers — the choice is recorded durably on the permanent).
 *
 * Used by the gift mechanic ([com.wingedsheep.sdk.dsl.MechanicPatterns.giftSpell]): "promise
 * an opponent a gift" — the promising player picks which opponent receives it.
 */
@SerialName("ChooseOpponentForSource")
@Serializable
data class ChooseOpponentForSourceEffect(
    val prompt: String = "Choose an opponent"
) : Effect {
    override val description: String = "choose an opponent"
}
