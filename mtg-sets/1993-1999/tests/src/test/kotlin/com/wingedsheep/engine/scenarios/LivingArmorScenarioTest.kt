package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.LivingArmor
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Living Armor — "{T}, Sacrifice this artifact: Put X +0/+1 counters on target
 * creature, where X is that creature's mana value."
 *
 * X reads the *target's* mana value, not the Armor's own {4}. Two creatures of different cost prove
 * the amount tracks the target rather than being a constant or the source's cost — a wiring that
 * read `Source` instead of `Target(0)` would hand out four counters both times.
 */
class LivingArmorScenarioTest : FunSpec({

    val abilityId = LivingArmor.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LivingArmor)
        return driver
    }

    fun toughnessOf(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.projectedState.getToughness(id) ?: error("no projected toughness")

    test("counters equal the target's mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val armor = driver.putPermanentOnBattlefield(me, "Living Armor")
        // Centaur Courser is {2}{G} — mana value 3, printed 3/3.
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = armor,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, courser)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("three +0/+1 counters, from {2}{G}") {
            toughnessOf(driver, courser) shouldBe 6
        }
        withClue("power is untouched — these are +0/+1 counters") {
            driver.state.projectedState.getPower(courser) shouldBe 3
        }
        withClue("the Armor sacrificed itself to pay") {
            driver.findPermanent(me, "Living Armor") shouldBe null
        }
    }

    test("a costlier target gets more counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val armor = driver.putPermanentOnBattlefield(me, "Living Armor")
        // Force of Nature is {3}{G}{G} — mana value 5, printed 5/5.
        val force = driver.putCreatureOnBattlefield(me, "Force of Nature")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = armor,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, force)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("five counters, not the Armor's own four") {
            toughnessOf(driver, force) shouldBe 10
        }
    }
})
