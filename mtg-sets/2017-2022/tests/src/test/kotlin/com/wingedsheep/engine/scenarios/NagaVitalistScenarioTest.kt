package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.NagaVitalist
import com.wingedsheep.mtg.sets.definitions.j22.cards.Jumpstart2022Wastes834
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Naga Vitalist — "{T}: Add one mana of any type that a land you control could
 * produce."
 *
 * "Type" includes colorless, so with a Wastes it can make {C}; with only a Forest it makes green and
 * nothing else. Both halves read only *your* lands — an opponent's Wastes or Island doesn't widen it.
 */
class NagaVitalistScenarioTest : FunSpec({

    val colorAbility = NagaVitalist.activatedAbilities[0].id
    val colorlessAbility = NagaVitalist.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NagaVitalist)
        driver.registerCard(Jumpstart2022Wastes834)
        return driver
    }

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.canActivate(player: EntityId, source: EntityId, abilityId: Any): Boolean =
        legalActions(player).any {
            val action = it.action as? ActivateAbility
            action != null && action.sourceId == source && action.abilityId == abilityId
        }

    fun setUp(driver: GameTestDriver): Pair<EntityId, EntityId> {
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val naga = driver.putCreatureOnBattlefield(me, "Naga Vitalist")
        driver.removeSummoningSickness(naga)
        return me to naga
    }

    test("a Forest makes green available") {
        val driver = createDriver()
        val (me, naga) = setUp(driver)
        driver.putLandOnBattlefield(me, "Forest")

        driver.submit(ActivateAbility(me, naga, colorAbility, manaColorChoice = Color.GREEN))
            .outcome shouldBe Outcome.Done
        driver.pool(me).green shouldBe 1
    }

    test("a Wastes you control makes colorless available, even tapped") {
        val driver = createDriver()
        val (me, naga) = setUp(driver)
        val wastes = driver.putLandOnBattlefield(me, "Wastes")
        driver.tapPermanent(wastes)

        withClue("the {C} half is offered while a land you control could produce {C}") {
            driver.canActivate(me, naga, colorlessAbility) shouldBe true
        }
        driver.submit(ActivateAbility(me, naga, colorlessAbility)).outcome shouldBe Outcome.Done
        driver.pool(me).colorless shouldBe 1
    }

    test("with only colored lands, colorless isn't available") {
        val driver = createDriver()
        val (me, naga) = setUp(driver)
        driver.putLandOnBattlefield(me, "Forest")

        driver.canActivate(me, naga, colorlessAbility) shouldBe false
        driver.canActivate(me, naga, colorAbility) shouldBe true
    }

    test("an opponent's lands don't widen it") {
        val driver = createDriver()
        val (me, naga) = setUp(driver)
        val opponent = driver.getOpponent(me)
        driver.putLandOnBattlefield(me, "Forest")
        driver.putLandOnBattlefield(opponent, "Wastes")
        driver.putLandOnBattlefield(opponent, "Island")

        withClue("the opponent's Wastes doesn't enable {C}") {
            driver.canActivate(me, naga, colorlessAbility) shouldBe false
        }
        driver.submit(ActivateAbility(me, naga, colorAbility, manaColorChoice = Color.BLUE))
        withClue("blue is what the opponent's Island makes; Naga reads my lands only") {
            driver.pool(me).blue shouldBe 0
        }
    }
})
