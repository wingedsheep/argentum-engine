package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.PlayerHexproofFromComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Veil of Summer — draw a card if an opponent has cast a blue or black spell this turn; spells you
 * control can't be countered this turn; you and permanents you control gain hexproof from blue and
 * from black until end of turn.
 */
class VeilOfSummerScenarioTest : ScenarioTestBase() {

    init {
        context("Veil of Summer") {
            test("after an opponent's blue spell: draws, and shields you and your permanents from blue and black only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Veil of Summer")
                    .withCardInLibrary(1, "Forest")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(2, "Divination")
                    .withCardInHand(2, "Doom Blade")
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Divination").error shouldBe null
                game.resolveStack()
                game.passPriority() // the active opponent passes on an empty stack; Player1 gets priority

                game.castSpell(1, "Veil of Summer").error shouldBe null
                game.resolveStack()
                withClue("an opponent cast a blue spell this turn, so Veil draws a card") {
                    game.handSize(1) shouldBe 1
                }
                withClue("the player carries hexproof from blue and black") {
                    game.state.getEntity(game.player1Id)?.get<PlayerHexproofFromComponent>() shouldNotBe null
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("an opponent's black Doom Blade can't target your creature") {
                    game.castSpell(2, "Doom Blade", bears).error shouldNotBe null
                }
                withClue("a red Lightning Bolt can still target you") {
                    game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                }
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 17
            }

            test("no card is drawn when the opponent's spells this turn were neither blue nor black") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Veil of Summer")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.passPriority()

                game.castSpell(1, "Veil of Summer").error shouldBe null
                game.resolveStack()
                withClue("a red spell doesn't satisfy \"a blue or black spell\"") {
                    game.handSize(1) shouldBe 0
                }
            }
        }
    }
}
