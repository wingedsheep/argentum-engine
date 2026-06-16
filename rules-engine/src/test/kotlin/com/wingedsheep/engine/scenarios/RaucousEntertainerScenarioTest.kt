package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RaucousEntertainer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Raucous Entertainer — {1}{G} 2/2 Creature — Plant Bard
 *
 * "{1}, {T}: Put a +1/+1 counter on each creature you control that entered this turn."
 *
 * Verifies that only creatures that entered the battlefield this turn (here, a freshly cast
 * Grizzly Bears) receive the counter — a pre-existing creature that did not enter this turn does
 * not. (Raucous itself was also placed without entering this turn, so it gets no counter either.)
 */
class RaucousEntertainerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("only creatures that entered this turn get the +1/+1 counter") {
        val driver = createDriver()
        val me = driver.player1

        val entertainer = driver.putCreatureOnBattlefield(me, "Raucous Entertainer")
        driver.removeSummoningSickness(entertainer)
        // A creature that was already in play (did not enter this turn).
        val old = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        // Cast a creature so it enters the battlefield THIS turn.
        val bearsCard = driver.putCardInHand(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bearsCard, targets = emptyList())
        driver.bothPass() // resolve the creature spell -> enters this turn
        // The freshly cast creature is the one carrying EnteredThisTurnComponent.
        val freshBears = driver.state.getBattlefield(me).first {
            driver.state.getEntity(it)
                ?.has<EnteredThisTurnComponent>() == true
        }

        // Activate Raucous Entertainer's ability.
        val abilityId = RaucousEntertainer.activatedAbilities.first().id
        driver.giveMana(me, Color.GREEN, 1)
        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = entertainer, abilityId = abilityId)
        )
        driver.bothPass()

        driver.plusOneCounters(freshBears) shouldBe 1
        driver.plusOneCounters(old) shouldBe 0
        driver.plusOneCounters(entertainer) shouldBe 0
    }
})
