package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.KravenProudPredator
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Kraven, Proud Predator (SPM #132).
 *
 * {1}{R}{G} star/4 Legendary Human Warrior Villain. Vigilance.
 * Top of the Food Chain — Kraven's power is equal to the greatest mana value among
 * permanents you control (Layer-7b CDA via
 * [com.wingedsheep.sdk.dsl.DynamicAmounts.battlefield] `.maxManaValue()`). Toughness stays a
 * printed 4. Kraven himself (mana value 3) is a permanent you control, so his own mana value
 * is included in the max; permanents your opponents control are excluded.
 */
class KravenProudPredatorScenarioTest : FunSpec({

    val projector = StateProjector()

    // Supporting permanents with known mana values.
    val PermanentMv2 = CardDefinition.creature(
        name = "Kraven MV2 Beast",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 1, toughness = 1
    )
    val PermanentMv6 = CardDefinition.creature(
        name = "Kraven MV6 Beast",
        manaCost = ManaCost.parse("{4}{G}{G}"),
        subtypes = emptySet(),
        power = 2, toughness = 2
    )
    val PermanentMv8 = CardDefinition.creature(
        name = "Kraven MV8 Beast",
        manaCost = ManaCost.parse("{6}{B}{B}"),
        subtypes = emptySet(),
        power = 3, toughness = 3
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(KravenProudPredator, PermanentMv2, PermanentMv6, PermanentMv8))
        return driver
    }

    test("power equals the greatest mana value among permanents you control; toughness stays printed 4") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val kraven = driver.putCreatureOnBattlefield(me, "Kraven, Proud Predator")

        // Alone, the greatest mana value you control is Kraven's own mana value (3).
        projector.getProjectedPower(driver.state, kraven) shouldBe 3
        projector.getProjectedToughness(driver.state, kraven) shouldBe 4

        // Add a lower-MV permanent (2): the max is still Kraven's own 3.
        driver.putCreatureOnBattlefield(me, "Kraven MV2 Beast")
        projector.getProjectedPower(driver.state, kraven) shouldBe 3
        projector.getProjectedToughness(driver.state, kraven) shouldBe 4

        // Add a higher-MV permanent (6): power tracks up to 6.
        driver.putCreatureOnBattlefield(me, "Kraven MV6 Beast")
        projector.getProjectedPower(driver.state, kraven) shouldBe 6
        projector.getProjectedToughness(driver.state, kraven) shouldBe 4

        // An opponent's even-higher-MV permanent (8) is excluded — power stays 6.
        driver.putCreatureOnBattlefield(opp, "Kraven MV8 Beast")
        projector.getProjectedPower(driver.state, kraven) shouldBe 6
        projector.getProjectedToughness(driver.state, kraven) shouldBe 4
    }
})
