package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class SageOfAncientLoreScenarioTest : ScenarioTestBase() {
    init {
        context("Sage of Ancient Lore // Werewolf of Ancient Hunger") {
            fun advanceToNextUpkeep(game: TestGame) {
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
            }

            test("front counts your hand after the ETB draw; back counts every player's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sage of Ancient Lore")
                    .withCardsInHand(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sage of Ancient Lore").error shouldBe null
                game.resolveStack()

                val sage = game.findPermanent("Sage of Ancient Lore")!!
                game.handSize(1) shouldBe 3
                game.state.projectedState.getPower(sage) shouldBe 3
                game.state.projectedState.getToughness(sage) shouldBe 3

                // A spell (the Sage itself) was cast last turn: no transform at the opponent's upkeep.
                advanceToNextUpkeep(game)
                game.state.getEntity(sage)!!.get<CardComponent>()!!.name shouldBe "Sage of Ancient Lore"

                // The opponent cast nothing: it transforms at our upkeep.
                advanceToNextUpkeep(game)
                game.state.getEntity(sage)!!.get<CardComponent>()!!.name shouldBe "Werewolf of Ancient Hunger"
                game.handSize(2) shouldBeGreaterThan 0
                val allHands = game.handSize(1) + game.handSize(2)
                game.state.projectedState.getPower(sage) shouldBe allHands
                game.state.projectedState.getToughness(sage) shouldBe allHands
            }

            test("with an empty hand it dies as a 0/0 before its draw trigger resolves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sage of Ancient Lore")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sage of Ancient Lore").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Sage of Ancient Lore") shouldBe false
                game.isInGraveyard(1, "Sage of Ancient Lore") shouldBe true
                game.handSize(1) shouldBe 1
            }
        }
    }
}
