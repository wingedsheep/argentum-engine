package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cityscape Leveler (BRO #233) — "When you cast this spell and whenever this creature attacks,
 * destroy up to one target nonland permanent. Its controller creates a tapped Powerstone token."
 *
 * The cast trigger resolves while the Leveler is still on the stack. The Powerstone goes to the
 * destroyed permanent's controller, and choosing no target creates no Powerstone at all.
 */
class CityscapeLevelerScenarioTest : ScenarioTestBase() {

    private fun build() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Cityscape Leveler")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 8)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast trigger destroys the target and its controller gets a tapped Powerstone") {
            val game = build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Cityscape Leveler").error shouldBe null
            withClue("the cast trigger asks for its target") { game.hasPendingDecision() shouldBe true }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Cityscape Leveler") shouldBe true

            val powerstone = game.findPermanent("Powerstone")
            powerstone shouldNotBe null
            withClue("the destroyed permanent's controller gets the Powerstone, tapped") {
                game.state.projectedState.getController(powerstone!!) shouldBe game.player2Id
                game.state.getEntity(powerstone)!!.has<TappedComponent>() shouldBe true
            }
        }

        test("choosing no target creates no Powerstone") {
            val game = build()

            game.castSpell(1, "Cityscape Leveler").error shouldBe null
            game.skipTargets().error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Cityscape Leveler") shouldBe true
            withClue("no permanent was destroyed, so nobody gets a Powerstone") {
                game.findPermanent("Powerstone") shouldBe null
            }
        }
    }
}
