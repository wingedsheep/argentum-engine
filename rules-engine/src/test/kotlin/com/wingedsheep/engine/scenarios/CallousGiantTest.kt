package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.CallousGiant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Callous Giant (INV #139) — Invasion engine gap #7, [AmountFilter] threshold.
 *
 * "If a source would deal 3 or less damage to this creature, prevent that damage."
 */
class CallousGiantTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CallousGiant))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    test("prevents 3 damage (at the threshold)") {
        val (driver, you) = newGame()
        val giant = driver.putCreatureOnBattlefield(you, "Callous Giant")

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Permanent(giant)))
        driver.bothPass()

        // 3 damage is fully prevented.
        (driver.state.getEntity(giant)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("does not prevent 4 damage (above the threshold)") {
        val (driver, you) = newGame()
        val giant = driver.putCreatureOnBattlefield(you, "Callous Giant")

        driver.giveMana(you, Color.RED, 4)
        val stoke = driver.putCardInHand(you, "Stoke the Flames")
        driver.castSpellWithTargets(you, stoke, listOf(ChosenTarget.Permanent(giant)))
        driver.bothPass()

        // 4 damage is not prevented — the 4/4 dies.
        driver.getGraveyardCardNames(you).contains("Callous Giant") shouldBe true
    }
})
