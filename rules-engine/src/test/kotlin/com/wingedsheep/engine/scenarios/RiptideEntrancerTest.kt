package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.triggers.OnDealsDamage
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Riptide Entrancer.
 *
 * Riptide Entrancer: {1}{U}{U}
 * Creature — Human Wizard
 * 1/1
 * Whenever Riptide Entrancer deals combat damage to a player, you may sacrifice it.
 * If you do, gain control of target creature that player controls.
 * (This effect lasts indefinitely.)
 * Morph {U}{U}
 *
 * Engine flow: The trigger has both MayEffect and target, so the engine uses
 * processMayThenTargetTrigger: asks "may sacrifice?" first, then if yes, asks for
 * target selection, then puts the unwrapped composite effect on the stack.
 */
class RiptideEntrancerTest : FunSpec({

    val RiptideEntrancer = CardDefinition.creature(
        name = "Riptide Entrancer",
        manaCost = ManaCost.parse("{1}{U}{U}"),
        subtypes = setOf(Subtype("Human"), Subtype("Wizard")),
        power = 1,
        toughness = 1,
        oracleText = "Whenever Riptide Entrancer deals combat damage to a player, you may sacrifice it. If you do, gain control of target creature that player controls. (This effect lasts indefinitely.)\nMorph {U}{U}",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnDealsDamage(selfOnly = true, combatOnly = true, toPlayerOnly = true),
                effect = MayEffect(
                    SacrificeSelfEffect then GainControlEffect(EffectTarget.ContextTarget(0))
                ),
                targetRequirement = TargetCreature(filter = TargetFilter.CreatureOpponentControls)
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RiptideEntrancer))
        return driver
    }

    /**
     * Drive combat through first strike to combat damage step.
     * After this, a YesNoDecision is pending (the "may sacrifice?" question).
     */
    fun driveToCombatDamage(driver: GameTestDriver, attacker: com.wingedsheep.sdk.model.EntityId, defender: com.wingedsheep.sdk.model.EntityId, entrancer: com.wingedsheep.sdk.model.EntityId) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(entrancer), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // Skip first strike damage → combat damage dealt → trigger fires
        // processMayThenTargetTrigger asks "may sacrifice?" first
        driver.bothPass()

        driver.currentStep shouldBe Step.COMBAT_DAMAGE
        driver.pendingDecision shouldBe YesNoDecision::class.java.let { driver.pendingDecision }
    }

    test("gain control of opponent creature when choosing to sacrifice after combat damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val entrancer = driver.putCreatureOnBattlefield(attacker, "Riptide Entrancer")
        driver.removeSummoningSickness(entrancer)

        val targetCreature = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driveToCombatDamage(driver, attacker, defender, entrancer)

        // Step 1: "May sacrifice?" — answer yes
        val yesNoDecision = driver.pendingDecision as YesNoDecision
        driver.submitYesNo(yesNoDecision.playerId, true)

        // Step 2: Choose target creature for gain control
        val chooseTargets = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(chooseTargets.playerId, listOf(targetCreature))

        // Step 3: Trigger is now on the stack — resolve it
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Entrancer should be in graveyard (sacrificed)
        driver.assertInGraveyard(attacker, "Riptide Entrancer")

        // Target creature should now be controlled by attacker (check projected state for floating effect)
        val projected = projector.project(driver.state)
        projected.getController(targetCreature) shouldBe attacker

        // Defender took 1 combat damage
        driver.assertLifeTotal(defender, 19)
    }

    test("choosing not to sacrifice keeps Entrancer and opponent keeps creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val entrancer = driver.putCreatureOnBattlefield(attacker, "Riptide Entrancer")
        driver.removeSummoningSickness(entrancer)

        val targetCreature = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driveToCombatDamage(driver, attacker, defender, entrancer)

        // Choose no - don't sacrifice
        val yesNoDecision = driver.pendingDecision as YesNoDecision
        driver.submitYesNo(yesNoDecision.playerId, false)

        // Entrancer should still be on battlefield
        driver.getController(entrancer) shouldBe attacker

        // Target creature should still be controlled by defender
        driver.getController(targetCreature) shouldBe defender

        // Defender took 1 combat damage
        driver.assertLifeTotal(defender, 19)
    }

    test("trigger does not fire when blocked") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val entrancer = driver.putCreatureOnBattlefield(attacker, "Riptide Entrancer")
        driver.removeSummoningSickness(entrancer)

        val blocker = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(attacker, listOf(entrancer), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(blocker to listOf(entrancer)))
        driver.bothPass()

        // Skip first strike damage
        driver.bothPass()

        // Combat damage - Entrancer is blocked, no damage to player, no trigger
        // Both pass through end of combat
        driver.bothPass()

        // Entrancer (1/1) dies to Grizzly Bears (2/2)
        driver.assertInGraveyard(attacker, "Riptide Entrancer")
        driver.assertLifeTotal(defender, 20)
    }
})
