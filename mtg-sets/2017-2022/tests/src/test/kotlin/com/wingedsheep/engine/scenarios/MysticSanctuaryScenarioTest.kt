package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mystic Sanctuary: enters tapped unless you control three or more other Islands, and only an
 * untapped entry triggers the graveyard-to-library ability.
 */
class MysticSanctuaryScenarioTest : ScenarioTestBase() {
    private fun sanctuaryGame(islands: Int) = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Mystic Sanctuary")
        .withLandsOnBattlefield(1, "Island", islands)
        .withCardInGraveyard(1, "Lightning Bolt")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(2, "Shock")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("with three other Islands it enters untapped and puts an instant from your graveyard on top") {
            val game = sanctuaryGame(islands = 3)
            val sanctuary = game.findCardsInHand(1, "Mystic Sanctuary").single()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

            game.execute(PlayLand(game.player1Id, sanctuary)).error shouldBe null
            game.state.getEntity(sanctuary)!!.has<TappedComponent>() shouldBe false

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            // Only instant or sorcery cards you own — not the creature, not the opponent's Shock.
            decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(bolt)
            game.selectTargets(listOf(bolt)).error shouldBe null
            game.resolveStack()

            game.state.getLibrary(game.player1Id).first() shouldBe bolt
            game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 0
        }

        test("with only two other Islands it enters tapped and does not trigger") {
            val game = sanctuaryGame(islands = 2)
            val sanctuary = game.findCardsInHand(1, "Mystic Sanctuary").single()
            val library = game.state.getLibrary(game.player1Id)

            game.execute(PlayLand(game.player1Id, sanctuary)).error shouldBe null
            game.state.getEntity(sanctuary)!!.has<TappedComponent>() shouldBe true
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.state.getLibrary(game.player1Id) shouldBe library
            game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 1
        }
    }
}
