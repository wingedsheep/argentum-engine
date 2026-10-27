package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.DungeonProgressComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * White Plume Adventurer (CLB #49): the ETB takes the initiative (and so ventures into Undercity),
 * and the opponent's-upkeep trigger untaps one chosen creature — or every creature you control once
 * you've completed a dungeon.
 */
class WhitePlumeAdventurerScenarioTest : ScenarioTestBase() {

    init {
        test("entering takes the initiative and ventures into Undercity") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "White Plume Adventurer")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "White Plume Adventurer").error shouldBe null
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.state.pendingDecision != null) && guard++ < 20) {
                when (val d = game.state.pendingDecision) {
                    null -> game.passPriority()
                    is SearchLibraryDecision -> game.submitDecision(CardsSelectedResponse(d.id, emptyList()))
                    // Undercity's search is asked even when it can find nothing (#3340).
                    is com.wingedsheep.engine.core.SelectCardsDecision -> game.submitDecision(CardsSelectedResponse(d.id, emptyList()))
                    else -> error("Unexpected decision $d")
                }
            }

            game.state.initiative shouldBe game.player1Id
            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Undercity"
        }

        test("at each opponent's upkeep it untaps one creature you control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "White Plume Adventurer")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            val giant = game.findPermanent("Hill Giant")!!
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("Only the chosen creature untapped") {
                game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe false
                game.state.getEntity(game.findPermanent("Grizzly Bears")!!)!!.has<TappedComponent>() shouldBe true
            }
            decision.playerId shouldBe game.player1Id
        }

        test("once you've completed a dungeon it untaps all creatures you control instead") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "White Plume Adventurer")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(DungeonProgressComponent(completed = listOf("Tomb of Annihilation")))
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.state.getEntity(game.findPermanent("Hill Giant")!!)!!.has<TappedComponent>() shouldBe false
            val bears = game.findAllPermanents("Grizzly Bears")
            val mine = bears.single { game.state.getEntity(it)!!.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()!!.playerId == game.player1Id }
            game.state.getEntity(mine)!!.has<TappedComponent>() shouldBe false
        }
    }
}
