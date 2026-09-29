package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.view.EventPresentationFactory
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Manages the stack: casting spells, activating abilities, and resolution.
 *
 * A façade over one collaborator per concern, so every caller keeps a single entry point:
 * - [SpellCaster] — putting a cast spell on the stack (CR 601.2)
 * - [StackPlacement] — putting spell copies and triggered / activated abilities on the stack
 * - [SpellResolver] — resolving a spell (CR 608.2), routing to [PermanentSpellResolver]
 *   (with [PermanentEntry] and [EntersWithChoicePrompt]) or [NonPermanentSpellResolver]
 * - [AbilityResolver] — resolving a triggered or activated ability
 * - [ResolutionTargetValidator] — the CR 608.2b target re-check both resolvers share
 * - [SpellCounterer] — countering and exiling stack objects
 *
 * Built once, by [com.wingedsheep.engine.core.EngineServices]. [StackPlacement] and
 * [SpellCounterer] need nothing from the resolution machinery, so an executor that only copies,
 * counters or exiles a stack object uses them directly rather than constructing one of these.
 */
class StackResolver(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val effects: EffectExecutorRegistry,
    private val spellCounterer: SpellCounterer,
    private val predicateEvaluator: PredicateEvaluator,
    /** The cast-time target validator, which re-validates a spliced card's own targets (CR 702.47d). */
    private val spliceTargetValidator: com.wingedsheep.engine.mechanics.targeting.TargetValidator,
    private val staticAbilityHandler: StaticAbilityHandler = StaticAbilityHandler(cardRegistry)
) {
    private val spellCaster = SpellCaster(
        cardRegistry, staticAbilityHandler, EventPresentationFactory(Visibility(cardRegistry, conditionEvaluator = predicateEvaluator.conditions))
    )
    private val targetValidator = ResolutionTargetValidator(predicateEvaluator)
    private val entersWithChoicePrompt = EntersWithChoicePrompt(cardRegistry)
    private val permanentEntry = PermanentEntry(cardRegistry, staticAbilityHandler, conditionEvaluator = predicateEvaluator.conditions)
    private val nonPermanentSpellResolver = NonPermanentSpellResolver(zones, cardRegistry, effects, predicateEvaluator, spliceTargetValidator)
    private val spellResolver = SpellResolver(
        cardRegistry = cardRegistry,
        predicateEvaluator = predicateEvaluator,
        targetValidator = targetValidator,
        permanentSpellResolver = PermanentSpellResolver(
            cardRegistry, effects, predicateEvaluator, permanentEntry, entersWithChoicePrompt
        ),
        nonPermanentSpellResolver = nonPermanentSpellResolver
    )
    private val abilityResolver = AbilityResolver(effects, targetValidator, conditionEvaluator = predicateEvaluator.conditions)

    // =========================================================================
    // Putting objects on the stack
    // =========================================================================

    /** Put a spell on the stack. See [SpellCaster.castSpell] for the parameters. */
    fun castSpell(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        targets: List<ChosenTarget> = emptyList(),
        xValue: Int? = null,
        sacrificedPermanents: List<EntitySnapshot> = emptyList(),
        castFaceDown: Boolean = false,
        castTransformed: Boolean = false,
        damageDistribution: Map<EntityId, Int>? = null,
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenCreatureType: String? = null,
        exiledCardCount: Int = 0,
        additionalCostBlightAmount: Int = 0,
        additionalCostPayXLifeAmount: Int? = null,
        declaredCostSlot: ChoiceSlot? = null,
        additionalCostChoices: Map<ChoiceSlot, Int> = emptyMap(),
        wasBlightPaid: Boolean = false,
        wasWaterbendPaid: Boolean = false,
        additionalEntryCounters: com.wingedsheep.engine.state.components.stack.AdditionalEntryCounters? = null,
        giftRecipient: EntityId? = null,
        wasWarped: Boolean = false,
        wasDashed: Boolean = false,
        wasEvoked: Boolean = false,
        wasImpending: Boolean = false,
        wasCleaved: Boolean = false,
        wasSneaked: Boolean = false,
        sneakAttackDefenderId: EntityId? = null,
        wasWebSlung: Boolean = false,
        webSlungReturnedManaValue: Int = 0,
        wasMayhem: Boolean = false,
        chosenModes: List<Int> = emptyList(),
        modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
        modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
        modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),
        /** Card-definition names spliced onto this spell, in splice order (CR 702.47a). */
        splicedCardNames: List<String> = emptyList(),
        totalManaSpent: Int = 0,
        beheldCards: List<EntityId> = emptyList(),
        convokedCreatures: Map<EntityId, Long> = emptyMap(),
        discardedAsCostCards: List<EntityId> = emptyList(),
        exiledAsCostCards: List<EntityId> = emptyList(),
        exiledAsCostSnapshots: List<EntitySnapshot> = emptyList(),
        chosenEntitySnapshots: List<EntitySnapshot> = emptyList(),
        manaSpentWhite: Int = 0,
        manaSpentBlue: Int = 0,
        manaSpentBlack: Int = 0,
        manaSpentRed: Int = 0,
        manaSpentGreen: Int = 0,
        manaSpentColorless: Int = 0,
        manaSpentOnXByColor: Map<Color, Int> = emptyMap(),
        faceIndex: Int? = null,
        spentManaProvenance: com.wingedsheep.engine.mechanics.mana.SpentManaProvenance =
            com.wingedsheep.engine.mechanics.mana.SpentManaProvenance(),
        castTimeFlags: Set<String> = emptySet(),
        alternativeCost: com.wingedsheep.engine.core.AlternativeCostType? = null,
        // Payment can tap or remove the source that made the origin visible. This immutable
        // input is consulted only while capturing the event; the event retains no GameState.
        castOriginState: GameState = state
    ): ExecutionResult =
        spellCaster.castSpell(
            state = state,
            cardId = cardId,
            casterId = casterId,
            targets = targets,
            xValue = xValue,
            sacrificedPermanents = sacrificedPermanents,
            castFaceDown = castFaceDown,
            castTransformed = castTransformed,
            damageDistribution = damageDistribution,
            targetRequirements = targetRequirements,
            chosenCreatureType = chosenCreatureType,
            exiledCardCount = exiledCardCount,
            additionalCostBlightAmount = additionalCostBlightAmount,
            additionalCostPayXLifeAmount = additionalCostPayXLifeAmount,
            declaredCostSlot = declaredCostSlot,
            additionalCostChoices = additionalCostChoices,
            wasBlightPaid = wasBlightPaid,
            wasWaterbendPaid = wasWaterbendPaid,
            additionalEntryCounters = additionalEntryCounters,
            giftRecipient = giftRecipient,
            wasWarped = wasWarped,
            wasDashed = wasDashed,
            wasEvoked = wasEvoked,
            wasImpending = wasImpending,
            wasCleaved = wasCleaved,
            wasSneaked = wasSneaked,
            sneakAttackDefenderId = sneakAttackDefenderId,
            wasWebSlung = wasWebSlung,
            webSlungReturnedManaValue = webSlungReturnedManaValue,
            wasMayhem = wasMayhem,
            chosenModes = chosenModes,
            modeTargetsOrdered = modeTargetsOrdered,
            modeTargetRequirements = modeTargetRequirements,
            modeDamageDistribution = modeDamageDistribution,
            splicedCardNames = splicedCardNames,
            totalManaSpent = totalManaSpent,
            beheldCards = beheldCards,
            convokedCreatures = convokedCreatures,
            discardedAsCostCards = discardedAsCostCards,
            exiledAsCostCards = exiledAsCostCards,
            exiledAsCostSnapshots = exiledAsCostSnapshots,
            chosenEntitySnapshots = chosenEntitySnapshots,
            manaSpentWhite = manaSpentWhite,
            manaSpentBlue = manaSpentBlue,
            manaSpentBlack = manaSpentBlack,
            manaSpentRed = manaSpentRed,
            manaSpentGreen = manaSpentGreen,
            manaSpentColorless = manaSpentColorless,
            manaSpentOnXByColor = manaSpentOnXByColor,
            faceIndex = faceIndex,
            spentManaProvenance = spentManaProvenance,
            castTimeFlags = castTimeFlags,
            alternativeCost = alternativeCost,
            castOriginState = castOriginState
        )

    /** Put a triggered ability on the stack. See [StackPlacement.putTriggeredAbility]. */
    fun putTriggeredAbility(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        causedByAttack: Boolean = false
    ): ExecutionResult =
        StackPlacement.putTriggeredAbility(state, ability, targets, targetRequirements, causedByAttack)

    /** Put a copy of a spell on the stack (CR 707.10). See [StackPlacement.putSpellCopy]. */
    fun putSpellCopy(
        state: GameState,
        sourceSpellId: EntityId,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenModes: List<Int>? = null,
        modeTargetsOrdered: List<List<ChosenTarget>>? = null,
        modeTargetRequirements: Map<Int, List<TargetRequirement>>? = null,
        copyIndex: Int? = null,
        copyTotal: Int? = null,
        controllerId: EntityId? = null
    ): ExecutionResult =
        StackPlacement.putSpellCopy(
            state, sourceSpellId, targets, targetRequirements, chosenModes, modeTargetsOrdered,
            modeTargetRequirements, copyIndex, copyTotal, controllerId
        )

    /** Put an activated ability on the stack. See [StackPlacement.putActivatedAbility]. */
    fun putActivatedAbility(
        state: GameState,
        ability: ActivatedAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        emitActivationEvent: Boolean = true,
        costsTap: Boolean = false,
        isExhaust: Boolean = false,
        cantBeCopied: Boolean = false,
        isLoyalty: Boolean = false,
        loyaltyCountersRemoved: Int = 0,
    ): ExecutionResult =
        StackPlacement.putActivatedAbility(
            state, ability, targets, targetRequirements, emitActivationEvent, costsTap, isExhaust,
            cantBeCopied, isLoyalty, loyaltyCountersRemoved
        )

    // =========================================================================
    // Resolution
    // =========================================================================

    /**
     * Resolve the top item on the stack.
     */
    fun resolveTop(state: GameState): ExecutionResult {
        val topId = state.getTopOfStack()
            ?: return ExecutionResult.error(state, "Stack is empty")

        val container = state.getEntity(topId)
            ?: return ExecutionResult.error(state, "Stack item not found: $topId")

        // Pop from stack
        val (_, poppedState) = state.popFromStack()

        // Determine what type of item this is
        return when {
            container.has<SpellOnStackComponent>() ->
                spellResolver.resolveSpell(poppedState, topId, container)

            container.has<TriggeredAbilityOnStackComponent>() ->
                abilityResolver.resolveTriggeredAbility(poppedState, topId, container)

            container.has<ActivatedAbilityOnStackComponent>() ->
                abilityResolver.resolveActivatedAbility(poppedState, topId, container)

            else ->
                ExecutionResult.error(state, "Unknown stack item type")
        }
    }

    /** Finish only the captured resolving spell, never a later visit of the same card. */
    fun finishResolvingSpell(state: GameState, continuation: FinishResolvingSpellContinuation): ExecutionResult =
        nonPermanentSpellResolver.finishResolvingSpell(state, continuation)

    /**
     * Complete the permanent entry to the battlefield (shared between normal resolution and clone
     * continuation). See [PermanentEntry.enterPermanentOnBattlefield].
     */
    internal fun enterPermanentOnBattlefield(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): Pair<GameState, List<GameEvent>> =
        permanentEntry.enterPermanentOnBattlefield(state, spellId, spellComponent, cardComponent, cardDef)

    /**
     * Apply the resolving permanent's "enters with …" replacement effects (CR 614.1c). See
     * [PermanentEntry.applyEntersWithReplacements].
     */
    internal fun applyEntersWithReplacements(
        state: GameState,
        entityId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        xValue: Int? = null,
        totalManaSpent: Int = 0
    ): Pair<GameState, List<GameEvent>> =
        permanentEntry.applyEntersWithReplacements(state, entityId, cardDef, controllerId, xValue, totalManaSpent)

    /**
     * Create the appropriate decision and continuation for an EntersWithChoice replacement effect.
     * Returns null if the choice cannot be presented. See [EntersWithChoicePrompt.pauseForEntersWithChoice].
     */
    internal fun pauseForEntersWithChoice(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        syntheticRiot: Boolean = false,
        syntheticRiotRemaining: Int = 0
    ): ExecutionResult? =
        entersWithChoicePrompt.pauseForEntersWithChoice(
            state, spellId, controllerId, ownerId, cardComponent, choice, syntheticRiot, syntheticRiotRemaining
        )

    // =========================================================================
    // Countering
    // =========================================================================

    /** Counter whatever stack object [entityId] is, spell or ability. See [SpellCounterer.counterSpellOrAbility]. */
    fun counterSpellOrAbility(state: GameState, entityId: EntityId, countererId: EntityId? = null): ExecutionResult =
        spellCounterer.counterSpellOrAbility(state, entityId, countererId)

    /** Counter a spell on the stack. See [SpellCounterer.counterSpell]. */
    fun counterSpell(state: GameState, spellId: EntityId, countererId: EntityId? = null): ExecutionResult =
        spellCounterer.counterSpell(state, spellId, countererId)

    /** Counter a spell into its owner's hand (Remand). See [SpellCounterer.counterSpellToHand]. */
    fun counterSpellToHand(state: GameState, spellId: EntityId, countererId: EntityId? = null): ExecutionResult =
        spellCounterer.counterSpellToHand(state, spellId, countererId)

    /** Counter a spell into exile. See [SpellCounterer.counterSpellToExile]. */
    fun counterSpellToExile(
        state: GameState,
        spellId: EntityId,
        grantFreeCast: Boolean,
        controllerId: EntityId
    ): ExecutionResult =
        spellCounterer.counterSpellToExile(state, spellId, grantFreeCast, controllerId)

    /** Exile a spell on the stack — not a counter. See [SpellCounterer.exileSpell]. */
    fun exileSpell(
        state: GameState,
        spellId: EntityId,
        makePlotted: Boolean,
        fixedAlternativeManaCost: com.wingedsheep.sdk.core.ManaCost? = null,
        linkToSourceId: EntityId? = null
    ): ExecutionResult =
        spellCounterer.exileSpell(state, spellId, makePlotted, fixedAlternativeManaCost, linkToSourceId)

    /** Counter an activated or triggered ability on the stack. See [SpellCounterer.counterAbility]. */
    fun counterAbility(state: GameState, abilityId: EntityId): ExecutionResult =
        spellCounterer.counterAbility(state, abilityId)

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Determine which zone a card is being cast from. See [SpellCaster.findCastFromZone]. */
    internal fun findCastFromZone(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId
    ): Zone? = spellCaster.findCastFromZone(state, cardId, playerId)

    /**
     * Which face-down mechanic lets [cardDef] be cast face down for {3} — morph (CR 702.37a) or
     * disguise (CR 702.168a) — or null when it can't be cast face down at all. Delegates to
     * [com.wingedsheep.engine.handlers.effects.FaceDownTurnUp.castMode], which owns the
     * keyword-to-mode mapping.
     */
    fun faceDownCastMode(cardDef: com.wingedsheep.sdk.model.CardDefinition?): FaceDownMode? =
        spellCaster.faceDownCastMode(cardDef)
}

/**
 * Build pipeline `storedCollections` for cost-chosen card IDs.
 *
 * The chosen IDs (from [AdditionalCost.Behold] — on its own or as an [AdditionalCost.OrPay] leg —
 * or [AdditionalCost.ChooseEntity]) are stored on the stack object as
 * [SpellOnStackComponent.beheldCards]. Each of those costs declares its own
 * `storeAs` key that the card's resolution-time effects reference (e.g. via
 * `EffectTarget.PipelineTarget`). To keep the effect's reference
 * stable across cost variants, expose the IDs under every relevant `storeAs`
 * key plus a default `"beheld"` key for backward compatibility with
 * pre-existing Behold-using cards.
 *
 * Top-level so non-stack consumers (e.g. the client-side preview text builder
 * in `ClientStateTransformer`) can populate the same pipeline view of the
 * spell's cost-chosen state without re-implementing the lookup.
 */
internal fun buildBeheldStoredCollections(
    beheldCards: List<EntityId>,
    cardDef: com.wingedsheep.sdk.model.CardDefinition?
): Map<String, List<EntityId>> {
    if (beheldCards.isEmpty()) return emptyMap()
    val keys = mutableSetOf("beheld")
    fun collect(cost: AdditionalCost) {
        when (cost) {
            is AdditionalCost.Behold -> keys += cost.storeAs
            is AdditionalCost.ChooseEntity -> keys += cost.storeAs
            is AdditionalCost.Composite -> cost.steps.forEach(::collect)
            is AdditionalCost.OrPay -> collect(cost.cost)
            else -> {}
        }
    }
    cardDef?.script?.additionalCosts?.forEach(::collect)
    return keys.associateWith { beheldCards }
}
