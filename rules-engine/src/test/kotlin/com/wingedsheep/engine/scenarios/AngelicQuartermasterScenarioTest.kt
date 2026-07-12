package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Angelic Quartermaster (VOW #2) — {3}{W}{W} Creature — Angel Soldier, 3/3,
 * Flying.
 *
 *   When this creature enters, put a +1/+1 counter on each of up to two other target creatures.
 *
 * Exercises the ETB fan-out: two other target creatures (any controller) each receive one
 * +1/+1 counter; the Quartermaster itself is excluded from the target pool.
 */
class AngelicQuartermasterScenarioTest : ScenarioTestBase() {

    init {
        context("Angelic Quartermaster ETB") {

            test("entering puts a +1/+1 counter on each of two other target creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angelic Quartermaster")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val card = game.findCardsInHand(1, "Angelic Quartermaster").first()
                game.execute(
                    CastSpell(game.player1Id, card, emptyList())
                ).error shouldBe null
                game.resolveStack() // creature enters -> ETB trigger asks for up to two targets

                game.selectTargets(listOf(bears, giant)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears (mine) gets a +1/+1 counter") {
                    game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("Hill Giant (opponent's) gets a +1/+1 counter") {
                    game.state.getEntity(giant)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
                withClue("Angelic Quartermaster is on the battlefield") {
                    game.isOnBattlefield("Angelic Quartermaster") shouldBe true
                }
            }

            test("declining targets puts no counters on anything") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angelic Quartermaster")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Angelic Quartermaster").error shouldBe null
                game.resolveStack()

                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("no counters placed when the optional targets are declined") {
                    val counters = game.state.getEntity(bears)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    counters shouldBe 0
                }
            }
        }
    }
}
