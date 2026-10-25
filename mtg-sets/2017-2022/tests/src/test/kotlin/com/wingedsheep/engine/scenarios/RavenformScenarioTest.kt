package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ravenform (KHM #72) — exile target artifact or creature; its controller creates a 1/1 blue
 * Bird with flying. The controller is read after the exile, so it relies on last-known info.
 */
class RavenformScenarioTest : ScenarioTestBase() {

    private fun board(victim: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Ravenform")
        .withCardOnBattlefield(2, victim)
        .withLandsOnBattlefield(1, "Island", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("exiles an opponent's creature and gives that opponent a 1/1 flying Bird") {
            val game = board("Hill Giant")
            game.castSpell(1, "Ravenform", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            val bird = game.findAllPermanents("Bird Token").single()
            game.state.getEntity(bird)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
            game.state.projectedState.getPower(bird) shouldBe 1
            game.state.projectedState.getToughness(bird) shouldBe 1
            game.state.projectedState.hasKeyword(bird, Keyword.FLYING) shouldBe true
        }

        test("an artifact is a legal target too") {
            val game = board("Ornithopter")
            game.castSpell(1, "Ravenform", game.findPermanent("Ornithopter")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Ornithopter") shouldBe true
            game.findAllPermanents("Bird Token").size shouldBe 1
        }
    }
}
