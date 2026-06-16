package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RustlerRampage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rustler Rampage (OTJ Spree instant).
 *
 * Mode 0 ({1}): untap all creatures the targeted player controls (`ForEachInGroup`
 * over a target-relative group filter). Mode 1 ({1}): target creature gains double
 * strike until end of turn. Both modes resolve their own target via `ContextTarget(0)`.
 */
class OtjRustlerRampageScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RustlerRampage)
        return driver
    }

    test("Mode 0 untaps all creatures the targeted player controls") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val a = driver.putCreatureOnBattlefield(player, "Black Creature")
        val b = driver.putCreatureOnBattlefield(player, "Black Creature")
        driver.tapPermanent(a)
        driver.tapPermanent(b)
        driver.isTapped(a) shouldBe true
        driver.isTapped(b) shouldBe true

        val spell = driver.putCardInHand(player, "Rustler Rampage")
        driver.giveMana(player, Color.WHITE, 1) // {W} base
        driver.giveColorlessMana(player, 1)      // {1} for mode 0
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(player)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(player)))
            )
        )
        driver.bothPass()

        driver.isTapped(a) shouldBe false
        driver.isTapped(b) shouldBe false
    }

    test("Mode 1 grants double strike to target creature until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(player, "Black Creature")
        projector.project(driver.state).hasKeyword(creature, Keyword.DOUBLE_STRIKE) shouldBe false

        val spell = driver.putCardInHand(player, "Rustler Rampage")
        driver.giveMana(player, Color.WHITE, 1) // {W} base
        driver.giveColorlessMana(player, 1)      // {1} for mode 1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(creature)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(creature)))
            )
        )
        driver.bothPass()

        projector.project(driver.state).hasKeyword(creature, Keyword.DOUBLE_STRIKE) shouldBe true
    }

    test("Both modes: untap your creatures AND grant double strike") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tapped = driver.putCreatureOnBattlefield(player, "Black Creature")
        val attacker = driver.putCreatureOnBattlefield(player, "Black Creature")
        driver.tapPermanent(tapped)

        val spell = driver.putCardInHand(player, "Rustler Rampage")
        driver.giveMana(player, Color.WHITE, 1) // {W} base
        driver.giveColorlessMana(player, 2)      // {1} + {1}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(player), ChosenTarget.Permanent(attacker)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Player(player)),
                    listOf(ChosenTarget.Permanent(attacker))
                )
            )
        )
        driver.bothPass()

        driver.isTapped(tapped) shouldBe false
        projector.project(driver.state).hasKeyword(attacker, Keyword.DOUBLE_STRIKE) shouldBe true
    }
})
