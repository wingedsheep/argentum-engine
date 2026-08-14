package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Resume after the spell's controller decides whether to pay a mana cost
 * to prevent their spell from being countered.
 *
 * "Counter target spell unless its controller pays {cost}."
 *
 * @property payingPlayerId The spell's controller who must decide whether to pay
 * @property spellEntityId The spell that will be countered if they don't pay
 * @property manaCost The mana cost to pay
 * @property sourceId The source of the counter-unless-pays effect
 * @property sourceName Name of the source for event messages
 * @property onPaid Optional rider executed only on the "they paid" branch (e.g. Divert
 *   Disaster's "If they do, you create a Lander token"). The rider runs with
 *   [controllerId] as its controller — i.e. the controller of the counter effect, who
 *   is "you" in the rider text — not the spell's controller who paid.
 */
@Serializable
data class CounterUnlessPaysContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val manaCost: ManaCost,
    val sourceId: EntityId?,
    val sourceName: String?,
    val exileOnCounter: Boolean = false,
    val controllerId: EntityId? = null,
    val onPaid: Effect? = null
) : ContinuationFrame

/**
 * Resume after the controller decides whether to pay a mana cost for an optional
 * mana payment effect (e.g., Lightning Rift's "you may pay {1}").
 *
 * If the player pays, the inner effect is executed. If not, nothing happens.
 *
 * @property playerId The player who may pay
 * @property sourceId The source of the effect
 * @property sourceName Name of the source for display
 * @property manaCost The mana cost to pay
 * @property effect The effect to execute if the player pays
 * @property controllerId The controller for effect context
 * @property xValue The X value if applicable
 * @property targets The chosen targets for effect context
 */
@Serializable
data class MayPayManaContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceName: String?,
    val manaCost: ManaCost,
    val effect: Effect,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Resume after the controller selects mana sources to pay a "you may pay" cost
 * for a non-targeted triggered ability. After payment, the inner effect is executed.
 *
 * @property playerId The player paying
 * @property sourceName Name of the source for display
 * @property manaCost The mana cost to pay
 * @property effect The effect to execute after payment
 * @property effectContext The context for effect execution
 * @property availableSources Available mana sources the player can choose from
 * @property autoPaySuggestion Pre-computed auto-tap suggestion
 */
@Serializable
data class MayPayManaSelectionContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceName: String?,
    val manaCost: ManaCost,
    val effect: Effect,
    val effectContext: EffectContext,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>,
    /**
     * True when this is a **waterbend** payment (Avatar: The Last Airbender): before paying the
     * remainder with mana sources, the resumer taps any
     * [ManaSourcesSelectedResponse.waterbendPermanents] the player chose (each paying {1} of the
     * generic, via the shared waterbend payment machinery). Mirrors
     * [CounterUnlessPaysManaSelectionContinuation.waterbend].
     */
    val waterbend: Boolean = false,
    /**
     * The "unless" branch — run when the player declines the payment (no mana sources, no auto-pay,
     * and no waterbend taps). Models the in-resolution "you may pay <cost>. Otherwise, <otherwise>"
     * gate (Waterbending Lesson: "discard a card unless you waterbend {2}"). Null for a plain
     * optional payment where declining simply does nothing.
     */
    val otherwise: Effect? = null
) : ContinuationFrame

/**
 * Resume after the controller chooses an X value for "you may pay {X}" effects.
 * The player selects a number (0 to max affordable), and if > 0, we pay that mana
 * and execute the inner effect with the chosen X value.
 *
 * Example: Decree of Justice cycling trigger - "you may pay {X}. If you do,
 * create X 1/1 white Soldier creature tokens."
 */
@Serializable
data class MayPayXContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceName: String?,
    val effect: Effect,
    val maxX: Int,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Resume after the controller decides whether to pay a mana cost for a triggered
 * ability that also requires targets (e.g., Lightning Rift).
 *
 * The flow is: ask "pay {cost}?" → if yes, show mana source selection → then target selection.
 * This is different from MayPayManaContinuation which asks about targets first.
 *
 * @property trigger The full pending trigger to process if the player pays
 * @property targetRequirement The target requirement for the ability
 * @property manaCost The mana cost to pay
 */
@Serializable
data class MayPayManaTriggerContinuation(
    override val decisionId: String,
    val trigger: PendingTrigger,
    val targetRequirement: TargetRequirement,
    val manaCost: ManaCost
) : ContinuationFrame

/**
 * Resume after the controller selects mana sources to pay a "counter unless pays" cost.
 * After payment, the spell resolves normally. If the player selected invalid sources,
 * the spell is countered.
 *
 * @property payingPlayerId The spell's controller who is paying
 * @property spellEntityId The spell that will be countered if payment fails
 * @property manaCost The mana cost to pay
 * @property availableSources Available mana sources the player can choose from
 * @property autoPaySuggestion Pre-computed auto-tap suggestion
 * @property exileOnCounter Whether to exile the spell if countered
 * @property controllerId The controller of the counter effect
 */
@Serializable
data class CounterUnlessPaysManaSelectionContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val manaCost: ManaCost,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>,
    val exileOnCounter: Boolean = false,
    val controllerId: EntityId? = null,
    /** See [CounterUnlessPaysContinuation.onPaid]. */
    val onPaid: Effect? = null,
    /** See [CounterUnlessPaysContinuation.sourceId]. Carried for the rider's [EffectContext]. */
    val sourceId: EntityId? = null,
    /**
     * Not-yet-paid components of an enclosing [WardCost.Composite] (e.g. the "Pay 2 life" part of
     * "Ward—{2}, Pay 2 life" while the mana part is being paid). When this is paid and the list is
     * non-empty, the resumer charges the next component instead of letting the spell resolve.
     */
    val remainingWardParts: List<WardCost> = emptyList(),
    /** The ward source permanent, carried so a composite cost can re-prompt its next component. */
    val wardSourceId: EntityId? = null,
    /**
     * True when this is a Ward—Waterbend payment (Avatar: The Last Airbender): the resumer first
     * taps any [ManaSourcesSelectedResponse.waterbendPermanents] the player chose (each paying {1}
     * of the generic, via the shared waterbend payment machinery) before paying the remainder with
     * mana sources.
     */
    val waterbend: Boolean = false
) : ContinuationFrame

/**
 * Resume after the controller selects mana sources to pay a cost for a triggered
 * ability that also requires targets.
 *
 * After mana sources are selected and cost is paid, proceeds to target selection.
 *
 * @property trigger The full pending trigger to process after payment
 * @property targetRequirement The target requirement for the ability
 * @property manaCost The mana cost to pay
 * @property availableSources Available mana sources the player can choose from
 * @property autoPaySuggestion Pre-computed auto-tap suggestion
 */
@Serializable
data class ManaSourceSelectionContinuation(
    override val decisionId: String,
    val trigger: PendingTrigger,
    val targetRequirement: TargetRequirement,
    val manaCost: ManaCost,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the controller decides whether to pay a life cost
 * to prevent their spell from being countered (e.g. Ward—Pay 2 life).
 *
 * "Counter target spell unless its controller pays N life."
 *
 * Yes → deduct life and let the spell resolve.
 * No  → counter the spell.
 *
 * @property payingPlayerId The spell's controller who must decide whether to pay
 * @property spellEntityId The spell that will be countered if they don't pay
 * @property lifeCost The life cost to pay
 * @property exileOnCounter If true, counter to exile rather than graveyard
 * @property controllerId Source controller, for counter-to-exile bookkeeping
 */
@Serializable
data class CounterUnlessPaysLifeContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val lifeCost: Int,
    val exileOnCounter: Boolean = false,
    val controllerId: EntityId? = null,
    /** See [CounterUnlessPaysManaSelectionContinuation.remainingWardParts]. */
    val remainingWardParts: List<WardCost> = emptyList(),
    /** See [CounterUnlessPaysManaSelectionContinuation.wardSourceId]. */
    val wardSourceId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after the controller decides whether to discard cards
 * to prevent their spell from being countered (e.g. Ward—Discard a card).
 *
 * Yes → discard [count] cards (chosen by the player, or at random if [random]) and let the spell resolve.
 * No  → counter the spell.
 *
 * @property payingPlayerId The spell's controller who must decide whether to pay
 * @property spellEntityId The spell that will be countered if they don't pay
 * @property count Number of cards to discard
 * @property random Whether the discard is at random (no player selection)
 */
@Serializable
data class CounterUnlessDiscardContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val count: Int,
    val random: Boolean = false,
    val filter: com.wingedsheep.sdk.scripting.GameObjectFilter? = null,
    val exileOnCounter: Boolean = false,
    val controllerId: EntityId? = null,
    /** See [CounterUnlessPaysManaSelectionContinuation.remainingWardParts]. */
    val remainingWardParts: List<WardCost> = emptyList(),
    /** See [CounterUnlessPaysManaSelectionContinuation.wardSourceId]. */
    val wardSourceId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after the controller chooses which permanent(s) to sacrifice to prevent
 * their spell/ability from being countered (e.g. Ward—Sacrifice a Food, CR 702.21).
 *
 * The player is shown an optional card selection (min 0, max [count]). Selecting
 * [count] qualifying permanents pays the cost and the spell resolves; selecting
 * fewer (declining) counters the spell. Valid permanents are recomputed against
 * projected state when the decision is presented, so subtypes granted by continuous
 * effects (Ygra making every other creature a Food) are honored.
 *
 * @property payingPlayerId The spell's controller who must decide whether to pay
 * @property spellEntityId The spell/ability that will be countered if they don't pay
 * @property filter Filter for valid sacrifice fodder (e.g. a Food permanent)
 * @property count Number of permanents that must be sacrificed to pay
 */
@Serializable
data class CounterUnlessSacrificeContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val filter: GameObjectFilter,
    val count: Int = 1,
    val exileOnCounter: Boolean = false,
    val controllerId: EntityId? = null,
    /** See [CounterUnlessPaysManaSelectionContinuation.remainingWardParts]. */
    val remainingWardParts: List<WardCost> = emptyList(),
    /** See [CounterUnlessPaysManaSelectionContinuation.wardSourceId]. */
    val wardSourceId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after the controller chooses which graveyard cards to exile to collect evidence
 * [amount] and so prevent their spell/ability from being countered
 * (Ward—Collect evidence N, CR 701.59 / 702.21 — Axebane Ferox).
 *
 * The decision is a variable-size selection gated on a **sum**, not a count: the player may exile
 * any number of cards as long as their total mana value reaches [amount] (over-paying is legal).
 * A selection that falls short — including the empty selection, which is how declining is
 * expressed — counters the spell. The payment itself runs through `CollectEvidenceResolver`, the
 * single source of truth every collect-evidence context shares, so the legality rule and the exile
 * are identical to the activated-ability and cast-cost forms.
 *
 * The graveyard is re-read at resume time, so cards that left it between the prompt and the
 * response no longer count toward the total.
 *
 * @property payingPlayerId The spell's controller who must decide whether to pay
 * @property spellEntityId The spell/ability that will be countered if they don't pay
 * @property amount The total mana value the exiled cards must reach
 */
@Serializable
data class CounterUnlessCollectEvidenceContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val amount: Int,
    val exileOnCounter: Boolean = false,
    val controllerId: EntityId? = null,
    /** See [CounterUnlessPaysManaSelectionContinuation.remainingWardParts]. */
    val remainingWardParts: List<WardCost> = emptyList(),
    /** See [CounterUnlessPaysManaSelectionContinuation.wardSourceId]. */
    val wardSourceId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after the controller decides whether to take counters on themselves to prevent their
 * spell/ability from being countered (e.g. Ward—Get five poison counters, CR 702.21a / 122.1).
 *
 * Yes → place [amount] [counterType] counters on the paying player and let the spell resolve.
 * No  → counter the spell.
 *
 * There is no can-pay re-check on resume, unlike the life / discard / sacrifice continuations: a
 * player can always get counters, so this cost never becomes unpayable between prompt and response.
 *
 * Ward is the only producer of this frame, and a ward never counters to exile, so unlike its
 * siblings it carries no `exileOnCounter`.
 *
 * @property payingPlayerId The spell's controller who must decide whether to pay
 * @property spellEntityId The spell/ability that will be countered if they don't pay
 * @property counterType The `Counters.*` symbol placed on the payer (e.g. `Counters.POISON`)
 * @property amount How many counters the payer gets
 */
@Serializable
data class CounterUnlessPlayerCountersContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val counterType: String,
    val amount: Int,
    val controllerId: EntityId? = null,
    /** See [CounterUnlessPaysManaSelectionContinuation.remainingWardParts]. */
    val remainingWardParts: List<WardCost> = emptyList(),
    /** See [CounterUnlessPaysManaSelectionContinuation.wardSourceId]. */
    val wardSourceId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after the controller picks **which** of a disjunctive ward cost's options to pay
 * (`WardCost.Choice` — "Ward—Discard a card or pay {2}", CR 702.21a).
 *
 * [options] holds only the options the payer could actually pay when the prompt was built, index
 * aligned with the decision's option labels; the trailing decision option (index == `options.size`)
 * is "Counter spell" and declines. The chosen option is then charged through the ordinary
 * per-cost ward machinery, carrying [remainingWardParts] so a Choice nested inside a
 * `WardCost.Composite` still charges the composite's remaining components afterwards.
 *
 * Ward is the only producer of this frame, and a ward never counters to exile, so unlike the
 * `CounterUnless*` siblings it carries no `exileOnCounter`.
 *
 * @property payingPlayerId The spell's controller who must pick and pay
 * @property spellEntityId The spell/ability that will be countered if they decline
 * @property options The payable options offered, positionally aligned with the decision's labels
 */
@Serializable
data class WardCostChoiceContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val options: List<WardCost>,
    val controllerId: EntityId? = null,
    /** See [CounterUnlessPaysManaSelectionContinuation.remainingWardParts]. */
    val remainingWardParts: List<WardCost> = emptyList(),
    /** See [CounterUnlessPaysManaSelectionContinuation.wardSourceId]. */
    val wardSourceId: EntityId? = null
) : ContinuationFrame

/**
 * Information about a mana source available for manual selection.
 *
 * @property requiresSacrifice Selecting this source also sacrifices the permanent
 *   (e.g. Treasure tokens — "{T}, Sacrifice this artifact: Add one mana of any color").
 *   Auto-pay never picks these; the manual-selection resumer performs the sacrifice
 *   when the player explicitly opts in.
 * @property requiresTappingAnotherPermanent Selecting this source also requires tapping
 *   another permanent (e.g. Springleaf Drum — "{T}, Tap an untapped creature you
 *   control: Add one mana of any color"). Auto-pay never picks these; the
 *   manual-selection resumer pauses for the player to pick which permanent to tap.
 */
@Serializable
data class ManaSourceOption(
    val entityId: EntityId,
    val name: String,
    val producesColors: Set<Color>,
    val producesColorless: Boolean,
    val requiresSacrifice: Boolean = false,
    val requiresTappingAnotherPermanent: Boolean = false
)

/**
 * Resume after the player picks which permanent to tap to satisfy the secondary
 * tap-permanents sub-cost of a mana ability selected during ward payment
 * (e.g. Springleaf Drum's "Tap an untapped creature you control").
 *
 * Pushed by [com.wingedsheep.engine.handlers.continuations.ManaPaymentContinuationResumer]
 * when the player's manual ward-payment selection contains one or more sources with
 * [ManaSourceOption.requiresTappingAnotherPermanent]. The resumer pre-tapped any
 * non-sub-cost sources before pushing this, so [currentPool]-style state already lives
 * on the player's mana pool component.
 *
 * Head of [pendingSubCostSources] is the source the current prompt is targeting; on
 * response, that source is tapped, the chosen creature is tapped, and its mana is added
 * to the pool. If more sub-cost sources remain, a new prompt is pushed; otherwise we
 * attempt to pay the ward cost and either resolve the spell or counter it.
 */
@Serializable
data class WardTapPermanentsSubCostContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val manaCost: com.wingedsheep.sdk.core.ManaCost,
    val exileOnCounter: Boolean,
    val controllerId: EntityId?,
    /** Source IDs still to process. Head is the one the current prompt is for. */
    val pendingSubCostSources: List<EntityId>,
    /** Original mana-source menu, kept for source-name lookups when emitting events. */
    val availableSources: List<ManaSourceOption>,
    /** See [CounterUnlessPaysContinuation.onPaid]. Carried so the rider fires after the
     *  spell's controller finishes paying through a tap-permanents sub-cost source. */
    val onPaid: Effect? = null,
    /** See [CounterUnlessPaysContinuation.sourceId]. Carried for the rider's [EffectContext]. */
    val sourceId: EntityId? = null,
    /** See [CounterUnlessPaysManaSelectionContinuation.remainingWardParts] — the not-yet-paid
     *  components of an enclosing composite ward cost, charged once this mana part is fully paid. */
    val remainingWardParts: List<WardCost> = emptyList(),
    /** See [CounterUnlessPaysManaSelectionContinuation.wardSourceId]. */
    val wardSourceId: EntityId? = null
) : ContinuationFrame

/**
 * Continuation for AddDynamicManaEffect.
 *
 * Resume after a player chooses how to distribute mana among allowed colors.
 * The player picks how much of [firstColor] to add; the remainder goes to [secondColor].
 *
 * @property playerId The player receiving the mana
 * @property sourceId The spell/ability that caused the effect
 * @property sourceName Name of the source for display
 * @property totalAmount Total mana to add
 * @property firstColor First color option
 * @property secondColor Second color option (gets remainder)
 */
@Serializable
data class AddDynamicManaContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val totalAmount: Int,
    val firstColor: Color,
    val secondColor: Color,
    val restriction: ManaRestriction? = null
) : ContinuationFrame

/**
 * Continuation for [com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect] when the
 * effect's allowed-color palette has 3 or more entries — "Add N mana in any combination of
 * colors". The player picks each pip's color independently via a sequence of
 * [ChooseColorDecision]s; this frame is repushed once per remaining pip.
 *
 * @property remainingPips How many pips are still owed at the time the current decision was created
 *   (decrements by 1 per resume; the loop ends when this reaches 0)
 * @property allowedColors The palette the player chooses from for each pip
 * @property restriction Optional restriction stamped onto every pip added to the pool
 */
@Serializable
data class AddManaPipsContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val remainingPips: Int,
    val allowedColors: Set<Color>,
    val restriction: ManaRestriction? = null
) : ContinuationFrame

/**
 * Restores a mana-payment window that was set aside so the player could activate a mana ability
 * inside it (CR 605.3a).
 *
 * Pushed by [com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow.suspend] on top of the payment
 * continuation the window belongs to, so a decision the mana ability raises for itself (a color
 * choice for Birds of Paradise, a Fertile Ground tap bonus) nests above this frame and the window
 * is re-raised only once the ability has fully resolved. Carries the decision verbatim; the
 * auto-resumer refreshes it against the post-activation board before re-raising it.
 */
@Serializable
data class ReopenManaPaymentDecisionContinuation(
    override val decisionId: String,
    val decision: SelectManaSourcesDecision
) : ContinuationFrame
