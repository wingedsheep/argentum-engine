package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Stinging Study (C21 #44) — "You draw X cards and you lose X life, where X is the mana value of a
 * commander you own on the battlefield or in the command zone."
 *
 * Covers the command-zone read, the choice between several commanders you own (including one
 * another player has taken, per the ruling, while an opponent's own commander is never offered),
 * and the no-commander case (X = 0).
 */
class StingingStudyScenarioTest : ScenarioTestBase() {

    private fun TestGame.playerId(player: Int): EntityId = if (player == 1) player1Id else player2Id

    private fun TestGame.commandCard(player: Int, name: String): EntityId =
        state.getZone(ZoneKey(playerId(player), Zone.COMMAND))
            .single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.makeCommander(id: EntityId, owner: Int) {
        state = state.updateEntity(id) { it.with(CommanderComponent(ownerId = playerId(owner))) }
    }

    private fun base(): ScenarioBuilder {
        var builder = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Stinging Study")
            .withLandsOnBattlefield(1, "Swamp", 5)
            .withFormat(Format.Commander())
            .withLifeTotal(1, 20)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(8) { builder = builder.withCardInLibrary(1, "Swamp") }
        repeat(2) { builder = builder.withCardInLibrary(2, "Swamp") }
        return builder
    }

    init {
        test("X is the mana value of your commander in the command zone") {
            val game = base().withCardInCommandZone(1, "Hill Giant").build()
            game.makeCommander(game.commandCard(1, "Hill Giant"), owner = 1)
            val handBefore = game.handSize(1)

            game.castSpell(1, "Stinging Study").error shouldBe null
            game.resolveStack()
            (game.getPendingDecision() as? SelectCardsDecision)?.let {
                game.selectCards(it.options.take(1)).error shouldBe null
                game.resolveStack()
            }

            withClue("drew 4 (Hill Giant's mana value) and lost 4 life") {
                game.handSize(1) shouldBe handBefore - 1 + 4
                game.getLifeTotal(1) shouldBe 16
            }
        }

        test("several commanders you own: you choose one, a stolen one counts, an opponent's never does") {
            val game = base()
                .withCardInCommandZone(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInCommandZone(2, "Serra Angel")
                .build()
            val bears = game.commandCard(1, "Grizzly Bears")
            val giant = game.findPermanent("Hill Giant")!!
            game.makeCommander(bears, owner = 1)
            game.makeCommander(giant, owner = 1)
            game.makeCommander(game.commandCard(2, "Serra Angel"), owner = 2)
            // Bob has taken Alice's Hill Giant — it is still a commander she owns.
            game.state = game.state.updateEntity(giant) { it.with(ControllerComponent(game.player2Id)) }
            val handBefore = game.handSize(1)

            game.castSpell(1, "Stinging Study").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only commanders Alice owns are offered, wherever they are and whoever controls them") {
                decision.options shouldContainExactlyInAnyOrder listOf(bears, giant)
            }
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore - 1 + 4
            game.getLifeTotal(1) shouldBe 16
        }

        test("no commander you own: X is 0") {
            val game = base().withCardInCommandZone(2, "Serra Angel").build()
            game.makeCommander(game.commandCard(2, "Serra Angel"), owner = 2)
            val handBefore = game.handSize(1)

            game.castSpell(1, "Stinging Study").error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore - 1
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
