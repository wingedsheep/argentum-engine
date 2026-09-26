package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.CostPaymentChoices
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.legality.LegalityKernel
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent
import com.wingedsheep.engine.state.components.player.ExhaustAbilitiesActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.player.LoyaltyAbilitiesActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlin.reflect.KClass

/**
 * Handler for the ActivateAbility action.
 *
 * Handles activating abilities on permanents, including:
 * - Mana abilities (immediate resolution)
 * - Non-mana abilities (go on stack)
 * - Planeswalker loyalty abilities
 *
 * Activation follows the activation procedure (CR 602.2, which runs the casting steps
 * CR 601.2b–h), one stage per collaborator:
 *  1. **Announce** ([announce]): look up the ability ([ActivatedAbilityResolver]), bound X, and
 *     determine the total cost ([ActivationCostTotaller]).
 *  2. **Choices** ([ActivationChoicePauses]): pause for any choice the action doesn't already
 *     carry — an opponent's targets, X, which objects pay a cost, an X-bounded target.
 *  3. **Pay** ([ActivationCostPayer]): alternative payments, mana abilities (explicit sources or
 *     [ActivationAutoTapper]), every cost atom, the X portion; capture last-known information.
 *  4. **Record** ([recordActivation]): the per-turn / once-ever / loyalty / equip / exhaust tallies.
 *  5. **Resolve or stack**: a mana ability resolves at once ([ActivatedManaAbilityResolver],
 *     CR 605.3); anything else goes on the stack ([putOnStack]), with any Station-style repeats.
 *
 * Legality is [ActivationValidator]'s job — one function per question.
 */
class ActivateAbilityHandler(
    private val cardRegistry: CardRegistry,
    private val turnManager: TurnManager,
    private val costHandler: CostHandler,
    private val manaSolver: ManaSolver,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val castPermissionUtils: CastPermissionUtils,
    private val legality: LegalityKernel,
    private val manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor,
    private val targetFinder: TargetFinder
) : ActionHandler<ActivateAbility> {
    override val actionType: KClass<ActivateAbility> = ActivateAbility::class

    private val abilityResolver = ActivatedAbilityResolver(cardRegistry, castPermissionUtils)
    private val costTotaller = ActivationCostTotaller(castPermissionUtils, amountEvaluator = conditionEvaluator.amounts)
    private val validator = ActivationValidator(
        cardRegistry = cardRegistry,
        turnManager = turnManager,
        costHandler = costHandler,
        manaSolver = manaSolver,
        alternativePaymentHandler = alternativePaymentHandler,
        targetValidator = targetValidator,
        castPermissionUtils = castPermissionUtils,
        abilityResolver = abilityResolver,
        costTotaller = costTotaller,
        legality = legality,
        predicateEvaluator = conditionEvaluator.predicates
    )
    private val choicePauses = ActivationChoicePauses(costHandler, manaSolver, targetFinder = targetFinder)
    private val autoTapper = ActivationAutoTapper(manaSolver, manaAbilitySideEffectExecutor)
    private val costPayer = ActivationCostPayer(costHandler, manaSolver, alternativePaymentHandler, autoTapper, predicateEvaluator = conditionEvaluator.predicates)
    private val manaAbilityResolver =
        ActivatedManaAbilityResolver(cardRegistry, conditionEvaluator, effectExecutorRegistry, predicateEvaluator = conditionEvaluator.predicates)

    override fun validate(state: GameState, action: ActivateAbility): String? =
        validator.validate(state, action)

    override fun execute(state: GameState, action: ActivateAbility): ExecutionResult {
        val window = ManaPaymentWindow.openFor(state, action.playerId)
            ?: return executeActivation(state, action)
        return executeInManaPaymentWindow(state, action, window)
    }

    /**
     * Runs a mana ability activated while the engine is asking [action]'s player for a mana payment
     * (CR 605.3a — see [ManaPaymentWindow]).
     *
     * The window is set aside for the duration so the ability resolves against a decision-free
     * state, then re-raised. Two things must survive the round trip:
     *  - **Priority.** The mana-ability path ends with `withPriority(activatingPlayer)` when the
     *    activation cost fired a trigger. That's right at priority and wrong here — the payment
     *    resumer will hand priority back itself once the payment completes — so it's restored.
     *  - **The window.** If the ability paused for a decision of its own, the
     *    [ReopenManaPaymentDecisionContinuation] pushed by [ManaPaymentWindow.suspend] re-raises it
     *    afterwards; otherwise it's re-raised here.
     */
    private fun executeInManaPaymentWindow(
        state: GameState,
        action: ActivateAbility,
        window: SelectManaSourcesDecision
    ): ExecutionResult {
        val result = executeActivation(ManaPaymentWindow.suspend(state, window), action)

        // A failed activation must not eat the window — roll all the way back.
        result.error?.let { return ExecutionResult.error(state, it) }

        val restored = if (result.state.priorityPlayerId == state.priorityPlayerId) result.state
        else result.state.copy(
            priorityPlayerId = state.priorityPlayerId,
            priorityPassedBy = state.priorityPassedBy
        )
        if (result.outcome is Outcome.Paused) {
            return ExecutionResult.propagatePause(restored, result.events)
        }
        return ManaPaymentWindow.resumeIfPending(restored, result.events, manaSolver)
            ?: ExecutionResult.success(restored, result.events)
    }
    private fun executeActivation(state: GameState, action: ActivateAbility): ExecutionResult {
        // 1. Announce (CR 602.2a–b): the ability, X, and the total cost.
        val activation = when (val announced = announce(state, action)) {
            is Announcement.Announced -> announced.activation
            is Announcement.Rejected -> return ExecutionResult.error(state, announced.reason)
        }

        // 2. Choices still to be made before any cost is paid (CR 601.2b–c): an opponent's targets,
        //    X, which objects pay a cost, a target bounded by a cost-defined X.
        choicePauses.firstPendingChoice(state, activation)?.let { return it }

        // 3. Pay the total cost (CR 601.2g–h): mana abilities first, then every cost atom.
        val paymentContext =
            buildAbilityPaymentContext(activation.cardComponent, state.projectedState, action.sourceId, activation.ability)
        val payment = when (val paid = costPayer.pay(state, activation, paymentContext)) {
            is ActivationPaymentOutcome.Paid -> paid.payment
            is ActivationPaymentOutcome.Failed -> return ExecutionResult.error(state, paid.reason)
        }

        // 4. The ability has been activated (CR 602.2i): record it for the activation limits.
        val recorded = recordActivation(payment.state, activation)

        // Apply text replacement if the source has a TextReplacementComponent
        val effect = activation.textReplacement
            ?.let { activation.ability.effect.applyTextReplacement(it) }
            ?: activation.ability.effect

        // 5. Mana abilities don't use the stack (CR 605.3); everything else goes on it.
        if (activation.ability.isManaAbility) {
            return manaAbilityResolver.resolve(
                stateBeforeActivation = state,
                state = recorded,
                activation = activation,
                effect = effect,
                events = payment.events,
                sacrificedSnapshots = payment.snapshots.sacrificed,
            )
        }
        return putOnStack(state, recorded, activation, effect, payment, paymentContext)
    }

    private sealed interface Announcement {
        data class Announced(val activation: Activation) : Announcement
        data class Rejected(val reason: String) : Announcement
    }

    /**
     * Stage 1 (CR 602.2a–b): find the ability on the object, check the announced X against its
     * bounds, determine the total cost, and bind X as far as the action already allows.
     */
    private fun announce(state: GameState, action: ActivateAbility): Announcement {
        val abilityEntityId = EntityId.generate()
        val sourceObject = state.objectRef(action.sourceId)
        val activationReferences = ObjectReferenceEnvironment(
            captured = true, origin = sourceObject, source = sourceObject, resolutionKey = abilityEntityId.value,
        )

        val container = state.getEntity(action.sourceId)
            ?: return Announcement.Rejected("Source not found")

        val cardComponent = container.get<CardComponent>()
            ?: return Announcement.Rejected("Source is not a card")

        val sourceName = nameVisibleToAll(state, action.sourceId, cardComponent.name)

        // Retain which lookup branch supplied the concrete ability. A CardComponent on the
        // receiving permanent proves only that the object is a card; it does not make a runtime,
        // static, emblem-granted, or intrinsic ability part of that card's definition.
        val abilityLookup = abilityResolver.lookup(state, action.sourceId, action.abilityId)
            ?: return Announcement.Rejected("Ability not found")
        val ability = abilityLookup.ability
        if (ability.cost == AbilityCost.LoyaltyX && action.xValue != null) {
            val loyalty = container.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
            if (action.xValue !in 0..loyalty) {
                return Announcement.Rejected("X must be between 0 and $loyalty")
            }
        }

        // "X can't be 0" abilities (Gogo, Master of Mimicry): reject an engine-direct activation that
        // pre-fills an X below the ability's minimum. The legal-actions submission path enforces the
        // same bound via the X-choice decision's lower value.
        if (action.xValue != null && action.xValue < ability.minimumXValue) {
            return Announcement.Rejected("X must be at least ${ability.minimumXValue} for ${sourceName}")
        }

        // Resolve a *defined* {X} (CR 107.3c) before the reductions, matching validate() and the
        // enumerator so all three paths charge the same number.
        val textReplacement = TextChanges.of(state, action.sourceId)
        val definedXValue = castPermissionUtils.definedXValue(state, ability, action.sourceId, action.playerId)
        val effectiveCost = costTotaller.total(state, action, ability, textReplacement)

        // Variable-count "exile/sacrifice one or more permanents you control" cost: X is defined by
        // the payer's cost choice (CR 601.2b — a variable defined by a cost choice is announced as
        // the ability is activated), measured as the chosen set's total mana value (Fabrication
        // Foundry) or its size (Radiant Lotus). It bounds any X-limited target and is stored on the
        // stack for 608.2b re-validation and for `DynamicAmount.XValue` reads at resolution. When
        // the cost is being paid, the chosen set already rides on the action's costPayment.
        val variablePermanentsCost = effectiveCost.extractVariablePermanentsCost()
        val chosenForCost = action.costPayment?.variableCostPermanents ?: emptyList()
        // An X the ability's own text defines (CR 107.3c) outranks both: it is not the payer's to
        // choose, and it is the value every other instance of X on this activation uses (CR 107.3i)
        // — the X-linked non-mana costs and any `DynamicAmount.XValue` read at resolution.
        val effectiveXValue: Int? = definedXValue
            ?: if (variablePermanentsCost != null && chosenForCost.isNotEmpty())
                variableCostX(state, variablePermanentsCost, chosenForCost)
            else action.xValue

        return Announcement.Announced(
            Activation(
                action = action,
                abilityEntityId = abilityEntityId,
                activationReferences = activationReferences,
                container = container,
                cardComponent = cardComponent,
                sourceName = sourceName,
                abilityLookup = abilityLookup,
                textReplacement = textReplacement,
                effectiveCost = effectiveCost,
                effectiveXValue = effectiveXValue,
            )
        )
    }

    /**
     * Stage 4: record the activation on the trackers the activation limits read — per-turn and
     * once-ever restrictions, the planeswalker loyalty limit (CR 606.3), and the per-player equip
     * and exhaust tallies.
     */
    private fun recordActivation(state: GameState, activation: Activation): GameState {
        val action = activation.action
        val ability = activation.ability
        var currentState = state

        // Track per-turn activation if the ability has an OncePerTurn or MaxPerTurn restriction.
        // `trackActivations` opts an unrestricted ability into the same tally so its own effect can
        // read the count back (Farrelite Priest's burnout clause).
        if (ability.trackActivations || LegalityKernel.tracksActivationsPerTurn(ability)) {
            // Only track if source is still on the battlefield (it might have been bounced as cost)
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
                    c.with(tracker.withActivated(ability.id))
                }
            }
        }

        // Track once-ever activation if the ability has an Once restriction
        if (LegalityKernel.tracksActivationsEver(ability)) {
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedEverComponent>() ?: AbilityActivatedEverComponent()
                    c.with(tracker.withActivated(ability.id))
                }
            }
        }

        // Track planeswalker loyalty ability activation (Rule 606.3: once per planeswalker per turn)
        if (ability.isPlaneswalkerAbility) {
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
                    c.with(tracker.withLoyaltyActivated())
                }
            }
        }

        // Track loyalty activations per player this turn ("if you've activated a loyalty ability
        // this turn" — Kiora of Salt and Sand). Counted at activation (CR 602.2), independent of the
        // per-planeswalker CR 606.3 tally above, which dies with the planeswalker.
        if (ability.isPlaneswalkerAbility) {
            currentState = currentState.updateEntity(action.playerId) { c ->
                val tracker = c.get<LoyaltyAbilitiesActivatedThisTurnComponent>()
                    ?: LoyaltyAbilitiesActivatedThisTurnComponent()
                c.with(tracker.copy(count = tracker.count + 1))
            }
        }

        // Track equip activations this turn (Forge Anew's free-first-equip keys off count == 0).
        if (ability.isEquipAbility) {
            currentState = currentState.updateEntity(action.playerId) { c ->
                val tracker = c.get<EquipActivationsThisTurnComponent>()
                    ?: EquipActivationsThisTurnComponent()
                c.with(tracker.copy(count = tracker.count + 1))
            }
        }

        // Track exhaust activations this turn (CR 702.177). Elvish Refueler's waiver of the
        // once-only memory holds only "as long as you haven't activated an exhaust ability this
        // turn", so the count has to advance for the very activation that used the waiver too —
        // which is why this is unconditional on whether the waiver applied.
        if (ability.isExhaust) {
            currentState = currentState.updateEntity(action.playerId) { c ->
                val tracker = c.get<ExhaustAbilitiesActivatedThisTurnComponent>()
                    ?: ExhaustAbilitiesActivatedThisTurnComponent()
                c.with(tracker.copy(count = tracker.count + 1))
            }
        }
        return currentState
    }

    /**
     * Stage 5 for a non-mana ability: put it on the stack, then queue any repeated activations
     * (repeatCount > 1).
     *
     * @param stateBeforeActivation the state the activation started from — the source's
     *   battlefield timestamp and face-change clock are read from it.
     */
    private fun putOnStack(
        stateBeforeActivation: GameState,
        state: GameState,
        activation: Activation,
        effect: Effect,
        payment: ActivationPayment,
        paymentContext: SpellPaymentContext?,
    ): ExecutionResult {
        val action = activation.action
        val ability = activation.ability
        val effectiveCost = activation.effectiveCost
        val snapshots = payment.snapshots
        val events = payment.events.toMutableList()

        // Snapshot of the activation's cost-side events (cost payment + the {T}/tap/loyalty events)
        // before anything else is appended: the objects these events name are the ones the
        // resolving ability may refer back to.
        val activationCostEvents = payment.events

        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.sourceId,
            sourceName = activation.sourceName,
            controllerId = action.playerId,
            effect = effect,
            sacrificedPermanents = snapshots.sacrificed,
            // VariablePermanents X (exiled total mana value) is stored so 608.2b re-validation of the
            // "mana value X or less" target and any XValue read resolve against it; else action.xValue.
            xValue = activation.effectiveXValue,
            tappedPermanents = payment.firstTapSlice,
            tappedEntitySnapshots = snapshots.tapped,
            // An exile cost records its selection so the resolving effect can refer back to the
            // cards it exiled (`CardSource.ExiledAsCost`) — the sum-gated form (Baron Helmut Zemo)
            // and the plain counted form (Necropolis) alike. Empty for an ability whose cost exiles
            // nothing, so nothing else changes.
            exiledAsCostCards = if (effectiveCost.hasExileAtom()) payment.exileChoices else emptyList(),
            // "The discarded card" (Hisoka, Minamo Sensei) — the activation counterpart of a
            // spell's additional discard cost, read at resolution as EffectTarget.DiscardedAsCost.
            discardedAsCostCards = payment.discardedCards,
            lastKnownSourceCounters = snapshots.lastKnownSourceCounters,
            lastKnownSourceSnapshot = snapshots.lastKnownSourceSnapshot,
            lastKnownSourceAttachments = snapshots.lastKnownSourceAttachments,
            revealedNotedCreatureType = snapshots.revealedNotedCreatureType,
            descriptionOverride = ability.descriptionOverride,
            abilityIdentity = activation.abilityLookup.definitionIdentity,
            activatedAbility = ability,
            granterId = activation.staticGranterId,
            objectReferences = activation.activationReferences.authorize(activationCostEvents),
            sourceBattlefieldTimestamp = stateBeforeActivation.getEntity(action.sourceId)
                ?.get<BattlefieldEntryTimestampComponent>()?.timestamp,
            // CR 701.28f — freeze the source's face-change clock as the ability goes on the stack;
            // an instruction inside it to transform that same permanent is ignored if the permanent
            // turns over before this resolves.
            sourceFaceChanges = stateBeforeActivation.getEntity(action.sourceId)
                ?.get<DoubleFacedComponent>()
                ?.faceChanges,
            // Lock in the activation-time damage division (CR 601.2d) so removal in response
            // can't hand the controller a fresh division at resolution.
            damageDistribution = action.damageDistribution
        )

        // Apply text-changing effects to the target requirements for resolution-time re-validation
        val effectiveTargetReqs = activation.targetRequirements

        val stackResult = stackResolver.putActivatedAbility(
            state, abilityOnStack, action.targets,
            targetRequirements = effectiveTargetReqs,
            costsTap = effectiveCost.hasTapCost(),
            isExhaust = ability.isExhaust,
            cantBeCopied = ability.cantBeCopied,
            isLoyalty = ability.isPlaneswalkerAbility,
            loyaltyCountersRemoved = when (val cost = ability.cost) {
                is AbilityCost.Loyalty -> if (cost.change < 0) -cost.change else 0
                AbilityCost.LoyaltyX -> activation.effectiveXValue ?: 0
                else -> 0
            },
        )
        var currentState = stackResult.newState
        events.addAll(stackResult.events)

        // Handle repeated activations (repeatCount > 1)
        if (action.repeatCount > 1) {
            currentState = putRepeatedActivationsOnStack(
                currentState, activation, effect, payment, paymentContext, effectiveTargetReqs,
                activationCostEvents, events
            )
        }

        val allEvents = events.toList()

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Activations 2..repeatCount of a repeated activation: each re-pays the cost from the current
     * pool (auto-tapping for its mana) and goes on the stack. Stops early — keeping what was
     * already queued — as soon as one can't be paid.
     */
    private fun putRepeatedActivationsOnStack(
        state: GameState,
        activation: Activation,
        effect: Effect,
        payment: ActivationPayment,
        paymentContext: SpellPaymentContext?,
        effectiveTargetReqs: List<TargetRequirement>,
        activationCostEvents: List<GameEvent>,
        events: MutableList<GameEvent>,
    ): GameState {
        val action = activation.action
        val ability = activation.ability
        val manaCost = payment.manaCost
        var currentState = state
        for (i in 2..action.repeatCount) {
            // Re-read mana pool from current state
            val repeatPoolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
                ?: ManaPoolComponent()
            var repeatPool = ManaPool(
                white = repeatPoolComponent.white,
                blue = repeatPoolComponent.blue,
                black = repeatPoolComponent.black,
                red = repeatPoolComponent.red,
                green = repeatPoolComponent.green,
                colorless = repeatPoolComponent.colorless,
                manaBySubtype = repeatPoolComponent.manaBySubtype,
                manaBySource = repeatPoolComponent.manaBySource
            )

            // Auto-tap for mana cost
            if (manaCost != null) {
                val autoTapResult = autoTapper.autoTapForManaCost(currentState, action.playerId, repeatPool, manaCost, 0, abilityContext = paymentContext)
                    ?: break // Can't afford — stop early
                currentState = autoTapResult.newState
                repeatPool = autoTapResult.newPool
                events.addAll(autoTapResult.events)
            }

            // Station-style batch: this activation taps the i-th chosen creature (1-indexed
            // list, so iteration `i` consumes element `i - 1`). Other repeatable abilities
            // (mana-only) carry no tap choices, so the slice is empty and the cost re-pays from
            // mana as before. Snapshot the creature before it's tapped (Rule 113.7a) so
            // DynamicAmount.StationCharge reads its power off this instance's own snapshot.
            val repeatTapSlice = if (payment.isTapBatch) listOf(action.costPayment!!.tappedPermanents[i - 1]) else emptyList()
            val repeatTapSnapshots = captureEntitySnapshots(repeatTapSlice, currentState.projectedState)

            // Pay the cost
            val repeatCostResult = costHandler.payAbilityCost(
                currentState, activation.effectiveCost, action.sourceId, action.playerId, repeatPool, CostPaymentChoices(tapChoices = repeatTapSlice), paymentContext
            )
            if (!repeatCostResult.success) break // Can't pay — stop early

            currentState = repeatCostResult.newState!!
            repeatPool = repeatCostResult.newManaPool!!
            events.addAll(repeatCostResult.events)

            // Update mana pool on state (consuming provenance for the floating mana this repeat
            // spent, same rule as the primary writeback in ActivationCostPayer).
            val repeatOriginalUnrestricted = repeatPoolComponent.white + repeatPoolComponent.blue +
                repeatPoolComponent.black + repeatPoolComponent.red + repeatPoolComponent.green +
                repeatPoolComponent.colorless
            val repeatFinalUnrestricted = repeatPool.white + repeatPool.blue + repeatPool.black +
                repeatPool.red + repeatPool.green + repeatPool.colorless
            val (repeatPoolAfterProvenance, _) =
                repeatPool.consumeProvenance(maxOf(0, repeatOriginalUnrestricted - repeatFinalUnrestricted))
            currentState = currentState.updateEntity(action.playerId) { c ->
                c.with(ManaPoolComponent(
                    white = repeatPool.white,
                    blue = repeatPool.blue,
                    black = repeatPool.black,
                    red = repeatPool.red,
                    green = repeatPool.green,
                    colorless = repeatPool.colorless,
                    manaBySubtype = repeatPoolAfterProvenance.manaBySubtype,
                    manaBySource = repeatPoolAfterProvenance.manaBySource
                ))
            }

            // Put another ability on the stack
            val repeatAbilityOnStack = ActivatedAbilityOnStackComponent(
                sourceId = action.sourceId,
                objectReferences = activation.activationReferences.authorize(activationCostEvents),
                sourceName = activation.sourceName,
                controllerId = action.playerId,
                effect = effect,
                sacrificedPermanents = emptyList(),
                xValue = action.xValue,
                tappedPermanents = repeatTapSlice,
                tappedEntitySnapshots = repeatTapSnapshots,
                descriptionOverride = ability.descriptionOverride,
                abilityIdentity = activation.abilityLookup.definitionIdentity,
                activatedAbility = ability,
                granterId = activation.staticGranterId
            )
            val repeatStackResult = stackResolver.putActivatedAbility(
                currentState, repeatAbilityOnStack, action.targets,
                targetRequirements = effectiveTargetReqs,
                isExhaust = ability.isExhaust,
            )
            currentState = repeatStackResult.newState
            events.addAll(repeatStackResult.events)
        }
        return currentState
    }

    internal fun pauseForOpponentChosenTargetsForDecider(
        state: GameState,
        action: ActivateAbility,
        sourceName: String,
        fullTargetReqs: List<TargetRequirement>,
        opponentReqs: List<TargetRequirement>,
        deciderId: EntityId
    ): ExecutionResult = choicePauses.pauseForOpponentChosenTargetsForDecider(
        state, action, sourceName, fullTargetReqs, opponentReqs, deciderId
    )

    companion object {
        fun create(services: EngineServices): ActivateAbilityHandler {
            return ActivateAbilityHandler(
                services.cardRegistry,
                services.turnManager,
                services.costHandler,
                services.manaSolver,
                services.alternativePaymentHandler,
                services.effectExecutorRegistry,
                services.stackResolver,
                services.targetValidator,
                services.conditionEvaluator,
                services.castPermissionUtils,
                services.legalityKernel,
                services.manaAbilitySideEffectExecutor,
                services.targetFinder
            )
        }
    }

    /**
     * The ability's X value for a [com.wingedsheep.sdk.scripting.costs.CostAtom.VariablePermanents]
     * cost, measured from the permanents the payer chose (CR 601.2b — a variable defined by a cost
     * choice is announced at activation). Read at target validation and stored on the stack for
     * resolution re-validation and `DynamicAmount.XValue`.
     *
     * Delegates to the shared [com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost.measure]
     * so the activated-ability path, the cast path, and the enumerators all measure a selection the
     * same way.
     */
    private fun variableCostX(
        state: GameState,
        atom: com.wingedsheep.sdk.scripting.costs.CostAtom.VariablePermanents,
        chosenIds: List<EntityId>,
    ): Int = com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
        .measure(state, atom.xMeasure, chosenIds)
}
