package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.Serializable

/**
 * Resume after player chooses a mode for a modal spell/ability.
 *
 * When a modal effect (e.g., "Choose one —") is executed, the player is presented
 * with a list of modes. After they choose, we need to execute the chosen mode's
 * effect, potentially after target selection.
 *
 * For "Choose N" modal spells (Commands), the player iteratively picks modes one
 * at a time. [selectedModeIndices] accumulates the picks (in order) and
 * [availableIndices] narrows the options each step. Once
 * `selectedModeIndices.size == chooseCount`, the chosen modes are executed in
 * the order they were selected.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that has the modal effect
 * @property sourceName Name of the source for event messages
 * @property modes The full list of modes (indexed by original position)
 * @property xValue The X value if applicable
 * @property chooseCount Total modes to pick (1 for classic modal, 2+ for Commands)
 * @property selectedModeIndices Original mode indices already picked, in order
 * @property availableIndices Original mode indices still offered; null = all
 * @property outerTargets Targets from the enclosing spell/ability (e.g., the
 *   target chosen by a triggered ability whose effect is a ModalEffect). These
 *   are propagated into each no-target mode's EffectContext so inner effects
 *   can resolve `EffectTarget.ContextTarget(n)` references to the outer scope.
 * @property outerNamedTargets Named targets from the enclosing pipeline state.
 */
@Serializable
data class ModalContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val modes: List<@Serializable Mode>,
    val xValue: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val chooseCount: Int = 1,
    /**
     * Minimum modes the player must pick (rule 700.2). When < [chooseCount], the
     * player may decline subsequent picks once this threshold is met — supports
     * "choose up to one" / "choose one or more" patterns.
     */
    val minChooseCount: Int = chooseCount,
    val selectedModeIndices: List<Int> = emptyList(),
    val availableIndices: List<Int>? = null,
    val outerTargets: List<ChosenTarget> = emptyList(),
    val outerNamedTargets: Map<String, ChosenTarget> = emptyMap(),
    /**
     * "Choose one that hasn't been chosen" (Gandalf the Grey): when true, each chosen
     * mode index is recorded in the source's
     * [com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent] so
     * later triggers exclude it.
     */
    val recordChosenModesOnSource: Boolean = false,
    /**
     * "Choose one that hasn't been chosen this turn" (Breeches, Eager Pillager): when true, each
     * chosen mode index is recorded in the source's
     * [com.wingedsheep.engine.state.components.battlefield.ChosenModesThisTurnComponent] so later
     * triggers *this turn* exclude it. Cleared at end of turn. Turn-scoped sibling of
     * [recordChosenModesOnSource].
     */
    val recordChosenModesThisTurn: Boolean = false
) : ContinuationFrame

/**
 * One queued (effect, targets, requirements) triple: an effect whose targets were already chosen, to
 * be executed with a context scoped to *its own* target slice — so its `ContextTarget(0)` means its
 * first target, not some enclosing spell's.
 *
 * Drained in order by
 * [com.wingedsheep.engine.handlers.effects.composite.processPreTargetedEffectQueue], which two
 * mechanics feed:
 *  - a **choose-N modal spell** whose modes and per-mode targets were picked at cast time
 *    (CR 700.2 / 601.2b–c) — see [ModalPreChosenContinuation]; and
 *  - the **splice tail** of a spell with cards spliced onto it (CR 702.47b — the spliced cards' text
 *    happens in the chosen order after the main spell's own effects) — see [SpliceTailContinuation].
 */
@Serializable
data class PreTargetedEffectEntry(
    val effect: @Serializable Effect,
    val targets: List<ChosenTarget>,
    val targetRequirements: List<@Serializable TargetRequirement>
)

/**
 * Auto-resumed continuation that drains the remaining modes of a choose-N modal
 * spell whose modes and targets were picked at cast time (rules 700.2, 601.2b–c).
 *
 * [com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor] iterates
 * the pre-chosen modes synchronously, but when a mode's effect pauses for a nested
 * decision (e.g., a reflexive trigger or ChooseAction inside the mode), it pushes
 * this frame BELOW the nested decision's continuation so that once the inner chain
 * resolves, the remaining mode queue resumes automatically via
 * [com.wingedsheep.engine.handlers.continuations.CoreAutoResumerModule].
 *
 * Carrying [remainingEntries] in the frame keeps the queue independent of the
 * spell's ModalEffect — remove-by-effect avoids any dependency on mode list
 * ordering, which matters for `allowRepeat` where the same mode index can appear
 * twice with different targets.
 */
@Serializable
data class ModalPreChosenContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val xValue: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val remainingEntries: List<PreTargetedEffectEntry>
) : ContinuationFrame

/**
 * Auto-resumed continuation that runs the **spliced text** of a spell with cards spliced onto it
 * (CR 702.47b — "The effects of the main spell must happen first", then each spliced card's text in
 * the order the caster chose).
 *
 * [com.wingedsheep.engine.mechanics.stack.StackResolver] pushes this frame BELOW the main spell's own
 * effect before executing it, so the splice tail runs whether the main effect completes synchronously
 * or pauses for a decision of its own (Through the Breach's "put a creature card from your hand onto
 * the battlefield" is exactly such a pause). Each entry carries its own target slice, so a spliced
 * card's `ContextTarget(0)` resolves to its own first target.
 *
 * [sourceId] is the *spell's* entity, never the spliced card's: the spell gained only the card's rules
 * text, not its characteristics (CR 702.47c), so spliced damage is dealt by the spell — which is why a
 * red splice card's damage on a blue Arcane spell can still hit a creature with protection from red.
 */
@Serializable
data class SpliceTailContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val remainingEntries: List<PreTargetedEffectEntry>
) : ContinuationFrame

/**
 * Auto-resumed continuation that drains the remaining chosen modes of a
 * resolution-time modal ability (rule 603.3c — modal triggered/activated abilities
 * that pick modes as they resolve, e.g. Bumi's "choose up to X" ETB).
 *
 * The resolution-time path
 * ([com.wingedsheep.engine.handlers.continuations.ModalAndCloneContinuationResumer.processChosenModeQueue])
 * executes each chosen mode in pick order. A mode's own effect can itself pause for a
 * nested decision (a targeted [com.wingedsheep.sdk.dsl.Effects.Scry]'s reorder prompt,
 * a ChooseAction, a reflexive trigger, …). Before executing each mode this frame is
 * pushed BELOW that mode's nested decision so, once the inner chain resolves, the
 * remaining modes resume automatically via
 * [com.wingedsheep.engine.handlers.continuations.CoreAutoResumerModule] — the
 * resolution-time twin of [ModalPreChosenContinuation].
 *
 * Without it, a nested pause mid-mode drops every not-yet-executed mode (Bumi:
 * choosing "scry 3" then "earthbend 3" never reached the earthbend land target).
 */
@Serializable
data class ModalChosenModeTailContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val xValue: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val remainingChosenModes: List<@Serializable Mode>,
    /** Outer-scope targets propagated to any remaining no-target modes. See
     *  [ModalContinuation.outerTargets]. */
    val outerTargets: List<ChosenTarget> = emptyList(),
    val outerNamedTargets: Map<String, ChosenTarget> = emptyMap()
) : ContinuationFrame

/**
 * Resume after player selects targets for a chosen mode of a modal spell.
 *
 * After mode selection, if the chosen mode requires targets, this continuation
 * is pushed while the player selects targets.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that has the modal effect
 * @property sourceName Name of the source for event messages
 * @property effect The chosen mode's effect to execute
 * @property xValue The X value if applicable
 */
@Serializable
data class ModalTargetContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val effect: Effect,
    val xValue: Int? = null,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    /** Original modes list for cancelling back to mode selection */
    val modes: List<@Serializable Mode>? = null,
    val triggeringEntityId: EntityId? = null,
    /** For "Choose N" modal spells: modes still queued to execute after this one. */
    val remainingChosenModes: List<@Serializable Mode> = emptyList(),
    /** Outer-scope targets from the enclosing spell/ability, propagated to any
     *  remaining no-target modes. See [ModalContinuation.outerTargets]. */
    val outerTargets: List<ChosenTarget> = emptyList(),
    val outerNamedTargets: Map<String, ChosenTarget> = emptyMap()
) : ContinuationFrame

/**
 * Resume after player selects a creature to copy for Clone-style effects.
 *
 * When a permanent with EntersAsCopy resolves, the player is asked to choose
 * a creature on the battlefield. This continuation handles the response
 * and completes the permanent's entry to the battlefield.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 * @property castFaceDown Whether the spell was cast face-down
 * @property optional Whether the copy is optional (Clone is optional)
 * @property additionalSubtypes Subtypes to add to the copy (e.g., "Bird" for Mockingbird)
 * @property additionalKeywords Keywords to grant to the copy (e.g., FLYING for Mockingbird)
 * @property nameOverride When non-null, the copy keeps this name instead of the copied object's
 *   name (Superior Spider-Man: "except his name is Superior Spider-Man")
 * @property powerOverride When non-null, overrides the copy's base power (Superior Spider-Man: 4)
 * @property toughnessOverride When non-null, overrides the copy's base toughness (Superior Spider-Man: 4)
 * @property exileCopiedCard When true, exile the copied card after copying it (Superior Spider-Man:
 *   "When you do, exile that card"). Used for graveyard copies.
 * @property additionalCounters When non-null, the number of additional +1/+1 counters the copy
 *   enters with (Altered Ego's "except it enters with X additional +1/+1 counters on it"). Applied
 *   only when a copy was actually made — declining the copy declines the counters too.
 */
@Serializable
data class CloneEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val castFaceDown: Boolean,
    val additionalSubtypes: List<String> = emptyList(),
    val additionalKeywords: List<Keyword> = emptyList(),
    val nameOverride: String? = null,
    val powerOverride: Int? = null,
    val toughnessOverride: Int? = null,
    val exileCopiedCard: Boolean = false,
    val additionalCounters: DynamicAmount? = null
) : ContinuationFrame

/**
 * Resume after a player chooses (or declines) a card/permanent to copy for an
 * [com.wingedsheep.sdk.scripting.EntersAsCopy] replacement on a permanent that has *already* been
 * placed on the battlefield **directly** — a land played (Echoing Deeps / Vesuva / Thespian's Stage)
 * or a token/put-onto-battlefield permanent — rather than a spell resolving off the stack (which uses
 * [CloneEntersContinuation]).
 *
 * The resumer copies the chosen object's copiable characteristics (CR 707.2) onto the entity, adds
 * any [additionalSubtypes] / [additionalKeywords] and overrides, taps the entity if
 * [tappedIfCopied] and a copy was actually made, optionally exiles the copied card, then fires the
 * entry's ETB triggers off a synthesized [ZoneChangeEvent] (so the copied identity's landfall /
 * "when ~ enters" triggers see the final characteristics). Declining leaves the permanent as its
 * printed self, untapped.
 *
 * @property entityId The permanent already on the battlefield.
 * @property controllerId Its controller (also the copy chooser).
 * @property fromZone The zone the permanent came from, used to synthesize the entry event.
 * @property tappedIfCopied Tap the permanent iff it entered as a copy ("enter tapped as a copy").
 * @property additionalCounters Additional +1/+1 counters the permanent enters with iff it entered
 *   as a copy. See [CloneEntersContinuation.additionalCounters].
 */
@Serializable
data class CloneEntersOnBattlefieldContinuation(
    override val decisionId: String,
    val entityId: EntityId,
    val controllerId: EntityId,
    val fromZone: Zone? = null,
    val additionalSubtypes: List<String> = emptyList(),
    val additionalKeywords: List<Keyword> = emptyList(),
    val nameOverride: String? = null,
    val powerOverride: Int? = null,
    val toughnessOverride: Int? = null,
    val exileCopiedCard: Boolean = false,
    val tappedIfCopied: Boolean = false,
    val additionalCounters: DynamicAmount? = null
) : ContinuationFrame

/**
 * Resume after player makes an "as enters" choice for a spell being resolved.
 *
 * Handles all choice types (color, creature type, creature on battlefield) via
 * the [choiceType] discriminator. For creature type choices, [creatureTypes] holds
 * the options presented. After storing the chosen value, checks for chained choices
 * (e.g., Riptide Replicator needs both color AND creature type).
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 * @property choiceType What kind of choice was presented
 * @property creatureTypes For CREATURE_TYPE choices, the list of options presented
 */
@Serializable
data class EntersWithChoiceSpellContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val choiceType: com.wingedsheep.sdk.scripting.ChoiceType,
    val creatureTypes: List<String> = emptyList(),
    /**
     * For [com.wingedsheep.sdk.scripting.ChoiceType.MODE] choices, the
     * positionally aligned list of mode ids. The response's chosen index
     * indexes into this list to recover the stable mode id stored on the
     * resulting [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent].
     */
    val modeOptionIds: List<String> = emptyList(),
    /**
     * For [com.wingedsheep.sdk.scripting.ChoiceType.BASIC_LAND_TYPE] choices, the
     * positionally aligned list of basic land type options presented. The response's
     * chosen index indexes into this list.
     */
    val landTypes: List<String> = emptyList(),
    /**
     * For [com.wingedsheep.sdk.scripting.ChoiceType.OPPONENT] choices, the positionally
     * aligned list of candidate opponent player entity ids presented in the decision.
     * The response's chosen index indexes into this list to recover the player id
     * stored on the resulting `CastChoicesComponent` under
     * [com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT].
     */
    val opponentIds: List<EntityId> = emptyList(),
    /**
     * For [com.wingedsheep.sdk.scripting.ChoiceType.CARD_NAME] choices, the positionally
     * aligned list of land card names presented. The response's chosen index indexes into
     * this list to recover the name stored under
     * [com.wingedsheep.sdk.scripting.ChoiceSlot.CARD_NAME].
     */
    val cardNames: List<String> = emptyList(),
    /**
     * True when this MODE choice is a **synthesized Riot** choice granted to the entity (not printed
     * on its card). The resumer then applies the chosen branch (a +1/+1 counter or a haste grant)
     * directly, because a granted permanent has no printed `EntersWithCounters`/haste static.
     */
    val syntheticRiot: Boolean = false,
    /**
     * The number of *further* synthesized Riot choices to present after this one (CR 702.136b — each
     * granted riot instance is a separate choice). The resumer re-pauses that many more times.
     */
    val syntheticRiotRemaining: Int = 0
) : ContinuationFrame

/**
 * Resume after a player makes an "as enters" choice for a permanent put **directly** onto the
 * battlefield (not cast as a spell) — a land played, or a token minted from a card definition
 * (e.g. the Momir Basic avatar's random creature). Set up via
 * [com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements.pauseForEntersWithChoice].
 *
 * Unlike [EntersWithChoiceSpellContinuation] (used for spells), the permanent is already on the
 * battlefield when this continuation fires — it just needs the chosen value stored. After storing,
 * checks for chained choices (e.g. both color and creature type), and once the final choice
 * resolves, fires the entry's ETB triggers off a synthesized [com.wingedsheep.engine.core.ZoneChangeEvent]
 * (landfall, "when ~ enters", …).
 *
 * @property entityId The permanent already on the battlefield
 * @property controllerId The player who controls it
 * @property choiceType What kind of choice was presented
 * @property creatureTypes For CREATURE_TYPE choices, the list of options presented
 * @property fromZone The zone the permanent came from (used to synthesize the entry event after
 *   the final choice resolves); `null` for a freshly-minted token with no prior zone.
 */
@Serializable
data class EntersWithChoiceOnBattlefieldContinuation(
    override val decisionId: String,
    val entityId: EntityId,
    val controllerId: EntityId,
    val choiceType: com.wingedsheep.sdk.scripting.ChoiceType,
    val creatureTypes: List<String> = emptyList(),
    /** For [com.wingedsheep.sdk.scripting.ChoiceType.MODE] choices, see
     *  [EntersWithChoiceSpellContinuation.modeOptionIds]. */
    val modeOptionIds: List<String> = emptyList(),
    /** For [com.wingedsheep.sdk.scripting.ChoiceType.BASIC_LAND_TYPE] choices, see
     *  [EntersWithChoiceSpellContinuation.landTypes]. */
    val landTypes: List<String> = emptyList(),
    /** For [com.wingedsheep.sdk.scripting.ChoiceType.OPPONENT] choices, see
     *  [EntersWithChoiceSpellContinuation.opponentIds]. */
    val opponentIds: List<EntityId> = emptyList(),
    /** For [com.wingedsheep.sdk.scripting.ChoiceType.CARD_NAME] choices, the land card names
     *  presented (the resumer stores the chosen name by index). */
    val cardNames: List<String> = emptyList(),
    val fromZone: Zone? = null,
    /** See [EntersWithChoiceSpellContinuation.syntheticRiot]. */
    val syntheticRiot: Boolean = false,
    /** See [EntersWithChoiceSpellContinuation.syntheticRiotRemaining]. */
    val syntheticRiotRemaining: Int = 0
) : ContinuationFrame

/**
 * Resume after player answers yes/no to "pay life or enter tapped" for a land played directly.
 *
 * Used for shock lands like Steam Vents. The land is already on the battlefield when this
 * continuation fires — if the player pays life, the land stays untapped; if not, it gets tapped.
 *
 * @property landId The land entity already on the battlefield
 * @property controllerId The player who played the land
 * @property lifeCost The amount of life to pay if they choose yes
 * @property fromZone The zone the land was played from (for the ZoneChangeEvent)
 */
@Serializable
data class PayLifeOrEnterTappedLandContinuation(
    override val decisionId: String,
    val landId: EntityId,
    val controllerId: EntityId,
    val lifeCost: Int,
    val fromZone: Zone
) : ContinuationFrame

/**
 * Resume after player answers yes/no to "pay life or enter tapped" for a spell resolving.
 *
 * Used for shock lands entering via effects (e.g., fetched onto the battlefield).
 * The spell is still being resolved — if the player pays life, the permanent enters untapped;
 * if not, it gets tapped.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who controls the spell
 * @property ownerId The owner of the card
 * @property lifeCost The amount of life to pay if they choose yes
 */
@Serializable
data class PayLifeOrEnterTappedSpellContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val lifeCost: Int
) : ContinuationFrame

/**
 * Resume after player reveals cards for enters-with-reveal-counters (Amplify mechanic).
 *
 * When a creature with this replacement effect enters, the controller may reveal
 * cards matching a filter. For each revealed card, N counters are placed on the
 * creature as it enters.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The card's owner
 * @property counterType Counter type description (e.g., "+1/+1")
 * @property countersPerReveal Number of counters per revealed card
 */
@Serializable
data class RevealCountersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val counterType: String,
    val countersPerReveal: Int
) : ContinuationFrame

/** Resume an as-enters linked-exile selection before the permanent enters the battlefield. */
@Serializable
data class ExileCountersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val counterType: String,
    val countersPerCard: Int
) : ContinuationFrame

/**
 * Resume after player selects permanents to sacrifice for Devour (CR 702.82).
 *
 * When a permanent with [com.wingedsheep.sdk.scripting.EntersWithDevour] is resolving
 * from the stack, the controller is prompted to pick any number of own permanents
 * matching the devour sacrifice filter. After they choose, this resumer sacrifices
 * those permanents, places `multiplier × count` counters on the entering spell entity,
 * then completes the permanent entry to the battlefield.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The card's owner
 * @property multiplier Counters placed per sacrificed permanent
 * @property counterType Serialized counter type (string form of [com.wingedsheep.sdk.scripting.events.CounterTypeFilter])
 */
@Serializable
data class DevourEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val multiplier: Int,
    val counterType: String
) : ContinuationFrame

/**
 * Resume after player chooses a budget modal combination (e.g., Season cycle pawprint modes).
 *
 * The executor pre-computes all valid combinations of modes that fit within the budget
 * Iterative mode selection: each step the player picks one mode (or "Done"),
 * deducting from the remaining budget. Modes execute in the order chosen,
 * giving the player control over sequencing.
 *
 * @property controllerId The player who controls the spell
 * @property sourceId The spell entity
 * @property sourceName Name of the source for event messages
 * @property modes The budget modes (cost + effect)
 * @property remainingBudget How many pawprints are left to spend
 * @property selectedModeIndices Mode indices selected so far, in order of selection
 */
@Serializable
data class BudgetModalContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val modes: List<@Serializable BudgetMode>,
    val remainingBudget: Int,
    val selectedModeIndices: List<Int> = emptyList(),
) : ContinuationFrame

/**
 * Resume after player chooses a permanent to create a token copy of.
 *
 * Used by CreateTokenCopyOfChosenPermanentEffect — the player selects one
 * artifact/creature/permanent they control, and a token copy is created.
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell/ability source
 * @property sourceName Name of the source for display
 */
@Serializable
data class CreateTokenCopyOfChosenContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after the controller chooses what an Aura token copy will enchant (CR 303.4h).
 *
 * A token copy of an Aura is created on the battlefield rather than cast, so its controller
 * picks a host instead of targeting one. The pick is raised *before* the token exists so it can
 * enter already attached — see
 * [com.wingedsheep.engine.handlers.effects.token.AuraTokenHostChooser].
 *
 * @property effect The original create-token-copy effect, re-executed for one token per pick
 * @property context The resolution context, so the effect's target still resolves on resume
 * @property controllerId The player creating (and choosing for) the token
 * @property auraDefinitionId Card definition of the copied Aura, for re-deriving legal hosts
 * @property auraName The copied Aura's name, for the next prompt
 * @property remaining How many Aura tokens are still owed, including the one this pick creates
 */
@Serializable
data class CreateTokenCopyAuraHostContinuation(
    override val decisionId: String,
    val effect: com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect,
    val context: com.wingedsheep.engine.handlers.EffectContext,
    val controllerId: EntityId,
    val auraDefinitionId: String,
    val auraName: String,
    val remaining: Int
) : ContinuationFrame

/**
 * Auto-resumed continuation that creates the **remaining** token copies of a multi-token
 * create-token-copy effect after one token paused for an "as-enters" choice (a printed
 * [com.wingedsheep.sdk.scripting.EntersWithChoice] or a synthesized granted-riot choice — CR 614.12 /
 * 702.136).
 *
 * A create-token-copy effect makes N tokens in a loop; each token must resolve its own as-enters
 * choice (CR 707.2 — the copy has the copied card's abilities). When a token pauses, this frame is
 * pushed **below** the choice's
 * [EntersWithChoiceOnBattlefieldContinuation] so that, once that token's choice (and every granted-riot
 * instance / chained printed choice) has resolved and its ETB triggers fired, the batch resumes here
 * and creates the next token — which may pause again, pushing a fresh frame with a smaller [remaining].
 * The resolution-time twin of [ModalPreChosenContinuation] / [ModalChosenModeTailContinuation]'s
 * "push a tail frame below the nested decision" pattern; the copy analogue of
 * [CreateTokenCopyAuraHostContinuation]'s per-token re-entry.
 *
 * [effect] is the original create-token-copy effect (a
 * [com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect] or
 * [com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect]) and [context] the resolution
 * context, both re-used so the copy's source/target still resolves on resume — mirroring
 * [CreateTokenCopyAuraHostContinuation].
 *
 * @property remaining how many more token copies are owed (always > 0 while this frame is live).
 */
@Serializable
data class CreateTokenCopyRemainingContinuation(
    override val decisionId: String,
    val effect: @Serializable Effect,
    val context: com.wingedsheep.engine.handlers.EffectContext,
    val controllerId: EntityId,
    val remaining: Int,
) : ContinuationFrame

/**
 * Resume after a player chooses an action from a list of labeled options.
 *
 * Used by [ChooseActionEffect] — the player is presented with feasible options
 * and picks one. This continuation stores the choices and original context
 * so the chosen effect can be executed with the correct targets.
 *
 * @property choosingPlayerId The player who is making the choice
 * @property controllerId The ability controller (for effect context)
 * @property sourceId The spell/ability source
 * @property sourceName Name of the source for display
 * @property choices The feasible choices presented to the player (indices match the decision options)
 * @property targets Original targets from the effect context (preserved for ContextTarget resolution)
 * @property namedTargets Named targets from the pipeline state
 * @property triggeringEntityId The entity that triggered the ability
 */
@Serializable
data class ChooseActionContinuation(
    override val decisionId: String,
    val choosingPlayerId: EntityId,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val choices: List<@Serializable EffectChoice>,
    val targets: List<ChosenTarget> = emptyList(),
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    val triggeringEntityId: EntityId? = null
) : ContinuationFrame
