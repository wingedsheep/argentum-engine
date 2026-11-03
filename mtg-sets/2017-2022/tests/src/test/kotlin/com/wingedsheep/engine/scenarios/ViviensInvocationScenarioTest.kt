package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m19.cards.ViviensInvocation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vivien's Invocation (M19 #209).
 *
 * The spell takes no targets; only a creature actually put onto the battlefield fires the
 * reflexive "it deals damage equal to its power to target creature an opponent controls"
 * (CR 603.12), whose target is chosen as it goes on the stack. The power is read when the
 * reflexive ability resolves — live while the creature is still there, and by last-known
 * information once it has left (2018-07-13 ruling).
 */
class ViviensInvocationScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ViviensInvocation))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.damageOn(entity: EntityId): Int =
        state.getEntity(entity)?.get<DamageComponent>()?.amount ?: 0

    /** Casts the Invocation, resolves it, and puts [pick] (or nothing) onto the battlefield. */
    fun GameTestDriver.castAndPick(me: EntityId, pick: EntityId?) {
        val invocation = putCardInHand(me, "Vivien's Invocation")
        giveMana(me, Color.GREEN, 7)
        val cast = castSpell(me, invocation)
        withClue("cast error: ${cast.error}") { cast.error shouldBe null }
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        if (pick != null) decision.options shouldContain pick
        submitCardSelection(me, listOfNotNull(pick))
    }

    test("the creature put onto the battlefield deals damage equal to its power to an opponent's creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val myElves = driver.putCreatureOnBattlefield(me, "Llanowar Elves")
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val courser = driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        driver.castAndPick(me, courser)
        driver.getPermanents(me) shouldContain courser

        val targeting = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targeting.legalTargets.getValue(0) shouldContain force
        targeting.legalTargets.getValue(0) shouldNotContain myElves
        targeting.legalTargets.getValue(0) shouldNotContain courser
        driver.submitTargetSelection(me, listOf(force)).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.damageOn(force) shouldBe 3
    }

    test("declining to put a creature onto the battlefield triggers nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val courser = driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        driver.castAndPick(me, null)
        while (driver.stackSize > 0) driver.bothPass()

        (driver.pendingDecision is ChooseTargetsDecision) shouldBe false
        driver.getPermanents(me) shouldNotContain courser
        driver.damageOn(force) shouldBe 0
    }

    test("power is read on resolution: a pump in response increases the damage") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val courser = driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        driver.castAndPick(me, courser)
        driver.submitTargetSelection(me, listOf(force)).error shouldBe null

        val growth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, growth, listOf(courser)).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        // Centaur Courser is 3/3; +3/+3 makes it deal 6, killing the 5/5.
        driver.getPermanents(opponent) shouldNotContain force
    }

    test("if the creature leaves before the damage, its last-known power is used") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val courser = driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        driver.castAndPick(me, courser)
        driver.submitTargetSelection(me, listOf(force)).error shouldBe null

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(courser)).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.getPermanents(me) shouldNotContain courser
        driver.damageOn(force) shouldBe 3
    }
})
