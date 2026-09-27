package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Feast of Worms — {3}{G}{G} Sorcery — Arcane (Champions of Kamigawa #207)
 *
 * "Destroy target land. If that land was legendary, its controller sacrifices another land of
 *  their choice."
 */
class FeastOfWormsScenarioTest : ScenarioTestBase() {

    private fun baseBuilder() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Feast of Worms")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Feast of Worms") {

            test("destroying a legendary land makes its controller sacrifice another land of their choice") {
                val game = baseBuilder()
                    .withCardOnBattlefield(2, "Eiganjo Castle")
                    .withCardOnBattlefield(2, "Plains")
                    .withCardOnBattlefield(2, "Island")
                    .build()

                val castle = game.findPermanent("Eiganjo Castle")!!
                val cast = game.castSpell(1, "Feast of Worms", castle)
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("The destroyed land's controller chooses") {
                    decision.playerId shouldBe game.player2Id
                }
                val plains = game.findPermanent("Plains")!!
                val island = game.findPermanent("Island")!!
                withClue("Only the controller's other lands are offered") {
                    decision.options shouldContainExactlyInAnyOrder listOf(plains, island)
                }
                game.selectCards(listOf(island))

                game.isInGraveyard(2, "Eiganjo Castle") shouldBe true
                game.isInGraveyard(2, "Island") shouldBe true
                game.isOnBattlefield("Plains") shouldBe true
                withClue("Caster's lands are untouched") {
                    game.findPermanents("Forest").size shouldBe 5
                }
            }

            test("a legendary land whose controller has no other land is just destroyed") {
                val game = baseBuilder()
                    .withCardOnBattlefield(2, "Eiganjo Castle")
                    .build()

                val castle = game.findPermanent("Eiganjo Castle")!!
                game.castSpell(1, "Feast of Worms", castle).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Eiganjo Castle") shouldBe true
                game.findPermanents("Forest").size shouldBe 5
            }

            test("a nonlegendary land is destroyed with no sacrifice") {
                val game = baseBuilder()
                    .withCardOnBattlefield(2, "Plains")
                    .withCardOnBattlefield(2, "Island")
                    .build()

                val plains = game.findPermanent("Plains")!!
                game.castSpell(1, "Feast of Worms", plains).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Plains") shouldBe true
                game.isOnBattlefield("Island") shouldBe true
            }
        }
    }
}
