package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ninth Bridge Patrol (KLD #22) — "Whenever another creature you control leaves the battlefield,
 * put a +1/+1 counter on this creature."
 *
 * Proves the three axes of the trigger: death counts, a bounce counts too ("leaves", not "dies"),
 * and an opponent's creature leaving does not (controller-scoped).
 */
class NinthBridgePatrolScenarioTest : ScenarioTestBase() {

    init {
        context("Ninth Bridge Patrol") {

            fun TestGame.patrolCounters(): Int = findPermanent("Ninth Bridge Patrol")
                ?.let { state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) }
                ?: 0

            test("another creature you control dying puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ninth Bridge Patrol")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()
                game.resolveStack()

                withClue("the Bears died, so the Patrol grew to 2/2") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.patrolCounters() shouldBe 1
                    val patrol = game.findPermanent("Ninth Bridge Patrol")!!
                    game.state.projectedState.getPower(patrol) shouldBe 2
                    game.state.projectedState.getToughness(patrol) shouldBe 2
                }
            }

            test("a creature you control returned to hand also counts — it only has to leave") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ninth Bridge Patrol")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Unsummon", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                withClue("the Bears were bounced, and the Patrol still got a counter") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.patrolCounters() shouldBe 1
                }
            }

            test("an opponent's creature leaving does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ninth Bridge Patrol")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()
                game.resolveStack()

                withClue("only creatures you control feed the Patrol") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.patrolCounters() shouldBe 0
                }
            }
        }
    }
}
