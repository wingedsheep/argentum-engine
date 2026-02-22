package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.FutureSight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Future Sight.
 *
 * Future Sight: {2}{U}{U}{U}
 * Enchantment
 * Play with the top card of your library revealed.
 * You may play lands and cast spells from the top of your library.
 */
class FutureSightTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("can cast a spell from top of library with Future Sight") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Future Sight on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Future Sight")

        // Put a spell on top of library
        val lightningBoltOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Lightning Bolt")

        // Give mana to cast it
        driver.giveMana(activePlayer, Color.RED, 1)

        // Put a creature on opponent's battlefield as a target
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast Lightning Bolt from top of library targeting the creature
        val castResult = driver.castSpell(activePlayer, lightningBoltOnTop, listOf(creature))
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // The creature should be destroyed
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
    }

    test("can play a land from top of library with Future Sight") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Future Sight on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Future Sight")

        // Put a land on top of library
        val forestOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Forest")

        // Play the land from top of library
        val playResult = driver.playLand(activePlayer, forestOnTop)
        playResult.isSuccess shouldBe true

        // The forest should be on the battlefield
        driver.findPermanent(activePlayer, "Forest") shouldNotBe null
    }

    test("cannot cast from top of library without Future Sight") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a spell on top of library (no Future Sight on battlefield)
        val lightningBoltOnTop = driver.putCardOnTopOfLibrary(activePlayer, "Lightning Bolt")

        // Give mana
        driver.giveMana(activePlayer, Color.RED, 1)

        // Put a creature on opponent's battlefield as a target
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Try to cast Lightning Bolt from top of library - should fail
        val castResult = driver.castSpell(activePlayer, lightningBoltOnTop, listOf(creature))
        castResult.isSuccess shouldBe false
    }

    test("top card updates after casting from library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Future Sight on the battlefield
        driver.putPermanentOnBattlefield(activePlayer, "Future Sight")

        // Put two spells on top of library (second one first, then first on top)
        val secondSpell = driver.putCardOnTopOfLibrary(activePlayer, "Lightning Bolt")
        val firstSpell = driver.putCardOnTopOfLibrary(activePlayer, "Lightning Bolt")

        // Give mana for both
        driver.giveMana(activePlayer, Color.RED, 2)

        // Put two creatures as targets
        val creature1 = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val creature2 = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast first Lightning Bolt from top
        val result1 = driver.castSpell(activePlayer, firstSpell, listOf(creature1))
        result1.isSuccess shouldBe true
        driver.bothPass()

        // Now second spell is on top - cast it too
        val result2 = driver.castSpell(activePlayer, secondSpell, listOf(creature2))
        result2.isSuccess shouldBe true
        driver.bothPass()

        // Both creatures should be destroyed (both were Grizzly Bears)
        driver.getGraveyardCardNames(opponent).count { it == "Grizzly Bears" } shouldBe 2
    }
})
