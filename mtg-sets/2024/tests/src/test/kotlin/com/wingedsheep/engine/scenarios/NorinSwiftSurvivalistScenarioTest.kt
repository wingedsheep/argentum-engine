package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Norin, Swift Survivalist (DSK #145) — {R} Legendary Creature — Human Coward, 2/1.
 *
 *   "Norin can't block.
 *    Whenever a creature you control becomes blocked, you may exile it. You may play that card
 *    from exile this turn."
 *
 * Exercises (a) the [com.wingedsheep.sdk.scripting.CantBlock] combat static, and (b) the
 * ANY-binding becomes-blocked trigger feeding a gather → exile → grant pipeline over the new
 * [com.wingedsheep.sdk.scripting.effects.CardSource.Entity(com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity)] source, with an
 * end-of-turn play-from-exile permission.
 */
class NorinSwiftSurvivalistScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        return driver
    }

    test("accepting the trigger exiles the blocked creature and lets you play it this turn") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val norin = driver.putCreatureOnBattlefield(attacker, "Norin, Swift Survivalist")
        driver.removeSummoningSickness(norin)

        val attackingCreature = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(attackingCreature)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(attackingCreature), defender)
        driver.bothPass()

        // Defender blocks the Grizzly Bears → Norin's trigger fires.
        driver.declareBlockers(defender, mapOf(blocker to listOf(attackingCreature)))
        driver.bothPass() // pass priority so the trigger resolves
        driver.submitYesNo(attacker, true) // "you may exile it" → yes

        // Grizzly Bears is exiled (no longer on the battlefield, no combat components) and the
        // owner has permission to play it from exile.
        driver.findPermanent(attacker, "Grizzly Bears") shouldBe null
        driver.state.getEntity(attackingCreature)?.has<AttackingComponent>() shouldBe false
        driver.state.getEntity(attackingCreature)?.has<BlockedComponent>() shouldBe false

        val exiled = driver.getExile(attacker).single()
        driver.getExileCardNames(attacker) shouldBe listOf("Grizzly Bears")
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true

        // No combat damage dealt — the attacker left combat before damage.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.assertLifeTotal(defender, 20)
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null
    }

    test("declining the trigger leaves the creature in combat") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val norin = driver.putCreatureOnBattlefield(attacker, "Norin, Swift Survivalist")
        driver.removeSummoningSickness(norin)

        val attackingCreature = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(attackingCreature)

        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(attackingCreature), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(blocker to listOf(attackingCreature)))
        driver.bothPass()
        driver.submitYesNo(attacker, false) // decline the exile

        // Grizzly Bears stays in combat; nothing is exiled.
        driver.getExile(attacker) shouldBe emptyList()
        driver.state.getEntity(attackingCreature)?.has<AttackingComponent>() shouldBe true

        // 3/3 Centaur Courser trades with / kills the 2/2 Grizzly Bears in combat.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.findPermanent(attacker, "Grizzly Bears") shouldBe null
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null
    }

    test("Norin can't block") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        // Active player swings with a Grizzly Bears; defender controls Norin.
        val norin = driver.putCreatureOnBattlefield(defender, "Norin, Swift Survivalist")
        driver.removeSummoningSickness(norin)
        val attackingCreature = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(attackingCreature)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(attackingCreature), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Declaring Norin as a blocker is illegal ("Norin can't block").
        driver.submitExpectFailure(
            DeclareBlockers(defender, mapOf(norin to listOf(attackingCreature)))
        )
    }
})
