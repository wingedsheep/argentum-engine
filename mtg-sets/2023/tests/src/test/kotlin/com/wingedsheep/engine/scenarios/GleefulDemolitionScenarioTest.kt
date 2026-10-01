package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gleeful Demolition — {R} Sorcery (ONE #134).
 *
 * "Destroy target artifact. If you controlled that artifact, create three 1/1 red Phyrexian
 *  Goblin creature tokens."
 */
class GleefulDemolitionScenarioTest : ScenarioTestBase() {

    private fun board(artifactOwner: Int, artifact: String): TestGame = scenario()
        .withPlayers()
        .withCardOnBattlefield(artifactOwner, artifact)
        .withCardInHand(1, "Gleeful Demolition")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("destroying your own artifact creates three Phyrexian Goblins") {
            val game = board(1, "Ornithopter")
            val target = game.findPermanent("Ornithopter")!!

            game.castSpell(1, "Gleeful Demolition", target).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Ornithopter") shouldBe true
            val goblins = game.findPermanents("Phyrexian Goblin Token")
            goblins.size shouldBe 3
            goblins.all { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id } shouldBe true
        }

        test("destroying an opponent's artifact creates no tokens") {
            val game = board(2, "Ornithopter")
            val target = game.findPermanent("Ornithopter")!!

            game.castSpell(1, "Gleeful Demolition", target).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            game.findPermanents("Phyrexian Goblin Token").size shouldBe 0
        }

        test("an indestructible artifact you control survives but still yields the Goblins") {
            val game = board(1, "Darksteel Citadel")
            val target = game.findPermanent("Darksteel Citadel")!!

            game.castSpell(1, "Gleeful Demolition", target).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Darksteel Citadel") shouldBe true
            game.findPermanents("Phyrexian Goblin Token").size shouldBe 3
        }
    }
}
