package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BridledBighorn
import com.wingedsheep.mtg.sets.definitions.otj.cards.MiriamHerdWhisperer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Miriam, Herd Whisperer — {G}{W} 3/2 Legendary Creature — Human Druid
 *
 * "During your turn, Mounts and Vehicles you control have hexproof."
 * "Whenever a Mount or Vehicle you control attacks, put a +1/+1 counter on it."
 *
 * Uses Bridled Bighorn (a Sheep Mount with vigilance) as the Mount under test.
 */
class MiriamHerdWhispererScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MiriamHerdWhisperer, BridledBighorn))
        return driver
    }

    fun plusCounters(driver: GameTestDriver, entity: EntityId): Int =
        driver.state.getEntity(entity)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("Mount you control gains hexproof during your turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Miriam, Herd Whisperer")
        val mount = driver.putCreatureOnBattlefield(me, "Bridled Bighorn")

        // It's my turn: the Mount has hexproof.
        driver.state.projectedState.hasKeyword(mount, Keyword.HEXPROOF) shouldBe true
    }

    test("Mount does NOT have hexproof on the opponent's turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Miriam + Mount belong to the OPPONENT (non-active player). It's MY turn, not theirs,
        // so the conditional "during your turn" grant is inactive for the opponent's Mount.
        driver.putCreatureOnBattlefield(opp, "Miriam, Herd Whisperer")
        val mount = driver.putCreatureOnBattlefield(opp, "Bridled Bighorn")

        driver.state.projectedState.hasKeyword(mount, Keyword.HEXPROOF) shouldBe false
    }

    test("Miriam herself (a Human, not a Mount/Vehicle) does not gain hexproof") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val miriam = driver.putCreatureOnBattlefield(me, "Miriam, Herd Whisperer")

        driver.state.projectedState.hasKeyword(miriam, Keyword.HEXPROOF) shouldBe false
    }

    test("attacking with a Mount you control puts a +1/+1 counter on it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.player1
        val opp = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Miriam, Herd Whisperer")
        val mount = driver.putCreatureOnBattlefield(me, "Bridled Bighorn")
        driver.removeSummoningSickness(mount)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(mount), opp)
        driver.bothPass() // resolve Miriam's attack trigger

        // Bridled Bighorn (3/4) gains one +1/+1 counter from Miriam's attack trigger.
        plusCounters(driver, mount) shouldBe 1
    }
})
