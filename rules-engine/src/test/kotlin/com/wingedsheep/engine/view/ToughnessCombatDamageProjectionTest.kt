package com.wingedsheep.engine.view

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.CombatDamageUtils
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tapestry Warden's "each creature you control with toughness greater than its power assigns combat
 * damage equal to its toughness rather than its power" changes no characteristic, so before
 * `ClientCard.toughnessCombatDamage` the board showed a 1/5 that would hit for 5 as a plain 1/5.
 * The projection reads the same rule combat applies, so the readout names the real damage and the
 * permanent responsible — and stays absent wherever the substitution doesn't apply.
 */
class ToughnessCombatDamageProjectionTest : FunSpec({

    val warden = card("Test Tapestry Warden") {
        manaCost = "{3}{G}"
        typeLine = "Artifact Creature — Robot Soldier"
        power = 3
        toughness = 4
        staticAbility {
            ability = AssignDamageEqualToToughness(
                filter = GroupFilter.AllCreaturesYouControl,
                onlyWhenToughnessGreaterThanPower = true,
            )
        }
    }
    val wall = card("Test Bulwark") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Wall"
        power = 1
        toughness = 5
    }
    val square = card("Test Square Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
    }

    // Streetwise Negotiator's shape: an unconditional, self-scoped substitution.
    val negotiator = card("Test Negotiator") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Human Citizen"
        power = 1
        toughness = 3
        staticAbility {
            ability = AssignDamageEqualToToughness(
                filter = GroupFilter.source(),
                onlyWhenToughnessGreaterThanPower = false,
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + warden + wall + square + negotiator)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cardView(viewer: EntityId, id: EntityId) =
        ClientStateTransformer(cardRegistry = cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(state, viewingPlayerId = viewer)
            .cards.getValue(id)

    test("a toughness-greater-than-power creature shows the damage it assigns and who grants it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Test Tapestry Warden")
        val myWall = driver.putCreatureOnBattlefield(me, "Test Bulwark")

        val expected = ClientToughnessCombatDamage(amount = 5, sourceName = "Test Tapestry Warden")
        driver.cardView(me, myWall).toughnessCombatDamage shouldBe expected
        // Board-visible information: the opponent sees the same readout.
        driver.cardView(opponent, myWall).toughnessCombatDamage shouldBe expected
        // Power is untouched — only combat damage assignment changes.
        driver.cardView(me, myWall).power shouldBe 1
    }

    test("the Warden covers itself (4 toughness over 3 power)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val wardenId = driver.putCreatureOnBattlefield(me, "Test Tapestry Warden")

        driver.cardView(me, wardenId).toughnessCombatDamage shouldBe
            ClientToughnessCombatDamage(amount = 4, sourceName = "Test Tapestry Warden")
    }

    test("absent when toughness doesn't exceed power, for the opponent's creatures, and without a Warden") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val lonelyWall = driver.putCreatureOnBattlefield(me, "Test Bulwark")
        driver.cardView(me, lonelyWall).toughnessCombatDamage shouldBe null

        driver.putCreatureOnBattlefield(me, "Test Tapestry Warden")
        val bear = driver.putCreatureOnBattlefield(me, "Test Square Bear")
        val theirWall = driver.putCreatureOnBattlefield(opponent, "Test Bulwark")
        driver.cardView(me, bear).toughnessCombatDamage shouldBe null
        driver.cardView(me, theirWall).toughnessCombatDamage shouldBe null
    }

    test("a face-down creature doesn't read its hidden card's own substitution (CR 708.2)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val negotiatorId = driver.putCreatureOnBattlefield(me, "Test Negotiator")
        fun source() = CombatDamageUtils.toughnessDamageSource(
            driver.state, driver.state.projectedState, negotiatorId, driver.cardRegistry,
            PredicateEvaluator(cardRegistry = null),
        )

        source() shouldBe CombatDamageUtils.ToughnessDamageSource(negotiatorId)
        driver.cardView(me, negotiatorId).toughnessCombatDamage shouldBe
            ClientToughnessCombatDamage(amount = 3, sourceName = "Test Negotiator")

        driver.replaceState(driver.state.updateEntity(negotiatorId) { it.with(FaceDownComponent) })
        source() shouldBe null
    }
})
