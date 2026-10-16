package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Rogues' Gallery (SNC #92) — {2}{B} Sorcery.
 *
 *   For each color, return up to one target creature card of that color from your graveyard to
 *   your hand.
 *
 * Pins the one-target-per-colour rule: a multicoloured card claims whichever colour is left free,
 * two cards whose only colour is the same can't both be chosen, and a colourless creature card is
 * never a legal target. The cast-time offer carries each candidate's colours so the picker can
 * keep the selection legal while it is built.
 */
class RoguesGalleryScenarioTest : ScenarioTestBase() {

    private fun graveyardGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Rogues' Gallery")
        .withCardInGraveyard(1, "Sky Spirit")      // white-blue
        .withCardInGraveyard(1, "Savannah Lions")  // white
        .withCardInGraveyard(1, "Wind Drake")      // blue
        .withCardInGraveyard(1, "Hill Giant")      // red
        .withCardInGraveyard(1, "Raging Goblin")   // red
        .withCardInGraveyard(1, "Ornithopter")     // colourless
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.graveyardCard(name: String) = findCardsInGraveyard(1, name).single()

    init {
        test("returns one creature card of each colour, a multicoloured card filling the free colour") {
            val game = graveyardGame()
            val targets = listOf("Sky Spirit", "Savannah Lions", "Hill Giant").map { game.graveyardCard(it) }

            game.castSpellTargetingGraveyardCard(1, "Rogues' Gallery", targets).error shouldBe null
            game.resolveStack()

            listOf("Sky Spirit", "Savannah Lions", "Hill Giant").forEach { game.isInHand(1, it) shouldBe true }
            game.isInGraveyard(1, "Wind Drake") shouldBe true
            game.isInGraveyard(1, "Raging Goblin") shouldBe true
        }

        test("two cards whose only colour is the same can't both be targeted") {
            val game = graveyardGame()
            val targets = listOf("Hill Giant", "Raging Goblin").map { game.graveyardCard(it) }

            val error = game.castSpellTargetingGraveyardCard(1, "Rogues' Gallery", targets).error
            error.shouldNotBeNull() shouldContain "one of each color"
        }

        test("a multicoloured card can't fill two colour slots at once") {
            val game = graveyardGame()
            val targets = listOf("Sky Spirit", "Savannah Lions", "Wind Drake").map { game.graveyardCard(it) }

            game.castSpellTargetingGraveyardCard(1, "Rogues' Gallery", targets).error.shouldNotBeNull()
        }

        test("a colourless creature card is not a legal target") {
            val game = graveyardGame()

            game.castSpellTargetingGraveyardCard(1, "Rogues' Gallery", listOf(game.graveyardCard("Ornithopter")))
                .error.shouldNotBeNull()
        }

        test("the cast offer lists each coloured candidate's colours and caps the count at the largest legal pick") {
            val game = graveyardGame()

            val cast = game.getLegalActions(1).single { it.description == "Cast Rogues' Gallery" }
            val requirement = cast.targetRequirements.shouldNotBeNull().single()
            val categories = requirement.distinctCategories.shouldNotBeNull()

            categories.keys shouldContainExactlyInAnyOrder
                listOf("Sky Spirit", "Savannah Lions", "Wind Drake", "Hill Giant", "Raging Goblin").map { game.graveyardCard(it) }
            categories[game.graveyardCard("Sky Spirit")] shouldBe listOf("BLUE", "WHITE")
            requirement.validTargets.contains(game.graveyardCard("Ornithopter")) shouldBe false
            // White, blue and red are the only colours present.
            requirement.maxTargets shouldBe 3
        }
    }
}
