package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ActivateAbilityChooseManaXContinuation
import com.wingedsheep.engine.core.ActivateAbilityChooseXContinuation
import com.wingedsheep.engine.core.ActivateAbilityControllerTargetContinuation
import com.wingedsheep.engine.core.ActivateAbilityExileFromGraveyardContinuation
import com.wingedsheep.engine.core.ActivateAbilityExileXFromGraveyardContinuation
import com.wingedsheep.engine.core.ActivateAbilityOpponentChooserContinuation
import com.wingedsheep.engine.core.ActivateAbilityOpponentTargetContinuation
import com.wingedsheep.engine.core.ActivateAbilitySacrificeContinuation
import com.wingedsheep.engine.core.ActivateAbilityVariablePermanentsContinuation
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.mechanics.cost.PlayerCounterPayment
import com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * The announcement-time choices an activation may have to stop and ask for before any cost is paid
 * (CR 601.2b–c via CR 602.2b): an opponent's choice of target, X, which objects pay a cost that
 * names a choice, and a target whose legality depends on that choice.
 *
 * The legal-actions submission path sends a bare [ActivateAbility] and relies on the engine to ask;
 * the engine-direct path pre-fills the answers on the action and skips every pause. Each pause's
 * continuation re-enters `ActivateAbilityHandler.execute` with the answer merged into the action.
 */
internal class ActivationChoicePauses(
    private val costHandler: CostHandler,
    private val manaSolver: ManaSolver,
    private val targetFinder: TargetFinder
) {

    /**
     * The first choice [activation] still needs, as a paused [ExecutionResult] — or an error when a
     * choice can't legally be made — or `null` when every choice is already answered and the
     * activation can go on to payment. Checked in activation-procedure order.
     */
    fun firstPendingChoice(state: GameState, activation: Activation): ExecutionResult? =
        opponentChosenTargets(state, activation)
            ?: tapXPermanentsChoice(state, activation)
            ?: manaXChoice(state, activation)
            ?: exileXFromGraveyardChoice(state, activation)
            ?: exileFromGraveyardChoice(state, activation)
            ?: sacrificeChoice(state, activation)
            ?: putOnLibraryChoice(state, activation)
            ?: variablePermanentsChoice(state, activation)
            ?: variablePermanentsTargetChoice(state, activation)

    // -------------------------------------------------------------------
    // "… of an opponent's choice" target selection (Cuombajj Witches).
    //
    // CR 601.2c (choose targets) precedes 601.2g–h (pay costs), so this runs before any cost
    // work. The controller's own targets already ride on `action.targets`; any opponent-chosen
    // requirement is selected here by routing a ChooseTargetsDecision to an opponent. The
    // resumer merges that pick into `action.targets` and re-enters with
    // `opponentTargetsChosen = true`, so this block is skipped on the second pass.
    // -------------------------------------------------------------------
    private fun opponentChosenTargets(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        if (action.opponentTargetsChosen) return null
        val fullTargetReqs = activation.targetRequirements
        val opponentReqs = fullTargetReqs.filter { it.chooser == TargetChooser.Opponent }
        if (opponentReqs.isEmpty()) return null
        return pauseForOpponentChosenTargets(
            state, action, activation.sourceName, fullTargetReqs, opponentReqs
        )
    }

    // -------------------------------------------------------------------
    // TapXPermanents two-step UI flow (legal-actions submission path).
    //
    // When the legal-actions list surfaces an X-variable tap cost (Secluded
    // Starforge: "Tap X untapped artifacts you control"), the frontend
    // submits the bare `ActivateAbility` (xValue/costPayment empty) and
    // expects the engine to pause for two follow-up decisions: pick X,
    // then pick the X permanents to tap. Without this branch the engine
    // silently treats X=0, pays no cost, and resolves a no-op activation
    // — see SecludedStarforgeTest's "UI flow: choosing X=3 …" case.
    //
    // The engine-direct path (pre-filling xValue and tappedPermanents on
    // the action — used by the prior passing test and most server-side
    // composite flows) is untouched: both `xValue != null` and a non-empty
    // `tappedPermanents` skip past this fast-path.
    // -------------------------------------------------------------------
    private fun tapXPermanentsChoice(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        val sourceName = activation.sourceName
        val tapXCost = activation.effectiveCost.extractTapXPermanentsCost()
        val alreadyTapping = (action.costPayment?.tappedPermanents?.isNotEmpty() == true)
        if (tapXCost == null || action.xValue != null || alreadyTapping) return null
        val tapTargets = costHandler.findUntappedMatchingPermanentsUnified(state, action.playerId, tapXCost.filter)
        val maxX = tapTargets.size
        val continuation = ActivateAbilityChooseXContinuation(
            action = action,
            tapTargets = tapTargets
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseNumberDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = "Choose X for ${sourceName} (0-$maxX)",
                    context = castingContext(action, sourceName),
                    minValue = 0,
                    maxValue = maxX
                )
            },
            answer = continuation
        )
    }

    // -------------------------------------------------------------------
    // {X} *mana* cost pause (legal-actions submission path).
    //
    // When the cost contains `{X}` mana (Wizard's Rockets: "{X}, {T}, Sacrifice this artifact:
    // Add X mana...") the frontend submits the bare `ActivateAbility` with no xValue, expecting
    // the engine to ask which X to pay. Without this the handler defaults X to 0
    // (`action.xValue ?: 0`), pays nothing, and the ability produces no mana — the player never
    // gets to choose X. The engine-direct path (xValue pre-filled) skips this.
    // -------------------------------------------------------------------
    private fun manaXChoice(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        val sourceName = activation.sourceName
        val effectiveCost = activation.effectiveCost
        val tapXCost = effectiveCost.extractTapXPermanentsCost()
        val manaXCost = effectiveCost.extractManaCost()
        val counterMaxX = PlayerCounterPayment.abilityMaxX(state, action.playerId, effectiveCost)
        if (!((manaXCost?.hasX == true || effectiveCost == AbilityCost.LoyaltyX || counterMaxX != null) && action.xValue == null && tapXCost == null)) {
            return null
        }
        val fixedMana = manaXCost?.cmc ?: 0 // the non-X portion ({X} alone is 0; {1}{X} is 1)
        val manaMaxX = if (manaXCost?.hasX == true) {
            (manaSolver.getAvailableManaCount(state, action.playerId) - fixedMana).coerceAtLeast(0) /
                manaXCost.xCount.coerceAtLeast(1)
        } else null
        val loyaltyMaxX = if (effectiveCost == AbilityCost.LoyaltyX) {
            activation.container.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
        } else null
        val maxX = listOfNotNull(manaMaxX, loyaltyMaxX, counterMaxX).minOrNull() ?: 0
        // "X can't be 0" abilities (Gogo, Master of Mimicry) set a minimum; clamp it to what the
        // player can actually pay so the decision bounds stay valid.
        val minX = activation.ability.minimumXValue.coerceAtMost(maxX)
        val continuation = ActivateAbilityChooseManaXContinuation(
            action = action
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseNumberDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = "Choose X for ${sourceName} ($minX-$maxX)",
                    context = castingContext(action, sourceName),
                    minValue = minX,
                    maxValue = maxX
                )
            },
            answer = continuation
        )
    }

    // -------------------------------------------------------------------
    // ExileXFromGraveyard pause (legal-actions submission path).
    //
    // "Exile X cards from your graveyard" needs exactly one decision, because X *is* the size
    // of the graveyard selection: pick the cards, and X is how many you picked. So there is no
    // number picker — the engine pauses for the cards and derives X from the count.
    //
    // Winter, Cursed Rider ("{2}{U}{B}, {T}, Exile X artifact cards from your graveyard: Each
    // other nonartifact creature gets -X/-X") has no `{X}` mana at all, so without this block
    // the handler falls through to `action.xValue ?: 0` — paying nothing and resolving a no-op.
    // Necropolis Fiend ("{X}, {T}, Exile X cards from your graveyard") pays X in mana too, so
    // the mana-X pause above has already bound `xValue`; here the selection is then pinned to
    // exactly that many rather than being free.
    //
    // Skipped when `exiledCards` is pre-filled (engine-direct path / resumed replay) or when
    // there is no real choice — X == candidates, which CostHandler pays without a prompt.
    // -------------------------------------------------------------------
    private fun exileXFromGraveyardChoice(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        val sourceName = activation.sourceName
        val tapXCost = activation.effectiveCost.extractTapXPermanentsCost()
        // Settled already when cards are pre-filled (engine-direct path, or the resume after this
        // very pause) or when X is a bound zero — a zero selection is a legal answer, and
        // re-pausing on it would spin forever.
        val exileXCost = activation.effectiveCost.extractExileXFromGraveyardCost()
        val exileXSettled = (action.costPayment?.exiledCards?.isNotEmpty() == true) || action.xValue == 0
        if (exileXCost == null || exileXSettled || tapXCost != null) return null
        val exileXCandidates = costHandler.findMatchingCardsUnified(
            state,
            state.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD)),
            exileXCost.filter,
            action.playerId
        )
        // A mana `{X}` already fixed the count; otherwise the player is free to exile any
        // number of matching cards (including none) and that count becomes X.
        val fixedCount = action.xValue
        val minSelections = fixedCount ?: 0
        val maxSelections = fixedCount ?: exileXCandidates.size
        val isRealChoice = exileXCandidates.size > minSelections
        // Not a real choice, so no prompt: either the graveyard has nothing matching (X = 0,
        // legal) or a mana-fixed X consumes every candidate, which CostHandler pays as-is.
        if (!isRealChoice) return null
        val prompt = if (fixedCount != null) {
            "Select $fixedCount card${if (fixedCount > 1) "s" else ""} to exile from " +
                "graveyard for ${sourceName}"
        } else {
            "Select any number of cards to exile from graveyard for ${sourceName} " +
                "(X is the number you choose)"
        }
        val continuation = ActivateAbilityExileXFromGraveyardContinuation(
            action = action,
            exileCandidates = exileXCandidates,
            fixedCount = fixedCount
        )
        return state.suspendForDecision(
            question = { decisionId ->
                SelectCardsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = castingContext(action, sourceName),
                    options = exileXCandidates,
                    minSelections = minSelections,
                    maxSelections = maxSelections
                )
            },
            answer = continuation
        )
    }

    // -------------------------------------------------------------------
    // ExileFromGraveyard cost-choice pause (legal-actions submission path).
    //
    // When the cost is `ExileFromGraveyard(count, filter)` (Rust Harvester:
    // "{2}, {T}, Exile an artifact card from your graveyard: ...") and the
    // player has more matching graveyard cards than the count, this is a
    // real choice — the engine must pause and ask which card(s) to exile,
    // not silently take the first N (CostHandler.exileCardsFromGraveyard
    // used to auto-pick when `exileChoices` was empty, dropping the
    // player's choice on the floor).
    //
    // Skipped when `exiledCards` is already pre-filled (engine-direct path
    // and resumed-replay case) or when candidates <= count (no real
    // choice).
    // -------------------------------------------------------------------
    private fun exileFromGraveyardChoice(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        val sourceName = activation.sourceName
        val exileFromGraveyardCost = activation.effectiveCost.extractExileFromGraveyardCost()
        val alreadyExiling = (action.costPayment?.exiledCards?.isNotEmpty() == true)
        if (exileFromGraveyardCost == null || alreadyExiling) return null
        // The pool follows the atom's own flags, not "the activator's graveyard": Night Soil
        // exiles "two creature cards from a single graveyard", so every player's graveyard is
        // in the pool, and a graveyard holding fewer than `count` matches is dropped because
        // it can't legally supply the whole payment on its own.
        val exileCandidatesByOwner = costHandler
            .exileCandidatesByOwner(state, exileFromGraveyardCost, action.playerId, action.sourceId)
            .values
        val exileCandidates =
            if (exileFromGraveyardCost.singleZone) {
                exileCandidatesByOwner.filter { it.size >= exileFromGraveyardCost.count }.flatten()
            } else {
                exileCandidatesByOwner.flatten()
            }
        if (exileCandidates.size <= exileFromGraveyardCost.count) return null
        val prompt = "Select ${exileFromGraveyardCost.count} card${if (exileFromGraveyardCost.count > 1) "s" else ""} to exile from graveyard for ${sourceName}"
        val continuation = ActivateAbilityExileFromGraveyardContinuation(
            action = action,
            exileCandidates = exileCandidates,
            exileCount = exileFromGraveyardCost.count
        )
        return state.suspendForDecision(
            question = { decisionId ->
                SelectCardsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = castingContext(action, sourceName),
                    options = exileCandidates,
                    minSelections = exileFromGraveyardCost.count,
                    maxSelections = exileFromGraveyardCost.count
                )
            },
            answer = continuation
        )
    }

    // -------------------------------------------------------------------
    // Sacrifice cost-choice pause (legal-actions submission path).
    //
    // When the cost is `Sacrifice(filter, count, excludeSelf)` (Sage of
    // Lat-Nam: "{T}, Sacrifice an artifact: Draw a card", Atog, Ashnod's
    // Altar, …) and the player controls more matching permanents than the
    // count, this is a real choice — the engine must pause and ask which
    // permanent(s) to sacrifice, not fail with "Not enough sacrifice
    // targets chosen" (an AI submitting a bare ActivateAbility with no
    // sacrifice chosen would otherwise spin forever).
    //
    // Skipped when `sacrificedPermanents` is already pre-filled
    // (engine-direct path and resumed-replay case) or when candidates <=
    // count (no real choice — Part 2 / CostHandler auto-picks). Mirrors the
    // ExileFromGraveyard pause block above.
    // -------------------------------------------------------------------
    private fun sacrificeChoice(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        val sourceName = activation.sourceName
        val sacrificeCost = activation.effectiveCost.extractSacrificeCost()
        val alreadySacrificing = (action.costPayment?.sacrificedPermanents?.isNotEmpty() == true)
        if (sacrificeCost == null || alreadySacrificing) return null
        val sacrificeCandidates = costHandler
            .findMatchingCardsUnified(
                state, state.getBattlefield(action.playerId), sacrificeCost.filter, action.playerId,
                // Source-relative filters ("an Equipment attached to this creature") need the
                // ability's own source to resolve; without it they match nothing.
                sourceId = action.sourceId,
            )
            .let { if (sacrificeCost.excludeSelf) it.filter { id -> id != action.sourceId } else it }
        // Normally we only pause when there's a real choice (candidates > count); the forced
        // case auto-picks. But "with different names" is always a real choice — the player must
        // pick a distinctly-named set even when candidates == count — so always pause for it.
        if (!(sacrificeCandidates.size > sacrificeCost.count || sacrificeCost.distinctNames)) return null
        val prompt = "Select ${sacrificeCost.count} permanent${if (sacrificeCost.count > 1) "s" else ""} to sacrifice for ${sourceName}"
        val continuation = ActivateAbilitySacrificeContinuation(
            action = action,
            sacrificeCandidates = sacrificeCandidates,
            sacrificeCount = sacrificeCost.count,
            distinctNames = sacrificeCost.distinctNames
        )
        return state.suspendForDecision(
            question = { decisionId ->
                SelectCardsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = castingContext(action, sourceName),
                    options = sacrificeCandidates,
                    minSelections = sacrificeCost.count,
                    maxSelections = sacrificeCost.count
                )
            },
            answer = continuation
        )
    }

    // -------------------------------------------------------------------
    // Put-from-hand-on-library cost-choice pause (Leashling). Which card goes back is always the
    // player's choice — it decides their next draw — so pause whenever the choice isn't pre-filled
    // and there is more than one way to pay. An exactly-sized hand is forced and CostHandler pays it
    // without a prompt.
    // -------------------------------------------------------------------
    private fun putOnLibraryChoice(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        val putOnLibraryCost = activation.effectiveCost.extractPutOnLibraryCost() ?: return null
        if (!action.costPayment?.cardsPutOnLibrary.isNullOrEmpty()) return null
        val candidates = costHandler.findMatchingCardsUnified(
            state,
            state.getZone(com.wingedsheep.engine.state.ZoneKey(action.playerId, Zone.HAND)),
            putOnLibraryCost.filter,
            action.playerId
        )
        if (candidates.size < putOnLibraryCost.count) {
            return ExecutionResult.error(state, "Not enough cards in hand to pay ${putOnLibraryCost.description}")
        }
        if (candidates.size == putOnLibraryCost.count) return null
        val n = putOnLibraryCost.count
        val prompt = "Choose ${if (n == 1) "a card" else "$n cards"} to put on top of your library for ${activation.sourceName}"
        return state.suspendForDecision(
            question = { decisionId ->
                SelectCardsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = castingContext(action, activation.sourceName),
                    options = candidates,
                    minSelections = n,
                    maxSelections = n
                )
            },
            answer = com.wingedsheep.engine.core.ActivateAbilityPutOnLibraryContinuation(
                action = action,
                candidates = candidates,
                count = n
            )
        )
    }

    // -------------------------------------------------------------------
    // VariablePermanents cost-choice pause (legal-actions submission path).
    //
    // "Exile/sacrifice one or more [filter] you control" (Fabrication Foundry, Radiant Lotus).
    // The player picks which permanents to pay with — a variable-count choice (at least
    // minCount). The bare ActivateAbility arrives with no selection; pause and raise a
    // SelectCardsDecision over the eligible permanents. The resumer fills the selection and
    // re-enters, which computes X from it and — for an ability whose target wasn't gathered up
    // front — pauses again for that target (block below).
    //
    // Skipped when variableCostPermanents is already filled (engine-direct path / resumed replay).
    // -------------------------------------------------------------------
    private fun variablePermanentsChoice(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        val sourceName = activation.sourceName
        val variablePermanentsCost = activation.variablePermanentsCost
        if (variablePermanentsCost == null || activation.chosenForCost.isNotEmpty()) return null
        val verb = VariablePermanentsCost.verb(variablePermanentsCost.action)
        val candidates = costHandler
            .findMatchingCardsUnified(
                state, state.getBattlefield(action.playerId), variablePermanentsCost.filter, action.playerId,
                // Same source-relative resolution as the sacrifice pause above, so the choices
                // offered here are exactly the ones payment will accept.
                sourceId = action.sourceId,
            )
            .let { if (variablePermanentsCost.excludeSelf) it.filter { id -> id != action.sourceId } else it }
        val minCount = variablePermanentsCost.minCount
        if (candidates.size < minCount) {
            return ExecutionResult.error(state, "Not enough permanents to $verb for ${sourceName}")
        }
        val prompt = "Choose one or more ${variablePermanentsCost.filter.description}s to $verb for ${sourceName}"
        val continuation = ActivateAbilityVariablePermanentsContinuation(
            action = action,
            candidates = candidates,
            minCount = minCount
        )
        return state.suspendForDecision(
            question = { decisionId ->
                SelectCardsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = castingContext(action, sourceName),
                    options = candidates,
                    minSelections = minCount,
                    maxSelections = candidates.size
                )
            },
            answer = continuation
        )
    }

    // -------------------------------------------------------------------
    // Target pause for a VariablePermanents ability (Fabrication Foundry, Radiant Lotus).
    //
    // The enumerator deliberately surfaces these abilities with no target gathered up front,
    // because a target may be bounded by X ("mana value X or less") and X isn't known until the
    // cost choice is made. Once that selection is known (block above resumed → X computed),
    // raise the controller's target choice with X threaded through the predicate context, so an
    // over-X target can't be picked and then fizzle. The resumer fills action.targets and
    // re-enters to pay + resolve. Skipped on the engine-direct path (targets already supplied)
    // and for abilities with no controller target.
    // -------------------------------------------------------------------
    private fun variablePermanentsTargetChoice(state: GameState, activation: Activation): ExecutionResult? {
        val action = activation.action
        val sourceName = activation.sourceName
        if (activation.variablePermanentsCost == null || activation.chosenForCost.isEmpty() || action.targets.isNotEmpty()) {
            return null
        }
        val controllerTargetReqsExec = activation.targetRequirements.filter { it.chooser == TargetChooser.Controller }
        if (controllerTargetReqsExec.none { it.effectiveMinCount > 0 }) return null
        val xForTargets = activation.effectiveXValue ?: 0
        val finder = targetFinder
        val pipelineContext = PredicateContext(
            controllerId = action.playerId,
            sourceId = action.sourceId,
            xValue = xForTargets
        )
        val legalTargets = mutableMapOf<Int, List<EntityId>>()
        val requirementInfos = controllerTargetReqsExec.mapIndexed { index, req ->
            val legal = finder.findLegalTargets(
                state, req, action.playerId, action.sourceId,
                targetingSourceType = TargetingSourceType.ACTIVATED_ABILITY, pipelineContext = pipelineContext
            )
            if (legal.isEmpty() && req.effectiveMinCount > 0) {
                return ExecutionResult.error(state, "No legal target for ${sourceName}")
            }
            legalTargets[index] = legal
            TargetRequirementInfo(
                index = index,
                description = req.description,
                mustDifferFromEarlier = req is com.wingedsheep.sdk.scripting.targets.TargetOther,
                minTargets = req.effectiveMinCount,
                maxTargets = req.count
            )
        }
        val prompt = "Choose ${controllerTargetReqsExec.joinToString(" and ") { it.description }} for ${sourceName}"
        val continuation = ActivateAbilityControllerTargetContinuation(
            action = action,
            requirements = controllerTargetReqsExec
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseTargetsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = castingContext(action, sourceName),
                    targetRequirements = requirementInfos,
                    legalTargets = legalTargets
                )
            },
            answer = continuation
        )
    }

    /**
     * Raise a [ChooseTargetsDecision] routed to an opponent for an
     * activated ability's "… of an opponent's choice" target requirement(s) (Cuombajj Witches),
     * and push the continuation that resumes the activation once the opponent has chosen.
     *
     * Legal targets are computed relative to [action].playerId (the ability's controller), so
     * hexproof/protection/shroud are measured against the controller — exactly the printed ruling
     * ("an opponent can't target a creature they control with hexproof"). The pause happens before
     * any cost is paid; cancellation simply pops the frame.
     */
    private fun pauseForOpponentChosenTargets(
        state: GameState,
        action: ActivateAbility,
        sourceName: String,
        fullTargetReqs: List<TargetRequirement>,
        opponentReqs: List<TargetRequirement>
    ): ExecutionResult {
        // The resumer interleaves the controller's and opponent's targets back into one list by
        // consuming exactly `count` targets per requirement (the positional model
        // EffectContext.buildNamedTargets uses on resolution). That holds only for fixed-count
        // requirements; an optional/variable/unlimited one would misalign the cursors. Cuombajj is
        // the only printed use and is fixed-count — reject the unsupported shape here, before
        // bothering an opponent with a decision, rather than after the pick on the resume path.
        if (fullTargetReqs.any { it.minCount != it.count || it.optional || it.unlimited }) {
            return ExecutionResult.error(
                state,
                "Opponent-chosen targets are only supported with fixed-count requirements"
            )
        }

        val opponentIds = state.getOpponents(action.playerId)
        if (opponentIds.isEmpty()) {
            return ExecutionResult.error(state, "No opponent available to choose a target")
        }
        if (opponentIds.size > 1) {
            return pauseForOpponentTargetChooser(
                state, action, sourceName, fullTargetReqs, opponentReqs, opponentIds
            )
        }

        return pauseForOpponentChosenTargetsForDecider(
            state = state,
            action = action,
            sourceName = sourceName,
            fullTargetReqs = fullTargetReqs,
            opponentReqs = opponentReqs,
            deciderId = opponentIds.single()
        )
    }

    private fun pauseForOpponentTargetChooser(
        state: GameState,
        action: ActivateAbility,
        sourceName: String,
        fullTargetReqs: List<TargetRequirement>,
        opponentReqs: List<TargetRequirement>,
        opponentIds: List<EntityId>
    ): ExecutionResult {
        val opponentNames = opponentIds.map { opponentId ->
            state.getEntity(opponentId)
                ?.get<PlayerComponent>()?.name
                ?: "Player ${opponentId.value}"
        }
        val prompt = "Choose an opponent to choose a target for $sourceName"
        val continuation = ActivateAbilityOpponentChooserContinuation(
            action = action,
            sourceName = sourceName,
            opponentRequirements = opponentReqs,
            fullRequirements = fullTargetReqs,
            opponentIds = opponentIds
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseOptionDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = castingContext(action, sourceName),
                    options = opponentNames
                )
            },
            answer = continuation
        )
    }

    fun pauseForOpponentChosenTargetsForDecider(
        state: GameState,
        action: ActivateAbility,
        sourceName: String,
        fullTargetReqs: List<TargetRequirement>,
        opponentReqs: List<TargetRequirement>,
        deciderId: EntityId
    ): ExecutionResult {
        if (!state.getOpponents(action.playerId).contains(deciderId)) {
            return ExecutionResult.error(state, "Chosen player is not an opponent")
        }

        val finder = targetFinder
        val legalTargets = mutableMapOf<Int, List<EntityId>>()
        val requirementInfos = opponentReqs.mapIndexed { index, req ->
            val legal = finder.findLegalTargets(state, req, action.playerId, action.sourceId, targetingSourceType = TargetingSourceType.ACTIVATED_ABILITY)
            if (legal.isEmpty() && req.effectiveMinCount > 0) {
                // A required target with no legal choice means the ability can't be activated
                // (the enumerator gates on this; guard the engine-direct path too).
                return ExecutionResult.error(state, "No legal target for opponent's choice")
            }
            legalTargets[index] = legal
            TargetRequirementInfo(
                index = index,
                description = req.description,
                mustDifferFromEarlier = req is com.wingedsheep.sdk.scripting.targets.TargetOther,
                minTargets = req.effectiveMinCount,
                maxTargets = req.count
            )
        }

        // The prompt is shown to the opponent who is making the choice, so the "of an opponent's
        // choice" suffix the requirement description carries is redundant noise here — strip it.
        val prompt = "Choose ${opponentReqs.joinToString(" and ") {
            it.description.removeSuffix(" of an opponent's choice")
        }} for $sourceName"
        val continuation = ActivateAbilityOpponentTargetContinuation(
            action = action,
            opponentRequirements = opponentReqs,
            fullRequirements = fullTargetReqs,
            deciderId = deciderId
        )
        return state.suspendForDecision(
            question = { decisionId ->
                ChooseTargetsDecision(
                    id = decisionId,
                    playerId = deciderId,
                    prompt = prompt,
                    context = castingContext(action, sourceName),
                    targetRequirements = requirementInfos,
                    legalTargets = legalTargets
                )
            },
            answer = continuation
        )
    }

    private fun castingContext(action: ActivateAbility, sourceName: String) = DecisionContext(
        sourceId = action.sourceId,
        sourceName = sourceName,
        phase = DecisionPhase.CASTING
    )
}
