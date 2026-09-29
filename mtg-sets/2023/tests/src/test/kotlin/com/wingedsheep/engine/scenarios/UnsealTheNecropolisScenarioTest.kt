package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unseal the Necropolis (MOM #128) — "Each player mills three cards. Then you return up to two
 * creature cards from your graveyard to your hand."
 */
class UnsealTheNecropolisScenarioTest : ScenarioTestBase() {

    private fun buildGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Unseal the Necropolis")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInGraveyard(1, "Raging Goblin")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Grizzly Bears")
        .withCardInLibrary(2, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("each player mills three, then you return up to two of your creature cards, milled ones included") {
            val game = buildGame()

            game.castSpell(1, "Unseal the Necropolis").error shouldBe null
            game.resolveStack()

            game.librarySize(1) shouldBe 0
            game.librarySize(2) shouldBe 0
            game.graveyardSize(2) shouldBe 3

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val myGraveyard = game.state.getGraveyard(game.player1Id)
            fun named(n: String) = myGraveyard.single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == n
            }
            withClue("only creature cards in your own graveyard are offered") {
                decision.options shouldContainExactlyInAnyOrder listOf(
                    named("Raging Goblin"), named("Grizzly Bears"), named("Hill Giant")
                )
            }
            decision.maxSelections shouldBe 2

            game.selectCards(listOf(named("Grizzly Bears"), named("Hill Giant"))).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Raging Goblin") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("returning zero cards is allowed") {
            val game = buildGame()

            game.castSpell(1, "Unseal the Necropolis").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe false
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Raging Goblin") shouldBe true
        }
    }
}
