package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.ThranduilsCompany
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thranduil's Company — {2}{G}{U} 3/4 Elf Soldier (HOB #168).
 *
 * "As long as you control another Elf, you may play an additional land on each of your turns.
 *  Landfall — Whenever a land you control enters, put two +1/+1 counters on target creature you
 *  control. It gains vigilance until end of turn."
 *
 * The land-drop half is gated on *another* Elf, so the card must never satisfy its own condition.
 */
class ThranduilsCompanyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThranduilsCompany))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Play a land, then settle the landfall trigger onto [preferredTarget] if it asks. */
    fun GameTestDriver.playLandAndSettle(playerId: EntityId, landId: EntityId, preferredTarget: EntityId) {
        playLand(playerId, landId)
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 40) {
            val pending = state.pendingDecision
            when {
                pending is ChooseTargetsDecision -> submitTargetSelection(pending.playerId, listOf(preferredTarget))
                pending != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("landfall puts two +1/+1 counters on the target and grants it vigilance") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val company = driver.putCreatureOnBattlefield(me, "Thranduil's Company")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val forest = driver.putCardInHand(me, "Forest")
        driver.playLandAndSettle(me, forest, bears)

        withClue("two counters, not one") {
            driver.plusOneCounters(bears) shouldBe 2
        }
        withClue("and vigilance until end of turn") {
            driver.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        }
        withClue("the untargeted Company got nothing") {
            driver.plusOneCounters(company) shouldBe 0
        }
    }

    test("no other Elf: the card does not satisfy its own 'another Elf' condition") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val company = driver.putCreatureOnBattlefield(me, "Thranduil's Company")

        val forest1 = driver.putCardInHand(me, "Forest")
        driver.playLandAndSettle(me, forest1, company)

        val forest2 = driver.putCardInHand(me, "Forest")
        withClue("only the base land drop — the Company is the only Elf") {
            driver.submitExpectFailure(PlayLand(me, forest2)).isSuccess shouldBe false
        }
    }

    test("with another Elf out, a second land drop is legal") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val company = driver.putCreatureOnBattlefield(me, "Thranduil's Company")
        driver.putCreatureOnBattlefield(me, "Llanowar Elves")

        val forest1 = driver.putCardInHand(me, "Forest")
        driver.playLandAndSettle(me, forest1, company)

        val forest2 = driver.putCardInHand(me, "Forest")
        driver.playLandAndSettle(me, forest2, company)

        withClue("both lands are on the battlefield") {
            driver.getLands(me).size shouldBe 2
        }
        withClue("two landfall triggers resolved, two counters each") {
            driver.plusOneCounters(company) shouldBe 4
        }

        val forest3 = driver.putCardInHand(me, "Forest")
        withClue("but only one extra drop — a third land is still illegal") {
            driver.submitExpectFailure(PlayLand(me, forest3)).isSuccess shouldBe false
        }
    }
})
