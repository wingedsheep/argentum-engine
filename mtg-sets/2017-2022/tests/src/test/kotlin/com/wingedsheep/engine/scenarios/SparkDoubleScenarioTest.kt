package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.ChandraFireArtisan
import com.wingedsheep.mtg.sets.definitions.war.cards.FeatherTheRedeemed
import com.wingedsheep.mtg.sets.definitions.war.cards.SparkDouble
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Spark Double (WAR #68) — {3}{U} Creature — Illusion 0/0.
 * "You may have this creature enter as a copy of a creature or planeswalker you control, except it
 * enters with an additional +1/+1 counter on it if it's a creature, it enters with an additional
 * loyalty counter on it if it's a planeswalker, and it isn't legendary."
 */
class SparkDoubleScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SparkDouble, ChandraFireArtisan, FeatherTheRedeemed))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.counters(id: EntityId, type: CounterType) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun GameTestDriver.castDouble(p: EntityId, copy: EntityId?, offered: (List<EntityId>) -> Unit = {}): EntityId {
        val double = putCardInHand(p, "Spark Double")
        giveMana(p, Color.BLUE, 4)
        castSpell(p, double).error shouldBe null
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        offered(decision.options)
        submitCardSelection(p, listOfNotNull(copy)).error shouldBe null
        return double
    }

    test("copying a creature: an additional +1/+1 counter and no loyalty counter") {
        val d = driver()
        val p = d.activePlayer!!
        val giant = d.putCreatureOnBattlefield(p, "Hill Giant")
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(p), "Grizzly Bears")

        val double = d.castDouble(p, giant) { options ->
            options shouldContain giant
            withClue("only creatures and planeswalkers you control are offered") { options shouldNotContain theirs }
        }

        d.state.getEntity(double)!!.get<CardComponent>()!!.name shouldBe "Hill Giant"
        d.counters(double, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.counters(double, CounterType.LOYALTY) shouldBe 0
        d.state.projectedState.getPower(double) shouldBe 4
        d.state.projectedState.getToughness(double) shouldBe 4
    }

    test("copying a legendary creature: the copy isn't legendary, so both stay") {
        val d = driver()
        val p = d.activePlayer!!
        val feather = d.putCreatureOnBattlefield(p, "Feather, the Redeemed")

        val double = d.castDouble(p, feather)

        d.state.projectedState.isLegendary(double) shouldBe false
        d.state.projectedState.isLegendary(feather) shouldBe true
        d.state.getBattlefield() shouldContain double
        d.state.getBattlefield() shouldContain feather
        d.counters(double, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("copying a planeswalker: printed loyalty plus one, no +1/+1 counter, both stay") {
        val d = driver()
        val p = d.activePlayer!!
        val chandra = d.putPermanentOnBattlefield(p, "Chandra, Fire Artisan")
        // The helper places no starting loyalty; give the original its printed 4 so it survives SBAs.
        d.replaceState(d.state.updateEntity(chandra) { it.with(CountersComponent(mapOf(CounterType.LOYALTY to 4))) })

        val double = d.castDouble(p, chandra)

        d.state.getEntity(double)!!.get<CardComponent>()!!.name shouldBe "Chandra, Fire Artisan"
        withClue("Chandra's printed loyalty is 4") { d.counters(double, CounterType.LOYALTY) shouldBe 5 }
        d.counters(double, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.projectedState.isLegendary(double) shouldBe false
        d.state.getBattlefield() shouldContain double
        d.state.getBattlefield() shouldContain chandra
    }

    test("declining the copy: a 0/0 Illusion with no counters, which dies") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, "Hill Giant")

        val double = d.castDouble(p, null)

        d.state.getBattlefield() shouldNotContain double
        d.state.getGraveyard(p) shouldContain double
    }
})
