package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * CR 400.7 / 608.2b — a permanent that leaves the battlefield and comes back is a *new object*.
 * A spell or ability that targeted the old object no longer has a legal target, so it doesn't
 * resolve.
 *
 * Entity ids survive zone round-trips in this engine, so target validation must compare the
 * battlefield-entry stamp captured when the target was chosen, not just "is that id still a
 * creature on the battlefield".
 */
class BlinkedTargetFizzleScenarioTest : ScenarioTestBase() {

    init {
        context("a target blinked in response is a new object") {

            test("Emptiness' -1/-1 trigger fizzles when its target is blinked by Personify") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Opponent (P2) casts Emptiness with {B}{B} among the mana spent.
                    .withCardInHand(2, "Emptiness")
                    .withLandsOnBattlefield(2, "Swamp", 6)
                    // I control Eirdu and hold Personify.
                    .withCardOnBattlefield(1, "Eirdu, Carrier of Dawn", summoningSickness = false)
                    .withCardInHand(1, "Personify")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Emptiness").error shouldBe null
                game.resolveStack()

                // The black gate trigger asks for its "up to one target creature".
                val triggerTargets = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the -1/-1 trigger; got ${game.state.pendingDecision}")
                val eirdu = game.findPermanent("Eirdu, Carrier of Dawn")
                    ?: error("Eirdu should be on the battlefield")
                game.submitDecision(TargetsResponse(triggerTargets.id, mapOf(0 to listOf(eirdu))))

                withClue("the trigger should be waiting on the stack") {
                    game.state.stack.isNotEmpty() shouldBe true
                }

                // P2 passes; I respond by blinking Eirdu with Personify.
                if (game.state.priorityPlayerId != game.state.turnOrder[0]) {
                    game.passPriority()
                }
                game.castSpell(1, "Personify", eirdu).error shouldBe null

                game.resolveStack()

                val returned = game.findPermanent("Eirdu, Carrier of Dawn")
                    ?: error("Eirdu should have returned to the battlefield")
                val counters = game.state.getEntity(returned)?.get<CountersComponent>()
                withClue("the returned Eirdu is a new object — the trigger's target is illegal, so it doesn't resolve") {
                    (counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
                }
            }

            test("without the blink the same trigger still puts its three -1/-1 counters on") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Emptiness")
                    .withLandsOnBattlefield(2, "Swamp", 6)
                    .withCardOnBattlefield(1, "Eirdu, Carrier of Dawn", summoningSickness = false)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Emptiness").error shouldBe null
                game.resolveStack()

                val triggerTargets = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the -1/-1 trigger; got ${game.state.pendingDecision}")
                val eirdu = game.findPermanent("Eirdu, Carrier of Dawn")
                    ?: error("Eirdu should be on the battlefield")
                game.submitDecision(TargetsResponse(triggerTargets.id, mapOf(0 to listOf(eirdu))))
                game.resolveStack()

                val counters = game.state.getEntity(eirdu)?.get<CountersComponent>()
                counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 3
            }

            test("a removal spell fizzles when its target is blinked in response") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardOnBattlefield(1, "Eirdu, Carrier of Dawn", summoningSickness = false)
                    .withCardInHand(1, "Personify")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val eirdu = game.findPermanent("Eirdu, Carrier of Dawn")
                    ?: error("Eirdu should be on the battlefield")
                game.castSpell(2, "Murder", eirdu).error shouldBe null

                if (game.state.priorityPlayerId != game.state.turnOrder[0]) {
                    game.passPriority()
                }
                game.castSpell(1, "Personify", eirdu).error shouldBe null

                game.resolveStack()

                withClue("Murder's target left and returned as a new object, so Murder doesn't resolve") {
                    game.isOnBattlefield("Eirdu, Carrier of Dawn") shouldBe true
                }
            }

            test("a removal spell that is not answered still resolves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardOnBattlefield(1, "Eirdu, Carrier of Dawn", summoningSickness = false)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val eirdu = game.findPermanent("Eirdu, Carrier of Dawn")
                    ?: error("Eirdu should be on the battlefield")
                game.castSpell(2, "Murder", eirdu).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Eirdu, Carrier of Dawn") shouldBe false
            }
        }
    }
}
