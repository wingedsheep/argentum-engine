package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.matchers.shouldBe

class TatoFarmerScenarioTest : ScenarioTestBase() {
    private val opponentMills = card("Tato Opponent Mills One") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(1, Player.EachOpponent) } }
    }
    private val opponentSurveils = card("Tato Opponent Puts Top Card In Graveyard") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val top = gather(com.wingedsheep.sdk.scripting.effects.CardSource.TopOfLibrary(1, Player.EachOpponent))
                move(top, com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.GRAVEYARD, Player.EachOpponent))
            }
        }
    }

    private fun board(): TestGame = scenario().withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Tato Farmer")
        .withCardInHand(1, opponentMills.name).withCardInHand(1, opponentSurveils.name)
        .withCardInHand(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Mountain").withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.opponentGraveyardMountain(): EntityId = state.getGraveyard(player2Id).first {
        state.getEntity(it)?.get<CardComponent>()?.name == "Mountain"
    }

    private fun TestGame.activate(target: EntityId) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Tato Farmer")!!,
            abilityId = cardRegistry.getCard("Tato Farmer")!!.script.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Card(target, player2Id, Zone.GRAVEYARD))
        )
    )

    init {
        listOf(opponentMills, opponentSurveils).forEach { cardRegistry.register(it) }

        test("puts a land card milled this turn onto the battlefield tapped under your control") {
            val game = board()
            game.castSpell(1, opponentMills.name).error shouldBe null
            game.resolveStack()
            val mountain = game.opponentGraveyardMountain()
            game.activate(mountain).error shouldBe null
            game.resolveStack()
            game.state.getBattlefield(game.player1Id).contains(mountain) shouldBe true
            game.state.projectedState.getController(mountain) shouldBe game.player1Id
            game.state.getEntity(mountain)?.has<TappedComponent>() shouldBe true
        }

        test("a land put into a graveyard from the library without milling is not a legal target") {
            val game = board()
            game.castSpell(1, opponentSurveils.name).error shouldBe null
            game.resolveStack()
            val mountain = game.opponentGraveyardMountain()
            (game.activate(mountain).error != null) shouldBe true
        }

        test("landfall: you may get two rad counters") {
            val game = board()
            val forest = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
            }
            game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.state.getEntity(game.player1Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) shouldBe 2
        }
    }
}
