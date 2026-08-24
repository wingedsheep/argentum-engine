package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.CityOfShadows
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for City of Shadows.
 *
 * The pile has to accumulate and the mana has to scale with it, so the test charges twice and
 * checks the output is 2 rather than 1 — a mana ability that produced a flat {C}, or read the wrong
 * permanent's counters, would pass a single-charge test and fail this one. An uncharged City
 * producing nothing is the other end of the same assertion.
 */
class CityOfShadowsScenarioTest : FunSpec({

    val chargeAbilityId = CityOfShadows.activatedAbilities[0].id
    val manaAbilityId = CityOfShadows.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CityOfShadows)
        return driver
    }

    fun pool(driver: GameTestDriver, player: EntityId): ManaPoolComponent =
        driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("each exiled creature adds a storage counter, and the mana scales with the pile") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val city = driver.putPermanentOnBattlefield(me, "City of Shadows")
        val first = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        // Charge once...
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = city,
                abilityId = chargeAbilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(first)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("the creature paid the cost and is in exile") {
            driver.findPermanent(me, "Grizzly Bears") shouldBe null
            driver.getExileCardNames(me) shouldBe listOf("Grizzly Bears")
        }

        // ...and again, after untapping the City.
        driver.untapPermanent(city)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = city,
                abilityId = chargeAbilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(second)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.untapPermanent(city)
        driver.submit(ActivateAbility(playerId = me, sourceId = city, abilityId = manaAbilityId))
            .isSuccess shouldBe true

        withClue("two storage counters -> {C}{C}") {
            pool(driver, me).colorless shouldBe 2
        }
    }

    test("an uncharged City produces nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val city = driver.putPermanentOnBattlefield(me, "City of Shadows")
        driver.submit(ActivateAbility(playerId = me, sourceId = city, abilityId = manaAbilityId))

        withClue("no counters, no mana") {
            pool(driver, me).colorless shouldBe 0
        }
    }
})
