package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Wakedancer ({2}{B}, 2/2): "Morbid — When this creature enters, if a creature died this turn,
 * create a 2/2 black Zombie creature token."
 */
class WakedancerScenarioTest : ScenarioTestBase() {

    init {
        context("Wakedancer") {

            test("with a creature having died this turn, it makes a 2/2 Zombie") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wakedancer")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true

                game.castSpell(1, "Wakedancer").error shouldBe null
                game.resolveStack()

                val zombie = game.findPermanent("Zombie Token")
                withClue("morbid is on, so the Zombie is made") { zombie.shouldNotBeNull() }
                game.state.projectedState.getPower(zombie!!) shouldBe 2
                game.state.projectedState.getToughness(zombie) shouldBe 2
            }

            test("with no creature dead this turn, nothing is made") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wakedancer")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wakedancer").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Wakedancer") shouldBe true
                game.findPermanent("Zombie Token") shouldBe null
            }
        }
    }
}
