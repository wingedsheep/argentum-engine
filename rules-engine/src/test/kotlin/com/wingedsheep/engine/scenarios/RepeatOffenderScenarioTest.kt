package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RepeatOffender
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Repeat Offender (MKM) — {1}{B} 2/1 Creature — Human Assassin.
 *
 * "{2}{B}: If this creature is suspected, put a +1/+1 counter on it. Otherwise, suspect it."
 *
 * The branch is a resolution-time state test, so these cover both arms and the hinge between them:
 * the first activation suspects (designation + menace + can't block, no counter), and every
 * activation afterwards takes the other arm and grows it instead of re-suspecting a no-op.
 */
class RepeatOffenderScenarioTest : FunSpec({

    val abilityId = RepeatOffender.script.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RepeatOffender)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun activate(driver: GameTestDriver, player: EntityId, offender: EntityId) {
        driver.giveMana(player, Color.BLACK, 3)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = offender, abilityId = abilityId)
        ).isSuccess shouldBe true
        driver.bothPass()
    }

    test("the first activation suspects it — designation, menace and can't block, no counter") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val offender = driver.putCreatureOnBattlefield(player, "Repeat Offender")

        withClue("a freshly-played Repeat Offender is not suspected") {
            StateProjector().project(driver.state).isSuspected(offender) shouldBe false
        }

        activate(driver, player, offender)

        val projected = StateProjector().project(driver.state)
        withClue("the else arm applies the suspected designation (CR 701.60a)") {
            projected.isSuspected(offender) shouldBe true
        }
        withClue("suspected grants menace (CR 701.60c)") {
            projected.hasKeyword(offender, Keyword.MENACE) shouldBe true
        }
        withClue("suspected can't block (CR 701.60c)") {
            projected.cantBlock(offender) shouldBe true
        }
        withClue("the counter arm must not have run — it wasn't suspected yet") {
            plusOneCounters(driver, offender) shouldBe 0
            projected.getPower(offender) shouldBe 2
            projected.getToughness(offender) shouldBe 1
        }
    }

    test("once suspected, further activations add +1/+1 counters instead") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val offender = driver.putCreatureOnBattlefield(player, "Repeat Offender")

        activate(driver, player, offender)
        activate(driver, player, offender)

        withClue("the second activation takes the counter arm") {
            plusOneCounters(driver, offender) shouldBe 1
            val projected = StateProjector().project(driver.state)
            projected.getPower(offender) shouldBe 3
            projected.getToughness(offender) shouldBe 2
            projected.isSuspected(offender) shouldBe true
        }

        activate(driver, player, offender)

        withClue("and keeps taking it — the counters accumulate") {
            plusOneCounters(driver, offender) shouldBe 2
            val projected = StateProjector().project(driver.state)
            projected.getPower(offender) shouldBe 4
            projected.getToughness(offender) shouldBe 3
        }
    }

    test("suspecting stays permanent — the designation survives into a later turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val offender = driver.putCreatureOnBattlefield(player, "Repeat Offender")

        activate(driver, player, offender)
        StateProjector().project(driver.state).isSuspected(offender) shouldBe true

        // Round the table back to the same player.
        do {
            driver.passPriorityUntil(Step.END)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        } while (driver.activePlayer != player)

        withClue("suspected is not an until-end-of-turn effect (CR 701.60a)") {
            StateProjector().project(driver.state).isSuspected(offender) shouldBe true
        }

        activate(driver, player, offender)

        withClue("so a later-turn activation takes the counter arm") {
            plusOneCounters(driver, offender) shouldBe 1
        }
    }
})
