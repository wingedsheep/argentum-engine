package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for [com.wingedsheep.sdk.scripting.effects.IterationSpace.CounterKindsOn],
 * `RemoveCountersOfIteratedKind`, and a `ChooseAction` inside a counter-kind loop — "for each kind
 * of counter on target permanent, put another counter of that kind on it or remove one from it"
 * (Dramatist's Puppet), driven through an inline sorcery.
 *
 * Proves: one add-or-remove prompt per kind on the target (and only the target), each label
 * naming its kind through `{counter}`; the chosen option still sees the kind after the decision;
 * a target without counters asks nothing; and the iterated-kind removal outside a counter-kind
 * loop removes nothing.
 */
class CounterKindsOnScenarioTest : ScenarioTestBase() {

    private val tinker = card("Test Counter Tinker") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "For each kind of counter on target permanent, put another counter of that kind on it or remove one from it."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.ForEachCounterKindOn(
                t,
                Effects.ChooseAction(
                    listOf(
                        EffectChoice("Put another {counter} counter on it",
                            Effects.AddCountersOfChosenKind(t, kindFrom = CounterKindSource.ITERATION)),
                        EffectChoice("Remove a {counter} counter from it",
                            Effects.RemoveCountersOfIteratedKind(t))
                    )
                )
            )
        }
    }

    private val pluck = card("Test Counter Pluck") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Remove a counter of that kind from target permanent."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.RemoveCountersOfIteratedKind(t)
        }
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent((c.get<CountersComponent>()?.counters ?: emptyMap()) + (type to amount)))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Map<CounterType, Int> =
        game.state.getEntity(id)?.get<CountersComponent>()?.counters.orEmpty().filterValues { it > 0 }

    private fun board(spell: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, spell)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Savannah Lions")
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.answer(label: String) {
        val decision = getPendingDecision() as ChooseOptionDecision
        val index = decision.options.indexOf(label)
        withClue("option '$label' offered among ${decision.options}") { (index >= 0) shouldBe true }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    init {
        cardRegistry.register(tinker)
        cardRegistry.register(pluck)

        test("each kind on the target gets its own add-or-remove choice, labelled with the kind") {
            val game = board("Test Counter Tinker")
            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 2)
            seed(game, bears, CounterType.STUN, 1)
            // Another permanent's kinds are not the target's.
            seed(game, lions, CounterType.SHIELD, 1)

            game.castSpell(1, "Test Counter Tinker", targetId = bears).error shouldBe null
            game.resolveStack()

            // Kinds visit in canonical id order: PLUS_ONE_PLUS_ONE, then STUN.
            val first = game.getPendingDecision() as ChooseOptionDecision
            first.options shouldBe listOf("Put another +1/+1 counter on it", "Remove a +1/+1 counter from it")
            game.answer("Put another +1/+1 counter on it")

            val second = game.getPendingDecision() as ChooseOptionDecision
            second.options shouldBe listOf("Put another stun counter on it", "Remove a stun counter from it")
            game.answer("Remove a stun counter from it")

            game.hasPendingDecision() shouldBe false
            counters(game, bears) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)
            counters(game, lions) shouldBe mapOf(CounterType.SHIELD to 1)
        }

        test("a target without counters asks nothing") {
            val game = board("Test Counter Tinker")
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Test Counter Tinker", targetId = bears).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            counters(game, bears) shouldBe emptyMap()
        }

        test("the iterated-kind removal outside a counter-kind loop removes nothing") {
            val game = board("Test Counter Pluck")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.STUN, 1)

            game.castSpell(1, "Test Counter Pluck", targetId = bears).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            counters(game, bears) shouldBe mapOf(CounterType.STUN to 1)
        }
    }
}
