package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.KitsaOtterballElite
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Prowess keyword ability:
 * Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.
 */
class ProwessTest : FunSpec({

    val prowessMonk = card("Prowess Monk") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Human Monk"
        power = 1
        toughness = 1
        prowess()
    }

    val allCards = TestCards.all + listOf(prowessMonk, KitsaOtterballElite)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    test("casting a noncreature spell triggers prowess (+1/+1)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Prowess Monk on battlefield
        val monkId = driver.putCreatureOnBattlefield(activePlayer, "Prowess Monk")

        // Put Lightning Bolt in hand
        val boltCard = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Add mana to cast Lightning Bolt ({R})
        driver.giveMana(activePlayer, Color.RED, 1)

        // Cast Lightning Bolt targeting opponent
        val opponentId = driver.getOpponent(activePlayer)
        driver.castSpell(activePlayer, boltCard, targets = listOf(opponentId))

        // Resolve prowess trigger (goes on stack when spell is cast)
        driver.bothPass()

        // Resolve the prowess triggered ability
        driver.bothPass()

        // Prowess Monk should be 2/2 (base 1/1 + prowess +1/+1)
        val projected = driver.state.projectedState
        projected.getPower(monkId) shouldBe 2
        projected.getToughness(monkId) shouldBe 2
    }

    test("casting a creature spell does NOT trigger prowess") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Prowess Monk on battlefield
        val monkId = driver.putCreatureOnBattlefield(activePlayer, "Prowess Monk")

        // Put a creature in hand
        val bearCard = driver.putCardInHand(activePlayer, "Centaur Courser")

        // Add mana
        driver.giveMana(activePlayer, Color.GREEN, 3)

        // Cast the creature
        driver.castSpell(activePlayer, bearCard)

        // Resolve the creature spell
        driver.bothPass()

        // Prowess Monk should still be 1/1 — no prowess trigger
        val projected = driver.state.projectedState
        projected.getPower(monkId) shouldBe 1
        projected.getToughness(monkId) shouldBe 1
    }

    test("Kitsa Otterball Elite prowess triggers on noncreature spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Kitsa on battlefield (1/3 with vigilance and prowess)
        val kitsaId = driver.putCreatureOnBattlefield(activePlayer, "Kitsa, Otterball Elite")

        // Put Lightning Bolt in hand (noncreature spell)
        val boltCard = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Add mana
        driver.giveMana(activePlayer, Color.RED, 1)

        // Cast Lightning Bolt targeting opponent
        val opponentId = driver.getOpponent(activePlayer)
        driver.castSpell(activePlayer, boltCard, targets = listOf(opponentId))

        // Resolve prowess trigger
        driver.bothPass()

        // Resolve the prowess triggered ability
        driver.bothPass()

        // Kitsa should be 2/4 (base 1/3 + prowess +1/+1)
        val projected = driver.state.projectedState
        projected.getPower(kitsaId) shouldBe 2
        projected.getToughness(kitsaId) shouldBe 4
    }
})
