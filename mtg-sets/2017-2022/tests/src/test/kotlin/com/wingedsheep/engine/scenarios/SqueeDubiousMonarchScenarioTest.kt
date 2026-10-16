package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
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

    private fun game(mountains: Int, otherCards: Int, muldrotha: Boolean = false) = scenario()
        .withPlayers()
        .let { if (muldrotha) it.withCardOnBattlefield(1, "Muldrotha, the Gravetide") else it }
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
            CastSpell(
                player1Id, squee, useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ZONE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder)
            )
        )
    }

    init {
        test("cast from the graveyard for {3}{R} by exiling four other graveyard cards") {
            val game = game(mountains = 4, otherCards = 5)
            val squee = game.findCardsInGraveyard(1, "Squee, Dubious Monarch").single()

            val offered = game.getLegalActions(1).single { (it.action as? CastSpell)?.cardId == squee }
            offered.manaCostString shouldBe "{3}{R}"
            offered.isAffordable shouldBe true
            (offered.action as CastSpell).alternativeCostType shouldBe AlternativeCostType.SELF_ZONE

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

        test("another graveyard permission still casts it for its printed {2}{R}, exiling nothing") {
            val game = game(mountains = 3, otherCards = 0, muldrotha = true)
            val squee = game.findCardsInGraveyard(1, "Squee, Dubious Monarch").single()

            val offers = game.getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == squee }
            offers.any { (it.action as CastSpell).alternativeCostType == null && it.isAffordable } shouldBe true

            game.execute(CastSpell(game.player1Id, squee)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Squee, Dubious Monarch") shouldNotBe null
        }
    }
}
