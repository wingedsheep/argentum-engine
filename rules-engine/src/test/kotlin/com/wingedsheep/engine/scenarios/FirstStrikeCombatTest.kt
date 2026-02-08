package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for first strike combat damage mechanics.
 *
 * First strike creatures deal damage in the first strike combat damage step.
 * Creatures killed by first strike damage don't deal regular combat damage.
 */
class FirstStrikeCombatTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("first strike attacker kills blocker before it can deal damage") {
        // 3/1 first strike attacks, 3/3 blocks
        // First strike step: 3/1 deals 3 to 3/3 (lethal), 3/3 dies
        // Regular step: 3/3 is dead, can't deal damage back
        // Result: 3/1 survives, 3/3 dies
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val knight = driver.putCreatureOnBattlefield(activePlayer, "First Strike Knight")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.removeSummoningSickness(knight)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(knight), opponent).isSuccess shouldBe true

        // Advance to declare blockers
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(knight))).isSuccess shouldBe true

        // Let combat damage happen through to postcombat main
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 3/1 first strike should survive - killed the 3/3 before it could hit back
        driver.findPermanent(activePlayer, "First Strike Knight") shouldNotBe null

        // 3/3 should be dead
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Centaur Courser"
    }

    test("first strike blocker kills attacker before regular damage") {
        // 3/3 attacks, 3/1 first strike blocks
        // First strike step: 3/1 deals 3 to 3/3 (lethal), 3/3 dies
        // Regular step: 3/3 is dead, can't deal damage back
        // Result: 3/1 survives, 3/3 dies
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")
        val blocker = driver.putCreatureOnBattlefield(opponent, "First Strike Knight")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 3/1 first strike blocker should survive
        driver.findPermanent(opponent, "First Strike Knight") shouldNotBe null

        // 3/3 attacker should be dead (killed by first strike before dealing damage)
        driver.findPermanent(activePlayer, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Centaur Courser"
    }

    test("first strike too weak — both deal damage in their respective steps") {
        // 2/1 first strike attacks, 3/3 blocks
        // First strike step: 2/1 deals 2 to 3/3 (not lethal)
        // Regular step: 3/3 deals 3 to 2/1 (lethal)
        // Result: 2/1 dies, 3/3 survives with 2 damage
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Blade of the Ninth Watch")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 2/1 first strike should be dead (3/3 survived first strike and hit back)
        driver.findPermanent(activePlayer, "Blade of the Ninth Watch") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Blade of the Ninth Watch"

        // 3/3 should survive (took 2 damage but has 3 toughness)
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null
    }

    test("both have first strike — simultaneous first strike damage") {
        // 2/1 FS attacks, 2/1 FS blocks
        // First strike step: both deal 2 damage to each other (both lethal)
        // Result: both die
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Blade of the Ninth Watch")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Blade of the Ninth Watch")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Both should be dead - simultaneous first strike damage
        driver.findPermanent(activePlayer, "Blade of the Ninth Watch") shouldBe null
        driver.findPermanent(opponent, "Blade of the Ninth Watch") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Blade of the Ninth Watch"
        driver.getGraveyardCardNames(opponent) shouldContain "Blade of the Ninth Watch"
    }

    test("unblocked first strike deals damage in first strike step") {
        // 2/1 first strike attacks unblocked
        // Player takes 2 damage in first strike step
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Blade of the Ninth Watch")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent)

        // Advance through first strike step
        driver.bothPass()
        driver.currentStep shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE

        // After first strike damage, opponent should have lost 2 life
        driver.bothPass()
        driver.assertLifeTotal(opponent, 18)
    }

    test("attacker killed by first strike blocker does not deal damage in normal step") {
        // 2/2 attacks, blocked by 3/1 first strike AND 1/1
        // First strike step: 3/1 deals 3 to 2/2 (lethal), 2/2 dies
        // Regular step: 2/2 is dead, can't deal damage; 1/1 can't deal damage to dead attacker
        // Result: 2/2 dies, 3/1 and 1/1 both survive
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val fsBlocker = driver.putCreatureOnBattlefield(opponent, "First Strike Knight")
        val normalBlocker = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(
            fsBlocker to listOf(attacker),
            normalBlocker to listOf(attacker)
        ))

        // Multiple blockers require damage assignment order decision
        val orderDecision = driver.pendingDecision as OrderObjectsDecision
        driver.submitDecision(
            activePlayer,
            OrderedResponse(orderDecision.id, listOf(fsBlocker, normalBlocker))
        )

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Attacker should be dead (killed by first strike)
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldContain "Grizzly Bears"

        // Both blockers should survive - attacker died before normal damage step
        driver.findPermanent(opponent, "First Strike Knight") shouldNotBe null
        driver.findPermanent(opponent, "Savannah Lions") shouldNotBe null
    }

    test("blocked first strike does not trample to player") {
        // 3/1 first strike attacks, 1/1 blocks
        // First strike step: 3/1 deals 3 to 1/1 (lethal, excess 2)
        // No trample, so excess damage does NOT go to player
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "First Strike Knight")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // 1/1 should be dead
        driver.findPermanent(opponent, "Savannah Lions") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Savannah Lions"

        // 3/1 first strike should survive (1/1 can't deal lethal)
        driver.findPermanent(activePlayer, "First Strike Knight") shouldNotBe null

        // Opponent should still be at 20 life (no trample)
        driver.assertLifeTotal(opponent, 20)
    }
})
