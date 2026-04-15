package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainActivatedAbilityOn
import com.wingedsheep.engine.legalactions.support.shouldNotContainActivatedAbilityOn
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.GraveyardAbilityEnumerator].
 *
 * The enumerator handles activated abilities whose `activateFromZone == GRAVEYARD`
 * (e.g. Undead Gladiator's self-return, Eternal Dragon's self-return). Unlike the
 * battlefield `ActivatedAbilityEnumerator`, this one DROPS unaffordable abilities
 * entirely via `continue` (no "greyed-out" entries).
 *
 * Paths covered:
 * - Simple `AbilityCost.Mana` (Eternal Dragon)
 * - Composite Mana + Discard cost with discard cost info (Undead Gladiator)
 * - Activation restrictions (`OnlyDuringYourTurn` + `DuringStep(UPKEEP)`) —
 *   off-step and opponent's-turn cases
 * - Opponent's graveyard does not surface my graveyard's abilities
 * - Empty graveyard produces nothing
 *
 * Deferred to a follow-up: ReturnToHand / ExileSelf composite sub-costs,
 * target requirements (auto-select player vs ordinary target), X-cost paths.
 * No currently-registered test card exercises these graveyard-ability sub-paths.
 */
class GraveyardAbilityEnumeratorTest : FunSpec({

    /** Graveyard entity id for the P1 card matching [name]. */
    fun entityInGraveyard(driver: com.wingedsheep.engine.legalactions.support.EnumerationTestDriver, name: String): EntityId {
        val state = driver.game.state
        return state.getGraveyard(driver.player1).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    // -------------------------------------------------------------------------
    context("Simple mana cost (Eternal Dragon: {3}{W}{W} at upkeep)") {

        test("in graveyard at upkeep with enough mana — ability is enumerated") {
            val driver = setupP1(
                battlefield = listOf("Plains", "Plains", "Plains", "Plains", "Plains"),
                graveyard = listOf("Eternal Dragon"),
                atStep = Step.UPKEEP
            )
            val dragonId = entityInGraveyard(driver, "Eternal Dragon")

            val view = driver.enumerateFor(driver.player1)

            view shouldContainActivatedAbilityOn dragonId
            val abilities = view.activatedAbilityActionsFor(dragonId)
            abilities shouldHaveSize 1
            abilities.single().manaCostString shouldBe "{3}{W}{W}"
        }

        test("at upkeep but insufficient mana — NOT enumerated (enumerator `continue`s past unpayable cost)") {
            val driver = setupP1(
                battlefield = listOf("Plains"),  // only 1 white source — need 5
                graveyard = listOf("Eternal Dragon"),
                atStep = Step.UPKEEP
            )
            val dragonId = entityInGraveyard(driver, "Eternal Dragon")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn dragonId
        }

        test("at precombat main phase — restriction `DuringStep(UPKEEP)` not met, NOT enumerated") {
            val driver = setupP1(
                battlefield = listOf("Plains", "Plains", "Plains", "Plains", "Plains"),
                graveyard = listOf("Eternal Dragon"),
                atStep = Step.PRECOMBAT_MAIN
            )
            val dragonId = entityInGraveyard(driver, "Eternal Dragon")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn dragonId
        }

        test("card in P1's graveyard is NOT visible to P2 (graveyard zone is per-player)") {
            val driver = setupP1(
                battlefield = listOf("Plains", "Plains", "Plains", "Plains", "Plains"),
                graveyard = listOf("Eternal Dragon"),
                atStep = Step.UPKEEP
            )
            val dragonId = entityInGraveyard(driver, "Eternal Dragon")

            driver.enumerateFor(driver.player2) shouldNotContainActivatedAbilityOn dragonId
        }

        test("empty graveyard — no graveyard-activated abilities surface at all") {
            val driver = setupP1(
                battlefield = listOf("Plains", "Plains", "Plains", "Plains", "Plains"),
                atStep = Step.UPKEEP
            )

            // No ActivateAbility action can be sourced from a graveyard entity
            // because there are no graveyard entities.
            val view = driver.enumerateFor(driver.player1)
            val graveyardIds = driver.game.state.getGraveyard(driver.player1).toSet()
            view.activatedAbilityActions()
                .filter { (it.action as ActivateAbility).sourceId in graveyardIds }
                .shouldBeEmpty()
        }
    }

    // -------------------------------------------------------------------------
    context("Composite mana + discard cost (Undead Gladiator: {1}{B}, Discard a card at upkeep)") {

        test("with mana and a card in hand — ability enumerated with DiscardCard cost info") {
            val driver = setupP1(
                hand = listOf("Forest"),  // something discardable
                battlefield = listOf("Swamp", "Swamp"),
                graveyard = listOf("Undead Gladiator"),
                atStep = Step.UPKEEP
            )
            val gladiatorId = entityInGraveyard(driver, "Undead Gladiator")

            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(gladiatorId).single()

            ability.manaCostString shouldBe "{1}{B}"
            val costInfo = ability.additionalCostInfo.shouldNotBeNull()
            costInfo.costType shouldBe "DiscardCard"
            costInfo.discardCount shouldBe 1
            // Valid discard targets = whatever's in hand (just the Forest).
            costInfo.validDiscardTargets shouldHaveSize 1
        }

        test("with mana but empty hand — NOT enumerated (Discard sub-cost unpayable)") {
            val driver = setupP1(
                // No cards in hand
                battlefield = listOf("Swamp", "Swamp"),
                graveyard = listOf("Undead Gladiator"),
                atStep = Step.UPKEEP
            )
            val gladiatorId = entityInGraveyard(driver, "Undead Gladiator")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn gladiatorId
        }

        test("with a card in hand but no mana — NOT enumerated (Mana sub-cost unpayable)") {
            val driver = setupP1(
                hand = listOf("Forest"),
                // no lands on the battlefield
                graveyard = listOf("Undead Gladiator"),
                atStep = Step.UPKEEP
            )
            val gladiatorId = entityInGraveyard(driver, "Undead Gladiator")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn gladiatorId
        }

        test("at precombat main phase — restriction fails, NOT enumerated even with resources") {
            val driver = setupP1(
                hand = listOf("Forest"),
                battlefield = listOf("Swamp", "Swamp"),
                graveyard = listOf("Undead Gladiator"),
                atStep = Step.PRECOMBAT_MAIN
            )
            val gladiatorId = entityInGraveyard(driver, "Undead Gladiator")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn gladiatorId
        }
    }

    // -------------------------------------------------------------------------
    context("Action wiring") {

        test("emitted ActivateAbility carries the graveyard entity id and correct player") {
            val driver = setupP1(
                battlefield = listOf("Plains", "Plains", "Plains", "Plains", "Plains"),
                graveyard = listOf("Eternal Dragon"),
                atStep = Step.UPKEEP
            )
            val dragonId = entityInGraveyard(driver, "Eternal Dragon")

            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(dragonId).single()
            val action = ability.action as ActivateAbility

            action.sourceId shouldBe dragonId
            action.playerId shouldBe driver.player1
            // No targets are required by this ability; the emitted action carries none.
            action.targets.shouldBeEmpty()
        }

        test("fixed-cost ability reports hasXCost=false, maxAffordableX=null, requiresTargets=false") {
            val driver = setupP1(
                battlefield = listOf("Plains", "Plains", "Plains", "Plains", "Plains"),
                graveyard = listOf("Eternal Dragon"),
                atStep = Step.UPKEEP
            )
            val dragonId = entityInGraveyard(driver, "Eternal Dragon")
            val ability = driver.enumerateFor(driver.player1)
                .activatedAbilityActionsFor(dragonId).single()

            ability.hasXCost shouldBe false
            ability.maxAffordableX shouldBe null
            ability.requiresTargets shouldBe false
        }
    }
})
