package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Blessing of Frost (KHM #161).
 *
 *   Distribute X +1/+1 counters among any number of creatures you control, where X is the amount
 *   of {S} spent to cast this spell. Then draw a card for each creature you control with power 4
 *   or greater.
 *
 * X is the snow mana spent on the cast, and the draw count is read after the counters land.
 */
class BlessingOfFrostScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun blessingBoard(vararg lands: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Blessing of Frost")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(1, "Craw Wurm")
            .withCardOnBattlefield(2, "Llanowar Elves")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder.withCardInLibrary(1, "Forest") }
        lands.forEach { builder.withLandsOnBattlefield(1, it, 1) }
        return builder.build()
    }

    init {
        context("Blessing of Frost") {

            test("distributes one counter per snow mana, then counts creatures grown to power 4") {
                val game = blessingBoard(
                    "Snow-Covered Forest", "Snow-Covered Forest", "Snow-Covered Forest", "Forest"
                )
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castSpell(1, "Blessing of Frost").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
                withClue("three of the four mana came from snow sources") { decision.totalAmount shouldBe 3 }
                withClue("only creatures you control are eligible") {
                    decision.targets shouldContainExactlyInAnyOrder listOf(bears, giant, wurm)
                }
                game.submitDistribution(mapOf(bears to 2, giant to 1)).error shouldBe null

                game.plusOnes(bears) shouldBe 2
                game.plusOnes(giant) shouldBe 1
                withClue("Bears (4/4), Giant (4/4) and Wurm (6/4) all have power 4 or greater after the counters") {
                    game.handSize(1) shouldBe 3
                }
            }

            test("no snow mana spent distributes nothing but still draws for big creatures") {
                val game = blessingBoard("Forest", "Forest", "Forest", "Forest")

                game.castSpell(1, "Blessing of Frost").error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
                withClue("only Craw Wurm has power 4 or greater") { game.handSize(1) shouldBe 1 }
            }
        }
    }
}
