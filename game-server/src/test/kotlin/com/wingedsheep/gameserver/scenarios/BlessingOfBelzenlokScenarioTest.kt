package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BlessingOfBelzenlokScenarioTest : ScenarioTestBase() {

    init {
        context("Blessing of Belzenlok") {
            test("gives +2/+1 to non-legendary creature without lifelink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blessing of Belzenlok")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Cabal Evangel") // 2/2, non-legendary
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Cabal Evangel")!!
                game.castSpell(1, "Blessing of Belzenlok", targetId)
                game.resolveStack()

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[targetId]
                withClue("Cabal Evangel should exist") {
                    cardInfo shouldNotBe null
                }
                withClue("Cabal Evangel should have 4 power (2+2)") {
                    cardInfo!!.power shouldBe 4
                }
                withClue("Cabal Evangel should have 3 toughness (2+1)") {
                    cardInfo!!.toughness shouldBe 3
                }
                withClue("Cabal Evangel should NOT have lifelink") {
                    cardInfo!!.keywords.contains(Keyword.LIFELINK) shouldBe false
                }
            }

            test("gives +2/+1 and lifelink to legendary creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blessing of Belzenlok")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Yargle, Glutton of Urborg") // 9/3, legendary
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Yargle, Glutton of Urborg")!!
                game.castSpell(1, "Blessing of Belzenlok", targetId)
                game.resolveStack()

                val clientState = game.getClientState(1)
                val cardInfo = clientState.cards[targetId]
                withClue("Yargle should exist") {
                    cardInfo shouldNotBe null
                }
                withClue("Yargle should have 11 power (9+2)") {
                    cardInfo!!.power shouldBe 11
                }
                withClue("Yargle should have 4 toughness (3+1)") {
                    cardInfo!!.toughness shouldBe 4
                }
                withClue("Yargle should have lifelink") {
                    cardInfo!!.keywords.contains(Keyword.LIFELINK) shouldBe true
                }
            }
        }
    }
}
