package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.pip.cards.TheMasterTranscendent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.matchers.shouldBe

/**
 * The Master, Transcendent — "{T}: Put target creature card in a graveyard that was milled this
 * turn onto the battlefield under your control. It's a green Mutant with base power and toughness
 * 3/3."
 *
 * Proves the three permanent riders land on the returned object: colour replaced by green, creature
 * types replaced by Mutant, base P/T 3/3, under the activator's control.
 */
class TheMasterTranscendentScenarioTest : ScenarioTestBase() {
    private val opponentMills = card("Master Opponent Mills Two") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(2, Player.EachOpponent) } }
    }

    private val abilityId = TheMasterTranscendent.activatedAbilities.first().id

    private fun board(): TestGame = scenario().withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "The Master, Transcendent")
        .withCardInHand(1, opponentMills.name)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Wind Drake").withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.opponentGraveyardDrake(): EntityId = state.getGraveyard(player2Id).first {
        state.getEntity(it)?.get<CardComponent>()?.name == "Wind Drake"
    }

    init {
        cardRegistry.register(opponentMills)

        test("returns a milled blue Drake under your control as a green 3/3 Mutant") {
            val game = board()
            game.castSpell(1, opponentMills.name).error shouldBe null
            game.resolveStack()
            val drake = game.opponentGraveyardDrake()

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("The Master, Transcendent")!!,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Card(drake, game.player2Id, Zone.GRAVEYARD))
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.getBattlefield(game.player1Id).contains(drake) shouldBe true
            val projected = game.state.projectedState
            projected.getController(drake) shouldBe game.player1Id
            projected.getColors(drake) shouldBe setOf("GREEN")
            projected.getSubtypes(drake) shouldBe setOf("Mutant")
            projected.getPower(drake) shouldBe 3
            projected.getToughness(drake) shouldBe 3
        }
    }
}
