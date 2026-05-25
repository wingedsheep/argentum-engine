package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * CR 509.3 / 508.4 — after the defending player declares blockers, the ACTIVE player
 * receives priority in the declare-blockers step. That window is where combat tricks
 * (e.g. Giant Growth) are cast, BEFORE the combat damage step. This pins down whether
 * the combat-board refactor preserved it.
 */
class DeclareBlockersPriorityWindowTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("active player gets a priority window in declare blockers before combat damage") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val beast = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")   // 3/3
        val blocker = driver.putCreatureOnBattlefield(defender, "Savannah Lions")  // 2/1
        driver.removeSummoningSickness(beast)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(beast), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Single blocker on a single attacker → no resolution board; damage would be
        // applied automatically when the step advances to COMBAT_DAMAGE.
        driver.declareBlockers(defender, mapOf(blocker to listOf(beast)))

        // After blocks are committed, the defending player holds priority (no pending decision).
        driver.state.pendingDecision.shouldBeNull()
        driver.state.priorityPlayerId shouldBe defender

        // Defending player passes. CR: priority should now pass to the ACTIVE player,
        // still in the declare-blockers step, with NO combat damage dealt yet.
        driver.submit(PassPriority(defender))

        driver.state.pendingDecision.shouldBeNull()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS
        driver.state.priorityPlayerId shouldBe attacker
        // No damage dealt: the 2/1 blocker is unharmed and still on the battlefield.
        driver.getCardName(blocker) shouldBe "Savannah Lions"
        driver.getLifeTotal(defender) shouldBe 20
    }

    test("attacker's combat trick is an affordable, targetable legal action at the declare-blockers window") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val beast = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")   // 3/3
        val blocker = driver.putCreatureOnBattlefield(defender, "Savannah Lions")  // 2/1
        driver.removeSummoningSickness(beast)
        // Give the attacker a combat trick and the mana to cast it.
        driver.putCardInHand(attacker, "Giant Growth")                            // {G} instant
        driver.putLandOnBattlefield(attacker, "Forest")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(beast), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(blocker to listOf(beast)))
        driver.submit(PassPriority(defender))

        // Active player now holds priority in declare blockers (pre-damage window).
        driver.state.priorityPlayerId shouldBe attacker
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Reproduce the server's legal-action view exactly (LegalActionEnumerator + enricher).
        val services = EngineServices(driver.cardRegistry)
        val enumerator = LegalActionEnumerator(
            driver.cardRegistry, services.manaSolver, services.costCalculator,
            services.predicateEvaluator, services.conditionEvaluator, services.turnManager
        )
        val enricher = LegalActionEnricher(services.manaSolver, driver.cardRegistry)
        val legalActions = enricher.enrich(enumerator.enumerate(driver.state, attacker), driver.state, attacker)

        val giantGrowth = legalActions.filter {
            it.actionType == "CastSpell" && it.description.contains("Giant Growth", ignoreCase = true)
        }
        // The AutoPassManager keys "is there a combat trick to stop for?" off exactly these fields:
        // a CastSpell that is affordable and (no targets required OR has valid targets).
        giantGrowth.shouldNotBeEmpty()
        giantGrowth.first().isAffordable shouldBe true
        giantGrowth.first().requiresTargets shouldBe true
        giantGrowth.first().validTargets.shouldNotBeNull()
        giantGrowth.first().validTargets!!.shouldNotBeEmpty()
    }

    test("combat trick is detectable in the two-blockers-block-two-attackers board shape") {
        val driver = createDriver()
        // "Block any number" blocker mirroring Ironfist Crusher (2/4).
        val crusher = CardDefinition.creature(
            name = "Test Crusher",
            manaCost = ManaCost.parse("{4}{W}"),
            subtypes = setOf(Subtype.SOLDIER),
            power = 2,
            toughness = 4,
            script = CardScript.creature(staticAbilities = listOf(CanBlockAnyNumber())),
        )
        driver.registerCard(crusher)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val a1 = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")  // 3/3
        val a2 = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")  // 3/3
        val b1 = driver.putCreatureOnBattlefield(defender, "Test Crusher")     // 2/4
        val b2 = driver.putCreatureOnBattlefield(defender, "Test Crusher")     // 2/4
        driver.removeSummoningSickness(a1)
        driver.removeSummoningSickness(a2)
        driver.putCardInHand(attacker, "Giant Growth")
        driver.putLandOnBattlefield(attacker, "Forest")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(a1, a2), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // Both blockers block both attackers (the user's exact board shape).
        driver.declareBlockers(defender, mapOf(b1 to listOf(a1, a2), b2 to listOf(a1, a2)))
        driver.state.pendingDecision.shouldBeNull()  // ordering folded into the board, no pause
        driver.submit(PassPriority(defender))

        // The active player must get the pre-damage window with the trick available.
        driver.state.priorityPlayerId shouldBe attacker
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS
        driver.state.pendingDecision.shouldBeNull()

        val services = EngineServices(driver.cardRegistry)
        val enumerator = LegalActionEnumerator(
            driver.cardRegistry, services.manaSolver, services.costCalculator,
            services.predicateEvaluator, services.conditionEvaluator, services.turnManager
        )
        val enricher = LegalActionEnricher(services.manaSolver, driver.cardRegistry)
        val legalActions = enricher.enrich(enumerator.enumerate(driver.state, attacker), driver.state, attacker)

        val giantGrowth = legalActions.filter {
            it.actionType == "CastSpell" && it.description.contains("Giant Growth", ignoreCase = true)
        }
        giantGrowth.shouldNotBeEmpty()
        giantGrowth.first().isAffordable shouldBe true
        giantGrowth.first().validTargets!!.shouldNotBeEmpty()
    }
})
