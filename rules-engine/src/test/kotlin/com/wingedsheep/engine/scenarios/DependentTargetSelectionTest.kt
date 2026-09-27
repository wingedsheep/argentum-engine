package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.DependentTargetSelection
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.scripting.targets.TargetObject

class DependentTargetSelectionTest : FunSpec({
    val cards = TestCards.all
    fun driver() = GameTestDriver().also {
        it.registerCards(cards)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
    }

    test("a paused dependent selection preserves its chosen prefix through serialization") {
        val d = driver()
        val chosen = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val frame = com.wingedsheep.engine.core.TriggeredAbilityContinuation(
            sourceId = chosen,
            sourceName = "Test source",
            controllerId = d.player1,
            effect = com.wingedsheep.sdk.dsl.Effects.DrawCards(1),
            description = "Test dependent targets",
            sequentialTargets = listOf(listOf(chosen)),
        )
        val json = kotlinx.serialization.json.Json {
            serializersModule = com.wingedsheep.engine.core.engineSerializersModule
            allowStructuredMapKeys = true
        }
        val serializer = com.wingedsheep.engine.core.TriggeredAbilityContinuation.serializer()
        json.decodeFromString(serializer, json.encodeToString(serializer, frame)) shouldBe frame
    }

    test("a color-relative filter offers only first choices with a compatible partner") {
        val d = driver()
        val green = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val partner = d.putCreatureOnBattlefield(d.player2, "Llanowar Elves")
        val requirements = listOf(
            TargetObject(filter = TargetFilter.CreatureYouControl),
            TargetObject(filter = TargetFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.Creature.opponentControls().sharingColorWith(EffectTarget.ContextTarget(0)))),
        )
        val context = PredicateContext(controllerId = d.player1)
        DependentTargetSelection.isRequired(requirements) shouldBe true
        DependentTargetSelection.legalNext(d.state, requirements, emptyList(), context, targetFinder = TargetFinder(PredicateEvaluator(cardRegistry = null))) shouldBe listOf(green)
        DependentTargetSelection.legalNext(d.state, requirements, listOf(listOf(green)), context, targetFinder = TargetFinder(PredicateEvaluator(cardRegistry = null))) shouldBe listOf(partner)
    }

    test("lookahead checks all remaining slots rather than only the next slot") {
        val d = driver()
        val small = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
        val large = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val middle = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val requirements = listOf(
            TargetObject(filter = TargetFilter.CreatureYouControl),
            TargetObject(filter = TargetFilter.CreatureOpponentControls.powerLessThanEntity(EffectTarget.ContextTarget(0))),
            TargetObject(filter = TargetFilter.CreatureYouControl.powerLessThanEntity(EffectTarget.ContextTarget(1))),
        )
        val context = PredicateContext(controllerId = d.player1)
        DependentTargetSelection.legalNext(d.state, requirements, emptyList(), context, targetFinder = TargetFinder(PredicateEvaluator(cardRegistry = null))) shouldBe listOf(large)
        DependentTargetSelection.legalNext(d.state, requirements, listOf(listOf(large)), context, targetFinder = TargetFinder(PredicateEvaluator(cardRegistry = null))) shouldBe listOf(middle)
        DependentTargetSelection.legalNext(d.state, requirements, listOf(listOf(large), listOf(middle)), context, targetFinder = TargetFinder(PredicateEvaluator(cardRegistry = null))) shouldBe listOf(small)
    }

    test("the last slot may take several targets, all controlled by the chosen player") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val elves = d.putCreatureOnBattlefield(d.player2, "Llanowar Elves")
        val requirements = listOf(
            com.wingedsheep.sdk.scripting.targets.TargetPlayer(),
            TargetObject(
                count = 5,
                optional = true,
                filter = TargetFilter.Creature.targetPlayerControls(EffectTarget.ContextTarget(0)),
            ),
        )
        val context = PredicateContext(controllerId = d.player1)
        val finder = TargetFinder(PredicateEvaluator(cardRegistry = null))
        DependentTargetSelection.isRequired(requirements) shouldBe true
        DependentTargetSelection.legalNext(d.state, requirements, emptyList(), context, targetFinder = finder) shouldContainExactlyInAnyOrder
            listOf(d.player1, d.player2)
        DependentTargetSelection.legalNext(d.state, requirements, listOf(listOf(d.player2)), context, targetFinder = finder) shouldContainExactlyInAnyOrder
            listOf(bears, elves)
    }

    test("a required multi-target last slot rules out a first choice that can't fill its minimum") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.putCreatureOnBattlefield(d.player2, "Llanowar Elves")
        val requirements = listOf(
            com.wingedsheep.sdk.scripting.targets.TargetPlayer(),
            TargetObject(count = 2, filter = TargetFilter.Creature.targetPlayerControls(EffectTarget.ContextTarget(0))),
        )
        val context = PredicateContext(controllerId = d.player1)
        val finder = TargetFinder(PredicateEvaluator(cardRegistry = null))
        DependentTargetSelection.legalNext(d.state, requirements, emptyList(), context, targetFinder = finder) shouldBe
            listOf(d.player2)
    }

    test("a multi-target slot before the last is rejected") {
        val d = driver()
        val requirements = listOf(
            TargetObject(count = 2, filter = TargetFilter.CreatureYouControl),
            TargetObject(filter = TargetFilter.CreatureOpponentControls.powerLessThanEntity(EffectTarget.ContextTarget(0))),
        )
        shouldThrow<IllegalArgumentException> {
            DependentTargetSelection.legalNext(d.state, requirements, emptyList(), PredicateContext(controllerId = d.player1),
                targetFinder = TargetFinder(PredicateEvaluator(cardRegistry = null)))
        }
    }
})
