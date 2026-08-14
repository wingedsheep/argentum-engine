package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m11.cards.CaptivatingVampire
import com.wingedsheep.mtg.sets.definitions.vow.cards.VoldarenEpicure
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Captivating Vampire (M11) — {1}{B}{B} Creature — Vampire 2/2
 *
 * "Other Vampire creatures you control get +1/+1.
 *  Tap five untapped Vampires you control: Gain control of target creature. It becomes a Vampire in
 *  addition to its other types."
 *
 * The interesting seam is that the two halves feed each other: the stolen creature *becomes* a
 * Vampire, so it immediately picks up the lord's +1/+1. Both effects are permanent per the M11
 * rulings, and the cost has no {T} symbol, so Captivating Vampire may be one of the five it taps.
 */
class CaptivatingVampireScenarioTest : FunSpec({

    val captivateAbilityId = CaptivatingVampire.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CaptivatingVampire)
        driver.registerCard(VoldarenEpicure)
        return driver
    }

    test("the lord pumps other Vampires you control but not itself") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val lord = driver.putCreatureOnBattlefield(me, "Captivating Vampire")
        val myVampire = driver.putCreatureOnBattlefield(me, "Voldaren Epicure")
        val theirVampire = driver.putCreatureOnBattlefield(opponent, "Voldaren Epicure")

        val projected = driver.state.projectedState
        projected.getPower(lord) shouldBe 2
        projected.getToughness(lord) shouldBe 2
        projected.getPower(myVampire) shouldBe 2
        projected.getToughness(myVampire) shouldBe 2
        // "you control" — the opponent's Vampire is untouched.
        projected.getPower(theirVampire) shouldBe 1
    }

    test("tapping five Vampires steals a creature, which becomes a Vampire and joins the lord bonus") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val lord = driver.putCreatureOnBattlefield(me, "Captivating Vampire")
        val fodder = (1..4).map { driver.putCreatureOnBattlefield(me, "Voldaren Epicure") }
        val prey = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.state.projectedState.getController(prey) shouldBe opponent

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = lord,
                abilityId = captivateAbilityId,
                targets = listOf(ChosenTarget.Permanent(prey)),
                // No {T} in the cost, so the lord itself is one of the five Vampires tapped.
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(lord) + fodder)
            )
        )
        driver.isTapped(lord) shouldBe true
        fodder.forEach { driver.isTapped(it) shouldBe true }

        driver.bothPass() // the ability resolves

        val projected = driver.state.projectedState
        projected.getController(prey) shouldBe me
        projected.hasSubtype(prey, "Vampire") shouldBe true
        // Still a Centaur Warrior too — "in addition to its other types".
        projected.hasSubtype(prey, "Centaur") shouldBe true
        // 3/3 base, now a Vampire I control, so the lord's +1/+1 applies.
        projected.getPower(prey) shouldBe 4
        projected.getToughness(prey) shouldBe 4
    }
})
