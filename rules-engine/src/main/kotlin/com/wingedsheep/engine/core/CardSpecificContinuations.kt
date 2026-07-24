package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Resume after putting a card from hand onto the battlefield after card selection.
 *
 * @property playerId The player selecting a card from their hand
 * @property entersTapped Whether the card enters the battlefield tapped
 * @property sourceId The source entity that triggered this effect
 * @property sourceName Name of the source for display
 */
@Serializable
data class PutFromHandContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val entersTapped: Boolean,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after a player chooses a number for secret bidding effects (Menacing Ogre).
 *
 * Each player secretly chooses a number. After all players have chosen,
 * execute outcome effects per matching bidder (xValue = bid, controllerId = bidder).
 *
 * @property sourceId The creature that entered the battlefield
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the source creature
 * @property currentPlayerId The player whose choice we are waiting for
 * @property remainingPlayers Players who still need to choose (APNAP order)
 * @property chosenNumbers Numbers chosen so far by each player
 * @property highestBidderEffect Effect executed per highest bidder
 * @property lowestBidderEffect Effect executed per lowest non-zero bidder
 * @property tiedBidderEffect Effect executed per bidder when all non-zero bids are equal
 */
@Serializable
data class SecretBidContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val chosenNumbers: Map<EntityId, Int>,
    val highestBidderEffect: Effect?,
    val lowestBidderEffect: Effect?,
    val tiedBidderEffect: Effect?
) : ContinuationFrame

/**
 * Which step of the [OpenLifeBidContinuation] the pending decision belongs to.
 */
@Serializable
enum class OpenLifeBidStage {
    /** A yes/no: does [OpenLifeBidContinuation.bidderToAsk] want to top the high bid? */
    AWAITING_TOP_DECISION,

    /** A number choice: how much does [OpenLifeBidContinuation.bidderToAsk] bid? */
    AWAITING_BID_AMOUNT
}

/**
 * Resume an open life-bidding auction (Mages' Contest).
 *
 * The two bidders — [casterId] and the resolved participant — alternate topping the high bid.
 * The player currently deciding is [bidderToAsk]; [highBidder] holds the current [highBid].
 * When [bidderToAsk] declines to top, the auction ends, the high bidder loses [highBid] life,
 * and (only if the caster won) [onWin] runs against [targets].
 *
 * @property casterId The player who cast the auction spell (the "you" who wins to apply [onWin])
 * @property highBidder The player currently holding the high bid
 * @property highBid The current high bid in life
 * @property bidderToAsk The player whose decision we are waiting for
 * @property stage Whether we await a top yes/no or a bid amount
 * @property onWin Effect run if the caster is the high bidder
 * @property targets Original targets, propagated so [onWin] can reference the spell
 * @property sourceId The auction spell entity (for prompt context)
 * @property sourceName Name of the auction spell (for prompts)
 */
@Serializable
data class OpenLifeBidContinuation(
    override val decisionId: String,
    val casterId: EntityId,
    val highBidder: EntityId,
    val highBid: Int,
    val bidderToAsk: EntityId,
    val stage: OpenLifeBidStage,
    val onWin: Effect,
    val targets: List<ChosenTarget>,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume a chosen player's optional retargeting of the triggering spell/ability (Psychic Battle's
 * reveal winner, or any "[chooser] may change its targets" effect). Targets are reselected one slot
 * at a time; this carries the state needed to apply the choice for [currentSlot] and continue with
 * the remaining slots.
 *
 * @property stackObjectId The triggering spell/ability whose targets are being changed
 * @property chooserId The player choosing the new targets
 * @property ownerControllerId The triggering object's controller (legality is judged from here)
 * @property perSlotRequirements One target requirement per target slot
 * @property originalTargets The triggering object's targets at trigger-resolution time
 * @property newTargets Targets chosen/kept for slots before [currentSlot]
 * @property currentSlot The slot the pending decision is choosing a target for
 * @property sourceId The retargeting effect's source (for prompt context)
 */
@Serializable
data class ContestedRetargetContinuation(
    override val decisionId: String,
    val stackObjectId: EntityId,
    val chooserId: EntityId,
    val ownerControllerId: EntityId,
    val perSlotRequirements: List<TargetRequirement>,
    val originalTargets: List<ChosenTarget>,
    val newTargets: List<ChosenTarget>,
    val currentSlot: Int,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after player has distributed counters from a source creature to other creatures.
 *
 * Used for effects like Forgotten Ancient where the player moves counters from
 * the source creature onto other creatures. The response contains the counter distribution.
 *
 * @property sourceId The creature the counters are being moved from
 * @property controllerId The player who controls the effect
 * @property counterType The type of counter being moved (e.g., "+1/+1")
 */
@Serializable
data class DistributeCountersContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val controllerId: EntityId,
    val counterType: String,
    /**
     * When true (the "move counters from this creature onto others" shape, e.g. Forgotten Ancient),
     * the distributed counters are first removed from [sourceId]. When false, the counters are newly
     * created on the chosen recipients with nothing removed from the source — the "distribute N
     * counters among …" shape (e.g. Crashing Wave's three stun counters).
     */
    val removeFromSource: Boolean = true
) : ContinuationFrame

/**
 * Resume after the controller picks how many counters of one kind to remove
 * from a target permanent. The executor for [RemoveAnyNumberOfCountersEffect]
 * issues one decision per counter kind currently on the target; on resume,
 * the response is applied and the next kind (if any) is prompted.
 *
 * When [remainingBudget] is non-null, a *total* cap is in force (`maxTotal` on the effect —
 * Heartless Act's "remove up to N counters"): each prompt is capped at `min(kindCount, budget)`,
 * the budget is decremented on resume, and prompting stops once it hits zero. Null means no cap
 * ("remove any number").
 *
 * @property targetId The permanent whose counters are being removed
 * @property controllerId The player making the choices
 * @property currentCounterType The counter kind the active decision is for
 * @property currentMaxAmount Cap shown to the player (0..currentMaxAmount)
 * @property remainingBudget Counters still removable in total after the active decision, or null for no cap
 * @property remainingCounterTypes Pending (counterType, maxAmount) prompts
 * @property targetName Display name for follow-up prompts
 * @property sourceId Source emitting the effect (for prompt context)
 * @property sourceName Source name for prompt context
 */
@Serializable
data class RemoveAnyNumberOfCountersContinuation(
    override val decisionId: String,
    val targetId: EntityId,
    val controllerId: EntityId,
    val currentCounterType: String,
    val currentMaxAmount: Int,
    val remainingCounterTypes: List<Pair<String, Int>>,
    val targetName: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val remainingBudget: Int? = null
) : ContinuationFrame

/**
 * Resume after the controller picks how many counters (0..max) to put on a target, for
 * `AddCountersUpToEffect` ("Put up to N [counterType] counters on target" — Esper Terra's lore
 * chapters). The chosen count is placed through the standard `AddCountersEffect` path so
 * counter-placement replacements and downstream (Saga chapter) triggers fire. Choosing 0 is a no-op.
 *
 * @property targetId The permanent to put counters on
 * @property controllerId The player who made the choice
 * @property counterType The counter kind to place
 * @property sourceId Source emitting the effect (for the placement context)
 */
@Serializable
data class AddCountersUpToContinuation(
    override val decisionId: String,
    val targetId: EntityId,
    val controllerId: EntityId,
    val counterType: String,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the controller picks how many counters of one kind to move from a
 * [sourceId] permanent onto a [destinationId] permanent. The executor for
 * `MoveChosenCountersToTargetEffect` issues one decision per counter kind on the source;
 * on resume, the chosen amount is removed from the source and added to the destination, and
 * the next kind (if any) is prompted. After the last kind, if [drawCardOnMove] is set and at
 * least one counter was moved overall, the controller draws a card. (Goldberry — ability B.)
 *
 * @property sourceId The permanent counters are moved from
 * @property destinationId The permanent counters are moved onto
 * @property controllerId The player making the choices (and who draws)
 * @property currentCounterType The counter kind the active decision is for
 * @property currentMaxAmount Cap shown to the player (0..currentMaxAmount)
 * @property remainingCounterTypes Pending (counterType, maxAmount) prompts
 * @property sourceName Display name of the source for follow-up prompts
 * @property destinationName Display name of the destination for follow-up prompts
 * @property drawCardOnMove Whether to draw a card at the end if any counter was moved
 * @property anyMovedSoFar Whether any counter has been moved across prior prompts
 */
@Serializable
data class MoveChosenCountersToTargetContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val destinationId: EntityId,
    val controllerId: EntityId,
    val currentCounterType: String,
    val currentMaxAmount: Int,
    val remainingCounterTypes: List<Pair<String, Int>>,
    val sourceName: String,
    val destinationName: String,
    val drawCardOnMove: Boolean,
    val anyMovedSoFar: Boolean = false
) : ContinuationFrame

/**
 * Resume after the controller picks the permanents and/or players that should
 * each receive another counter of each kind already on them (Proliferate).
 *
 * @property controllerId The player resolving the proliferate
 * @property eligibleEntities The permanents/players that had at least one counter
 *                            when the decision was offered. Used to discard stale
 *                            selections and to defend against the response naming
 *                            an entity that had no counters at decision time.
 */
@Serializable
data class ProliferateContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val eligibleEntities: List<EntityId>
) : ContinuationFrame

/**
 * Resume after a tempted player chooses which creature becomes their Ring-bearer (CR 701.54a).
 *
 * The emblem and tempt-count increment are applied before the decision; this continuation only
 * moves the Ring-bearer designation to the chosen creature and announces the temptation.
 *
 * @property temptedPlayerId The player being tempted.
 * @property temptCount That player's tempt count after this temptation (for the announcement).
 * @property sourceName The card/ability that tempted (for display).
 * @property candidates The creatures offered at decision time, used to validate the response.
 */
@Serializable
data class RingTemptContinuation(
    override val decisionId: String,
    val temptedPlayerId: EntityId,
    val temptCount: Int,
    val sourceName: String,
    val candidates: List<EntityId>
) : ContinuationFrame

/**
 * Resume "amass [subtype] N" after the controller chooses which Army to put the counters on
 * (CR 701.47a). Only reached when the controller has more than one Army; the chosen Army receives
 * [amount] +1/+1 counters and becomes [subtype] if it isn't already.
 *
 * @property controllerId The player amassing (also the chooser).
 * @property subtype The Army subtype being amassed (e.g., "Orc").
 * @property amount Number of +1/+1 counters to place.
 * @property sourceId The amassing source (for context/display).
 * @property candidates The Armies offered at decision time, used to validate the response.
 */
@Serializable
data class AmassContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val subtype: String,
    val amount: Int,
    val sourceId: EntityId?,
    val candidates: List<EntityId>
) : ContinuationFrame

/**
 * Resume creating a copy of a triggered ability after the player selects new targets.
 *
 * Used by Kirol-style effects ("Copy target triggered ability you control. You may choose
 * new targets for the copy."). The controller re-chooses targets, then the copy is cloned
 * from the source ability's TriggeredAbilityOnStackComponent and pushed onto the stack.
 *
 * @property abilityEntityId The triggered ability being copied (on the stack)
 * @property controllerId The player controlling the copy (also the target-chooser)
 * @property targetRequirements Target requirements inherited from the source ability
 */
@Serializable
data class CopyTriggeredAbilityTargetContinuation(
    override val decisionId: String,
    val abilityEntityId: EntityId,
    val controllerId: EntityId,
    val targetRequirements: List<TargetRequirement>
) : ContinuationFrame

/**
 * Resume creating a copy of an *activated* ability after the copier selects new targets
 * (CR 707.10c). The mirror of [CopyTriggeredAbilityTargetContinuation] for the activated-ability
 * branch of "copy target spell or ability" (Return the Favor). The copy is cloned from the source
 * ability's ActivatedAbilityOnStackComponent and pushed onto the stack under [controllerId].
 *
 * @property abilityEntityId The activated ability being copied (on the stack)
 * @property controllerId The player controlling the copy (also the target-chooser)
 * @property targetRequirements Target requirements inherited from the source ability
 */
@Serializable
data class CopyActivatedAbilityTargetContinuation(
    override val decisionId: String,
    val abilityEntityId: EntityId,
    val controllerId: EntityId,
    val targetRequirements: List<TargetRequirement>
) : ContinuationFrame

/**
 * Resume the "copy target activated or triggered ability [N] times" loop after the copier chooses
 * new targets for one copy (CR 707.10 / 707.10c). Backs [Effects.CopyTargetSpellOrAbility] with
 * `copies > 1` — Gogo, Master of Mimicry ("Copy target activated or triggered ability you control X
 * times. You may choose new targets for the copies."). Each pause handles a single copy; after its
 * copy is pushed, [remainingCopies] drives whether the loop prompts again for the next copy.
 * Handles both the activated and triggered branches — the resumer reads the kind off the source
 * ability entity.
 *
 * @property abilityEntityId The ability being copied (still on the stack)
 * @property controllerId The player controlling the copies (also the target-chooser)
 * @property targetRequirements Target requirements inherited from the source ability
 * @property remainingCopies Copies still to create, including the one whose targets were just chosen
 * @property totalCopies Total copies requested (for the "copy i of N" prompt label)
 */
@Serializable
data class CopyAbilityTargetContinuation(
    override val decisionId: String,
    val abilityEntityId: EntityId,
    val controllerId: EntityId,
    /** The copier's source (e.g. Gogo) — used to validate the new targets for each copy. */
    val copierSourceId: EntityId?,
    val targetRequirements: List<TargetRequirement>,
    val remainingCopies: Int,
    val totalCopies: Int
) : ContinuationFrame

/**
 * Resume creating Storm copies after the player selects targets for a copy.
 *
 * When a Storm spell has targets, we pause to ask for target selection for each copy.
 * After the player chooses targets, we create the copy and (if more copies remain)
 * prompt for the next copy's targets.
 *
 * @property remainingCopies Number of copies still to create (including the one being targeted)
 * @property spellEffect The effect of the original spell to copy. Null when copying a
 *   permanent spell (creatures, artifacts, etc.) — those resolve into permanents via
 *   the spell's CardComponent rather than a stack effect.
 * @property spellTargetRequirements Target requirements for each copy
 * @property spellName Name of the original spell
 * @property controllerId The player who controls the copies
 * @property sourceId The source spell entity ID
 * @property removeLegendary If true, the Legendary supertype is stripped from each copy's
 *   CardComponent (CR 707.10f token-copy that "isn't legendary", e.g., Jackal).
 */
@Serializable
data class StormCopyTargetContinuation(
    override val decisionId: String,
    val remainingCopies: Int,
    val spellEffect: Effect?,
    val spellTargetRequirements: List<TargetRequirement>,
    val spellName: String,
    val controllerId: EntityId,
    val sourceId: EntityId,
    val totalCopies: Int = remainingCopies,  // Original total copies (defaults to remainingCopies for backward compat)
    /** Keyword enum names (e.g., "WITHER") to grant to each copy while it's on the stack. */
    val keywordsForCopy: Set<String> = emptySet(),
    val removeLegendary: Boolean = false
) : ContinuationFrame

/**
 * Resume copying a list of targeted spells one at a time (CR 707.10).
 *
 * Used by [com.wingedsheep.sdk.scripting.effects.CopyEachTargetSpellEffect] (Display of
 * Power). [remainingSpellIds] holds every spell still to be copied, head first; the head
 * is the spell whose copy's targets are being chosen by the paused decision. On resume the
 * head is copied with the selected targets, then the tail is processed.
 *
 * @property remainingSpellIds Spells still to copy (head = the one being retargeted now)
 * @property controllerId Player who controls the copies and picks targets
 * @property targetRequirements Target requirements of the head spell (for the copy)
 */
@Serializable
data class CopyEachSpellContinuation(
    override val decisionId: String,
    val remainingSpellIds: List<EntityId>,
    val controllerId: EntityId,
    val targetRequirements: List<TargetRequirement>,
    val keywordsForCopy: Set<String> = emptySet(),
    val removeLegendary: Boolean = false
) : ContinuationFrame

/**
 * Resume Storm modal target selection (rule 702.40a + 707.10).
 *
 * Modal Storm spells keep their chosen modes on every copy (700.2g — modes are
 * decided once by the caster), but the copy controller may re-choose targets
 * for each mode per 702.40a. This continuation iterates through chosen modes
 * for a single copy, pausing once per mode that has target requirements and
 * advancing without prompt for no-target modes. When all modes are collected
 * for a copy, the copy is pushed on the stack and the loop restarts for the
 * next copy.
 *
 * @property remainingCopies Copies still to create (including the one being built)
 * @property totalCopies Original copy count (for copyIndex labeling)
 * @property spellName Display name of the original spell
 * @property controllerId Player who controls the copies and picks targets
 * @property sourceId Original spell's entity ID on the stack
 * @property chosenModes Inherited chosen mode indices — fixed for every copy
 * @property modeTargetRequirements Inherited per-mode target requirements
 * @property accumulatedOrdinalTargets Targets chosen so far for the current copy,
 *   aligned 1:1 with [chosenModes] positions 0..[currentOrdinal]-1
 * @property currentOrdinal Index into [chosenModes] whose targets we are collecting
 */
@Serializable
data class StormCopyModalTargetContinuation(
    override val decisionId: String,
    val remainingCopies: Int,
    val totalCopies: Int,
    val spellName: String,
    val controllerId: EntityId,
    val sourceId: EntityId,
    val chosenModes: List<Int>,
    val modeTargetRequirements: Map<Int, List<TargetRequirement>>,
    val accumulatedOrdinalTargets: List<List<ChosenTarget>>,
    val currentOrdinal: Int,
    /** Keyword enum names (e.g., "WITHER") to grant to each copy while it's on the stack. */
    val keywordsForCopy: Set<String> = emptySet(),
    /** If true, strip the Legendary supertype from each resulting copy. */
    val removeLegendary: Boolean = false
) : ContinuationFrame

/**
 * Resume after Meddle's controller chooses a new creature target for a spell.
 *
 * @property spellEntityId The spell whose target is being changed
 * @property sourceId The source of the change-target effect (Meddle)
 */
@Serializable
data class ChangeSpellTargetContinuation(
    override val decisionId: String,
    val spellEntityId: EntityId,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after player selects which legendary permanent to keep (legend rule 704.5j).
 *
 * The player chose one permanent to keep; all others with the same name are put into the graveyard.
 *
 * @property playerId The player who controls the duplicates
 * @property allDuplicates All entity IDs of the legendary permanents with the same name
 */
@Serializable
data class LegendRuleContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val allDuplicates: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the commander's owner answers the CR 903.9a state-based action prompt.
 *
 * Yes → the commander moves from [currentZone] to the command zone. No → mark the commander
 * as asked-this-stay so the SBA stops re-prompting; the marker is cleared automatically by
 * the next zone transition (see [com.wingedsheep.engine.state.components.identity.
 * CommanderZoneChoiceAskedComponent]).
 *
 * @property commanderId The commander entity awaiting the choice.
 * @property ownerId The owner answering the prompt (i.e. the player whose decision this is).
 * @property currentZone The non-command zone the commander is sitting in while the prompt
 *   is open. Captured so the resumer can issue the correct from-zone on the move and so the
 *   prompt remains traceable in logs even after the commander moves.
 */
@Serializable
data class CommanderZoneChoiceContinuation(
    override val decisionId: String,
    val commanderId: EntityId,
    val ownerId: EntityId,
    val currentZone: com.wingedsheep.sdk.core.Zone
) : ContinuationFrame

/**
 * Resume after a player picks X for an activated ability with an X-variable cost
 * (currently [com.wingedsheep.sdk.scripting.AbilityCost.TapXPermanents]).
 *
 * The legal-actions path surfaces such activations with `hasXCost=true` and no
 * pre-filled `xValue`/`tappedPermanents`. When [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler]
 * sees that shape, it raises a [ChooseNumberDecision] and pushes this frame so the
 * resumer can chain into a follow-up tap-target [SelectCardsDecision] sized to the
 * chosen X (or skip straight to re-executing when X = 0).
 *
 * @property action The original [ActivateAbility] (xValue/costPayment still null), so the resumer
 *   can re-enter the handler with the chosen X and selected tap targets filled in.
 * @property tapTargets The untapped permanents matching the cost's filter at announcement time —
 *   captured here so the follow-up [SelectCardsDecision] has a stable option list even if the
 *   board changes between decisions.
 */
@Serializable
data class ActivateAbilityChooseXContinuation(
    override val decisionId: String,
    val action: ActivateAbility,
    val tapTargets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after a player chooses X for an activated ability whose cost contains `{X}` **mana**
 * (e.g. Wizard's Rockets: "{X}, {T}, Sacrifice this artifact: Add X mana..."). The legal-actions
 * submission path sends a bare [ActivateAbility] with `xValue == null`; the handler raises a
 * ChooseNumberDecision and stores this frame. On resume the handler is re-entered with `xValue`
 * bound to the chosen number, and the `{X}` mana cost is paid for that amount.
 *
 * Unlike [ActivateAbilityChooseXContinuation] (a tap-X cost, which has a follow-up tap-target
 * selection), a mana-X cost has no second decision — paying the mana is automatic.
 */
@Serializable
data class ActivateAbilityChooseManaXContinuation(
    override val decisionId: String,
    val action: ActivateAbility
) : ContinuationFrame

/**
 * Resume after a player picks which permanents to tap to satisfy a TapXPermanents cost,
 * once X has already been chosen via [ActivateAbilityChooseXContinuation].
 *
 * @property action The original [ActivateAbility] (xValue/costPayment still null on the stored copy).
 * @property chosenX The X value the player picked in the preceding [ChooseNumberDecision].
 * @property tapTargets Permanents that were offered as options in the [SelectCardsDecision]; used
 *   to validate the response is a subset of the originally legal targets.
 */
@Serializable
data class ActivateAbilityTapXTargetsContinuation(
    override val decisionId: String,
    val action: ActivateAbility,
    val chosenX: Int,
    val tapTargets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after a player picks which graveyard cards to exile to satisfy an
 * [com.wingedsheep.sdk.scripting.AbilityCost.ExileFromGraveyard] cost on an activated ability.
 *
 * Surfaces the cost-choice the original engine path silently auto-paid: when an activation's
 * `ExileFromGraveyard(count, filter)` cost has more matching graveyard cards than `count`, the
 * handler raises a [SelectCardsDecision] and pushes this frame so the resumer can re-enter the
 * handler with the player's chosen cards filled into `costPayment.exiledCards`.
 *
 * Skipped (no pause) when `exiledCards` is already pre-filled (engine-direct path / resumed
 * replay) or when candidate count ≤ required count (no real choice). Rust Harvester
 * ({2}, {T}, Exile an artifact card from your graveyard: …) is the canonical trigger case.
 *
 * @property action The original [ActivateAbility] (`costPayment.exiledCards` still empty), so the
 *   resumer can re-enter the handler with the chosen exile victims filled in.
 * @property exileCandidates The matching graveyard cards offered to the player as options; used
 *   to validate the response is a subset of the originally legal candidates.
 * @property exileCount Exact number of cards the player must pick (`count` from the cost).
 */
@Serializable
data class ActivateAbilityExileFromGraveyardContinuation(
    override val decisionId: String,
    val action: ActivateAbility,
    val exileCandidates: List<EntityId>,
    val exileCount: Int
) : ContinuationFrame

/**
 * Resume after an opponent picks the target(s) for an activated ability's "… of an opponent's
 * choice" requirement (Cuombajj Witches: "{T}: This creature deals 1 damage to any target and 1
 * damage to any target of an opponent's choice").
 *
 * The activating player chose their own (controller) targets up front; those ride on [action].
 * The handler then paused and raised a [com.wingedsheep.engine.core.ChooseTargetsDecision] routed
 * to [deciderId] (the opponent) for the [opponentRequirements]. The resumer converts that response
 * into [ChosenTarget]s, interleaves them with the controller's targets according to
 * [fullRequirements] (preserving script order, so [com.wingedsheep.engine.handlers.EffectContext.buildNamedTargets]
 * maps each to the right requirement), and re-enters the handler with
 * `opponentTargetsChosen = true` so cost payment + stack placement proceed exactly once.
 *
 * The pause happens before any cost is paid, so cancellation pops this frame and restores priority
 * with no side effects.
 *
 * @property action Original [ActivateAbility] carrying the controller's already-chosen targets.
 * @property opponentRequirements The opponent-chosen requirements, in script order; aligned 1:1
 *   with the indices in the [com.wingedsheep.engine.core.TargetsResponse].
 * @property fullRequirements All target requirements (controller + opponent), in script order,
 *   used to interleave the two target lists back into one aligned list.
 * @property deciderId The opponent who makes the selection.
 */
@Serializable
data class ActivateAbilityOpponentTargetContinuation(
    override val decisionId: String,
    val action: ActivateAbility,
    val opponentRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
    val fullRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
    val deciderId: EntityId
) : ContinuationFrame

/**
 * Resume after an activated ability's controller chooses which opponent will choose the
 * "... of an opponent's choice" target. In two-player games the handler skips this frame and
 * routes the target choice directly to the only opponent; this frame exists for multiplayer.
 */
@Serializable
data class ActivateAbilityOpponentChooserContinuation(
    override val decisionId: String,
    val action: ActivateAbility,
    val sourceName: String,
    val opponentRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
    val fullRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
    val opponentIds: List<EntityId>
) : ContinuationFrame

/**
 * Resume after a player picks which permanents to sacrifice to satisfy a
 * [com.wingedsheep.sdk.scripting.costs.CostAtom.Sacrifice] cost on an activated ability.
 *
 * Surfaces the cost-choice the original engine path used to fail on: when an activation's
 * `Sacrifice(filter, count, excludeSelf)` cost has more matching battlefield permanents than
 * `count`, the handler raises a [SelectCardsDecision] and pushes this frame so the resumer can
 * re-enter the handler with the player's chosen permanents filled into
 * `costPayment.sacrificedPermanents`.
 *
 * Skipped (no pause) when `sacrificedPermanents` is already pre-filled (engine-direct path /
 * resumed replay) or when candidate count ≤ required count (no real choice — CostHandler
 * auto-picks). Sage of Lat-Nam ({T}, Sacrifice an artifact: Draw a card) is the canonical case.
 *
 * @property action The original [ActivateAbility] (`costPayment.sacrificedPermanents` still empty),
 *   so the resumer can re-enter the handler with the chosen sacrifice victims filled in.
 * @property sacrificeCandidates The matching battlefield permanents offered to the player as
 *   options; used to validate the response is a subset of the originally legal candidates.
 * @property sacrificeCount Exact number of permanents the player must pick (`count` from the cost).
 * @property distinctNames When true the chosen permanents must all have different names
 *   ("sacrifice three artifact tokens with different names" — Transmutation Font).
 */
@Serializable
data class ActivateAbilitySacrificeContinuation(
    override val decisionId: String,
    val action: ActivateAbility,
    val sacrificeCandidates: List<EntityId>,
    val sacrificeCount: Int,
    val distinctNames: Boolean = false
) : ContinuationFrame

/**
 * Resume after the controller picks which permanents to exile for a variable-count
 * [com.wingedsheep.sdk.scripting.costs.CostAtom.ExilePermanents] cost — "Exile one or more other
 * [filter] you control with total mana value X" (Fabrication Foundry).
 *
 * The bare [ActivateAbility] arrives with no exile selection; the handler raised a
 * [SelectCardsDecision] over the eligible permanents (min [minCount], max = all eligible) and pushed
 * this frame. The resumer validates the pick, fills it into `costPayment.exiledCards`, and re-enters
 * the handler — which then computes X (the exiled set's total mana value) and pauses again for the
 * X-bounded target ([ActivateAbilityControllerTargetContinuation]). The exile pause happens before
 * any cost is paid, so cancellation pops this frame with no side effects.
 *
 * @property action The original [ActivateAbility] (`costPayment.exiledCards` still empty).
 * @property exileCandidates The eligible permanents offered as options (used to validate the pick).
 * @property minCount Minimum number of permanents that must be exiled (the cost's floor).
 */
@Serializable
data class ActivateAbilityExilePermanentsContinuation(
    override val decisionId: String,
    val action: ActivateAbility,
    val exileCandidates: List<EntityId>,
    val minCount: Int
) : ContinuationFrame

/**
 * Resume after the controller picks the target for an activated ability whose target legality
 * depends on a value determined during activation — specifically the X-bounded reanimation target of
 * a [com.wingedsheep.sdk.scripting.costs.CostAtom.ExilePermanents] ability (Fabrication Foundry:
 * "Return target artifact card with mana value X or less from your graveyard to the battlefield").
 *
 * Because X isn't known until the exile selection is made, the target is chosen *after* the exile
 * ([ActivateAbilityExilePermanentsContinuation]) rather than up front. The handler raised a
 * [ChooseTargetsDecision] whose legal targets were found with X threaded through the predicate
 * context, and pushed this frame. The resumer converts the response into [ChosenTarget]s, fills
 * `action.targets`, and re-enters the handler to pay the cost and put the ability on the stack.
 *
 * @property action The original [ActivateAbility] carrying the already-chosen exiled permanents
 *   (and thus the resolved X) but no target yet.
 * @property requirements The controller target requirements, in script order, aligned 1:1 with the
 *   indices in the [TargetsResponse].
 */
@Serializable
data class ActivateAbilityControllerTargetContinuation(
    override val decisionId: String,
    val action: ActivateAbility,
    val requirements: List<TargetRequirement>
) : ContinuationFrame
