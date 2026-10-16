package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * Feature test for [com.wingedsheep.sdk.scripting.effects.IterationSpace.CounterKindsAmong] and
 * `AddCountersOfChosenKind(kindFrom = CounterKindSource.ITERATION)` — "for each kind of counter
 * among creatures you control, put a counter of that kind on …" (Exotic Pets), driven through an
 * inline sorcery aimed at one target.
 *
 * Proves: each kind counts once however many matching permanents carry it and however many
 * counters of it there are; permanents outside the group (an opponent's creature, your
 * noncreature permanent) contribute nothing; a kind present at zero doesn't count; no kinds means
 * no iterations; and the iterated-kind placement is a no-op outside such a loop.
 */
class ForEachCounterKindScenarioTest : ScenarioTestBase() {

    private val bloom = card("Test Counter Bloom") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "For each kind of counter among creatures you control, put a counter of that kind on target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ForEachCounterKindAmong(
                GroupFilter.AllCreaturesYouControl,
                Effects.AddCountersOfChosenKind(t, kindFrom = CounterKindSource.ITERATION)
            )
        }
    }

    private val stray = card("Test Stray Kind") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Put a counter of that kind on target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.AddCountersOfChosenKind(t, kindFrom = CounterKindSource.ITERATION)
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
        .withCardOnBattlefield(1, "Centaur Courser")
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardOnBattlefield(1, "Test Enchantment")
        .withCardOnBattlefield(2, "Goblin Guide")
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(bloom)
        cardRegistry.register(stray)

        test("each kind among your creatures adds exactly one counter of that kind") {
            val game = board("Test Counter Bloom")
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            val lions = game.findPermanent("Savannah Lions")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 3)
            seed(game, bears, CounterType.STUN, 1)
            // Same kind on a second creature: still one kind.
            seed(game, courser, CounterType.PLUS_ONE_PLUS_ONE, 1)
            // A kind at zero is not "among" anything.
            seed(game, courser, CounterType.SHIELD, 0)

            game.castSpell(1, "Test Counter Bloom", targetId = lions).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            counters(game, lions) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1, CounterType.STUN to 1)
        }

        test("an opponent's creature and your noncreature permanent contribute no kinds") {
            val game = board("Test Counter Bloom")
            val lions = game.findPermanent("Savannah Lions")!!
            seed(game, game.findPermanent("Goblin Guide")!!, CounterType.SHIELD, 1)
            seed(game, game.findPermanent("Test Enchantment")!!, CounterType.CHARGE, 2)

            game.castSpell(1, "Test Counter Bloom", targetId = lions).error shouldBe null
            game.resolveStack()

            counters(game, lions) shouldBe emptyMap()
        }

        test("the recipient's own counters count when it is one of the group") {
            val game = board("Test Counter Bloom")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Counter Bloom", targetId = bears).error shouldBe null
            game.resolveStack()

            counters(game, bears) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)
        }

        test("the iterated kind outside a counter-kind loop places nothing") {
            val game = board("Test Stray Kind")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Test Stray Kind", targetId = bears).error shouldBe null
            game.resolveStack()

            counters(game, bears) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)
        }
    }
}
