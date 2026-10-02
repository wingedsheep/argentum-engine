package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.SerumCoreChimera
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Serum-Core Chimera (ONE #215) — {2}{U}{R} 2/4 Creature — Phyrexian Chimera.
 *
 * "Flying
 *  Whenever you cast a noncreature spell, put an oil counter on this creature.
 *  Remove three oil counters from this creature: Draw a card. Then you may discard a nonland card.
 *  When you discard a card this way, this creature deals 3 damage to target creature or
 *  planeswalker. Activate only as a sorcery."
 */
class SerumCoreChimeraScenarioTest : FunSpec({

    val abilityId = SerumCoreChimera.activatedAbilities.single().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SerumCoreChimera))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun seedOil(driver: GameTestDriver, id: EntityId, amount: Int) {
        driver.replaceState(driver.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, amount))
        })
    }

    /** Resolve the ability, answering the optional discard and the reflexive target. Returns whether a target was asked for. */
    fun resolve(driver: GameTestDriver, discard: Boolean, discardName: String?, target: EntityId?): Boolean {
        val p1 = driver.player1
        var targeted = false
        var guard = 0
        while (guard++ < 40) {
            when (val dec = driver.pendingDecision) {
                is YesNoDecision -> driver.submitYesNo(p1, discard)
                is SelectCardsDecision -> {
                    val pick = dec.options.first { driver.getCardName(it) == discardName }
                    driver.submitCardSelection(p1, listOf(pick))
                }
                is ChooseTargetsDecision -> {
                    driver.submitTargetSelection(p1, listOf(target!!)); targeted = true
                }
                null -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else return targeted
                else -> error("Unexpected decision: $dec")
            }
        }
        return targeted
    }

    test("casting a noncreature spell adds an oil counter; a creature spell does not") {
        val driver = newDriver()
        val p1 = driver.player1
        val chimera = driver.putCreatureOnBattlefield(p1, "Serum-Core Chimera")
        val victim = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")

        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        oil(driver, chimera) shouldBe 1

        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, bears).outcome shouldBe Outcome.Done
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        oil(driver, chimera) shouldBe 1
    }

    test("draw, discard a nonland card, then the reflexive trigger deals 3 damage to a target creature") {
        val driver = newDriver()
        val p1 = driver.player1
        val chimera = driver.putCreatureOnBattlefield(p1, "Serum-Core Chimera")
        val courser = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")

        // Two counters can't pay the cost.
        seedOil(driver, chimera, 2)
        driver.submitExpectFailure(ActivateAbility(playerId = p1, sourceId = chimera, abilityId = abilityId))

        seedOil(driver, chimera, 1)
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        val handBefore = driver.getHandSize(p1)
        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = chimera, abilityId = abilityId))
        oil(driver, chimera) shouldBe 0

        resolve(driver, discard = true, discardName = "Grizzly Bears", target = courser) shouldBe true

        driver.getHandSize(p1) shouldBe handBefore
        driver.getGraveyardCardNames(p1) shouldBe listOf("Grizzly Bears")
        driver.assertInGraveyard(driver.player2, "Centaur Courser")
    }

    test("declining the discard deals no damage") {
        val driver = newDriver()
        val p1 = driver.player1
        val chimera = driver.putCreatureOnBattlefield(p1, "Serum-Core Chimera")
        val courser = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        seedOil(driver, chimera, 3)
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        val handBefore = driver.getHandSize(p1)

        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = chimera, abilityId = abilityId))
        resolve(driver, discard = false, discardName = null, target = courser) shouldBe false

        driver.getHandSize(p1) shouldBe handBefore + 1
        driver.getGraveyardCardNames(p1) shouldBe emptyList()
        driver.findPermanent(driver.player2, "Centaur Courser") shouldBe courser
    }

    test("with only lands in hand the discard is never offered and nothing is dealt damage") {
        val driver = newDriver()
        val p1 = driver.player1
        val chimera = driver.putCreatureOnBattlefield(p1, "Serum-Core Chimera")
        val courser = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        seedOil(driver, chimera, 3)
        driver.putCardOnTopOfLibrary(p1, "Island")

        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = chimera, abilityId = abilityId))
        var guard = 0
        while (guard++ < 20) {
            val dec = driver.pendingDecision
            if (dec != null) error("No decision expected when the hand holds only lands, got: $dec")
            if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
        }
        driver.getGraveyardCardNames(p1) shouldBe emptyList()
        driver.findPermanent(driver.player2, "Centaur Courser") shouldBe courser
    }

    test("activate only as a sorcery — not while a spell is on the stack") {
        val driver = newDriver()
        val p1 = driver.player1
        val chimera = driver.putCreatureOnBattlefield(p1, "Serum-Core Chimera")
        val courser = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        seedOil(driver, chimera, 3)

        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(courser))).error shouldBe null
        driver.state.stack.isNotEmpty() shouldBe true
        driver.submitExpectFailure(ActivateAbility(playerId = p1, sourceId = chimera, abilityId = abilityId))
    }
})
