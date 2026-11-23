package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.battlefield.chosenColors
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tablet of the Guilds (RTR #235) — {2} Artifact.
 *
 *   As this artifact enters, choose two colors.
 *   Whenever you cast a spell, if it's at least one of the chosen colors, you gain 1 life for each
 *   of the chosen colors it is.
 */
class TabletOfTheGuildsScenarioTest : ScenarioTestBase() {

    private fun TestGame.castTabletChoosing(vararg colors: Color) {
        castSpell(1, "Tablet of the Guilds").error shouldBe null
        resolveStack()
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
        decision.minColors shouldBe 2
        submitDecision(ColorChosenResponse(decision.id, colors.first(), colors.toList())).error shouldBe null
        resolveStack()
    }

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Tablet of the Guilds")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("records both chosen colors; a spell of both gains 2, of one gains 1") {
            val game = base()
                .withCardInHand(1, "Judge's Familiar")
                .withCardInHand(1, "Savannah Lions")
                .build()
            game.castTabletChoosing(Color.WHITE, Color.BLUE)
            val tablet = game.findPermanent("Tablet of the Guilds")!!
            game.state.getEntity(tablet)!!.chosenColors() shouldBe listOf(Color.WHITE, Color.BLUE)
            val start = game.getLifeTotal(1)

            // Judge's Familiar ({W/U}) is both white and blue.
            game.castSpell(1, "Judge's Familiar").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe start + 2

            game.castSpell(1, "Savannah Lions").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe start + 3
        }

        test("a spell of neither chosen color doesn't trigger it") {
            val game = base()
                .withCardInHand(1, "Raging Goblin")
                .build()
            game.castTabletChoosing(Color.WHITE, Color.BLUE)
            val start = game.getLifeTotal(1)

            game.castSpell(1, "Raging Goblin").error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.getLifeTotal(1) shouldBe start
        }
    }
}
