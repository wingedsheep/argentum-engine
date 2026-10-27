package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sarulf, Realm Eater (KHM #228) — {1}{B}{G} Legendary Creature — Wolf 3/3.
 *
 *   Whenever a permanent an opponent controls is put into a graveyard from the battlefield, put a
 *   +1/+1 counter on Sarulf.
 *   At the beginning of your upkeep, if Sarulf has one or more +1/+1 counters on it, you may remove
 *   all of them. If you do, exile each other nonland permanent with mana value less than or equal
 *   to the number of counters removed this way.
 */
class SarulfRealmEaterScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.sarulfCounters(): Int =
        state.getEntity(findPermanent("Sarulf, Realm Eater")!!)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun ScenarioTestBase.TestGame.giveSarulfCounters(n: Int) {
        val sarulf = findPermanent("Sarulf, Realm Eater")!!
        state = state.updateEntity(sarulf) {
            it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, n))
        }
    }

    private fun upkeepBoard() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Sarulf, Realm Eater")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Llanowar Elves")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Forest")
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)

    init {
        test("a permanent an opponent controls dying puts a +1/+1 counter on Sarulf; your own doesn't") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sarulf, Realm Eater")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Llanowar Elves")!!).error shouldBe null
            game.resolveStack()
            game.sarulfCounters() shouldBe 1

            game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            withClue("your own permanent dying doesn't count") { game.sarulfCounters() shouldBe 1 }
        }

        test("removing the counters exiles each other nonland permanent with small enough mana value") {
            val game = upkeepBoard().build()
            game.giveSarulfCounters(2)

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.resolveStack()

            withClue("Sarulf stays and loses all its +1/+1 counters") {
                game.findPermanent("Sarulf, Realm Eater") shouldNotBe null
                game.sarulfCounters() shouldBe 0
            }
            withClue("mana value 2 or less is exiled, on either side") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Llanowar Elves") shouldBe true
            }
            withClue("bigger permanents and lands stay") {
                game.findPermanent("Hill Giant") shouldNotBe null
                game.findPermanent("Forest") shouldNotBe null
            }
        }

        test("declining keeps the counters and exiles nothing") {
            val game = upkeepBoard().build()
            game.giveSarulfCounters(2)

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.sarulfCounters() shouldBe 2
            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.findPermanent("Llanowar Elves") shouldNotBe null
        }

        test("with no +1/+1 counters the upkeep ability doesn't trigger") {
            val game = upkeepBoard().build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.findPermanent("Llanowar Elves") shouldNotBe null
        }
    }
}
