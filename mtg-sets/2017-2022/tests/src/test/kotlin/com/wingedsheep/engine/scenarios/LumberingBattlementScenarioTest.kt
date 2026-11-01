package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lumbering Battlement (RNA #15) — {4}{W} Creature — Beast, 4/5
 *
 *   Vigilance
 *   When this creature enters, exile any number of other nontoken creatures you control until it
 *   leaves the battlefield.
 *   This creature gets +2/+2 for each card exiled with it.
 *
 * Proves the choice offers only your other nontoken creatures, that the static bonus counts the
 * cards linked to the Battlement, and that they return when it leaves.
 */
class LumberingBattlementScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Lumbering Battlement")
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(1, "Llanowar Elves", isToken = true)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Lumbering Battlement") {

            test("exiles chosen creatures, grows +2/+2 per card exiled with it, and returns them when it leaves") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val myGiant = game.findAllPermanents("Hill Giant")
                    .first { game.state.getBattlefield(game.player1Id).contains(it) }

                game.castSpell(1, "Lumbering Battlement").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only your other nontoken creatures are offered") {
                    decision.options shouldContainExactlyInAnyOrder listOf(bears, myGiant)
                }
                game.selectCards(listOf(bears, myGiant)).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(1, "Hill Giant") shouldBe true

                val battlement = game.findPermanent("Lumbering Battlement")!!
                withClue("two cards exiled with it: 4/5 + 4/4") {
                    game.state.projectedState.getPower(battlement) shouldBe 8
                    game.state.projectedState.getToughness(battlement) shouldBe 9
                }

                game.castSpell(1, "Unsummon", battlement).error shouldBe null
                game.resolveStack()

                withClue("when the Battlement leaves, the exiled creatures return") {
                    game.isInHand(1, "Lumbering Battlement") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.findAllPermanents("Hill Giant").size shouldBe 2
                }
            }

            test("exiling nothing leaves it a 4/5") {
                val game = board()

                game.castSpell(1, "Lumbering Battlement").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                val battlement = game.findPermanent("Lumbering Battlement")!!
                game.state.projectedState.getPower(battlement) shouldBe 4
                game.state.projectedState.getToughness(battlement) shouldBe 5
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
