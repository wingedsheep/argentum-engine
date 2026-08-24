package com.wingedsheep.engine.core

import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.TapReason
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
     * Frozen last-known information (CR 113.7a / 603.10 / 608.2h) captured the instant this
     * permanent left the battlefield: projected power/toughness, counters, keywords, type line,
     * controller, the aura host it was attached to, combat pairings, token-ness, per-player damage,
     * damage sources, and lost-abilities. `null` for any transition that did not leave the
     * battlefield (entries, library→graveyard mill, etc.), so readers fall back to live state.
     *
     * This replaces what were ~16 parallel `lastKnown*` scalar fields. See [EntitySnapshot] for the
     * per-field documentation, and its derived `plusOnePlusOneCounters` / `minusOneMinusOneCounters`
     * / `totalCounters` accessors for the former counter-count scalars.
     */
    val lastKnown: com.wingedsheep.engine.state.components.stack.EntitySnapshot? = null,
    /** The original card name when this permanent entered as a copy (e.g., "Clever Impersonator") */
    val copyOfOriginalName: String? = null,
    /**
     * The `{X}` associated with this object's zone change. On entry to the battlefield it is the X
     * of the spell that put the permanent there (for ETB triggers using `DynamicAmount.XValue`). On
     * leaving the battlefield it is the cast-time X carried by `CastChoicesComponent`, captured as
     * last-known information so dies/leaves triggers can still read `DynamicAmount.CastX`. Kept off
     * [lastKnown] because, unlike that leave-only snapshot, it is also populated on entry.
     */
    val xValue: Int? = null,
    /**
     * True when this battlefield exit was a sacrifice (CR 701.21). Lets leaves/dies triggers
     * distinguish a sacrifice from any other way a permanent is put into the graveyard —
     * e.g. Urza's Miter ("if it wasn't sacrificed"). Always `false` for non-battlefield exits and
     * for non-sacrifice deaths (destruction, lethal damage, state-based actions).
     */
    val wasSacrificed: Boolean = false,
    /**
     * True when this battlefield exit was the exile of a material chosen to pay a Craft cost
     * (CR 702.167). Lets a SELF exile trigger ("When this creature is exiled from the battlefield
     * while you're activating a craft ability", Market Gnome) distinguish craft-material exile
     * from any other way a permanent is exiled. Always `false` for non-exile exits and for exiles
     * that are not craft materials (removal, the crafted card's own self-exile, etc.).
     */
    val craftMaterial: Boolean = false
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
    val targetToughnessAtDamage: Int? = null,
    /**
     * The damage source's chosen targets at the instant damage was dealt. Resolving spells lose
     * their TargetsComponent before event-trigger detection, so target/recipient relationship
     * predicates consume this event-side snapshot instead of consulting later state.
     */
    val sourceTargetIdsAtDamage: List<EntityId>? = null
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
 * A land was **played** (the special land-play action, CR 305.1) — distinct from a land an effect
 * puts onto the battlefield (which emits only a [ZoneChangeEvent]). Emitted by
 * `PlayLandHandler` alongside the entry [ZoneChangeEvent], carrying the zone it was played from so
 * "whenever you play a land … from anywhere other than your hand" triggers (Shadow of the Goblin)
 * can gate on [fromZone].
 *
 * @property cardId The land that was played
 * @property controllerId The player who played it
 * @property fromZone The zone it was played from (HAND for a normal land drop; GRAVEYARD / EXILE /
 *   LIBRARY for a land played via a play permission)
 */
@Serializable
@SerialName("LandPlayedEvent")
data class LandPlayedEvent(
    val cardId: EntityId,
    val controllerId: EntityId,
    val fromZone: com.wingedsheep.sdk.core.Zone
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
 * A player gained the city's blessing (CR 702.131).
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
 * A player gained an enduring story (The Hobbit, CR 702.195).
 *
 * Fired by the storied state-based action the first time its controller controls three or more
 * permanents that are artifacts, Sagas, and/or legendary. The designation is permanent for the rest
 * of the game (CR 702.195a), so this event fires at most once per player per game.
 */
@Serializable
@SerialName("EnduringStoryGainedEvent")
data class EnduringStoryGainedEvent(
    val playerId: EntityId,
    val playerName: String,
    val sourceName: String
) : GameEvent

/**
 * A player's speed changed (Aetherdrift, CR 702.179).
 *
 * Fired both by the CR 704.5aa state-based action that starts a speed at 1 ([oldSpeed] = 0) and by
 * every later increase. Speed only ever rises and never past
 * [com.wingedsheep.sdk.core.Speed.MAX], so [newSpeed] > [oldSpeed] always holds and reaching
 * [com.wingedsheep.sdk.core.Speed.MAX] fires at most once per player per game — which is what lets
 * the client animate "max speed reached" off this event alone.
 *
 * @param sourceName The permanent or ability that raised the speed; "Start your engines!" for the
 *   state-based action, which has no source object.
 */
@Serializable
@SerialName("SpeedChangedEvent")
data class SpeedChangedEvent(
    val playerId: EntityId,
    val playerName: String,
    val oldSpeed: Int,
    val newSpeed: Int,
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
 * A player's maximum hand size was reduced for the rest of the game (Inspired Idea,
 * "Your maximum hand size is reduced by three for the rest of the game"). [amount] is the
 * amount this application reduced by; [newReductionTotal] is the accumulated reduction after it.
 * Permanent — fires once per application (repeat casts stack).
 */
@Serializable
@SerialName("MaximumHandSizeReducedEvent")
data class MaximumHandSizeReducedEvent(
    val playerId: EntityId,
    val playerName: String,
    val amount: Int,
    val newReductionTotal: Int,
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
 * A player just finished a `scry N` (CR 701.22). Fires once per scry, after the
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
 * A player just finished a `surveil N` (CR 701.25). Fires once per surveil, after the
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
 * A player just finished a `discover N` (CR 701.55). Fires once per discover, after the whole
 * process — including the "cast for free or put into hand" decision — resolves (CR 701.55b). Drives
 * "Whenever you discover" triggers; see [com.wingedsheep.sdk.scripting.EventPattern.DiscoveredEvent].
 *
 * @property playerId The player who discovered.
 * @property value The discover threshold N used (the mana value, not the number of cards exiled).
 *   Surfaced via `TRIGGER_DISCOVER_VALUE` so "discover again for the same value" payoffs can reuse it.
 * @property sourceName The card/ability that caused the discover (for display).
 */
@Serializable
@SerialName("DiscoveredEvent")
data class DiscoveredEvent(
    val playerId: EntityId,
    val value: Int,
    val sourceName: String
) : GameEvent

/**
 * A player just collected evidence (CR 701.57). Fires once per collection, after the chosen cards
 * have all been exiled — never when the collection was declined or was impossible (CR 701.57b), so
 * a payoff can trust that evidence genuinely changed hands. Drives "Whenever you collect evidence"
 * triggers (Surveillance Monitor, Evidence Examiner); see
 * [com.wingedsheep.sdk.scripting.EventPattern.EvidenceCollectedEvent].
 *
 * Emitted from the single payment implementation shared by every context the mechanic appears in —
 * activated-ability cost, cast-time additional cost, ward cost, and the resolution-time
 * [com.wingedsheep.sdk.scripting.effects.CollectEvidenceEffect] — so no context can collect evidence
 * without the payoffs seeing it.
 *
 * @property playerId The player who collected evidence (and whose graveyard was spent).
 * @property value The threshold N that was met — the *required* total, not the total actually
 *   exiled, which may be higher since the player may exile more than needed (CR 701.57a).
 * @property exiledCards The cards exiled to collect, in selection order.
 * @property totalManaValue The combined mana value actually exiled; always >= [value].
 * @property sourceName The card/ability that caused the collection (for display).
 */
@Serializable
@SerialName("EvidenceCollectedEvent")
data class EvidenceCollectedEvent(
    val playerId: EntityId,
    val value: Int,
    val exiledCards: List<EntityId>,
    val totalManaValue: Int,
    val sourceName: String
) : GameEvent

/**
 * A player just foraged (CR 701.59a — "Exile three cards from your graveyard or sacrifice a Food").
 * Fires once per forage, after the three cards are exiled or the Food is sacrificed — never for a
 * declined forage and never for one no mode was feasible for, since forage has no "even if you
 * can't" clause. Drives "Whenever you forage" triggers (Corpseberry Cultivator); see
 * [com.wingedsheep.sdk.scripting.EventPattern.ForagedEvent].
 *
 * Emitted from **two** places, which is one more than [EvidenceCollectedEvent] needs and the reason
 * worth recording: forage's three *cost* contexts share one payment implementation
 * (`ForageCostResolver.pay`) and emit it there, while its *effect* form lowers to generic
 * gather/select/move and sacrifice effects with no forage-shaped executor to emit from — so that one
 * carries the [com.wingedsheep.sdk.scripting.effects.ForagedEffect] marker instead. Waterbend is
 * split the same way. A third path added later must pick one of the two.
 *
 * @property playerId The player who foraged — whoever *paid*, which need not be the source's
 *   controller: Feed the Cycle can be cast by either player and a ward cost is paid by an opponent.
 * @property sourceName The card/ability that caused the forage (for display), null when unknown.
 */
@Serializable
@SerialName("ForagedEvent")
data class ForagedEvent(
    val playerId: EntityId,
    val sourceName: String? = null
) : GameEvent

/**
 * A permanent just explored (CR 701.44). Fires once per explore, after the reveal + hand/counter
 * resolution is determined. Drives [com.wingedsheep.sdk.scripting.EventPattern.ExploredEvent]
 * triggers ("whenever a creature you control explores [a land / nonland card]").
 *
 * @property exploringPermanentId The permanent that explored (the trigger's subject).
 * @property controllerId The exploring permanent's controller at explore time.
 * @property revealedCardWasLand `true` if the revealed card was a land, `false` if a nonland,
 *   `null` if no card was revealed (empty library — the permanent still explored per CR 701.44b).
 *   Gates the `LAND` / `NONLAND` reveal-type triggers; `ANY` matches regardless.
 * @property sourceName The card/ability that caused the explore (for display).
 */
@Serializable
@SerialName("PermanentExploredEvent")
data class PermanentExploredEvent(
    val exploringPermanentId: EntityId,
    val controllerId: EntityId,
    val revealedCardWasLand: Boolean?,
    val sourceName: String? = null
) : GameEvent

/**
 * A permanent just connived (CR 701.50). Emitted once per connive as the tail of the connive
 * pipeline — after the discard decision and the +1/+1 counter — so it lands in a completed
 * resolution batch. Per CR 701.50f the permanent connives even if some or all of the draw/discard
 * was impossible, so this fires on an empty hand or an empty library too. Drives
 * [com.wingedsheep.sdk.scripting.EventPattern.ConnivedEvent] triggers ("whenever a creature you
 * control connives").
 *
 * Internal-only: dropped from the client log (`ClientEventTransformer`) — the draw, discard and
 * counter are each surfaced by their own events already.
 *
 * @property connivingPermanentId The permanent that connived (the trigger's subject).
 * @property controllerId The connive effect's controller at connive time.
 * @property sourceName The card/ability that caused the connive (for display).
 */
@Serializable
@SerialName("PermanentConnivedEvent")
data class PermanentConnivedEvent(
    val connivingPermanentId: EntityId,
    val controllerId: EntityId,
    val sourceName: String? = null
) : GameEvent

/**
 * A player just performed one of the four elemental bending keyword actions (CR 701.65b Airbend /
 * 701.66b Earthbend / 701.67c Waterbend / 702.189b Firebending). Fires once per bend so
 * [com.wingedsheep.sdk.scripting.EventPattern.BendPerformedEvent] triggers
 * ([com.wingedsheep.sdk.dsl.Triggers.YouBend]) match. Emitted alongside a fold of [bendType] into
 * the player's `BendsThisTurnComponent` (see `BendEvents.record`). Internal-only: dropped from the
 * client log (`ClientEventTransformer`).
 *
 * @property playerId The player who bent.
 * @property bendType Which of the four elemental actions was performed.
 */
@Serializable
@SerialName("BendPerformedEvent")
data class BendPerformedEvent(
    val playerId: EntityId,
    val bendType: BendType
) : GameEvent

/**
 * A player just finished a `manifest dread` (CR 701.60). Fires once per manifest-dread, after the
 * chosen card has been manifested face down and the other card(s) put into the graveyard. Drives
 * "Whenever you manifest dread" triggers; see
 * [com.wingedsheep.sdk.scripting.EventPattern.ManifestedDreadEvent]. Per CR 701.60b it fires even
 * when the library held fewer than two cards.
 *
 * @property playerId The player who manifested dread.
 * @property graveyardCardIds The card(s) put into the graveyard this way (the looked-at cards that
 *   were not manifested). Carried so a payoff that references "a card you put into your graveyard
 *   this way" (Paranormal Analyst) can move it out — seeded into the trigger's pipeline under
 *   `PipelineState.TRIGGER_CAPTURED_COLLECTION` via [TriggerContext.capturedEntityIds]. Empty when
 *   the library held fewer than two cards.
 * @property sourceName The card/ability that caused the manifest dread (for display).
 */
@Serializable
@SerialName("ManifestedDreadEvent")
data class ManifestedDreadEvent(
    val playerId: EntityId,
    val graveyardCardIds: List<EntityId>,
    val sourceName: String
) : GameEvent

/**
 * A player just searched their library (CR 701.23). Fires once per search, after the found
 * cards have been moved and the library shuffled. Drives "Whenever a player searches their
 * library" triggers (Wan Shi Tong, Librarian); see
 * [com.wingedsheep.sdk.scripting.EventPattern.SearchLibraryEvent]. Searching is the act of looking
 * through the zone (CR 701.23a) and finding a card is not required (CR 701.23b), so any ability that
 * triggers on a library being searched still fires even when no card was found.
 *
 * @property playerId The player whose library was searched (the searcher).
 * @property sourceName The card/ability that caused the search (for display).
 */
@Serializable
@SerialName("LibrarySearchedEvent")
data class LibrarySearchedEvent(
    val playerId: EntityId,
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
 * A creature type that had been chosen secretly is now public information — "Reveal the creature
 * type you chose" (A Killer Among Us), the reveal half of the hidden-agenda pair (CR 702.106c).
 *
 * Distinct from [CreatureTypeChosenEvent], which announces that a choice happened; this one
 * announces *what* the choice was, and fires once, when the reveal cost is paid.
 */
@Serializable
@SerialName("CreatureTypeRevealedEvent")
data class CreatureTypeRevealedEvent(
    val playerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val revealedType: String
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
    /**
     * Which optional-additional-cost mechanic this cast declared, or null when none — the fact
     * "whenever you cast a kicked spell" filters on (`SpellCastPredicate.WasKicked` matches
     * [ChoiceSlot.KICKED] only, so a bargained spell doesn't satisfy it).
     */
    val declaredCostSlot: ChoiceSlot? = null,
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
     * Producing-source subtypes of the mana spent on this cast (`Subtype.TREASURE` when any came
     * from a Treasure, `Subtype.CAVE` from a Cave, …). Drives SDK triggers built with
     * `Triggers.youCastSpell(requires = setOf(SpellCastPredicate.PaidWithManaFromSubtype(subtype)))`.
     * See [com.wingedsheep.engine.state.components.player.ManaPoolComponent.manaBySubtype].
     */
    val spentManaSubtypes: Set<com.wingedsheep.sdk.core.Subtype> = emptySet(),
    /**
     * Entity ids of the sources whose mana was spent on this cast. Drives
     * `SpellCastPredicate.PaidWithManaFromSource` ("cast … using mana produced by this land" —
     * Tecutlan, Barracks of the Thousand, The Myriad Pools).
     */
    val spentManaSourceIds: Set<EntityId> = emptySet(),
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
    val manaValue: Int = 0,
    /**
     * The zone this spell was cast from (CR 601.2a), or null when it couldn't be resolved. Purely
     * descriptive — triggers that care about the origin zone read
     * [com.wingedsheep.engine.state.CastSpellRecord] — but it is what lets the client say *where*
     * a spell came from instead of leaving every cast looking like a cast out of hand. See
     * [com.wingedsheep.engine.view.CastProvenance].
     */
    val castFromZone: Zone? = null,
    /**
     * Which alternative casting cost paid for this spell (CR 118.9), or null for a normal cast.
     * Mirrors [com.wingedsheep.engine.state.components.stack.SpellOnStackComponent.alternativeCost];
     * also descriptive only.
     */
    val alternativeCost: AlternativeCostType? = null,
    /**
     * Names of the permanents sacrificed to pay this cast's cost, as last known before they left
     * (CR 608.2h). Descriptive only, and only meaningful alongside [alternativeCost]: emerge
     * (CR 702.119a) prices itself off the sacrificed creature's mana value, so the log line has to
     * name the body or the reduced [totalManaSpent] looks arbitrary. See
     * [com.wingedsheep.engine.view.CastProvenance.logPhrase].
     */
    val sacrificedAsCostNames: List<String> = emptyList()
) : GameEvent

/**
 * An ability was activated.
 *
 * [costsTap] is true iff the activation cost includes the {T} symbol; [isManaAbility] is true for
 * mana abilities (CR 605.1). These let triggers distinguish the two "activates an ability" wordings:
 * "isn't a mana ability" (Flamescroll Celebrant — fired only for non-mana abilities, which use the
 * stack) versus "without {T} in its activation cost" (Antiquities Haunting Wind / Powerleech /
 * Artifact Possession — fired for any ability, mana or not, whose cost lacks {T}).
 */
@Serializable
@SerialName("AbilityActivatedEvent")
data class AbilityActivatedEvent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val abilityEntityId: EntityId? = null,
    val costsTap: Boolean = false,
    val isManaAbility: Boolean = false,
    val isExhaust: Boolean = false,
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
    val abilityEntityId: EntityId? = null,
    /**
     * True when this ability was put on the stack because its own source creature was declared as
     * an attacker — a per-attacker "whenever this creature attacks" ability (SELF-bound
     * [com.wingedsheep.sdk.scripting.EventPattern.AttackEvent]). Read by the
     * [com.wingedsheep.sdk.scripting.EventPattern.AbilityTriggeredEvent] `requireAttackCause`
     * pattern (Firebender Ascension). Defaults false for every other trigger and for ability copies
     * (which don't re-fire the meta-trigger).
     */
    val causedByAttack: Boolean = false
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
 * The "action" half of a `ReflexiveTriggerEffect` ("You may [action]. When you do, [reflexiveEffect]")
 * completed successfully. Per CR 603.12, "when you do" is a genuinely separate reflexive triggered
 * ability — this event is what [com.wingedsheep.engine.event.TriggerDetector]'s
 * `detectReflexiveTriggers` pass turns into a real [com.wingedsheep.engine.event.PendingTrigger], so
 * the reflexive half goes on the stack, gets its target chosen as it's placed there, and gets a real
 * priority round before it resolves — instead of resolving inline/atomically with no response window.
 *
 * @property carriedPipeline Pipeline state the action produced (e.g. `Amass`'s army reference, a
 * discard's resolved count) that the reflexive effect may read via `EntityReference`/
 * `VariableReference` — carried across the stack round-trip since the reflexive ability builds a
 * fresh [com.wingedsheep.engine.handlers.EffectContext] when it resolves.
 */
@Serializable
@SerialName("ReflexiveAbilityTriggeredEvent")
data class ReflexiveAbilityTriggeredEvent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val granterId: EntityId? = null,
    val reflexiveEffect: com.wingedsheep.sdk.scripting.effects.Effect,
    val reflexiveTargetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement> = emptyList(),
    val descriptionOverride: String? = null,
    val carriedPipeline: com.wingedsheep.engine.handlers.PipelineState = com.wingedsheep.engine.handlers.PipelineState.EMPTY,
    /**
     * The ORIGINAL ability's own trigger context (X value, triggering entity/player, damage/counter
     * amounts, etc.) — a reflexive effect (or its target filter) may reference any of these (e.g.
     * Fire Lord Sozin's "target creature card in the graveyard of the player dealt combat damage" or
     * Wildborn Preserver's "put X +1/+1 counters" reading the X the original ability's context held),
     * and the reflexive ability builds a brand-new `EffectContext` on resolve, so this must be
     * threaded across the stack round-trip rather than assumed to still be in scope.
     */
    val carriedTriggerContext: com.wingedsheep.engine.event.TriggerContext = com.wingedsheep.engine.event.TriggerContext()
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
 *
 * [firstTimeAttackers] is the subset of [attackers] that were declared as an attacker
 * *for the first time this turn* — i.e. they had not attacked in an earlier combat phase
 * this turn. It backs `AttackPredicate.FirstTimeEachTurn` ("attacks for the first time
 * each turn"): the matcher cannot derive this from post-declaration state because the
 * per-turn attacker set already includes the just-declared attacker by detection time, so
 * the "first time" fact is captured on the event at declaration (mirroring
 * `LifeChangeEvent.firstThisTurn` / `BecomesTargetEvent.firstTimeByThisController`).
 *
 * [attackersAgainstPlayer] is the subset of [attackers] declared as attacking a **player**
 * (CR 508.1), as opposed to a planeswalker or battle. It backs
 * `AttackPredicate.DefenderIsPlayer` ("attacks an opponent"): the defender kind is fixed at
 * declaration and the event doesn't otherwise carry per-attacker defender identity, so the
 * player-vs-permanent fact is stamped here rather than re-derived downstream.
 */
@Serializable
@SerialName("AttackersDeclaredEvent")
data class AttackersDeclaredEvent(
    val attackers: List<EntityId>,
    val attackerNames: List<String> = emptyList(),
    val attackingPlayerId: EntityId? = null,
    val firstTimeAttackers: Set<EntityId> = emptySet(),
    val attackersAgainstPlayer: Set<EntityId> = emptySet()
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
 * The game's day/night designation changed (CR 731). [oldDesignation] is `null` when the game gains
 * a designation from the "neither" state it starts in (CR 731.1); a non-null → non-null change is a
 * "day becomes night" / "night becomes day" flip (CR 731.1a). [sourceName] attributes the change —
 * the untap-step turn-based action (CR 502.2), a daybound/nightbound keyword (CR 702.145d/g), or an
 * effect such as Into the Night.
 */
@Serializable
@SerialName("DayNightChangedEvent")
data class DayNightChangedEvent(
    val oldDesignation: com.wingedsheep.sdk.core.DayNight?,
    val newDesignation: com.wingedsheep.sdk.core.DayNight,
    val sourceName: String
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
 *
 * @property tappedById the player who tapped it — the controller of the spell, ability, or cost
 *   payment that caused the permanent to become tapped. A permanent tapped as a turn-based action
 *   (declaring it as an attacker) or to pay its own controller's cost is tapped by its controller,
 *   which is why [com.wingedsheep.engine.core.tap] defaults this to the permanent's controller
 *   rather than leaving it unattributed. Drives the "whenever **you tap** a creature an opponent
 *   controls" trigger family ([com.wingedsheep.sdk.scripting.EventPattern.TapEvent.tapper]); null
 *   only for a permanent with no controller at all.
 * @property reason *why* it became tapped, for triggers that name a cause ("becomes tapped to pay a
 *   teamwork cost" — [com.wingedsheep.sdk.scripting.EventPattern.TapEvent.reason]). Orthogonal to
 *   [tappedById], which says *who* tapped it: a teamwork tap and an attack tap are both performed by
 *   the permanent's own controller. Defaults to [TapReason.UNSPECIFIED] — only the tap sites the
 *   engine has been taught to classify name a cause, and an unclassified tap must never masquerade
 *   as a classified one. The default also lets an event serialized before the field existed decode
 *   unchanged.
 * @property firstThisTurn true when this is the **first time this permanent became tapped this
 *   turn** — the per-permanent rider
 *   ([com.wingedsheep.sdk.scripting.EventPattern.TapEvent.firstTimeEachTurn], Captain America,
 *   Living Legend). Computed against the permanent's
 *   [com.wingedsheep.engine.state.components.battlefield.HasBecomeTappedComponent] stamp
 *   *before* [com.wingedsheep.engine.core.tap] records this tap, so the tap that sets the stamp is
 *   itself reported as the first one. A permanent that entered the battlefield tapped never became
 *   tapped (CR 701.26a), so it carries no stamp and its first real tap that turn reports true.
 *   Defaults to true, matching [BecameSaddledEvent.firstThisTurn]: a hand-constructed or
 *   previously-serialized event names no earlier tap.
 */
@Serializable
@SerialName("TappedEvent")
data class TappedEvent(
    val entityId: EntityId,
    val entityName: String,
    val tappedById: EntityId? = null,
    val reason: TapReason = TapReason.UNSPECIFIED,
    val firstThisTurn: Boolean = true
) : GameEvent

/**
 * A permanent was exerted (CR 701.43a) — it won't untap during its controller's next untap step.
 * Lets animations and "whenever you exert a permanent" reactions (none printed yet) fire instead
 * of the state changing silently.
 */
@Serializable
@SerialName("ExertedEvent")
data class ExertedEvent(
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
 * A Case became solved (CR 719.3b) — its "To solve" trigger resolved with the condition met.
 * Fires once per permanent: the designation is sticky, and the trigger's intervening-if stops an
 * already-solved Case from triggering again, so there is no "already solved" flag to carry.
 *
 * Emitted so the log and the client can show the moment a Case flips, and so "whenever a Case you
 * control becomes solved" payoffs have an event to match.
 */
@Serializable
@SerialName("CaseSolvedEvent")
data class CaseSolvedEvent(
    val entityId: EntityId,
    val entityName: String,
    /**
     * The player who solved it — the Case's controller at the moment the "To solve" trigger
     * resolved. Carried on the event rather than looked up from the Case, because a "whenever you
     * solve a Case" payoff (Case File Auditor) is matched after the fact, when the Case may already
     * have been sacrificed by its own Solved ability.
     */
    val controllerId: EntityId
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
 * An Aura/Equipment became unattached from its host (CR 701.3d) — the mirror of
 * [PermanentAttachedEvent], driving the "becomes unattached" trigger family
 * ([com.wingedsheep.sdk.scripting.EventPattern.BecomesUnattachedEvent]): Stitcher's Graft.
 *
 * Emitted from **every** path that breaks an attachment, because the card asks for all of them:
 * the explicit unattach effect
 * ([com.wingedsheep.engine.handlers.effects.permanent.attachments.UnattachEquipmentExecutor]),
 * re-equipping onto a different host
 * ([com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachEquipmentExecutor]), the
 * CR 704.5m/n state-based unattach
 * ([com.wingedsheep.engine.mechanics.sba.permanent.UnattachedAurasCheck]), and the attachment
 * itself leaving the battlefield while attached
 * ([com.wingedsheep.engine.handlers.effects.ZoneTransitionService]).
 *
 * @property attachmentId the aura/equipment that became unattached.
 * @property attachedToId the permanent it was attached to (its former host). May already have left
 *   the battlefield — that is the case where Stitcher's Graft's trigger fires but does nothing.
 * @property controllerId the attachment's controller at the moment it unattached. Captured here
 *   rather than read back off the entity because the leave-the-battlefield path has already
 *   stripped `ControllerComponent` by the time triggers are detected (CR 603.6e last-known
 *   information).
 */
@Serializable
@SerialName("PermanentUnattachedEvent")
data class PermanentUnattachedEvent(
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
 * Two creatures became soulbond-paired (CR 702.95b). Emitted once for the pair, not once per half.
 */
@Serializable
@SerialName("CreaturesPairedEvent")
data class CreaturesPairedEvent(
    val firstId: EntityId,
    val firstName: String,
    val secondId: EntityId,
    val secondName: String,
    val controllerId: EntityId
) : GameEvent

/**
 * A soulbond pair was broken (CR 702.95e) — one half left the battlefield, stopped being a
 * creature, or changed controller. Emitted once per pair by `SoulbondPairingCheck`, naming the half
 * that is still around ([entityId]) and the one it was paired with ([formerPartnerId]), which may
 * already be gone.
 */
@Serializable
@SerialName("CreaturesUnpairedEvent")
data class CreaturesUnpairedEvent(
    val entityId: EntityId,
    val entityName: String,
    val formerPartnerId: EntityId
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
    val firstThisTurn: Boolean = false,
    /**
     * The player who *put* these counters, per CR 122.6a — the controller of the effect that
     * placed them, that permanent's controller (for a permanent entering with counters), the mover's
     * controller (CR 122.5: moving a counter "puts" it on the destination), or the damage source's
     * controller (wither, CR 702.80). Drives "Whenever **you** put one or more counters on a
     * creature" triggers ([com.wingedsheep.sdk.scripting.EventPattern.CountersPlacedEvent.placedBy]).
     * `null` for the few paths that don't attribute a placer (saga lore counters, poison counters on
     * players); a null placer never matches a placer-restricted trigger.
     */
    val placedBy: EntityId? = null
) : GameEvent

/**
 * Counters were removed from a permanent.
 *
 * @property remainingCount How many counters of [counterType] the permanent had left immediately
 *   after *this* removal, or null when the emitter didn't record it. It can't be re-derived from
 *   game state at trigger-detection time: several removals can happen in one batch (two attackers
 *   damaging the same battle), and by the time triggers are detected the state shows the count
 *   after *all* of them — which would make every removal in the batch look like the one that
 *   emptied the permanent. "When the last counter is removed" (CR 310.12b) reads this field, and
 *   falls back to the live count when it is null.
 */
@Serializable
@SerialName("CountersRemovedEvent")
data class CountersRemovedEvent(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int,
    val entityName: String = "",
    val remainingCount: Int? = null
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
    val cardNames: List<String> = emptyList(),
    /**
     * True when the discard is the cost of a cycling/typecycling ability (CR 702.29a). Triggers
     * ignore this — cycling really is a discard and "whenever you discard" payoffs must see it —
     * but the client suppresses the "You discarded X" log line, because the accompanying
     * [CardCycledEvent] already narrates the same action.
     */
    val asCyclingCost: Boolean = false
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
 * Why a library was shuffled. Only [SPELL_OR_ABILITY] satisfies "a spell or ability causes a
 * player to shuffle their library" (Psychogenic Probe) — the other two are game rules shuffling
 * as part of setting up, not effects, so no ability may key off them.
 */
enum class ShuffleCause {
    /** An effect of a resolving spell, an ability, or a replacement effect (CR 701.24). */
    SPELL_OR_ABILITY,

    /** Shuffling each opening library while the game is set up (CR 103.2). */
    GAME_SETUP,

    /** Shuffling a hand back to take a mulligan (CR 103.5). */
    MULLIGAN
}

/**
 * Library was shuffled (CR 701.24).
 *
 * Emitted once per shuffle, so two effects shuffling the same library simultaneously produce two
 * events and any shuffle trigger fires twice (CR 701.24f). A library holding zero or one cards is
 * still shuffled and still emits (CR 701.24e), as is a search-then-shuffle where the found cards
 * are held out of the randomization (CR 701.24b).
 *
 * [cause] defaults to [ShuffleCause.SPELL_OR_ABILITY] because every shuffle that happens once the
 * game is under way is caused by one; the two game-rules shuffles ([ShuffleCause.GAME_SETUP],
 * [ShuffleCause.MULLIGAN]) name themselves at their emission sites.
 */
@Serializable
@SerialName("LibraryShuffledEvent")
data class LibraryShuffledEvent(
    val playerId: EntityId,
    val cause: ShuffleCause = ShuffleCause.SPELL_OR_ABILITY
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

/**
 * A creature exploited a creature (CR 702.110b): the controller of an exploit ability sacrificed a
 * creature as that ability resolved. Emitted once per sacrificed creature by `EmitExploitedEventExecutor`
 * (wired into the `exploit()` reflexive action right after the sacrifice), so external watchers —
 * [com.wingedsheep.sdk.scripting.EventPattern.ExploitedEvent], e.g. Skull Skaab — can react. Declining the
 * optional sacrifice sacrifices nothing, so no event is emitted.
 *
 * @property exploiterId The creature with exploit that did the sacrificing (the "exploiter").
 * @property exploiterControllerId Controller of the exploit ability (scopes "a creature *you* control exploits").
 * @property sacrificedId The creature that was sacrificed.
 * @property sacrificedWasToken Last-known token-ness of the sacrificed creature, snapshotted **before** the
 *   zone change — the creature is gone by the time watchers resolve. Drives Skull Skaab's "nontoken" clause.
 * @property sacrificedName The sacrificed creature's name (for display).
 * @property sourceName The exploiter's name (for display).
 */
@Serializable
@SerialName("ExploitedEvent")
data class ExploitedEvent(
    val exploiterId: EntityId,
    val exploiterControllerId: EntityId,
    val sacrificedId: EntityId,
    val sacrificedWasToken: Boolean,
    val sacrificedName: String,
    val sourceName: String
) : GameEvent

/**
 * A creature trained (CR 702.149c): a resolving training ability put one or more +1/+1 counters on
 * this creature. Emitted by `EmitTrainedEventExecutor` — the tail of the `training()` ability's
 * composite, right after its `AddCountersEffect` — but **only when the counter actually landed**
 * (the ability places nothing under a "can't have counters put on it" prohibition, and then no event
 * fires, faithful to "puts one or more +1/+1 counters"). Drives "When this creature trains" payoffs
 * ([com.wingedsheep.sdk.scripting.EventPattern.TrainedEvent], e.g. Savior of Ollenbock).
 *
 * Distinct from [CountersAddedEvent], which the same counter placement also emits for generic
 * "whenever counters are placed" watchers (Cloaked Cadet): this event marks that the placement came
 * from a *resolving training ability* specifically (the Option A distinction).
 *
 * @property trainedId The creature that trained (the trigger's subject; selected by the watching
 *   ability's `TriggerBinding` — SELF for "this creature trains").
 * @property controllerId The trained creature's controller at train time.
 * @property counters How many +1/+1 counters the training ability placed (>= 1; normally 1, more
 *   under a Hardened Scales / Doubling Season counter-placement replacement).
 * @property sourceName The trained creature's name (for display / logging).
 */
@Serializable
@SerialName("TrainedEvent")
data class TrainedEvent(
    val trainedId: EntityId,
    val controllerId: EntityId,
    val counters: Int,
    val sourceName: String
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
 * A permanent, spell, or player became the target of a spell or ability.
 * [firstTimeByThisController] indicates whether this is the first time this turn
 * the target was targeted by a spell/ability controlled by [controllerId].
 * Used for Valiant triggers ("for the first time each turn").
 * [targetIsSpell] is true when the targeted object is a spell on the stack rather
 * than a permanent on the battlefield (Rule 601.2c). Lets triggers that fire on a
 * "creature spell you control" being targeted (e.g. Surrak, Elusive Hunter) match,
 * while battlefield-only triggers (ward) never see spell targets because they are
 * generated only from permanents.
 * [targetIsPlayer] is its player counterpart — true when the chosen target is a player
 * (Loki, God of Mischief: "a player or permanent becomes the target"). Like spell targets,
 * player targets are opt-in on the trigger side (`EventPattern.BecomesTargetEvent
 * .includePlayerTargets`), so the permanent-only wordings already in the card pool don't
 * start firing on them.
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
    val sourceIsSpell: Boolean = false,
    val targetIsPlayer: Boolean = false
) : GameEvent

// =============================================================================
// Cycling Events
// =============================================================================

/**
 * A player cycled a card.
 *
 * @property xValue The value announced for `{X}` in the cycling cost, for cards with an X cycling
 *   cost (Webstrike Elite's "Cycling {X}{G}{G}"). A "when you cycle this card" trigger reads it as
 *   `DynamicAmount.XValue` via `TriggerContext.xValue`, so "destroy … with mana value X" and
 *   "create X tokens" resolve against the X that was actually paid. Null for ordinary cycling.
 */
@Serializable
@SerialName("CardCycledEvent")
data class CardCycledEvent(
    val playerId: EntityId,
    val cardId: EntityId,
    val cardName: String,
    val xValue: Int? = null
) : GameEvent

/**
 * A creature was tapped as a contributor to a Crew or Saddle activation cost.
 *
 * One event is emitted per contributing creature. [permanentId] is the Vehicle or Mount that the
 * creature crewed or saddled and becomes the triggering entity for the resolving payoff.
 */
@Serializable
@SerialName("CrewOrSaddleContributionEvent")
data class CrewOrSaddleContributionEvent(
    val contributorId: EntityId,
    val permanentId: EntityId,
    val controllerId: EntityId,
    val kind: CrewOrSaddleKind
) : GameEvent

@Serializable
enum class CrewOrSaddleKind { CREW, SADDLE }

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
// Madness Events
// =============================================================================

/**
 * A card with madness (CR 702.35) was discarded into exile instead of into its owner's graveyard.
 *
 * Fires from the discard path immediately after the redirected move, alongside the ordinary
 * [CardsDiscardedEvent] and [ZoneChangeEvent] — the card really was discarded, so discard payoffs
 * still see it. This event is what the trigger detector turns into the CR 702.35a triggered
 * ability ("when this card is exiled this way, its owner may cast it for its madness cost"), which
 * is why it carries the owner rather than whoever caused the discard.
 */
@Serializable
@SerialName("CardExiledWithMadnessEvent")
data class CardExiledWithMadnessEvent(
    val ownerId: EntityId,
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
 * One event per coin that was actually flipped. Under a
 * [com.wingedsheep.sdk.scripting.FlipAdditionalCoins] replacement (Krark's Thumb) a single coin the
 * game asked for becomes several real flips, all of which are reported — only one of them decides
 * the outcome and the rest carry [ignored].
 *
 * @property playerId The player who flipped the coin
 * @property won Whether the player won the flip
 * @property sourceId The entity that caused the coin flip
 * @property sourceName The name of the card/ability that caused the coin flip
 * @property ignored True when this coin was flipped but then discarded by a "flip N coins and
 *   ignore all but one" replacement, so its result had no effect. The flip still happened — it is
 *   reported so the log and the animation show what was really flipped — but nothing reads its
 *   [won] value.
 */
@Serializable
@SerialName("CoinFlipEvent")
data class CoinFlipEvent(
    val playerId: EntityId,
    val won: Boolean,
    val sourceId: EntityId,
    val sourceName: String,
    val ignored: Boolean = false
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

/**
 * A door of a Room lost its "unlocked" designation — it was locked (CR 709.5g). The twin of
 * [DoorUnlockedEvent], emitted by the resolution-time "lock a door" effect (`LockDoorEffect`, e.g.
 * Keys to the House). Locking is not a trigger source in the rules and can never fully unlock a
 * Room, so unlike unlocking there is no companion event — this is purely informational (game log /
 * animation). The locked half's name, mana cost, and rules text turn off via projection.
 */
@Serializable
@SerialName("DoorLockedEvent")
data class DoorLockedEvent(
    val roomId: EntityId,
    val roomName: String,
    val faceId: com.wingedsheep.engine.state.components.identity.RoomFaceId,
    val faceName: String,
    val controllerId: EntityId
) : GameEvent
