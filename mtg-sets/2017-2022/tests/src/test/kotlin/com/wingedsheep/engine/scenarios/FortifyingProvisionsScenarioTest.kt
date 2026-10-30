package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.FortifyingProvisions
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fortifying Provisions (ELD #13) — "Creatures you control get +0/+1. When this enchantment
 * enters, create a Food token."
 */
class FortifyingProvisionsScenarioTest : FunSpec({

    test("entering makes a Food, and only your creatures get +0/+1") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FortifyingProvisions) + PredefinedTokens.allTokens)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val foe = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirBears = driver.putCreatureOnBattlefield(foe, "Grizzly Bears")

        val provisions = driver.putCardInHand(me, "Fortifying Provisions")
        driver.giveMana(me, Color.WHITE, 3)
        driver.castSpell(me, provisions).error shouldBe null
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.findPermanent(me, "Food") shouldNotBe null
        driver.state.projectedState.getPower(bears) shouldBe 2
        driver.state.projectedState.getToughness(bears) shouldBe 3
        driver.state.projectedState.getToughness(theirBears) shouldBe 2
    }
})
