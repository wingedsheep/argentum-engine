package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * The Nipton Lottery (PIP) — "Choose a creature at random. You gain control of that creature until
 * end of turn. Untap it. It gains haste until end of turn. Then destroy all other creatures."
 *
 * The pick is the engine's seeded RNG (`chooseRandomSplit`), so the test asserts the invariant
 * across seeds — exactly one survivor, stolen, untapped and hasted, everything else destroyed — and
 * that different seeds really pick different winners. Control returns at end of turn.
 */
class TheNiptonLotteryScenarioTest : ScenarioTestBase() {

    private fun ticket(name: String) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Human Citizen"
        power = 1
        toughness = 1
    }

    private val mine = ticket("Nipton Test Ticket Mine")
    private val theirsA = ticket("Nipton Test Ticket Alpha")
    private val theirsB = ticket("Nipton Test Ticket Beta")
    private val names = listOf(mine.name, theirsA.name, theirsB.name)

    private fun drawLottery(seed: Long): TestGame {
        val game = scenario()
            .withPlayers("You", "Opponent")
            .withCardInHand(1, "The Nipton Lottery")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(1, mine.name, tapped = true)
            .withCardOnBattlefield(2, theirsA.name, tapped = true)
            .withCardOnBattlefield(2, theirsB.name, tapped = true)
            .withRngSeed(seed)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "The Nipton Lottery").error shouldBe null
        if (game.getPendingDecision()?.let { it::class.simpleName?.contains("Mana") } == true) {
            game.submitManaSourcesAutoPay()
        }
        game.resolveStack()
        return game
    }

    init {
        listOf(mine, theirsA, theirsB).forEach { cardRegistry.register(it) }

        test("one random creature survives — stolen, untapped and hasted — and the rest are destroyed") {
            val winners = mutableSetOf<String>()
            for (seed in 1L..16L) {
                val game = drawLottery(seed)
                val survivors = names.filter { game.findPermanent(it) != null }
                withClue("seed $seed: exactly one creature survives") { survivors shouldHaveSize 1 }
                val winnerName = survivors.single()
                winners += winnerName
                val winner = game.findPermanent(winnerName)!!

                withClue("seed $seed: you control the winner") {
                    game.state.projectedState.getController(winner) shouldBe game.player1Id
                }
                withClue("seed $seed: the winner is untapped") {
                    game.state.getEntity(winner)?.has<TappedComponent>() shouldBe false
                }
                withClue("seed $seed: the winner has haste") {
                    game.state.projectedState.hasKeyword(winner, Keyword.HASTE) shouldBe true
                }
                val ownGraveyard = game.findCardsInGraveyard(1, mine.name).size
                val opponentGraveyard = game.findCardsInGraveyard(2, theirsA.name).size +
                    game.findCardsInGraveyard(2, theirsB.name).size
                withClue("seed $seed: the two others went to their owners' graveyards") {
                    (ownGraveyard + opponentGraveyard) shouldBe 2
                }
            }
            withClue("the pick is random, not fixed: different seeds chose $winners") {
                winners.size shouldBeGreaterThan 1
            }
        }

        test("a stolen winner returns to its owner at end of turn") {
            val seed = (1L..32L).first { seed ->
                drawLottery(seed).findPermanent(theirsA.name) != null
            }
            val game = drawLottery(seed)
            val winner = game.findPermanent(theirsA.name)!!
            game.state.projectedState.getController(winner) shouldBe game.player1Id

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.getController(winner) shouldBe game.player2Id
        }
    }
}
