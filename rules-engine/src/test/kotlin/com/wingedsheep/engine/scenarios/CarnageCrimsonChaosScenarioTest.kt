package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Carnage, Crimson Chaos (SPM) — ETB reanimates a mv≤3 creature card and grants it "attacks each
 * combat if able" + "when it deals combat damage to a player, sacrifice it". Pins the
 * grant-abilities-to-a-reanimated-target composition (`GrantStaticAbilityEffect(MustAttack())` +
 * `GrantTriggeredAbilityEffect(... SacrificeSelfEffect)` with `Duration.Permanent`).
 */
class CarnageCrimsonChaosScenarioTest : FunSpec({

    val smallCreature = card("Carnage Test Grunt") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
    }

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(smallCreature))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("reanimates the target, which must attack and is sacrificed after dealing combat damage") {
        val (driver, you, opponent) = newGame()
        val grunt = driver.putCardInGraveyard(you, "Carnage Test Grunt") // mv 2

        // Cast Carnage; its ETB reanimates the grunt.
        driver.giveMana(you, Color.BLACK, 3)
        driver.giveMana(you, Color.RED, 1)
        val carnage = driver.putCardInHand(you, "Carnage, Crimson Chaos")
        driver.castSpell(you, carnage)
        resolveStack(driver)

        // ETB target: choose the grunt in the graveyard.
        val decision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(you, listOf(grunt))
        resolveStack(driver)

        driver.state.getBattlefield().contains(grunt) shouldBe true // reanimated
        driver.removeSummoningSickness(grunt) // so it can attack this turn

        // The granted "attacks each combat if able" is enforced: declaring no attackers is illegal
        // while the grunt can attack. (Regression: granted MustAttack lives in
        // grantedStaticAbilities, not projection, so the point-of-use check must consult it.)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, emptyList(), defendingPlayer = opponent).error shouldNotBe null

        // The grunt attacks; it deals combat damage and is then sacrificed (granted trigger).
        driver.declareAttackers(you, listOf(grunt), defendingPlayer = opponent).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        resolveStack(driver)

        // The grunt dealt 2 to the opponent, then sacrificed itself.
        driver.getLifeTotal(opponent) shouldBe 18
        driver.state.getBattlefield().contains(grunt) shouldBe false
    }
})
