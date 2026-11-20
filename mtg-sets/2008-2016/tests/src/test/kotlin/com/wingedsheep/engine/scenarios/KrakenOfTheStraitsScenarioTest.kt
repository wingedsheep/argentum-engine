package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.KrakenOfTheStraits
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kraken of the Straits (Born of the Gods #42) — {5}{U}{U} 6/6.
 * "Creatures with power less than the number of Islands you control can't block this creature."
 *
 * "Less than" is strict: with three Islands a 2-power creature can't block, a 3-power one can.
 * Only the Kraken controller's Islands count — the defender's Islands don't.
 */
class KrakenOfTheStraitsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + KrakenOfTheStraits)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Put the Kraken in play for [me], attack with it, and advance to declare blockers. */
    fun GameTestDriver.attackWithKraken(me: EntityId, opponent: EntityId): EntityId {
        val kraken = putCreatureOnBattlefield(me, "Kraken of the Straits")
        removeSummoningSickness(kraken)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(kraken), defendingPlayer = opponent).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        return kraken
    }

    test("with three Islands, a 2-power creature can't block it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        repeat(3) { driver.putLandOnBattlefield(me, "Island") }
        val guide = driver.putCreatureOnBattlefield(opponent, "Goblin Guide") // 2/1

        val kraken = driver.attackWithKraken(me, opponent)

        driver.declareBlockers(opponent, mapOf(guide to listOf(kraken))).error.shouldNotBeNull()
    }

    test("with three Islands, a 3-power creature can block it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        repeat(3) { driver.putLandOnBattlefield(me, "Island") }
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        val kraken = driver.attackWithKraken(me, opponent)

        driver.declareBlockers(opponent, mapOf(courser to listOf(kraken))).error shouldBe null
    }

    test("the defending player's Islands don't count") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        repeat(5) { driver.putLandOnBattlefield(opponent, "Island") }
        val guide = driver.putCreatureOnBattlefield(opponent, "Goblin Guide") // 2/1

        val kraken = driver.attackWithKraken(me, opponent)

        driver.declareBlockers(opponent, mapOf(guide to listOf(kraken))).error shouldBe null
    }
})
