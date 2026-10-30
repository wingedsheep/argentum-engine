package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.GoldenEgg
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Golden Egg (ELD #220) — {2} Artifact — Food.
 *
 * "When this artifact enters, draw a card.
 *  {1}, {T}, Sacrifice this artifact: Add one mana of any color.
 *  {2}, {T}, Sacrifice this artifact: You gain 3 life."
 */
class GoldenEggScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoldenEgg))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("casting it draws a card when it enters") {
        val driver = createDriver()
        val me = driver.player1
        val egg = driver.putCardInHand(me, "Golden Egg")
        val handBefore = driver.getHandSize(me)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, egg).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(me, "Golden Egg") shouldNotBe null
        driver.getHandSize(me) shouldBe handBefore // egg left the hand, one card drawn
    }

    test("{2}, {T}, sacrifice: gain 3 life") {
        val driver = createDriver()
        val me = driver.player1
        val egg = driver.putPermanentOnBattlefield(me, "Golden Egg")
        driver.giveColorlessMana(me, 2)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = egg,
                abilityId = GoldenEgg.activatedAbilities.single { !it.isManaAbility }.id,
            )
        ).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(me) shouldBe 23
        driver.findPermanent(me, "Golden Egg") shouldBe null
        driver.getGraveyardCardNames(me).contains("Golden Egg") shouldBe true
    }
})
