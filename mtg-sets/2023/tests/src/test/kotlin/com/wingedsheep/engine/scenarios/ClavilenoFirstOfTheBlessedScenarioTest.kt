package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lcc.cards.ClavilenoFirstOfTheBlessed
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Clavileño, First of the Blessed (LCC #2) — {1}{W}{B} Legendary Creature — Vampire Cleric 2/2.
 *
 * "Whenever you attack, target attacking Vampire that isn't a Demon becomes a Demon in addition to
 *  its other types. It gains 'When this creature dies, draw a card and create a tapped 4/3 white
 *  and black Vampire Demon creature token with flying.'"
 *
 * Proven end-to-end:
 *  1. Only attacking Vampires are legal targets — a non-Vampire attacker is not offered.
 *  2. The target becomes a Demon while keeping its Vampire type.
 *  3. The granted dies trigger survives into the creature's death: when the Demonized Vampire dies
 *     in combat, its controller draws a card and gets a *tapped* 4/3 flying Vampire Demon token.
 */
class ClavilenoFirstOfTheBlessedScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ClavilenoFirstOfTheBlessed)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.vampireDemonTokens(playerId: EntityId): List<EntityId> =
        getCreatures(playerId).filter { getCardName(it) == "Vampire Demon Token" }

    fun GameTestDriver.resolveCombatAndTriggers() {
        if (state.pendingDecision != null) confirmCombatDamage()
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("the attacking Vampire becomes a Demon and, when it dies, leaves a draw and a tapped Vampire Demon") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val clavileno = driver.putCreatureOnBattlefield(me, "Clavileño, First of the Blessed")
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val blocker = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        driver.removeSummoningSickness(clavileno)
        driver.removeSummoningSickness(courser)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(clavileno, courser), opp)

        val decision = driver.pendingDecision as ChooseTargetsDecision
        withClue("only the attacking Vampire is a legal target, not the non-Vampire attacker") {
            decision.legalTargets[0] shouldBe listOf(clavileno)
        }
        driver.submitTargetSelection(me, listOf(clavileno))
        driver.bothPass()

        withClue("it becomes a Demon in addition to its other types") {
            driver.state.projectedState.hasSubtype(clavileno, "Demon") shouldBe true
            driver.state.projectedState.hasSubtype(clavileno, "Vampire") shouldBe true
        }

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opp, mapOf(blocker to listOf(clavileno)))
        val handBefore = driver.getHandSize(me)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        driver.resolveCombatAndTriggers()

        withClue("the 2/2 died to the 3/3 blocker") {
            driver.assertInGraveyard(me, "Clavileño, First of the Blessed")
        }
        withClue("the granted dies trigger drew a card") {
            driver.getHandSize(me) shouldBe handBefore + 1
        }
        val tokens = driver.vampireDemonTokens(me)
        withClue("and created one Vampire Demon token") {
            tokens.size shouldBe 1
        }
        val token = tokens.first()
        withClue("a tapped 4/3 white and black flyer") {
            driver.isTapped(token) shouldBe true
            driver.state.projectedState.getPower(token) shouldBe 4
            driver.state.projectedState.getToughness(token) shouldBe 3
            driver.state.projectedState.hasKeyword(token, Keyword.FLYING) shouldBe true
        }
    }
})
