package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dis.cards.TrygonPredator
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Trygon Predator.
 *
 * Trygon Predator: {1}{G}{U}
 * Creature — Beast
 * 2/3
 * Flying
 * Whenever Trygon Predator deals combat damage to a player, you may destroy target
 * artifact or enchantment that player controls.
 *
 * The interesting bit is that the target is scoped to the *damaged* player
 * (Player.TriggeringPlayer → ControlledByReferencedPlayer), not "any player" — so the
 * controller's own artifacts/enchantments must never be legal targets.
 */
class TrygonPredatorScenarioTest : FunSpec({

    val TestArtifact = CardDefinition.artifact("Test Artifact", ManaCost.parse("{2}"))

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestArtifact))
        return driver
    }

    test("deals combat damage and destroys the damaged player's artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        val predator = driver.putCreatureOnBattlefield(attacker, "Trygon Predator")
        driver.removeSummoningSickness(predator)

        val artifact = driver.putPermanentOnBattlefield(defender, "Test Artifact")
        driver.findPermanent(defender, "Test Artifact") shouldNotBe null

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(predator), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()

        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // "You may destroy?" — yes.
        val yesNo = driver.pendingDecision as YesNoDecision
        driver.submitYesNo(yesNo.playerId, true)

        // Choose the target artifact.
        val choose = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(attacker, listOf(artifact))
        driver.bothPass()

        driver.findPermanent(defender, "Test Artifact") shouldBe null
        driver.getGraveyardCardNames(defender) shouldContain "Test Artifact"
        driver.assertLifeTotal(defender, 18)
    }

    test("destroys the damaged player's enchantment") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        val predator = driver.putCreatureOnBattlefield(attacker, "Trygon Predator")
        driver.removeSummoningSickness(predator)
        val enchantment = driver.putPermanentOnBattlefield(defender, "Test Enchantment")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(predator), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()

        val yesNo = driver.pendingDecision as YesNoDecision
        driver.submitYesNo(yesNo.playerId, true)
        val choose = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(attacker, listOf(enchantment))
        driver.bothPass()

        driver.findPermanent(defender, "Test Enchantment") shouldBe null
        driver.assertLifeTotal(defender, 18)
    }

    test("only the damaged player's permanents are legal targets — not the controller's own") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        val predator = driver.putCreatureOnBattlefield(attacker, "Trygon Predator")
        driver.removeSummoningSickness(predator)

        // Both players control an artifact; only the defender's should be targetable.
        driver.putPermanentOnBattlefield(attacker, "Test Artifact")
        val defenderArtifact = driver.putPermanentOnBattlefield(defender, "Test Artifact")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(predator), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()

        val yesNo = driver.pendingDecision as YesNoDecision
        driver.submitYesNo(yesNo.playerId, true)

        val choose = driver.pendingDecision as ChooseTargetsDecision
        // The scoped filter must offer exactly the defender's artifact, never the attacker's.
        choose.legalTargets[0]!! shouldContainExactly listOf(defenderArtifact)
    }

    test("declining leaves the artifact on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        val predator = driver.putCreatureOnBattlefield(attacker, "Trygon Predator")
        driver.removeSummoningSickness(predator)
        driver.putPermanentOnBattlefield(defender, "Test Artifact")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(predator), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()

        val yesNo = driver.pendingDecision as YesNoDecision
        driver.submitYesNo(yesNo.playerId, false)

        driver.findPermanent(defender, "Test Artifact") shouldNotBe null
        driver.assertLifeTotal(defender, 18)
    }

    test("no trigger goes on the stack when the damaged player controls no artifact or enchantment") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), startingLife = 20)

        val attacker = driver.player1
        val defender = driver.player2

        val predator = driver.putCreatureOnBattlefield(attacker, "Trygon Predator")
        driver.removeSummoningSickness(predator)

        // The attacker controls an artifact, but the defender controls none — the trigger
        // must not fire, proving the target isn't "any player".
        driver.putPermanentOnBattlefield(attacker, "Test Artifact")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(predator), defender)
        driver.bothPass()
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // No pending "may" decision; game proceeds and defender simply took 2 damage.
        driver.assertLifeTotal(defender, 18)
        driver.findPermanent(attacker, "Test Artifact") shouldNotBe null
    }
})
