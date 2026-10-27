package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.nph.cards.TriumphOfTheHordes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Triumph of the Hordes: until end of turn, creatures you control get +1/+1 and gain trample and
 * infect. The first card to *grant* infect by an effect — pins that granted infect turns the
 * pumped creature's combat damage to a player into poison counters instead of life loss.
 */
class TriumphOfTheHordesScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TriumphOfTheHordes)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.poison(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("pumped creatures gain trample and infect, and deal combat damage to players as poison") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.removeSummoningSickness(courser)
        val triumph = driver.putCardInHand(me, "Triumph of the Hordes")
        driver.giveMana(me, Color.GREEN, 4)

        driver.castSpell(me, triumph)
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(courser) shouldBe 4
        projected.getToughness(courser) shouldBe 4
        projected.hasKeyword(courser, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(courser, Keyword.INFECT) shouldBe true

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(courser), opponent)
        driver.bothPass()
        driver.declareNoBlockers(opponent)
        driver.bothPass()
        driver.bothPass()

        driver.poison(opponent) shouldBe 4
        driver.assertLifeTotal(opponent, 20)
    }
})
