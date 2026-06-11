package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.OrokuSakiShredderRising
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Oroku Saki, Shredder Rising (TMT #68) — combat damage to a player → draw a card and lose 1 life.
 */
class OrokuSakiShredderRisingTest : FunSpec({
    test("combat damage to a player draws a card and loses 1 life") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OrokuSakiShredderRising))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val saki = driver.putCreatureOnBattlefield(player, "Oroku Saki, Shredder Rising")
        driver.removeSummoningSickness(saki)
        val handBefore = driver.getHandSize(player)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(saki), opponent)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.assertLifeTotal(opponent, 17) // 3 combat damage
        driver.assertLifeTotal(player, 19)   // lost 1 life
        driver.getHandSize(player) shouldBeGreaterThan handBefore // drew a card
    }
})
