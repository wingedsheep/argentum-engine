package com.wingedsheep.engine.state.components.player

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Keyword
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
        riders: Set<ManaSpellRider> = emptySet()
    ): ManaPoolComponent {
        val entries = (1..amount).map { RestrictedManaEntry(color, restriction, riders) }
        return copy(restrictedMana = restrictedMana + entries)
    }

    /**
     * Empty the mana pool.
     */
    fun empty(): ManaPoolComponent = ManaPoolComponent()
}

/**
 * A single unit of mana with a spending restriction.
 * @param color The color of the mana, or null for colorless.
 * @param restriction The restriction on how this mana can be spent.
 * @param riders Side-effects applied to a spell when this mana is spent on it
 *   (e.g. [ManaSpellRider.MakesSpellUncounterable] for Cavern of Souls).
 */
@Serializable
data class RestrictedManaEntry(
    val color: Color?,
    val restriction: ManaRestriction,
    val riders: Set<ManaSpellRider> = emptySet()
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
    val pendingLeylineCardIds: List<EntityId> = emptyList()
) : Component {
    companion object {
        const val STARTING_HAND_SIZE = 7
    }

    /**
     * The number of cards this player must put on the bottom after keeping.
     */
    val cardsToBottom: Int get() = mulligansTaken

    /**
     * Check if the player can still mulligan (they can always mulligan until
     * they would have to bottom more cards than they drew).
     */
    val canMulligan: Boolean get() = mulligansTaken < STARTING_HAND_SIZE && !hasKept

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
 * Component tracking additional combat phases to be inserted into the current turn.
 * When the postcombat main phase would advance to the end step, if this component
 * exists, the game instead transitions to an additional combat phase followed by
 * a main phase. The count is decremented each time an additional combat phase begins.
 *
 * Applied by effects like Aggravated Assault.
 *
 * @param count Number of additional combat+main phase cycles remaining
 */
@Serializable
data class AdditionalCombatPhasesComponent(
    val count: Int = 1
) : Component

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
 * Marker component indicating that this player has sacrificed a Food artifact this turn.
 * Cleared at end of turn by CleanupPhaseManager.
 *
 * Used for conditions like "if you've sacrificed a Food this turn" (Bonecache Overseer).
 */
@Serializable
data object SacrificedFoodThisTurnComponent : Component

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
 * Marker component indicating that a player should skip their entire next turn.
 * Applied by effects like Last Chance (which gives the opponent an "extra turn"
 * by skipping the other player's turn in a 2-player game).
 *
 * This component is consumed (removed) when the turn would start, and the turn
 * is skipped instead.
 */
@Serializable
data object SkipNextTurnComponent : Component

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
