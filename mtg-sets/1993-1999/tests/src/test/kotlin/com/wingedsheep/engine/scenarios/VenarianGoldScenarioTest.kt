package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Venarian Gold (LEG #83) — "When this Aura enters, tap enchanted creature and put X sleep counters
 * on it. Enchanted creature doesn't untap during its controller's untap step if it has a sleep
 * counter on it. At the beginning of the upkeep of enchanted creature's controller, remove a sleep
 * counter from that creature."
 *
 * What the script leans on, and so what is proved here: the enters trigger reads X off the resolved
 * Aura (the spell is gone by then), the untap lock is gated on counters on the *creature* rather
 * than on the Aura, and the counter comes off only in the enchanted creature's controller's upkeep —
 * the Aura goes on the opponent's creature so the two upkeeps can be told apart.
 */
class VenarianGoldScenarioTest : ScenarioTestBase() {

    init {
        context("Venarian Gold") {

            fun board() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Venarian Gold")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .apply {
                    repeat(8) {
                        withCardInLibrary(1, "Island")
                        withCardInLibrary(2, "Island")
                    }
                }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.castGold(x: Int, target: EntityId) = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = state.getHand(player1Id).first {
                        state.getEntity(it)?.get<CardComponent>()?.name == "Venarian Gold"
                    },
                    targets = listOf(ChosenTarget.Permanent(target)),
                    xValue = x
                )
            )

            fun TestGame.sleep(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SLEEP) ?: 0

            fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)!!.has<TappedComponent>()

            /** Pass from a main phase to the next turn's precombat main. */
            fun TestGame.nextTurnMain() {
                passUntilPhase(Phase.ENDING, Step.END)
                passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            }

            test("enters tapping the creature and putting X sleep counters on it") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castGold(2, bears).error shouldBe null
                game.resolveStack()

                withClue("the enters trigger taps the enchanted creature") { game.isTapped(bears) shouldBe true }
                withClue("X = 2 sleep counters") { game.sleep(bears) shouldBe 2 }
            }

            test("holds the creature tapped while it has a sleep counter, one coming off each of its controller's upkeeps") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castGold(2, bears).error shouldBe null
                game.resolveStack()

                // Player 2's turn: skipped by the untap step, then its upkeep removes one: 2 -> 1.
                game.nextTurnMain()
                game.state.activePlayerId shouldBe game.player2Id
                withClue("doesn't untap while it has a sleep counter") { game.isTapped(bears) shouldBe true }
                withClue("its controller's upkeep removed a sleep counter") { game.sleep(bears) shouldBe 1 }

                // Player 1's turn: the Aura controller's upkeep is not the trigger's upkeep.
                game.nextTurnMain()
                game.state.activePlayerId shouldBe game.player1Id
                withClue("no counter comes off in the Aura controller's upkeep") { game.sleep(bears) shouldBe 1 }

                // Player 2's turn: it still had a counter during the untap step, so it stays tapped; 1 -> 0.
                game.nextTurnMain()
                game.state.activePlayerId shouldBe game.player2Id
                withClue("one counter left at the untap step still holds it") { game.isTapped(bears) shouldBe true }
                game.sleep(bears) shouldBe 0

                // Player 2's next turn: no sleep counter, so it untaps normally.
                game.nextTurnMain()
                game.nextTurnMain()
                game.state.activePlayerId shouldBe game.player2Id
                withClue("with the counters gone it untaps") { game.isTapped(bears) shouldBe false }
            }

            test("X = 0 taps the creature but places no counters, so it untaps next turn") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castGold(0, bears).error shouldBe null
                game.resolveStack()

                game.isTapped(bears) shouldBe true
                game.sleep(bears) shouldBe 0

                game.nextTurnMain()
                game.state.activePlayerId shouldBe game.player2Id
                game.isTapped(bears) shouldBe false
            }
        }
    }
}
