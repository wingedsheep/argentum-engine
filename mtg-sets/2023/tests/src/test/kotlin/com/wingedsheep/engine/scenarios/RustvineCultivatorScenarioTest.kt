package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.RustvineCultivator
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rustvine Cultivator (ONE #181) — {G} 1/2 Creature — Phyrexian Elf Druid.
 *
 * "{T}: Put an oil counter on this creature.
 *  {T}, Remove an oil counter from this creature: Untap target land."
 */
class RustvineCultivatorScenarioTest : FunSpec({

    val oilAbility = RustvineCultivator.activatedAbilities[0].id
    val untapAbility = RustvineCultivator.activatedAbilities[1].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RustvineCultivator))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun tapped(driver: GameTestDriver, id: EntityId) = driver.state.getEntity(id)?.get<TappedComponent>() != null

    test("tapping puts an oil counter on it") {
        val driver = newDriver()
        val cultivator = driver.putCreatureOnBattlefield(driver.player1, "Rustvine Cultivator")
        driver.removeSummoningSickness(cultivator)

        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = cultivator, abilityId = oilAbility))
        tapped(driver, cultivator) shouldBe true
        driver.bothPass()
        oil(driver, cultivator) shouldBe 1
    }

    test("tap and remove an oil counter to untap target land") {
        val driver = newDriver()
        val cultivator = driver.putCreatureOnBattlefield(driver.player1, "Rustvine Cultivator")
        driver.removeSummoningSickness(cultivator)
        // Build up an oil counter with the first ability, then ready the creature again.
        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = cultivator, abilityId = oilAbility))
        driver.bothPass()
        oil(driver, cultivator) shouldBe 1
        driver.untapPermanent(cultivator)
        val forest = driver.putLandOnBattlefield(driver.player1, "Forest")
        driver.tapPermanent(forest)
        tapped(driver, forest) shouldBe true

        driver.submitSuccess(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = cultivator,
                abilityId = untapAbility,
                targets = listOf(ChosenTarget.Permanent(forest)),
            )
        )
        oil(driver, cultivator) shouldBe 0
        tapped(driver, cultivator) shouldBe true
        driver.bothPass()
        tapped(driver, forest) shouldBe false
    }

    test("untap ability can't be activated without an oil counter") {
        val driver = newDriver()
        val cultivator = driver.putCreatureOnBattlefield(driver.player1, "Rustvine Cultivator")
        driver.removeSummoningSickness(cultivator)
        val forest = driver.putLandOnBattlefield(driver.player1, "Forest")
        driver.tapPermanent(forest)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = driver.player1,
                sourceId = cultivator,
                abilityId = untapAbility,
                targets = listOf(ChosenTarget.Permanent(forest)),
            )
        )
        tapped(driver, cultivator) shouldBe false
        tapped(driver, forest) shouldBe true
    }
})
