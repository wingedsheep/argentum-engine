package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Drowner of Truth // Drowned Jungle (MH3): the cast trigger makes two Eldrazi Spawn only when {C}
 * was spent, and the land back enters tapped.
 */
class DrownerOfTruthScenarioTest : ScenarioTestBase() {

    init {
        test("spending {C} creates two Eldrazi Spawn") {
            // Exactly seven sources, one colorless: the solver must spend the {C}.
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Drowner of Truth")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withLandsOnBattlefield(1, "Snow-Covered Wastes", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Drowner of Truth").error shouldBe null
            game.state.stack.size shouldBe 2
            game.resolveStack()
            game.findPermanents("Eldrazi Spawn").size shouldBe 2
            game.isOnBattlefield("Drowner of Truth") shouldBe true
        }

        test("paying with only colored mana creates no Spawn") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Drowner of Truth")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Drowner of Truth").error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.findPermanents("Eldrazi Spawn").size shouldBe 0
            game.isOnBattlefield("Drowner of Truth") shouldBe true
        }

        test("Drowned Jungle enters tapped when played as a land") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Drowner of Truth")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val card = game.state.getHand(game.player1Id).single()
            game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
            val land = game.findPermanent("Drowned Jungle")!!
            game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
        }
    }
}
