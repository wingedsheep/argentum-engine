package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BloodHustler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Blood Hustler — {1}{B} 1/1 Creature — Vampire Rogue
 *
 * "Whenever you commit a crime, put a +1/+1 counter on this creature. This ability triggers only
 * once each turn."
 * "{3}{B}: Target opponent loses 1 life and you gain 1 life."
 *
 * Verifies: a crime adds a +1/+1 counter (1/1 -> 2/2); a second crime in the same turn does NOT add
 * a second counter (once-per-turn gate); and the drain ability swings life by 1 each way.
 */
class BloodHustlerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BloodHustler)
        return driver
    }

    test("committing a crime puts a +1/+1 counter, but only once per turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val hustler = driver.putCreatureOnBattlefield(me, "Blood Hustler")

        driver.state.projectedState.getPower(hustler) shouldBe 1
        driver.state.projectedState.getToughness(hustler) shouldBe 1

        // Commit a crime: cast Lightning Bolt targeting the opponent.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> commit-crime -> counter trigger on stack
        driver.bothPass() // resolve counter trigger

        driver.state.projectedState.getPower(hustler) shouldBe 2
        driver.state.projectedState.getToughness(hustler) shouldBe 2

        // Commit a second crime this turn: it must NOT add another counter (once per turn).
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        driver.state.projectedState.getPower(hustler) shouldBe 2
        driver.state.projectedState.getToughness(hustler) shouldBe 2
    }

    test("activated ability drains 1 life from target opponent and gains 1") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val hustler = driver.putCreatureOnBattlefield(me, "Blood Hustler")
        val abilityId = BloodHustler.activatedAbilities.first().id

        driver.giveMana(me, Color.BLACK, 4)
        val result = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = hustler,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opp))
            )
        )
        result.isSuccess shouldBe true
        // Activating an ability that targets an opponent is itself a crime, so Blood Hustler's
        // own counter trigger also goes on the stack above the drain ability. Resolve both.
        driver.bothPass() // resolve the once-per-turn counter trigger
        driver.bothPass() // resolve the drain ability

        driver.getLifeTotal(opp) shouldBe 19
        driver.getLifeTotal(me) shouldBe 21
    }
})
