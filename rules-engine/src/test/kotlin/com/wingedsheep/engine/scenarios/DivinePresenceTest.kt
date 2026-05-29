package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.DivinePresence
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Divine Presence (INV #15) — Invasion engine gap #7, the [CapDamage] replacement.
 *
 * "If a source would deal 4 or more damage to a permanent or player, that source deals 3
 * damage to that permanent or player instead."
 */
class DivinePresenceTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DivinePresence))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        return Triple(driver, you, driver.getOpponent(you))
    }

    fun GameTestDriver.lifeOf(playerId: com.wingedsheep.sdk.model.EntityId): Int =
        state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0

    test("caps 4 damage to 3") {
        val (driver, you, opponent) = newGame()
        driver.putPermanentOnBattlefield(you, "Divine Presence")

        driver.giveMana(you, Color.RED, 4)
        val stoke = driver.putCardInHand(you, "Stoke the Flames")
        driver.castSpellWithTargets(you, stoke, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        // 4 damage capped to 3 → opponent loses 3, not 4.
        driver.lifeOf(opponent) shouldBe 17
    }

    test("leaves 3-or-less damage unchanged") {
        val (driver, you, opponent) = newGame()
        driver.putPermanentOnBattlefield(you, "Divine Presence")

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        // 3 is below the cap — full 3 damage.
        driver.lifeOf(opponent) shouldBe 17
    }
})
