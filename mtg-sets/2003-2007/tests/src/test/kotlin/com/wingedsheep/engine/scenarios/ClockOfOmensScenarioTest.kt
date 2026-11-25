package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.ClockOfOmens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Clock of Omens (5DN #110) — {4} Artifact.
 *   Tap two untapped artifacts you control: Untap target artifact.
 *
 * The cost doesn't say "other", so the Clock may tap itself as one of the two.
 */
class ClockOfOmensScenarioTest : FunSpec({

    val abilityId = ClockOfOmens.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ClockOfOmens)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("tapping itself and another artifact untaps target artifact") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val clock = driver.putPermanentOnBattlefield(p1, "Clock of Omens")
        val other = driver.putPermanentOnBattlefield(p1, "Clock of Omens")
        val tappedOne = driver.putPermanentOnBattlefield(p1, "Clock of Omens")
        driver.tapPermanent(tappedOne)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = clock,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(tappedOne)),
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(clock, other))
            )
        )
        result.error shouldBe null
        driver.isTapped(clock) shouldBe true
        driver.isTapped(other) shouldBe true

        driver.bothPass()
        driver.isTapped(tappedOne) shouldBe false
    }

    test("cannot activate with only one untapped artifact") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val clock = driver.putPermanentOnBattlefield(p1, "Clock of Omens")
        val tappedOne = driver.putPermanentOnBattlefield(p1, "Clock of Omens")
        driver.tapPermanent(tappedOne)

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = clock,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(tappedOne)),
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(clock, tappedOne))
            )
        )
        result.error shouldNotBe null
        driver.isTapped(tappedOne) shouldBe true
    }
})
