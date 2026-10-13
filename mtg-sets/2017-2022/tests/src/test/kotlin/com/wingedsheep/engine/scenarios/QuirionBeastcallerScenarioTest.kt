package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Quirion Beastcaller (DMU #175) — "Whenever you cast a creature spell, put a +1/+1 counter on
 * this creature. When this creature dies, distribute X +1/+1 counters among any number of target
 * creatures you control, where X is the number of +1/+1 counters on this creature."
 *
 * X is last-known information: the counters are gone with the creature, so the dies trigger must
 * read them off the zone change — both for the pool and for the target cap (CR 601.2d).
 */
class QuirionBeastcallerScenarioTest : ScenarioTestBase() {

    init {
        context("Quirion Beastcaller") {

            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Quirion Beastcaller")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Llanowar Elves")
                .withCardInHand(1, "Llanowar Elves")
                .withCardInHand(1, "Terror")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.plusOnes(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            fun TestGame.castTwoElves() {
                repeat(2) {
                    castSpell(1, "Llanowar Elves").error shouldBe null
                    resolveStack()
                }
            }

            test("each creature spell cast puts a +1/+1 counter on it") {
                val game = board()
                val caller = game.findPermanent("Quirion Beastcaller")!!
                game.castTwoElves()
                game.plusOnes(caller) shouldBe 2
                game.state.projectedState.getPower(caller) shouldBe 4
            }

            test("dying distributes its last-known counters among creatures you control") {
                val game = board()
                val caller = game.findPermanent("Quirion Beastcaller")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castTwoElves()

                game.castSpell(1, "Terror", caller).error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("a single target takes the whole last-known pool") {
                    game.plusOnes(bears) shouldBe 2
                }
            }

            test("dying with no counters asks for no targets") {
                val game = board()
                val caller = game.findPermanent("Quirion Beastcaller")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Terror", caller).error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.plusOnes(bears) shouldBe 0
            }

            test("the target count is capped at X") {
                val game = board()
                val caller = game.findPermanent("Quirion Beastcaller")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castTwoElves()
                val elves = game.findAllPermanents("Llanowar Elves")

                game.castSpell(1, "Terror", caller).error shouldBe null
                game.resolveStack()
                withClue("three targets for two counters is illegal") {
                    game.selectTargets(listOf(bears) + elves).error shouldNotBe null
                }
                game.selectTargets(listOf(bears, elves.first())).error shouldBe null
                val decision = game.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
                decision.totalAmount shouldBe 2
                game.submitDecision(
                    DistributionResponse(decision.id, mapOf(bears to 1, elves.first() to 1))
                ).error shouldBe null
                game.resolveStack()
                game.plusOnes(bears) shouldBe 1
                game.plusOnes(elves.first()) shouldBe 1
            }
        }
    }
}
