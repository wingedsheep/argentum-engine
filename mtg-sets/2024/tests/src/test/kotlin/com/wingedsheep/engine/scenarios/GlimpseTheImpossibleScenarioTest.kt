package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Glimpse the Impossible (MH3 #124) — {2}{R} Sorcery.
 *
 *   "Exile the top three cards of your library. You may play those cards this turn. At the
 *    beginning of the next end step, if any of those cards remain exiled, put them into your
 *    graveyard, then create a 0/1 colorless Eldrazi Spawn creature token for each card put into
 *    your graveyard this way."
 */
class GlimpseTheImpossibleScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Glimpse the Impossible")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardInLibrary(1, "Mountain") // top
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Glimpse the Impossible") {

            test("a card played from exile is not graveyarded; the rest become Spawn at the end step") {
                val game = setup()

                val cast = game.castSpell(1, "Glimpse the Impossible")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("the top three cards are exiled") {
                    game.isInExile(1, "Mountain") shouldBe true
                    game.isInExile(1, "Island") shouldBe true
                    game.isInExile(1, "Swamp") shouldBe true
                    game.librarySize(1) shouldBe 2
                }

                val exiledMountain = game.state.getExile(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Mountain"
                }
                val play = game.execute(PlayLand(game.player1Id, exiledMountain))
                withClue("the exiled Mountain can be played: ${play.error}") { play.error shouldBe null }
                game.isInExile(1, "Mountain") shouldBe false

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the two cards still in exile went to the graveyard") {
                    game.isInExile(1, "Island") shouldBe false
                    game.isInExile(1, "Swamp") shouldBe false
                    game.isInGraveyard(1, "Island") shouldBe true
                    game.isInGraveyard(1, "Swamp") shouldBe true
                    game.isInGraveyard(1, "Mountain") shouldBe false
                }
                withClue("one Eldrazi Spawn per card put into the graveyard") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 2
                }
            }

            test("with nothing played, all three are graveyarded for three Spawn") {
                val game = setup()

                game.castSpell(1, "Glimpse the Impossible").error shouldBe null
                game.resolveStack()

                withClue("no Spawn before the end step") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("all three exiled cards plus the sorcery are in the graveyard") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                    game.isInGraveyard(1, "Island") shouldBe true
                    game.isInGraveyard(1, "Swamp") shouldBe true
                    game.graveyardSize(1) shouldBe 4
                }
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 3
            }
        }
    }
}
