package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThan

/**
 * Tests for death triggers (when a creature dies).
 *
 * ## Covered Scenarios
 * - Death trigger fires when creature dies from lethal damage (via spell)
 * - Death trigger effect is executed (controller gains life)
 *
 * The key issue being tested: Death triggers must fire when creatures die
 * from state-based actions (SBAs). The events from SBAs must be included
 * when detecting triggers after spell/ability resolution.
 */
class DeathTriggerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("death trigger fires when creature is destroyed by lethal damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Death Trigger Test Creature on the battlefield for active player
        // This creature has: "When this creature dies, you gain 3 life."
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Death Trigger Test Creature")
        creature shouldNotBe null

        // Verify starting life
        driver.getLifeTotal(activePlayer) shouldBe 20

        // Give active player mana to cast Lightning Bolt
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        // Cast Lightning Bolt targeting our own creature (to trigger death)
        val castResult = driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(creature)))
        castResult.isSuccess shouldBe true

        // Resolve the spell (creature takes 3 damage, dies from SBA)
        driver.bothPass()

        // The creature should be dead (moved to graveyard)
        driver.findPermanent(activePlayer, "Death Trigger Test Creature") shouldBe null

        // The death trigger should be on the stack
        // This is the key assertion - the fix ensures SBA events (death) are
        // included in trigger detection after spell resolution
        driver.stackSize shouldBeGreaterThan 0

        // Resolve the death trigger
        driver.bothPass()

        // Active player should have gained 3 life from the death trigger
        driver.getLifeTotal(activePlayer) shouldBe 23
    }
})
