package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Cavalier of Dawn — "destroy up to one target nonland permanent. Its controller creates a 3/3
 * colorless Golem artifact creature token." The Golem goes to the destroyed permanent's
 * controller; with no target chosen nobody gets one (2025-02-07 ruling), rather than the
 * Cavalier's controller.
 */
class CavalierOfDawnScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Cavalier of Dawn")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castCavalierTargeting(targets: List<com.wingedsheep.sdk.model.EntityId>) {
        castSpell(1, "Cavalier of Dawn").error shouldBe null
        resolveStack()
        val decision = state.pendingDecision as? ChooseTargetsDecision
            ?: error("expected the enters trigger's target choice; got ${state.pendingDecision}")
        submitDecision(TargetsResponse(decision.id, mapOf(0 to targets))).error shouldBe null
        resolveStack()
    }

    init {
        test("destroying an opponent's permanent gives that opponent the Golem") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castCavalierTargeting(listOf(bears))

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            val golems = game.findPermanents("Golem Token")
            golems shouldHaveSize 1
            game.state.projectedState.getController(golems.single()) shouldBe game.player2Id
            game.state.projectedState.getPower(golems.single()) shouldBe 3
            game.state.projectedState.isCreature(golems.single()) shouldBe true
        }

        test("choosing no target destroys nothing and creates no Golem for anyone") {
            val game = board()

            game.castCavalierTargeting(emptyList())

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.findPermanents("Golem Token").shouldBeEmpty()
        }
    }
}
