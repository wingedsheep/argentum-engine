package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rakdos, Lord of Riots (RTR 187).
 *
 * "You can't cast Rakdos unless an opponent lost life this turn.
 *  Flying, trample
 *  Creature spells you cast cost {1} less to cast for each 1 life your opponents have lost this turn."
 */
class RakdosLordOfRiotsScenarioTest : ScenarioTestBase() {
    init {
        test("can't be cast while no opponent has lost life this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Rakdos, Lord of Riots")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Rakdos, Lord of Riots").error shouldNotBe null
        }

        test("can be cast once an opponent has lost life this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Rakdos, Lord of Riots")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            game.castSpell(1, "Rakdos, Lord of Riots").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Rakdos, Lord of Riots") shouldBe true
        }

        test("creature spells cost {1} less for each life opponents lost this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Rakdos, Lord of Riots")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            withClue("three life lost takes {3}{R} down to {R}, payable with the last Mountain") {
                game.castSpell(1, "Hill Giant").error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("without life loss creature spells cost full price") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Rakdos, Lord of Riots")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Hill Giant").error shouldNotBe null
        }
    }
}
