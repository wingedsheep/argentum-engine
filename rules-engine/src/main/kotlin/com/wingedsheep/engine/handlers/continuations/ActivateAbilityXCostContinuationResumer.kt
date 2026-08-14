package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.ActivateAbilityChooseManaXContinuation
import com.wingedsheep.engine.core.ActivateAbilityChooseXContinuation
import com.wingedsheep.engine.core.ActivateAbilityControllerTargetContinuation
import com.wingedsheep.engine.core.ActivateAbilityExileFromGraveyardContinuation
import com.wingedsheep.engine.core.ActivateAbilityVariablePermanentsContinuation
import com.wingedsheep.engine.core.ActivateAbilityExileXFromGraveyardContinuation
import com.wingedsheep.engine.core.ActivateAbilitySacrificeContinuation
import com.wingedsheep.engine.core.ActivateAbilityTapXTargetsContinuation
import com.wingedsheep.engine.core.CancelDecisionResponse
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.AdditionalCostPayment

/**
 * Resumer module for the two-step legal-actions submission flow on activated abilities whose cost
 * is "Tap X untapped permanents you control" (Secluded Starforge's pump ability, and any future
 * TapXPermanents-bearing card).
 *
 * Step 1 ([ActivateAbilityChooseXContinuation]) – the player just answered the ChooseNumberDecision
 * the handler raised. If X = 0 we re-enter the handler with `xValue = 0` and an empty tap list, and
 * the activation resolves with no permanents tapped (legal per CR 614 / `canPayAbilityCost` for
 * TapXPermanents). If X > 0 we raise a follow-up [SelectCardsDecision] sized to exactly X out of the
 * tap-target list captured at announcement time, and push [ActivateAbilityTapXTargetsContinuation].
 *
 * Step 2 ([ActivateAbilityTapXTargetsContinuation]) – the player just picked the X permanents to
 * tap. We re-enter the handler with the X and the chosen tap targets filled in. Cost payment +
 * stack placement + resolution then follow the normal engine-direct path, identical to what would
 * have happened if the action had arrived pre-filled (the regression-guard case in
 * SecludedStarforgeTest).
 */
class ActivateAbilityXCostContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    private val handler: ActivateAbilityHandler by lazy { ActivateAbilityHandler.create(services) }

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ActivateAbilityChooseXContinuation::class, ::resumeChooseX),
        resumer(ActivateAbilityChooseManaXContinuation::class, ::resumeChooseManaX),
        resumer(ActivateAbilityExileXFromGraveyardContinuation::class, ::resumeExileXFromGraveyard),
        resumer(ActivateAbilityTapXTargetsContinuation::class, ::resumeTapXTargets),
        resumer(ActivateAbilityExileFromGraveyardContinuation::class, ::resumeExileFromGraveyard),
        resumer(ActivateAbilitySacrificeContinuation::class, ::resumeSacrifice),
        resumer(ActivateAbilityVariablePermanentsContinuation::class, ::resumeVariablePermanents),
        resumer(ActivateAbilityControllerTargetContinuation::class, ::resumeControllerTargets)
    )

    /**
     * Resume after the player chooses X for an activated ability with an `{X}` **mana** cost
     * (Wizard's Rockets). Re-enter the handler with the chosen X bound; the `{X}` mana cost is then
     * paid for that amount and the ability resolves normally. No follow-up decision is needed.
     */
    private fun resumeChooseManaX(
        state: GameState,
        continuation: ActivateAbilityChooseManaXContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number response for ActivateAbility mana-X choice")
        }
        val chosenX = response.number.coerceAtLeast(0)
        return reenter(handler.execute(state, continuation.action.copy(xValue = chosenX)), checkForMore)
    }

    private fun resumeChooseX(
        state: GameState,
        continuation: ActivateAbilityChooseXContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number response for ActivateAbility X choice")
        }
        val chosenX = response.number.coerceIn(0, continuation.tapTargets.size)
        val action = continuation.action

        if (chosenX == 0) {
            // No permanents to tap — re-enter the handler with X bound to 0 and empty tap list,
            // mirroring the engine-direct path. The handler will pay the rest of the cost
            // (mana, etc.), put the ability on the stack, and run resolution normally.
            val replay = action.copy(
                xValue = 0,
                costPayment = (action.costPayment ?: AdditionalCostPayment())
                    .copy(tappedPermanents = emptyList())
            )
            return reenter(handler.execute(state, replay), checkForMore)
        }

        // Raise the follow-up tap-target selection. minSelections == maxSelections == chosenX so
        // the frontend renders "Select N/N" with a hard count (this is the assertion the
        // SecludedStarforgeTest UI-flow case pins).
        val sourceName = state.getEntity(action.sourceId)?.get<CardComponent>()?.name
        val decisionId = java.util.UUID.randomUUID().toString()
        val prompt = "Select $chosenX permanents to tap for ${sourceName ?: "this ability"}"
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = action.playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = action.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING
            ),
            options = continuation.tapTargets,
            minSelections = chosenX,
            maxSelections = chosenX,
            useTargetingUI = true
        )
        val nextFrame = ActivateAbilityTapXTargetsContinuation(
            decisionId = decisionId,
            action = action,
            chosenX = chosenX,
            tapTargets = continuation.tapTargets
        )
        val pausedState = state
            .withPendingDecision(decision)
            .pushContinuation(nextFrame)
        val event: GameEvent = DecisionRequestedEvent(
            decisionId = decisionId,
            playerId = action.playerId,
            decisionType = "SELECT_CARDS",
            prompt = prompt
        )
        return ExecutionResult.paused(pausedState, decision, listOf(event))
    }

    /**
     * Resume after the player picks which graveyard cards to exile for an
     * `ExileFromGraveyard` activated-ability cost (Rust Harvester etc.). Re-enters the handler
     * with the chosen cards filled into `costPayment.exiledCards`; CostHandler then exiles
     * exactly those cards instead of auto-picking the first N.
     */
    private fun resumeExileFromGraveyard(
        state: GameState,
        continuation: ActivateAbilityExileFromGraveyardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card-selection response for ActivateAbility ExileFromGraveyard")
        }
        if (response.selectedCards.size != continuation.exileCount) {
            return ExecutionResult.error(
                state,
                "Expected ${continuation.exileCount} cards to exile, got ${response.selectedCards.size}"
            )
        }
        if (response.selectedCards.any { it !in continuation.exileCandidates }) {
            return ExecutionResult.error(state, "Selected card is not in the list of valid exile candidates")
        }

        val action = continuation.action
        val replay = action.copy(
            costPayment = (action.costPayment ?: AdditionalCostPayment())
                .copy(exiledCards = response.selectedCards)
        )
        return reenter(handler.execute(state, replay), checkForMore)
    }

    /**
     * Resume after the player picks the graveyard cards for an `ExileXFromGraveyard` cost. X *is*
     * the size of that selection, so a single decision settles both: re-enter the handler with the
     * chosen cards in `costPayment.exiledCards` and `xValue` bound to how many were chosen.
     *
     * When a `{X}` mana symbol already fixed X (Necropolis Fiend), [fixedCount] pins the selection
     * size and `xValue` on the action is left as it was — the two agree by construction.
     */
    private fun resumeExileXFromGraveyard(
        state: GameState,
        continuation: ActivateAbilityExileXFromGraveyardContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(
                state,
                "Expected card-selection response for ActivateAbility ExileXFromGraveyard"
            )
        }
        val selected = response.selectedCards
        if (selected.any { it !in continuation.exileCandidates }) {
            return ExecutionResult.error(state, "Selected card is not in the list of valid exile candidates")
        }
        val fixedCount = continuation.fixedCount
        if (fixedCount != null && selected.size != fixedCount) {
            return ExecutionResult.error(
                state,
                "Expected $fixedCount cards to exile, got ${selected.size}"
            )
        }

        val action = continuation.action
        val replay = action.copy(
            xValue = fixedCount ?: selected.size,
            costPayment = (action.costPayment ?: AdditionalCostPayment())
                .copy(exiledCards = selected)
        )
        return reenter(handler.execute(state, replay), checkForMore)
    }

    /**
     * Resume after the player picks which permanents to sacrifice for a `Sacrifice` activated-ability
     * cost (Sage of Lat-Nam etc.). Re-enters the handler with the chosen permanents filled into
     * `costPayment.sacrificedPermanents`; CostHandler then sacrifices exactly those.
     */
    private fun resumeSacrifice(
        state: GameState,
        continuation: ActivateAbilitySacrificeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card-selection response for ActivateAbility Sacrifice")
        }
        if (response.selectedCards.size != continuation.sacrificeCount) {
            return ExecutionResult.error(
                state,
                "Expected ${continuation.sacrificeCount} permanents to sacrifice, got ${response.selectedCards.size}"
            )
        }
        if (response.selectedCards.any { it !in continuation.sacrificeCandidates }) {
            return ExecutionResult.error(state, "Selected permanent is not in the list of valid sacrifice candidates")
        }
        // "Sacrifice N ... with different names" — the chosen permanents must be pairwise distinct.
        if (continuation.distinctNames) {
            val names = response.selectedCards.mapNotNull {
                state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name
            }
            if (names.size != response.selectedCards.size || names.toSet().size != names.size) {
                return ExecutionResult.error(state, "Sacrificed permanents must all have different names")
            }
        }

        val action = continuation.action
        val replay = action.copy(
            costPayment = (action.costPayment ?: AdditionalCostPayment())
                .copy(sacrificedPermanents = response.selectedCards)
        )
        return reenter(handler.execute(state, replay), checkForMore)
    }

    /**
     * Resume after the controller picks which permanents pay a variable-count
     * `CostAtom.VariablePermanents` cost — "Exile one or more other [filter] you control with total
     * mana value X" (Fabrication Foundry) or "Sacrifice one or more [filter]" (Radiant Lotus).
     * Fills the chosen permanents into `costPayment.variableCostPermanents` and re-enters the
     * handler, which computes X from them and pauses again for the ability's target.
     */
    private fun resumeVariablePermanents(
        state: GameState,
        continuation: ActivateAbilityVariablePermanentsContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response is CancelDecisionResponse) {
            // The cost is paid after this pause, so bailing here is side-effect-free.
            return ExecutionResult.success(state.withPriority(continuation.action.playerId))
        }
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card-selection response for ActivateAbility VariablePermanents")
        }
        val selected = response.selectedCards
        if (selected.size < continuation.minCount) {
            return ExecutionResult.error(
                state,
                "Must choose at least ${continuation.minCount} permanent(s), got ${selected.size}"
            )
        }
        if (selected.toSet().size != selected.size) {
            return ExecutionResult.error(state, "Cannot choose the same permanent twice for one cost")
        }
        if (selected.any { it !in continuation.candidates }) {
            return ExecutionResult.error(state, "Selected permanent is not in the list of valid candidates")
        }
        val action = continuation.action
        val replay = action.copy(
            costPayment = (action.costPayment ?: AdditionalCostPayment())
                .copy(variableCostPermanents = selected)
        )
        return reenter(handler.execute(state, replay), checkForMore)
    }

    /**
     * Resume after the controller picks the X-bounded target of an `VariablePermanents` ability
     * (Fabrication Foundry: "Return target artifact card with mana value X or less …"). The exile
     * selection — and thus X — is already on the action; convert the response into [ChosenTarget]s,
     * fill `action.targets`, and re-enter the handler to pay the cost and put the ability on the
     * stack.
     */
    private fun resumeControllerTargets(
        state: GameState,
        continuation: ActivateAbilityControllerTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response is CancelDecisionResponse) {
            // The cost still hasn't been paid at this point, so cancelling is side-effect-free.
            return ExecutionResult.success(state.withPriority(continuation.action.playerId))
        }
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for ActivateAbility target selection")
        }
        val chosen: List<ChosenTarget> = response.selectedTargets.entries
            .sortedBy { it.key }
            .flatMap { (_, ids) -> ids.map { entityIdToChosenTarget(state, it) } }
        val expectedMin = continuation.requirements.sumOf { it.effectiveMinCount }
        if (chosen.size < expectedMin) {
            return ExecutionResult.error(state, "Not enough targets chosen")
        }
        val replay = continuation.action.copy(targets = chosen)
        return reenter(handler.execute(state, replay), checkForMore)
    }

    private fun resumeTapXTargets(
        state: GameState,
        continuation: ActivateAbilityTapXTargetsContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card-selection response for ActivateAbility TapX targets")
        }
        if (response.selectedCards.size != continuation.chosenX) {
            return ExecutionResult.error(
                state,
                "Expected ${continuation.chosenX} permanents to tap, got ${response.selectedCards.size}"
            )
        }
        if (response.selectedCards.any { it !in continuation.tapTargets }) {
            return ExecutionResult.error(state, "Selected permanent is not in the list of valid tap targets")
        }

        val action = continuation.action
        val replay = action.copy(
            xValue = continuation.chosenX,
            costPayment = (action.costPayment ?: AdditionalCostPayment())
                .copy(tappedPermanents = response.selectedCards)
        )
        return reenter(handler.execute(state, replay), checkForMore)
    }

    /**
     * Chains a handler re-entry into `checkForMore` so a continuation pushed *beneath* this
     * activation still resumes.
     *
     * These resumers finish by calling [ActivateAbilityHandler.execute] again with the player's
     * choice filled in. Returning that result directly strands anything underneath — most visibly a
     * [com.wingedsheep.engine.core.ReopenManaPaymentDecisionContinuation], which is how a mana
     * ability activated during a mana payment (CR 605.3a) puts the payment window back up. A paused
     * or failed re-entry is passed through untouched; its own frame is still in flight.
     */
    private fun reenter(result: ExecutionResult, checkForMore: CheckForMore): ExecutionResult =
        if (result.isPaused || result.error != null) result
        else checkForMore(result.newState, result.events)
}
