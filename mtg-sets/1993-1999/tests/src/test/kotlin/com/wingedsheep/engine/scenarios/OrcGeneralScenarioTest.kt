package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.OrcGeneral
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Orc General — "{T}, Sacrifice another Orc or Goblin: Other Orc creatures get
 * +1/+1 until end of turn."
 *
 * Both "other"s point at the General himself, and both are easy to lose. He must not pump himself,
 * and — since the pump names no controller — an opponent's Orc must get the buff too. A Goblin is
 * on the board as fodder *and* as a non-Orc that must not be pumped, which catches a filter that
 * had drifted to "Orc or Goblin" on the payoff side.
 */
class OrcGeneralScenarioTest : FunSpec({

    val abilityId = OrcGeneral.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(OrcGeneral)
        return driver
    }

    fun powerOf(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.projectedState.getPower(id) ?: error("no projected power")

    test("other Orcs are pumped; the General and non-Orcs are not") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val general = driver.putCreatureOnBattlefield(me, "Orc General")
        driver.removeSummoningSickness(general)
        // Orcish Mechanics is a plain Orc — the "other" the General is supposed to pump.
        val otherOrc = driver.putCreatureOnBattlefield(me, "Orcish Mechanics")
        // And one under the opponent's control: the pump names no controller, so it counts too.
        val opposingOrc = driver.putCreatureOnBattlefield(opponent, "Orcish Mechanics")
        // Fodder, and a non-Orc that must stay unpumped.
        val goblin = driver.putCreatureOnBattlefield(me, "Goblin Balloon Brigade")

        val generalBefore = powerOf(driver, general)
        val otherOrcBefore = powerOf(driver, otherOrc)
        val opposingOrcBefore = powerOf(driver, opposingOrc)

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = general,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(goblin)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("the General does not pump himself") {
            powerOf(driver, general) shouldBe generalBefore
        }
        withClue("another Orc I control is pumped") {
            powerOf(driver, otherOrc) shouldBe otherOrcBefore + 1
        }
        withClue("so is the opponent's Orc — the card names no controller") {
            powerOf(driver, opposingOrc) shouldBe opposingOrcBefore + 1
        }
        withClue("the Goblin paid the cost and is gone") {
            driver.findPermanent(me, "Goblin Balloon Brigade") shouldBe null
        }
    }

    test("the General can't eat himself to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val general = driver.putCreatureOnBattlefield(me, "Orc General")
        driver.removeSummoningSickness(general)

        withClue("'another' excludes the source, so with no other Orc or Goblin the cost is unpayable") {
            driver.submit(
                ActivateAbility(
                    playerId = me,
                    sourceId = general,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(general)),
                )
            ).isSuccess shouldBe false
        }
    }
})
