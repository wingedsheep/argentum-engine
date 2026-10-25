package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.WarlockClass
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Warlock Class {B} — Enchantment — Class (AFR #125).
 *
 * Pins level 1's morbid intervening-if at your end step, level 2's becomes-level look-three-keep-one,
 * and level 3's per-opponent "loses life equal to the life they lost this turn" — the
 * `ForEachPlayer` loop must read each opponent's own life-lost tally, not the Class controller's.
 */
class WarlockClassScenarioTest : FunSpec({

    data class Board(val driver: GameTestDriver, val you: EntityId, val opponent: EntityId, val warlock: EntityId)

    fun setup(level: Int): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WarlockClass))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val warlock = driver.putPermanentOnBattlefield(you, "Warlock Class", classLevel = level)
        return Board(driver, you, driver.getOpponent(you), warlock)
    }

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, bolt, listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.passToEndStepAndResolve() {
        passPriorityUntil(Step.END)
        var guard = 0
        while (guard++ < 30 && (pendingDecision != null || state.stack.isNotEmpty())) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    test("level 1: each opponent loses 1 life at your end step if a creature died this turn") {
        val (driver, you, opponent) = setup(level = 1)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.bolt(you, bears)
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"

        driver.passToEndStepAndResolve()

        driver.getLifeTotal(opponent) shouldBe 19
        driver.getLifeTotal(you) shouldBe 20
    }

    test("level 1: no life loss when no creature died this turn") {
        val (driver, you, opponent) = setup(level = 1)

        driver.passToEndStepAndResolve()

        driver.getLifeTotal(opponent) shouldBe 20
        driver.getLifeTotal(you) shouldBe 20
    }

    test("level 2: becoming level 2 keeps one of the top three and mills the rest") {
        val (driver, you, _, warlock) = setup(level = 1)
        val forest = driver.putCardOnTopOfLibrary(you, "Forest")
        val island = driver.putCardOnTopOfLibrary(you, "Island")
        val bears = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")

        driver.giveMana(you, Color.BLACK, 2)
        driver.submit(
            ActivateAbility(you, warlock, AbilityId.classLevelUp(2), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the level-up
        driver.bothPass() // resolve the becomes-level-2 trigger

        (driver.pendingDecision is SelectCardsDecision) shouldBe true
        driver.submitCardSelection(you, listOf(bears))

        driver.getHand(you) shouldContain bears
        driver.getGraveyard(you) shouldContainAll listOf(forest, island)
        driver.getHand(you) shouldNotContain forest
    }

    test("level 3: each opponent loses life equal to the life they lost this turn") {
        val (driver, you, opponent) = setup(level = 3)
        driver.bolt(you, opponent)
        driver.getLifeTotal(opponent) shouldBe 17
        // Your own life loss must not feed the opponent's tally.
        driver.bolt(you, you)
        driver.getLifeTotal(you) shouldBe 17

        driver.passToEndStepAndResolve()

        driver.getLifeTotal(opponent) shouldBe 14
        driver.getLifeTotal(you) shouldBe 17
    }

    test("level 3: an opponent who lost no life this turn loses nothing") {
        val (driver, you, opponent) = setup(level = 3)

        driver.passToEndStepAndResolve()

        driver.getLifeTotal(opponent) shouldBe 20
        driver.getLifeTotal(you) shouldBe 20
    }
})
