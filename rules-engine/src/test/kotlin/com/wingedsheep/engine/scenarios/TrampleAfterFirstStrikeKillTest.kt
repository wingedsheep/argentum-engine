package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Regression: an attacker with double strike + trample blocked by a creature
 * its first-strike damage kills should still trample over for its regular
 * combat damage step.
 *
 * Bug history: the manual [DamageAssignmentComponent] set during the first
 * strike step persisted into the regular damage step. With the blocker now
 * dead, [CombatDamageManager.proposeDamageAssignments] re-emitted the same
 * assignment to a target that was no longer on the battlefield, where it
 * was silently dropped. CR 702.19c says trample damage must carry over to
 * the defending player when the blocker has been removed from combat.
 */
class TrampleAfterFirstStrikeKillTest : FunSpec({

    val DoubleStrikeTrampler = CardDefinition.creature(
        name = "Double Strike Trampler",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Beast")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.DOUBLE_STRIKE, Keyword.TRAMPLE)
    )

    val ToughBlocker = CardDefinition.creature(
        name = "Tough Blocker",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 3,
        toughness = 2
    )

    test("trample damage carries through after first strike kills the blocker") {
        // 2/2 double strike + trample attacks; 3/2 blocks.
        // First strike step: attacker assigns 2 to blocker → blocker dies.
        // Regular damage step: blocker is gone, trample carries 2 to player.
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DoubleStrikeTrampler, ToughBlocker))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Double Strike Trampler")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Tough Blocker")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true

        // Lets first-strike damage step run (auto-resolves the CombatResolutionDecision with
        // the default: 2 damage to the blocker, none to the player). Blocker dies as
        // SBA before the regular damage step.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Blocker died from first-strike damage.
        driver.findPermanent(opponent, "Tough Blocker") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Tough Blocker"

        // Attacker survives — blocker never reached its damage step.
        driver.findPermanent(activePlayer, "Double Strike Trampler") shouldNotBe null

        // Regular damage step: blocker is gone, trample sends 2 to defender.
        driver.assertLifeTotal(opponent, 18)
    }

    test("without trample, double-strike damage to dead blocker is lost (no spillover)") {
        // 2/2 double strike (no trample) attacks; 3/2 blocks.
        // First strike step: 2 to blocker → blocker dies.
        // Regular damage step: blocker is gone; no trample, so no damage to player.
        val DoubleStrikeNoTrample = CardDefinition.creature(
            name = "Double Strike Knight",
            manaCost = ManaCost.parse("{1}{W}"),
            subtypes = setOf(Subtype("Knight")),
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.DOUBLE_STRIKE)
        )

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DoubleStrikeNoTrample, ToughBlocker))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(activePlayer, "Double Strike Knight")
        val blocker = driver.putCreatureOnBattlefield(opponent, "Tough Blocker")
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(activePlayer, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).isSuccess shouldBe true
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.findPermanent(opponent, "Tough Blocker") shouldBe null
        driver.findPermanent(activePlayer, "Double Strike Knight") shouldNotBe null

        // No trample: damage to dead blocker is lost. Defender takes nothing.
        driver.assertLifeTotal(opponent, 20)
    }
})
