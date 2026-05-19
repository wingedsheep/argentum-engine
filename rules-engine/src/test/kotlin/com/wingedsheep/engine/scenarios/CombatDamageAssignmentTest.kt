package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatDamagePlanDecision
import com.wingedsheep.engine.core.CombatDamagePlanResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for combat damage assignment UI flow.
 *
 * When an attacker has trample or multiple blockers with excess power,
 * the attacking player is presented with an AssignDamageDecision to
 * choose how to distribute combat damage.
 */
class CombatDamageAssignmentTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("trample creature with single blocker presents damage assignment decision") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 5/5 trample attacking into 2/2 blocker
        val trampler = driver.putCreatureOnBattlefield(activePlayer, "Trample Beast")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(trampler)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(trampler), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(trampler))).isSuccess shouldBe true

        // Advance to combat damage - should present CombatDamagePlanDecision (the batched
        // form emitted since commit 61a92bf57; for a single manual-assignment attacker it
        // carries one entry).
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<CombatDamagePlanDecision>()
        decision.entries.size shouldBe 1
        val entry = decision.entries.single()
        entry.attackerId shouldBe trampler
        entry.availablePower shouldBe 5
        entry.orderedTargets shouldBe listOf(blocker)
        entry.defenderId shouldBe opponent
        entry.hasTrample shouldBe true
        entry.hasDeathtouch shouldBe false

        // Default assignments: 2 to blocker (lethal), 3 to player
        entry.defaultAssignments[blocker] shouldBe 2
        entry.defaultAssignments[opponent] shouldBe 3

        // Submit default assignment
        driver.submitDecision(
            activePlayer,
            CombatDamagePlanResponse(decision.id, mapOf(entry.attackerId to entry.defaultAssignments))
        )

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Blocker should be dead
        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"

        // 3 trample damage to player (5 - 2 lethal = 3)
        driver.assertLifeTotal(opponent, 17)
    }

    test("trample creature with custom damage assignment - all to blocker") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val trampler = driver.putCreatureOnBattlefield(activePlayer, "Trample Beast")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(trampler)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(trampler), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(trampler))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        val decision = driver.pendingDecision as CombatDamagePlanDecision
        val entry = decision.entries.single()

        // Override: assign 4 to blocker, 1 to player
        driver.submitDecision(
            activePlayer,
            CombatDamagePlanResponse(decision.id, mapOf(entry.attackerId to mapOf(blocker to 4, opponent to 1)))
        )

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Blocker dead, only 1 damage to player
        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        driver.assertLifeTotal(opponent, 19)
    }

    test("multiple blockers with damage assignment order decision then damage assignment") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 5/5 trample attacking into two blockers
        val trampler = driver.putCreatureOnBattlefield(activePlayer, "Trample Beast")
        val blocker1 = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")  // 2/2
        val blocker2 = driver.putCreatureOnBattlefield(opponent, "Goblin Guide")    // 2/1
        driver.removeSummoningSickness(trampler)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(trampler), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(
            blocker1 to listOf(trampler),
            blocker2 to listOf(trampler)
        ))

        // First: damage assignment order decision
        val orderDecision = driver.pendingDecision as OrderObjectsDecision
        driver.submitDecision(
            activePlayer,
            OrderedResponse(orderDecision.id, listOf(blocker1, blocker2))
        )

        // Advance to combat damage - should present CombatDamagePlanDecision
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<CombatDamagePlanDecision>()
        val entry = decision.entries.single()
        entry.attackerId shouldBe trampler
        entry.availablePower shouldBe 5
        entry.orderedTargets shouldBe listOf(blocker1, blocker2)
        entry.defenderId shouldBe opponent
        entry.hasTrample shouldBe true

        // Default: 2 to bears (lethal), 1 to goblin (lethal), 2 to player
        entry.defaultAssignments[blocker1] shouldBe 2
        entry.defaultAssignments[blocker2] shouldBe 1
        entry.defaultAssignments[opponent] shouldBe 2

        // Submit default
        driver.submitDecision(
            activePlayer,
            CombatDamagePlanResponse(decision.id, mapOf(entry.attackerId to entry.defaultAssignments))
        )

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both blockers dead
        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        driver.findPermanent(opponent, "Goblin Guide").shouldBeNull()

        // 2 trample damage to player (5 - 2 - 1 = 2)
        driver.assertLifeTotal(opponent, 18)
    }

    test("deathtouch + trample assigns 1 damage per blocker, rest to player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 3/3 deathtouch trample attacking into 2/2 blocker
        val trampler = driver.putCreatureOnBattlefield(activePlayer, "Deathtouch Trampler")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2
        driver.removeSummoningSickness(trampler)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(trampler), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(trampler))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<CombatDamagePlanDecision>()
        val entry = decision.entries.single()
        entry.hasDeathtouch shouldBe true
        entry.hasTrample shouldBe true

        // With deathtouch, 1 damage is lethal. Default: 1 to blocker, 2 to player
        entry.minimumAssignments[blocker] shouldBe 1
        entry.defaultAssignments[blocker] shouldBe 1
        entry.defaultAssignments[opponent] shouldBe 2

        driver.submitDecision(
            activePlayer,
            CombatDamagePlanResponse(decision.id, mapOf(entry.attackerId to entry.defaultAssignments))
        )

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 2 trample damage to player (3 power - 1 to blocker = 2)
        driver.assertLifeTotal(opponent, 18)

        // 704.5h: blocker received 1 deathtouch damage, so it dies
        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
    }

    test("deathtouch 1/1 kills larger creature via 704.5h") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 1/1 deathtouch attacking into 5/5 blocker
        val rat = driver.putCreatureOnBattlefield(activePlayer, "Deathtouch Rat")
        val bigCreature = driver.putCreatureOnBattlefield(opponent, "Force of Nature") // 5/5
        driver.removeSummoningSickness(rat)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(rat), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(bigCreature to listOf(rat))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 704.5h: 5/5 received 1 deathtouch damage — it dies
        driver.findPermanent(opponent, "Force of Nature").shouldBeNull()

        // 1/1 rat also dies from 5 damage
        driver.findPermanent(activePlayer, "Deathtouch Rat").shouldBeNull()
    }

    test("single blocker without trample - no decision needed, auto-assigned") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 5/5 without trample attacking into 2/2 blocker - no choice needed
        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Force of Nature")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        // Should pass straight through combat damage with no decision
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Blocker dead, all 5 damage to blocker (no trample)
        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()

        // No trample damage to player
        driver.assertLifeTotal(opponent, 20)

        // Attacker survives (5/5 took 2 damage from blocker)
        driver.findPermanent(activePlayer, "Force of Nature").shouldNotBeNull()
    }

    test("auto-resolve uses default assignments for trample") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val trampler = driver.putCreatureOnBattlefield(activePlayer, "Trample Beast")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.removeSummoningSickness(trampler)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(trampler), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(trampler))).isSuccess shouldBe true

        // Use passPriorityUntil which auto-resolves decisions
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Default assignment: 2 to blocker (lethal), 3 to player
        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()
        driver.assertLifeTotal(opponent, 17)
    }

    test("Daunting Defender - default assignment accounts for prevention and kills Cleric") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Set up: 5/5 trample attacks, 2/2 Cleric blocks, Daunting Defender on battlefield
        val trampler = driver.putCreatureOnBattlefield(activePlayer, "Trample Beast") // 5/5 trample
        val clericBlocker = driver.putCreatureOnBattlefield(opponent, "Test Cleric")  // 2/2 Cleric
        val defender = driver.putCreatureOnBattlefield(opponent, "Daunting Defender")  // 3/3, prevents 1 to Clerics
        driver.removeSummoningSickness(trampler)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(trampler), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(clericBlocker to listOf(trampler))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<CombatDamagePlanDecision>()
        val entry = decision.entries.single()

        // Minimum is based on toughness only (2), per MTG rules
        entry.minimumAssignments[clericBlocker] shouldBe 2
        // Default accounts for Daunting Defender's prevention: 2 toughness + 1 prevention = 3
        entry.defaultAssignments[clericBlocker] shouldBe 3
        entry.defaultAssignments[opponent] shouldBe 2

        // Submit default: 3 to cleric (overcomes prevention), 2 to player
        driver.submitDecision(
            activePlayer,
            CombatDamagePlanResponse(decision.id, mapOf(entry.attackerId to entry.defaultAssignments))
        )

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 3 assigned - 1 prevented = 2 damage dealt = lethal to 2/2 Cleric
        driver.findPermanent(opponent, "Test Cleric").shouldBeNull()
        driver.getGraveyardCardNames(opponent) shouldContain "Test Cleric"

        // 2 trample damage to player
        driver.assertLifeTotal(opponent, 18)

        // Both Daunting Defender and Trample Beast survive
        driver.findPermanent(opponent, "Daunting Defender").shouldNotBeNull()
        driver.findPermanent(activePlayer, "Trample Beast").shouldNotBeNull()
    }

    test("Daunting Defender - player assigns minimum (toughness only) and Cleric survives") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val trampler = driver.putCreatureOnBattlefield(activePlayer, "Trample Beast") // 5/5 trample
        val clericBlocker = driver.putCreatureOnBattlefield(opponent, "Test Cleric")  // 2/2 Cleric
        val defender = driver.putCreatureOnBattlefield(opponent, "Daunting Defender")  // prevents 1
        driver.removeSummoningSickness(trampler)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(trampler), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(clericBlocker to listOf(trampler))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.COMBAT_DAMAGE)

        val decision = driver.pendingDecision as CombatDamagePlanDecision
        val entry = decision.entries.single()

        // Player deliberately assigns only toughness-worth of damage (2) to maximize trample.
        // This is below the prevention-aware default (3) but still valid per MTG rules
        // (prevention doesn't change what counts as "lethal" for assignment ordering purposes).
        driver.submitDecision(
            activePlayer,
            CombatDamagePlanResponse(
                decision.id,
                mapOf(entry.attackerId to mapOf(clericBlocker to 2, opponent to 3)),
            ),
        )

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 2 assigned - 1 prevented = 1 actual damage, less than 2 toughness → Cleric survives!
        driver.findPermanent(opponent, "Test Cleric").shouldNotBeNull()

        // 3 trample damage to player (player chose to maximize trample over kill)
        driver.assertLifeTotal(opponent, 17)
    }
})
