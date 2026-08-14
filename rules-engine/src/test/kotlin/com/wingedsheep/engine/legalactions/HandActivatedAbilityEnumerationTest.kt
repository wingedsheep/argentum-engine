package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.EnumerationTestDriver
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.legalactions.support.shouldContainActivatedAbilityOn
import com.wingedsheep.engine.legalactions.support.shouldNotContainActivatedAbilityOn
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.ZoneActivatedAbilityEnumerator] over the HAND zone — the
 * "{cost}, Discard this card: ..." style abilities that function from hand
 * (`activateFromZone == Zone.HAND`), e.g. Steel Wrecking Ball, Stegron the Dinosaur
 * Man, and Urban Retreat's return-a-creature put-onto-battlefield ability.
 *
 * Regression: before the HAND zone was wired into the enumerator, these abilities were
 * never surfaced as legal actions, so a player holding the card could only *cast* it —
 * the from-hand ability was unreachable in a real game even though
 * [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler] accepted it.
 */
class HandActivatedAbilityEnumerationTest : FunSpec({

    /** Hand entity id for the P1 card matching [name]. */
    fun entityInHand(driver: EnumerationTestDriver, name: String): EntityId {
        val state = driver.game.state
        return state.getZone(driver.player1, Zone.HAND).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    /** Battlefield entity id for the P1 card matching [name]. */
    fun entityOnBattlefield(driver: EnumerationTestDriver, name: String): EntityId {
        val state = driver.game.state
        return state.getZone(driver.player1, Zone.BATTLEFIELD).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    // -------------------------------------------------------------------------
    context("Steel Wrecking Ball ({1}{R}, Discard this card: Destroy target artifact)") {

        test("in hand with mana and a target artifact — from-hand ability is enumerated") {
            val driver = setupP1(
                hand = listOf("Steel Wrecking Ball"),
                // Two Mountains for {1}{R}; a second Steel Wrecking Ball as the target artifact.
                battlefield = listOf("Mountain", "Mountain", "Steel Wrecking Ball"),
                atStep = Step.PRECOMBAT_MAIN
            )
            val handId = entityInHand(driver, "Steel Wrecking Ball")
            val targetArtifact = entityOnBattlefield(driver, "Steel Wrecking Ball")

            val view = driver.enumerateFor(driver.player1)
            view shouldContainActivatedAbilityOn handId

            val ability = view.activatedAbilityActionsFor(handId).single()
            ability.manaCostString shouldBe "{1}{R}"
            ability.requiresTargets shouldBe true
            ability.validTargets.shouldNotBeNull() shouldContain targetArtifact
        }

        test("in hand but only one red source — NOT enumerated ({1}{R} unpayable)") {
            val driver = setupP1(
                hand = listOf("Steel Wrecking Ball"),
                battlefield = listOf("Mountain", "Steel Wrecking Ball"),
                atStep = Step.PRECOMBAT_MAIN
            )
            val handId = entityInHand(driver, "Steel Wrecking Ball")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn handId
        }
    }

    // -------------------------------------------------------------------------
    context("Stegron the Dinosaur Man ({1}{R}, Discard this card: pump target creature you control)") {

        test("in hand with mana and a creature you control — from-hand ability is enumerated") {
            val driver = setupP1(
                hand = listOf("Stegron the Dinosaur Man"),
                battlefield = listOf("Mountain", "Mountain", "Vulture, Scheming Scavenger"),
                atStep = Step.PRECOMBAT_MAIN
            )
            val handId = entityInHand(driver, "Stegron the Dinosaur Man")
            val ownCreature = entityOnBattlefield(driver, "Vulture, Scheming Scavenger")

            val view = driver.enumerateFor(driver.player1)
            view shouldContainActivatedAbilityOn handId

            val ability = view.activatedAbilityActionsFor(handId).single()
            ability.manaCostString shouldBe "{1}{R}"
            ability.requiresTargets shouldBe true
            ability.validTargets.shouldNotBeNull() shouldContain ownCreature
        }
    }

    // -------------------------------------------------------------------------
    context("Urban Retreat ({2}, Return a tapped creature you control: put this onto the battlefield)") {

        test("at sorcery speed with mana and a tapped creature — enumerated with a bounce cost") {
            val driver = setupP1(
                hand = listOf("Urban Retreat"),
                battlefield = listOf("Mountain", "Mountain", "Vulture, Scheming Scavenger"),
                atStep = Step.PRECOMBAT_MAIN
            )
            val handId = entityInHand(driver, "Urban Retreat")
            val creature = entityOnBattlefield(driver, "Vulture, Scheming Scavenger")
            driver.game.tapPermanent(creature) // the bounce cost needs a *tapped* creature

            val view = driver.enumerateFor(driver.player1)
            view shouldContainActivatedAbilityOn handId

            val ability = view.activatedAbilityActionsFor(handId).single()
            ability.manaCostString shouldBe "{2}"
            val costInfo = ability.additionalCostInfo.shouldNotBeNull()
            costInfo.costType shouldBe "BouncePermanent"
            costInfo.validBounceTargets shouldContain creature
        }

        test("no tapped creature to return — NOT enumerated (bounce cost unpayable)") {
            val driver = setupP1(
                hand = listOf("Urban Retreat"),
                // Untapped creature can't pay the "return a tapped creature" cost.
                battlefield = listOf("Mountain", "Mountain", "Vulture, Scheming Scavenger"),
                atStep = Step.PRECOMBAT_MAIN
            )
            val handId = entityInHand(driver, "Urban Retreat")

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn handId
        }

        test("at instant speed (upkeep) — NOT enumerated (activate only as a sorcery)") {
            val driver = setupP1(
                hand = listOf("Urban Retreat"),
                battlefield = listOf("Mountain", "Mountain", "Vulture, Scheming Scavenger"),
                atStep = Step.UPKEEP
            )
            val handId = entityInHand(driver, "Urban Retreat")
            driver.game.tapPermanent(entityOnBattlefield(driver, "Vulture, Scheming Scavenger"))

            driver.enumerateFor(driver.player1) shouldNotContainActivatedAbilityOn handId
        }
    }
})
