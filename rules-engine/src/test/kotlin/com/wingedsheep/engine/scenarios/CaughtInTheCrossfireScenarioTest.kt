package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.CaughtInTheCrossfire
import com.wingedsheep.mtg.sets.definitions.otj.cards.ExplosiveDerailment
import com.wingedsheep.mtg.sets.definitions.otj.cards.ThunderSalvo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Caught in the Crossfire. */
class CaughtInTheCrossfireScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + ThunderSalvo + ExplosiveDerailment + CaughtInTheCrossfire
        )
        return driver
    }

    // ---------------------------------------------------------------------
    // Caught in the Crossfire (Spree)
    // ---------------------------------------------------------------------

    test("Caught in the Crossfire outlaw mode hits only outlaws") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Test Hasty Prospector is a 2/1 Pirate (outlaw); Black Creature is a 2/2 non-outlaw.
        val ragavan = driver.putCreatureOnBattlefield(opponent, "Test Hasty Prospector")
        driver.putCreatureOnBattlefield(opponent, "Black Creature")

        val spell = driver.putCardInHand(player, "Caught in the Crossfire")
        driver.giveMana(player, Color.RED, 2)
        driver.giveColorlessMana(player, 1) // {R}{R} base + {1} outlaw mode
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(0)
            )
        )
        driver.bothPass()

        // 2 damage to outlaws only: Ragavan (2/1) dies, Black Creature (2/2) survives.
        driver.findPermanent(opponent, "Test Hasty Prospector") shouldBe null
        driver.findPermanent(opponent, "Black Creature") shouldNotBe null
    }

    test("Caught in the Crossfire non-outlaw mode hits only non-outlaws") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Test Hasty Prospector") // 2/1 Pirate
        driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2 non-outlaw

        val spell = driver.putCardInHand(player, "Caught in the Crossfire")
        driver.giveMana(player, Color.RED, 2)
        driver.giveColorlessMana(player, 1) // {R}{R} base + {1} non-outlaw mode
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(1)
            )
        )
        driver.bothPass()

        // 2 damage to non-outlaws only: Black Creature (2/2) dies, Ragavan survives.
        driver.findPermanent(opponent, "Black Creature") shouldBe null
        driver.findPermanent(opponent, "Test Hasty Prospector") shouldNotBe null
    }

    test("Caught in the Crossfire both modes: 2 damage to every creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(opponent, "Test Hasty Prospector") // outlaw
        driver.putCreatureOnBattlefield(opponent, "Black Creature") // non-outlaw

        val spell = driver.putCardInHand(player, "Caught in the Crossfire")
        driver.giveMana(player, Color.RED, 2)
        driver.giveColorlessMana(player, 2) // {R}{R} base + {1} + {1}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(0, 1)
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Test Hasty Prospector") shouldBe null
        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }
})
