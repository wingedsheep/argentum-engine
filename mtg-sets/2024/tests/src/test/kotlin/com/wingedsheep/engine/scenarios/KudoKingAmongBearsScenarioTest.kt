package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.Weatherlight
import com.wingedsheep.mtg.sets.definitions.mh3.cards.KudoKingAmongBears
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kudo, King Among Bears (MH3) — other creatures have base P/T 2/2 and are Bears in addition to
 * their other types; counters still apply on top of the set base.
 */
class KudoKingAmongBearsScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + KudoKingAmongBears + Weatherlight)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("other creatures on both sides become 2/2 Bears; Kudo itself is unaffected") {
        val d = driver()
        val mine = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        d.state.projectedState.getPower(mine) shouldBe 3

        val kudo = d.putCreatureOnBattlefield(d.player1, "Kudo, King Among Bears")
        val projected = d.state.projectedState
        for (id in listOf(mine, theirs)) {
            projected.getPower(id) shouldBe 2
            projected.getToughness(id) shouldBe 2
            projected.hasSubtype(id, "Bear") shouldBe true
        }
        projected.hasSubtype(mine, "Centaur") shouldBe true
        projected.getPower(kudo) shouldBe 2
        projected.getToughness(kudo) shouldBe 2
    }

    test("+1/+1 counters apply on top of the 2/2 base") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Kudo, King Among Bears")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.replaceState(d.state.updateEntity(courser) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        })
        d.state.projectedState.getPower(courser) shouldBe 4
        d.state.projectedState.getToughness(courser) shouldBe 4
    }

    test("a Vehicle crewed after Kudo is a 2/2 Bear — crewing does not set power and toughness") {
        val d = driver()
        val kudo = d.putCreatureOnBattlefield(d.player1, "Kudo, King Among Bears")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val weatherlight = d.putPermanentOnBattlefield(d.player1, "Weatherlight")
        d.state.projectedState.isCreature(weatherlight) shouldBe false

        // Crew 3: Kudo (2) + the courser, now base 2/2 (2).
        d.submitSuccess(CrewVehicle(d.player1, weatherlight, listOf(kudo, courser)))
        d.bothPass()

        val projected = d.state.projectedState
        projected.isCreature(weatherlight) shouldBe true
        projected.getPower(weatherlight) shouldBe 2
        projected.getToughness(weatherlight) shouldBe 2
        projected.hasSubtype(weatherlight, "Bear") shouldBe true
        projected.hasSubtype(weatherlight, "Vehicle") shouldBe true
    }
})
