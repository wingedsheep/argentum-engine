package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Step
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
 * Tests for Gustcloak Sentinel.
 *
 * Gustcloak Sentinel: {2}{W}{W}
 * Creature — Human Soldier
 * 3/3
 * Whenever Gustcloak Sentinel becomes blocked, you may untap it and remove it from combat.
 */
class GustcloakSentinelTest : FunSpec({

    val GustcloakSentinel = CardDefinition.creature(
        name = "Gustcloak Sentinel",
        manaCost = ManaCost.parse("{2}{W}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 3,
        toughness = 3,
        keywords = emptySet(),
        oracleText = "Whenever Gustcloak Sentinel becomes blocked, you may untap it and remove it from combat.",
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
        driver.registerCards(TestCards.all + listOf(GustcloakSentinel))
        return driver
    }

    test("Gustcloak Sentinel is untapped and removed from combat when blocked and player chooses yes") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val sentinel = driver.putCreatureOnBattlefield(attacker, "Gustcloak Sentinel")
        driver.removeSummoningSickness(sentinel)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(sentinel), defender)

        // Sentinel should be tapped from attacking
        driver.state.getEntity(sentinel)?.has<TappedComponent>() shouldBe true

        driver.bothPass()

        // Block with Hill Giant (3/3)
        driver.declareBlockers(defender, mapOf(blocker to listOf(sentinel)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose yes - untap and remove from combat
        driver.submitYesNo(attacker, true)

        // Sentinel should be untapped
        driver.state.getEntity(sentinel)?.has<TappedComponent>() shouldBe false

        // Sentinel should no longer be attacking
        driver.state.getEntity(sentinel)?.has<AttackingComponent>() shouldBe false

        // Sentinel should not be marked as blocked
        driver.state.getEntity(sentinel)?.has<BlockedComponent>() shouldBe false

        // The blocker should no longer be blocking
        driver.state.getEntity(blocker)?.has<BlockingComponent>() shouldBe false

        // Advance through combat - no damage
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both creatures should still be alive
        driver.findPermanent(attacker, "Gustcloak Sentinel") shouldNotBe null
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null

        // No damage to either player
        driver.assertLifeTotal(attacker, 20)
        driver.assertLifeTotal(defender, 20)
    }

    test("Gustcloak Sentinel stays in combat when player chooses no") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val sentinel = driver.putCreatureOnBattlefield(attacker, "Gustcloak Sentinel")
        driver.removeSummoningSickness(sentinel)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(sentinel), defender)
        driver.bothPass()

        // Block with Hill Giant (3/3)
        driver.declareBlockers(defender, mapOf(blocker to listOf(sentinel)))

        // Trigger fires. Both pass to resolve.
        driver.bothPass()

        // Choose no - stay in combat
        driver.submitYesNo(attacker, false)

        // Sentinel should still be tapped and attacking
        driver.state.getEntity(sentinel)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(sentinel)?.has<AttackingComponent>() shouldBe true

        // Both 3/3 creatures deal lethal to each other
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both should be dead
        driver.findPermanent(attacker, "Gustcloak Sentinel") shouldBe null
        driver.findPermanent(defender, "Centaur Courser") shouldBe null
    }
})
