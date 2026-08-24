package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.GoblinFlotilla
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Goblin Flotilla (Fallen Empires).
 *
 * The {R} is protection money: paying buys the Flotilla out of the drawback for that combat, and
 * declining installs a "this combat" watcher that hands first strike to whatever ends up fighting
 * it. Both halves are exercised here, since getting the polarity backwards is the obvious way to
 * write this card wrong.
 */
class GoblinFlotillaScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GoblinFlotilla)
        return driver
    }

    test("declining the {R} gives the blocker first strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flotilla = driver.putCreatureOnBattlefield(alice, "Goblin Flotilla")
        driver.removeSummoningSickness(flotilla)
        val blocker = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        // Alice controls no red source, so the {R} is unpayable and the trigger goes straight to
        // its "suffer" half without asking — the rider is installed with no prompt at all.
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(flotilla), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(blocker to listOf(flotilla)))
        driver.bothPass()

        withClue("the blocker picked up first strike from the unpaid rider") {
            projector.project(driver.state).hasKeyword(blocker, Keyword.FIRST_STRIKE) shouldBe true
        }
    }

    test("paying the {R} keeps the blocker ordinary") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flotilla = driver.putCreatureOnBattlefield(alice, "Goblin Flotilla")
        driver.removeSummoningSickness(flotilla)
        val blocker = driver.putCreatureOnBattlefield(bob, "Elvish Warrior")

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        // Float the {R} here rather than earlier — a mana pool empties at every step boundary — so
        // the payment comes straight out of the pool with no source selection in between.
        driver.giveMana(alice, com.wingedsheep.sdk.core.Color.RED, 1)
        driver.bothPass()
        driver.submitYesNo(alice, true)

        withClue("paying installs no rider; delayed triggers = ${driver.state.delayedTriggers.size}") {
            driver.state.delayedTriggers.isEmpty() shouldBe true
        }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(flotilla), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(blocker to listOf(flotilla)))
        driver.bothPass()

        withClue("the {R} bought the Flotilla out of the drawback") {
            projector.project(driver.state).hasKeyword(blocker, Keyword.FIRST_STRIKE) shouldBe false
        }
    }
})
