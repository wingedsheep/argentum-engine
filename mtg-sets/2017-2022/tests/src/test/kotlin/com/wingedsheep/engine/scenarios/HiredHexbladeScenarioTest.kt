package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.HiredHexblade
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hired Hexblade (AFR #109) — {1}{B} 2/2.
 *
 *   When this creature enters, if mana from a Treasure was spent to cast it, you draw a card and
 *   you lose 1 life.
 *
 * The intervening "if" reads the permanent's cast record on entering: controlling an unused
 * Treasure is not spending its mana, and an uncast Hexblade never triggers.
 */
class HiredHexbladeScenarioTest : FunSpec({

    val treasureManaAbilityId = PredefinedTokens.Treasure.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HiredHexblade))
        driver.registerCard(PredefinedTokens.Treasure)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("cast with Treasure mana: draw a card and lose 1 life") {
        val driver = createDriver()
        val me = driver.player1

        val treasure = driver.putPermanentOnBattlefield(me, "Treasure")
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = treasure,
                abilityId = treasureManaAbilityId,
                manaColorChoice = Color.BLACK,
            )
        )
        driver.giveMana(me, Color.BLACK, 1)

        val hexblade = driver.putCardInHand(me, "Hired Hexblade")
        val handBefore = driver.getHandSize(me)
        val lifeBefore = driver.getLifeTotal(me)
        driver.submitSuccess(
            CastSpell(playerId = me, cardId = hexblade, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass() // creature spell resolves, ETB trigger goes on the stack
        driver.bothPass() // trigger resolves

        driver.findPermanent(me, "Hired Hexblade") shouldNotBe null
        withClue("Hexblade left the hand, then one card was drawn") {
            driver.getHandSize(me) shouldBe handBefore
        }
        driver.getLifeTotal(me) shouldBe lifeBefore - 1
    }

    test("cast without Treasure mana: no trigger, even while controlling a Treasure") {
        val driver = createDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Treasure")
        driver.giveMana(me, Color.BLACK, 2)

        val hexblade = driver.putCardInHand(me, "Hired Hexblade")
        val handBefore = driver.getHandSize(me)
        val lifeBefore = driver.getLifeTotal(me)
        driver.submitSuccess(
            CastSpell(playerId = me, cardId = hexblade, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        driver.findPermanent(me, "Hired Hexblade") shouldNotBe null
        driver.getHandSize(me) shouldBe handBefore - 1
        driver.getLifeTotal(me) shouldBe lifeBefore
    }
})
