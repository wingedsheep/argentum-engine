package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of all game actions.
 * Actions are the inputs to the game engine.
 */
@Serializable
sealed interface GameAction {
    /** The player performing the action */
    val playerId: EntityId
}

// =============================================================================
// Priority Actions
// =============================================================================

/**
 * Player passes priority.
 */
@Serializable
@SerialName("PassPriority")
data class PassPriority(
    override val playerId: EntityId
) : GameAction

// =============================================================================
// Spell Actions
// =============================================================================

/**
 * Player casts a spell.
 *
 * @property playerId The player casting the spell
 * @property cardId The card being cast
 * @property targets Chosen targets for the spell (flat union of mode targets for modal spells —
 *           use [modeTargetsOrdered] to recover per-mode bindings)
 * @property xValue The value of X for X-cost spells
 * @property paymentStrategy How the player intends to pay the mana cost
 * @property alternativePayment Optional alternative payment choices (Delve, Convoke)
 * @property castFaceDown If true, cast as a face-down 2/2 creature for {3} (morph)
 * @property damageDistribution Pre-chosen damage distribution for DividedDamageEffect spells (target ID -> damage amount)
 * @property chosenModes Cast-time mode choices for modal spells (rules 700.2). Ordered; the same index
 *           may repeat when the [ModalEffect.allowRepeat] flag is set (Escalate/Spree).
 * @property modeTargetsOrdered Per-mode target bindings, aligned 1:1 with [chosenModes]. Required for
 *           choose-N modal spells so the resolution pipeline can resolve `ContextTarget(k)` inside each mode's scope.
 * @property modeDamageDistribution Per-mode DividedDamageEffect allocations (future — no current card uses this).
 */
@Serializable
@SerialName("CastSpell")
data class CastSpell(
    override val playerId: EntityId,
    val cardId: EntityId,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay,
    val alternativePayment: AlternativePaymentChoice? = null,
    val additionalCostPayment: AdditionalCostPayment? = null,
    val castFaceDown: Boolean = false,
    val wasKicked: Boolean = false,
    val damageDistribution: Map<EntityId, Int>? = null,
    val useAlternativeCost: Boolean = false,
    val chosenModes: List<Int> = emptyList(),
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
    val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),
    val graveyardLifeCost: Int = 0,
    /**
     * Creatures tapped to pay Conspire's optional additional cost. When non-empty, must contain
     * exactly two distinct untapped creatures the caster controls that each share a color with
     * the spell being cast (CR 702.78). When empty, Conspire was not invoked.
     */
    val conspiredCreatures: List<EntityId> = emptyList(),
    /**
     * Index into `cardDef.cardFaces` identifying which face of a split-layout card is being
     * cast (CR 709.3a — only the chosen half is put on the stack and evaluated for legality).
     * `null` for normal single-face cards. Required for SPLIT cards; the cast handler reads
     * the face's mana cost and the resulting permanent enters the battlefield with that
     * face's door unlocked (CR 709.5d).
     */
    val faceIndex: Int? = null,
    /**
     * Invokes a `MayCastWithoutPayingManaCost` battlefield permission (e.g. Weftwalking): cast
     * the spell for {0} via the "without paying its mana cost" alternative cost. Per CR 118.9a
     * only one alternative cost can apply to a cast, so this is mutually exclusive with
     * [useAlternativeCost]; distinguishing the two via separate flags is what lets the player
     * choose between this free cast and any other alt cost (Jodah's `GrantAlternativeCastingCost`,
     * flashback, harmonize, warp, evoke, impending, `selfAlternativeCost`) when both are legal.
     * The enumerator emits a separate `CastWithoutPayingManaCost` action per granted permission.
     */
    val useWithoutPayingManaCost: Boolean = false,
    /**
     * When [useAlternativeCost] is true, identifies *which* alternative cost the player chose.
     *
     * A card can have more than one alternative cost available simultaneously for the same
     * card+zone — most commonly when a battlefield static grants warp to a card that already
     * has evoke/impending/a self-alternative cost (e.g. Tannuk, Steadfast Second granting warp
     * to a red evoke creature in hand). Because every alternative-cost legal action sets
     * `useAlternativeCost = true`, that flag alone can't say which one was clicked, so the
     * handler would otherwise fall back to a fixed priority order and silently charge the wrong
     * cost. This discriminator records the choice explicitly (CR 601.2b/601.2f — the player
     * announces the alternative cost as part of casting).
     *
     * `null` means "unspecified" — the handler falls back to its legacy priority chain. The
     * legal-action enumerators always stamp it; only hand-constructed actions (some tests,
     * synthesized free casts) leave it null. Mirrors how [useWithoutPayingManaCost] was split
     * out as its own flag for the same reason (CR 118.9a — only one alternative cost per cast).
     */
    val alternativeCostType: AlternativeCostType? = null
) : GameAction

/**
 * Which alternative casting cost a [CastSpell] with `useAlternativeCost = true` is using. Lets the
 * handler honor the player's explicit choice instead of guessing by priority when several
 * alternative costs are legal for the same card at once (CR 118.9a — only one may apply).
 */
@Serializable
enum class AlternativeCostType {
    /** Flashback ([com.wingedsheep.sdk.scripting.KeywordAbility.Flashback]) — graveyard. */
    FLASHBACK,
    /** Harmonize ([com.wingedsheep.sdk.scripting.KeywordAbility.Harmonize], printed or granted) — graveyard. */
    HARMONIZE,
    /** Warp ([com.wingedsheep.sdk.scripting.KeywordAbility.Warp], printed or granted) — hand (graveyard if opted in). */
    WARP,
    /** Evoke ([com.wingedsheep.sdk.scripting.KeywordAbility.Evoke]) — hand. */
    EVOKE,
    /** Impending ([com.wingedsheep.sdk.scripting.KeywordAbility.Impending]) — hand. */
    IMPENDING,
    /** A card's own `selfAlternativeCost` (e.g. Zahid's "tap an untapped artifact") — hand. */
    SELF_ALTERNATIVE,
    /** A battlefield-granted alternative cost (e.g. Jodah's {W}{U}{B}{R}{G}) — `GrantAlternativeCastingCost`. */
    GRANTED
}

/**
 * Defines how a player intends to pay the mana cost of a spell or ability.
 *
 * This allows the engine to support both:
 * - **Auto-tap**: The engine automatically selects lands to tap (smooth UX for casual play)
 * - **Manual payment**: The player has already floated mana via ActivateAbility actions (hardcore mode)
 */
@Serializable
sealed interface PaymentStrategy {
    /**
     * Engine automatically calculates the best lands/mana sources to tap.
     * The engine will emit TappedEvents for each source used.
     */
    @Serializable
    @SerialName("AutoPay")
    data object AutoPay : PaymentStrategy

    /**
     * Player has already activated mana abilities manually.
     * Engine verifies the ManaPool has sufficient mana and deducts it.
     */
    @Serializable
    @SerialName("FromPool")
    data object FromPool : PaymentStrategy

    /**
     * Advanced: Player specifies exactly which mana abilities to activate.
     * The engine activates these abilities and uses the resulting mana.
     *
     * @property manaAbilitiesToActivate Entity IDs of permanents whose mana abilities to activate
     */
    @Serializable
    @SerialName("Explicit")
    data class Explicit(val manaAbilitiesToActivate: List<EntityId>) : PaymentStrategy
}

// =============================================================================
// Ability Actions
// =============================================================================

/**
 * Player activates an ability.
 *
 * @property playerId The player activating the ability
 * @property sourceId The permanent whose ability is being activated
 * @property abilityId The ID of the ability being activated
 * @property targets Chosen targets for the ability's effect
 * @property costPayment Payment choices for costs (sacrifice, etc.)
 * @property manaColorChoice Color chosen for "add one mana of any color" abilities
 */
@Serializable
@SerialName("ActivateAbility")
data class ActivateAbility(
    override val playerId: EntityId,
    val sourceId: EntityId,
    val abilityId: AbilityId,
    val targets: List<ChosenTarget> = emptyList(),
    val costPayment: AdditionalCostPayment? = null,
    val manaColorChoice: Color? = null,
    val xValue: Int? = null,
    val repeatCount: Int = 1,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay,
    val alternativePayment: AlternativePaymentChoice? = null
) : GameAction

// =============================================================================
// Cycling Actions
// =============================================================================

/**
 * Player cycles a card from their hand.
 *
 * Cycling is an activated ability that can be activated from hand.
 * The player pays the cycling cost, discards the card, and draws a card.
 *
 * @property playerId The player cycling the card
 * @property cardId The card being cycled
 */
@Serializable
@SerialName("CycleCard")
data class CycleCard(
    override val playerId: EntityId,
    val cardId: EntityId,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay
) : GameAction

/**
 * Player plots a card from their hand (Outlaws of Thunder Junction, CR 718).
 *
 * Plot is a special action — does not use the stack, sorcery-speed only. The player
 * pays the plot cost from the [KeywordAbility.Plot][com.wingedsheep.sdk.scripting.KeywordAbility.Plot]
 * ability and exiles the card from their hand. The exiled card is marked plotted
 * (see [com.wingedsheep.engine.state.components.identity.PlottedComponent]) and may
 * be cast from exile without paying its mana cost on a later turn.
 *
 * @property playerId The player plotting the card
 * @property cardId The card being plotted
 */
@Serializable
@SerialName("PlotCard")
data class PlotCard(
    override val playerId: EntityId,
    val cardId: EntityId,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay
) : GameAction

/**
 * Player typecycles a card from their hand.
 *
 * Typecycling (e.g., Swampcycling, Wizardcycling) is an activated ability from hand.
 * The player pays the typecycling cost, discards the card, then searches their
 * library for a card of the specified type, reveals it, puts it into their hand,
 * then shuffles.
 *
 * @property playerId The player typecycling the card
 * @property cardId The card being typecycled
 */
@Serializable
@SerialName("TypecycleCard")
data class TypecycleCard(
    override val playerId: EntityId,
    val cardId: EntityId,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay
) : GameAction

// =============================================================================
// Land Actions
// =============================================================================

/**
 * Player plays a land.
 */
@Serializable
@SerialName("PlayLand")
data class PlayLand(
    override val playerId: EntityId,
    val cardId: EntityId
) : GameAction

// =============================================================================
// Combat Actions
// =============================================================================

/**
 * Player declares attackers.
 *
 * @property attackers attacker entity id -> defender (player or planeswalker) being attacked.
 * @property bands Optional band groupings (CR 702.22). Each entry is the set of creatures in
 *   one band. Every creature in a band must also appear as a key in [attackers] with the same
 *   defender; at most one creature per band may lack the BANDING keyword (CR 702.22c). Bands of
 *   size < 2 are rejected. Empty by default — no bands declared.
 */
@Serializable
@SerialName("DeclareAttackers")
data class DeclareAttackers(
    override val playerId: EntityId,
    val attackers: Map<EntityId, EntityId>,  // attacker -> defending player
    val bands: List<Set<EntityId>> = emptyList()
) : GameAction

/**
 * Player declares blockers.
 */
@Serializable
@SerialName("DeclareBlockers")
data class DeclareBlockers(
    override val playerId: EntityId,
    val blockers: Map<EntityId, List<EntityId>>  // blocker -> list of attackers
) : GameAction

/**
 * Player orders blockers for an attacker.
 */
@Serializable
@SerialName("OrderBlockers")
data class OrderBlockers(
    override val playerId: EntityId,
    val attackerId: EntityId,
    val orderedBlockers: List<EntityId>
) : GameAction

// =============================================================================
// Mana Actions
// =============================================================================

/**
 * Player chooses a color for "add one mana of any color" effects.
 */
@Serializable
@SerialName("ChooseManaColor")
data class ChooseManaColor(
    override val playerId: EntityId,
    val color: Color
) : GameAction

// =============================================================================
// Decision Response
// =============================================================================

/**
 * Player submits a response to a pending decision.
 *
 * This is used to resume the engine after it has paused for player input.
 * The response must match the type expected by the pending decision.
 */
@Serializable
@SerialName("SubmitDecision")
data class SubmitDecision(
    override val playerId: EntityId,
    val response: DecisionResponse
) : GameAction

// =============================================================================
// Mulligan Actions
// =============================================================================

/**
 * Player takes a mulligan, shuffling their hand into their library
 * and drawing a new hand of (7 - mulligans taken) cards.
 */
@Serializable
@SerialName("TakeMulligan")
data class TakeMulligan(
    override val playerId: EntityId
) : GameAction

/**
 * Player keeps their current hand.
 *
 * After all players have kept, each player who took mulligans
 * puts that many cards from their hand on the bottom of their library.
 */
@Serializable
@SerialName("KeepHand")
data class KeepHand(
    override val playerId: EntityId
) : GameAction

/**
 * Player puts cards on the bottom of their library after keeping a mulligan hand.
 * This is the "London mulligan" rule where you put cards back equal to mulligans taken.
 *
 * @property cardIds The cards to put on bottom (in the order they should be placed)
 */
@Serializable
@SerialName("BottomCards")
data class BottomCards(
    override val playerId: EntityId,
    val cardIds: List<EntityId>
) : GameAction

// =============================================================================
// Concession
// =============================================================================

/**
 * Player concedes the game.
 */
@Serializable
@SerialName("Concede")
data class Concede(
    override val playerId: EntityId
) : GameAction

// =============================================================================
// Crew Actions
// =============================================================================

/**
 * Player crews a Vehicle by tapping creatures with total power >= crew requirement.
 * Crew is an activated ability that goes on the stack. The cost (tapping creatures)
 * is paid immediately; the effect (Vehicle becomes a creature) resolves on the stack.
 *
 * @property playerId The player crewing the vehicle
 * @property vehicleId The Vehicle permanent being crewed
 * @property crewCreatures Creatures to tap as the crew cost
 */
@Serializable
@SerialName("CrewVehicle")
data class CrewVehicle(
    override val playerId: EntityId,
    val vehicleId: EntityId,
    val crewCreatures: List<EntityId>
) : GameAction

// =============================================================================
// Saddle Actions
// =============================================================================

/**
 * Player saddles a Mount (or other permanent with Saddle N) by tapping any number of *other*
 * untapped creatures they control whose total power >= the saddle requirement (CR 702.171a).
 * Saddle is an activated ability that goes on the stack; the cost (tapping creatures) is paid
 * immediately and the effect (the permanent becomes saddled until end of turn) resolves on the
 * stack. Activated only as a sorcery.
 *
 * @property playerId The player saddling the mount
 * @property mountId The permanent being saddled
 * @property saddleCreatures Creatures to tap as the saddle cost
 */
@Serializable
@SerialName("SaddleMount")
data class SaddleMount(
    override val playerId: EntityId,
    val mountId: EntityId,
    val saddleCreatures: List<EntityId>
) : GameAction

// =============================================================================
// Morph Actions
// =============================================================================

/**
 * Player turns a face-down creature face up by paying its morph cost.
 * This is a special action that doesn't use the stack and can be done any time
 * the player has priority.
 *
 * @property playerId The player turning the creature face up
 * @property sourceId The face-down creature to turn face up (named sourceId for frontend consistency with ActivateAbility)
 * @property paymentStrategy How the player intends to pay the morph cost
 * @property costTargetIds Permanents chosen for non-mana morph costs (e.g., return a Bird to hand)
 */
@Serializable
@SerialName("TurnFaceUp")
data class TurnFaceUp(
    override val playerId: EntityId,
    val sourceId: EntityId,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay,
    val costTargetIds: List<EntityId> = emptyList(),
    val xValue: Int? = null
) : GameAction

// =============================================================================
// Room Actions (DSK)
// =============================================================================

/**
 * Unlock a locked door of a Room permanent the player controls (CR 709.5e).
 *
 * The unlock cost is the locked face's printed mana cost. Per rule 116, this is a
 * **special action** — it does not use the stack, can't be responded to between
 * declaration and effect, and can't be countered. Timing: any time the controller has
 * priority, the stack is empty, and it is a main phase of their turn.
 *
 * @property roomId The Room permanent whose door is being unlocked
 * @property faceId The locked face whose door is being unlocked
 * @property paymentStrategy How to pay the face's mana cost
 */
@Serializable
@SerialName("UnlockRoomDoor")
data class UnlockRoomDoor(
    override val playerId: EntityId,
    val roomId: EntityId,
    val faceId: RoomFaceId,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay
) : GameAction
