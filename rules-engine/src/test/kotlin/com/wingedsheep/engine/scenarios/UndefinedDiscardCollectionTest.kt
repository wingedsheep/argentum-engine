package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.conditions.CollectionSharesCardType
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.shouldBe

class UndefinedDiscardCollectionTest : ScenarioTestBase() {
    init {
        val redirect = card("Undefined Collection Redirect") {
            typeLine = "Artifact"
            replacementEffect(OptionalEffectDiscardDestination(
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)))
        }
        cardRegistry.register(redirect)

        fun board() = scenario().withPlayers("First", "Second")
            .withCardOnBattlefield(1, redirect.name)
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(1, "Mountain")
            .withCardInHand(1, "Forest")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(1).withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        fun unknownContext(game: TestGame): EffectContext {
            val bear = game.findCardsInHand(1, "Grizzly Bears").single()
            return EffectContext(sourceId = null, controllerId = game.player1Id,
                pipeline = PipelineState(storedCollections = mapOf(
                    "discarded" to listOf(bear),
                    "${EffectDiscardDestinations.UNDEFINED}:discarded" to listOf(bear))))
        }

        test("a later paused selection preserves previous discard knowledge") {
            val game = board()
            val bear = game.findCardsInHand(1, "Grizzly Bears").single()
            val effect = Effects.Composite(listOf(
                GatherCardsEffect(CardSource.FromZone(Zone.HAND), "hand"),
                SelectFromCollectionEffect("hand", SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "discarded"),
                MoveCollectionEffect("discarded", CardDestination.ToZone(Zone.GRAVEYARD), moveType = MoveType.Discard),
                GatherCardsEffect(CardSource.FromZone(Zone.HAND), "fresh"),
                SelectFromCollectionEffect("fresh", SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "picked"),
                FilterCollectionEffect("discarded", GameObjectFilter.Creature, storeMatching = "creatures"),
                Effects.GainLife(DynamicAmounts.storedNumber("creatures_count"))))
            val result = services.effectExecutorRegistry.execute(game.state, effect,
                EffectContext(sourceId = null, controllerId = game.player1Id))
            result.error shouldBe null
            game.state = result.state
            game.selectCards(listOf(bear)).error shouldBe null
            val destination = game.state.pendingDecision as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(destination.id, 0)).error shouldBe null
            game.selectCards(listOf(game.findCardsInHand(1, "Mountain").single())).error shouldBe null
            game.getLifeTotal(1) shouldBe 20
        }

        test("variable gathers preserve undefined characteristics through aliases") {
            val game = board()
            val context = unknownContext(game)
            val alias = services.effectExecutorRegistry.execute(game.state,
                GatherCardsEffect(CardSource.FromVariable("discarded"), "alias"), context)
            val aliasContext = context.copy(pipeline = context.pipeline.copy(
                storedCollections = context.pipeline.storedCollections + alias.updatedCollections))
            val result = services.effectExecutorRegistry.execute(game.state,
                FilterCollectionEffect("alias", GameObjectFilter.Creature, storeMatching = "creatures"), aliasContext)
            result.updatedCollections["creatures"] shouldBe emptyList()
            alias.updatedCollections["${EffectDiscardDestinations.UNDEFINED}:alias"] shouldBe
                context.pipeline.storedCollections["discarded"]
        }

        test("characteristic selection and restrictions cannot classify undefined cards") {
            val game = board()
            val context = unknownContext(game)
            val union = GameObjectFilter(anyOf = listOf(GameObjectFilter.Creature, GameObjectFilter.Land))
            for (filter in listOf(GameObjectFilter.Creature, union)) {
                val result = services.effectExecutorRegistry.execute(game.state,
                    SelectFromCollectionEffect("discarded", SelectionMode.All,
                        filter = filter, storeSelected = "selected"), context)
                result.updatedCollections["selected"] shouldBe emptyList()
            }
            for (restriction in listOf(SelectionRestriction.OnePerCardType,
                SelectionRestriction.TotalManaValueAtMost(5))) {
                val result = services.effectExecutorRegistry.execute(game.state,
                    SelectFromCollectionEffect("discarded", SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                        storeSelected = "selected", restrictions = listOf(restriction)), context)
                result.updatedCollections["selected"] shouldBe emptyList()
            }
            val countOnly = services.effectExecutorRegistry.execute(game.state,
                SelectFromCollectionEffect("discarded", SelectionMode.All, storeSelected = "selected"), context)
            countOnly.updatedCollections["selected"] shouldBe context.pipeline.storedCollections["discarded"]
        }

        test("undefined characteristics cannot reduce a selection minimum") {
            val game = board()
            val context = unknownContext(game)
            val cards = context.pipeline.storedCollections["discarded"].orEmpty() +
                game.findCardsInHand(1, "Mountain").single()
            val twoCards = context.copy(pipeline = context.pipeline.copy(storedCollections =
                context.pipeline.storedCollections + ("discarded" to cards)))
            val result = services.effectExecutorRegistry.execute(game.state,
                SelectFromCollectionEffect("discarded", SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                    storeSelected = "selected", alwaysPrompt = true,
                    restrictions = listOf(SelectionRestriction.ReducedMinimumIfMatches(
                        reducedMinimum = 1, filter = GameObjectFilter.Creature))), twoCards)
            val decision = result.state.pendingDecision as SelectCardsDecision
            decision.minSelections shouldBe 2
            decision.conditionalMinimums shouldBe emptyList()
        }

        test("undefined cards offer no castable faces") {
            val game = board()
            val result = services.effectExecutorRegistry.execute(game.state,
                SelectFromCollectionEffect("discarded", SelectionMode.ChooseSpell, storeSelected = "spell"),
                unknownContext(game))
            (result.state.pendingDecision as SelectCardsDecision).options shouldBe emptyList()
        }

        test("collection conditions do not read undefined card types") {
            val game = board()
            val context = unknownContext(game)
            val mountain = game.findCardsInHand(1, "Mountain").single()
            val union = GameObjectFilter(anyOf = listOf(GameObjectFilter.Creature, GameObjectFilter.Land))
            services.conditionEvaluator.evaluate(game.state,
                CollectionContainsMatch("discarded", union), context) shouldBe false
            services.conditionEvaluator.evaluate(game.state,
                CollectionContainsMatch("discarded"), context) shouldBe true
            val twoCards = context.copy(pipeline = context.pipeline.copy(storedCollections =
                context.pipeline.storedCollections + ("discarded" to listOf(mountain,
                    game.findCardsInHand(1, "Forest").single())) +
                    ("${EffectDiscardDestinations.UNDEFINED}:discarded" to listOf(mountain))))
            services.conditionEvaluator.evaluate(game.state,
                CollectionSharesCardType("discarded"), twoCards) shouldBe false
        }
    }
}
