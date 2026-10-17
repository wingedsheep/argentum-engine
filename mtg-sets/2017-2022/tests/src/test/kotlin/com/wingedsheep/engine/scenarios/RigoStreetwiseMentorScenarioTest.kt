package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.RigoStreetwiseMentor
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rigo, Streetwise Mentor (SNC #215) — "Whenever you attack a player or planeswalker with one or
 * more creatures with power 1 or less, draw a card."
 *
 * Proves the attacker filter (power 1 or less, not attacking a battle) gates the once-per-combat
 * "whenever you attack" trigger: one qualifying attacker among several draws exactly one card, and
 * an attack made only with bigger creatures draws nothing.
 */
class RigoStreetwiseMentorScenarioTest : FunSpec({

    val smallAttacker = CardDefinition.creature(
        name = "Test One-Power Attacker",
        manaCost = ManaCost.parse("{W}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RigoStreetwiseMentor)
        driver.registerCard(smallAttacker)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (!isPaused && stackSize > 0 && guard++ < 50) bothPass()
    }

    test("attacking with a power-1 creature alongside bigger ones draws exactly one card") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val rigo = driver.putCreatureOnBattlefield(player, "Rigo, Streetwise Mentor")
        val small1 = driver.putCreatureOnBattlefield(player, "Test One-Power Attacker")
        val small2 = driver.putCreatureOnBattlefield(player, "Test One-Power Attacker")
        listOf(rigo, small1, small2).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = driver.getHandSize(player)
        driver.declareAttackers(player, listOf(rigo, small1, small2), opponent)
        driver.resolveStack()

        withClue("one trigger per combat, however many small attackers") {
            driver.getHandSize(player) shouldBe handBefore + 1
        }
    }

    test("attacking only with creatures of power 2 or greater draws nothing") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val rigo = driver.putCreatureOnBattlefield(player, "Rigo, Streetwise Mentor")
        driver.putCreatureOnBattlefield(player, "Test One-Power Attacker") // stays home
        driver.removeSummoningSickness(rigo)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val handBefore = driver.getHandSize(player)
        driver.declareAttackers(player, listOf(rigo), opponent)
        driver.resolveStack()

        withClue("a non-attacking power-1 creature doesn't count") {
            driver.getHandSize(player) shouldBe handBefore
        }
    }
})
