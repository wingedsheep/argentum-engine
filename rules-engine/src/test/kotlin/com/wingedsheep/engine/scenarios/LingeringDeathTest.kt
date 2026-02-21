package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Lingering Death.
 *
 * Lingering Death: {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * At the beginning of the end step of enchanted creature's controller,
 * that player sacrifices that creature.
 */
class LingeringDeathTest : FunSpec({

    val LingeringDeath = CardDefinition.aura(
        name = "Lingering Death",
        manaCost = ManaCost.parse("{1}{B}"),
        oracleText = "Enchant creature\nAt the beginning of the end step of enchanted creature's controller, that player sacrifices that creature.",
        script = CardScript(
            auraTarget = TargetCreature(),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = GameEvent.EnchantedCreatureControllerStepEvent(Step.END),
                    binding = TriggerBinding.ANY,
                    effect = MoveToZoneEffect(
                        target = EffectTarget.EnchantedCreature,
                        destination = Zone.GRAVEYARD
                    )
                )
            )
        )
    )

    val TestCreature = CardDefinition.creature(
        name = "Test Creature",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Zombie")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LingeringDeath, TestCreature))
        return driver
    }

    fun advanceToPlayerEndStep(driver: GameTestDriver, targetPlayer: EntityId) {
        // Advance until we reach the end step of the target player's turn
        var passes = 0
        while (passes < 200) {
            if (driver.currentStep == Step.END && driver.activePlayer == targetPlayer) {
                break
            }
            driver.passPriority()
            passes++
        }
        driver.currentStep shouldBe Step.END
        driver.activePlayer shouldBe targetPlayer
    }

    test("enchanted creature is sacrificed at end of controller's turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on the battlefield
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Test Creature")
        driver.removeSummoningSickness(creature)

        // Cast Lingering Death on the creature
        val lingeringDeath = driver.putCardInHand(activePlayer, "Lingering Death")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, lingeringDeath, listOf(creature))
        driver.bothPass() // Resolve aura

        // Creature should still be on the battlefield
        driver.findPermanent(activePlayer, "Test Creature") shouldBe creature

        // Advance to active player's end step
        advanceToPlayerEndStep(driver, activePlayer)

        // Trigger should be on the stack
        driver.stackSize shouldBe 1

        // Resolve the trigger
        driver.bothPass()

        // Creature should be gone (sacrificed)
        driver.findPermanent(activePlayer, "Test Creature") shouldBe null

        // Verify it went to graveyard
        driver.getGraveyard(activePlayer).any { card ->
            driver.state.getComponent<com.wingedsheep.engine.state.components.CardComponent>(card)?.name == "Test Creature"
        } shouldBe true
    }

    test("trigger fires on enchanted creature's controller's end step, not aura controller's") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature on opponent's battlefield
        val creature = driver.putCreatureOnBattlefield(opponent, "Test Creature")
        driver.removeSummoningSickness(creature)

        // Active player casts Lingering Death on opponent's creature
        val lingeringDeath = driver.putCardInHand(activePlayer, "Lingering Death")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, lingeringDeath, listOf(creature))
        driver.bothPass() // Resolve aura

        // Advance to active player's end step - trigger should NOT fire
        advanceToPlayerEndStep(driver, activePlayer)

        // No trigger because the enchanted creature is controlled by opponent, not active player
        driver.stackSize shouldBe 0

        // Advance to opponent's end step
        advanceToPlayerEndStep(driver, opponent)

        // Now the trigger fires
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Creature should be gone
        driver.findPermanent(opponent, "Test Creature") shouldBe null
    }
})
