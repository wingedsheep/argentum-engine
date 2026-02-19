package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Ixidor's Will.
 *
 * Ixidor's Will: {2}{U}
 * Instant
 * Counter target spell unless its controller pays {2} for each Wizard on the battlefield.
 */
class IxidorsWillTest : FunSpec({

    val IxidorsWill = CardDefinition(
        name = "Ixidor's Will",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.parse("Instant"),
        oracleText = "Counter target spell unless its controller pays {2} for each Wizard on the battlefield.",
        script = CardScript.spell(
            effect = Effects.CounterUnlessDynamicPays(
                DynamicAmount.Multiply(
                    DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Wizard")),
                    2
                )
            ),
            Targets.Spell
        )
    )

    val TestWizard = CardDefinition(
        name = "Test Wizard",
        manaCost = ManaCost.parse("{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Wizard"))),
        oracleText = "",
        creatureStats = CreatureStats(1, 1),
        script = CardScript.permanent()
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IxidorsWill, TestWizard))
        return driver
    }

    test("does not counter when no wizards on battlefield (cost is 0)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 2 casts Lightning Bolt
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!

        driver.passPriority(player2)

        // Player 1 casts Ixidor's Will targeting the spell
        val ixidorsWill = driver.putCardInHand(player1, "Ixidor's Will")
        driver.giveMana(player1, Color.BLUE, 3)
        driver.castSpellWithTargets(player1, ixidorsWill, listOf(ChosenTarget.Spell(spellOnStack)))

        // Resolve Ixidor's Will - no wizards, cost is 0, spell is NOT countered
        driver.bothPass()

        // Spell should still be on the stack (cost was 0 so it resolves through)
        driver.stackSize shouldBe 1
        driver.getStackSpellNames() shouldContain "Lightning Bolt"
    }

    test("counters spell with wizards on battlefield when opponent declines to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 2 wizards on the battlefield (one for each player)
        driver.putCreatureOnBattlefield(player1, "Test Wizard")
        driver.putCreatureOnBattlefield(player2, "Test Wizard")

        // Player 2 casts Lightning Bolt
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!

        driver.passPriority(player2)

        // Player 1 casts Ixidor's Will
        val ixidorsWill = driver.putCardInHand(player1, "Ixidor's Will")
        driver.giveMana(player1, Color.BLUE, 3)
        driver.castSpellWithTargets(player1, ixidorsWill, listOf(ChosenTarget.Spell(spellOnStack)))

        // Give player 2 enough mana so they CAN pay (decision is offered)
        driver.giveMana(player2, Color.RED, 4)

        // Resolve Ixidor's Will - 2 wizards, cost is {4}
        driver.bothPass()

        // Player 2 should be asked to pay {4}
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldNotBeNull()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val decision = driver.pendingDecision as YesNoDecision
        decision.playerId shouldBe player2

        // Player 2 declines to pay
        driver.submitYesNo(player2, false)

        // Spell should be countered
        driver.getGraveyardCardNames(player2) shouldContain "Lightning Bolt"
    }

    test("spell resolves when controller pays the dynamic cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 1 wizard on the battlefield
        driver.putCreatureOnBattlefield(player1, "Test Wizard")

        // Player 2 casts Lightning Bolt
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!

        driver.passPriority(player2)

        // Player 1 casts Ixidor's Will
        val ixidorsWill = driver.putCardInHand(player1, "Ixidor's Will")
        driver.giveMana(player1, Color.BLUE, 3)
        driver.castSpellWithTargets(player1, ixidorsWill, listOf(ChosenTarget.Spell(spellOnStack)))

        // Give player 2 mana to pay {2} (1 wizard × 2)
        driver.giveMana(player2, Color.RED, 2)

        // Resolve Ixidor's Will
        driver.bothPass()

        // Player 2 should be asked to pay
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        // Player 2 chooses to pay
        driver.submitYesNo(player2, true)

        // Spell should still be on the stack
        driver.stackSize shouldBe 1
        driver.getStackSpellNames() shouldContain "Lightning Bolt"

        // Resolve the bolt
        driver.bothPass()

        // Player 1 should have taken 3 damage
        driver.getLifeTotal(player1) shouldBe 17
    }

    test("auto-counters when controller cannot pay the dynamic cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 3 wizards on the battlefield (cost will be {6})
        driver.putCreatureOnBattlefield(player1, "Test Wizard")
        driver.putCreatureOnBattlefield(player1, "Test Wizard")
        driver.putCreatureOnBattlefield(player2, "Test Wizard")

        // Player 2 casts Lightning Bolt with exact mana
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!

        driver.passPriority(player2)

        // Player 1 casts Ixidor's Will
        val ixidorsWill = driver.putCardInHand(player1, "Ixidor's Will")
        driver.giveMana(player1, Color.BLUE, 3)
        driver.castSpellWithTargets(player1, ixidorsWill, listOf(ChosenTarget.Spell(spellOnStack)))

        // Resolve Ixidor's Will - 3 wizards, cost is {6}, player 2 has no mana
        driver.bothPass()

        // Should auto-counter (no decision)
        driver.isPaused shouldBe false
        driver.getGraveyardCardNames(player2) shouldContain "Lightning Bolt"
    }

    test("cost scales with total wizard count from all players") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 1 wizard on each side (cost = {4})
        driver.putCreatureOnBattlefield(player1, "Test Wizard")
        driver.putCreatureOnBattlefield(player2, "Test Wizard")

        // Player 2 casts Lightning Bolt
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!

        driver.passPriority(player2)

        // Player 1 casts Ixidor's Will
        val ixidorsWill = driver.putCardInHand(player1, "Ixidor's Will")
        driver.giveMana(player1, Color.BLUE, 3)
        driver.castSpellWithTargets(player1, ixidorsWill, listOf(ChosenTarget.Spell(spellOnStack)))

        // Give player 2 only 3 mana (not enough for {4})
        driver.giveMana(player2, Color.RED, 3)

        // Resolve - player 2 has 3 mana but needs 4
        driver.bothPass()

        // Should auto-counter (can't pay {4})
        driver.isPaused shouldBe false
        driver.getGraveyardCardNames(player2) shouldContain "Lightning Bolt"
    }
})
