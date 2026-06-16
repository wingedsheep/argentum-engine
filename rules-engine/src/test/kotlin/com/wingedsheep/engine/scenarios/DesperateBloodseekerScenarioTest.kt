package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.DesperateBloodseeker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Desperate Bloodseeker (OTJ #86) — {1}{B} Creature — Vampire 2/2.
 *
 *   "Lifelink. When this creature enters, target player mills two cards."
 *
 * Casts the creature so its enters trigger fires, then resolves the trigger targeting the
 * opponent and verifies exactly two cards were milled into their graveyard. Also confirms the
 * creature has lifelink.
 */
class DesperateBloodseekerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(DesperateBloodseeker))
        return driver
    }

    test("enters trigger mills two cards from the targeted player's library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Forest" to 10), startingLife = 20)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val graveyardBefore = driver.getGraveyard(opponent).size

        // Cast Desperate Bloodseeker from hand ({1}{B}).
        val card = driver.putCardInHand(you, "Desperate Bloodseeker")
        driver.giveMana(you, Color.BLACK, 2)
        driver.castSpell(you, card)

        // Resolve the creature spell so it enters the battlefield and fires its enters trigger.
        driver.bothPass()

        // Resolve the enters trigger: choose the opponent as the player who mills.
        driver.submitTargetSelection(you, listOf(opponent))
        driver.bothPass()

        // The opponent milled exactly two cards.
        driver.getGraveyard(opponent).size shouldBe graveyardBefore + 2

        // Lifelink is present on the resolved creature.
        val bloodseeker = driver.getCreatures(you).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Desperate Bloodseeker"
        }
        val projected = StateProjector().project(driver.state)
        projected.hasKeyword(bloodseeker, Keyword.LIFELINK) shouldBe true
    }
})
