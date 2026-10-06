package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Aspect of Wolf — {1}{G} Aura.
 * "Enchanted creature gets +X/+Y, where X is half the number of Forests you control, rounded
 * down, and Y is half the number of Forests you control, rounded up."
 *
 * Proves the two halves round in opposite directions on an odd Forest count (3 Forests →
 * +1/+2), that the bonus stays live as a Forest enters (4 Forests → +2/+2), and that the
 * opponent's Forests are not counted.
 */
class AspectOfWolfScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        test("power rounds down, toughness rounds up, and both track Forests you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Aspect of Wolf")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            val castResult = game.castSpell(1, "Aspect of Wolf", bears)
            withClue("Aspect of Wolf should cast successfully: ${castResult.error}") {
                castResult.error shouldBe null
            }
            game.resolveStack()

            withClue("Aspect of Wolf should be on the battlefield (attached)") {
                game.findPermanent("Aspect of Wolf").shouldNotBeNull()
            }

            // 3 Forests you control: +1/+2 (half of 3 rounded down / rounded up). The opponent's
            // two Forests don't count.
            withClue("Grizzly Bears power with 3 Forests (2 + 1)") {
                stateProjector.getProjectedPower(game.state, bears) shouldBe 3
            }
            withClue("Grizzly Bears toughness with 3 Forests (2 + 2)") {
                stateProjector.getProjectedToughness(game.state, bears) shouldBe 4
            }

            val forestInHand = game.findCardsInHand(1, "Forest").first()
            val playResult = game.execute(PlayLand(game.player1Id, forestInHand))
            withClue("Playing a 4th Forest should succeed: ${playResult.error}") {
                playResult.error shouldBe null
            }

            // 4 Forests: +2/+2.
            withClue("Grizzly Bears power with 4 Forests (2 + 2)") {
                stateProjector.getProjectedPower(game.state, bears) shouldBe 4
            }
            withClue("Grizzly Bears toughness with 4 Forests (2 + 2)") {
                stateProjector.getProjectedToughness(game.state, bears) shouldBe 4
            }
        }
    }
}
