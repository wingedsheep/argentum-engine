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
 * Scenario tests for Blood on the Snow (KHM #79).
 *
 *   Choose one — Destroy all creatures; or Destroy all planeswalkers. Then return a creature or
 *   planeswalker card with mana value X or less from your graveyard to the battlefield, where X is
 *   the amount of {S} spent to cast this spell.
 *
 * The return follows either mode, is chosen on resolution after the destruction (so a just-destroyed
 * creature is eligible), and is capped by the snow mana spent on the cast.
 */
class BloodOnTheSnowScenarioTest : ScenarioTestBase() {

    init {
        context("Blood on the Snow") {

            test("destroy all creatures, then return one with mana value up to the snow mana spent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blood on the Snow")
                    .withLandsOnBattlefield(1, "Snow-Covered Swamp", 3)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Serra Angel")
                    .withCardInGraveyard(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Blood on the Snow", 0).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("X = 3: own creature cards of mana value 3 or less, including the just-destroyed Bears") {
                    decision.options shouldContainExactlyInAnyOrder listOf(
                        game.findCardsInGraveyard(1, "Grizzly Bears").single(),
                        game.findCardsInGraveyard(1, "Centaur Courser").single(),
                    )
                }
                game.selectCards(listOf(game.findCardsInGraveyard(1, "Centaur Courser").single())).error shouldBe null

                game.isOnBattlefield("Centaur Courser") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Serra Angel") shouldBe false
                game.isInGraveyard(2, "Serra Angel") shouldBe true
            }

            test("destroy all planeswalkers leaves creatures; no snow mana caps the return at mana value 0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Blood on the Snow")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Garruk Wildspeaker")
                    .withCardInGraveyard(1, "Ornithopter")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Blood on the Snow", 1).error shouldBe null
                game.resolveStack()

                val pending = game.getPendingDecision()
                if (pending is SelectCardsDecision) {
                    withClue("X = 0: only the mana value 0 Ornithopter qualifies") {
                        pending.options shouldContainExactlyInAnyOrder
                            listOf(game.findCardsInGraveyard(1, "Ornithopter").single())
                    }
                    game.selectCards(pending.options).error shouldBe null
                }

                game.isOnBattlefield("Ornithopter") shouldBe true
                game.isOnBattlefield("Centaur Courser") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Garruk Wildspeaker") shouldBe false
                game.isInGraveyard(2, "Garruk Wildspeaker") shouldBe true
            }
        }
    }
}
