package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Primal Druid (EMN #167) — {1}{G} 0/3 Creature — Human Druid.
 *
 *   When this creature dies, you may search your library for a basic land card, put it onto the
 *   battlefield tapped, then shuffle.
 */
class PrimalDruidScenarioTest : ScenarioTestBase() {

    init {
        fun boltTheDruid(): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Primal Druid")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val druid = game.findPermanent("Primal Druid")!!
            val cast = game.castSpell(1, "Lightning Bolt", druid)
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            game.isInGraveyard(1, "Primal Druid") shouldBe true
            return game
        }

        test("dying fetches a basic land onto the battlefield tapped") {
            val game = boltTheDruid()
            val forest = game.findCardsInLibrary(1, "Forest").single()

            game.answerYesNo(true)
            game.selectCards(listOf(forest))
            game.resolveStack()

            withClue("the Forest is on the battlefield, tapped") {
                game.findPermanent("Forest") shouldBe forest
                game.state.getEntity(forest)?.has<TappedComponent>() shouldBe true
            }
        }

        test("declining the search leaves the land in the library") {
            val game = boltTheDruid()

            game.answerYesNo(false)
            game.resolveStack()

            game.isOnBattlefield("Forest") shouldBe false
            game.findCardsInLibrary(1, "Forest").size shouldBe 1
        }
    }
}
