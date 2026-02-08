package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
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
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Disruptive Pitmage.
 *
 * Disruptive Pitmage: {2}{U}
 * Creature — Human Wizard
 * 1/1
 * {T}: Counter target spell unless its controller pays {1}.
 * Morph {U}
 */
class DisruptivePitmageTest : FunSpec({

    val pitmageAbilityId = AbilityId(UUID.randomUUID().toString())

    val DisruptivePitmage = CardDefinition(
        name = "Disruptive Pitmage",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Wizard"))),
        oracleText = "{T}: Counter target spell unless its controller pays {1}.\nMorph {U}",
        creatureStats = CreatureStats(1, 1),
        keywordAbilities = listOf(KeywordAbility.Morph(ManaCost.parse("{U}"))),
        script = CardScript.permanent(
            ActivatedAbility(
                id = pitmageAbilityId,
                cost = AbilityCost.Tap,
                effect = Effects.CounterUnlessPays("{1}"),
                targetRequirement = Targets.Spell
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DisruptivePitmage))
        return driver
    }

    test("counter succeeds when opponent declines to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Pitmage on battlefield and remove summoning sickness
        val pitmage = driver.putCreatureOnBattlefield(activePlayer, "Disruptive Pitmage")
        driver.removeSummoningSickness(pitmage)

        // Active player casts Lightning Bolt (puts it on the stack, active player retains priority)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        val castResult = driver.castSpell(activePlayer, bolt, listOf(opponent))
        castResult.isSuccess shouldBe true

        // Lightning Bolt is now on the stack
        driver.stackSize shouldBe 1
        val spellOnStack = driver.getTopOfStack()!!

        // Active player activates Pitmage targeting their own spell
        // (In a real game this would target an opponent's spell, but for testing
        // we're just verifying the counter-unless-pays mechanic works)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = pitmage,
                abilityId = pitmageAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack))
            )
        )
        result.isSuccess shouldBe true

        // Stack: ability on top, Lightning Bolt below
        driver.stackSize shouldBe 2

        // Give active player mana so they CAN pay (must be before resolution)
        driver.giveMana(activePlayer, Color.RED, 1)

        // Both pass to resolve the Pitmage ability
        driver.bothPass()

        // The spell's controller should now be presented with a YesNo decision
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldNotBeNull()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val decision = driver.pendingDecision as YesNoDecision
        decision.playerId shouldBe activePlayer

        // Spell controller declines to pay
        driver.submitYesNo(activePlayer, false)

        // Spell should be countered (in graveyard)
        driver.getGraveyardCardNames(activePlayer) shouldContain "Lightning Bolt"
    }

    test("spell resolves when controller pays the cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Pitmage on battlefield
        val pitmage = driver.putCreatureOnBattlefield(activePlayer, "Disruptive Pitmage")
        driver.removeSummoningSickness(pitmage)

        // Active player casts Lightning Bolt (instant)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        val spellOnStack = driver.getTopOfStack()!!

        // Active player activates Pitmage targeting their own spell
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = pitmage,
                abilityId = pitmageAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack))
            )
        )

        // Give active player mana to pay {1} (must be before resolution)
        driver.giveMana(activePlayer, Color.RED, 1)

        // Both pass to resolve the ability
        driver.bothPass()

        // Controller should be asked to pay
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        // Controller chooses to pay
        driver.submitYesNo(activePlayer, true)

        // Lightning Bolt should still be on the stack (not countered)
        driver.stackSize shouldBe 1
        driver.getStackSpellNames() shouldContain "Lightning Bolt"

        // Now resolve the bolt spell
        driver.bothPass()

        // Opponent should have taken 3 damage
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("auto-counters when controller has no mana to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Pitmage on battlefield
        val pitmage = driver.putCreatureOnBattlefield(activePlayer, "Disruptive Pitmage")
        driver.removeSummoningSickness(pitmage)

        // Active player casts Lightning Bolt with exact mana (no floating mana left)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        val spellOnStack = driver.getTopOfStack()!!

        // Active player activates Pitmage targeting the spell
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = pitmage,
                abilityId = pitmageAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack))
            )
        )

        // Both pass to resolve the ability
        driver.bothPass()

        // Active player has no mana in pool — spell should be auto-countered
        // No YesNo decision should be presented
        driver.isPaused shouldBe false
        driver.getGraveyardCardNames(activePlayer) shouldContain "Lightning Bolt"
    }

    test("offers pay decision when opponent has untapped lands but no floating mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 1 has Pitmage on battlefield
        val pitmage = driver.putCreatureOnBattlefield(player1, "Disruptive Pitmage")
        driver.removeSummoningSickness(pitmage)

        // Player 2 has 4 untapped lands (no floating mana)
        driver.putLandOnBattlefield(player2, "Mountain")
        driver.putLandOnBattlefield(player2, "Mountain")
        driver.putLandOnBattlefield(player2, "Mountain")
        driver.putLandOnBattlefield(player2, "Mountain")

        // Player 2 casts a 3-mana spell using floating mana (simulating tapping 3 lands)
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1) // Pass to player 2
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!

        // Player 1 responds by activating Pitmage
        val result = driver.submit(
            ActivateAbility(
                playerId = player1,
                sourceId = pitmage,
                abilityId = pitmageAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack))
            )
        )
        result.isSuccess shouldBe true

        // Both pass to resolve the Pitmage ability
        driver.bothPass()

        // Player 2 has no floating mana but has 4 untapped lands — should still get the decision
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldNotBeNull()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

        val decision = driver.pendingDecision as YesNoDecision
        decision.playerId shouldBe player2
    }

    test("Pitmage taps when ability is activated") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Pitmage on battlefield
        val pitmage = driver.putCreatureOnBattlefield(activePlayer, "Disruptive Pitmage")
        driver.removeSummoningSickness(pitmage)

        // Active player casts Lightning Bolt (instant)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.giveMana(activePlayer, Color.RED, 1)
        driver.castSpell(activePlayer, bolt, listOf(opponent))

        val spellOnStack = driver.getTopOfStack()!!

        // Activate Pitmage
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = pitmage,
                abilityId = pitmageAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack))
            )
        )

        // Pitmage should be tapped
        driver.isTapped(pitmage) shouldBe true
    }
})
