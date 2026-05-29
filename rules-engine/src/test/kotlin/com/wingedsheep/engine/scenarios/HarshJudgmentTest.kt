package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.HarshJudgment
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Harsh Judgment (INV #19) — Invasion engine gap #7, first user of the [RedirectDamage]
 * static replacement + [EffectTarget.ControllerOfDamageSource].
 *
 * "If an instant or sorcery spell of the chosen color would deal damage to you, it deals that
 * damage to its controller instead."
 *
 * The Harsh Judgment controller is the *non-active* player so the active player's spell has a
 * distinct controller to redirect to. The chosen color is injected directly (the
 * EntersWithChoice(COLOR) → ChosenColorComponent path is covered elsewhere).
 */
class HarshJudgmentTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HarshJudgment))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        return Triple(driver, active, driver.getOpponent(active))
    }

    fun GameTestDriver.lifeOf(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0

    test("redirects chosen-color instant damage to the spell's controller") {
        val (driver, active, defender) = newGame()
        // Defender controls Harsh Judgment with red chosen.
        val judgment = driver.putPermanentOnBattlefield(defender, "Harsh Judgment")
        driver.addComponent(judgment, ChosenColorComponent(Color.RED))

        // Active player casts a red instant (Lightning Bolt) at the defender.
        driver.giveMana(active, Color.RED, 1)
        val bolt = driver.putCardInHand(active, "Lightning Bolt")
        driver.castSpellWithTargets(active, bolt, listOf(ChosenTarget.Player(defender)))
        driver.bothPass()

        // Damage is redirected to the bolt's controller (the active player).
        driver.lifeOf(defender) shouldBe 20
        driver.lifeOf(active) shouldBe 17
    }

    test("does not redirect a spell of a different color") {
        val (driver, active, defender) = newGame()
        val judgment = driver.putPermanentOnBattlefield(defender, "Harsh Judgment")
        driver.addComponent(judgment, ChosenColorComponent(Color.BLUE))

        driver.giveMana(active, Color.RED, 1)
        val bolt = driver.putCardInHand(active, "Lightning Bolt")
        driver.castSpellWithTargets(active, bolt, listOf(ChosenTarget.Player(defender)))
        driver.bothPass()

        // Red bolt is not the chosen color (blue) — normal damage to the defender.
        driver.lifeOf(defender) shouldBe 17
        driver.lifeOf(active) shouldBe 20
    }
})
