package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.DesertWereWorm
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Desert Were-Worm {4}{R}{R} — Creature — Dragon Wurm 0/5
 *   This creature gets +2/+0 for each Mountain you control.
 *   Whenever you attack with creatures with total power 12 or greater for the first time each
 *   turn, untap all attacking creatures. After this phase, there is an additional combat phase.
 *
 * The gate is an intervening-"if" over the attackers' total power, and CR 603.4 says a trigger
 * whose condition is false never triggers — so an under-12 swing must leave the `oncePerTurn` cap
 * unspent. These tests pin both sides of that gate, and that the Mountain pump is what feeds the
 * total power in the first place.
 */
class DesertWereWormScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DesertWereWorm))
        return driver
    }

    /**
     * Puts the Were-Worm and [mountains] Mountains onto the battlefield, then attacks alone.
     * Returns the Were-Worm's entity id.
     */
    fun attackAlone(driver: GameTestDriver, mountains: Int): Pair<GameTestDriver, EntityId> {
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(mountains) { driver.putPermanentOnBattlefield(you, "Mountain") }
        val worm = driver.putCreatureOnBattlefield(you, "Desert Were-Worm")
        driver.removeSummoningSickness(worm)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(worm), defendingPlayer = opponent).error shouldBe null
        driver.bothPass() // resolve the attack trigger, if it fired at all
        return driver to worm
    }

    test("six Mountains make it a 12/5, and attacking untaps it and queues an extra combat phase") {
        val (driver, worm) = attackAlone(createDriver(), mountains = 6)
        val you = driver.activePlayer!!

        driver.state.projectedState.getPower(worm) shouldBe 12
        driver.state.projectedState.getToughness(worm) shouldBe 5
        // Declaring it as an attacker tapped it; the trigger untapped it again.
        driver.isTapped(worm) shouldBe false
        driver.state.getEntity(you)?.has<AdditionalPhasesComponent>() shouldBe true
    }

    test("five Mountains leave it at 10 power, below the gate — no untap, no extra combat") {
        val (driver, worm) = attackAlone(createDriver(), mountains = 5)
        val you = driver.activePlayer!!

        driver.state.projectedState.getPower(worm) shouldBe 10
        driver.isTapped(worm) shouldBe true
        driver.state.getEntity(you)?.has<AdditionalPhasesComponent>() shouldBe false
    }
})
