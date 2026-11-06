package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.Decommission
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Decommission (AER) — destroy target artifact or enchantment; revolt gains 3 life. Revolt is
 * checked after the destroy, so destroying your own permanent enables it (2017-02-09 ruling).
 */
class DecommissionScenarioTest : FunSpec({
    fun driver(plains: Int): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + Decommission)
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(plains) { _ -> it.putLandOnBattlefield(it.player1, "Plains") }
    }

    test("destroying an opponent's enchantment with no revolt gains no life") {
        val d = driver(3)
        val enchantment = d.putPermanentOnBattlefield(d.player2, "Test Enchantment")
        val spell = d.putCardInHand(d.player1, "Decommission")

        d.castSpell(d.player1, spell, listOf(enchantment)).error shouldBe null
        d.bothPass()

        d.getGraveyardCardNames(d.player2) shouldContain "Test Enchantment"
        withClue("no permanent of yours left the battlefield") { d.getLifeTotal(d.player1) shouldBe 20 }
    }

    test("destroying your own enchantment enables its own revolt") {
        val d = driver(3)
        val enchantment = d.putPermanentOnBattlefield(d.player1, "Test Enchantment")
        val spell = d.putCardInHand(d.player1, "Decommission")

        d.castSpell(d.player1, spell, listOf(enchantment)).error shouldBe null
        d.bothPass()

        d.getGraveyardCardNames(d.player1) shouldContain "Test Enchantment"
        withClue("the destroyed enchantment was yours") { d.getLifeTotal(d.player1) shouldBe 23 }
    }

    test("a permanent of yours that left earlier in the turn turns on revolt") {
        val d = driver(6)
        val mine = d.putPermanentOnBattlefield(d.player1, "Test Enchantment")
        val theirs = d.putPermanentOnBattlefield(d.player2, "Test Enchantment")
        val first = d.putCardInHand(d.player1, "Decommission")
        val second = d.putCardInHand(d.player1, "Decommission")

        d.castSpell(d.player1, first, listOf(mine)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23

        d.castSpell(d.player1, second, listOf(theirs)).error shouldBe null
        d.bothPass()
        d.getGraveyardCardNames(d.player2) shouldContain "Test Enchantment"
        withClue("revolt still holds from the first Decommission") { d.getLifeTotal(d.player1) shouldBe 26 }
    }
})
