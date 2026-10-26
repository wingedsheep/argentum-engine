package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.rollout.FastDecisionResponder
import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.CancelDecisionResponse
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DamageAssignmentResponse
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ManaSourceOption
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.ModeOption
import com.wingedsheep.engine.core.ModesChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TrivialDecisionsTest : FunSpec({

    val player = EntityId.of("player")
    val first = EntityId.of("first")
    val second = EntityId.of("second")
    val context = DecisionContext(phase = DecisionPhase.RESOLUTION)

    test("structurally forced decisions produce validator-approved responses") {
        val decisions = listOf(
            ChooseTargetsDecision(
                id = "targets",
                playerId = player,
                prompt = "Choose",
                context = context,
                targetRequirements = listOf(
                    TargetRequirementInfo(index = 0, description = "first target"),
                ),
                legalTargets = mapOf(0 to listOf(first)),
            ),
            SelectCardsDecision(
                id = "cards",
                playerId = player,
                prompt = "Choose",
                context = context,
                options = listOf(first),
                minSelections = 1,
                maxSelections = 1,
            ),
            ChooseOptionDecision(
                id = "option",
                playerId = player,
                prompt = "Choose",
                context = context,
                options = listOf("Only option"),
            ),
            ChooseColorDecision(
                id = "color",
                playerId = player,
                prompt = "Choose",
                context = context,
                availableColors = setOf(Color.BLUE),
            ),
            ChooseModeDecision(
                id = "mode",
                playerId = player,
                prompt = "Choose",
                context = context,
                modes = listOf(
                    ModeOption(index = 4, text = "Only available"),
                    ModeOption(index = 9, text = "Unavailable", available = false),
                ),
                minModes = 1,
                maxModes = 1,
            ),
            ChooseNumberDecision(
                id = "number",
                playerId = player,
                prompt = "Choose",
                context = context,
                minValue = 3,
                maxValue = 3,
            ),
            OrderObjectsDecision(
                id = "order",
                playerId = player,
                prompt = "Order",
                context = context,
                objects = listOf(first),
            ),
            ReorderLibraryDecision(
                id = "library-order",
                playerId = player,
                prompt = "Order",
                context = context,
                cards = listOf(first),
                cardInfo = mapOf(first to SearchCardInfo("Card", "", "Land")),
            ),
        )

        for (decision in decisions) {
            val response = TrivialDecisions.responseFor(decision)
            response.shouldNotBeNull()
            DecisionValidators.validate(decision, response, GameState()) shouldBe null
        }
    }

    test("an optional pick with nothing to pick from is forced to select nothing") {
        val reveal = SelectCardsDecision(
            id = "reveal",
            playerId = player,
            prompt = "You may reveal a Dragon card from your hand",
            context = context,
            options = emptyList(),
            minSelections = 0,
            maxSelections = 1,
        )
        val search = SearchLibraryDecision(
            id = "search",
            playerId = player,
            prompt = "Search for a basic land",
            context = context,
            options = emptyList(),
            minSelections = 0,
            maxSelections = 1,
            cards = emptyMap(),
            filterDescription = "basic land",
        )

        for (decision in listOf(reveal, search)) {
            val response = TrivialDecisions.responseFor(decision)
            response.shouldBeInstanceOf<CardsSelectedResponse>()
            response.selectedCards shouldBe emptyList()
            DecisionValidators.validate(decision, response, GameState()) shouldBe null
        }

        // One eligible card under an "up to one" is a real choice: take it or not.
        TrivialDecisions.responseFor(reveal.copy(options = listOf(first))).shouldBeNull()
        TrivialDecisions.responseFor(search.copy(options = listOf(first))).shouldBeNull()
    }

    test("a lone target is not forced when cancellation or declining is legal") {
        val cancellable = ChooseTargetsDecision(
            id = "target",
            playerId = player,
            prompt = "Choose",
            context = context,
            targetRequirements = listOf(TargetRequirementInfo(index = 0, description = "target")),
            legalTargets = mapOf(0 to listOf(first)),
            canCancel = true,
        )
        val targetResponse = TargetsResponse(cancellable.id, mapOf(0 to listOf(first)))
        val cancelResponse = CancelDecisionResponse(cancellable.id)

        DecisionValidators.validate(cancellable, targetResponse) shouldBe null
        DecisionValidators.validate(cancellable, cancelResponse) shouldBe null
        TrivialDecisions.responseFor(cancellable).shouldBeNull()

        val optional = cancellable.copy(
            id = "optional-target",
            canCancel = false,
            targetRequirements = listOf(
                TargetRequirementInfo(index = 0, description = "up to one target", minTargets = 0),
            ),
        )
        DecisionValidators.validate(optional, TargetsResponse(optional.id, emptyMap())) shouldBe null
        TrivialDecisions.responseFor(optional).shouldBeNull()
    }

    test("a lone target with a state-dependent legality cap is not guessed without state") {
        val decision = ChooseTargetsDecision(
            id = "aggregate-target",
            playerId = player,
            prompt = "Choose",
            context = context,
            targetRequirements = listOf(
                TargetRequirementInfo(
                    index = 0,
                    description = "target with limited total mana value",
                    totalManaValueAtMost = 0,
                ),
            ),
            legalTargets = mapOf(0 to listOf(first)),
        )

        TrivialDecisions.responseFor(decision).shouldBeNull()
    }

    test("singleton target groups remain nontrivial when cross-group independence is not represented") {
        val decision = ChooseTargetsDecision(
            id = "multiple-target-groups",
            playerId = player,
            prompt = "Choose",
            context = context,
            targetRequirements = listOf(
                TargetRequirementInfo(index = 0, description = "first target"),
                TargetRequirementInfo(index = 1, description = "another target"),
            ),
            legalTargets = mapOf(0 to listOf(first), 1 to listOf(second)),
        )

        // The original TargetRequirement objects may impose cross-slot distinctness (for example,
        // TargetOther), but ChooseTargetsDecision exposes no such field to this state-free helper.
        TrivialDecisions.responseFor(decision).shouldBeNull()
    }

    test("a lone option is not forced when the decision can be cancelled") {
        val decision = ChooseOptionDecision(
            id = "option",
            playerId = player,
            prompt = "Choose",
            context = context,
            options = listOf("Only option"),
            canCancel = true,
        )

        DecisionValidators.validate(decision, CancelDecisionResponse(decision.id)) shouldBe null
        TrivialDecisions.responseFor(decision).shouldBeNull()
    }

    test("one visible mode is not forced when the response contract permits another answer") {
        val optional = ChooseModeDecision(
            id = "mode",
            playerId = player,
            prompt = "Choose",
            context = context,
            modes = listOf(ModeOption(index = 0, text = "Only available")),
            minModes = 0,
            maxModes = 1,
        )
        DecisionValidators.validate(optional, ModesChosenResponse(optional.id, emptyList())) shouldBe null
        DecisionValidators.validate(optional, ModesChosenResponse(optional.id, listOf(0))) shouldBe null
        TrivialDecisions.responseFor(optional).shouldBeNull()

        val broaderMaximum = optional.copy(
            id = "repeatable-mode-contract",
            minModes = 1,
            maxModes = 2,
        )
        // A wider maximum does not prove that the single available mode is repeatable,
        // so the forced-only helper must not infer a unique response.
        TrivialDecisions.responseFor(broaderMaximum).shouldBeNull()
    }

    test("selecting every card is not forced when order is a player choice") {
        val decision = SelectCardsDecision(
            id = "ordered-cards",
            playerId = player,
            prompt = "Choose an order",
            context = context,
            options = listOf(first, second),
            minSelections = 2,
            maxSelections = 2,
            ordered = true,
        )

        DecisionValidators.validate(decision, CardsSelectedResponse(decision.id, listOf(first, second))) shouldBe null
        DecisionValidators.validate(decision, CardsSelectedResponse(decision.id, listOf(second, first))) shouldBe null
        TrivialDecisions.responseFor(decision).shouldBeNull()
    }

    test("multi-card selection is not forced without explicit repeated-selection semantics") {
        val decision = SelectCardsDecision(
            id = "shared-card-selection",
            playerId = player,
            prompt = "Choose two",
            context = context,
            options = listOf(first, second),
            minSelections = 2,
            maxSelections = 2,
        )

        // The same pending-decision shape is used by distributed counter-removal costs,
        // where repeated IDs carry multiplicity. The decision itself does not identify
        // that use, so selecting each option once is not provably the unique response.
        DecisionValidators.validate(
            decision,
            CardsSelectedResponse(decision.id, listOf(first, second)),
        ) shouldBe null
        TrivialDecisions.responseFor(decision).shouldBeNull()

        val rollout = FastDecisionResponder().respond(GameState(), decision, player)
        rollout.shouldBeInstanceOf<CardsSelectedResponse>()
        rollout.selectedCards shouldBe listOf(first, second)
    }

    test("state-dependent card constraints are not guessed without state") {
        val decision = SelectCardsDecision(
            id = "constrained-cards",
            playerId = player,
            prompt = "Choose",
            context = context,
            options = listOf(first),
            minSelections = 1,
            maxSelections = 1,
            onePerCardType = true,
        )

        TrivialDecisions.responseFor(decision).shouldBeNull()
    }

    test("a default damage assignment is policy when another legal assignment exists") {
        val blockerOne = EntityId.of("blocker-one")
        val blockerTwo = EntityId.of("blocker-two")
        val default = mapOf(blockerOne to 3)
        val alternative = mapOf(blockerOne to 2, blockerTwo to 1)
        val decision = AssignDamageDecision(
            id = "damage",
            playerId = player,
            prompt = "Assign damage",
            context = context,
            attackerId = first,
            availablePower = 3,
            orderedTargets = listOf(blockerOne, blockerTwo),
            defenderId = null,
            minimumAssignments = mapOf(blockerOne to 2, blockerTwo to 2),
            defaultAssignments = default,
            hasTrample = false,
            hasDeathtouch = false,
        )

        DecisionValidators.validate(decision, DamageAssignmentResponse(decision.id, default)) shouldBe null
        DecisionValidators.validate(decision, DamageAssignmentResponse(decision.id, alternative)) shouldBe null
        TrivialDecisions.responseFor(decision).shouldBeNull()

        val rollout = FastDecisionResponder().respond(GameState(), decision, player)
        rollout.shouldBeInstanceOf<DamageAssignmentResponse>()
        rollout.assignments shouldBe default
    }

    test("an auto-pay suggestion remains rollout policy rather than forced infrastructure") {
        val landOne = ManaSourceOption(first, "Island", setOf(Color.BLUE), producesColorless = false)
        val landTwo = ManaSourceOption(second, "Island", setOf(Color.BLUE), producesColorless = false)
        val decision = SelectManaSourcesDecision(
            id = "mana",
            playerId = player,
            prompt = "Pay {U}",
            context = context,
            availableSources = listOf(landOne, landTwo),
            requiredCost = "{U}",
            autoPaySuggestion = listOf(first),
            canDecline = true,
        )
        val auto = ManaSourcesSelectedResponse(decision.id, autoPay = true)
        val manual = ManaSourcesSelectedResponse(decision.id, selectedSources = listOf(second))
        val decline = ManaSourcesSelectedResponse(decision.id, declined = true)

        DecisionValidators.validate(decision, auto) shouldBe null
        DecisionValidators.validate(decision, manual) shouldBe null
        DecisionValidators.validate(decision, decline) shouldBe null
        TrivialDecisions.responseFor(decision).shouldBeNull()

        val rollout = FastDecisionResponder().respond(GameState(), decision, player)
        rollout.shouldBeInstanceOf<ManaSourcesSelectedResponse>()
        rollout.autoPay shouldBe true
    }
})
