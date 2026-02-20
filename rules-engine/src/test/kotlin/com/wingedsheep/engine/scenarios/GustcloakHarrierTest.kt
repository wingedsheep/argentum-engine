package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Gustcloak Harrier.
 *
 * Gustcloak Harrier: {1}{W}{W}
 * Creature — Bird Soldier
 * 2/2
 * Flying
 * Whenever Gustcloak Harrier becomes blocked, you may untap it and remove it from combat.
 */
class GustcloakHarrierTest : FunSpec({

    val GustcloakHarrier = CardDefinition.creature(
        name = "Gustcloak Harrier",
        manaCost = ManaCost.parse("{1}{W}{W}"),
        subtypes = setOf(Subtype("Bird"), Subtype("Soldier")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhenever Gustcloak Harrier becomes blocked, you may untap it and remove it from combat.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = GameEvent.BecomesBlockedEvent,
                binding = TriggerBinding.SELF,
                effect = MayEffect(
                    TapUntapEffect(EffectTarget.Self, tap = false) then
                            RemoveFromCombatEffect(EffectTarget.Self)
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GustcloakHarrier))
        return driver
    }

    test("Gustcloak Harrier is untapped and removed from combat when blocked and player chooses yes") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Gustcloak Harrier on the battlefield
        val harrier = driver.putCreatureOnBattlefield(attacker, "Gustcloak Harrier")
        driver.removeSummoningSickness(harrier)

        // Put a flying blocker (can block the Harrier)
        val blocker = driver.putCreatureOnBattlefield(defender, "Wind Drake")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Harrier
        driver.declareAttackers(attacker, listOf(harrier), defender)

        // Harrier should be tapped from attacking
        driver.state.getEntity(harrier)?.has<TappedComponent>() shouldBe true

        driver.bothPass()

        // Block with Wind Drake
        driver.declareBlockers(defender, mapOf(blocker to listOf(harrier)))

        // Trigger fires and goes on stack. Both pass to resolve.
        driver.bothPass()

        // MayEffect creates a yes/no decision. Choose yes.
        driver.submitYesNo(attacker, true)

        // Harrier should be untapped (from the effect)
        driver.state.getEntity(harrier)?.has<TappedComponent>() shouldBe false

        // Harrier should no longer be attacking
        driver.state.getEntity(harrier)?.has<AttackingComponent>() shouldBe false

        // Harrier should not be marked as blocked
        driver.state.getEntity(harrier)?.has<BlockedComponent>() shouldBe false

        // The blocker should no longer be blocking (since the attacker was removed)
        driver.state.getEntity(blocker)?.has<BlockingComponent>() shouldBe false

        // Skip through combat - no damage should be dealt since Harrier was removed
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both creatures should still be alive
        driver.findPermanent(attacker, "Gustcloak Harrier") shouldNotBe null
        driver.findPermanent(defender, "Wind Drake") shouldNotBe null

        // No damage should have been dealt to either player
        driver.assertLifeTotal(attacker, 20)
        driver.assertLifeTotal(defender, 20)
    }

    test("Gustcloak Harrier stays in combat when player chooses no") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Gustcloak Harrier on the battlefield
        val harrier = driver.putCreatureOnBattlefield(attacker, "Gustcloak Harrier")
        driver.removeSummoningSickness(harrier)

        // Put a flying blocker
        val blocker = driver.putCreatureOnBattlefield(defender, "Wind Drake")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Harrier
        driver.declareAttackers(attacker, listOf(harrier), defender)
        driver.bothPass()

        // Block with Wind Drake
        driver.declareBlockers(defender, mapOf(blocker to listOf(harrier)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // MayEffect creates a yes/no decision. Choose no.
        driver.submitYesNo(attacker, false)

        // Harrier should still be tapped and attacking
        driver.state.getEntity(harrier)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(harrier)?.has<AttackingComponent>() shouldBe true

        // Advance through combat damage - Harrier (2/2) vs Wind Drake (2/2)
        // Both take lethal damage and die
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both should be dead
        driver.findPermanent(attacker, "Gustcloak Harrier") shouldBe null
        driver.findPermanent(defender, "Wind Drake") shouldBe null
    }

    test("Gustcloak Harrier deals no combat damage when removed from combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Gustcloak Harrier on the battlefield
        val harrier = driver.putCreatureOnBattlefield(attacker, "Gustcloak Harrier")
        driver.removeSummoningSickness(harrier)

        // Put a 2/2 flying blocker
        val blocker = driver.putCreatureOnBattlefield(defender, "Wind Drake")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Harrier
        driver.declareAttackers(attacker, listOf(harrier), defender)
        driver.bothPass()

        // Block
        driver.declareBlockers(defender, mapOf(blocker to listOf(harrier)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose yes - remove from combat
        driver.submitYesNo(attacker, true)

        // Advance past combat
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Defender's life should be unchanged (Harrier removed before damage)
        driver.assertLifeTotal(defender, 20)

        // Spider should be unharmed (still on battlefield)
        driver.findPermanent(defender, "Wind Drake") shouldNotBe null
    }
})
