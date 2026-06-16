package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Oko, the Ringleader (OTJ #223, {2}{G}{U} planeswalker, loyalty 3).
 *
 *   At the beginning of combat on your turn, Oko becomes a copy of up to one target creature you
 *   control until end of turn, except he has hexproof.
 *   +1: Draw two cards. If you've committed a crime this turn, discard a card. Otherwise, discard two.
 *   -1: Create a 3/3 green Elk creature token.
 *   -5: For each other nonland permanent you control, create a token that's a copy of that permanent.
 */
class OkoTheRingleaderScenarioTest : ScenarioTestBase() {

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** Seed a planeswalker's starting loyalty (withCardOnBattlefield doesn't stamp it). */
    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    init {
        test("-1: create a 3/3 green Elk token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Oko, the Ringleader")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oko = game.findPermanent("Oko, the Ringleader")!!
            seedLoyalty(game, oko, 3)
            loyalty(game, oko) shouldBe 3

            val minusOne = cardRegistry.getCard("Oko, the Ringleader")!!.script.activatedAbilities[1]
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = oko, abilityId = minusOne.id)
            ).error shouldBe null
            game.resolveStack()

            withClue("Oko is at loyalty 2 after -1") { loyalty(game, oko) shouldBe 2 }
            val elks = game.findPermanents("Elk Token").filter {
                game.state.getEntity(it)?.has<TokenComponent>() == true
            }
            elks.size shouldBe 1
            game.state.projectedState.getPower(elks.single()) shouldBe 3
            game.state.projectedState.getToughness(elks.single()) shouldBe 3
        }

        test("+1 without a crime: draw two, discard two") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Oko, the Ringleader")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInHand(1, "Mountain")
                .withCardInHand(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oko = game.findPermanent("Oko, the Ringleader")!!
            seedLoyalty(game, oko, 3)
            val plusOne = cardRegistry.getCard("Oko, the Ringleader")!!.script.activatedAbilities[0]
            val handBefore = game.state.getHand(game.player1Id).size

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = oko, abilityId = plusOne.id)
            ).error shouldBe null
            game.resolveStack()
            // No crime committed → discard two. Drew 2, discarded 2 → net hand unchanged.
            while (game.hasPendingDecision()) {
                game.selectCards(game.state.getHand(game.player1Id).take(2))
                game.resolveStack()
            }
            withClue("Oko is at loyalty 4 after +1") { loyalty(game, oko) shouldBe 4 }
            withClue("drew two, discarded two (no crime) → hand size unchanged") {
                game.state.getHand(game.player1Id).size shouldBe handBefore
            }
        }

        test("at beginning of combat, Oko becomes a copy of a target creature you control, with hexproof") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Oko, the Ringleader")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oko = game.findPermanent("Oko, the Ringleader")!!
            seedLoyalty(game, oko, 3)
            val bear = game.findPermanent("Grizzly Bears")!!

            // Move to beginning of combat; the trigger pauses to choose the optional target creature.
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            if (game.hasPendingDecision()) game.selectTargets(listOf(bear))
            game.resolveStack()

            withClue("Oko became a copy of Grizzly Bears (2/2)") {
                game.state.getEntity(oko)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
                game.state.projectedState.getPower(oko) shouldBe 2
                game.state.projectedState.getToughness(oko) shouldBe 2
            }
            withClue("…except he has hexproof") {
                game.state.projectedState.hasKeyword(oko, com.wingedsheep.sdk.core.Keyword.HEXPROOF) shouldBe true
            }
        }
    }
}
