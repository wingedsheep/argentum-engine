package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Kozilek's Unsealing (MH3 #65) — {2}{U} Enchantment
 *
 *   Devoid
 *   Whenever you cast a creature spell with mana value 4, 5, or 6, create two Eldrazi Spawn.
 *   Whenever you cast a creature spell with mana value 7 or greater, draw three cards.
 */
class KozileksUnsealingScenarioTest : ScenarioTestBase() {

    private fun board(creature: String, forests: Int) = scenario()
        .withPlayers("Player1", "Opponent")
        .withCardOnBattlefield(1, "Kozilek's Unsealing")
        .withCardInHand(1, creature)
        .withLandsOnBattlefield(1, "Forest", forests)
        .apply { repeat(5) { withCardInLibrary(1, "Island") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Kozilek's Unsealing") {
            test("a creature spell with mana value 4 makes two Eldrazi Spawn") {
                val game = board("Giant Spider", 4)
                game.castSpell(1, "Giant Spider").error shouldBe null
                game.state.stack shouldHaveSize 2
                game.resolveStack()

                game.findPermanents("Eldrazi Spawn") shouldHaveSize 2
                game.handSize(1) shouldBe 0
            }

            test("a creature spell with mana value 6 makes two Eldrazi Spawn") {
                val game = board("Craw Wurm", 6)
                game.castSpell(1, "Craw Wurm").error shouldBe null
                game.resolveStack()

                game.findPermanents("Eldrazi Spawn") shouldHaveSize 2
                game.handSize(1) shouldBe 0
            }

            test("a creature spell with mana value 7 draws three cards and makes no Spawn") {
                val game = board("Whiptail Wurm", 7)
                game.castSpell(1, "Whiptail Wurm").error shouldBe null
                game.state.stack shouldHaveSize 2
                game.resolveStack()

                game.handSize(1) shouldBe 3
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
            }

            test("a creature spell with mana value 3 or less triggers nothing") {
                val game = board("Grizzly Bears", 2)
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.state.stack shouldHaveSize 1
                game.resolveStack()

                game.handSize(1) shouldBe 0
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
            }
        }
    }
}
