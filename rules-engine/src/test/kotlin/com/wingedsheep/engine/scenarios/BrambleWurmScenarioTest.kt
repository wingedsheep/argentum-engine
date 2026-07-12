package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Bramble Wurm (VOW #189) — {6}{G} Creature — Wurm, 7/6, Reach, Trample.
 *
 *   When this creature enters, you gain 5 life.
 *   {2}{G}, Exile this card from your graveyard: You gain 5 life.
 *
 * Exercises the ETB lifegain and the graveyard-activated exile-self ability, both gaining 5 life.
 */
class BrambleWurmScenarioTest : ScenarioTestBase() {

    init {
        context("Bramble Wurm") {

            test("entering the battlefield gains 5 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bramble Wurm")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Bramble Wurm").error shouldBe null
                game.resolveStack()

                withClue("Bramble Wurm resolved onto the battlefield") {
                    game.isOnBattlefield("Bramble Wurm") shouldBe true
                }
                withClue("controller gained 5 life on entering the battlefield") {
                    game.getLifeTotal(1) shouldBe 25
                }
            }

            test("exiling it from the graveyard for {2}{G} gains 5 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Bramble Wurm")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findCardsInGraveyard(1, "Bramble Wurm").single()
                val abilityId = cardRegistry.getCard("Bramble Wurm")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wurm,
                        abilityId = abilityId
                    )
                )
                withClue("activation from the graveyard should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("the card is exiled from the graveyard") {
                    game.isInGraveyard(1, "Bramble Wurm") shouldBe false
                    game.isInExile(1, "Bramble Wurm") shouldBe true
                }
                withClue("controller gained 5 life") {
                    game.getLifeTotal(1) shouldBe 25
                }
            }
        }
    }
}
