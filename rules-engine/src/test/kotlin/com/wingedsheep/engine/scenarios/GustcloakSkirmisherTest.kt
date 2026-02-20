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
import com.wingedsheep.sdk.scripting.triggers.OnBecomesBlocked
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Gustcloak Skirmisher.
 *
 * Gustcloak Skirmisher: {3}{W}
 * Creature — Bird Soldier
 * 2/3
 * Flying
 * Whenever Gustcloak Skirmisher becomes blocked, you may untap it and remove it from combat.
 */
class GustcloakSkirmisherTest : FunSpec({

    val GustcloakSkirmisher = CardDefinition.creature(
        name = "Gustcloak Skirmisher",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype("Bird"), Subtype("Soldier")),
        power = 2,
        toughness = 3,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhenever Gustcloak Skirmisher becomes blocked, you may untap it and remove it from combat.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnBecomesBlocked(selfOnly = true),
                effect = MayEffect(
                    TapUntapEffect(EffectTarget.Self, tap = false) then
                            RemoveFromCombatEffect(EffectTarget.Self)
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GustcloakSkirmisher))
        return driver
    }

    test("Gustcloak Skirmisher is untapped and removed from combat when blocked and player chooses yes") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Put Gustcloak Skirmisher on the battlefield
        val skirmisher = driver.putCreatureOnBattlefield(attacker, "Gustcloak Skirmisher")
        driver.removeSummoningSickness(skirmisher)

        // Put a flying blocker
        val blocker = driver.putCreatureOnBattlefield(defender, "Wind Drake")
        driver.removeSummoningSickness(blocker)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with Skirmisher
        driver.declareAttackers(attacker, listOf(skirmisher), defender)

        // Skirmisher should be tapped from attacking
        driver.state.getEntity(skirmisher)?.has<TappedComponent>() shouldBe true

        driver.bothPass()

        // Block with Wind Drake
        driver.declareBlockers(defender, mapOf(blocker to listOf(skirmisher)))

        // Trigger fires and goes on stack. Both pass to resolve.
        driver.bothPass()

        // MayEffect creates a yes/no decision. Choose yes.
        driver.submitYesNo(attacker, true)

        // Skirmisher should be untapped (from the effect)
        driver.state.getEntity(skirmisher)?.has<TappedComponent>() shouldBe false

        // Skirmisher should no longer be attacking
        driver.state.getEntity(skirmisher)?.has<AttackingComponent>() shouldBe false

        // Skirmisher should not be marked as blocked
        driver.state.getEntity(skirmisher)?.has<BlockedComponent>() shouldBe false

        // The blocker should no longer be blocking
        driver.state.getEntity(blocker)?.has<BlockingComponent>() shouldBe false

        // Skip through combat - no damage should be dealt since Skirmisher was removed
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both creatures should still be alive
        driver.findPermanent(attacker, "Gustcloak Skirmisher") shouldNotBe null
        driver.findPermanent(defender, "Wind Drake") shouldNotBe null

        // No damage should have been dealt to either player
        driver.assertLifeTotal(attacker, 20)
        driver.assertLifeTotal(defender, 20)
    }

    test("Gustcloak Skirmisher stays in combat when player chooses no") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val skirmisher = driver.putCreatureOnBattlefield(attacker, "Gustcloak Skirmisher")
        driver.removeSummoningSickness(skirmisher)

        // Put a flying blocker
        val blocker = driver.putCreatureOnBattlefield(defender, "Wind Drake")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(attacker, listOf(skirmisher), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(blocker to listOf(skirmisher)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose no - stay in combat
        driver.submitYesNo(attacker, false)

        // Skirmisher should still be tapped and attacking
        driver.state.getEntity(skirmisher)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(skirmisher)?.has<AttackingComponent>() shouldBe true

        // Advance through combat damage - Skirmisher (2/3) vs Wind Drake (2/2)
        // Wind Drake takes 2 damage (lethal), Skirmisher takes 2 damage (survives with 3 toughness)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Skirmisher survives (2/3 takes 2 damage)
        driver.findPermanent(attacker, "Gustcloak Skirmisher") shouldNotBe null

        // Wind Drake dies (2/2 takes 2 damage)
        driver.findPermanent(defender, "Wind Drake") shouldBe null
    }

    test("Gustcloak Skirmisher deals no combat damage when removed from combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val skirmisher = driver.putCreatureOnBattlefield(attacker, "Gustcloak Skirmisher")
        driver.removeSummoningSickness(skirmisher)

        val blocker = driver.putCreatureOnBattlefield(defender, "Wind Drake")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.declareAttackers(attacker, listOf(skirmisher), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(blocker to listOf(skirmisher)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose yes - remove from combat
        driver.submitYesNo(attacker, true)

        // Advance past combat
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Defender's life should be unchanged
        driver.assertLifeTotal(defender, 20)

        // Blocker should be unharmed
        driver.findPermanent(defender, "Wind Drake") shouldNotBe null
    }
})
