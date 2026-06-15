package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Turn/Phase Conditions
// =============================================================================

/**
 * Condition: "If it's your turn"
 */
@SerialName("IsYourTurn")
@Serializable
data object IsYourTurn : Condition {
    override val description: String = "if it's your turn"
}

/**
 * Condition: "If it's not your turn"
 */
@SerialName("IsNotYourTurn")
@Serializable
data object IsNotYourTurn : Condition {
    override val description: String = "if it's not your turn"
}

/**
 * Condition: "If the current phase matches any of the listed phases"
 * When `yoursOnly = true` (default), also requires that it's the controller's turn —
 * i.e. "your main phase" means it's both your turn AND the main phase.
 * Used for cards like Dose of Dawnglow ("if it isn't your main phase").
 */
@SerialName("IsInPhase")
@Serializable
data class IsInPhase(
    val phases: List<Phase>,
    val yoursOnly: Boolean = true
) : Condition {
    override val description: String = buildString {
        append("if it's ")
        if (yoursOnly) append("your ")
        append(phases.joinToString(" or ") { it.displayName.removeSuffix(" Phase").lowercase() })
        if (phases.any { !it.isMainPhase } || phases.size > 1) append(" phase")
    }
}

/**
 * Condition: "If the current step matches any of the listed steps."
 * When `yoursOnly = true` (default), also requires that it's the controller's turn —
 * i.e. "your end step" means it's both your turn AND the end step.
 *
 * Board-derived (reads `state.step` and the active player), so it evaluates identically at
 * resolution and under projection — making it usable as a [ConditionalStaticAbility] gate.
 * Used for Zurgo, Thunder's Decree ("During your end step, ...").
 */
@SerialName("IsInStep")
@Serializable
data class IsInStep(
    val steps: List<Step>,
    val yoursOnly: Boolean = true
) : Condition {
    override val description: String = buildString {
        append("if it's ")
        if (yoursOnly) append("your ")
        append(steps.joinToString(" or ") { it.displayName.removeSuffix(" Step").lowercase() })
        append(" step")
    }
}

// =============================================================================
// Combat Conditions
// =============================================================================

/**
 * Condition: "If you've been attacked this step"
 * Used for cards like Defiant Stand and Harsh Justice that can only be cast
 * during the declare attackers step if you've been attacked.
 */
@SerialName("YouWereAttackedThisStep")
@Serializable
data object YouWereAttackedThisStep : Condition {
    override val description: String = "if you've been attacked this step"
}

/**
 * Condition: "If [player] attacked with [atLeast] or more creatures matching [filter] this turn".
 * Counts every creature [player] declared as an attacker this turn whose current state
 * (per the projected state) matches the filter.
 *
 * The `Conditions.YouAttackedWithCreaturesThisTurn(...)` DSL helper passes [Player.You],
 * which the engine resolves to the source's controller in both resolution and static-ability
 * (projection) contexts.
 *
 * Used for cards like Deepway Navigator: "as long as you attacked with three or more
 * Merfolk this turn".
 */
@SerialName("PlayerAttackedWithCreaturesThisTurn")
@Serializable
data class PlayerAttackedWithCreaturesThisTurn(
    val player: Player = Player.You,
    val filter: GameObjectFilter,
    val atLeast: Int
) : Condition {
    override val description: String =
        "if ${player.description} attacked with $atLeast or more ${DynamicAmount.pluralize(filter.description)} this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Condition: "If [attacker] attacked [defender] this turn" (CR 508.6) — i.e. [attacker]
 * declared one or more creatures as attackers whose defending player was [defender] (the
 * player itself, or the controller of a planeswalker / protector of a battle the creature
 * attacked). Reads [attacker]'s per-turn attacked-players record.
 *
 * Negate via `Conditions.Not(...)` for "didn't attack you that turn" (Faramir, Prince of
 * Ithilien: "you draw a card if they didn't attack you that turn").
 */
@SerialName("PlayerAttackedPlayerThisTurn")
@Serializable
data class PlayerAttackedPlayerThisTurn(
    val attacker: Player,
    val defender: Player = Player.You
) : Condition {
    override val description: String =
        "if ${attacker.description} attacked ${defender.description} this turn"
}

/**
 * Condition: "If [player] has cast [atLeast] or more spells matching [filter] this turn".
 * Counts [player]'s `CastSpellRecord`s captured at cast time, so every spell
 * cast counts even if it was countered, fizzled, or is still on the stack.
 *
 * The `Conditions.YouCastSpellsThisTurn(...)` DSL helper passes [Player.You], which the
 * engine resolves to the source's controller in both resolution and static-ability
 * (projection) contexts.
 *
 * Used for cards like Brightspear Zealot ("as long as you've cast two or more
 * spells this turn") and Illvoi Infiltrator ("if you've cast two or more spells
 * this turn"). Pass `GameObjectFilter.Any` for the unfiltered "any spell" form.
 *
 * [fromZone] optionally restricts the count to spells cast from that zone. With
 * `fromZone = Zone.HAND` this expresses "you('ve) cast a spell from your hand this turn"
 * (negate it for the Prairie Dog cycle's "you haven't cast a spell from your hand this turn").
 * The zone qualifier is matched independently of [filter], so a face-down (morph) spell cast
 * from hand still counts even though its characteristics are unknown (CR 708.2).
 */
@SerialName("PlayerCastSpellsThisTurn")
@Serializable
data class PlayerCastSpellsThisTurn(
    val player: Player = Player.You,
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val atLeast: Int,
    val fromZone: Zone? = null
) : Condition {
    override val description: String = buildString {
        append("if ${player.description} cast $atLeast or more ")
        if (filter != GameObjectFilter.Any) append("${DynamicAmount.pluralize(filter.description)} ")
        append("spells")
        if (fromZone != null) append(" from ${fromZone.name.lowercase()}")
        append(" this turn")
    }
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Condition: "as long as [player] has drawn [atLeast] or more cards this turn".
 *
 * Backed by the per-player `CardsDrawnThisTurnComponent` (reset for all players at the start of
 * each turn), so it counts every draw this turn regardless of how it happened. Used by Gwaihir the
 * Windlord ("This spell costs {2} less to cast as long as you've drawn two or more cards this
 * turn"). Works in both resolution and cost-reduction (projection) contexts. The
 * `Conditions.YouDrewCardsThisTurn` DSL helper passes [Player.You].
 */
@SerialName("PlayerDrewCardsThisTurn")
@Serializable
data class PlayerDrewCardsThisTurn(
    val player: Player = Player.You,
    val atLeast: Int = 1
) : Condition {
    override val description: String =
        "if ${player.description} drew $atLeast or more cards this turn"
}

/**
 * Condition: "If [player] has committed a crime this turn" (CR Outlaws of Thunder Junction —
 * a player commits a crime as they cast a spell, activate an ability, or put a triggered ability
 * on the stack that targets one or more opponents, permanents/spells/abilities an opponent controls,
 * and/or cards in an opponent's graveyard).
 *
 * Pure turn-scoped tracker: once a crime is committed it stays true for the rest of the turn, even
 * if the crime-committing spell/ability is countered or leaves the stack. Crime detection lives in
 * the engine (`CrimeDetector` at the `CommitCrimeEvent` emit sites); this condition only reads the
 * recorded set. The `Conditions.YouCommittedCrimeThisTurn` DSL helper passes [Player.You], resolved
 * to the source's controller in both resolution and projection (cost-reduction) contexts.
 *
 * Used for cards like Seize the Secrets ("This spell costs {1} less to cast if you've committed a
 * crime this turn").
 */
@SerialName("PlayerCommittedCrimeThisTurn")
@Serializable
data class PlayerCommittedCrimeThisTurn(
    val player: Player = Player.You
) : Condition {
    override val description: String = "if ${player.description} committed a crime this turn"
}

/**
 * Condition: "if this is the first spell you've cast this turn that mana from a Treasure
 * was spent to cast." Used by Rain of Riches.
 *
 * The triggering spell itself must have been paid for with treasure mana, and it must be
 * the only such spell in the controller's `CastSpellRecord` history for the turn.
 */
@SerialName("IsFirstSpellPaidWithTreasureManaCastThisTurn")
@Serializable
data object IsFirstSpellPaidWithTreasureManaCastThisTurn : Condition {
    override val description: String =
        "if it's the first spell you've cast this turn that mana from a Treasure was spent to cast"
}

/**
 * Condition: "If a permanent of [cardType] entered the battlefield under [player]'s control this turn."
 *
 * Pure event tracker — the permanent does not need to still be on the battlefield, still be of
 * that type, or still be under that player's control when the condition is evaluated. Once the
 * type was recorded at entry, it remains true for the rest of the turn.
 *
 * Used for Mechan Shieldmate (EOE): "As long as an artifact entered the battlefield under your
 * control this turn, this creature can attack as though it didn't have defender."
 */
@SerialName("PermanentTypeEnteredBattlefieldThisTurn")
@Serializable
data class PermanentTypeEnteredBattlefieldThisTurn(
    val cardType: CardType,
    val player: Player = Player.You
) : Condition {
    override val description: String =
        "if ${anA(cardType.displayName)} entered the battlefield under ${player.possessive} control this turn"

    private fun anA(word: String): String =
        if (word.firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) "an $word" else "a $word"
}

// =============================================================================
// Ability Resolution Conditions
// =============================================================================

/**
 * Condition: "if this is the Nth time this ability has resolved this turn"
 * Checks the AbilityResolutionCountThisTurnComponent on the source entity.
 * Used for cards like Harvestrite Host.
 */
@SerialName("SourceAbilityResolvedNTimesThisTurn")
@Serializable
data class SourceAbilityResolvedNTimesThisTurn(val count: Int) : Condition {
    override val description: String = "if this is the ${ordinal(count)} time this ability has resolved this turn"

    private fun ordinal(n: Int): String = when (n) {
        1 -> "first"
        2 -> "second"
        3 -> "third"
        else -> "$n${ordinalSuffix(n)}"
    }

    /** English ordinal suffix: 21st, 22nd, 23rd, 24th, with the 11th/12th/13th exceptions. */
    private fun ordinalSuffix(n: Int): String =
        if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
}

// =============================================================================
// Void (Edge of Eternities ability word)
// =============================================================================

/**
 * Condition: "if a nonland permanent left the battlefield this turn or
 * a spell was warped this turn".
 *
 * Backs the Void ability word from Edge of Eternities. The condition is global — it
 * is satisfied by any player's nonland permanent leaving the battlefield (tokens
 * count, lands do not) or any spell that was cast for its warp cost this turn,
 * even if that spell was countered.
 */
@SerialName("Void")
@Serializable
data object VoidCondition : Condition {
    override val description: String =
        "if a nonland permanent left the battlefield this turn or a spell was warped this turn"
}

// =============================================================================
// Stack Conditions
// =============================================================================

/**
 * Condition: "If an opponent has cast a spell (it's on the stack)"
 * Used for Portal counterspells like Mystic Denial that can only be cast
 * in response to an opponent's spell.
 */
@SerialName("OpponentSpellOnStack")
@Serializable
data object OpponentSpellOnStack : Condition {
    override val description: String = "if an opponent has cast a spell"
}

// =============================================================================
// Death Conditions
// =============================================================================

/**
 * Intervening-if condition (Rule 603.4): "if a creature died this turn".
 * True when the controlling player's CreaturesDiedThisTurnComponent has count > 0.
 * Evaluated both at trigger time and at resolution per Rule 603.4.
 */
@SerialName("CreatureDiedThisTurn")
@Serializable
data object CreatureDiedThisTurnCondition : Condition {
    override val description: String = "if a creature died this turn"
}

/**
 * Intervening-if condition (Rule 603.4): "if a creature died under your control this turn".
 * True when the source's controller's CreaturesDiedThisTurnComponent has count > 0.
 * Unlike [CreatureDiedThisTurnCondition] (which counts creatures dying under any player's
 * control), this is scoped to the source's controller. Used by Barrensteppe Siege (Mardu).
 */
@SerialName("ControlledCreatureDiedThisTurn")
@Serializable
data object ControlledCreatureDiedThisTurnCondition : Condition {
    override val description: String = "if a creature died under your control this turn"
}

/**
 * Intervening-if condition: "if a permanent [player] controlled left the battlefield this turn".
 *
 * True when [player]'s `PermanentLeftBattlefieldThisTurnComponent` has count > 0. Counts
 * permanents of every type (creatures, lands, artifacts, enchantments, planeswalkers) and
 * includes tokens — anything that went battlefield → anywhere else this turn. Credited to
 * the *last-known controller* at the moment of departure, so a Threaten-style steal then
 * sacrifice counts for the thief.
 *
 * Broader than [CreatureDiedThisTurnCondition] (creatures only, dying only — not e.g. a
 * blink) and per-player rather than global. Distinct from the Void global
 * `nonlandPermanentLeftBattlefieldThisTurn` tracker: this *includes* lands and is scoped
 * to a single player.
 *
 * The `Conditions.YouHadPermanentLeaveBattlefieldThisTurn` DSL constant passes [Player.You],
 * which the engine resolves to the source's controller in both resolution and static-ability
 * contexts. Used by Shortcut to Mushrooms (LTR): "At the beginning of your end step, if a
 * permanent you controlled left the battlefield this turn, put a +1/+1 counter on target
 * creature you control."
 */
@SerialName("PermanentLeftBattlefieldThisTurn")
@Serializable
data class PermanentLeftBattlefieldThisTurn(
    val player: Player = Player.You
) : Condition {
    override val description: String =
        "if a permanent ${player.description} controlled left the battlefield this turn"
}

// =============================================================================
// Plot (CR 718, Outlaws of Thunder Junction)
// =============================================================================

/**
 * Gate condition for the cast-from-exile permission granted by plot.
 *
 * True when the source card carries a `PlottedComponent` whose `turnPlotted` is
 * strictly less than the current `state.turnNumber` — i.e. the plotted card was
 * plotted on a prior turn. Used as the `MayPlayPermission.condition` so plotted
 * cards cannot be cast on the same turn they were plotted (CR 718.2).
 */
@SerialName("SourcePlottedOnPriorTurn")
@Serializable
data object SourcePlottedOnPriorTurn : Condition {
    override val description: String = "if this card was plotted on a prior turn"
}

// =============================================================================
// City's Blessing (Ixalan, CR 702.131 / 700.5)
// =============================================================================

/**
 * Intervening-if / static condition: "if [player] has the city's blessing".
 *
 * The city's blessing is a permanent player designation (once gained, never lost
 * for the rest of the game per CR 702.131c). Granted by Ascend abilities when the
 * controller controls ten or more permanents on resolution.
 *
 * The `Conditions.YouHaveCitysBlessing` DSL constant passes [Player.You], which the
 * engine resolves to the source's controller in both resolution and static-ability
 * (projection) contexts. Used by spell triggers/effects and by [ConditionalStaticAbility]
 * (e.g. Tendershoot Dryad's "Saprolings you control get +2/+2 as long as you have
 * the city's blessing").
 */
@SerialName("PlayerHasCitysBlessing")
@Serializable
data class PlayerHasCitysBlessing(val player: Player = Player.You) : Condition {
    override val description: String = "if ${player.description} has the city's blessing"
}

/**
 * Intervening-if / resolution condition: "if the Ring has tempted [player] [times] or more times
 * this game" (CR 701.54).
 *
 * Reads the cumulative `temptCount` the engine tracks on the player's The Ring emblem
 * (`TheRingComponent`), which only ever increases as the Ring tempts that player. A player who has
 * never been tempted has no emblem, so the count is treated as 0. The `Conditions.RingHasTemptedYouAtLeast`
 * DSL helper passes [Player.You], resolved to the source's controller. Used by Frodo, Sauron's Bane's
 * granted Rogue ability ("that player loses the game if the Ring has tempted you four or more times
 * this game").
 *
 * @property times The minimum cumulative tempt count required for the condition to hold.
 */
@SerialName("RingHasTemptedPlayerAtLeast")
@Serializable
data class RingHasTemptedPlayerAtLeast(
    val times: Int,
    val player: Player = Player.You
) : Condition {
    override val description: String =
        "if the Ring has tempted ${player.description} $times or more times this game"
}

