package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.Banshee
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Banshee — "{X}, {T}: deals half X damage, rounded down, to any target, and
 * half X damage, rounded up, to you."
 *
 * The two halves round in opposite directions, so an *odd* X is the only value that can tell a
 * correct implementation from one that rounds both ways the same: X=5 must be 2 to them and 3 to
 * you, never 2/2 or 3/3. An even X is included as the control that both agree there.
 */
class BansheeScenarioTest : FunSpec({

    val abilityId = Banshee.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Banshee)
        return driver
    }

    fun fire(x: Int): Pair<Int, Int> {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val banshee = driver.putCreatureOnBattlefield(me, "Banshee")
        driver.removeSummoningSickness(banshee)
        driver.giveColorlessMana(me, x)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = banshee,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, opponent)),
                xValue = x,
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        return driver.getLifeTotal(opponent) to driver.getLifeTotal(me)
    }

    test("an odd X splits down for them and up for you") {
        val (them, me) = fire(5)
        withClue("half of 5 rounded down") { them shouldBe 18 }
        withClue("half of 5 rounded up — the Banshee's own controller pays more") { me shouldBe 17 }
    }

    test("an even X splits evenly") {
        val (them, me) = fire(4)
        them shouldBe 18
        me shouldBe 18
    }
})
