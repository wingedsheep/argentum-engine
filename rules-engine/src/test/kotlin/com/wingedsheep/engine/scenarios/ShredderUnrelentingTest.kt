package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.ShredderUnrelenting
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shredder, Unrelenting (TMT #74) — Deathtouch; "Whenever Shredder enters or
 * attacks, another target creature you control gains deathtouch until end of turn."
 */
class ShredderUnrelentingTest : FunSpec({
    test("attacking grants deathtouch to another target creature you control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShredderUnrelenting))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val shredder = driver.putCreatureOnBattlefield(player, "Shredder, Unrelenting")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(shredder)
        driver.removeSummoningSickness(bear)

        // Shredder himself has deathtouch (static).
        driver.state.projectedState.hasKeyword(shredder, Keyword.DEATHTOUCH) shouldBe true
        driver.state.projectedState.hasKeyword(bear, Keyword.DEATHTOUCH) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(shredder), opponent)
        // Attacks trigger asks for the "another target creature you control".
        driver.submitTargetSelection(player, listOf(bear))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.projectedState.hasKeyword(bear, Keyword.DEATHTOUCH) shouldBe true
    }
})
