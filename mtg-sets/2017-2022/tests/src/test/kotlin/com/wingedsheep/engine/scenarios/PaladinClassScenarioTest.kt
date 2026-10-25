package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.PaladinClass
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Paladin Class {W} — Enchantment — Class (AFR #29).
 *
 * Level 1's "spells your opponents cast during your turn cost {1} more" pins the cost gate's
 * player: "your turn" is the Class controller's turn, so the opponent is taxed on the
 * controller's turn and not on their own, and the controller's own spells are never taxed.
 * Level 2 is the anthem; level 3 pumps the targeted attacker by the *other* attackers and gives
 * it double strike — a lone attacker gets +0/+0 and double strike (2021-07-23 ruling).
 */
class PaladinClassScenarioTest : FunSpec({

    fun setup(level: Int): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PaladinClass)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Paladin Class", classLevel = level)
        return driver to you
    }

    fun GameTestDriver.boltCostFor(caster: EntityId) =
        CostCalculator(cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .calculateEffectiveCost(state, cardRegistry.requireCard("Lightning Bolt"), caster)

    test("level 1: opponents' spells cost {1} more during your turn only; yours never do") {
        val (driver, you) = setup(level = 1)
        val opponent = driver.getOpponent(you)

        withClue("on the Class controller's turn the opponent's {R} costs {1}{R}") {
            driver.boltCostFor(opponent).cmc shouldBe 2
        }
        withClue("the controller's own spells are untaxed") {
            driver.boltCostFor(you).cmc shouldBe 1
        }

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opponent)
        withClue("on the opponent's own turn \"your turn\" is false for the Class controller") {
            driver.boltCostFor(opponent).cmc shouldBe 1
        }
    }

    test("level 2: creatures you control get +1/+1") {
        val (driver, you) = setup(level = 2)
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(driver.getOpponent(you), "Grizzly Bears")

        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 3
        driver.state.projectedState.getPower(theirs) shouldBe 2
    }

    test("level 3: the targeted attacker gets +1/+1 per other attacker and double strike") {
        val (driver, you) = setup(level = 3)
        val attackers = List(3) { driver.putCreatureOnBattlefield(you, "Grizzly Bears") }
        attackers.forEach(driver::removeSummoningSickness)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, attackers, driver.getOpponent(you)).error shouldBe null
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(you, listOf(attackers[0])).error shouldBe null
        driver.bothPass()

        // 2/2 base, +1/+1 from level 2's anthem, +2/+2 for the two other attackers.
        driver.state.projectedState.getPower(attackers[0]) shouldBe 5
        driver.state.projectedState.getToughness(attackers[0]) shouldBe 5
        driver.state.projectedState.hasKeyword(attackers[0], Keyword.DOUBLE_STRIKE) shouldBe true
        driver.state.projectedState.getPower(attackers[1]) shouldBe 3
        driver.state.projectedState.hasKeyword(attackers[1], Keyword.DOUBLE_STRIKE) shouldBe false
    }

    test("level 3: a lone attacker gets no bonus but still gains double strike") {
        val (driver, you) = setup(level = 3)
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(bears), driver.getOpponent(you)).error shouldBe null
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(you, listOf(bears)).error shouldBe null
        driver.bothPass()

        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
    }
})
