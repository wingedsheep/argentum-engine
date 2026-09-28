package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nahiri's Warcrafting (March of the Machine #155): 5 damage; excess = X cards looked at.
 */
class NahirisWarcraftingScenarioTest : ScenarioTestBase() {

    private fun game(creature: String) = scenario().withPlayers()
        .withCardInHand(1, "Nahiri's Warcrafting")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardOnBattlefield(2, creature)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("no excess damage looks at no cards and exiles nothing") {
            val game = game("Colossal Dreadmaw") // 6/6 survives? 5 damage, no excess
            val libBefore = game.librarySize(1)
            game.castSpell(1, "Nahiri's Warcrafting", game.findPermanent("Colossal Dreadmaw")!!).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.librarySize(1) shouldBe libBefore
        }

        test("excess damage lets you exile one of the top X cards") {
            val game = game("Grizzly Bears") // 2/2 -> 3 excess
            val libBefore = game.librarySize(1)
            game.castSpell(1, "Nahiri's Warcrafting", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.selectCards(listOf(game.state.getLibrary(game.player1Id).first()))
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.librarySize(1) shouldBe libBefore - 1
            game.isInExile(1, "Island") shouldBe true
        }
    }
}
