package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.DoubleVision
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Double Vision {3}{R}{R} — Enchantment.
 * "Whenever you cast your first instant or sorcery spell each turn, copy that spell. You may choose
 *  new targets for the copy."
 *
 * Pins the first-spell restriction around the copy trigger: only the first instant or sorcery each
 * turn triggers, and one cast before Double Vision entered still uses up the turn's "first"
 * (2020-06-23 ruling).
 */
class DoubleVisionScenarioTest : FunSpec({

    fun copyTriggers(driver: GameTestDriver) =
        driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is CopyTargetSpellEffect }

    fun setup(): Pair<GameTestDriver, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DoubleVision))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    test("copies only the first instant or sorcery you cast each turn") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Double Vision")
        driver.giveMana(you, Color.RED, 2)
        val first = driver.putCardInHand(you, "Lightning Bolt")
        val second = driver.putCardInHand(you, "Lightning Bolt")

        driver.castSpellWithTargets(you, first, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        copyTriggers(driver).size shouldBe 1

        driver.castSpellWithTargets(you, second, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        copyTriggers(driver).size shouldBe 1
    }

    test("does not trigger when the turn's first instant was cast before Double Vision entered") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        driver.giveMana(you, Color.RED, 2)
        val first = driver.putCardInHand(you, "Lightning Bolt")
        val second = driver.putCardInHand(you, "Lightning Bolt")

        driver.castSpellWithTargets(you, first, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        driver.putPermanentOnBattlefield(you, "Double Vision")

        driver.castSpellWithTargets(you, second, listOf(ChosenTarget.Player(opponent))).error shouldBe null
        copyTriggers(driver).size shouldBe 0
    }
})
