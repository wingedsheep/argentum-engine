package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.EvershrikesGift
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Evershrike's Gift — graveyard-activated ability with a Blight cost.
 *
 * Regression for a bug where [GraveyardAbilityEnumerator] didn't surface Blight as
 * an additional cost, so the UI never prompted for a blight target and activation
 * silently failed with "No blight target chosen".
 */
class EvershrikesGiftTest : FunSpec({

    val graveyardAbility = EvershrikesGift.activatedAbilities.first { ability ->
        ability.activateFromZone == com.wingedsheep.sdk.core.Zone.GRAVEYARD
    }
    val abilityId = graveyardAbility.id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(EvershrikesGift)
        return driver
    }

    test("graveyard activation exposes Blight additional cost in legal actions") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInGraveyard(activePlayer, "Evershrike's Gift")
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Give enough mana ({1}{W})
        repeat(2) { driver.putLandOnBattlefield(activePlayer, "Plains") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val legalActions = enumerator.enumerate(driver.state, activePlayer)
        val activate = legalActions.firstOrNull { action ->
            action.actionType == "ActivateAbility" &&
                (action.action as? ActivateAbility)?.abilityId == abilityId
        }
        activate shouldNotBe null
        val costInfo = activate!!.additionalCostInfo
        costInfo shouldNotBe null
        costInfo!!.costType shouldBe "Blight"
        costInfo.blightAmount shouldBe 2
        costInfo.validBlightTargets.isNotEmpty() shouldBe true
    }

    test("activating with blight target returns card and places two -1/-1 counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giftInGraveyard = driver.putCardInGraveyard(activePlayer, "Evershrike's Gift")
        val target = driver.putCreatureOnBattlefield(activePlayer, "Force of Nature")
        driver.removeSummoningSickness(target)
        repeat(2) { driver.putLandOnBattlefield(activePlayer, "Plains") }

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = giftInGraveyard,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(blightTargets = listOf(target))
            )
        )
        result.isSuccess shouldBe true

        // Resolve the MoveToZone(Self, HAND) effect
        driver.bothPass()
        while (driver.isPaused) {
            driver.submitCardSelection(activePlayer, emptyList())
        }

        driver.state.getZone(ZoneKey(activePlayer, Zone.HAND)).contains(giftInGraveyard) shouldBe true
        driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD)).contains(giftInGraveyard) shouldBe false

        val counters = driver.state.getEntity(target)?.get<CountersComponent>()
        counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
    }

    test("activation is not legal when controller has no creatures to blight") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInGraveyard(activePlayer, "Evershrike's Gift")
        repeat(2) { driver.putLandOnBattlefield(activePlayer, "Plains") }

        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val legalActions = enumerator.enumerate(driver.state, activePlayer)
        val activate = legalActions.firstOrNull { action ->
            action.actionType == "ActivateAbility" &&
                (action.action as? ActivateAbility)?.abilityId == abilityId
        }
        activate shouldBe null
    }
})
