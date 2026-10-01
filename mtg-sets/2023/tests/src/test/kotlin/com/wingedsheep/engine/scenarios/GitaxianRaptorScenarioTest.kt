package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.GitaxianRaptor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gitaxian Raptor (ONE #53) — {2}{U} 1/4 Creature — Phyrexian Bird.
 *
 * Flying; enters with three oil counters; remove an oil counter: +1/-1 until end of turn.
 */
class GitaxianRaptorScenarioTest : FunSpec({

    val pumpId = GitaxianRaptor.activatedAbilities[0].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GitaxianRaptor))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castRaptor(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val raptor = driver.putCardInHand(player, "Gitaxian Raptor")
        driver.giveMana(player, Color.BLUE, 3)
        driver.castSpell(player, raptor).error shouldBe null
        driver.bothPass()
        return raptor
    }

    test("enters with three oil counters and flying") {
        val driver = newDriver()
        val raptor = castRaptor(driver)
        oil(driver, raptor) shouldBe 3
        driver.state.projectedState.hasKeyword(raptor, Keyword.FLYING) shouldBe true
    }

    test("removing oil counters gives +1/-1 each, until end of turn") {
        val driver = newDriver()
        val raptor = castRaptor(driver)
        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = raptor, abilityId = pumpId))
        oil(driver, raptor) shouldBe 2
        driver.bothPass()
        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = raptor, abilityId = pumpId))
        oil(driver, raptor) shouldBe 1
        driver.bothPass()
        driver.state.projectedState.getPower(raptor) shouldBe 3
        driver.state.projectedState.getToughness(raptor) shouldBe 2

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getPower(raptor) shouldBe 1
        driver.state.projectedState.getToughness(raptor) shouldBe 4
        oil(driver, raptor) shouldBe 1
    }

    test("can't activate without an oil counter") {
        val driver = newDriver()
        val raptor = driver.putCreatureOnBattlefield(driver.player1, "Gitaxian Raptor")
        oil(driver, raptor) shouldBe 0
        driver.submitExpectFailure(ActivateAbility(playerId = driver.player1, sourceId = raptor, abilityId = pumpId))
    }
})
