package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.Jumpstart2022Wastes834
import com.wingedsheep.mtg.sets.definitions.tmp.cards.ReflectingPool
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Reflecting Pool — "{T}: Add one mana of any type that a land you control could
 * produce."
 *
 * Unlike Naga Vitalist, the Pool is itself one of the lands it reads, so it must not reflect its own
 * abilities: alone or beside only other Pools it makes nothing, and its own {C} half never unlocks
 * itself.
 */
class ReflectingPoolScenarioTest : FunSpec({

    val colorAbility = ReflectingPool.activatedAbilities[0].id
    val colorlessAbility = ReflectingPool.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ReflectingPool)
        driver.registerCard(Jumpstart2022Wastes834)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.canActivate(player: EntityId, source: EntityId, abilityId: Any): Boolean =
        legalActions(player).any {
            val action = it.action as? ActivateAbility
            action != null && action.sourceId == source && action.abilityId == abilityId
        }

    test("beside a Forest it makes green, and its own {C} half doesn't unlock itself") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val reflectingPool = driver.putLandOnBattlefield(me, "Reflecting Pool")
        driver.putLandOnBattlefield(me, "Forest")

        withClue("no other land could produce {C}") {
            driver.canActivate(me, reflectingPool, colorlessAbility) shouldBe false
        }
        driver.submit(ActivateAbility(me, reflectingPool, colorAbility, manaColorChoice = Color.GREEN))
            .outcome shouldBe Outcome.Done
        driver.pool(me).green shouldBe 1
    }

    test("a tapped Wastes you control makes colorless available") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val reflectingPool = driver.putLandOnBattlefield(me, "Reflecting Pool")
        driver.tapPermanent(driver.putLandOnBattlefield(me, "Wastes"))

        driver.canActivate(me, reflectingPool, colorlessAbility) shouldBe true
        driver.submit(ActivateAbility(me, reflectingPool, colorlessAbility)).outcome shouldBe Outcome.Done
        driver.pool(me).colorless shouldBe 1
    }

    test("two Reflecting Pools and no other lands make nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val first = driver.putLandOnBattlefield(me, "Reflecting Pool")
        val second = driver.putLandOnBattlefield(me, "Reflecting Pool")

        driver.canActivate(me, first, colorlessAbility) shouldBe false
        driver.canActivate(me, second, colorlessAbility) shouldBe false
        driver.submit(ActivateAbility(me, first, colorAbility, manaColorChoice = Color.WHITE))
        withClue("a Pool reflecting only another Pool adds no mana") {
            val pool = driver.pool(me)
            (pool.white + pool.blue + pool.black + pool.red + pool.green + pool.colorless) shouldBe 0
        }
    }

    test("an opponent's lands don't widen it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val reflectingPool = driver.putLandOnBattlefield(me, "Reflecting Pool")
        driver.putLandOnBattlefield(me, "Forest")
        driver.putLandOnBattlefield(driver.getOpponent(me), "Island")

        driver.submit(ActivateAbility(me, reflectingPool, colorAbility, manaColorChoice = Color.BLUE))
        withClue("blue isn't on offer; green, from my Forest, is the only mana that can come out") {
            val pool = driver.pool(me)
            pool.blue shouldBe 0
            (pool.white + pool.black + pool.red + pool.colorless) shouldBe 0
        }
    }
})
