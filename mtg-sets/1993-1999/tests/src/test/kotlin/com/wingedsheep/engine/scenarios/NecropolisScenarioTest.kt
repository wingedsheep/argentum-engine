package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.Necropolis
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Necropolis — "Exile a creature card from your graveyard: Put X +0/+1 counters
 * on this creature, where X is the exiled card's mana value."
 *
 * The exile is a **cost**, so by resolution the card is already in exile and can no longer be read
 * off the graveyard. What is being proved here is that `CardSource.ExiledAsCost` still names it and
 * that the mana value read is the exiled card's, not a constant: two creature cards of different
 * cost must produce different counter counts.
 */
class NecropolisScenarioTest : FunSpec({

    val abilityId = Necropolis.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Necropolis)
        return driver
    }

    fun toughnessOf(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.projectedState.getToughness(id) ?: error("no projected toughness")

    test("counters equal the exiled creature card's mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val necropolis = driver.putPermanentOnBattlefield(me, "Necropolis")
        // Centaur Courser is {2}{G} — mana value 3.
        val fuel = driver.putCardInGraveyard(me, "Centaur Courser")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = necropolis,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(fuel)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("0/1 plus three +0/+1 counters") {
            toughnessOf(driver, necropolis) shouldBe 4
        }
        withClue("power stays 0 — +0/+1 counters") {
            driver.state.projectedState.getPower(necropolis) shouldBe 0
        }
        withClue("the fuel was exiled, not milled or left behind") {
            driver.getGraveyardCardNames(me) shouldBe emptyList()
            driver.getExileCardNames(me) shouldBe listOf("Centaur Courser")
        }
    }

    test("a costlier exile gives more counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val necropolis = driver.putPermanentOnBattlefield(me, "Necropolis")
        // Force of Nature is {3}{G}{G} — mana value 5.
        val fuel = driver.putCardInGraveyard(me, "Force of Nature")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = necropolis,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(exiledCards = listOf(fuel)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        toughnessOf(driver, necropolis) shouldBe 6
    }
})
