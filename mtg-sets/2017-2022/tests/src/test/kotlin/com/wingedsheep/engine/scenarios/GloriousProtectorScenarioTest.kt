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
 * Glorious Protector (KHM #12) — {2}{W}{W} Creature — Angel Cleric, 3/4
 *
 *   When this creature enters, you may exile any number of non-Angel creatures you control until
 *   this creature leaves the battlefield.
 *
 * Proves the untargeted resolution-time choice offers only your non-Angel creatures, that the
 * chosen ones are exiled linked to the Protector and come back when it leaves, and that choosing
 * none exiles nothing.
 */
class GloriousProtectorScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Glorious Protector")
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(1, "Serra Angel")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Glorious Protector") {

            test("exiles the chosen non-Angel creatures you control until it leaves the battlefield") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giants = game.findAllPermanents("Hill Giant")
                val myGiant = giants.first { game.state.getBattlefield(game.player1Id).contains(it) }

                game.castSpell(1, "Glorious Protector").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only your non-Angel creatures are offered") {
                    decision.options shouldContainExactlyInAnyOrder listOf(bears, myGiant)
                }
                game.selectCards(listOf(bears, myGiant)).error shouldBe null
                game.resolveStack()

                withClue("both chosen creatures are exiled; the Angel and the opponent's creature stay") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(1, "Hill Giant") shouldBe true
                    game.isOnBattlefield("Serra Angel") shouldBe true
                    game.findAllPermanents("Hill Giant").size shouldBe 1
                }

                val protector = game.findPermanent("Glorious Protector")!!
                game.castSpell(1, "Unsummon", protector).error shouldBe null
                game.resolveStack()

                withClue("when the Protector leaves, the exiled creatures return") {
                    game.isInHand(1, "Glorious Protector") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.findAllPermanents("Hill Giant").size shouldBe 2
                    game.isInExile(1, "Grizzly Bears") shouldBe false
                }
            }

            test("choosing no creatures exiles nothing") {
                val game = board()

                game.castSpell(1, "Glorious Protector").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.findAllPermanents("Hill Giant").size shouldBe 2
                game.isOnBattlefield("Glorious Protector") shouldBe true
                game.state.stack.isEmpty() shouldBe true
            }
        }
    }
}
