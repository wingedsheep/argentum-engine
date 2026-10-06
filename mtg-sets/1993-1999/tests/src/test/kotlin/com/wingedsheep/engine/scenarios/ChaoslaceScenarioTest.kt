package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Chaoslace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chaoslace — "Target spell or permanent becomes red."
 *
 * The color change has no duration: a permanent stays red past end of turn, and a spell on the
 * stack projects as red while it waits to resolve.
 */
class ChaoslaceScenarioTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Chaoslace))
        driver.initMirrorMatch(Deck.of("Mountain" to 30, "Forest" to 30))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    test("a permanent becomes red indefinitely") {
        val (driver, player) = newGame()
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.state.projectedState.getColors(bears) shouldBe setOf("GREEN")

        val lace = driver.putCardInHand(player, "Chaoslace")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, lace, listOf(bears)).error shouldBe null
        driver.bothPass().error shouldBe null

        driver.state.projectedState.getColors(bears) shouldBe setOf("RED")

        // No "until end of turn" — still red on the next turn.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getColors(bears) shouldBe setOf("RED")
    }

    test("a spell on the stack becomes red") {
        val (driver, player) = newGame()
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val growth = driver.putCardInHand(player, "Giant Growth")
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, growth, listOf(bears)).error shouldBe null
        driver.state.stack.contains(growth) shouldBe true

        val lace = driver.putCardInHand(player, "Chaoslace")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpellWithTargets(player, lace, listOf(ChosenTarget.Spell(growth))).error shouldBe null
        driver.bothPass().error shouldBe null // resolve Chaoslace only

        driver.state.stack.contains(growth) shouldBe true
        driver.state.projectedState.getColors(growth) shouldBe setOf("RED")
    }
})
