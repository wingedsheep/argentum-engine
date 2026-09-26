package com.wingedsheep.engine.handlers.actions.decision

import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.BatchYesNoResponse
import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.core.CancelDecisionResponse
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ReplacementChosenResponse
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.CombatResolutionResponse
import com.wingedsheep.engine.core.DamageEdge
import com.wingedsheep.engine.core.DamageEdgeDirection
import com.wingedsheep.engine.core.DamageAssignmentResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.ModesChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.PilesSplitResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Validators for different types of decision responses.
 *
 * Each decision type requires specific validation to ensure
 * the player's response is legal and complete.
 */
object DecisionValidators {

    /**
     * Validate a decision response against its pending decision.
     *
     * @param decision The pending decision requiring a response
     * @param response The player's response
     * @return An error message if invalid, null if valid
     */
    fun validate(decision: PendingDecision, response: DecisionResponse, state: GameState? = null): String? {
        return when (decision) {
            is ChooseTargetsDecision -> validateTargets(decision, response, state)
            is SelectCardsDecision -> validateSelectCards(decision, response, state)
            is YesNoDecision -> validateYesNo(response)
            is ChooseModeDecision -> validateModes(decision, response)
            is ChooseColorDecision -> validateColor(decision, response)
            is ChooseNumberDecision -> validateNumber(decision, response)
            is DistributeDecision -> validateDistribute(decision, response)
            is OrderObjectsDecision -> validateOrder(decision, response)
            is SplitPilesDecision -> validateSplitPiles(decision, response)
            is ChooseOptionDecision -> validateOption(decision, response)
            is ChooseReplacementDecision -> validateReplacement(decision, response)
            is BudgetModalDecision -> validateBudgetModal(decision, response)
            is AssignDamageDecision -> validateDamageAssignment(decision, response)
            is CombatResolutionDecision -> validateCombatResolution(decision, response)
            is SearchLibraryDecision -> validateLibrarySearch(decision, response)
            is ReorderLibraryDecision -> validateLibraryReorder(decision, response)
            is SelectManaSourcesDecision -> validateManaSourcesSelection(response)
            is BatchYesNoDecision -> validateBatchYesNo(response)
        }
    }

    /**
     * Validate a [CombatResolutionResponse] against its [CombatResolutionDecision].
     *
     * Geometric checks only (the resumer enforces per-edge ownership by filtering to the current
     * chooser's [DamageEdge.editableBy], so this stays submitter-agnostic):
     * - Each submitted amount lies in `[0, maximum]` for its edge.
     * - Per source, the total assigned across its edges doesn't exceed its power.
     * - CR 510.1c damage-assignment order: the attacking player picks the order, so an assignment is
     *   legal iff it's legal under *some* order — i.e. for an [DamageEdge.orderConstrained] source,
     *   at most one blocker it damages may be left below lethal (counting cross-source damage this
     *   step, which is what lets a band cooperate). Banding edges (orderConstrained = false) don't gate.
     * - CR 702.19b trample lethal-first: a trample drain may carry damage only once every blocker
     *   the trampling attacker assigns to is at lethal (aggregated across the step). Independent of
     *   CR 510.1c order, so banding never relaxes it.
     */
    private fun validateCombatResolution(
        decision: CombatResolutionDecision,
        response: DecisionResponse,
    ): String? {
        if (response !is CombatResolutionResponse) {
            return "Expected combat resolution response"
        }

        val edgesById = decision.edges.associateBy { it.id }
        val submitted = response.edges.associateBy { it.edgeId }

        for ((edgeId, entry) in submitted) {
            val edge = edgesById[edgeId] ?: return "Unknown edge id: $edgeId"
            if (entry.amount < 0) return "Edge $edgeId: amount ${entry.amount} below 0"
            if (entry.amount > edge.maximum) return "Edge $edgeId: amount ${entry.amount} exceeds maximum ${edge.maximum}"
        }

        fun amountOf(edge: DamageEdge): Int = submitted[edge.id]?.amount ?: edge.amount

        // Per-source budget — every edge from a source caps at that source's power (its `maximum`).
        val edgesBySource = decision.edges.groupBy { it.sourceId }
        for ((sourceId, sourceEdges) in edgesBySource) {
            val total = sourceEdges.sumOf { amountOf(it) }
            val power = sourceEdges.maxOf { it.maximum }
            if (total > power) return "Source $sourceId: damage total $total exceeds available power $power"
        }

        // Aggregate damage reaching each target this step (CR 510.1c cross-source lethal counting).
        val aggregate = mutableMapOf<EntityId, Int>()
        for (edge in decision.edges) {
            if (edge.isTrampleDrain) continue
            aggregate.merge(edge.targetId, amountOf(edge), Int::plus)
        }

        // CR 510.1c: the attacking player chooses the damage-assignment order, so there is no fixed
        // order to gate against — an assignment is legal iff it's legal under *some* order. Walking
        // the chosen order, every blocker a source damages except the last (where it runs out) must
        // be at lethal; blockers it skips are unconstrained. Equivalently: at most one blocker a
        // source assigns damage to may be left below its lethal need (counting cross-source damage
        // this step). Banding (orderConstrained = false) lifts even this — the chooser divides freely.
        for ((sourceId, sourceEdges) in edgesBySource) {
            val belowLethal = sourceEdges.count { edge ->
                edge.orderConstrained && !edge.isTrampleDrain &&
                    amountOf(edge) > 0 && (aggregate[edge.targetId] ?: 0) < edge.lethal
            }
            if (belowLethal > 1) {
                return "Source $sourceId: must assign lethal damage to all but one blocker before " +
                    "spreading damage further"
            }
        }

        // CR 702.19b trample lethal-first.
        val damageToBlocker = mutableMapOf<EntityId, Int>()
        for (edge in decision.edges) {
            if (edge.direction != DamageEdgeDirection.ATTACKER_TO_BLOCKER) continue
            damageToBlocker.merge(edge.targetId, amountOf(edge), Int::plus)
        }
        for (drain in decision.edges) {
            if (!drain.isTrampleDrain) continue
            if (amountOf(drain) <= 0) continue
            for (blockerEdge in decision.edges) {
                if (blockerEdge.sourceId != drain.sourceId) continue
                if (blockerEdge.direction != DamageEdgeDirection.ATTACKER_TO_BLOCKER) continue
                if ((damageToBlocker[blockerEdge.targetId] ?: 0) < blockerEdge.lethal) {
                    return "Trample drain ${drain.id}: preceding blocker not at lethal"
                }
            }
        }

        return null
    }

    private fun validateTargets(
        decision: ChooseTargetsDecision,
        response: DecisionResponse,
        state: GameState? = null
    ): String? {
        if (response is CancelDecisionResponse) {
            return if (decision.canCancel) null else "This decision cannot be cancelled"
        }
        if (response !is TargetsResponse) {
            return "Expected target selection response"
        }

        // Legality and per-requirement duplicates are checked against what was actually submitted.
        // A group keyed to a requirement the decision never declared is the mirror of an omitted
        // one: the counts below iterate the *declared* requirements, so an undeclared group is
        // never reached by them and an empty one would slip through unexamined.
        for ((reqIndex, selectedIds) in response.selectedTargets) {
            if (decision.targetRequirements.none { it.index == reqIndex }) {
                return "Unknown target requirement $reqIndex"
            }
            val legalForReq = decision.legalTargets[reqIndex] ?: emptyList()
            for (id in selectedIds) {
                if (id !in legalForReq) {
                    return "Invalid target: $id is not a legal choice for requirement $reqIndex"
                }
            }

            // The same object/player can't be chosen more than once for a single targeting
            // requirement (CR 601.2c — "two target creatures" needs two different creatures).
            if (selectedIds.size != selectedIds.toSet().size) {
                return "The same target can't be chosen more than once for requirement $reqIndex"
            }

            // Separate requirements are separate "target" words, so they may share a pick (Seeds
            // of Strength) — except an "another target" requirement, which must avoid every
            // earlier requirement's picks.
            if (decision.targetRequirements.first { it.index == reqIndex }.mustDifferFromEarlier) {
                val earlier = response.selectedTargets
                    .filterKeys { it < reqIndex }
                    .values.flatten().toSet()
                if (selectedIds.any { it in earlier }) {
                    return "Target for requirement $reqIndex must differ from the other chosen targets"
                }
            }
        }

        // Every *declared* requirement is then checked against the group that answered it, so the
        // counts and the aggregate restrictions are driven by the decision rather than by whatever
        // the response happened to contain. An omitted group is an empty selection for its
        // requirement, not a way to bypass its minimum — which is what lets an optional group
        // (`minTargets == 0`) stay absent while a mandatory one can't.
        for (req in decision.targetRequirements) {
            val selectedIds = response.selectedTargets[req.index] ?: emptyList()
            val reqIndex = req.index
            if (selectedIds.size < req.minTargets) {
                return "Not enough targets for requirement $reqIndex: need at least ${req.minTargets}"
            }
            if (selectedIds.size > req.maxTargets) {
                return "Too many targets for requirement $reqIndex: maximum is ${req.maxTargets}"
            }
            // "... from a single graveyard" — every chosen card target must share an owner.
            if (req.sameOwner && selectedIds.size > 1 && state != null) {
                val owners = selectedIds.mapNotNull { id ->
                    state.getEntity(id)?.get<OwnerComponent>()?.playerId
                }
                if (owners.toSet().size > 1) {
                    return "Targets for requirement $reqIndex must be from a single graveyard"
                }
            }
            // "... with total mana value N or less" — the summed mana value of the chosen card
            // targets may not exceed the resolved cap (Fire Lord Sozin's "total mana value X or
            // less"; the cap was baked to a concrete int at decision-build time). CR 601.2c.
            val manaCap = req.totalManaValueAtMost
            if (manaCap != null && state != null) {
                val totalManaValue = selectedIds.sumOf { id ->
                    state.getEntity(id)?.get<CardComponent>()?.manaValue ?: 0
                }
                if (totalManaValue > manaCap) {
                    return "Targets for requirement $reqIndex exceed total mana value $manaCap"
                }
            }
            // "... with different names" — no two chosen targets may share a name (Behold the
            // Sinister Six!). TargetValidator is authoritative; this rejects it interactively too.
            if (req.differentNames && selectedIds.size > 1 && state != null) {
                val names = selectedIds.map { id ->
                    state.projectedState.getName(id) ?: state.getEntity(id)?.get<CardComponent>()?.name
                }
                if (names.size != names.toSet().size) {
                    return "Targets for requirement $reqIndex must have different names"
                }
            }
            // "For each other player, ... up to one target creature that player controls" — no
            // two chosen targets may share a controller (Kaya, Spirits' Justice). TargetValidator
            // is authoritative; this rejects it interactively too.
            if (req.differentControllers && selectedIds.size > 1 && state != null) {
                val controllers = selectedIds.map { id ->
                    state.projectedState.getController(id)
                        ?: state.getEntity(id)?.get<ControllerComponent>()?.playerId
                }
                if (controllers.size != controllers.toSet().size) {
                    return "Targets for requirement $reqIndex must be controlled by different players"
                }
            }
            // "Up to one target ... of each card type" (Uldaros Theorix). TargetValidator is
            // authoritative; this rejects it interactively too.
            if (req.onePerCardType && selectedIds.size > 1 && state != null &&
                !com.wingedsheep.engine.mechanics.targeting.OnePerCardType.isSatisfied(state, selectedIds)
            ) {
                return "Targets for requirement $reqIndex must be at most one of each card type"
            }
        }
        return null
    }

    private fun validateSelectCards(
        decision: SelectCardsDecision,
        response: DecisionResponse,
        state: GameState?
    ): String? {
        if (response !is CardsSelectedResponse) {
            return "Expected card selection response"
        }

        for (cardId in response.selectedCards) {
            if (cardId !in decision.options) {
                return "Invalid selection: $cardId is not a valid option"
            }
        }
        if (response.selectedCards.size < decision.minSelections) {
            return "Not enough cards selected: need at least ${decision.minSelections}"
        }
        if (response.selectedCards.size > decision.maxSelections) {
            return "Too many cards selected: maximum is ${decision.maxSelections}"
        }
        // "... with total mana value N or greater" — collect evidence N (CR 701.59a). Unlike the
        // `maxTotalManaValue` cap, an under-total selection can't be trimmed into legality, so it
        // is rejected outright: there is no correct way to complete an insufficient payment, and
        // CR 701.59b means the player was only offered this decision because a legal one exists.
        // An *empty* selection is governed by `minSelections` alone, not by the floor: where the
        // collection is optional (a ward cost the player may simply not pay) the decision is raised
        // with `minSelections = 0` and selecting nothing is how you decline. Enforcing the floor
        // there would make declining impossible.
        val manaFloor = decision.minTotalManaValue?.takeIf { response.selectedCards.isNotEmpty() }
        if (manaFloor != null && state != null) {
            val totalManaValue = response.selectedCards.distinct().sumOf { id ->
                state.getEntity(id)?.get<CardComponent>()?.manaValue ?: 0
            }
            if (totalManaValue < manaFloor) {
                return "Selected cards total mana value $totalManaValue, need at least $manaFloor"
            }
        }
        val unmetConditionalMinimums = decision.conditionalMinimums.filter {
            response.selectedCards.size < it.requiredSelections
        }
        if (unmetConditionalMinimums.isNotEmpty()) {
            val satisfiesAlternative = unmetConditionalMinimums.any { minimum ->
                val matchingCount = response.selectedCards.count { it in minimum.matchingOptions }
                response.selectedCards.size >= minimum.minimumSelections && matchingCount >= minimum.requiredMatches
            }
            if (!satisfiesAlternative) {
                return unmetConditionalMinimums.first().description ?: "Selection does not satisfy the conditional minimum"
            }
        }
        return null
    }

    private fun validateYesNo(response: DecisionResponse): String? {
        if (response !is YesNoResponse) {
            return "Expected yes/no response"
        }
        return null
    }

    private fun validateBatchYesNo(response: DecisionResponse): String? {
        if (response !is BatchYesNoResponse) {
            return "Expected batch yes/no response"
        }
        return null
    }

    private fun validateModes(decision: ChooseModeDecision, response: DecisionResponse): String? {
        if (response !is ModesChosenResponse) {
            return "Expected mode selection response"
        }

        for (modeIndex in response.selectedModes) {
            val mode = decision.modes.find { it.index == modeIndex }
            if (mode == null) {
                return "Invalid mode index: $modeIndex"
            }
            if (!mode.available) {
                return "Mode $modeIndex is not available"
            }
        }
        if (response.selectedModes.size < decision.minModes) {
            return "Not enough modes selected: need at least ${decision.minModes}"
        }
        if (response.selectedModes.size > decision.maxModes) {
            return "Too many modes selected: maximum is ${decision.maxModes}"
        }
        return null
    }

    private fun validateColor(decision: ChooseColorDecision, response: DecisionResponse): String? {
        if (response !is ColorChosenResponse) {
            return "Expected color choice response"
        }
        if (response.color !in decision.availableColors) {
            return "Invalid color: ${response.color} is not available"
        }
        if (response.colors.isNotEmpty()) {
            // Multi-color answer ("the color or colors of your choice"): a nonempty set of
            // distinct, offered colors, no larger than the decision allows, naming `color` too.
            if (response.colors.size > decision.maxColors) {
                return "Too many colors: at most ${decision.maxColors} may be chosen"
            }
            if (response.colors.toSet().size != response.colors.size) {
                return "Duplicate colors in response"
            }
            response.colors.firstOrNull { it !in decision.availableColors }?.let {
                return "Invalid color: $it is not available"
            }
            if (response.color !in response.colors) {
                return "The primary color must be one of the chosen colors"
            }
        }
        return null
    }

    private fun validateNumber(decision: ChooseNumberDecision, response: DecisionResponse): String? {
        if (response !is NumberChosenResponse) {
            return "Expected number choice response"
        }
        if (response.number < decision.minValue || response.number > decision.maxValue) {
            return "Invalid number: must be between ${decision.minValue} and ${decision.maxValue}"
        }
        return null
    }

    private fun validateDistribute(decision: DistributeDecision, response: DecisionResponse): String? {
        if (response !is DistributionResponse) {
            return "Expected distribution response"
        }

        val total = response.distribution.values.sum()
        if (decision.allowPartial) {
            if (total > decision.totalAmount) {
                return "Distribution must not exceed ${decision.totalAmount}, got $total"
            }
        } else {
            if (total != decision.totalAmount) {
                return "Distribution must total ${decision.totalAmount}, got $total"
            }
        }

        for ((targetId, amount) in response.distribution) {
            if (targetId !in decision.targets) {
                return "Invalid target for distribution: $targetId"
            }
            if (amount < decision.minPerTarget) {
                return "Each target must receive at least ${decision.minPerTarget}"
            }
            val maxForTarget = decision.maxPerTarget[targetId]
            if (maxForTarget != null && amount > maxForTarget) {
                return "Target $targetId cannot receive more than $maxForTarget"
            }
        }
        // A target left out of the map would receive nothing, so a minimum covers every target.
        if (decision.minPerTarget > 0 && decision.targets.any { it !in response.distribution }) {
            return "Each target must receive at least ${decision.minPerTarget}"
        }
        return null
    }

    private fun validateOrder(decision: OrderObjectsDecision, response: DecisionResponse): String? {
        if (response !is OrderedResponse) {
            return "Expected ordering response"
        }
        if (!isSameCollection(response.orderedObjects, decision.objects)) {
            return "Ordered objects must contain exactly the same objects as the decision"
        }
        return null
    }

    /**
     * True when [submitted] holds exactly the objects in [expected] — same count, same contents.
     *
     * Both conditions are load-bearing, and set equality alone is not enough: it discards
     * multiplicity, so a one-object ordering answered with `[first, first]` has the same set as
     * `[first]`. Comparing sizes as well makes an ordering response a permutation of the decision's
     * objects — every object, exactly once — while still allowing any legal reordering.
     */
    private fun isSameCollection(submitted: List<EntityId>, expected: List<EntityId>): Boolean =
        submitted.size == expected.size && submitted.toSet() == expected.toSet()

    private fun validateSplitPiles(decision: SplitPilesDecision, response: DecisionResponse): String? {
        if (response !is PilesSplitResponse) {
            return "Expected pile split response"
        }

        // Flattened rather than set-compared: a card can't be in two piles at once, so a split that
        // duplicates one card and drops another has the same set as a legal one (the multiplicity
        // hole [isSameCollection] closes for orderings).
        if (!isSameCollection(response.piles.flatten(), decision.cards)) {
            return "Piles must contain exactly the same cards as the decision"
        }
        if (response.piles.size != decision.numberOfPiles) {
            return "Must split into exactly ${decision.numberOfPiles} piles"
        }
        return null
    }

    private fun validateOption(decision: ChooseOptionDecision, response: DecisionResponse): String? {
        if (response is CancelDecisionResponse) {
            return if (decision.canCancel) null else "This decision cannot be cancelled"
        }
        if (response !is OptionChosenResponse) {
            return "Expected option choice response"
        }
        if (response.optionIndex < 0 || response.optionIndex >= decision.options.size) {
            return "Invalid option index: ${response.optionIndex}"
        }
        return null
    }

    private fun validateReplacement(decision: ChooseReplacementDecision, response: DecisionResponse): String? {
        if (response !is ReplacementChosenResponse) {
            return "Expected replacement choice response"
        }
        if (response.fromIndex < 0 || response.fromIndex >= decision.fromOptions.size) {
            return "Invalid from index: ${response.fromIndex}"
        }
        if (response.toIndex < 0 || response.toIndex >= decision.toOptions.size) {
            return "Invalid to index: ${response.toIndex}"
        }
        // If the FROM constrains its allowed TO set, enforce it (Crystal Spray: same category).
        val allowed = decision.allowedToByFrom.getOrNull(response.fromIndex)
        if (allowed != null && !allowed.contains(response.toIndex)) {
            return "Replacement ${decision.toOptions[response.toIndex]} not allowed for ${decision.fromOptions[response.fromIndex]}"
        }
        return null
    }

    private fun validateBudgetModal(decision: BudgetModalDecision, response: DecisionResponse): String? {
        if (response !is BudgetModalResponse) {
            return "Expected budget modal response"
        }
        for (idx in response.selectedModeIndices) {
            if (idx < 0 || idx >= decision.modes.size) {
                return "Invalid mode index: $idx"
            }
        }
        val totalCost = response.selectedModeIndices.sumOf { decision.modes[it].cost }
        if (totalCost > decision.budget) {
            return "Total cost ($totalCost) exceeds budget (${decision.budget})"
        }
        return null
    }

    private fun validateDamageAssignment(decision: AssignDamageDecision, response: DecisionResponse): String? {
        if (response !is DamageAssignmentResponse) {
            return "Expected damage assignment response"
        }

        val totalDamage = response.assignments.values.sum()
        if (totalDamage > decision.availablePower) {
            return "Total damage ($totalDamage) exceeds available power (${decision.availablePower})"
        }

        val validTargets = decision.orderedTargets.toSet() + listOfNotNull(decision.defenderId)
        for (targetId in response.assignments.keys) {
            if (targetId !in validTargets) {
                return "Invalid damage target: $targetId"
            }
        }

        // Validate damage assignment order
        var allPreviousHaveLethal = true
        for (blockerId in decision.orderedTargets) {
            val assignedDamage = response.assignments[blockerId] ?: 0
            val lethalRequired = decision.minimumAssignments[blockerId] ?: 0
            val hasLethal = assignedDamage >= lethalRequired

            if (!hasLethal && !allPreviousHaveLethal) {
                // Fine - can assign 0 to later blockers
            } else if (!hasLethal) {
                allPreviousHaveLethal = false
            }

            // Check no subsequent blocker has damage if this one doesn't have lethal
            if (!hasLethal) {
                val laterBlockers = decision.orderedTargets.dropWhile { it != blockerId }.drop(1)
                for (laterBlocker in laterBlockers) {
                    if ((response.assignments[laterBlocker] ?: 0) > 0) {
                        return "Cannot assign damage to later blocker until earlier blockers have lethal damage"
                    }
                }
            }
        }

        // Validate trample damage
        val damageToDefender = response.assignments[decision.defenderId] ?: 0
        if (damageToDefender > 0) {
            if (!decision.hasTrample) {
                return "Cannot assign damage to defending player without trample"
            }
            if (!allPreviousHaveLethal) {
                return "Cannot assign trample damage until all blockers have lethal damage"
            }
        }
        return null
    }

    private fun validateLibrarySearch(decision: SearchLibraryDecision, response: DecisionResponse): String? {
        if (response !is CardsSelectedResponse) {
            return "Expected card selection response for library search"
        }

        for (cardId in response.selectedCards) {
            if (cardId !in decision.options) {
                return "Invalid selection: $cardId is not a valid option"
            }
        }
        if (response.selectedCards.size > decision.maxSelections) {
            return "Too many cards selected: maximum is ${decision.maxSelections}"
        }
        return null
    }

    private fun validateLibraryReorder(decision: ReorderLibraryDecision, response: DecisionResponse): String? {
        if (response !is OrderedResponse) {
            return "Expected ordered response for library reorder"
        }

        if (!isSameCollection(response.orderedObjects, decision.cards)) {
            return "Invalid reorder: response must contain exactly the ${decision.cards.size} cards " +
                "of the decision, each once"
        }
        return null
    }

    private fun validateManaSourcesSelection(response: DecisionResponse): String? {
        if (response !is ManaSourcesSelectedResponse) {
            return "Expected mana sources selected response"
        }
        return null
    }
}
