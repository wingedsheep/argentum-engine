package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.apc.cards.LayOfTheLand
import com.wingedsheep.mtg.sets.definitions.m15.cards.ObNixilisUnshackled
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ob Nixilis, Unshackled — {4}{B}{B} 4/4 flying, trample. "Whenever an opponent searches their
 * library, that player sacrifices a creature of their choice and loses 10 life. Whenever another
 * creature dies, put a +1/+1 counter on Ob Nixilis."
 *
 * "That player" must be the searcher, never Ob Nixilis's controller, and the creature they
 * sacrifice dying grows Ob Nixilis.
 */
class ObNixilisUnshackledScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ObNixilisUnshackled, LayOfTheLand))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        return d
    }

    fun GameTestDriver.plusOneCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while ((pendingDecision != null || stackSize > 0) && guard++ < 40) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    test("an opponent's search makes them sacrifice a creature and lose 10 life; the death grows Ob Nixilis") {
        val d = driver()
        val searcher = d.activePlayer!!
        val me = d.getOpponent(searcher)
        val ob = d.putCreatureOnBattlefield(me, "Ob Nixilis, Unshackled")
        val myLions = d.putCreatureOnBattlefield(me, "Savannah Lions")
        val theirCourser = d.putCreatureOnBattlefield(searcher, "Centaur Courser")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.giveMana(searcher, Color.GREEN, 1)
        d.castSpell(searcher, d.putCardInHand(searcher, "Lay of the Land")).outcome shouldBe Outcome.Done
        d.resolveAll()

        withClue("the searcher loses 10") { d.getLifeTotal(searcher) shouldBe 10 }
        withClue("Ob Nixilis's controller loses nothing") { d.getLifeTotal(me) shouldBe 20 }
        withClue("the searcher sacrificed their creature") { d.getGraveyard(searcher).contains(theirCourser) shouldBe true }
        withClue("Ob Nixilis's controller kept theirs") { d.findPermanent(me, "Savannah Lions") shouldBe myLions }
        withClue("another creature died") { d.plusOneCounters(ob) shouldBe 1 }
    }

    test("a searcher with no creature still loses 10 life") {
        val d = driver()
        val searcher = d.activePlayer!!
        val me = d.getOpponent(searcher)
        val ob = d.putCreatureOnBattlefield(me, "Ob Nixilis, Unshackled")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.giveMana(searcher, Color.GREEN, 1)
        d.castSpell(searcher, d.putCardInHand(searcher, "Lay of the Land")).outcome shouldBe Outcome.Done
        d.resolveAll()

        d.getLifeTotal(searcher) shouldBe 10
        withClue("nothing died") { d.plusOneCounters(ob) shouldBe 0 }
    }

    test("the controller's own search does not trigger it") {
        val d = driver()
        val me = d.activePlayer!!
        val ob = d.putCreatureOnBattlefield(me, "Ob Nixilis, Unshackled")
        val lions = d.putCreatureOnBattlefield(me, "Savannah Lions")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.giveMana(me, Color.GREEN, 1)
        d.castSpell(me, d.putCardInHand(me, "Lay of the Land")).outcome shouldBe Outcome.Done
        d.resolveAll()

        d.getLifeTotal(me) shouldBe 20
        d.findPermanent(me, "Savannah Lions") shouldBe lions
        d.plusOneCounters(ob) shouldBe 0
    }
})
