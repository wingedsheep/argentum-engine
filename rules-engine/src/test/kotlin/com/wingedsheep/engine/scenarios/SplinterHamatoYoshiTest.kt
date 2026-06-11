package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.SplinterHamatoYoshi
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Splinter, Hamato Yoshi (TMT #79) — Menace + "Other Ninjas you control get +1/+1."
 */
class SplinterHamatoYoshiTest : FunSpec({
    test("boosts other Ninjas you control but not himself") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SplinterHamatoYoshi))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)
        val player = driver.activePlayer!!

        val splinter = driver.putCreatureOnBattlefield(player, "Splinter, Hamato Yoshi")
        val ninja = driver.putCreatureOnBattlefield(player, "Foot Ninjas") // 5/5 Ninja

        // Foot Ninjas is boosted to 6/6; Splinter (himself) stays 1/3.
        driver.state.projectedState.getPower(ninja) shouldBe 6
        driver.state.projectedState.getToughness(ninja) shouldBe 6
        driver.state.projectedState.getPower(splinter) shouldBe 1
    }
})
