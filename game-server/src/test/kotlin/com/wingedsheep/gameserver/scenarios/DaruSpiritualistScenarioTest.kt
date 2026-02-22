package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Daru Spiritualist.
 *
 * Card reference:
 * - Daru Spiritualist: {1}{W} Creature — Human Cleric 1/1
 *   Whenever a Cleric creature you control becomes the target of a spell or ability,
 *   it gets +0/+2 until end of turn.
 */
class DaruSpiritualistScenarioTest : ScenarioTestBase() {

    init {
        context("Daru Spiritualist triggered ability") {
            test("Cleric survives damage thanks to +0/+2 toughness boost") {
                // Spark Spray deals 1 damage. Without trigger, 1/1 Spiritualist dies.
                // With trigger, Spiritualist becomes 1/3 and survives.
                val game = scenario()
                    .withPlayers("Cleric Player", "Opponent")
                    .withCardOnBattlefield(1, "Daru Spiritualist")
                    .withCardInHand(2, "Spark Spray")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spiritualist = game.findPermanent("Daru Spiritualist")!!

                // Opponent casts Spark Spray targeting Daru Spiritualist (a Cleric)
                game.castSpell(2, "Spark Spray", spiritualist)

                // Resolve entire stack: trigger resolves first (+0/+2), then Spark Spray (1 damage)
                game.resolveStack()

                // Spiritualist should survive: 1/3 with 1 damage = still alive
                val clientState = game.getClientState(1)
                val spiritualistInfo = clientState.cards[spiritualist]
                withClue("Daru Spiritualist should survive 1 damage as 1/3") {
                    spiritualistInfo shouldNotBe null
                }
            }

            test("does not trigger for non-Cleric creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Daru Spiritualist")
                    .withCardOnBattlefield(1, "Glory Seeker") // Human Soldier, not a Cleric
                    .withCardInHand(2, "Spark Spray") // 1 damage to any target
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val soldier = game.findPermanent("Glory Seeker")!!

                // Opponent casts Spark Spray targeting Glory Seeker (not a Cleric)
                game.castSpell(2, "Spark Spray", soldier)

                // Should resolve without any trigger firing (no Cleric targeted)
                game.resolveStack()

                // Glory Seeker (2/2) takes 1 damage, should survive
                val clientState = game.getClientState(1)
                val soldierInfo = clientState.cards[soldier]
                withClue("Glory Seeker should survive 1 damage (no trigger fired, still 2/2)") {
                    soldierInfo shouldNotBe null
                    soldierInfo!!.power shouldBe 2
                    soldierInfo.toughness shouldBe 2
                }
            }
        }
    }
}
