package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sarek, Federation Ambassador ({1}{G}{W}{U} Legendary Creature — Vulcan Advisor, 0/5):
 * "Federation — At the beginning of combat on your turn, draw a card for every five creature types
 * among non-Borg creatures you control. If you draw three or more cards this way, put two +1/+1
 * counters on each creature you control."
 */
class SarekFederationAmbassadorScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // Sarek (Vulcan, Advisor) + these = 14 creature types.
    private val fourteenTypes = listOf(
        "Grizzly Bears",   // Bear
        "Hill Giant",      // Giant
        "Elite Vanguard",  // Human Soldier
        "Llanowar Elves",  // Elf Druid
        "Raging Goblin",   // Goblin Berserker
        "Merfolk Looter",  // Merfolk Rogue
        "Storm Crow",      // Bird
        "Scathe Zombies",  // Zombie
    )

    private fun board(creatures: List<String>): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Sarek, Federation Ambassador")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        creatures.forEach { builder = builder.withCardOnBattlefield(1, it) }
        repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
        repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    init {
        test("fifteen creature types draws three cards and puts two +1/+1 counters on each creature") {
            val game = board(fourteenTypes + "Savannah Lions") // + Cat = 15
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            withClue("15 types / 5 = 3 cards drawn") {
                game.handSize(1) shouldBe handBefore + 3
            }
            withClue("three cards drawn puts two +1/+1 counters on each creature you control") {
                plusOneCounters(game, game.findPermanent("Sarek, Federation Ambassador")!!) shouldBe 2
                plusOneCounters(game, game.findPermanent("Savannah Lions")!!) shouldBe 2
                plusOneCounters(game, game.findPermanent("Grizzly Bears")!!) shouldBe 2
            }
        }

        test("fourteen creature types draws two cards and puts no counters") {
            val game = board(fourteenTypes)
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            withClue("14 types / 5 rounds down to 2 cards drawn") {
                game.handSize(1) shouldBe handBefore + 2
            }
            withClue("fewer than three cards drawn: no counters") {
                plusOneCounters(game, game.findPermanent("Sarek, Federation Ambassador")!!) shouldBe 0
                plusOneCounters(game, game.findPermanent("Grizzly Bears")!!) shouldBe 0
            }
        }
    }
}
