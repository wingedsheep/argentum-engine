package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Oath of Liliana (EMN #99) — {2}{B} Legendary Enchantment.
 *
 *   When Oath of Liliana enters, each opponent sacrifices a creature of their choice.
 *   At the beginning of each end step, if a planeswalker entered the battlefield under your
 *   control this turn, create a 2/2 black Zombie creature token.
 *
 * The end-step trigger is an intervening "if" over the turn's entry log, read for the Oath's
 * controller: a planeswalker that entered before the Oath did still counts (ruling 2016-07-13),
 * and an opponent's planeswalker never does.
 */
class OathOfLilianaScenarioTest : ScenarioTestBase() {

    init {
        test("enters: each opponent sacrifices a creature of their choice") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Oath of Liliana")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Elite Vanguard")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Oath of Liliana").error shouldBe null
            game.resolveStack()

            withClue("the opponent chooses which of their creatures to sacrifice") {
                game.hasPendingDecision() shouldBe true
                game.state.pendingDecision!!.playerId shouldBe game.player2Id
            }
            game.selectCards(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            withClue("only opponents sacrifice") {
                game.isOnBattlefield("Elite Vanguard") shouldBe true
            }
        }

        test("a planeswalker that entered before the Oath this turn still makes a Zombie at the end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Liliana Vess")
                .withCardInHand(1, "Oath of Liliana")
                .withLandsOnBattlefield(1, "Swamp", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Liliana Vess").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Liliana Vess") shouldBe true

            game.castSpell(1, "Oath of Liliana").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Oath of Liliana") shouldBe true
            game.findPermanents("Zombie Token") shouldHaveSize 0

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("one Zombie for the turn a planeswalker entered under the Oath's controller") {
                game.findPermanents("Zombie Token") shouldHaveSize 1
            }
        }

        test("no Zombie at the end step of a turn in which no planeswalker entered") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Oath of Liliana")
                .withCardOnBattlefield(1, "Liliana Vess")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.findPermanents("Zombie Token") shouldHaveSize 0
        }

        test("an opponent's planeswalker entering doesn't count") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Oath of Liliana")
                .withCardInHand(2, "Liliana Vess")
                .withLandsOnBattlefield(2, "Swamp", 5)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Liliana Vess").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Liliana Vess") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.findPermanents("Zombie Token") shouldHaveSize 0
        }
    }
}
