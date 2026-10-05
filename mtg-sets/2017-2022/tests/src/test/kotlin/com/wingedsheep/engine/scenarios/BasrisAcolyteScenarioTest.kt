package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Basri's Acolyte (M21 #8) — {2}{W}{W} Creature — Cat Cleric, 2/3, Lifelink.
 *
 *   When this creature enters, put a +1/+1 counter on each of up to two other target
 *   creatures you control.
 *
 * Exercises the ETB fan-out: two other creatures you control each get one +1/+1 counter;
 * the Acolyte itself and opponents' creatures are not legal targets; declining all targets
 * places no counters.
 */
class BasrisAcolyteScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Basri's Acolyte ETB") {

            test("entering puts a +1/+1 counter on each of two other target creatures you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Basri's Acolyte")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val lions = game.findPermanent("Savannah Lions")!!

                game.castSpell(1, "Basri's Acolyte").error shouldBe null
                game.resolveStack() // creature enters -> ETB trigger asks for up to two targets

                val acolyte = game.findPermanent("Basri's Acolyte")!!
                val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
                withClue("only other creatures you control are legal: not the Acolyte, not the opponent's Lions") {
                    decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(bears, giant)
                }

                game.selectTargets(listOf(bears, giant)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears gets a +1/+1 counter") { plusOneCounters(game, bears) shouldBe 1 }
                withClue("Hill Giant gets a +1/+1 counter") { plusOneCounters(game, giant) shouldBe 1 }
                withClue("the Acolyte itself gets nothing") { plusOneCounters(game, acolyte) shouldBe 0 }
                withClue("the opponent's Savannah Lions gets nothing") { plusOneCounters(game, lions) shouldBe 0 }
                withClue("Grizzly Bears is now 3/3") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }

            test("declining targets ('up to two') puts no counters on anything") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Basri's Acolyte")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Basri's Acolyte").error shouldBe null
                game.resolveStack()

                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("no counters placed when the optional targets are declined") {
                    plusOneCounters(game, bears) shouldBe 0
                }
                withClue("Basri's Acolyte is on the battlefield") {
                    game.isOnBattlefield("Basri's Acolyte") shouldBe true
                }
            }
        }
    }
}
