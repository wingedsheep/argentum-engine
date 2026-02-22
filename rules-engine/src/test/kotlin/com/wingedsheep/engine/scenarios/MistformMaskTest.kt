package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.MistformMask
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Mistform Mask.
 *
 * Mistform Mask: {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * {1}: Enchanted creature becomes the creature type of your choice until end of turn.
 */
class MistformMaskTest : FunSpec({

    val maskAbilityId = MistformMask.activatedAbilities.first().id

    val GoblinWarrior = CardDefinition.creature(
        name = "Goblin Warrior",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 2,
        toughness = 2
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoblinWarrior))
        return driver
    }

    test("enchanted creature becomes chosen creature type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on battlefield and enchant it with Mistform Mask
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val mask = driver.putCardInHand(activePlayer, "Mistform Mask")
        driver.giveMana(activePlayer, Color.BLUE, 2)
        driver.castSpell(activePlayer, mask, listOf(goblin))
        driver.bothPass()

        // Verify initial subtype
        val projectedBefore = projector.project(driver.state)
        projectedBefore.getSubtypes(goblin) shouldBe setOf("Goblin")

        // Activate the mask's ability
        driver.giveMana(activePlayer, Color.BLUE, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mask,
                abilityId = maskAbilityId
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Choose "Elf"
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // Creature should now be an Elf, not a Goblin
        val projected = projector.project(driver.state)
        projected.getSubtypes(goblin) shouldBe setOf("Elf")
    }

    test("type change wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val mask = driver.putCardInHand(activePlayer, "Mistform Mask")
        driver.giveMana(activePlayer, Color.BLUE, 2)
        driver.castSpell(activePlayer, mask, listOf(goblin))
        driver.bothPass()

        // Activate the mask's ability
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mask,
                abilityId = maskAbilityId
            )
        )
        driver.bothPass()

        // Choose "Elf"
        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // Verify it's an Elf now
        val projected = projector.project(driver.state)
        projected.getSubtypes(goblin) shouldBe setOf("Elf")

        // Pass to next turn
        driver.passPriorityUntil(Step.UPKEEP)

        // Effect should have worn off, creature is Goblin again
        val projectedNextTurn = projector.project(driver.state)
        projectedNextTurn.getSubtypes(goblin) shouldBe setOf("Goblin")
    }

    test("can activate multiple times to change type again") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val mask = driver.putCardInHand(activePlayer, "Mistform Mask")
        driver.giveMana(activePlayer, Color.BLUE, 2)
        driver.castSpell(activePlayer, mask, listOf(goblin))
        driver.bothPass()

        // First activation: become Elf
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mask,
                abilityId = maskAbilityId
            )
        )
        driver.bothPass()
        val decision1 = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision1.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision1.id, elfIndex))

        val projectedAfterFirst = projector.project(driver.state)
        projectedAfterFirst.getSubtypes(goblin) shouldBe setOf("Elf")

        // Second activation: become Wizard
        driver.giveMana(activePlayer, Color.BLUE, 1)
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = mask,
                abilityId = maskAbilityId
            )
        )
        driver.bothPass()
        val decision2 = driver.pendingDecision as ChooseOptionDecision
        val wizardIndex = decision2.options.indexOf("Wizard")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision2.id, wizardIndex))

        // Should now be Wizard (latest type change wins)
        val projectedAfterSecond = projector.project(driver.state)
        projectedAfterSecond.getSubtypes(goblin) shouldBe setOf("Wizard")
    }
})
