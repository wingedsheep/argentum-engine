package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.DestroyAtEndOfCombatEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Serpentine Basilisk.
 *
 * Serpentine Basilisk: {2}{G}{G}
 * Creature — Basilisk
 * 2/3
 * Whenever Serpentine Basilisk deals combat damage to a creature,
 * destroy that creature at end of combat.
 * Morph {1}{G}{G}
 */
class SerpentineBasiliskTest : FunSpec({

    val SerpentineBasilisk = CardDefinition.creature(
        name = "Serpentine Basilisk",
        manaCost = ManaCost.parse("{2}{G}{G}"),
        subtypes = setOf(Subtype("Basilisk")),
        power = 2,
        toughness = 3,
        oracleText = "Whenever Serpentine Basilisk deals combat damage to a creature, destroy that creature at end of combat.\nMorph {1}{G}{G}",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = GameEvent.DealsDamageEvent(damageType = DamageType.Combat, recipient = RecipientFilter.AnyCreature),
                binding = TriggerBinding.SELF,
                effect = DestroyAtEndOfCombatEffect(EffectTarget.TriggeringEntity)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SerpentineBasilisk))
        return driver
    }

    test("basilisk destroys blocker at end of combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Serpentine Basilisk on the battlefield
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Serpentine Basilisk")
        driver.removeSummoningSickness(basilisk)

        // Put a 3/3 blocker (survives 2 combat damage but should be destroyed at end of combat)
        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with basilisk
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()

        // Block with Centaur Courser
        driver.declareBlockers(defender, mapOf(blocker to listOf(basilisk)))
        driver.bothPass()

        // Skip first strike damage
        driver.currentStep shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
        driver.bothPass()

        // Combat damage step - basilisk deals 2 to Centaur Courser, trigger fires
        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // Trigger goes on stack - both pass to resolve it
        driver.bothPass()

        // Centaur Courser should still be alive (3 toughness, took 2 damage)
        // but marked for destruction at end of combat
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null

        // Advance to end of combat - creature is destroyed
        driver.passPriorityUntil(Step.END_COMBAT)

        // Centaur Courser should now be destroyed
        driver.findPermanent(defender, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(defender) shouldContain "Centaur Courser"
    }

    test("trigger does not fire when dealing combat damage to a player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Serpentine Basilisk on the battlefield
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Serpentine Basilisk")
        driver.removeSummoningSickness(basilisk)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with basilisk (no blockers)
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // Skip first strike damage
        driver.bothPass()

        // Combat damage to player - trigger should NOT fire (toCreatureOnly)
        // Player just takes 2 damage, no trigger on stack
        driver.assertLifeTotal(defender, 18)
    }

    test("basilisk kills blocker that would survive the combat damage") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Basilisk (2/3) vs Grizzly Bears (2/2)
        // Bears die to combat damage anyway, but the trigger also fires
        val basilisk = driver.putCreatureOnBattlefield(attacker, "Serpentine Basilisk")
        driver.removeSummoningSickness(basilisk)

        val blocker = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack and block
        driver.declareAttackers(attacker, listOf(basilisk), defender)
        driver.bothPass()
        driver.declareBlockers(defender, mapOf(blocker to listOf(basilisk)))
        driver.bothPass()

        // Skip first strike damage
        driver.currentStep shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
        driver.bothPass()

        // Combat damage - Bears (2/2) take 2 lethal damage and die to SBA
        // Trigger also fires but Bears are already dead
        // Basilisk (2/3) takes 2 damage, survives with 1 toughness
        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // Resolve the trigger (if any) and advance
        driver.bothPass()

        // Bears should be in graveyard (died to lethal combat damage)
        driver.getGraveyardCardNames(defender) shouldContain "Grizzly Bears"

        // Basilisk should survive
        driver.findPermanent(attacker, "Serpentine Basilisk") shouldNotBe null
    }
})
