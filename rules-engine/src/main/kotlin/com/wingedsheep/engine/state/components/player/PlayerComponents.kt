package com.wingedsheep.engine.state.components.player

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * Mana pool for a player.
 */
@Serializable
data class ManaPoolComponent(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0,
    val restrictedMana: List<RestrictedManaEntry> = emptyList(),
    /**
     * Count of mana units currently in the pool that were added by a permanent
     * with the Treasure subtype (Treasure tokens, etc.). Treasure mana is
     * fungible — `treasureMana` does not track color or position in the pool,
     * only the count. When mana is spent for a spell, [CastPaymentProcessor]
     * consumes from this counter proportional to the total mana taken from the
     * pool and flags the spell as "paid with Treasure mana" if any was
     * consumed. Powers Alchemist's Talent level 3. The counter is cleared
     * whenever the pool empties.
     */
    val treasureMana: Int = 0
) : Component {
    /**
     * Add mana of a specific color.
     */
    fun add(color: Color, amount: Int = 1): ManaPoolComponent = when (color) {
        Color.WHITE -> copy(white = white + amount)
        Color.BLUE -> copy(blue = blue + amount)
        Color.BLACK -> copy(black = black + amount)
        Color.RED -> copy(red = red + amount)
        Color.GREEN -> copy(green = green + amount)
    }

    /**
     * Add colorless mana.
     */
    fun addColorless(amount: Int): ManaPoolComponent =
        copy(colorless = colorless + amount)

    /**
     * Get mana of a specific color.
     */
    fun getAmount(color: Color): Int = when (color) {
        Color.WHITE -> white
        Color.BLUE -> blue
        Color.BLACK -> black
        Color.RED -> red
        Color.GREEN -> green
    }

    /**
     * Remove mana of a specific color.
     */
    fun spend(color: Color, amount: Int = 1): ManaPoolComponent? {
        val current = getAmount(color)
        return if (current >= amount) {
            when (color) {
                Color.WHITE -> copy(white = white - amount)
                Color.BLUE -> copy(blue = blue - amount)
                Color.BLACK -> copy(black = black - amount)
                Color.RED -> copy(red = red - amount)
                Color.GREEN -> copy(green = green - amount)
            }
        } else null
    }

    /**
     * Spend colorless mana.
     */
    fun spendColorless(amount: Int): ManaPoolComponent? =
        if (colorless >= amount) copy(colorless = colorless - amount) else null

    /**
     * Total mana available.
     */
    val total: Int get() = white + blue + black + red + green + colorless + restrictedMana.size

    /**
     * Check if pool is empty. Includes the `treasureMana` counter so a stale
     * tag without backing mana still triggers the end-of-step pool reset.
     */
    val isEmpty: Boolean get() = total == 0 && treasureMana == 0

    /**
     * Add restricted mana to the pool.
     * Each unit of restricted mana is tracked individually with its restriction.
     */
    fun addRestricted(
        color: Color?,
        amount: Int,
        restriction: ManaRestriction,
        riders: Set<ManaSpellRider> = emptySet(),
        expiry: ManaExpiry = ManaExpiry.END_OF_TURN
    ): ManaPoolComponent {
        val entries = (1..amount).map { RestrictedManaEntry(color, restriction, riders, expiry) }
        return copy(restrictedMana = restrictedMana + entries)
    }

    /**
     * Remove every restricted-mana entry whose expiry matches [expiry]. Used to discard
     * combat-duration mana ([ManaExpiry.END_OF_COMBAT], firebending) when combat ends,
     * without disturbing ordinary mana that persists to end of turn.
     */
    fun clearExpired(expiry: ManaExpiry): ManaPoolComponent =
        copy(restrictedMana = restrictedMana.filterNot { it.expiry == expiry })

    /**
     * Empty the mana pool.
     */
    fun empty(): ManaPoolComponent = ManaPoolComponent()

    /**
     * Empty the pool except for mana of [keep] colours, which is retained. Used by the
     * colour-filtered, turn-scoped retention ([RetainUnspentManaComponent], The Last Agni Kai):
     * at end-of-turn cleanup the kept colours' plain counters and any same-colour restricted
     * entries survive, while every other colour, colorless, treasure, and off-colour restricted
     * mana empties as normal. An empty [keep] set is identical to [empty].
     */
    fun emptyExcept(keep: Set<Color>): ManaPoolComponent =
        ManaPoolComponent(
            white = if (Color.WHITE in keep) white else 0,
            blue = if (Color.BLUE in keep) blue else 0,
            black = if (Color.BLACK in keep) black else 0,
            red = if (Color.RED in keep) red else 0,
            green = if (Color.GREEN in keep) green else 0,
            colorless = 0,
            restrictedMana = restrictedMana.filter { it.color != null && it.color in keep },
            treasureMana = 0
        )
}

/**
 * "Until end of turn, you don't lose unspent mana of [colors] as steps and phases end."
 *
 * Turn-scoped, single-player, colour-filtered mana retention conferred directly on a player by
 * [com.wingedsheep.sdk.scripting.effects.RetainUnspentManaEffect] (The Last Agni Kai). The
 * one-shot, source-independent cousin of the permanent-static
 * [com.wingedsheep.sdk.scripting.PreventManaPoolEmptying] (Upwelling): rather than stopping all
 * emptying for everyone while a permanent is in play, it spares only this player's [colors] mana.
 *
 * Read by [com.wingedsheep.engine.core.CleanupPhaseManager] during the end-of-turn mana-emptying
 * step (the engine's only mana-pool-emptying point): the player's pool is emptied via
 * [ManaPoolComponent.emptyExcept] keeping [colors], then the component is removed if its
 * [removeOn] is [PlayerEffectRemoval.EndOfTurn] (the default).
 *
 * @property colors Mana colours kept through this turn's pool emptying.
 * @property removeOn When this component is removed.
 */
@Serializable
data class RetainUnspentManaComponent(
    val colors: Set<Color> = emptySet(),
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * A single unit of mana with a spending restriction.
 * @param color The color of the mana, or null for colorless.
 * @param restriction The restriction on how this mana can be spent.
 * @param riders Side-effects applied to a spell when this mana is spent on it
 *   (e.g. [ManaSpellRider.MakesSpellUncounterable] for Cavern of Souls).
 * @param expiry When this mana leaves the pool. [ManaExpiry.END_OF_TURN] is ordinary mana;
 *   [ManaExpiry.END_OF_COMBAT] is firebending-style mana cleared by `CombatManager.endCombat`.
 */
@Serializable
data class RestrictedManaEntry(
    val color: Color?,
    val restriction: ManaRestriction,
    val riders: Set<ManaSpellRider> = emptySet(),
    val expiry: ManaExpiry = ManaExpiry.END_OF_TURN
)

/**
 * Tracks how many turns this player has taken so far in the game.
 *
 * Per CR 500.1 each turn consists of five phases, and CR 500.11 / 614.10a make a
 * skipped turn "proceed past as though it didn't exist" — so this counter is
 * incremented inside [com.wingedsheep.engine.core.TurnManager.startTurn], which
 * only runs for turns the player actually takes. Skipped turns don't fire it.
 * Initialized to 0 in GameInitializer; the value is 1 once they're partway
 * through their first taken turn.
 *
 * Used by cards like Starting Town: "this land enters tapped unless it's your
 * first, second, or third turn of the game." Eval-side condition is
 * `ControllerTurnsTakenAtMost(3)`.
 */
@Serializable
data class PlayerTurnsTakenComponent(
    val count: Int = 0
) : Component {
    fun increment(): PlayerTurnsTakenComponent = copy(count = count + 1)
}

/**
 * Tracks land drops for the turn.
 */
@Serializable
data class LandDropsComponent(
    val remaining: Int = 1,
    val maxPerTurn: Int = 1
) : Component {
    /**
     * Use a land drop.
     */
    fun use(): LandDropsComponent = copy(remaining = remaining - 1)

    /**
     * Reset for a new turn.
     */
    fun reset(): LandDropsComponent = copy(remaining = maxPerTurn)

    /**
     * Check if a land can be played.
     */
    val canPlayLand: Boolean get() = remaining > 0
}

/**
 * Tracks mulligan state for a player during game setup.
 *
 * Per CR 103.4 (London Mulligan): A player may mulligan any number of times.
 * After taking a mulligan, the player shuffles their hand into their library
 * and draws a new hand of 7 cards. After all players have kept, each player
 * who took mulligans puts that many cards from their hand on the bottom of
 * their library in any order.
 */
@Serializable
data class MulliganStateComponent(
    /** Number of mulligans taken so far */
    val mulligansTaken: Int = 0,
    /** Whether the player has decided to keep their current hand */
    val hasKept: Boolean = false,
    /**
     * Whether the Leyline phase has already been initiated for this player.
     *
     * The leyline phase runs once per player after all mulligans and bottoming complete:
     * the engine populates [pendingLeylineCardIds] with the leyline cards currently in this
     * player's opening hand and walks each one through a yes/no decision. Setting this flag
     * prevents the phase from being re-initiated if the player draws another leyline card
     * into hand later (per CR 103.6, the opening-hand window closes once the first turn begins).
     */
    val leylinePhaseStarted: Boolean = false,
    /**
     * Card entity IDs in this player's opening hand that still need a leyline yes/no decision.
     * Drained as the player resolves each prompt (yes = card moved to battlefield, no = card
     * stays in hand). The leyline phase for this player is "complete" once the list is empty.
     */
    val pendingLeylineCardIds: List<EntityId> = emptyList(),
    /**
     * Whether this player's *first* mulligan is free (CR 800.6). In a multiplayer game (a game
     * that began with more than two players) the first mulligan a player takes doesn't count
     * toward the number of cards they put on the bottom or the number of mulligans they may take;
     * subsequent mulligans count as normal. Set once at game setup from the table size and carried
     * through bottoming — see [GameInitializer]. False for two-player games (London Mulligan as-is).
     */
    val freeMulligan: Boolean = false
) : Component {
    companion object {
        const val STARTING_HAND_SIZE = 7
    }

    /**
     * The number of cards this player must put on the bottom after keeping. With [freeMulligan]
     * (CR 800.6) the first mulligan is discounted, so an N-mulligan keep bottoms N−1 cards.
     */
    val cardsToBottom: Int get() = if (freeMulligan) maxOf(0, mulligansTaken - 1) else mulligansTaken

    /**
     * Check if the player can still mulligan. A player can mulligan until keeping would force
     * them to bottom their whole hand — so the cap is on [cardsToBottom], not the raw mulligan
     * count. With [freeMulligan] the free first mulligan grants one extra mulligan before that cap.
     */
    val canMulligan: Boolean get() = !hasKept && cardsToBottom < STARTING_HAND_SIZE

    /**
     * Record a mulligan taken.
     */
    fun takeMulligan(): MulliganStateComponent = copy(mulligansTaken = mulligansTaken + 1)

    /**
     * Record that the player keeps their hand.
     */
    fun keep(): MulliganStateComponent = copy(hasKept = true)

    /**
     * Whether this player still has leyline cards awaiting a yes/no choice.
     */
    val hasPendingLeylineChoices: Boolean get() = pendingLeylineCardIds.isNotEmpty()
}

/**
 * One queued extra phase (CR 500.8) inserted into the active player's turn after the postcombat
 * main phase. Combat phases and main phases are tracked as distinct kinds so a card can ask for
 * exactly what it prints — "an additional combat phase" (Aurelia / Fear of Missing Out) versus
 * "an additional combat phase followed by an additional main phase" (Aggravated Assault).
 */
@Serializable
enum class ExtraPhaseKind { COMBAT, MAIN }

/**
 * Component tracking the queue of additional phases to be inserted into the current turn, in the
 * order they should occur (CR 500.8). When the postcombat main phase would advance to the end step,
 * if this component exists the game drains the queue one entry at a time — redirecting to a fresh
 * combat phase ([ExtraPhaseKind.COMBAT]) or postcombat main phase ([ExtraPhaseKind.MAIN]).
 *
 * Built atomically: [com.wingedsheep.sdk.scripting.effects.AddCombatPhaseEffect] appends a COMBAT
 * entry, [com.wingedsheep.sdk.scripting.effects.AddMainPhaseEffect] appends a MAIN entry. Composing
 * the two (Aggravated Assault, All-Out Assault) yields `[COMBAT, MAIN]`; the combat atom alone
 * (Great Train Heist, Raph & Leo, Éomer, Fear of Missing Out) yields `[COMBAT]` and adds no main.
 *
 * @param phases Remaining extra phases to insert, head first.
 */
@Serializable
data class AdditionalPhasesComponent(
    val phases: List<ExtraPhaseKind> = emptyList()
) : Component

/**
 * Marker placed on the active player while the game is inside an *inserted* extra combat phase
 * (see [AdditionalPhasesComponent]). It tells the TurnManager that advancing out of that combat
 * phase's end-of-combat step must drain the extra-phase queue rather than fall through into a
 * postcombat main phase — so a combat-only extra phase never grants an unwanted main phase. The
 * trailing main phase is added only when an explicit [ExtraPhaseKind.MAIN] is queued. Consumed when
 * the queue is exhausted (the game proceeds to the end step) or when a queued main phase begins.
 */
@Serializable
data object InAdditionalCombatPhaseComponent : Component

/**
 * Component tracking additional upkeep steps to be inserted into the current turn
 * (Obeka, Splitter of Seconds). Per CR 500.10, adding a step after a phase creates the
 * beginning phase that normally contains that step directly after the specified phase, with
 * its other steps (untap and draw) skipped. Per CR 500.8, these extra phases occur after the
 * current phase, and after any extra combat phases added to the same point (CR 500.8: the most
 * recently created phase occurs first — combat phases are created before the upkeep phases here,
 * so combat happens first).
 *
 * The TurnManager drains this at the postcombat-main → end transition, *after* the
 * additional-combat-phase check, redirecting into a fresh beginning phase whose only step is the
 * upkeep step. The count is decremented each time an additional upkeep step begins.
 *
 * @param count Number of additional upkeep steps (beginning phases) remaining to insert
 */
@Serializable
data class AdditionalUpkeepStepsComponent(
    val count: Int = 1
) : Component

/**
 * Marker placed on the active player while the game is inside an inserted additional upkeep step
 * (see [AdditionalUpkeepStepsComponent]). It tells the TurnManager that advancing out of this
 * upkeep step must skip the draw step and return to the postcombat main phase (CR 500.10), rather
 * than following the normal upkeep → draw progression. Consumed when that redirect happens.
 */
@Serializable
data object InAdditionalUpkeepStepComponent : Component

/**
 * Component tracking additional end steps to be inserted into the current turn (Y'shtola Rhul).
 * Per CR 500.9, adding a step after a step inserts it directly after that step; if several are
 * created after the same step the most recently created occurs first. Each is a full end step
 * (CR 513) — the active player gets priority and "at the beginning of the end step" abilities
 * trigger again.
 *
 * The TurnManager drains this when advancing out of the end step, redirecting back into a fresh
 * end step instead of proceeding to the cleanup step, decrementing the count each time. Always
 * added to the active player's own turn (CR 500.10a).
 *
 * @param count Number of additional end steps remaining to insert
 */
@Serializable
data class AdditionalEndStepsComponent(
    val count: Int = 1
) : Component

/**
 * Marker placed on the active player once at least one [AdditionalEndStepsComponent] end step has
 * begun this turn. It distinguishes an *extra* end step from the turn's first (natural) end step,
 * which is what [com.wingedsheep.sdk.scripting.conditions.IsFirstEndStepOfTurn] reads to keep
 * "there is an additional end step after this step" riders from looping. Cleared at end-of-turn
 * cleanup so the next turn's first end step is "first" again.
 */
@Serializable
data object InAdditionalEndStepComponent : Component

/**
 * Marker component indicating that a player should skip all combat phases
 * during their next turn. Applied by effects like False Peace.
 *
 * This component is consumed (removed) at the start of the combat phase
 * when the turn is skipped.
 */
@Serializable
data object SkipCombatPhasesComponent : Component

/**
 * Component indicating that a player's creatures and/or lands don't untap
 * during their next untap step. Applied by effects like Exhaustion.
 *
 * This component is consumed (removed) after the untap step is processed.
 *
 * @param affectsCreatures If true, creatures controlled by this player don't untap
 * @param affectsLands If true, lands controlled by this player don't untap
 */
@Serializable
data class SkipUntapComponent(
    val affectsCreatures: Boolean = true,
    val affectsLands: Boolean = true
) : Component

/**
 * Marker component indicating that a player should skip their next draw step.
 * Applied by effects like Elfhame Sanctuary ("you skip your draw step this turn").
 *
 * This component is consumed (removed) the next time that player's draw step would
 * occur — the same turn when applied during that turn's upkeep, or the player's next
 * turn otherwise. When consumed, the draw step performs no draw.
 */
@Serializable
data object SkipDrawStepComponent : Component

/**
 * Component marking that a player has lost the game.
 *
 * This is added when a player loses due to various game rules:
 * - 704.5a: Life total 0 or less
 * - 704.5b: 10 or more poison counters
 * - 704.5c: Attempted to draw from empty library
 * - Concession
 *
 * The checkGameEnd SBA uses this to determine when the game ends.
 */
@Serializable
data class PlayerLostComponent(
    val reason: LossReason
) : Component

/**
 * Marks that a player who lost the game has already had the "leaving the game"
 * processing (CR 800.4a–c) applied — their owned objects removed, their stack objects
 * cleared, and control effects involving them ended.
 *
 * A player carries [PlayerLostComponent] the instant they lose, but the leave-the-game
 * cleanup is a separate step run once by [com.wingedsheep.engine.mechanics.sba.player.PlayerLeavesGameCheck].
 * This marker is the idempotency guard so the SBA loop applies that cleanup exactly once.
 */
@Serializable
data object PlayerLeftGameComponent : Component

/**
 * Reason why a player lost the game.
 */
@Serializable
enum class LossReason {
    LIFE_ZERO,
    POISON_COUNTERS,
    EMPTY_LIBRARY,
    CONCESSION,
    CARD_EFFECT,
    /** Commander format: 21+ combat damage from a single commander (CR 903.10a). */
    COMMANDER_DAMAGE,
    /** Two-Headed Giant: this player's team lost, so they lose with it (CR 810.8a). */
    TEAM_DEFEATED,
}

/**
 * Component indicating that a player has shroud.
 * Applied by spells like Gilded Light ("You gain shroud until end of turn").
 *
 * When present on a player entity, that player cannot be the target of
 * spells or abilities.
 *
 * @param removeOn When this component should be removed:
 *   - [PlayerEffectRemoval.EndOfTurn] — removed during end-of-turn cleanup (e.g., Gilded Light)
 *   - [PlayerEffectRemoval.Permanent] — stays until explicitly removed
 */
@Serializable
data class PlayerShroudComponent(
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Marks a player as having hexproof — the player can't be the target of
 * spells or abilities opponents control.
 *
 * Unlike shroud, hexproof allows the player to still target themselves.
 *
 * @param removeOn When this component should be removed:
 *   - [PlayerEffectRemoval.EndOfTurn] — removed during end-of-turn cleanup (e.g., Dawn's Truce)
 *   - [PlayerEffectRemoval.Permanent] — stays until explicitly removed
 */
@Serializable
data class PlayerHexproofComponent(
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Marks a player as having protection from each quality in [scopes] (CR 702.16).
 *
 * Player-level protection (The One Ring's "you gain protection from everything until
 * your next turn"). Unlike hexproof/shroud, protection from a quality also prevents
 * damage from sources of that quality (CR 702.16e, the "D" of DEBT), not just targeting.
 * For a player only the **D**amage and **T**argeting parts of DEBT apply — players can't
 * be blocked, enchanted (outside Auras), or equipped in the common case.
 *
 * Multiple grants stack additively — each grant appends to [scopes]; the player is
 * protected from a source if it matches **any** scope. The whole component is removed in
 * one shot when the duration elapses ([removeOn]).
 *
 * @property scopes The qualities the player is protected from (e.g.
 *   [com.wingedsheep.sdk.scripting.ProtectionScope.Everything]).
 * @property removeOn When this component is removed.
 */
@Serializable
data class PlayerProtectionComponent(
    val scopes: List<com.wingedsheep.sdk.scripting.ProtectionScope> = emptyList(),
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.UntilYourNextTurn
) : Component

/**
 * Marks a player as unable to play cards from their **hand** (CR 601 casting and CR 305 land
 * plays are both blocked, but only for cards in the hand zone).
 *
 * Applied by Memory Vessel's "they can't play cards from their hand" clause, which it pairs
 * with an impulse-style grant so a player effectively swaps their hand for the top cards of
 * their library for a turn. Cards in other zones (exile via a may-play permission, graveyard
 * via Muldrotha) stay playable — the restriction is hand-scoped.
 *
 * Read at legal-action enumeration (CastSpellEnumerator, PlayLandEnumerator) and re-checked
 * authoritatively in the cast/play handlers. Defaults to the
 * [PlayerEffectRemoval.UntilYourNextTurn] lifecycle, expired in the same post-untap hook as
 * [PlayerProtectionComponent] and floating `UntilYourNextTurn` effects.
 *
 * @param removeOn When this component is removed.
 * @param expiresForPlayerId For a [PlayerEffectRemoval.UntilYourNextTurn] lifecycle, whose
 *   "next turn" closes the window. Null → the component's own owner (the normal case). Memory
 *   Vessel sets this to the *activating* player so every affected player's restriction lifts on
 *   the activating player's next turn, not on each player's own next turn (which would lift an
 *   opponent's restriction at the start of their own turn — one turn too early).
 */
@Serializable
data class PlayerCantPlayFromHandComponent(
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.UntilYourNextTurn,
    val expiresForPlayerId: EntityId? = null
) : Component

/**
 * Marks a player as having the city's blessing (CR 702.131 / 700.5).
 *
 * Granted by Ascend triggers when their controller controls 10+ permanents on
 * resolution. Per CR 702.131c, the city's blessing is **permanent for the rest of
 * the game** — it is never removed once granted, even if the granting permanent
 * leaves play or the controller loses all their permanents.
 *
 * That's why this component has no `removeOn` field: cleanup never touches it.
 */
@Serializable
data object PlayerCitysBlessingComponent : Component

/**
 * Marks a player as having no maximum hand size for the rest of the game.
 *
 * Conferred by resolution effects like Wisdom of Ages ("You have no maximum hand size for the
 * rest of the game"). Distinct from the battlefield-only
 * [com.wingedsheep.sdk.scripting.NoMaximumHandSize] static ability (Reliquary Tower, Thought
 * Vessel), which only applies while its permanent is in play. Like the city's blessing, this is
 * permanent for the rest of the game — cleanup never removes it, so it has no `removeOn` field.
 */
@Serializable
data object PlayerNoMaximumHandSizeComponent : Component

/**
 * Tracks a player's emblem named **The Ring** (CR 701.54c).
 *
 * Presence of this component means the player has the Ring emblem. [temptCount] is how many times
 * the Ring has tempted that player, gating the emblem's four cumulative abilities:
 * - `>= 1` your Ring-bearer is legendary and can't be blocked by creatures with greater power
 * - `>= 2` whenever your Ring-bearer attacks, draw a card then discard a card
 * - `>= 3` whenever your Ring-bearer becomes blocked, the blocker's controller sacrifices it at end of combat
 * - `>= 4` whenever your Ring-bearer deals combat damage to a player, each opponent loses 3 life
 *
 * Like the city's blessing, the Ring emblem is never removed once gained. The Ring-bearer
 * designation itself lives on the creature ([com.wingedsheep.engine.state.components.identity.RingBearerComponent]).
 */
@Serializable
data class TheRingComponent(
    val temptCount: Int = 0
) : Component

/**
 * Describes when a player-level effect component should be removed.
 */
@Serializable
enum class PlayerEffectRemoval {
    /** Removed at end of turn during cleanup (e.g., Gilded Light) */
    EndOfTurn,
    /**
     * Removed after the untap step of the owning player's next turn — the
     * player-component analogue of [com.wingedsheep.sdk.scripting.Duration.UntilYourNextTurn]
     * (The One Ring). Cleared in the same post-untap hook as floating
     * `UntilYourNextTurn` effects.
     */
    UntilYourNextTurn,
    /** Never removed automatically — must be explicitly cleared */
    Permanent
}

/**
 * Spells matching any of [filters] that the player owning this component casts
 * can't be countered, for the duration described by [removeOn].
 *
 * Player-scoped counterpart to the permanent-static
 * [com.wingedsheep.sdk.scripting.GrantCantBeCountered]: same protection, but the source
 * is a player rather than a battlefield permanent, so the protection survives the granter
 * leaving the battlefield (Domri, Anarch of Bolas's +1).
 *
 * Multiple grants stack additively — each call appends to [filters]; a spell is uncounterable
 * if it matches any entry. The whole component is removed in one shot when the duration elapses.
 *
 * @property filters Filters matched against the spell on the stack.
 * @property removeOn When this component is removed.
 */
@Serializable
data class SpellsCantBeCounteredComponent(
    val filters: List<GameObjectFilter> = emptyList(),
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Spells matching any of [filters] that the player owning this component casts may be cast as
 * though they had flash (CR 702.8a), for the duration described by [removeOn].
 *
 * Player-scoped, duration-bounded counterpart to the permanent-static
 * [com.wingedsheep.sdk.scripting.GrantFlashToSpellType]. Both are consulted by
 * [com.wingedsheep.engine.legalactions.utils.CastPermissionUtils.hasGrantedFlash]; this
 * component lets the grant survive its source (a sorcery/instant such as Borne Upon a Wind)
 * leaving the stack, since the static-on-permanent variant would die with its source.
 *
 * Multiple grants stack additively — each [com.wingedsheep.sdk.scripting.effects.GrantFlashToSpellsEffect]
 * resolution appends to [filters]; a spell gains flash if it matches any entry. The whole
 * component is removed in one shot when the duration elapses.
 *
 * @property filters Filters matched against the card being cast (read in any zone).
 * @property removeOn When this component is removed.
 */
@Serializable
data class FlashGrantsThisTurnComponent(
    val filters: List<GameObjectFilter> = emptyList(),
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Component indicating that a player cannot cast spells for the rest of this turn.
 * Applied by effects like Xantid Swarm ("defending player can't cast spells this turn").
 *
 * When present on a player entity, that player's spell casting legal actions
 * are suppressed in LegalActionsCalculator.
 *
 * @param removeOn When this component should be removed:
 *   - [PlayerEffectRemoval.EndOfTurn] — removed during end-of-turn cleanup (default)
 *   - [PlayerEffectRemoval.Permanent] — stays until explicitly removed
 */
@Serializable
data class CantCastSpellsComponent(
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Component indicating that a player can't gain life. Conferred directly on the player by
 * [com.wingedsheep.sdk.scripting.effects.LockLifeGainEffect] (Screaming Nemesis), so the lock is
 * independent of any source permanent — distinct from the
 * [com.wingedsheep.sdk.scripting.PreventLifeGain] static replacement, which ends when its
 * permanent leaves play.
 *
 * Consulted by [com.wingedsheep.engine.handlers.effects.DamageUtils.isLifeGainPrevented].
 *
 * @param removeOn When this component should be removed:
 *   - [PlayerEffectRemoval.Permanent] — rest of the game (default; Screaming Nemesis)
 *   - [PlayerEffectRemoval.EndOfTurn] — removed during end-of-turn cleanup
 *   - [PlayerEffectRemoval.UntilYourNextTurn] — removed after that player's next untap step
 */
@Serializable
data class CantGainLifeComponent(
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.Permanent
) : Component

/**
 * Component indicating that a player cannot activate planeswalkers' loyalty abilities for the
 * rest of this turn. Applied by effects like Revel in Silence. Sibling of [CantCastSpellsComponent].
 *
 * When present on a player entity, that player's loyalty-ability activations are suppressed in
 * ActivatedAbilityEnumerator and rejected by ActivateAbilityHandler.validate.
 *
 * @param removeOn When this component should be removed:
 *   - [PlayerEffectRemoval.EndOfTurn] — removed during end-of-turn cleanup (default)
 *   - [PlayerEffectRemoval.Permanent] — stays until explicitly removed
 */
@Serializable
data class CantActivateLoyaltyAbilitiesComponent(
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Grants permission to cast creature spells from graveyard by paying the forage
 * additional cost (exile 3 cards from graveyard or sacrifice a Food).
 * Creatures cast this way enter with a finality counter.
 *
 * Applied by Osteomancer Adept's activated ability. Removed at end of turn.
 *
 * @param sourceId The permanent that granted this permission (for tracking)
 */
@Serializable
data class MayCastCreaturesFromGraveyardWithForageComponent(
    val sourceId: EntityId? = null,
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Tracks the number of cards a player has drawn during the current turn.
 * Reset to 0 at the start of each turn (for ALL players, since "each turn"
 * means every turn, not just your own).
 *
 * Used by RevealFirstDrawEachTurn to determine when to emit reveal events.
 */
@Serializable
data class CardsDrawnThisTurnComponent(
    val count: Int = 0
) : Component

/**
 * Number of equip abilities this player has activated during the current turn. Reset to 0 at
 * turn start by TurnManager. Read by Forge Anew's [com.wingedsheep.sdk.scripting.FreeFirstEquipEachTurn]
 * to know whether the next equip is the "first equip this turn" (count == 0) that may be paid for
 * with {0}.
 */
@Serializable
data class EquipActivationsThisTurnComponent(
    val count: Int = 0
) : Component

/**
 * Tracks the total damage dealt to a player during the current turn.
 * Includes both combat and non-combat damage. Prevented damage is not counted.
 * Cleared at end of turn by TurnManager.
 *
 * Used by Final Punishment: "Target player loses life equal to the damage
 * already dealt to that player this turn."
 */
@Serializable
data class DamageReceivedThisTurnComponent(
    val amount: Int = 0
) : Component

/**
 * Tracks the total damage dealt to a player this turn *by artifact sources* (a source that is an
 * artifact when it deals the damage — CR 109.5 / 702.16-style source typing). Both combat and
 * non-combat artifact damage counts; prevented damage does not (the accumulator is only incremented
 * for damage that is actually dealt). Cleared at end of turn alongside [DamageReceivedThisTurnComponent].
 *
 * Used by Reverse Polarity: "You gain X life, where X is twice the damage dealt to you so far this
 * turn by artifacts." Read via `DynamicAmount.TurnTracking(player, TurnTracker.DAMAGE_RECEIVED_FROM_ARTIFACTS)`.
 */
@Serializable
data class DamageReceivedFromArtifactsThisTurnComponent(
    val amount: Int = 0
) : Component

/**
 * Tracks the total mana spent on casting spells during the current turn.
 *
 * Used by the Expend mechanic (Bloomburrow): "Whenever you expend N" triggers
 * when the player's cumulative mana spent on spells crosses the N threshold.
 * The trigger detects the "crossing" by comparing previous vs current total,
 * ensuring each threshold fires at most once per turn.
 *
 * Reset at turn start by TurnManager.
 *
 * @param totalSpent Cumulative mana spent on casting spells this turn
 */
@Serializable
data class ManaSpentOnSpellsThisTurnComponent(
    val totalSpent: Int = 0
) : Component

/**
 * Tracks the number of nontoken creatures put into this player's graveyard
 * from the battlefield during the current turn.
 * Cleared at end of turn by TurnManager.
 *
 * Used by Caller of the Claw: "create a 2/2 green Bear creature token for each
 * nontoken creature put into your graveyard from the battlefield this turn."
 */
@Serializable
data class NonTokenCreaturesDiedThisTurnComponent(
    val count: Int = 0
) : Component

/**
 * Tracks the number of all creatures (including tokens) that died under this player's
 * control during the current turn. Cleared at end of turn by CleanupPhaseManager.
 *
 * Used by Season of Loss: "Draw a card for each creature that died under your control this turn."
 */
@Serializable
data class CreaturesDiedThisTurnComponent(
    val count: Int = 0
) : Component

/**
 * Records the last-known subtypes of each creature that died under this player's control during
 * the current turn — one [diedSubtypeSets] entry per death, in death order. Subtypes are stored
 * as their raw strings (e.g. "Zombie"), captured from the dying creature's projected type line at
 * the moment it left the battlefield (CR 603.10 / last-known information), so a creature that loses
 * its types after death is still recorded with the subtypes it had as it died.
 *
 * Cleared at end of turn by CleanupPhaseManager alongside [CreaturesDiedThisTurnComponent].
 *
 * Backs the subtype-filtered death conditions
 * ([com.wingedsheep.sdk.scripting.conditions.CreatureWithSubtypeDiedThisTurn]): "a Goblin died this
 * turn" / "a non-Zombie creature died this turn" (Undead Sprinter, DSK). Summed across all players
 * this gives the game-wide record, since the condition is global.
 */
@Serializable
data class CreatureSubtypesDiedThisTurnComponent(
    val diedSubtypeSets: List<Set<String>> = emptyList()
) : Component

/**
 * Tracks the number of permanents — of any type, token or nontoken — that left the
 * battlefield this turn while under this player's control. Credited to the last-known
 * controller at the moment of departure (so a Threaten-style steal-and-sacrifice
 * counts for the thief, not the original owner). Cleared at end of turn by
 * CleanupPhaseManager.
 *
 * Used by Shortcut to Mushrooms (LTR): "if a permanent you controlled left the
 * battlefield this turn".
 */
@Serializable
data class PermanentLeftBattlefieldThisTurnComponent(
    val count: Int = 0
) : Component

/**
 * Tracks the number of creatures that were exiled from opponents' control this turn.
 * Used by Vren, the Relentless: "create X tokens where X is the number of creatures
 * that were exiled under your opponents' control this turn."
 *
 * Stored on the player who controls the exiling effect (i.e., Vren's controller).
 * Cleared at end of turn by CleanupPhaseManager.
 */
@Serializable
data class OpponentCreaturesExiledThisTurnComponent(
    val count: Int = 0
) : Component

/**
 * Tracks whether this player has gained life during the current turn.
 * Set whenever life is gained by this player.
 * Cleared at end of turn by CleanupPhaseManager.
 *
 * Used for conditions like "if you gained life this turn" (Lunar Convocation).
 */
@Serializable
data object LifeGainedThisTurnComponent : Component

/**
 * Tracks the total amount of life this player has gained during the current turn.
 * Accumulates across all life-gain events. Cleared at end of turn by CleanupPhaseManager.
 *
 * Used for `DynamicAmount.TurnTracking(player, TurnTracker.LIFE_GAINED)` — e.g.
 * Bre of Clan Stoutarm's "less than or equal to the amount of life you gained this turn".
 */
@Serializable
data class LifeGainedAmountThisTurnComponent(val amount: Int = 0) : Component

/**
 * Tracks whether this player has lost life during the current turn.
 * Set whenever a LifeChangedEvent with a non-gain reason is emitted for this player.
 * Cleared at end of turn by CleanupPhaseManager.
 *
 * Used for conditions like "if an opponent lost life this turn" (Hired Claw).
 */
@Serializable
data object LifeLostThisTurnComponent : Component

/**
 * Tracks the number of cards that left this player's graveyard this turn.
 * Cleared at end of turn by CleanupPhaseManager.
 *
 * Used for conditions like "if three or more cards left your graveyard this turn"
 * (Bonecache Overseer).
 */
@Serializable
data class CardsLeftGraveyardThisTurnComponent(val count: Int = 0) : Component

/**
 * Tracks the number of this player's (owned) cards that were put into exile this turn. Tokens are
 * excluded — a token briefly placed in exile isn't a card. Summed across all players it yields the
 * game-wide "cards put into exile this turn" count. Reset to 0 for every player at the start of
 * each turn by [com.wingedsheep.engine.core.TurnManager].
 *
 * Used for conditions like "if one or more cards were put into exile this turn" (Ennis, Debate
 * Moderator).
 */
@Serializable
data class CardsPutIntoExileThisTurnComponent(val count: Int = 0) : Component

/**
 * Marker component indicating that this player has sacrificed a Food artifact this turn.
 * Cleared at end of turn by CleanupPhaseManager.
 *
 * Used for conditions like "if you've sacrificed a Food this turn" (Bonecache Overseer).
 */
@Serializable
data object SacrificedFoodThisTurnComponent : Component

/**
 * Tracks the number of permanents this player has sacrificed this turn (controller-scoped,
 * any permanent type). Incremented by `ZoneTransitionService.trackPermanentSacrifice` on the
 * sacrificing player and cleared at end of turn by `CleanupPhaseManager`.
 *
 * Distinct from the game-wide turn-scoped `GameState.permanentsSacrificedThisTurn` counter
 * (which sums every player's sacrifices for "for each permanent sacrificed this turn" cost
 * reductions): this is per-player, so it backs "if you sacrificed one or more permanents this
 * turn, ... deals that much damage" (Sawblade Skinripper) via
 * `TurnTracker.PERMANENTS_SACRIFICED`.
 */
@Serializable
data class PermanentsSacrificedThisTurnComponent(val count: Int = 0) : Component

/**
 * Tracks whether a permanent entered the battlefield face down under this player's control during
 * the current turn (morph, manifest, disguise, cloak, or any face-down entry). Incremented in
 * `ZoneTransitionService` at the moment of a face-down battlefield entry and cleared at the turn
 * boundary by `CleanupPhaseManager`.
 *
 * Backs Oblivious Bookworm's "...unless a permanent entered the battlefield face down under your
 * control this turn..." via the `PermanentEnteredFaceDownThisTurn` condition.
 */
@Serializable
data class PermanentEnteredFaceDownThisTurnComponent(val count: Int = 0) : Component

/**
 * Tracks whether this player turned a permanent face up during the current turn (morph/disguise
 * turn-up special action, or any "turn it face up" effect). Incremented in the turn-face-up
 * handler and cleared at the turn boundary by `CleanupPhaseManager`.
 *
 * Backs Oblivious Bookworm's "...or you turned a permanent face up this turn." via the
 * `PlayerTurnedPermanentFaceUpThisTurn` condition.
 */
@Serializable
data class TurnedPermanentFaceUpThisTurnComponent(val count: Int = 0) : Component

/**
 * Tracks the number of permanent (nontoken) cards put into this player's graveyard from
 * any zone during the current turn — i.e. the number of times this player has
 * "descended" per CR 700.11. Cleared at end of turn by CleanupPhaseManager.
 *
 * Per CR 700.11 / Scryfall ruling, the cards do not need to still be in the graveyard;
 * the count is a pure event tracker.
 *
 * Used for the descend gate ("if you descended this turn", Ruin-Lurker Bat) and the
 * descend N / fathomless descent ability words ("if you descended four or more times
 * this turn", "the number of times you descended this turn").
 */
@Serializable
data class PlayerDescendedThisTurnComponent(val count: Int = 0) : Component

/**
 * Tracks which card types have entered the battlefield under this player's control this turn.
 * Populated by `BattlefieldEntry.place` (via `PermanentEntryTracker.record`) from the projected
 * types at the moment of entry, so a permanent that's an artifact-by-effect at ETB is recorded
 * just like a printed artifact. Once recorded, the entry is insensitive to later state changes
 * — the entry of an artifact remains recorded even if the permanent is destroyed, loses its
 * artifact type, or changes controllers later in the turn.
 *
 * Cleared at end of turn by CleanupPhaseManager.
 *
 * Used for conditions like Mechan Shieldmate's "as long as an artifact entered the battlefield
 * under your control this turn".
 */
@Serializable
data class PermanentTypesEnteredBattlefieldThisTurnComponent(
    val cardTypes: Set<com.wingedsheep.sdk.core.CardType> = emptySet()
) : Component

/**
 * Tracks the number of lands that have entered the battlefield under this player's control
 * during the current turn. Counts every ETB regardless of how the land arrived — normal land
 * drops, Lander-token search, Cultivate-style "put a land onto the battlefield" effects,
 * gift-a-land effects, etc. — so it diverges from `LandDropsComponent` (which only counts
 * the from-hand action).
 *
 * Populated by `PermanentEntryTracker.record` whenever the entering permanent's projected
 * types include `LAND`. Cleared at end of turn by [CleanupPhaseManager].
 *
 * Used for `DynamicAmount.TurnTracking(player, TurnTracker.LANDS_ENTERED_UNDER_CONTROL)` —
 * e.g. Bioengineered Future's "for each land that entered the battlefield under your
 * control this turn".
 */
@Serializable
data class LandsEnteredUnderControlThisTurnComponent(val count: Int = 0) : Component

/**
 * One permanent that entered the battlefield under a player's control this turn, captured for
 * subtype-keyed "for each [creature type] that entered the battlefield under your control this
 * turn" counts (Geralf, the Fleshwright). [subtypes] is snapshotted from the **projected** state
 * at the instant of entry, so a permanent that was a Zombie when it entered still counts even
 * after it leaves the battlefield or loses the type (CR 603.10 last-known style — the entry event
 * is what matters, not current state). [entityId] lets a triggered ability exclude the permanent
 * that caused it to trigger ("each *other* Zombie", and simultaneous entries per the 2024-04-12
 * ruling).
 */
@Serializable
data class EnteredPermanentRecord(
    val entityId: EntityId,
    val subtypes: Set<String> = emptySet()
)

/**
 * Tracks every permanent that entered the battlefield under this player's control during the
 * current turn, with the subtypes each had at entry. Populated by `PermanentEntryTracker.record`
 * (exactly once per ETB) and cleared at end of turn by [CleanupPhaseManager].
 *
 * Backs `DynamicAmount.SubtypeEnteredUnderControlThisTurn(player, subtype, excludeTriggeringEntity)`
 * — "for each [other] [subtype] that entered the battlefield under your control this turn"
 * (Geralf, the Fleshwright). Counts entries even if the permanent has since left or changed type.
 */
@Serializable
data class PermanentsEnteredUnderControlThisTurnComponent(
    val entries: List<EnteredPermanentRecord> = emptyList()
) : Component

/**
 * Marker component indicating that this player has put a counter on a creature this turn.
 * Cleared at end of turn by CleanupPhaseManager.
 *
 * Used for conditions like "if you put a counter on a creature this turn" (Lasting Tarfire).
 */
@Serializable
data object PutCounterOnCreatureThisTurnComponent : Component

/**
 * Marks a player as having been dealt combat damage this turn.
 * Cleared at end of turn by CleanupPhaseManager.
 * Used for YouWereDealtCombatDamageThisTurn condition.
 */
@Serializable
data object WasDealtCombatDamageThisTurnComponent : Component

/**
 * Marks a player as having been dealt combat damage by a legendary creature this turn.
 * Cleared at end of turn by CleanupPhaseManager.
 * Backs the DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE turn tracker
 * (AnOpponentWasDealtCombatDamageByLegendaryCreatureThisTurn condition — Blitzball).
 */
@Serializable
data object WasDealtCombatDamageByLegendaryCreatureThisTurnComponent : Component

/**
 * Component indicating that a player should skip their entire next [turns] turns.
 * Applied by effects like Last Chance (which gives the opponent an "extra turn" by skipping the
 * other player's turn in a 2-player game) and Ral Zarek, Guest Lecturer's ultimate (skip several
 * turns).
 *
 * One skipped turn is consumed each time the affected player's turn would start: [turns] is
 * decremented, the turn is skipped, and the component is removed once the count reaches zero.
 * Re-applying it adds to the remaining count rather than overwriting (multiple skip effects stack).
 *
 * @property turns How many of the player's upcoming turns remain to be skipped (≥ 1 while present).
 */
@Serializable
data class SkipNextTurnComponent(val turns: Int = 1) : Component

/**
 * Tracks a Mindslaver-style "you control target opponent during their next turn" effect.
 *
 * Lifecycle:
 *  - Created with [HijackState.SCHEDULED] when [HijackNextTurnEffect] resolves.
 *  - At the start of the affected player's next turn (after any [SkipNextTurnComponent]
 *    skipping resolves) the component transitions to [HijackState.ACTIVE] in
 *    [TurnManager.startTurn].
 *  - Removed at end-of-turn cleanup of the controlled turn.
 *
 * Per the Scryfall rulings on The Dominion Bracelet: multiple hijacks affecting the same
 * player overwrite each other (latest wins). The affected player is still the rules
 * controller of their own permanents/spells; only input authority moves to [controllerId]
 * for the duration of the [ACTIVE] window.
 *
 * @property controllerId The player making decisions during the controlled turn
 * @property state Whether this hijack is queued for a future turn or actively in effect
 */
@Serializable
data class PlayerTurnHijackedComponent(
    val controllerId: EntityId,
    val state: HijackState = HijackState.SCHEDULED
) : Component {
    @Serializable
    enum class HijackState { SCHEDULED, ACTIVE }
}

/**
 * Marks a player whose *input authority* is permanently delegated to [controllerId] for
 * the entire game — the session-level "hotseat" / play-against-yourself setting.
 *
 * This is the non-turn-scoped generalization of [PlayerTurnHijackedComponent]: where a
 * hijack moves input authority for a single controlled turn, this moves it for the whole
 * game with no lifecycle. In a hotseat scenario the single human connection is set as the
 * [controllerId] for *both* seats, so one client answers every decision and may submit
 * actions for either player.
 *
 * As with hijack, this moves input authority ONLY — resource ownership (mana, cards, life,
 * spell/permanent controllership) stays with the affected player, which is exactly what
 * keeps the engine's seat/ownership checks correct. It is consulted solely at the
 * input-routing seam via [com.wingedsheep.engine.state.GameState.actorFor]: legal-action
 * enumeration, decision delivery/validation, per-action seat authorization, and hand
 * visibility.
 *
 * It is written only by the server's hotseat-scenario creation path and is never settable
 * by a client message, so it cannot widen authority or visibility in a normal game.
 *
 * @property controllerId The connection/player that holds input authority for this seat.
 */
@Serializable
data class HotseatControlComponent(
    val controllerId: EntityId
) : Component

/**
 * Component indicating that a player will lose the game at the beginning of
 * a future end step. Applied by effects like Last Chance.
 *
 * @param turnsUntilLoss Number of end steps to skip before triggering.
 *   - 1 means lose at the end of the NEXT turn (not the current one)
 *   - 0 means lose at the end of the CURRENT turn
 * @param message Optional custom message to display when the player loses
 *
 * This component is consumed (removed) when it triggers and the player loses.
 */
@Serializable
data class LoseAtEndStepComponent(
    val turnsUntilLoss: Int = 1,
    val message: String? = null
) : Component

/**
 * Component granting a flat damage bonus to sources a player controls.
 * Applied by effects like The Flame of Keld Chapter III: "If a red source you control
 * would deal damage to a permanent or player this turn, it deals that much damage plus 2 instead."
 *
 * @param bonusAmount The flat bonus to add to damage
 * @param sourceFilter Which sources get the bonus (e.g., SourceFilter.HasColor(Color.RED) for red sources)
 * @param removeOn When this component should be removed
 */
@Serializable
data class DamageBonusComponent(
    val bonusAmount: Int,
    val sourceFilter: SourceFilter = SourceFilter.Any,
    val removeOn: PlayerEffectRemoval = PlayerEffectRemoval.EndOfTurn
) : Component

/**
 * Component granting additional keywords to spells a player casts.
 * Applied by emblems like Ral's "Instant and sorcery spells you cast have storm."
 *
 * @param grants List of keyword grants, each specifying which keyword is granted
 *   and which spell types it applies to
 */
@Serializable
data class GrantedSpellKeywordsComponent(
    val grants: List<SpellKeywordGrant>
) : Component

/**
 * A single keyword grant for spells matching a filter.
 *
 * @param keyword The keyword to grant (e.g., STORM)
 * @param spellFilter Which spells get the keyword (e.g., GameObjectFilter.InstantOrSorcery)
 */
@Serializable
data class SpellKeywordGrant(
    val keyword: Keyword,
    val spellFilter: GameObjectFilter
)
