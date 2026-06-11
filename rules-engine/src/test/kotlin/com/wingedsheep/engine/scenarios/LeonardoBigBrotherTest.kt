package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.LeonardoBigBrother
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Leonardo, Big Brother (TMT #14) — "Leonardo gets +1/+0 for each other creature you control."
 */
class LeonardoBigBrotherTest : FunSpec({
    test("gains +1/+0 per other creature you control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LeonardoBigBrother))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)
        val player = driver.activePlayer!!

        val leo = driver.putCreatureOnBattlefield(player, "Leonardo, Big Brother")
        driver.state.projectedState.getPower(leo) shouldBe 1 // base, no other creatures

        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        // 1 base + 2 other creatures = 3 power, toughness unchanged at 3.
        driver.state.projectedState.getPower(leo) shouldBe 3
        driver.state.projectedState.getToughness(leo) shouldBe 3
    }
})
