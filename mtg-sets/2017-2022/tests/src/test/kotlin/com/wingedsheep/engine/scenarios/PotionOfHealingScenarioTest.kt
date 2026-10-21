package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.PotionOfHealing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Potion of Healing (AFR #34) — {1}{W} Artifact.
 *
 * "When this artifact enters, draw a card.
 *  {W}, {T}, Sacrifice this artifact: You gain 3 life."
 */
class PotionOfHealingScenarioTest : FunSpec({

    test("entering draws a card; {W}, {T}, sacrifice gains 3 life") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PotionOfHealing)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val potion = driver.putCardInHand(me, "Potion of Healing")
        val handBefore = driver.getHandSize(me)

        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, potion)
        driver.bothPass() // resolve the spell -> it enters -> draw trigger
        driver.bothPass() // resolve the draw trigger

        // The Potion left the hand, the trigger drew one: net hand size unchanged.
        driver.getHandSize(me) shouldBe handBefore

        val potionId = driver.findPermanent(me, "Potion of Healing")!!
        driver.giveMana(me, Color.WHITE, 1)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = potionId,
                abilityId = PotionOfHealing.activatedAbilities.single().id
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.findPermanent(me, "Potion of Healing") shouldBe null
        driver.getLifeTotal(me) shouldBe 23
    }
})
