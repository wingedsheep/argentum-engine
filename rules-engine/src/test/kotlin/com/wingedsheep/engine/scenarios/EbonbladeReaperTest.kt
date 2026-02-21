package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Ebonblade Reaper.
 *
 * Ebonblade Reaper: {2}{B}
 * Creature — Human Cleric
 * 1/1
 * Whenever Ebonblade Reaper attacks, you lose half your life, rounded up.
 * Whenever Ebonblade Reaper deals combat damage to a player, that player loses half their life, rounded up.
 * Morph {3}{B}{B}
 */
class EbonbladeReaperTest : FunSpec({

    val EbonbladeReaper = card("Ebonblade Reaper") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1

        triggeredAbility {
            trigger = Triggers.Attacks
            effect = Effects.LoseHalfLife(roundUp = true, target = EffectTarget.Controller)
        }

        triggeredAbility {
            trigger = Triggers.DealsCombatDamageToPlayer
            effect = Effects.LoseHalfLife(
                roundUp = true,
                target = EffectTarget.PlayerRef(Player.Opponent),
                lifePlayer = Player.Opponent
            )
        }

        morph = "{3}{B}{B}"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EbonbladeReaper))
        return driver
    }

    /**
     * Helper to advance to player 1's declare attackers step.
     * Handles the case where player 2 may go first due to random turn order.
     */
    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    test("attacking with Ebonblade Reaper causes you to lose half your life rounded up") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val attacker = driver.player1
        val opponent = driver.player2

        val reaper = driver.putCreatureOnBattlefield(attacker, "Ebonblade Reaper")
        driver.removeSummoningSickness(reaper)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(attacker, listOf(reaper), opponent)

        // Resolve the attack trigger - you lose half your life (20 -> 10)
        driver.bothPass()

        driver.assertLifeTotal(attacker, 10)
    }

    test("combat damage trigger causes opponent to lose half their life rounded up") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val attacker = driver.player1
        val opponent = driver.player2

        val reaper = driver.putCreatureOnBattlefield(attacker, "Ebonblade Reaper")
        driver.removeSummoningSickness(reaper)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(attacker, listOf(reaper), opponent)

        // Resolve the attack trigger (attacker loses half life: 20 -> 10)
        driver.bothPass()
        driver.assertLifeTotal(attacker, 10)

        // Advance to declare blockers
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(opponent)
        driver.bothPass()

        // Skip first strike damage
        driver.currentStep shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
        driver.bothPass()

        // Combat damage step - reaper deals 1 damage, then combat damage trigger fires
        // Opponent life: 20 - 1 (combat damage) = 19, then loses half (10 rounded up) = 9
        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // Resolve the combat damage trigger
        driver.bothPass()

        driver.assertLifeTotal(opponent, 9)
        driver.assertLifeTotal(attacker, 10)
    }

    test("half life rounds up on odd life totals") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 15
        )

        val attacker = driver.player1
        val opponent = driver.player2

        val reaper = driver.putCreatureOnBattlefield(attacker, "Ebonblade Reaper")
        driver.removeSummoningSickness(reaper)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(attacker, listOf(reaper), opponent)

        // Resolve the attack trigger - lose half of 15 rounded up = 8, so 15 -> 7
        driver.bothPass()

        driver.assertLifeTotal(attacker, 7)
    }

    test("no combat damage trigger when blocked") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val attacker = driver.player1
        val opponent = driver.player2

        val reaper = driver.putCreatureOnBattlefield(attacker, "Ebonblade Reaper")
        driver.removeSummoningSickness(reaper)

        val blocker = driver.putCreatureOnBattlefield(opponent, "Wind Drake")
        driver.removeSummoningSickness(blocker)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(attacker, listOf(reaper), opponent)

        // Resolve the attack trigger (attacker still loses half life: 20 -> 10)
        driver.bothPass()
        driver.assertLifeTotal(attacker, 10)

        // Advance to declare blockers
        driver.bothPass()

        driver.declareBlockers(opponent, mapOf(blocker to listOf(reaper)))
        driver.bothPass()

        // Skip first strike damage
        driver.bothPass()

        // Combat damage - reaper is blocked and dies, no damage to player
        // No combat damage trigger should fire
        driver.assertLifeTotal(opponent, 20)
    }

    test("both triggers fire in sequence during unblocked attack") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        val attacker = driver.player1
        val opponent = driver.player2

        val reaper = driver.putCreatureOnBattlefield(attacker, "Ebonblade Reaper")
        driver.removeSummoningSickness(reaper)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(attacker, listOf(reaper), opponent)

        // Attack trigger: attacker loses half of 20 = 10
        driver.bothPass()
        driver.assertLifeTotal(attacker, 10)
        driver.assertLifeTotal(opponent, 20)

        // Advance to declare blockers
        driver.bothPass()

        driver.declareNoBlockers(opponent)
        driver.bothPass()

        // Skip first strike damage
        driver.bothPass()

        // Combat damage: 1 damage to opponent (20 -> 19)
        // Then combat damage trigger: opponent loses half of 19 rounded up = 10, so 19 -> 9
        driver.bothPass()

        driver.assertLifeTotal(attacker, 10)
        driver.assertLifeTotal(opponent, 9)
    }
})
