package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.RetrofittedTransmogrant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Retrofitted Transmogrant — {B} Artifact Creature — Zombie, 1/1.
 *
 * "{3}{B}: Return this card from your graveyard to the battlefield tapped with two +1/+1 counters
 *  on it."
 */
class RetrofittedTransmograntScenarioTest : FunSpec({

    val abilityId = RetrofittedTransmogrant.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RetrofittedTransmogrant)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("{3}{B} returns it from the graveyard tapped as a 3/3 with two +1/+1 counters") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val card = driver.putCardInGraveyard(me, "Retrofitted Transmogrant")
        driver.giveMana(me, Color.BLACK, 4)

        driver.submit(ActivateAbility(playerId = me, sourceId = card, abilityId = abilityId)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.isPaused shouldBe false

        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(card) shouldBe true
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(card) shouldBe false
        driver.isTapped(card) shouldBe true

        val counters = driver.state.getEntity(card)?.get<CountersComponent>()?.counters ?: emptyMap()
        counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
        driver.state.projectedState.getPower(card) shouldBe 3
        driver.state.projectedState.getToughness(card) shouldBe 3
    }
})
