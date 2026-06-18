package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Glorious Gale — "Counter target creature spell. If it was a legendary spell, the Ring tempts you."
 * Verifies the legendary check on the targeted spell-on-stack (the only non-obvious composition).
 * Player 1 controls no creatures, so the temptation increments the count without pausing to choose a
 * Ring-bearer.
 */
class GloriousGaleScenarioTest : ScenarioTestBase() {

    private fun gale(opponentSpell: String) = scenario()
        .withPlayers()
        .withCardInHand(1, "Glorious Gale")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(2, opponentSpell)
        .withCardOnBattlefield(2, "Island")
        .withCardOnBattlefield(2, "Forest")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("counters a legendary creature spell and the Ring tempts you") {
            val game = gale("Naban, Dean of Iteration") // {1}{U} legendary creature
            game.castSpell(2, "Naban, Dean of Iteration").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(1, "Glorious Gale", "Naban, Dean of Iteration").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Naban, Dean of Iteration") shouldBe false
            game.isInGraveyard(2, "Naban, Dean of Iteration") shouldBe true
            (game.state.getEntity(game.player1Id)?.get<TheRingComponent>()?.temptCount ?: 0) shouldBe 1
        }

        test("counters a nonlegendary creature spell without tempting you") {
            val game = gale("Grizzly Bears") // {1}{G} nonlegendary creature
            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(1, "Glorious Gale", "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            (game.state.getEntity(game.player1Id)?.get<TheRingComponent>()?.temptCount ?: 0) shouldBe 0
        }
    }
}
