package com.wingedsheep.engine.handlers.actions.spell
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.giftKeyword

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CastWithCreatureTypeContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.AdditionalCostSelectionKind
import com.wingedsheep.engine.core.CastSpellAdditionalCostContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.mechanics.DisturbCasts
import com.wingedsheep.engine.mechanics.EmergeCasts
import com.wingedsheep.engine.mechanics.SpliceCasts
import com.wingedsheep.engine.mechanics.EscalateCosts
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.ModalChooseCounts
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.MayhemGrants
import com.wingedsheep.engine.mechanics.SneakWindow
import com.wingedsheep.engine.mechanics.WebSlinging
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.MiracleGrants
import com.wingedsheep.engine.mechanics.mana.paymentSubtypesOf
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.bend.BendEvents
import com.wingedsheep.engine.handlers.effects.life.LifePaymentService
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.TapForGeneric
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.permissions.activeMayPlayFor
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.PlayerCantPlayFromHandComponent
import com.wingedsheep.engine.state.components.player.CantCastFromNonHandZonesComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
import com.wingedsheep.engine.legality.LegalityKernel
import com.wingedsheep.engine.mechanics.mana.AdditionalManaForCounters
import com.wingedsheep.engine.state.components.stack.AdditionalEntryCounters
import com.wingedsheep.engine.mechanics.cost.spell.SpellCostCheck
import com.wingedsheep.engine.mechanics.cost.spell.SpellCostLedger
import com.wingedsheep.engine.mechanics.cost.spell.SpellCosts
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern as SdkGameEvent
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.core.Keyword

import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import kotlin.reflect.KClass
import com.wingedsheep.engine.core.Outcome

/**
 * Handler for the CastSpell action.
 *
 * Orchestrates spell casting by delegating to focused components:
 * - [CastZoneResolver]: Determines where a card can be cast from
 * - [CastPaymentProcessor]: Handles mana payment via three strategies
 *
 * This handler owns the top-level validate/execute flow, cast restrictions,
 * additional cost processing, and trigger detection.
 */
class CastSpellHandler(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val turnManager: TurnManager,
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val costHandler: CostHandler,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
    private val legality: LegalityKernel,
    private val targetFinder: TargetFinder
) : ActionHandler<CastSpell> {
    override val actionType: KClass<CastSpell> = CastSpell::class
    private val predicateEvaluator = conditionEvaluator.predicates

    private val zoneResolver = CastZoneResolver(cardRegistry, conditionEvaluator, legality)
    private val castPermissionUtils = com.wingedsheep.engine.legalactions.utils.CastPermissionUtils(
        cardRegistry, predicateEvaluator, conditionEvaluator
    )
    private val castCostTotaller = CastCostTotaller(
        cardRegistry, costCalculator, alternativePaymentHandler, zoneResolver, predicateEvaluator
    )
    private val paymentProcessor = CastPaymentProcessor(zones, manaSolver, costHandler, manaAbilitySideEffectExecutor)
    private val castCostPayer = CastCostPayer(
        zones,
        cardRegistry, costHandler, costCalculator, manaSolver, alternativePaymentHandler, paymentProcessor,
        castCostTotaller, zoneResolver, castPermissionUtils, conditionEvaluator, predicateEvaluator,
    )
    private val castRecords = CastRecords(
        cardRegistry, zoneResolver, costCalculator, castCostTotaller, stackResolver, predicateEvaluator
    )
    private val grantedKeywordResolver = com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver(cardRegistry)
    private val castTriggers = CastTriggers(predicateEvaluator, grantedKeywordResolver, stackResolver)
    private val castValidator = CastValidator(
        cardRegistry, turnManager, costCalculator, alternativePaymentHandler, costHandler, targetValidator,
        conditionEvaluator, zoneResolver, castPermissionUtils, castCostTotaller, castCostPayer,
        grantedKeywordResolver, predicateEvaluator, legality,
    )
    private val costEnumerationUtils = com.wingedsheep.engine.legalactions.utils.CostEnumerationUtils(
        manaSolver, costCalculator, predicateEvaluator, cardRegistry
    )

    override fun validate(state: GameState, action: CastSpell): String? = castValidator.validate(state, action)

    /** Each cost the caster owes, reduced to the leg they took (see [SpellCosts.reduceAlternatives]). */
    private fun reduceCostAlternatives(
        costs: List<AdditionalCost>,
        state: GameState,
        playerId: EntityId,
        payment: AdditionalCostPayment?,
    ): List<AdditionalCost> = SpellCosts.reduceAlternatives(costs, state, playerId, payment, costHandler)

    /**
     * Casts the spell, one stage of the casting procedure (CR 601.2) after another:
     *  1. announce it — which face, which modes, which targets for each mode (601.2a–c);
     *  2. determine the total cost (601.2f) and the additional costs it owes;
     *  3. pay (601.2g–h);
     *  4. record the cast (601.2i) and put it on the stack;
     *  5. the abilities and riders the cast itself sets off.
     * Any stage can pause for a player's choice; a pause before payment leaves no side effects, so
     * the re-entry with the answer merged into the action is safe.
     */
    override fun execute(state: GameState, action: CastSpell): ExecutionResult {
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Card not found")
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // --- 1. Announce (CR 601.2a–c) -----------------------------------------------------------

        // Modal DFC back face (CR 712.11b) — resolved pre-cast, while the card is still in hand. Kept
        // apart from `transformedFace` because the cast's mana value reads *this* face's printed cost,
        // which the merged face alone can't distinguish from the other routes.
        val modalBackFace = if (action.useAlternativeCost && action.altAllows(AlternativeCostType.MODAL_BACK_FACE)) {
            zoneResolver.modalBackCastFace(state, action.playerId, action.cardId)
        } else null
        val transformedFace = transformedCastFace(state, action, modalBackFace)

        // Rule 400.7: a card that changed zones is a new object. Drop any stale LinkedExileComponent
        // carried over from a previous battlefield visit (e.g. Veteran Survivor bounced to hand, then
        // recast) before additional costs run — a behold-and-exile cost on this same cast will attach
        // a fresh one afterwards.
        val announcedState = state.updateEntity(action.cardId) { c -> c.without<LinkedExileComponent>() }
        pauseForUnannouncedModesOrTargets(announcedState, action, cardDef, cardComponent)?.let { return it }
        val authorization = castRecords.captureAuthorization(announcedState, action, cardComponent)

        // --- 2. Determine the total cost (CR 601.2f) ---------------------------------------------

        // Free if PlayWithoutPayingCostComponent is present, or if a MayCastWithoutPayingManaCost
        // battlefield source (e.g. Weftwalking) is the chosen alt; mutual exclusion and the gate were
        // already enforced in validate(). The card may have left the command zone in `state` since
        // validate(), but `announcedState` still has it there because `castSpell` hasn't run.
        val playForFree = zoneResolver.hasPlayWithoutPayingCost(announcedState, action.playerId, action.cardId) ||
            action.useWithoutPayingManaCost
        val totalCost = castCostTotaller.totalCost(
            announcedState, action, cardDef, cardComponent, playForFree,
            castingFromCommandZone = zoneResolver.hasCommanderCastPermission(announcedState, action.playerId, action.cardId),
        )
            // validate() rejects a cast with no available base, so this is only reached by a
            // server-initiated cast that skipped it; pay the printed cost rather than nothing.
            ?: cardComponent.manaCost

        val owedCosts = reduceCostAlternatives(
            castCostPayer.owedAdditionalCosts(announcedState, action, cardDef), announcedState, action.playerId, action.additionalCostPayment
        )
        // The declared optional cost, put through the *same* reduction as the full list, so payment
        // can recognise it by equality. Reducing both sides is what makes the match survive an
        // `AdditionalCost.Composite` or `Choice` wrapper: the reduction flattens composites and picks
        // a Choice's leg, so an unreduced wrapper would never match and would silently drop the tap
        // cause.
        val declaredSlotCosts = reduceCostAlternatives(
            listOfNotNull(castCostPayer.declaredSlotCost(action, cardDef)), announcedState, action.playerId, action.additionalCostPayment
        )

        // Server-initiated free cast: pay the spell's printed additional costs even though the mana
        // cost is waived (CR 601.2f / 118.9). A normal client cast arrives with the selections already
        // in `additionalCostPayment` (validated in validate()); copy-and-cast pipelines (Roving
        // Actuator, Shiko, Cascade) call execute() directly with no payment, so the selection is
        // surfaced here. The pause sits before any cost is paid, so the re-entry on resume (with the
        // chosen entities merged into the payment) is side-effect free.
        surfaceUnpaidAdditionalCostSelection(announcedState, action, owedCosts)?.let { return it }

        // "You may pay any amount of mana" as an additional cost (Chorus of the Conclave): the {N}
        // is part of the total cost above. The grant is read *now*, before any cost is paid: the
        // payment was announced while the source was on the battlefield (CR 601.2b), so sacrificing
        // that source to another cost of this same spell doesn't take the counters back.
        val additionalEntryCounters = if (action.additionalManaForCounters > 0) {
            AdditionalManaForCounters.applicableGrant(announcedState, action.playerId, action.cardId, cardRegistry, predicateEvaluator = predicateEvaluator)
                ?.let { AdditionalEntryCounters(it.counterType, action.additionalManaForCounters) }
        } else null

        // --- 3. Pay (CR 601.2g–h) ----------------------------------------------------------------

        val ledger = SpellCostLedger(
            state = announcedState,
            action = action,
            castCardName = cardComponent.name,
            cardDefinitionName = cardDef?.name,
            cardRegistry = cardRegistry,
            zones = zones,
            declaredSlotCosts = declaredSlotCosts,
        )
        val paid = when (val outcome = castCostPayer.pay(ledger, cardComponent, cardDef, totalCost, owedCosts, playForFree)) {
            is CastPaymentOutcome.Failed -> return ExecutionResult.error(ledger.state, outcome.reason)
            is CastPaymentOutcome.Paid -> outcome.payment
        }

        val targeting = spellTargeting(state, action, cardDef, transformedFace)

        // A creature type chosen as the spell is cast (e.g., Aphetto Dredging).
        cardDef?.script?.castTimeCreatureTypeChoice?.let { castTimeChoice ->
            pauseForCreatureTypeChoice(
                ledger.state, action, castTimeChoice, ledger.sacrificedSnapshots, targeting.requirements, ledger.events
            )?.let { return it }
        }

        val returned = castCostPayer.returnForAlternativeCost(ledger, cardDef)

        // --- 4. Record the cast (CR 601.2i) and put it on the stack ------------------------------

        val (afterMarks, marks) = castRecords.markAlternativeCost(ledger.state, action, cardDef)
        val (afterRecord, stormCount) = castRecords.recordSpellCast(
            afterMarks, action, cardDef, cardComponent, transformedFace, modalBackFace, marks.wasWarped, paid.payment
        )
        var currentState = afterRecord
        val castingFromGraveyardViaMuldrotha = castRecords.isCastViaMuldrotha(currentState, action, cardComponent)

        // Splice (CR 702.47a): reveal each spliced card from hand. The reveal is public — it is how
        // opponents learn what text the spell gained — but the caster picked the cards, so it doesn't
        // get an overlay of its own choice. The cards are *not* moved: they stay in hand, castable
        // later or splice-able onto a later spell, and can even be discarded to pay a discard cost of
        // the very spell they were spliced onto.
        val splicedCardNames = action.splicedCardIds.mapNotNull { splicedId ->
            currentState.getEntity(splicedId)?.get<CardComponent>()?.name
        }
        if (splicedCardNames.isNotEmpty()) {
            ledger.events.add(
                CardsRevealedEvent(
                    revealingPlayerId = action.playerId,
                    cardIds = action.splicedCardIds,
                    cardNames = splicedCardNames,
                    source = "Splice",
                    revealToSelf = false,
                    fromZone = Zone.HAND,
                    toZone = Zone.HAND
                )
            )
        }

        val castResult = putSpellOnStack(
            currentState, state, action, cardDef, transformedFace, ledger, paid, targeting, returned, marks, splicedCardNames,
            additionalEntryCounters,
        )
        if (castResult.outcome !is Outcome.Done) {
            return castResult
        }
        currentState = castResult.newState

        // --- 5. What the cast sets off -----------------------------------------------------------

        currentState = castRecords.applyCastThisWayRiders(currentState, action, cardComponent, authorization, paid.isForageCast)
        val spell = CastSpellOnStack(action, cardDef, cardComponent, targeting.requirements)
        val (afterManaRiders, riderTriggers) = castTriggers.applyManaRiders(currentState, spell, paid.payment.consumedRiders)
        currentState = castRecords.consumeCastPermissions(
            afterManaRiders, state, action, cardDef, cardComponent, authorization, castingFromGraveyardViaMuldrotha
        )
        val copyTriggers = castTriggers.copyTriggers(currentState, spell, stormCount)
        val afterRiders = castTriggers.consumeNextSpellRiders(currentState, spell, ledger.events + castResult.events)
        if (afterRiders.outcome !is Outcome.Done) return afterRiders
        currentState = afterRiders.newState

        // Storm, conspire, casualty and rider triggers are known here rather than detected from an
        // event. They join the waiting queue ahead of the triggers the settle boundary detects from
        // the cast events (including additional-cost events like a sacrifice). Within a player's own
        // triggers that order is kept, so Storm goes on the stack just above the spell that caused it
        // (CR 702.40a), and APNAP order puts non-active players' triggers above.
        val synthesizedTriggers = riderTriggers + copyTriggers
        if (synthesizedTriggers.isNotEmpty()) {
            currentState = currentState.copy(pendingTriggers = currentState.pendingTriggers + synthesizedTriggers)
        }
        return ExecutionResult.success(currentState.withPriority(action.playerId), afterRiders.events)
    }

    /**
     * The face this cast puts on the stack when it is cast **transformed**, resolved against the
     * pre-cast state while the card is still in its origin zone. Non-null means the back face
     * supplies the spell's characteristics (CR 712.8c / 712.8f). Three routes, mirroring validate():
     * disturb (CR 702.146a) casts transformed from the graveyard for its disturb cost; the modal-DFC
     * face choice (CR 712.11b) casts the back face from hand for its own mana cost; and a
     * `castTransformed` may-play permission casts transformed from wherever the permission covers
     * (CR 310.12b — "exile it, then you may cast it transformed").
     */
    private fun transformedCastFace(state: GameState, action: CastSpell, modalBackFace: com.wingedsheep.sdk.model.CardDefinition?): com.wingedsheep.sdk.model.CardDefinition? =
        (if (action.useAlternativeCost && action.altAllows(AlternativeCostType.DISTURB)) {
            zoneResolver.disturbCastFace(state, action.playerId, action.cardId)
        } else null)
            ?: modalBackFace
            ?: zoneResolver.permissionTransformedCastFace(state, action.playerId, action.cardId)

    /**
     * The mode and target announcements (CR 601.2b–c) the action arrived without, asked for before
     * anything is paid so cancelling leaves no side effects.
     *
     * - Modes: applies uniformly to choose-1 and choose-N modal spells. The web client supplies
     *   `chosenModes` up front for choose-1 spells (the local mode picker), so it bypasses this pause;
     *   synthesized free casts (Sunbird's Invocation, Cascade) and any other server-initiated cast
     *   that doesn't pre-supply a mode hits it. The resolution-time mode picker in
     *   [com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor] remains for modal
     *   *triggered* / *activated* abilities (CR 603.3c), which don't go through the cast pipeline.
     * - Per-mode targets: a modal cast whose modes were chosen up front but whose targets were
     *   deferred to the engine — the single-panel client mode selector submits `chosenModes` only and
     *   lets the server drive on-battlefield targeting. This runs the same per-mode target flow the
     *   sequential mode-selection pause transitions into, then re-enters execute() with a
     *   fully-populated action so cost payment and stack placement happen exactly once. The choose-1
     *   client path and AI supply flat `targets`, so they skip this and fall through to
     *   deriveModeTargetsFromFlat.
     */
    private fun pauseForUnannouncedModesOrTargets(
        state: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        cardComponent: CardComponent,
    ): ExecutionResult? {
        val modalEffect = cardDef?.script?.spellEffect as? ModalEffect ?: return null
        if (action.chosenModes.isEmpty() && modalEffect.chooseCount >= 1) {
            return pauseForCastTimeModeSelection(state, action, cardComponent, modalEffect)
        }
        if (action.chosenModes.isNotEmpty() &&
            action.modeTargetsOrdered.isEmpty() &&
            action.targets.isEmpty() &&
            action.chosenModes.any { modalEffect.modes.getOrNull(it)?.targetRequirements?.isNotEmpty() == true }
        ) {
            return presentCastModalTargetDecision(
                state = state,
                cardId = action.cardId,
                casterId = action.playerId,
                cardName = cardComponent.name,
                baseCastAction = action,
                modes = modalEffect.modes,
                chosenModeIndices = action.chosenModes,
                resolvedModeTargets = emptyList(),
                currentOrdinal = 0
            )
        }
        return null
    }

    /** What the spell targets, for resolution-time re-validation (CR 608.2b). */
    private class SpellTargeting(
        /** The spell's own requirements, then the aura target, then each spliced card's. */
        val requirements: List<TargetRequirement>,
        /** For a modal spell with modes chosen at cast time, each chosen mode's own requirements. */
        val perMode: Map<Int, List<TargetRequirement>>,
        val modalEffect: ModalEffect?,
    )

    private fun spellTargeting(
        state: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        transformedFace: com.wingedsheep.sdk.model.CardDefinition?,
    ): SpellTargeting {
        val modalEffect = cardDef?.script?.spellEffect as? ModalEffect
        val perMode: Map<Int, List<TargetRequirement>> =
            if (modalEffect != null && action.chosenModes.isNotEmpty()) {
                action.chosenModes.distinct().associateWith { idx ->
                    modalEffect.modes.getOrNull(idx)?.targetRequirements ?: emptyList()
                }
            } else emptyMap()

        if (cardDef == null) return SpellTargeting(emptyList(), perMode, modalEffect)
        // Adventure / split face cast (CR 715 / 709) — read targets from the face's script; a
        // disturb cast reads the back face's (CR 712.8c). Mirrors validate().
        val faceScriptForTargets = action.faceIndex?.let { cardDef.cardFaces.getOrNull(it)?.script }
            ?: transformedFace?.script
        val baseTargetReqs = if (action.chosenModes.isNotEmpty() && modalEffect != null) {
            // Modal spell with modes chosen at cast time — the union of the per-mode requirements,
            // so resolution can re-check every targeted slot.
            action.chosenModes.flatMap { idx -> modalEffect.modes.getOrNull(idx)?.targetRequirements ?: emptyList() }
        } else if (action.declaredCostSlot != null && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
            cardDef.script.kickerTargetRequirements
        } else if (isCleaveCast(action, cardDef) && cardDef.script.cleaveTargetRequirements.isNotEmpty()) {
            cardDef.script.cleaveTargetRequirements
        } else {
            (faceScriptForTargets ?: cardDef.script).targetRequirements
        }
        val requirements = buildList {
            addAll(baseTargetReqs)
            (transformedFace ?: cardDef).script.auraTarget?.let { add(it) }
            // Splice (CR 702.47d): the spliced text's own requirements, appended in splice order.
            // They must be here and not only in validate(): this list becomes the spell's
            // TargetsComponent, which drives resolution-time 608.2b re-validation and the tail that
            // StackResolver slices off to hand each spliced card its own targets.
            addAll(SpliceCasts.targetRequirementsFor(state, action.splicedCardIds, cardRegistry))
        }
        return SpellTargeting(requirements, perMode, modalEffect)
    }

    /** Puts the paid-for spell on the stack with everything its resolution will read. */
    private fun putSpellOnStack(
        state: GameState,
        originState: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        transformedFace: com.wingedsheep.sdk.model.CardDefinition?,
        ledger: SpellCostLedger,
        paid: CastPayment,
        targeting: SpellTargeting,
        returned: ReturnedForAlternativeCost,
        marks: AlternativeCostMarks,
        splicedCardNames: List<String>,
        additionalEntryCounters: AdditionalEntryCounters?,
    ): ExecutionResult {
        // Derive per-mode target groups from the flat target list when the action arrived with
        // chosenModes but no modeTargetsOrdered (current web-client cast-time UI for choose-1 modal
        // spells). Slice action.targets in mode order using each mode's total target slot count so
        // modal resolution can read per-mode targets.
        val modalEffect = targeting.modalEffect
        val effectiveModeTargetsOrdered = if (
            action.modeTargetsOrdered.isEmpty() && action.chosenModes.isNotEmpty() &&
            modalEffect != null && action.targets.isNotEmpty()
        ) {
            deriveModeTargetsFromFlat(modalEffect, action.chosenModes, action.targets)
        } else {
            action.modeTargetsOrdered
        }

        // Pay-X-life additional cost (AdditionalCost.PayXLife): record the declared X (non-null,
        // including 0) only when the spell actually carries this cost, so it's coalesced into the
        // resolution X value. Other spells leave this null and keep xValue purely from {X}.
        val castTimeScript = action.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script } ?: cardDef?.script
        val payXLifeAmount: Int? =
            if (castTimeScript?.additionalCosts?.any { it is AdditionalCost.PayXLife } == true) {
                action.additionalCostPayment?.payXLifeAmount ?: 0
            } else null

        val manaSpentEvent = paid.manaSpentEvent
        return stackResolver.castSpell(
            state,
            action.cardId,
            action.playerId,
            action.targets,
            action.xValue,
            ledger.sacrificedSnapshots,
            castFaceDown = action.castFaceDown,
            castTransformed = transformedFace != null,
            damageDistribution = action.damageDistribution,
            targetRequirements = targeting.requirements,
            exiledCardCount = ledger.exiledCardCount,
            additionalCostBlightAmount = action.additionalCostPayment?.blightAmount ?: 0,
            additionalCostPayXLifeAmount = payXLifeAmount,
            declaredCostSlot = action.declaredCostSlot,
            wasBlightPaid = (action.additionalCostPayment?.blightTargets?.isNotEmpty() == true),
            // True when the spell's waterbend additional cost was paid (Avatar) — mandatory costs
            // always, optional "you may waterbend {N}" only when the player elected it.
            wasWaterbendPaid = cardDef?.script?.spellWaterbend?.let { !it.optional || action.wasWaterbendPaid } == true,
            additionalEntryCounters = additionalEntryCounters,
            // Gift (CR 702.174a): the promised opponent, elected as part of casting. Only honored
            // for a card that actually has gift — validate() rejects the flag otherwise.
            giftRecipient = action.giftRecipient?.takeIf { cardDef?.giftKeyword() != null },
            wasWarped = marks.wasWarped,
            wasDashed = marks.wasDashed,
            wasEvoked = marks.wasEvoked,
            wasImpending = marks.wasImpending,
            wasCleaved = marks.wasCleaved,
            wasSneaked = returned.wasSneaked,
            sneakAttackDefenderId = returned.sneakAttackDefenderId,
            wasWebSlung = returned.wasWebSlung,
            webSlungReturnedManaValue = returned.webSlungReturnedManaValue,
            wasMayhem = marks.wasMayhem,
            chosenModes = action.chosenModes,
            modeTargetsOrdered = effectiveModeTargetsOrdered,
            modeTargetRequirements = targeting.perMode,
            modeDamageDistribution = action.modeDamageDistribution,
            // Splice (CR 702.47a): the *text* the spell gained, recorded by card name. The cards
            // themselves stay in hand — nothing about splicing moves them.
            splicedCardNames = splicedCardNames,
            totalManaSpent = paid.manaSpent,
            beheldCards = ledger.beheldCards,
            discardedAsCostCards = ledger.discardedAsCostCards,
            exiledAsCostCards = ledger.exiledAsCostCards,
            exiledAsCostSnapshots = ledger.exiledAsCostSnapshots,
            chosenEntitySnapshots = ledger.chosenEntitySnapshots,
            manaSpentWhite = manaSpentEvent?.white ?: 0,
            manaSpentBlue = manaSpentEvent?.blue ?: 0,
            manaSpentBlack = manaSpentEvent?.black ?: 0,
            manaSpentRed = manaSpentEvent?.red ?: 0,
            manaSpentGreen = manaSpentEvent?.green ?: 0,
            manaSpentColorless = manaSpentEvent?.colorless ?: 0,
            manaSpentOnXByColor = paid.payment.xManaSpentByColor,
            faceIndex = action.faceIndex,
            spentManaProvenance = paid.payment.spentManaProvenance,
            castTimeFlags = castTimeFlags(state, action, castTimeScript),
            // Every enumerated alternative-cost offer names its mechanic explicitly, so this is the
            // declared choice rather than a guess. Descriptive only — the rules consequences of each
            // mechanic ride the `was*` flags above.
            alternativeCost = action.alternativeCostType?.takeIf { action.useAlternativeCost },
            castOriginState = originState
        )
    }

    /**
     * Evaluates the "as you cast this spell" condition captures (CR 601.2i). The spell has finished
     * being cast (costs paid) but isn't on the stack yet; freezing the answers now lets the resolving
     * effect read the cast-time board even if it has since changed (Steer Clear's "if you controlled
     * a Mount as you cast this spell"). The caster is the controller; the captured names are carried
     * onto SpellOnStackComponent.castTimeFlags.
     */
    private fun castTimeFlags(state: GameState, action: CastSpell, castTimeScript: com.wingedsheep.sdk.model.CardScript?): Set<String> {
        val captures = castTimeScript?.castTimeCaptures.orEmpty()
        if (captures.isEmpty()) return emptySet()
        val captureContext = EffectContext(sourceId = action.cardId, controllerId = action.playerId, targets = emptyList(), xValue = 0)
        return captures.filter { conditionEvaluator.evaluate(state, it.condition, captureContext) }.map { it.flag }.toSet()
    }

    /**
     * Check if the spell needs a creature type choice during casting (e.g., Aphetto Dredging).
     * If so, scan the appropriate zone for creature types and pause for the choice.
     * Returns null if no pause is needed (e.g., no creature types found).
     */
    private fun pauseForCreatureTypeChoice(
        currentState: GameState,
        action: CastSpell,
        source: com.wingedsheep.sdk.model.CastTimeCreatureTypeSource,
        sacrificedSnapshots: List<EntitySnapshot>,
        spellTargetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        priorEvents: List<GameEvent>
    ): ExecutionResult? {
        // Determine which zone to scan based on source
        val zone = when (source) {
            com.wingedsheep.sdk.model.CastTimeCreatureTypeSource.GRAVEYARD ->
                ZoneKey(action.playerId, Zone.GRAVEYARD)
        }
        val zoneCards = currentState.getZone(zone)

        // Collect creature subtypes and which cards have each type
        val typeToCardIds = mutableMapOf<String, MutableList<EntityId>>()
        for (cardId in zoneCards) {
            val cc = currentState.getEntity(cardId)?.get<CardComponent>() ?: continue
            val typeLine = cc.typeLine
            if (typeLine.isCreature) {
                for (subtype in typeLine.subtypes) {
                    typeToCardIds.getOrPut(subtype.value) { mutableListOf() }.add(cardId)
                }
            }
        }

        // If no creature types found, skip the decision — casting proceeds normally
        if (typeToCardIds.isEmpty()) return null

        val sortedTypes = typeToCardIds.keys.sorted()
        val cardComponent = currentState.getEntity(action.cardId)?.get<CardComponent>()
        val sourceName = cardComponent?.name

        // Build option index → card IDs mapping for client preview
        val optionCardIds = sortedTypes.mapIndexed { index, type ->
            index to typeToCardIds[type]!!.toList()
        }.toMap()

        val continuation = CastWithCreatureTypeContinuation(
            cardId = action.cardId,
            casterId = action.playerId,
            targets = action.targets,
            xValue = action.xValue,
            sacrificedPermanents = sacrificedSnapshots,
            targetRequirements = spellTargetRequirements,
            count = 0,
            creatureTypes = sortedTypes
        )
        return currentState.withPriority(action.playerId).suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = "Choose a creature type",
                    context = DecisionContext(
                        sourceId = action.cardId,
                        sourceName = sourceName,
                        phase = DecisionPhase.CASTING
                    ),
                    options = sortedTypes,
                    optionCardIds = optionCardIds
                )
            },
            answer = continuation,
            events = priorEvents
        )
    }

    /**
     * Initial entry point for choose-N modal cast-time mode selection (rule 700.2).
     *
     * Pre-filters modes by 700.2a target legality, then pauses with a ChooseOption
     * decision in the CASTING phase. The resumer iterates until `chooseCount` modes
     * are picked (or "Done" fires once `minChooseCount` is satisfied), then
     * transitions to per-mode target selection or directly back into [execute] with
     * a fully populated action.
     */
    private fun pauseForCastTimeModeSelection(
        currentState: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        modalEffect: ModalEffect
    ): ExecutionResult {
        // Apply chooseAllIfBlightPaid: if the player paid blight, force choosing all
        // modes; otherwise the regular [minChooseCount, chooseCount] range applies.
        val (effectiveMin, effectiveMax) = castValidator.effectiveModalChooseCounts(currentState, modalEffect, action)
        val effectiveModalEffect = if (effectiveMin == modalEffect.minChooseCount &&
            effectiveMax == modalEffect.chooseCount) {
            modalEffect
        } else {
            modalEffect.copy(chooseCount = effectiveMax, minChooseCount = effectiveMin)
        }

        val available = effectiveModalEffect.modes.withIndex()
            .filter { (_, mode) -> modeHasSatisfiableTargets(currentState, action.playerId, action.cardId, mode) }
            .map { it.index }

        if (available.size < effectiveModalEffect.minChooseCount) {
            return ExecutionResult.error(currentState, "No legal mode selection available for ${cardComponent.name}")
        }

        return presentCastModalModeDecision(
            state = currentState,
            cardId = action.cardId,
            casterId = action.playerId,
            cardName = cardComponent.name,
            baseCastAction = action,
            modalEffect = effectiveModalEffect,
            selectedModeIndices = emptyList(),
            availableIndices = if (effectiveModalEffect.allowRepeat) null else available,
            repeatAvailableIndices = if (effectiveModalEffect.allowRepeat) available else null
        )
    }

    /**
     * Check whether a modal mode can potentially be cast — either it has no targets, or
     * at least one legal target exists for each of its [TargetRequirement]s (rule 700.2a).
     */
    private fun modeHasSatisfiableTargets(
        state: GameState,
        casterId: EntityId,
        sourceId: EntityId,
        mode: com.wingedsheep.sdk.scripting.effects.Mode
    ): Boolean {
        if (mode.targetRequirements.isEmpty()) return true
        return mode.targetRequirements.all { req ->
            req.effectiveMinCount == 0 ||
                targetFinder.findLegalTargets(state, req, casterId, sourceId).isNotEmpty()
        }
    }

    /**
     * Mana-affordability gate for cast-time mode selection: can the caster still pay
     * the spell's total cost if [chosenIndices] end up being the chosen modes? Chosen
     * modes' additional mana costs stack (rule 700.2h), so a pick that is affordable
     * alone can become unpayable combined with earlier picks — and by the time payment
     * runs (after target selection) the only way out is cancelling the whole cast.
     *
     * The base cost comes from [computeTotalCastCost] — the same pipeline payment uses —
     * so alternative costs, cost modifiers, and alternative payments (convoke/delve)
     * can't make the gate disagree with payment. A "without paying its mana cost" cast
     * still owes the stacked per-mode additional costs (CR 601.2b, 601.2f — additional
     * costs apply on top of an alternative cost).
     */
    private fun canPayModeSelection(
        state: GameState,
        action: CastSpell,
        modalEffect: ModalEffect,
        chosenIndices: List<Int>
    ): Boolean {
        // Escalate with a non-mana cost (CR 702.120a): each mode beyond the first owes another
        // discard / tap / …, so a pick can run the caster out of cards to pay with just as it can
        // run them out of mana.
        val escalatePayability = EscalateCosts.payability(
            state, action.playerId, action.cardId, modalEffect, costEnumerationUtils, predicateEvaluator
        )
        if (escalatePayability != null &&
            (chosenIndices.size - 1).coerceAtLeast(0) > escalatePayability.maxExtraModes
        ) {
            return false
        }

        val extraCosts = buildList {
            addAll(chosenIndices.mapNotNull { modalEffect.modes.getOrNull(it)?.additionalManaCost })
            modalEffect.additionalManaCostPerExtraMode?.let { perExtraMode ->
                repeat((chosenIndices.size - 1).coerceAtLeast(0)) { add(perExtraMode) }
            }
        }
        // Nothing stacks — base-cost affordability was already validated on the cast action.
        if (extraCosts.isEmpty()) return true
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>() ?: return true
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return true
        val playForFree = zoneResolver.hasPlayWithoutPayingCost(state, action.playerId, action.cardId) ||
            action.useWithoutPayingManaCost
        val computed = castCostTotaller.validationCost(
            state,
            action,
            cardDef,
            cardComponent,
            playForFree,
            castingFromCommandZone = zoneResolver.hasCommanderCastPermission(state, action.playerId, action.cardId)
        ) ?: return false
        var cost = computed.cost
        for (extra in extraCosts) {
            cost = cost + ManaCost.parse(extra)
        }
        return castCostPayer.validateManaPayment(state, action, cost, computed.paymentXValue) == null
    }

    /**
     * Build a ChooseOptionDecision + CastModalModeSelectionContinuation for the next
     * mode pick. Shared between the initial pause (here) and the iterative resumer.
     */
    internal fun presentCastModalModeDecision(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        cardName: String,
        baseCastAction: CastSpell,
        modalEffect: ModalEffect,
        selectedModeIndices: List<Int>,
        availableIndices: List<Int>?,
        repeatAvailableIndices: List<Int>?
    ): ExecutionResult {
        val candidateIndices = availableIndices ?: repeatAvailableIndices ?: modalEffect.modes.indices.toList()
        // Rule 700.2h — only offer a mode the caster can still pay for on top of the
        // modes already picked. Without this gate an unpayable combination sails
        // through mode + target selection and dead-ends at payment, where the pending
        // decision can never be answered legally (only cancelled).
        val offerIndices = candidateIndices.filter { candidate ->
            canPayModeSelection(state, baseCastAction, modalEffect, selectedModeIndices + candidate)
        }
        if (offerIndices.isEmpty() && selectedModeIndices.size < modalEffect.minChooseCount) {
            return ExecutionResult.error(
                state,
                "Cannot afford the additional cost of any remaining mode for $cardName"
            )
        }
        val doneOffered = selectedModeIndices.size >= modalEffect.minChooseCount &&
            selectedModeIndices.size < modalEffect.chooseCount

        val optionLabels = offerIndices.map { modalEffect.modes[it].description } +
            (if (doneOffered) listOf("Done") else emptyList())

        val pickNumber = selectedModeIndices.size + 1
        val alreadyPicked = if (selectedModeIndices.isNotEmpty()) {
            val labels = selectedModeIndices.map { modalEffect.modes[it].description }
            "\nAlready picked: ${labels.joinToString("; ")}"
        } else ""
        val prompt = "Choose a mode for $cardName ($pickNumber of ${modalEffect.chooseCount})$alreadyPicked"
        val continuation = com.wingedsheep.engine.core.CastModalModeSelectionContinuation(
            cardId = cardId,
            casterId = casterId,
            baseCastAction = baseCastAction,
            modes = modalEffect.modes,
            chooseCount = modalEffect.chooseCount,
            minChooseCount = modalEffect.minChooseCount,
            allowRepeat = modalEffect.allowRepeat,
            offeredIndices = offerIndices,
            availableIndices = availableIndices,
            selectedModeIndices = selectedModeIndices,
            doneOptionOffered = doneOffered
        )
        return state.withPriority(casterId).suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = casterId,
                    prompt = prompt,
                    context = DecisionContext(
                        sourceId = cardId,
                        sourceName = cardName,
                        phase = DecisionPhase.CASTING
                    ),
                    options = optionLabels,
                    // Cast-time mode selection must be cancellable (rule 601.2b–c, K1 in plan):
                    // the pause happens before any cost is paid, so aborting is safe.
                    canCancel = true
                )
            },
            answer = continuation
        )
    }

    /**
     * Build a ChooseTargetsDecision + CastModalTargetSelectionContinuation for the next
     * mode that needs targets. Skips modes whose requirements are empty, advancing the
     * ordinal and appending an empty target list until it finds one that needs targets
     * or all modes are resolved.
     */
    /**
     * Surface the first selection-requiring additional cost on a server-initiated free cast that
     * the action hasn't already paid, pausing for the caster's choice. See
     * [CastSpellAdditionalCostContinuation] for the re-entry contract.
     *
     * - Only the *selection* atoms need a player choice (Sacrifice / Discard / ExileFrom /
     *   TapPermanents / ReturnToHand). PayLife / mana / reveal-from-hand are auto-paid downstream
     *   and need no prompt, so they're ignored here.
     * - A cost already satisfied by the action's payment (the normal client-cast path, which is
     *   gated by `validate()`) is skipped — so this never fires for a normal cast.
     * - If a mandatory cost can't be paid at all (fewer legal options than the count required),
     *   the cast can't be completed (CR 601.2h — unpayable costs can't be paid): return an error so
     *   the free-cast caller treats it as a no-op and the card stays where it is.
     *
     * Returns null when nothing needs choosing — the cast proceeds inline.
     */
    private fun surfaceUnpaidAdditionalCostSelection(
        state: GameState,
        action: CastSpell,
        flattenedCosts: List<AdditionalCost>,
    ): ExecutionResult? {
        val payment = action.additionalCostPayment
        for (cost in flattenedCosts) {
            val atom = (cost as? AdditionalCost.Atom)?.atom ?: continue
            val (kind, count, options) = when (atom) {
                is CostAtom.Sacrifice -> Triple(
                    AdditionalCostSelectionKind.SACRIFICE,
                    atom.count,
                    costEnumerationUtils.findSacrificeTargets(state, action.playerId, atom)
                )
                is CostAtom.Discard -> {
                    if (atom.random) continue // random discard needs no selection
                    Triple(
                        AdditionalCostSelectionKind.DISCARD,
                        atom.count,
                        costEnumerationUtils.findDiscardTargets(state, action.playerId, atom.filter)
                            .filter { it != action.cardId }
                    )
                }
                is CostAtom.ExileFrom -> Triple(
                    AdditionalCostSelectionKind.EXILE,
                    atom.count,
                    costEnumerationUtils.findExileTargets(state, action.playerId, atom.filter, atom.zone)
                        .filter { it != action.cardId }
                )
                is CostAtom.TapPermanents -> Triple(
                    AdditionalCostSelectionKind.TAP,
                    atom.count,
                    costEnumerationUtils.findAbilityTapTargets(state, action.playerId, atom.filter)
                        .let { if (atom.excludeSelf) it.filter { id -> id != action.cardId } else it }
                )
                is CostAtom.ReturnToHand -> Triple(
                    AdditionalCostSelectionKind.RETURN_TO_HAND,
                    atom.count,
                    costEnumerationUtils.findAbilityBounceTargets(state, action.playerId, atom.filter, atom.youControl)
                        .filter { id -> id != action.cardId }
                )
                else -> continue
            }
            if (count <= 0) continue

            val alreadyPaid = when (kind) {
                AdditionalCostSelectionKind.SACRIFICE -> payment?.sacrificedPermanents?.size ?: 0
                AdditionalCostSelectionKind.DISCARD -> payment?.discardedCards?.size ?: 0
                AdditionalCostSelectionKind.EXILE -> payment?.exiledCards?.size ?: 0
                AdditionalCostSelectionKind.TAP -> payment?.tappedPermanents?.size ?: 0
                AdditionalCostSelectionKind.RETURN_TO_HAND -> payment?.bouncedPermanents?.size ?: 0
            }
            if (alreadyPaid >= count) continue // supplied by the caller (normal cast) — nothing to choose

            if (options.size < count) {
                // CR 601.2h — "Unpayable costs can't be paid": the additional cost can't be met,
                // so the cast can't be completed.
                return ExecutionResult.error(state, "Cannot pay additional cost: not enough valid choices")
            }

            // No real choice (exactly enough legal options) — auto-pay and re-enter, so a forced
            // single sacrifice doesn't prompt. The re-entry sees this cost satisfied and moves on.
            if (options.size == count) {
                return execute(state, withAdditionalCostSelection(action, kind, options))
            }

            val cardName = state.getEntity(action.cardId)?.get<CardComponent>()?.name ?: "spell"
            val verb = when (kind) {
                AdditionalCostSelectionKind.SACRIFICE -> "sacrifice"
                AdditionalCostSelectionKind.DISCARD -> "discard"
                AdditionalCostSelectionKind.EXILE -> "exile"
                AdditionalCostSelectionKind.TAP -> "tap"
                AdditionalCostSelectionKind.RETURN_TO_HAND -> "return to hand"
            }
            val prompt = "Choose $count ${if (count > 1) "cards" else "card"} to $verb for $cardName"
            // Permanents you control are chosen on the battlefield; hidden/zone cards via overlay.
            val useTargetingUI = kind == AdditionalCostSelectionKind.SACRIFICE ||
                kind == AdditionalCostSelectionKind.TAP ||
                kind == AdditionalCostSelectionKind.RETURN_TO_HAND
            val continuation = CastSpellAdditionalCostContinuation(
                cardId = action.cardId,
                casterId = action.playerId,
                baseCastAction = action,
                costKind = kind,
            )
            return state.withPriority(action.playerId).suspendForDecision(
                question = { decisionId ->
                    SelectCardsDecision(
                        id = decisionId,
                        playerId = action.playerId,
                        prompt = prompt,
                        context = DecisionContext(
                            sourceId = action.cardId,
                            sourceName = cardName,
                            phase = DecisionPhase.CASTING,
                        ),
                        options = options,
                        minSelections = count,
                        maxSelections = count,
                        useTargetingUI = useTargetingUI,
                    )
                },
                answer = continuation
            )
        }
        return null
    }

    /**
     * Merge a chosen additional-cost payment into [base]'s [AdditionalCostPayment] for the given
     * [kind], appending to whatever was already paid. Used by the free-cast additional-cost
     * resumer to re-enter [execute] with the selection recorded.
     */
    internal fun withAdditionalCostSelection(
        base: CastSpell,
        kind: AdditionalCostSelectionKind,
        chosen: List<EntityId>,
    ): CastSpell {
        val payment = base.additionalCostPayment ?: AdditionalCostPayment()
        val merged = when (kind) {
            AdditionalCostSelectionKind.SACRIFICE ->
                payment.copy(sacrificedPermanents = payment.sacrificedPermanents + chosen)
            AdditionalCostSelectionKind.DISCARD ->
                payment.copy(discardedCards = payment.discardedCards + chosen)
            AdditionalCostSelectionKind.EXILE ->
                payment.copy(exiledCards = payment.exiledCards + chosen)
            AdditionalCostSelectionKind.TAP ->
                payment.copy(tappedPermanents = payment.tappedPermanents + chosen)
            AdditionalCostSelectionKind.RETURN_TO_HAND ->
                payment.copy(bouncedPermanents = payment.bouncedPermanents + chosen)
        }
        return base.copy(additionalCostPayment = merged)
    }

    /**
     * How many targets a mode's requirement may take, for the cast-time per-mode decision.
     *
     * A [TargetObject.dynamicMaxCount] is authoritative when present — the static `count` is only
     * the placeholder the author writes when the real cap isn't knowable until cast time. Mirrors
     * [TargetValidator]'s `effectiveMaxCount`, so the count the player is *offered* and the count
     * the cast is *validated* against are the same number; otherwise "up to X target creatures"
     * offers one target at X = 3, or offers unbounded picks the validator then rejects.
     */
    private fun resolveModeTargetMaxCount(
        state: GameState,
        requirement: TargetRequirement,
        casterId: EntityId,
        cardId: EntityId,
        xValue: Int?
    ): Int {
        val unboundedFallback = if (requirement.unlimited) Int.MAX_VALUE else requirement.count
        if (requirement !is com.wingedsheep.sdk.scripting.targets.TargetObject) return unboundedFallback
        val dyn = requirement.dynamicMaxCount ?: return unboundedFallback
        if (dyn == com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue) {
            return xValue ?: unboundedFallback
        }
        return try {
            conditionEvaluator.amounts.evaluate(
                state,
                dyn,
                EffectContext(sourceId = cardId, controllerId = casterId, xValue = xValue)
            ).coerceAtLeast(0)
        } catch (_: Exception) {
            unboundedFallback
        }
    }

    internal fun presentCastModalTargetDecision(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        cardName: String,
        baseCastAction: CastSpell,
        modes: List<com.wingedsheep.sdk.scripting.effects.Mode>,
        chosenModeIndices: List<Int>,
        resolvedModeTargets: List<List<ChosenTarget>>,
        currentOrdinal: Int
    ): ExecutionResult {
        var ordinal = currentOrdinal
        var targetsAccum = resolvedModeTargets

        while (ordinal < chosenModeIndices.size) {
            val modeIndex = chosenModeIndices[ordinal]
            val mode = modes[modeIndex]
            // Read through text-changing effects in force (CR 613.1c) — the spell exists (601.2a).
            val modeText = TextChanges.forSpell(state, cardId)
            val modeTargetReqs = mode.targetRequirements.map { req -> modeText?.let { req.applyTextReplacement(it) } ?: req }
            if (modeTargetReqs.isEmpty()) {
                targetsAccum = targetsAccum + listOf(emptyList())
                ordinal++
                continue
            }

            // Find legal targets per requirement. If any required slot has no legal
            // targets (and is mandatory), this mode can't resolve — surface an error.
            //
            // X was announced with the modes (CR 601.2b), so it is known here and every mode
            // that reads it must see it: as a filter bound ("creature card with mana value X or
            // less" — unbound, `ManaValueAtMostX` matches permissively and would offer targets
            // the cast then rejects) and as a target count ("up to X target creatures", a
            // `dynamicMaxCount` the static `count` placeholder would clamp to one).
            val xContext = PredicateContext(
                controllerId = casterId,
                sourceId = cardId,
                xValue = baseCastAction.xValue
            )
            val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
            modeTargetReqs.forEachIndexed { index, req ->
                legalTargetsMap[index] = targetFinder.findLegalTargets(
                    state, req, casterId, cardId, pipelineContext = xContext
                )
            }
            val allSatisfied = modeTargetReqs.withIndex().all { (index, req) ->
                legalTargetsMap[index]?.isNotEmpty() == true || req.effectiveMinCount == 0
            }
            if (!allSatisfied) {
                return ExecutionResult.error(state, "No legal targets for mode: ${mode.description}")
            }
            val requirementInfos = modeTargetReqs.mapIndexed { index, req ->
                // Targets are distinct objects, so no requirement can take more than there are
                // legal ones — which also keeps an unbounded "any number of target …" mode from
                // handing the client Int.MAX_VALUE as its cap. The floor stays the requirement's
                // own minimum: a mandatory "two target creatures" with one legal creature is an
                // unsatisfiable mode, and shrinking its cap would quietly let it through with one.
                val legalCount = legalTargetsMap[index]?.size ?: 0
                val maxTargets = resolveModeTargetMaxCount(state, req, casterId, cardId, baseCastAction.xValue)
                    .coerceAtMost(maxOf(legalCount, req.effectiveMinCount))
                com.wingedsheep.engine.core.TargetRequirementInfo(
                    index = index,
                    description = req.description,
                    mustDifferFromEarlier = req is com.wingedsheep.sdk.scripting.targets.TargetOther,
                    minTargets = req.effectiveMinCount,
                    maxTargets = maxTargets
                )
            }

            val pickNumber = ordinal + 1
            val prompt = "Choose targets for $cardName — ${mode.description} ($pickNumber of ${chosenModeIndices.size})"
            val continuation = com.wingedsheep.engine.core.CastModalTargetSelectionContinuation(
                cardId = cardId,
                casterId = casterId,
                baseCastAction = baseCastAction,
                modes = modes,
                chosenModeIndices = chosenModeIndices,
                resolvedModeTargets = targetsAccum,
                currentOrdinal = ordinal
            )
            return state.withPriority(casterId).suspendForDecision(
                question = { decisionId ->
                    com.wingedsheep.engine.core.ChooseTargetsDecision(
                        id = decisionId,
                        playerId = casterId,
                        prompt = prompt,
                        context = DecisionContext(
                            sourceId = cardId,
                            sourceName = cardName,
                            phase = DecisionPhase.CASTING,
                            effectHint = mode.description
                        ),
                        targetRequirements = requirementInfos,
                        legalTargets = legalTargetsMap,
                        // Cast-time per-mode target selection must be cancellable (K2 in plan):
                        // the pause sits before cost payment, so aborting rolls back cleanly.
                        canCancel = true
                    )
                },
                answer = continuation
            )
        }

        // All modes resolved without needing another decision — finalize directly.
        return finalizeModalCast(state, baseCastAction, chosenModeIndices, targetsAccum)
    }

    /**
     * Complete a choose-N modal cast by re-entering [execute] with a finalized
     * [CastSpell] action. `chosenModes`, `modeTargetsOrdered`, and the flat `targets`
     * union are populated so the normal cost / target / stack flow runs exactly once.
     */
    internal fun finalizeModalCast(
        state: GameState,
        baseCastAction: CastSpell,
        chosenModeIndices: List<Int>,
        resolvedModeTargets: List<List<ChosenTarget>>
    ): ExecutionResult {
        val flatTargets = resolvedModeTargets.flatten()
        val finalAction = baseCastAction.copy(
            chosenModes = chosenModeIndices,
            modeTargetsOrdered = resolvedModeTargets,
            targets = flatTargets
        )
        return execute(state, finalAction)
    }

    /**
     * Slice a flat target list into per-mode groups using each chosen mode's total
     * target slot count. Used when an action arrives with [CastSpell.chosenModes] and
     * [CastSpell.targets] populated but [CastSpell.modeTargetsOrdered] empty (the
     * web-client choose-1 modal cast path), so resolution can read targets per mode.
     *
     * If the flat target count doesn't line up with the modes' summed slot counts
     * (truncated, missing optional slots, etc.), returns an empty list — the cast
     * proceeds with the pre-existing flat-targets behavior rather than risking a
     * mis-sliced binding.
     */
    private fun deriveModeTargetsFromFlat(
        modalEffect: com.wingedsheep.sdk.scripting.effects.ModalEffect,
        chosenModes: List<Int>,
        flatTargets: List<ChosenTarget>
    ): List<List<ChosenTarget>> {
        // Choose-1: all flat targets belong to the single chosen mode. Using the mode's
        // max `count` here would mis-slice "up to N target" modes when the player picks
        // fewer than the maximum (e.g. Dewdrop Cure's "return up to two/three").
        if (chosenModes.size == 1) {
            return listOf(flatTargets.toList())
        }

        val perModeSlotCounts = chosenModes.map { idx ->
            modalEffect.modes.getOrNull(idx)?.targetRequirements?.sumOf { it.count } ?: 0
        }
        if (perModeSlotCounts.sum() != flatTargets.size) return emptyList()

        val result = mutableListOf<List<ChosenTarget>>()
        var cursor = 0
        for (slotCount in perModeSlotCounts) {
            result.add(flatTargets.subList(cursor, cursor + slotCount).toList())
            cursor += slotCount
        }
        return result
    }

    companion object {
        fun create(services: EngineServices): CastSpellHandler {
            return CastSpellHandler(
                services.zones,
                services.cardRegistry,
                services.turnManager,
                services.manaSolver,
                services.costCalculator,
                services.alternativePaymentHandler,
                services.costHandler,
                services.stackResolver,
                services.targetValidator,
                services.conditionEvaluator,
                services.manaAbilitySideEffectExecutor,
                services.legalityKernel,
                services.targetFinder
            )
        }
    }
}
