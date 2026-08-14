package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.KellanPlanarTrailblazer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kellan, Planar Trailblazer (FDN #91).
 *
 * {1}{R}: If Kellan is a Scout, it becomes a Human Faerie Detective and gains "Whenever Kellan
 *   deals combat damage to a player, exile the top card of your library. You may play that card
 *   this turn."
 * {2}{R}: If Kellan is a Detective, it becomes a 3/2 Human Faerie Rogue and gains double strike.
 *
 * The interesting parts of the card are all about *when* the "If Kellan is a …" clause is checked
 * and how permanent its consequences are, so that's what these cover:
 *  - step 1 replaces the Scout type (it doesn't add Detective alongside it) and confers a working
 *    combat-damage impulse trigger;
 *  - the type gate is a resolution-time state test, so activating step 2 out of order is legal and
 *    simply fizzles into a no-op;
 *  - step 2 after step 1 sets base 3/2, replaces Detective with Rogue, and grants double strike;
 *  - none of it has a duration, so all of it survives the turn ending.
 */
class KellanPlanarTrailblazerScenarioTest : FunSpec({

    val becomeDetectiveId = KellanPlanarTrailblazer.activatedAbilities[0].id
    val becomeRogueId = KellanPlanarTrailblazer.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KellanPlanarTrailblazer))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Resolve Kellan's first ability, leaving him a Human Faerie Detective. */
    fun becomeDetective(driver: GameTestDriver, you: EntityId, kellan: EntityId) {
        driver.giveMana(you, Color.RED, 2)
        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = kellan, abilityId = becomeDetectiveId)
        )
        driver.bothPass()
    }

    test("first ability replaces Scout with Human Faerie Detective") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kellan = driver.putCreatureOnBattlefield(you, "Kellan, Planar Trailblazer")
        driver.state.projectedState.hasSubtype(kellan, Subtype.SCOUT.value) shouldBe true

        becomeDetective(driver, you, kellan)

        val projected = driver.state.projectedState
        projected.hasSubtype(kellan, Subtype.DETECTIVE.value) shouldBe true
        projected.hasSubtype(kellan, Subtype.HUMAN.value) shouldBe true
        projected.hasSubtype(kellan, Subtype.FAERIE.value) shouldBe true
        // The types are *replaced*, not added to — Scout is gone, which is what makes the chain
        // one-way (the first ability can never apply a second time).
        projected.hasSubtype(kellan, Subtype.SCOUT.value) shouldBe false
        // Still a vanilla 2/1 at this point — only the second ability changes the body.
        projected.getPower(kellan) shouldBe 2
        projected.getToughness(kellan) shouldBe 1
    }

    test("granted trigger impulse-exiles the top card when Kellan connects") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kellan = driver.putCreatureOnBattlefield(you, "Kellan, Planar Trailblazer")
        driver.removeSummoningSickness(kellan)
        becomeDetective(driver, you, kellan)

        driver.getExile(you).size shouldBe 0

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(kellan), opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision is CombatResolutionDecision) {
            driver.confirmCombatDamage()
        }
        driver.bothPass() // resolve the granted combat-damage trigger

        driver.getLifeTotal(opponent) shouldBe 18
        // "exile the top card of your library. You may play that card this turn."
        driver.getExile(you).size shouldBe 1
    }

    test("without the first ability there is no combat-damage trigger") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kellan = driver.putCreatureOnBattlefield(you, "Kellan, Planar Trailblazer")
        driver.removeSummoningSickness(kellan)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(kellan), opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent).error shouldBe null
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (driver.pendingDecision is CombatResolutionDecision) {
            driver.confirmCombatDamage()
        }
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getExile(you).size shouldBe 0
    }

    test("second ability activated out of order resolves as a no-op") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kellan = driver.putCreatureOnBattlefield(you, "Kellan, Planar Trailblazer")
        driver.giveMana(you, Color.RED, 3)

        // Activating is legal regardless of Kellan's current types — the check happens on
        // resolution, and a Scout simply isn't a Detective.
        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = kellan, abilityId = becomeRogueId)
        )
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(kellan) shouldBe 2
        projected.getToughness(kellan) shouldBe 1
        projected.hasSubtype(kellan, Subtype.SCOUT.value) shouldBe true
        projected.hasSubtype(kellan, Subtype.ROGUE.value) shouldBe false
        projected.hasKeyword(kellan, Keyword.DOUBLE_STRIKE) shouldBe false
    }

    test("second ability after the first makes a 3/2 Human Faerie Rogue with double strike") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kellan = driver.putCreatureOnBattlefield(you, "Kellan, Planar Trailblazer")
        becomeDetective(driver, you, kellan)

        driver.giveMana(you, Color.RED, 3)
        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = kellan, abilityId = becomeRogueId)
        )
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(kellan) shouldBe 3
        projected.getToughness(kellan) shouldBe 2
        projected.hasSubtype(kellan, Subtype.ROGUE.value) shouldBe true
        projected.hasSubtype(kellan, Subtype.HUMAN.value) shouldBe true
        projected.hasSubtype(kellan, Subtype.FAERIE.value) shouldBe true
        projected.hasSubtype(kellan, Subtype.DETECTIVE.value) shouldBe false
        projected.hasKeyword(kellan, Keyword.DOUBLE_STRIKE) shouldBe true
    }

    test("neither ability has a duration — the upgrades survive the turn ending") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kellan = driver.putCreatureOnBattlefield(you, "Kellan, Planar Trailblazer")
        becomeDetective(driver, you, kellan)
        driver.giveMana(you, Color.RED, 3)
        driver.submitSuccess(
            ActivateAbility(playerId = you, sourceId = kellan, abilityId = becomeRogueId)
        )
        driver.bothPass()

        // Roll into the opponent's turn and back — Permanent duration must not be cleaned up.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val projected = driver.state.projectedState
        projected.getPower(kellan) shouldBe 3
        projected.getToughness(kellan) shouldBe 2
        projected.hasSubtype(kellan, Subtype.ROGUE.value) shouldBe true
        projected.hasKeyword(kellan, Keyword.DOUBLE_STRIKE) shouldBe true
    }
})
