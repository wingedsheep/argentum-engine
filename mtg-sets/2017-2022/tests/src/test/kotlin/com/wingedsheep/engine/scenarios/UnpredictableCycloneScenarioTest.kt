package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Unpredictable Cyclone (IKO) — "If a cycling ability of another nonland card
 * would cause you to draw a card, instead exile cards from the top of your library until you exile
 * a card that shares a card type with the cycled card. You may cast that card without paying its
 * mana cost. Then put the exiled cards that weren't cast this way on the bottom of your library in
 * a random order."
 */
class UnpredictableCycloneScenarioTest : ScenarioTestBase() {

    private fun libraryNames(game: TestGame): List<String> =
        game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
            .map { game.state.getEntity(it)?.get<CardComponent>()?.name ?: "?" }

    /** Library, top first: [library] (by default Mountain, Shock, Grizzly Bears, Forest). */
    private fun setup(
        vararg inHand: String,
        library: List<String> = listOf("Mountain", "Shock", "Grizzly Bears", "Forest"),
    ): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Unpredictable Cyclone")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withLandsOnBattlefield(1, "Plains", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (card in inHand) builder = builder.withCardInHand(1, card)
        for (card in library) builder = builder.withCardInLibrary(1, card)
        repeat(4) { builder = builder.withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    init {
        context("Unpredictable Cyclone") {
            test("cycling a creature card exiles to the first creature, which may be cast for free") {
                val game = setup("Lava Serpent")
                val handBefore = game.handSize(1)

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                withClue("the free cast is offered as part of the replacement") {
                    game.answerYesNo(true).error shouldBe null
                }
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                withClue("no card was drawn") { game.handSize(1) shouldBe handBefore - 1 }
                val library = libraryNames(game)
                library.first() shouldBe "Forest"
                library.drop(1) shouldContainExactlyInAnyOrder listOf("Mountain", "Shock")
            }

            test("declining the cast puts the found card on the bottom with the rest") {
                val game = setup("Lava Serpent")

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                val library = libraryNames(game)
                library.first() shouldBe "Forest"
                library.drop(1) shouldContainExactlyInAnyOrder listOf("Mountain", "Shock", "Grizzly Bears")
            }

            test("with no card sharing a type, the whole library is exiled and put back, and nothing is drawn") {
                val game = setup("Lava Serpent", library = listOf("Mountain", "Shock", "Forest"))
                val handBefore = game.handSize(1)

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                game.resolveStack()

                withClue("no creature to find, so no cast is offered") { game.hasPendingDecision() shouldBe false }
                game.handSize(1) shouldBe handBefore - 1
                libraryNames(game) shouldContainExactlyInAnyOrder listOf("Mountain", "Shock", "Forest")
            }

            test("cycling a land card draws normally") {
                val game = setup("Secluded Steppe")
                val handBefore = game.handSize(1)

                game.cycleCard(1, "Secluded Steppe").error shouldBe null
                game.resolveStack()

                withClue("cycled away one card, drew the top Mountain") {
                    game.handSize(1) shouldBe handBefore
                }
                libraryNames(game) shouldBe listOf("Shock", "Grizzly Bears", "Forest")
            }
        }
    }
}
