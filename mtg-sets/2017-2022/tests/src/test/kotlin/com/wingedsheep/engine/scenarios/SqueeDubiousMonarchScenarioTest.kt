package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Squee, Dubious Monarch — "You may cast this card from your graveyard by paying {3}{R} and
 * exiling four other cards from your graveyard rather than paying its mana cost." A keywordless
 * alternative cost (`MayCastSelfFromZones.alternativeManaCost` + its exile-four non-mana half),
 * not escape.
 */
class SqueeDubiousMonarchScenarioTest : ScenarioTestBase() {

    private fun game(mountains: Int, otherCards: Int) = scenario()
        .withPlayers()
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withCardInGraveyard(1, "Squee, Dubious Monarch")
        .let { (1..otherCards).fold(it) { b, _ -> b.withCardInGraveyard(1, "Grizzly Bears") } }
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castSquee(): com.wingedsheep.engine.core.ExecutionResult {
        val squee = findCardsInGraveyard(1, "Squee, Dubious Monarch").single()
        val fodder = findCardsInGraveyard(1, "Grizzly Bears").take(4)
        return execute(
            CastSpell(player1Id, squee, additionalCostPayment = AdditionalCostPayment(exiledCards = fodder))
        )
    }

    init {
        test("cast from the graveyard for {3}{R} by exiling four other graveyard cards") {
            val game = game(mountains = 4, otherCards = 5)
            val squee = game.findCardsInGraveyard(1, "Squee, Dubious Monarch").single()

            val offered = game.getLegalActions(1).single { (it.action as? CastSpell)?.cardId == squee }
            offered.manaCostString shouldBe "{3}{R}"
            offered.isAffordable shouldBe true

            game.castSquee().error shouldBe null
            game.resolveStack()

            game.findPermanent("Squee, Dubious Monarch") shouldNotBe null
            game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
            game.graveyardSize(1) shouldBe 1
        }

        test("three mountains can't pay {3}{R}") {
            val game = game(mountains = 3, otherCards = 4)

            game.castSquee().error shouldNotBe null
            game.isInGraveyard(1, "Squee, Dubious Monarch") shouldBe true
        }

        test("needs four other cards in the graveyard") {
            val game = game(mountains = 4, otherCards = 3)
            val squee = game.findCardsInGraveyard(1, "Squee, Dubious Monarch").single()

            game.getLegalActions(1).single { (it.action as? CastSpell)?.cardId == squee }.isAffordable shouldBe false
            game.castSquee().error shouldNotBe null
        }
    }
}
