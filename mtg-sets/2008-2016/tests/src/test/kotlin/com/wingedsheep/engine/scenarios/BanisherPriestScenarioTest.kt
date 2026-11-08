package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m14.cards.BanisherPriest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Banisher Priest (Magic 2014 #7) — {1}{W}{W} Creature — Human Cleric 2/2.
 *
 * "When this creature enters, exile target creature an opponent controls until this creature
 *  leaves the battlefield."
 */
class BanisherPriestScenarioTest : FunSpec({

    test("exiles an opponent's creature until the Priest leaves, then the creature returns") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BanisherPriest)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val theirs = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val mine = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val priestCard = driver.putCardInHand(me, "Banisher Priest")
        driver.giveMana(me, Color.WHITE, 3)
        driver.castSpell(me, priestCard).outcome shouldBe Outcome.Done
        driver.bothPass() // Priest resolves; enters trigger asks for a target

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.legalTargets[0].orEmpty() shouldContain theirs
        decision.legalTargets[0].orEmpty() shouldNotContain mine
        driver.submitTargetSelection(me, listOf(theirs))
        driver.bothPass() // trigger resolves

        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getExile(opponent) shouldContain theirs

        // Kill the Priest — the exiled creature comes back under its owner's control.
        val priest = driver.findPermanent(me, "Banisher Priest").shouldNotBeNull()
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(priest)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.findPermanent(me, "Banisher Priest") shouldBe null
        driver.findPermanent(opponent, "Grizzly Bears").shouldNotBeNull()
        driver.getExile(opponent).isEmpty() shouldBe true
    }
})
