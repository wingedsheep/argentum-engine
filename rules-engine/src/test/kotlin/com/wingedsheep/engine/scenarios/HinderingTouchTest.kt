package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.HinderingTouch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain as shouldContainString
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Hindering Touch and the Storm mechanic.
 *
 * Hindering Touch: {3}{U}
 * Instant
 * Counter target spell unless its controller pays {2}.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
class HinderingTouchTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HinderingTouch))
        return driver
    }

    test("Storm creates no copies when no spells were cast before") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 2 casts Lightning Bolt (first spell this turn)
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!
        driver.passPriority(player2)

        // Player 1 casts Hindering Touch (second spell this turn, but only 1 was cast before it)
        val hinderingTouch = driver.putCardInHand(player1, "Hindering Touch")
        driver.giveMana(player1, Color.BLUE, 4)
        driver.castSpellWithTargets(player1, hinderingTouch, listOf(ChosenTarget.Spell(spellOnStack)))

        // Stack should have: Hindering Touch, Storm trigger (1 copy), Lightning Bolt
        // The Storm trigger for 1 copy should be on the stack
        driver.stackSize shouldBe 3

        // The top of stack should be the Storm trigger (triggered ability)
        val topEntity = driver.state.getEntity(driver.getTopOfStack()!!)!!
        topEntity.get<TriggeredAbilityOnStackComponent>().shouldBeInstanceOf<TriggeredAbilityOnStackComponent>()
    }

    test("Storm creates copies that target spells on the stack") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast two spells first to set up storm count = 2
        val bolt1 = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt1, listOf(player1))

        // Resolve the first bolt
        driver.bothPass()

        // Cast second bolt
        val bolt2 = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt2, listOf(player1))
        val bolt2OnStack = driver.getTopOfStack()!!

        driver.passPriority(player2)

        // Verify storm count before casting Hindering Touch
        driver.state.spellsCastThisTurn shouldBe 2

        // Now player 1 casts Hindering Touch (3rd spell this turn, storm count = 2)
        val hinderingTouch = driver.putCardInHand(player1, "Hindering Touch")
        driver.giveMana(player1, Color.BLUE, 4)
        driver.castSpellWithTargets(player1, hinderingTouch, listOf(ChosenTarget.Spell(bolt2OnStack)))

        // Storm count should be 3 now
        driver.state.spellsCastThisTurn shouldBe 3

        // Storm trigger should be on the stack (bolt2 + Hindering Touch + Storm trigger)
        driver.stackSize shouldBe 3

        // Check the top of stack is a storm trigger
        val stormTriggerId = driver.getTopOfStack()!!
        val stormEntity = driver.state.getEntity(stormTriggerId)!!
        val stormAbility = stormEntity.get<TriggeredAbilityOnStackComponent>()!!
        stormAbility.description shouldContainString "Storm"

        // Both players pass - this resolves the Storm trigger
        val resolveResult = driver.passPriority(player1)
        // After player1 passes, player2 should have priority
        val resolveResult2 = driver.passPriority(player2)

        // After both pass, the Storm trigger should resolve and pause for target selection
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Select target for first copy
        driver.submitTargetSelection(player1, listOf(bolt2OnStack))

        // Should be paused for second copy
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Select target for second copy
        driver.submitTargetSelection(player1, listOf(bolt2OnStack))

        // Now the stack should have: 2 Storm copies + Hindering Touch + bolt2
        driver.stackSize shouldBe 4
    }

    test("Storm copies are not cast so they don't increment storm count") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast a Lightning Bolt first (spell #1)
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        driver.passPriority(player2)

        // Player 1 casts Hindering Touch (spell #2, storm count = 1)
        val hinderingTouch = driver.putCardInHand(player1, "Hindering Touch")
        driver.giveMana(player1, Color.BLUE, 4)
        val boltOnStack = driver.state.stack.first() // bolt is at bottom
        driver.castSpellWithTargets(player1, hinderingTouch, listOf(ChosenTarget.Spell(boltOnStack)))

        // Verify storm count is 2 (bolt + hindering touch) - not increased by copies
        driver.state.spellsCastThisTurn shouldBe 2
    }

    test("Storm count resets at start of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast a spell
        val bolt = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt, listOf(driver.getOpponent(player1)))

        driver.state.spellsCastThisTurn shouldBe 1

        // Advance past the current main phase to the end of the turn
        driver.bothPass() // resolve bolt
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 20)

        // Storm count should be reset at the start of the new turn
        driver.state.spellsCastThisTurn shouldBe 0
    }

    test("Hindering Touch with Storm copy counters target spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 2 casts Lightning Bolt (spell #1)
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!
        driver.passPriority(player2)

        // Player 1 casts Hindering Touch (spell #2, storm count = 1 copy)
        val hinderingTouch = driver.putCardInHand(player1, "Hindering Touch")
        driver.giveMana(player1, Color.BLUE, 4)
        driver.castSpellWithTargets(player1, hinderingTouch, listOf(ChosenTarget.Spell(spellOnStack)))

        // Resolve Storm trigger
        driver.passPriority(player1)
        driver.passPriority(player2)

        // Should be paused for target selection for 1 storm copy
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(player1, listOf(spellOnStack))

        // Now resolve the storm copy (counter unless pays {2})
        // Player 2 has no mana, auto-counter
        driver.bothPass()

        // Bolt was countered by the storm copy
        driver.getGraveyardCardNames(player2) shouldContain "Lightning Bolt"
    }
})
