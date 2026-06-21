package com.wingedsheep.engine.core

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of all game events.
 * Events are emitted by the engine to describe what happened.
 */
@Serializable
sealed interface GameEvent

// =============================================================================
// Zone Change Events
// =============================================================================

/**
 * An entity moved between zones.
 */
@Serializable
@SerialName("ZoneChangeEvent")
data class ZoneChangeEvent(
    val entityId: EntityId,
    val entityName: String,
    val fromZone: Zone?,
    val toZone: Zone,
    val ownerId: EntityId,
    /**
     * Last-known controller when leaving the battlefield. Differs from [ownerId] for stolen
     * creatures (Threaten / Mind Control) and for tokens whose controller ≠ creator's owner.
     * Trigger filters that scope "creatures that player controls" (e.g., "the creature with
     * the greatest power among creatures that player controls" — Kraven the Hunter) read
     * this in preference to [ownerId]. Null for non-battlefield transitions.
     */
    val lastKnownController: EntityId? = null,
    /** Last known +1/+1 counter count when leaving battlefield (for death triggers needing last known info) */
    val lastKnownCounterCount: Int = 0,
    /** Last known -1/-1 counter count when leaving battlefield (used by the persist keyword's "had no -1/-1 counters" check) */
    val lastKnownMinusOneMinusOneCounterCount: Int = 0,
    /** Last known total counter count (all counter types) when leaving battlefield. Used by triggers that care about any counter (e.g., Shadow Urchin). */
    val lastKnownTotalCounterCount: Int = 0,
    /** True if the leaving entity was a token. Used to suppress persist-style return triggers on tokens (Rule 704.5d — tokens cease to exist when they leave the battlefield). */
    val lastKnownWasToken: Boolean = false,
    /** Last known projected power when leaving battlefield (for trigger filters needing last known info) */
    val lastKnownPower: Int? = null,
    /** Last known projected toughness when leaving battlefield (for trigger filters needing last known info) */
    val lastKnownToughness: Int? = null,
    /** The original card name when this permanent entered as a copy (e.g., "Clever Impersonator") */
    val copyOfOriginalName: String? = null,
    /** For auras: the entity this aura was attached to when it left the battlefield (for "enchanted creature dies" triggers) */
    val lastKnownAttachedTo: EntityId? = null,
    /**
     * Creatures that were blocking, or blocked by, this creature when it left the battlefield
     * (the union of its [com.wingedsheep.engine.state.components.combat.BlockedComponent] blockers
     * and its [com.wingedsheep.engine.state.components.combat.BlockingComponent] blocked-attackers,
     * CR 509). Captured as last-known information because the live combat cross-references are torn
     * down as the creature leaves — read by "destroy all creatures blocking or blocked by it" dies
     * triggers (Abu Ja'far). Empty for non-combat / non-battlefield transitions.
     */
    val lastKnownBlockingOrBlockedByIds: List<EntityId> = emptyList(),
    /** Last known type line when leaving battlefield (for trigger detection when entity has been cleaned up, e.g., tokens) */
    val lastKnownTypeLine: TypeLine? = null,
    /**
     * Last known card definition id when leaving battlefield. Needed so dies/leaves triggers can
     * still be resolved for tokens after 704.5s cleans them out of the graveyard in the same SBA
     * pass as they were put there.
     */
    val lastKnownCardDefinitionId: String? = null,
    /** Last known projected keywords when leaving battlefield (for trigger filters needing keyword info after death) */
    val lastKnownKeywords: Set<String> = emptySet(),
    /**
     * True if the leaving entity had its abilities stripped (via a layer-6 `RemoveAllAbilities`
     * floating effect) at the moment it left the battlefield. CR 603.10a — leaves-the-battlefield
     * triggers look back in time at the object's appearance immediately prior to the event — so
     * the dies / leaves-battlefield detector reads this snapshot, not the (now-cleared) projected
     * `lostAllAbilities` flag.
     */
    val lastKnownLostAllAbilities: Boolean = false,
    /**
     * Last-known counter map (counter-type-string → count) when leaving the battlefield.
     * Used by triggers that move every counter (e.g., Essence Channeler), not just one
     * specific kind. Counter-type strings match the wire format used on counter effects
     * (e.g., "+1/+1", "-1/-1", "loyalty", "charge", ...).
     */
    val lastKnownCounters: Map<String, Int> = emptyMap(),
    /**
     * The `{X}` associated with this object's zone change. On entry to the battlefield it is the X
     * of the spell that put the permanent there (for ETB triggers using `DynamicAmount.XValue`). On
     * leaving the battlefield it is the cast-time X carried by `CastChoicesComponent`, captured as
     * last-known information so dies/leaves triggers can still read `DynamicAmount.CastX`.
     */
    val xValue: Int? = null,
    /**
     * Per-player damage dealt to this entity this turn (keyed by source-controller player id),
     * captured at the moment of leaving the battlefield. Read by LTB triggers like Grothama:
     * "each player draws cards equal to the damage dealt to ~ this turn by sources they controlled."
     */
    val lastKnownDamageDealtByPlayers: Map<EntityId, Int> = emptyMap(),
    /**
     * Last-known snapshots of the sources that dealt damage to this entity this turn, captured at
     * the moment of leaving the battlefield. Read by observer death triggers of the form "whenever
     * another creature dealt damage this turn by [a source matching a filter] dies" (Shelob, Child
     * of Ungoliant) so a source that died in the same combat is still evaluated against the filter.
     */
    val lastKnownDamageSources: Set<com.wingedsheep.engine.state.components.battlefield.DamageSourceLki> = emptySet()
) : GameEvent

// =============================================================================
// Life Events
// =============================================================================

/**
 * A player's life total changed.
 *
 * [firstThisTurn] is only meaningful for [LifeChangeReason.LIFE_GAIN] events: it is `true` when this
 * is the first life-gaining event for [playerId] this turn (computed by `DamageUtils.gainLife`
 * before the per-turn life-gained marker is set). It backs "whenever you gain life for the first
 * time each turn" triggers (Leech Collector). It is always `false` for non-gain reasons.
 */
@Serializable
@SerialName("LifeChangedEvent")
data class LifeChangedEvent(
    val playerId: EntityId,
    val oldLife: Int,
    val newLife: Int,
    val reason: LifeChangeReason,
    val firstThisTurn: Boolean = false
) : GameEvent

@Serializable
enum class LifeChangeReason {
    DAMAGE,
    LIFE_LOSS,
    LIFE_GAIN,
    PAYMENT
}

// =============================================================================
// Damage Events
// =============================================================================

/**
 * Damage was dealt.
 */
@Serializable
@SerialName("DamageDealtEvent")
data class DamageDealtEvent(
    val sourceId: EntityId?,
    val targetId: EntityId,
    val amount: Int,
    val isCombatDamage: Boolean,
    val sourceName: String? = null,
    val targetName: String? = null,
    val targetIsPlayer: Boolean = false,
    val targetWasFaceDown: Boolean = false,
    /**
     * The recipient's controller at the instant the damage was dealt (CR 603.10 last-known
     * information). Lets recipient-based damage triggers ("whenever a creature you control / an
     * opponent controls is dealt damage") still match a recipient that left the battlefield to
     * the same damage event — combat-damage state-based actions strip the dead creature's
     * `ControllerComponent` before trigger detection runs. `null` for players and for events
     * emitted before this was captured.
     */
    val targetControllerId: EntityId? = null,
    /** Whether the recipient was a creature when the damage was dealt (LKI, see [targetControllerId]). */
    val targetWasCreature: Boolean = false,
    /**
     * Damage in excess of what the creature target needed to be destroyed (CR 120.4a) —
     * i.e. `max(0, amount - max(0, projectedToughness - markedDamageBeforeThisHit))`, or
     * `max(0, amount - 1)` if the source has deathtouch. Always 0 for non-creature targets
     * (planeswalkers, players). Used by triggers like
     * Fall of Cair Andros that fire on "excess [non]combat damage" via
     * `DealsDamageEvent(requireExcess = true)` and by payoffs that read
     * `ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT`.
     */
    val excessAmount: Int = 0,
    /**
     * The recipient creature's toughness at the instant the damage was dealt (CR 603.10 last-known
     * information), captured before state-based actions can move a lethally-damaged creature to the
     * graveyard. Read by triggers keyed on "damage equal to that creature's toughness" (Taii Wakeen,
     * Perfect Shot) via `ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS`. `null` for players,
     * planeswalkers, and events emitted before this was captured.
     */
    val targetToughnessAtDamage: Int? = null
) : GameEvent

/**
 * A "next damage from a chosen source" shield fired on an instance of damage (Deflecting Palm,
 * New Way Forward, Eye for an Eye). Carries [linkId] so the shield's own delayed triggered ability
 * ("When damage is prevented this way, …") fires on the stack and reads [amount] (the captured
 * amount) and [sourceId] (whose controller to hit).
 *
 * NOTE: despite the name, this fires even when the damage is NOT prevented — a `preventDamage = false`
 * shield (Eye for an Eye) still emits this to fire its reaction while letting the damage proceed in
 * full. It is internal (no client event, no generic trigger matches it — only the linked delayed
 * trigger keyed by [linkId]), so the misnomer has no observable effect.
 *
 * @property sourceId The source whose damage triggered the shield
 * @property recipientId The protected player the shield was attached to
 * @property amount The captured damage amount
 * @property linkId The id of the delayed triggered ability linked to this shield
 */
@Serializable
@SerialName("DamagePreventedEvent")
data class DamagePreventedEvent(
    val sourceId: EntityId,
    val recipientId: EntityId,
    val amount: Int,
    val linkId: String,
    val sourceName: String? = null
) : GameEvent

/**
 * A card was played (cast as a spell or played as a land) using an impulse-style
 * "you may play this card" permission that carried an "When you play a card this way, …"
 * rider (Fires of Mount Doom). Emitted at the play site; carries [linkId] so the granting
 * permission's linked delayed triggered ability fires the rider on the stack. Mirrors
 * [DamagePreventedEvent]'s link-id scoping.
 *
 * @property cardId The card that was played
 * @property controllerId The player who played it
 * @property sourceId The permanent that granted the play permission (the rider's source)
 * @property linkId The id shared with the linked delayed triggered ability
 */
@Serializable
@SerialName("CardPlayedFromPermissionEvent")
data class CardPlayedFromPermissionEvent(
    val cardId: EntityId,
    val controllerId: EntityId,
    val sourceId: EntityId,
    val linkId: String
) : GameEvent

/**
 * Stats were modified (e.g., +3/+3 until end of turn).
 */
@Serializable
@SerialName("StatsModifiedEvent")
data class StatsModifiedEvent(
    val targetId: EntityId,
    val targetName: String,
    val powerChange: Int,
    val toughnessChange: Int,
    val sourceName: String
) : GameEvent

/**
 * A keyword was granted (e.g., "gains flying until end of turn").
 */
@Serializable
@SerialName("KeywordGrantedEvent")
data class KeywordGrantedEvent(
    val targetId: EntityId,
    val targetName: String,
    val keyword: String,
    val sourceName: String
) : GameEvent

/**
 * A player gained the city's blessing (CR 702.131 / 700.5).
 *
 * Fired by Ascend triggers when their controller controls 10+ permanents on
 * resolution. The blessing is permanent for the rest of the game — this event
 * fires at most once per player per game.
 */
@Serializable
@SerialName("CitysBlessingGainedEvent")
data class CitysBlessingGainedEvent(
    val playerId: EntityId,
    val playerName: String,
    val sourceName: String
) : GameEvent

/**
 * A player lost their maximum hand size for the rest of the game (Wisdom of Ages,
 * "You have no maximum hand size for the rest of the game"). Permanent — fires at most
 * once per player per game.
 */
@Serializable
@SerialName("MaximumHandSizeRemovedEvent")
data class MaximumHandSizeRemovedEvent(
    val playerId: EntityId,
    val playerName: String,
    val sourceName: String
) : GameEvent

/**
 * The Ring tempted a player (CR 701.54d). Emitted after the "the Ring tempts you" action
 * completes (even if some or all of it was impossible). Drives "Whenever the Ring tempts you"
 * triggers; see [com.wingedsheep.sdk.scripting.EventPattern.RingTemptedEvent].
 *
 * @property playerId The tempted player.
 * @property temptCount That player's tempt count after this tempt (1..n).
 * @property bearerId The creature designated Ring-bearer, or null if the player controlled none.
 * @property sourceName The card/ability that caused the temptation (for display).
 */
@Serializable
@SerialName("RingTemptedEvent")
data class RingTemptedEvent(
    val playerId: EntityId,
    val temptCount: Int,
    val bearerId: EntityId?,
    val sourceName: String
) : GameEvent

/**
 * A player just finished a `scry N` (CR 701.18). Fires once per scry, after the
 * top/bottom moves have all resolved. Drives "Whenever you scry" triggers; see
 * [com.wingedsheep.sdk.scripting.EventPattern.ScriedEvent].
 *
 * @property playerId The player who scried.
 * @property count Number of cards actually looked at (equals scry N unless the
 *   library held fewer cards). Surfaced via `TRIGGER_SCRY_COUNT` so payoffs can
 *   scale by "the number of cards looked at."
 * @property sourceName The card/ability that caused the scry (for display).
 */
@Serializable
@SerialName("ScriedEvent")
data class ScriedEvent(
    val playerId: EntityId,
    val count: Int,
    val sourceName: String
) : GameEvent

/**
 * A player just finished a `surveil N` (CR 701.42). Fires once per surveil, after the
 * kept/graveyard moves have all resolved. Drives "Whenever you surveil" and "Whenever you
 * scry or surveil" triggers; see [com.wingedsheep.sdk.scripting.EventPattern.SurveiledEvent].
 *
 * @property playerId The player who surveiled.
 * @property count Number of cards actually looked at (equals surveil N unless the library held
 *   fewer cards). Surfaced via `TRIGGER_SCRY_COUNT` ("the number of cards looked at").
 * @property sourceName The card/ability that caused the surveil (for display).
 */
@Serializable
@SerialName("SurveiledEvent")
data class SurveiledEvent(
    val playerId: EntityId,
    val count: Int,
    val sourceName: String
) : GameEvent

/**
 * A player chose a creature type (e.g., "Choose a creature type" for Walking Desecration).
 * This is a public announcement visible to all players.
 */
@Serializable
@SerialName("CreatureTypeChosenEvent")
data class CreatureTypeChosenEvent(
    val playerId: EntityId,
    val chosenType: String,
    val sourceName: String?
) : GameEvent

/**
 * A creature's type was changed (e.g., "becomes a Goblin until end of turn").
 */
@Serializable
@SerialName("CreatureTypeChangedEvent")
data class CreatureTypeChangedEvent(
    val targetId: EntityId,
    val targetName: String,
    val newType: String,
    val sourceName: String
) : GameEvent

// =============================================================================
// Spell/Ability Events
// =============================================================================

/**
 * A spell was cast.
 */
@Serializable
@SerialName("SpellCastEvent")
data class SpellCastEvent(
    val spellEntityId: EntityId,
    val cardName: String,
    val casterId: EntityId,
    val targetNames: List<String> = emptyList(),
    val xValue: Int? = null,
    val wasKicked: Boolean = false,
    /** Total mana spent to cast this spell (for Expend trigger detection) */
    val totalManaSpent: Int = 0,
    /**
     * Number of distinct colors of mana spent to cast this spell (0–5); colorless is not a
     * color (CR 105.1) and never contributes. Feeds
     * `ContextPropertyKey.COLORS_SPENT_ON_TRIGGERING_SPELL` so a "Whenever you cast an instant
     * or sorcery spell, … for each color of mana spent to cast that spell" payoff on a separate
     * permanent (Magmablood Archaic) scales by the triggering spell's color count.
     */
    val distinctColorsSpent: Int = 0,
    /**
     * True when any of the mana spent on this cast was tagged as Treasure
     * mana (see [com.wingedsheep.engine.state.components.player.ManaPoolComponent.treasureMana]).
     * Drives SDK triggers built with
     * `Triggers.youCastSpell(requires = setOf(SpellCastPredicate.PaidWithManaFromSubtype(Subtype.TREASURE)))`.
     */
    val paidWithTreasureMana: Boolean = false,
    /**
     * Number of mode picks recorded on this cast (size of
     * [com.wingedsheep.engine.state.components.stack.SpellOnStackComponent.chosenModes]).
     * `0` for non-modal spells. Drives `SpellCastPredicate.IsModal` matching and feeds
     * `ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL` for cards like Riku of Many
     * Paths whose triggered ability scales by the cast's mode count.
     */
    val chosenModesCount: Int = 0,
    /**
     * Mana value of the cast spell (CR 202.3), captured at cast time. Distinct from
     * [totalManaSpent] (actual mana paid, which can differ with cost reductions, alternative
     * costs, or X). Feeds `ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE` so payoffs that key
     * off "a spell with equal or lesser mana value" (Kellan, the Kid) read the printed value of
     * the spell that fired the trigger, not the mana spent on it.
     */
    val manaValue: Int = 0
) : GameEvent

/**
 * An ability was activated.
 */
@Serializable
@SerialName("AbilityActivatedEvent")
data class AbilityActivatedEvent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val abilityEntityId: EntityId? = null
) : GameEvent

/**
 * An ability triggered.
 */
@Serializable
@SerialName("AbilityTriggeredEvent")
data class AbilityTriggeredEvent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val description: String,
    val abilityEntityId: EntityId? = null
) : GameEvent

/**
 * A spell or ability's target was randomly reselected (e.g., by Grip of Chaos).
 */
@Serializable
@SerialName("TargetReselectedEvent")
data class TargetReselectedEvent(
    val spellOrAbilityName: String,
    val oldTargetName: String,
    val newTargetName: String,
    val sourceName: String
) : GameEvent

/**
 * A spell or ability resolved.
 */
@Serializable
@SerialName("ResolvedEvent")
data class ResolvedEvent(
    val entityId: EntityId,
    val name: String
) : GameEvent

/**
 * A copy of a spell was put onto the stack (Storm, Fork, Copy Target Spell, etc.).
 * Per rule 707.10 copies aren't cast, so this event is distinct from [SpellCastEvent]
 * and must not match "whenever you cast" triggers.
 */
@Serializable
@SerialName("SpellCopiedEvent")
data class SpellCopiedEvent(
    val copyEntityId: EntityId,
    val cardName: String,
    val controllerId: EntityId,
    val originalSpellId: EntityId? = null,
    val copyIndex: Int? = null,
    val copyTotal: Int? = null
) : GameEvent

/**
 * A spell was countered.
 */
@Serializable
@SerialName("SpellCounteredEvent")
data class SpellCounteredEvent(
    val spellEntityId: EntityId,
    val cardName: String
) : GameEvent

/**
 * An activated or triggered ability was countered.
 */
@Serializable
@SerialName("AbilityCounteredEvent")
data class AbilityCounteredEvent(
    val abilityEntityId: EntityId,
    val description: String
) : GameEvent

/**
 * A spell fizzled (all targets became invalid).
 */
@Serializable
@SerialName("SpellFizzledEvent")
data class SpellFizzledEvent(
    val spellEntityId: EntityId,
    val cardName: String,
    val reason: String
) : GameEvent

/**
 * An ability resolved.
 */
@Serializable
@SerialName("AbilityResolvedEvent")
data class AbilityResolvedEvent(
    val sourceId: EntityId,
    val description: String
) : GameEvent

/**
 * A Saga's chapter ability resolved. Emitted by [com.wingedsheep.engine.mechanics.stack.StackResolver]
 * when a triggered ability carrying saga-chapter metadata finishes resolving. [isFinalChapter] is
 * true when [chapterNumber] equals the Saga's highest chapter number (CR 714) — the cue for
 * "Whenever the final chapter ability of a Saga you control resolves" (Tom Bombadil).
 *
 * @property sagaId The Saga permanent whose chapter ability resolved.
 * @property controllerId The Saga's controller (used to scope "Saga you control" triggers).
 * @property chapterNumber Which chapter ability resolved.
 * @property finalChapterNumber The Saga's highest chapter number.
 * @property isFinalChapter Whether [chapterNumber] is the final chapter.
 */
@Serializable
@SerialName("SagaChapterResolvedEvent")
data class SagaChapterResolvedEvent(
    val sagaId: EntityId,
    val controllerId: EntityId,
    val chapterNumber: Int,
    val finalChapterNumber: Int,
    val isFinalChapter: Boolean
) : GameEvent

/**
 * An ability fizzled (all targets became invalid).
 */
@Serializable
@SerialName("AbilityFizzledEvent")
data class AbilityFizzledEvent(
    val sourceId: EntityId,
    val description: String,
    val reason: String
) : GameEvent

/**
 * An optional ("you may") ability's may-question was resolved automatically from the controller's
 * persistent auto-answer yield instead of prompting (MTGO "Always yes/no" — backlog §C). Emitted so
 * the controller sees in the log that the system acted for them; it carries no rules effect of its
 * own (the chosen branch's own events follow when [answer] is true).
 */
@Serializable
@SerialName("AbilityAutoAnsweredEvent")
data class AbilityAutoAnsweredEvent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val answer: Boolean
) : GameEvent

/**
 * A player committed a crime (CR Outlaws of Thunder Junction). Emitted at the same time
 * as [SpellCastEvent], [AbilityActivatedEvent], or [AbilityTriggeredEvent] when at least
 * one initial target is an opponent, a permanent/spell/ability an opponent controls, or
 * a card in an opponent's graveyard. Emitted at most once per spell or ability.
 */
@Serializable
@SerialName("CommitCrimeEvent")
data class CommitCrimeEvent(
    val playerId: EntityId,
    val sourceEntityId: EntityId,
    val sourceName: String
) : GameEvent

/**
 * A player chose one or more targets. Emitted at the same time as [SpellCastEvent],
 * [AbilityActivatedEvent], or [AbilityTriggeredEvent] when the spell/ability has at least one
 * chosen target. Emitted at most once per spell or ability. [stackObjectId] is the spell/ability
 * on the stack (the trigger's triggering entity), so an effect resolving from a
 * "whenever a player chooses targets" trigger can read and change those targets (Psychic Battle).
 */
@Serializable
@SerialName("TargetsChosenEvent")
data class TargetsChosenEvent(
    val chooserId: EntityId,
    val stackObjectId: EntityId,
    val sourceName: String
) : GameEvent

// =============================================================================
// Combat Events
// =============================================================================

/**
 * Attackers were declared.
 */
@Serializable
@SerialName("AttackersDeclaredEvent")
data class AttackersDeclaredEvent(
    val attackers: List<EntityId>,
    val attackerNames: List<String> = emptyList(),
    val attackingPlayerId: EntityId? = null
) : GameEvent

/**
 * Blockers were declared.
 */
@Serializable
@SerialName("BlockersDeclaredEvent")
data class BlockersDeclaredEvent(
    val blockers: Map<EntityId, List<EntityId>>,  // blocker -> blocked attackers
    val blockerNames: Map<EntityId, String> = emptyMap(),
    val attackerNames: Map<EntityId, String> = emptyMap()
) : GameEvent

/**
 * Player ordered blockers for damage assignment.
 */
@Serializable
@SerialName("BlockerOrderDeclaredEvent")
data class BlockerOrderDeclaredEvent(
    val attackerId: EntityId,
    val orderedBlockers: List<EntityId>  // First in list receives damage first
) : GameEvent

/**
 * Attacking player ordered their attackers for a blocker's damage assignment.
 */
@Serializable
@SerialName("AttackerOrderDeclaredEvent")
data class AttackerOrderDeclaredEvent(
    val blockerId: EntityId,
    val orderedAttackers: List<EntityId>  // First in list receives damage first
) : GameEvent

/**
 * Combat damage was assigned.
 */
@Serializable
@SerialName("DamageAssignedEvent")
data class DamageAssignedEvent(
    val attackerId: EntityId,
    val assignments: Map<EntityId, Int>  // target -> damage amount
) : GameEvent

/**
 * A creature was goaded (CR 701.15). Emitted whenever a goader is newly added to
 * the creature's goader set — repeat goads by the same player are silent
 * (CR 701.15d) so this event fires at most once per (creature, goader) pair.
 */
@Serializable
@SerialName("CreatureGoadedEvent")
data class CreatureGoadedEvent(
    val creatureId: EntityId,
    val creatureName: String,
    val goaderId: EntityId,
    val goaderName: String
) : GameEvent

/**
 * A creature's goaded designation lapsed (CR 701.15a). Emitted at the start of
 * each goader's next turn as that player is removed from the creature's goader
 * set, with [stillGoadedByPlayerIds] listing any goaders still in effect after
 * the removal. When the set is empty, the [GoadedComponent] has been removed.
 */
@Serializable
@SerialName("CreatureNoLongerGoadedEvent")
data class CreatureNoLongerGoadedEvent(
    val creatureId: EntityId,
    val creatureName: String,
    val expiredGoaderId: EntityId,
    val stillGoadedByPlayerIds: Set<EntityId>
) : GameEvent

// =============================================================================
// Turn/Phase Events
// =============================================================================

/**
 * The phase changed.
 */
@Serializable
@SerialName("PhaseChangedEvent")
data class PhaseChangedEvent(
    val newPhase: Phase
) : GameEvent

/**
 * The step changed.
 */
@Serializable
@SerialName("StepChangedEvent")
data class StepChangedEvent(
    val newStep: Step
) : GameEvent

/**
 * The turn changed.
 */
@Serializable
@SerialName("TurnChangedEvent")
data class TurnChangedEvent(
    val turnNumber: Int,
    val activePlayerId: EntityId
) : GameEvent

/**
 * Priority changed to a player.
 */
@Serializable
@SerialName("PriorityChangedEvent")
data class PriorityChangedEvent(
    val playerId: EntityId
) : GameEvent

// =============================================================================
// Permanent Events
// =============================================================================

/**
 * A permanent was tapped.
 */
@Serializable
@SerialName("TappedEvent")
data class TappedEvent(
    val entityId: EntityId,
    val entityName: String
) : GameEvent

/**
 * A permanent became saddled (CR 702.171b) — a Saddle ability resolved. Lets animations and
 * "whenever this becomes saddled" triggers react instead of the state changing silently.
 *
 * [firstThisTurn] is true when the permanent was not already saddled when this Saddle ability
 * resolved — i.e. this is the first time it became saddled this turn. Saddle may be activated
 * again while a permanent is already saddled (CR 702.171a/b), and the SaddledComponent persists
 * until the cleanup step, so a second activation in the same turn reports false. Drives the
 * "becomes saddled for the first time each turn" intervening-if (Stubborn Burrowfiend).
 */
@Serializable
@SerialName("BecameSaddledEvent")
data class BecameSaddledEvent(
    val entityId: EntityId,
    val entityName: String,
    val firstThisTurn: Boolean = true
) : GameEvent

/**
 * An Aura, Equipment, or Fortification became attached to a permanent (CR 603.2e). Emitted only
 * at the moment of attaching — when the attachment moves onto a new host — not when an
 * already-attached state persists, and not on phasing in/out (CR 702.26j). Emitted from every
 * attach site: aura ETB onto its enchant target (StackResolver), equip resolution
 * (AttachEquipmentExecutor), and an aura moved onto the battlefield attached by an effect
 * (MoveCollectionExecutor).
 *
 * Drives the "becomes attached" trigger family
 * ([com.wingedsheep.sdk.scripting.EventPattern.BecomesAttachedEvent]): Assimilation Aegis
 * ("whenever this Equipment becomes attached to a creature") and Eriette, the Beguiler
 * ("whenever an Aura you control becomes attached to a … permanent an opponent controls").
 *
 * @property attachmentId the aura/equipment that became attached (the triggering entity).
 * @property attachedToId the permanent it became attached to.
 * @property controllerId the controller of the attachment.
 */
@Serializable
@SerialName("PermanentAttachedEvent")
data class PermanentAttachedEvent(
    val attachmentId: EntityId,
    val attachmentName: String,
    val attachedToId: EntityId,
    val controllerId: EntityId,
) : GameEvent

/**
 * A player tapped a land for mana (a land's mana ability resolved).
 *
 * Drives the "Whenever a player taps a land for mana" trigger family
 * ([com.wingedsheep.sdk.scripting.EventPattern.LandTappedForMana]). Emitted only on the manual
 * mana-ability activation path; automatic cost payment adds mana via the solver without emitting
 * this event.
 */
@Serializable
@SerialName("LandTappedForManaEvent")
data class LandTappedForManaEvent(
    val tapperId: EntityId,
    val landId: EntityId,
    val landName: String
) : GameEvent

/**
 * A permanent was untapped.
 */
@Serializable
@SerialName("UntappedEvent")
data class UntappedEvent(
    val entityId: EntityId,
    val entityName: String
) : GameEvent

/**
 * A permanent phased out (Rule 702.26).
 */
@Serializable
@SerialName("PhasedOutEvent")
data class PhasedOutEvent(
    val entityId: EntityId,
    val entityName: String
) : GameEvent

/**
 * A permanent phased in (Rule 702.26).
 */
@Serializable
@SerialName("PhasedInEvent")
data class PhasedInEvent(
    val entityId: EntityId,
    val entityName: String
) : GameEvent

/**
 * Counters were added to a permanent.
 */
@Serializable
@SerialName("CountersAddedEvent")
data class CountersAddedEvent(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int,
    val entityName: String = "",
    /**
     * True when this is the first counter placement on [entityId] this turn. Drives
     * "first time counters have been put on that creature this turn" intervening-if triggers
     * (Stalwart Successor). Computed against the target's [ReceivedCountersThisTurnComponent]
     * before that marker is set; defaults to false for emitters that don't track it.
     */
    val firstThisTurn: Boolean = false
) : GameEvent

/**
 * Counters were removed from a permanent.
 */
@Serializable
@SerialName("CountersRemovedEvent")
data class CountersRemovedEvent(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int,
    val entityName: String = ""
) : GameEvent

/**
 * Loyalty on a planeswalker changed (due to ability activation).
 */
@Serializable
@SerialName("LoyaltyChangedEvent")
data class LoyaltyChangedEvent(
    val entityId: EntityId,
    val entityName: String,
    val change: Int
) : GameEvent

// =============================================================================
// Card Events
// =============================================================================

/**
 * Cards were drawn.
 */
@Serializable
@SerialName("CardsDrawnEvent")
data class CardsDrawnEvent(
    val playerId: EntityId,
    val count: Int,
    val cardIds: List<EntityId>,
    val cardNames: List<String> = emptyList()
) : GameEvent

/**
 * A card was revealed from the first draw of a turn.
 * Emitted when a permanent with RevealFirstDrawEachTurn is on the battlefield
 * and the controller draws their first card of a turn.
 */
@Serializable
@SerialName("CardRevealedFromDrawEvent")
data class CardRevealedFromDrawEvent(
    val playerId: EntityId,
    val cardEntityId: EntityId,
    val cardName: String,
    val isCreature: Boolean
) : GameEvent

/**
 * A player failed to draw (empty library).
 */
@Serializable
@SerialName("DrawFailedEvent")
data class DrawFailedEvent(
    val playerId: EntityId,
    val reason: String
) : GameEvent

/**
 * Cards were discarded.
 */
@Serializable
@SerialName("CardsDiscardedEvent")
data class CardsDiscardedEvent(
    val playerId: EntityId,
    val cardIds: List<EntityId>,
    val cardNames: List<String> = emptyList()
) : GameEvent

/**
 * A player needs to discard cards during cleanup.
 */
@Serializable
@SerialName("DiscardRequiredEvent")
data class DiscardRequiredEvent(
    val playerId: EntityId,
    val count: Int
) : GameEvent

/**
 * Library was shuffled.
 */
@Serializable
@SerialName("LibraryShuffledEvent")
data class LibraryShuffledEvent(
    val playerId: EntityId
) : GameEvent

/**
 * Permanents were sacrificed.
 */
@Serializable
@SerialName("PermanentsSacrificedEvent")
data class PermanentsSacrificedEvent(
    val playerId: EntityId,
    val permanentIds: List<EntityId>,
    val permanentNames: List<String> = emptyList()
) : GameEvent

// =============================================================================
// Class Level Events
// =============================================================================

/**
 * A Class enchantment gained a new level.
 * Used to fire "When this Class becomes level N" triggers.
 */
@Serializable
@SerialName("ClassLevelChangedEvent")
data class ClassLevelChangedEvent(
    val entityId: EntityId,
    val newLevel: Int,
    val controllerId: EntityId
) : GameEvent

// =============================================================================
// Decision Events
// =============================================================================

/**
 * The engine paused and is awaiting a decision.
 */
@Serializable
@SerialName("DecisionRequestedEvent")
data class DecisionRequestedEvent(
    val decisionId: String,
    val playerId: EntityId,
    val decisionType: String,
    val prompt: String
) : GameEvent

/**
 * A player submitted a decision response.
 */
@Serializable
@SerialName("DecisionSubmittedEvent")
data class DecisionSubmittedEvent(
    val decisionId: String,
    val playerId: EntityId,
    /** Human-readable description of what was decided, for the game log */
    val description: String? = null
) : GameEvent

// =============================================================================
// Game State Events
// =============================================================================

/**
 * The game ended.
 */
@Serializable
@SerialName("GameEndedEvent")
data class GameEndedEvent(
    val winnerId: EntityId?,
    val reason: GameEndReason
) : GameEvent

@Serializable
enum class GameEndReason {
    LIFE_ZERO,
    DECK_EMPTY,
    POISON_COUNTERS,
    CONCESSION,
    ALTERNATIVE_WIN,
    CARD_EFFECT,
    DRAW,
    /** Commander format: 21+ combat damage from a single commander (CR 903.10a). */
    COMMANDER_DAMAGE,
    /** Two-Headed Giant: a player lost with their team (CR 810.8a). */
    TEAM_DEFEATED,
    /** Rule 104.4c — SBAs never stabilized, treated as an unbreakable infinite loop. */
    INFINITE_LOOP,
    UNKNOWN
}

/**
 * A player lost the game.
 */
@Serializable
@SerialName("PlayerLostEvent")
data class PlayerLostEvent(
    val playerId: EntityId,
    val reason: GameEndReason,
    val message: String? = null
) : GameEvent

/**
 * A player left the game and their "leaving the game" processing (CR 800.4a–c) was
 * applied: all objects they owned left the game, their stack objects were removed, and
 * control effects involving them ended. In a multiplayer pod the game continues for the
 * remaining players. [removedObjectCount] is informational (for logs / animation).
 */
@Serializable
@SerialName("PlayerLeftGameEvent")
data class PlayerLeftGameEvent(
    val playerId: EntityId,
    val reason: GameEndReason,
    val removedObjectCount: Int
) : GameEvent

// =============================================================================
// Creature Events
// =============================================================================

/**
 * A creature was destroyed.
 */
@Serializable
@SerialName("CreatureDestroyedEvent")
data class CreatureDestroyedEvent(
    val entityId: EntityId,
    val name: String,
    val reason: String,
    val controllerId: EntityId? = null
) : GameEvent

// =============================================================================
// Mana Events
// =============================================================================

/**
 * Mana was added to a player's pool.
 */
@Serializable
@SerialName("ManaAddedEvent")
data class ManaAddedEvent(
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0
) : GameEvent {
    val total: Int get() = white + blue + black + red + green + colorless
}

/**
 * Mana was spent from a player's pool.
 */
@Serializable
@SerialName("ManaSpentEvent")
data class ManaSpentEvent(
    val playerId: EntityId,
    val reason: String,
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0
) : GameEvent {
    val total: Int get() = white + blue + black + red + green + colorless
}

// =============================================================================
// Information Events
// =============================================================================

/**
 * A player looked at another player's hand.
 * The cards in the hand are now revealed to the viewing player.
 */
@Serializable
@SerialName("HandLookedAtEvent")
data class HandLookedAtEvent(
    val viewingPlayerId: EntityId,
    val targetPlayerId: EntityId,
    val cardIds: List<EntityId>
) : GameEvent

/**
 * A player revealed their hand to all players.
 * Unlike HandLookedAtEvent, this reveals cards publicly to everyone.
 */
@Serializable
@SerialName("HandRevealedEvent")
data class HandRevealedEvent(
    val revealingPlayerId: EntityId,
    val cardIds: List<EntityId>
) : GameEvent

/**
 * A player revealed specific cards to all players.
 * Used for tutor effects that require revealing the chosen card.
 */
@Serializable
@SerialName("CardsRevealedEvent")
data class CardsRevealedEvent(
    val revealingPlayerId: EntityId,
    val cardIds: List<EntityId>,
    val cardNames: List<String>,
    val imageUris: List<String?> = emptyList(),
    val source: String? = null,
    /**
     * Owner of each revealed card, parallel to [cardIds]. Populated when one reveal spans
     * cards from more than one player (e.g. Psychic Battle: each player reveals their top card)
     * so the UI can attribute each card to its revealer. Empty for single-owner reveals.
     */
    val cardOwnerIds: List<EntityId> = emptyList(),
    /** If false, the revealing player does not see the reveal overlay (e.g., behold from hand) */
    val revealToSelf: Boolean = true,
    /**
     * Optional zone transition context. When the reveal represents a card moving
     * between zones (e.g., graveyard → hand via Morcant's Loyalist), the UI can
     * use these to render an explanatory message like
     * "Returned from graveyard to hand — <source>" instead of the generic "Revealed".
     */
    val fromZone: com.wingedsheep.sdk.core.Zone? = null,
    val toZone: com.wingedsheep.sdk.core.Zone? = null
) : GameEvent

/**
 * A player looked at cards (from library, etc.).
 * Used for "look at the top N cards" effects.
 */
@Serializable
@SerialName("LookedAtCardsEvent")
data class LookedAtCardsEvent(
    val playerId: EntityId,
    val cardIds: List<EntityId>,
    val source: String? = null
) : GameEvent

/**
 * A player reordered cards on top of their library.
 * Used for effects like Omen ("put them back in any order").
 */
@Serializable
@SerialName("LibraryReorderedEvent")
data class LibraryReorderedEvent(
    val playerId: EntityId,
    val cardCount: Int,
    val source: String? = null
) : GameEvent

// =============================================================================
// Morph Events
// =============================================================================

/**
 * A face-down creature was turned face up.
 */
@Serializable
@SerialName("TurnFaceUpEvent")
data class TurnFaceUpEvent(
    val entityId: EntityId,
    val cardName: String,
    val controllerId: EntityId,
    val xValue: Int? = null
) : GameEvent

/**
 * A creature was turned face down (e.g., by Backslide).
 */
@Serializable
@SerialName("TurnedFaceDownEvent")
data class TurnedFaceDownEvent(
    val entityId: EntityId,
    val controllerId: EntityId
) : GameEvent

/**
 * A double-faced permanent transformed (CR 701.27).
 *
 * [intoBackFace] is true when the permanent transformed from its front face to its back face,
 * and false when it transformed from its back face to its front face.
 * [newFaceName] is the name of the face that is now up after transform.
 */
@Serializable
@SerialName("TransformedEvent")
data class TransformedEvent(
    val entityId: EntityId,
    val intoBackFace: Boolean,
    val newFaceName: String,
    val controllerId: EntityId
) : GameEvent

// =============================================================================
// Control Events
// =============================================================================

/**
 * Control of a permanent changed.
 */
@Serializable
@SerialName("ControlChangedEvent")
data class ControlChangedEvent(
    val permanentId: EntityId,
    val permanentName: String,
    val oldControllerId: EntityId,
    val newControllerId: EntityId
) : GameEvent

// =============================================================================
// Targeting Events
// =============================================================================

/**
 * A permanent or spell became the target of a spell or ability.
 * [firstTimeByThisController] indicates whether this is the first time this turn
 * the target was targeted by a spell/ability controlled by [controllerId].
 * Used for Valiant triggers ("for the first time each turn").
 * [targetIsSpell] is true when the targeted object is a spell on the stack rather
 * than a permanent on the battlefield (Rule 601.2c). Lets triggers that fire on a
 * "creature spell you control" being targeted (e.g. Surrak, Elusive Hunter) match,
 * while battlefield-only triggers (ward) never see spell targets because they are
 * generated only from permanents.
 */
@Serializable
@SerialName("BecomesTargetEvent")
data class BecomesTargetEvent(
    val targetEntityId: EntityId,
    val targetName: String,
    val sourceEntityId: EntityId,
    val controllerId: EntityId,
    val firstTimeByThisController: Boolean = true,
    val targetIsSpell: Boolean = false,
    /** True when the targeting source is a spell on the stack (vs. an activated/triggered ability). */
    val sourceIsSpell: Boolean = false
) : GameEvent

// =============================================================================
// Cycling Events
// =============================================================================

/**
 * A player cycled a card.
 */
@Serializable
@SerialName("CardCycledEvent")
data class CardCycledEvent(
    val playerId: EntityId,
    val cardId: EntityId,
    val cardName: String
) : GameEvent

// =============================================================================
// Plot Events
// =============================================================================

/**
 * A player plotted a card (Outlaws of Thunder Junction).
 *
 * Fires when the plot special action resolves — the plot cost was paid and the
 * card was exiled face-up from hand. The card is now marked plotted and may be
 * cast from exile on a later turn without paying its mana cost.
 */
@Serializable
@SerialName("CardPlottedEvent")
data class CardPlottedEvent(
    val playerId: EntityId,
    val cardId: EntityId,
    val cardName: String
) : GameEvent

// =============================================================================
// Gift Events
// =============================================================================

/**
 * A player gave a gift (Bloomburrow gift mechanic).
 * Emitted when a gift mode is chosen and the gift effect resolves.
 *
 * @property controllerId The player who gave the gift
 * @property sourceId The card/spell that provided the gift
 * @property sourceName The name of the source card
 */
@Serializable
data class GiftGivenEvent(
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : GameEvent

// =============================================================================
// Coin Flip Events
// =============================================================================

/**
 * A player flipped a coin.
 *
 * @property playerId The player who flipped the coin
 * @property won Whether the player won the flip
 * @property sourceId The entity that caused the coin flip
 * @property sourceName The name of the card/ability that caused the coin flip
 */
@Serializable
@SerialName("CoinFlipEvent")
data class CoinFlipEvent(
    val playerId: EntityId,
    val won: Boolean,
    val sourceId: EntityId,
    val sourceName: String
) : GameEvent

/**
 * Emitted when a player has been scheduled to control another player's next turn
 * (Mindslaver-style hijack). PR 1 ships this as informational only — the full
 * input/visibility routing arrives in a follow-up PR.
 */
@Serializable
@SerialName("TurnHijackedEvent")
data class TurnHijackedEvent(
    val controllerId: EntityId,
    val hijackedPlayerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String
) : GameEvent

// =============================================================================
// Room Events (DSK)
// =============================================================================

/**
 * A player fully unlocked a Room (Duskmourn mechanic).
 * Emitted when both doors of a Room permanent have been unlocked.
 * Used to trigger Eerie abilities.
 */
@Serializable
@SerialName("RoomFullyUnlockedEvent")
data class RoomFullyUnlockedEvent(
    val roomId: EntityId,
    val roomName: String,
    val controllerId: EntityId
) : GameEvent

/**
 * A door of a Room was given the "unlocked" designation (CR 709.5h). Fires both for the
 * cast face's ETB unlock and for the unlock special action; either way, the Room's
 * face-scoped "When you unlock this door" abilities see this event.
 */
@Serializable
@SerialName("DoorUnlockedEvent")
data class DoorUnlockedEvent(
    val roomId: EntityId,
    val roomName: String,
    val faceId: com.wingedsheep.engine.state.components.identity.RoomFaceId,
    val faceName: String,
    val controllerId: EntityId,
    /**
     * True when this unlock transition completes the Room (the second door becoming
     * unlocked while the other is already unlocked). The handler also emits a separate
     * [RoomFullyUnlockedEvent] in this case for Eerie matching.
     */
    val becameFullyUnlocked: Boolean
) : GameEvent
