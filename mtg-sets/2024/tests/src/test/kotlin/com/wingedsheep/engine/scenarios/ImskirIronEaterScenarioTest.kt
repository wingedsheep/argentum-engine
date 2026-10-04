package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ImskirIronEater
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Imskir Iron-Eater (MH3 #189) — {6}{B}{R} Legendary Creature — Demon 5/5.
 *
 * Affinity for artifacts; ETB draw X / lose X where X = floor(artifacts you control / 2);
 * {3}{R}, Sacrifice an artifact: damage equal to the sacrificed artifact's mana value.
 */
class ImskirIronEaterScenarioTest : FunSpec({

    test("affinity discounts the cast and the ETB draws and drains half the artifacts, rounded down") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Five artifacts: affinity shaves {5}, and X = 5 / 2 = 2.
        repeat(5) { driver.putPermanentOnBattlefield(player, "Legion Extruder") }
        val imskir = driver.putCardInHand(player, "Imskir Iron-Eater")
        // {6}{B}{R} - 5 = {1}{B}{R}
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveMana(player, Color.RED, 2)

        val handBefore = driver.getHandSize(player)
        driver.castSpell(player, imskir).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the ETB trigger

        driver.state.getBattlefield().contains(imskir) shouldBe true
        // Imskir left the hand (-1), then two cards drawn (+2).
        driver.getHandSize(player) shouldBe handBefore - 1 + 2
        driver.getLifeTotal(player) shouldBe 18
    }

    test("sacrifice an artifact: deals damage equal to its mana value to any target") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val imskir = driver.putPermanentOnBattlefield(player, "Imskir Iron-Eater")
        // Legion Extruder: mana value 2.
        val fodder = driver.putPermanentOnBattlefield(player, "Legion Extruder")
        driver.giveMana(player, Color.RED, 4)

        driver.submit(
            ActivateAbility(
                player,
                imskir,
                ImskirIronEater.activatedAbilities.first().id,
                listOf(ChosenTarget.Player(opponent))
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.getBattlefield().contains(fodder) shouldBe false
        driver.getLifeTotal(opponent) shouldBe 18
    }
})
