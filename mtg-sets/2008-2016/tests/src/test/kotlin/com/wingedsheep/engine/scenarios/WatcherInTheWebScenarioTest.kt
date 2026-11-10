package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.WatcherInTheWeb
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Watcher in the Web (SOI #239) — "This creature can block an additional seven creatures each
 * combat."
 *
 * The first card to hand `CanBlockAdditionalForCreatureGroup` a `count` above one, so the pair
 * pins the cap exactly: eight attackers (one plus seven) is a legal block, nine is rejected.
 */
class WatcherInTheWebScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + WatcherInTheWeb)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.attackers(count: Int): List<EntityId> =
        (1..count).map {
            putCreatureOnBattlefield(player1, "Grizzly Bears").also { id -> removeSummoningSickness(id) }
        }

    test("blocks eight attackers at once") {
        val d = driver()
        val p2 = d.getOpponent(d.player1)
        val bears = d.attackers(8)
        val watcher = d.putCreatureOnBattlefield(p2, "Watcher in the Web")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, bears, p2).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("one plus seven additional blocks covers all eight attackers") {
            d.declareBlockers(p2, mapOf(watcher to bears)).error shouldBe null
        }
    }

    test("cannot block a ninth attacker") {
        val d = driver()
        val p2 = d.getOpponent(d.player1)
        val bears = d.attackers(9)
        val watcher = d.putCreatureOnBattlefield(p2, "Watcher in the Web")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, bears, p2).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("nine blocks exceeds the 1 + 7 cap") {
            d.declareBlockers(p2, mapOf(watcher to bears)).error shouldNotBe null
        }
    }
})
