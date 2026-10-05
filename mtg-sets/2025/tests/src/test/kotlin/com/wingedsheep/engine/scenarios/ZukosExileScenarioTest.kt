package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.ZukosExile
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zuko's Exile (TLA #3) — {5} Instant — Lesson.
 *
 *   "Exile target artifact, creature, or enchantment. Its controller creates a Clue token."
 *
 * "Its controller" is the controller of the exiled permanent (last-known information).
 */
class ZukosExileScenarioTest : FunSpec({

    test("exiles an opponent's creature and that opponent gets the Clue") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZukosExile))
        driver.registerCard(PredefinedTokens.Clue)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val target = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val spell = driver.putCardInHand(me, "Zuko's Exile")
        driver.giveColorlessMana(me, 5)

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.error shouldBe null
        driver.bothPass()

        driver.findPermanent(opp, "Centaur Courser") shouldBe null
        driver.findPermanent(opp, "Clue") shouldNotBe null
        driver.findPermanent(me, "Clue") shouldBe null
    }
})
