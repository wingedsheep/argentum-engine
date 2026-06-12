package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BovineIntervention
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bovine Intervention (OTJ #6) — {1}{W} Instant.
 *
 *   "Destroy target artifact or creature. Its controller creates a 2/2 white Ox creature token."
 *
 * "Its controller" is the controller of the destroyed permanent (last-known information).
 */
class BovineInterventionScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BovineIntervention))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("destroys an opponent's creature and that opponent gets the Ox") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val target = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val spell = driver.putCardInHand(me, "Bovine Intervention")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 1)

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        (result.error == null) shouldBe true
        driver.bothPass()

        // Creature destroyed.
        driver.assertInGraveyard(opp, "Centaur Courser")
        // The destroyed permanent's controller (the opponent) gets a 2/2 Ox; caster gets nothing.
        val oppCreatures = driver.getCreatures(opp)
        oppCreatures.any { driver.getCardName(it) == "Ox Token" } shouldBe true
        driver.getCreatures(me).any { driver.getCardName(it) == "Ox Token" } shouldBe false
    }

    test("destroys an artifact creature and its controller gets the Ox") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val target = driver.putPermanentOnBattlefield(opp, "Artifact Creature")
        val spell = driver.putCardInHand(me, "Bovine Intervention")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 1)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        driver.assertInGraveyard(opp, "Artifact Creature")
        driver.getCreatures(opp).any { driver.getCardName(it) == "Ox Token" } shouldBe true
    }

    test("destroying my own creature gives the Ox to me") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val target = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val spell = driver.putCardInHand(me, "Bovine Intervention")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 1)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        driver.assertInGraveyard(me, "Centaur Courser")
        driver.getCreatures(me).any { driver.getCardName(it) == "Ox Token" } shouldBe true
    }
})
