package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.clb.cards.GluntchTheBestower
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gluntch, the Bestower (CLB #275) — "At the beginning of your end step, choose a player. They
 * put two +1/+1 counters on a creature they control. Choose a second player to draw a card. Then
 * choose a third player to create two Treasure tokens."
 *
 * Pinned (rulings 2022-06-10): the three players are all different — each later pick no longer
 * offers the earlier ones; the first player picks their own creature; and with only two players
 * there is no third player, so nobody creates Treasure.
 */
class GluntchTheBestowerScenarioTest : ScenarioTestBase() {

    private fun driver(seats: Int): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(PredefinedTokens.allTokens)
        driver.registerCard(GluntchTheBestower)
        val players = driver.initMultiplayer(decks = List(seats) { Deck.of("Forest" to 40) })
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(players[0], "Gluntch, the Bestower")
        return driver to players
    }

    /** Passes from main phase into the end step until Gluntch's trigger asks for its first player. */
    private fun GameTestDriver.toFirstChoice(): ChooseTargetsDecision {
        passPriorityUntil(Step.END)
        var guard = 0
        while (state.pendingDecision == null && state.stack.isNotEmpty() && guard++ < 20) {
            passPriority(state.priorityPlayerId!!)
        }
        return pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    private fun GameTestDriver.treasures(player: EntityId) =
        getPermanents(player).count { cardName(it)?.startsWith("Treasure") == true }

    private fun GameTestDriver.cardName(id: EntityId) =
        state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name

    private fun GameTestDriver.plusCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("three different players: counters on the first's chosen creature, a draw, two Treasures") {
            val (game, players) = driver(seats = 3)
            val (you, second, third) = players
            val bears = game.putCreatureOnBattlefield(second, "Grizzly Bears")
            val giant = game.putCreatureOnBattlefield(second, "Hill Giant")
            val handBefore = players.associateWith { game.getHandSize(it) }

            val first = game.toFirstChoice()
            withClue("you choose; every player is offered for the first pick") {
                first.playerId shouldBe you
                first.legalTargets[0]!! shouldContainExactlyInAnyOrder players
            }
            game.submitTargetSelection(you, listOf(second))

            val creature = game.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the first chosen player picks the creature among their own") {
                creature.playerId shouldBe second
            }
            game.submitCardSelection(second, listOf(giant))

            val drawPick = game.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("the second pick no longer offers the first chosen player") {
                drawPick.playerId shouldBe you
                drawPick.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(you, third)
            }
            game.submitTargetSelection(you, listOf(third))

            withClue("only one player is left for the third pick, so they are chosen without a prompt") {
                game.pendingDecision shouldBe null
            }

            game.plusCounters(giant) shouldBe 2
            game.plusCounters(bears) shouldBe 0
            withClue("the second chosen player drew a card") {
                game.getHandSize(third) shouldBe handBefore.getValue(third) + 1
                game.getHandSize(you) shouldBe handBefore.getValue(you)
            }
            withClue("the third chosen player — you — created two Treasures") {
                game.treasures(you) shouldBe 2
                game.treasures(third) shouldBe 0
                game.treasures(second) shouldBe 0
            }
        }

        test("with only two players nobody creates Treasure, and the first player may control no creature") {
            val (game, players) = driver(seats = 2)
            val (you, opponent) = players
            val handBefore = game.getHandSize(you)

            game.toFirstChoice()
            game.submitTargetSelection(you, listOf(opponent))

            withClue("the opponent controls no creature, so the counters do nothing and the draw pick is forced") {
                game.pendingDecision shouldBe null
            }
            game.getHandSize(you) shouldBe handBefore + 1
            withClue("no third player exists — no Treasure for anyone, the controller included") {
                game.treasures(you) shouldBe 0
                game.treasures(opponent) shouldBe 0
            }
        }
    }
}
