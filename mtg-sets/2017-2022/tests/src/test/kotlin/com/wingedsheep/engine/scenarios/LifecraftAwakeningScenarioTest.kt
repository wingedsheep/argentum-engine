package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.LifecraftAwakening
import com.wingedsheep.mtg.sets.definitions.aer.cards.UntetheredExpress
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Lifecraft Awakening (AER) — X +1/+1 counters on an artifact you control; a noncreature,
 * non-Vehicle artifact also becomes a base 0/0 Construct artifact creature for good.
 */
class LifecraftAwakeningScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + LifecraftAwakening + UntetheredExpress + Bonesplitter)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(4) { _ -> it.putLandOnBattlefield(it.player1, "Forest") }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castAwakening(x: Int, target: EntityId) {
        val spell = putCardInHand(player1, "Lifecraft Awakening")
        castXSpell(player1, spell, x, listOf(target)).error shouldBe null
        bothPass()
    }

    test("a noncreature artifact becomes a 0/0 Construct artifact creature with X counters") {
        val d = driver()
        val equipment = d.putPermanentOnBattlefield(d.player1, "Bonesplitter")
        d.state.projectedState.isCreature(equipment) shouldBe false

        d.castAwakening(3, equipment)

        val projected = d.state.projectedState
        d.plusOneCounters(equipment) shouldBe 3
        projected.isCreature(equipment) shouldBe true
        projected.hasType(equipment, "ARTIFACT") shouldBe true
        projected.hasSubtype(equipment, "Construct") shouldBe true
        projected.getPower(equipment) shouldBe 3
        projected.getToughness(equipment) shouldBe 3
    }

    test("a Vehicle gets the counters but does not become a creature") {
        val d = driver()
        val vehicle = d.putPermanentOnBattlefield(d.player1, "Untethered Express")

        d.castAwakening(2, vehicle)

        val projected = d.state.projectedState
        d.plusOneCounters(vehicle) shouldBe 2
        withClue("Vehicle is excluded from the animation") { projected.isCreature(vehicle) shouldBe false }
        projected.hasSubtype(vehicle, "Construct") shouldBe false
    }

    test("an artifact creature keeps its stats and types and just gets the counters") {
        val d = driver()
        val golem = d.putPermanentOnBattlefield(d.player1, "Artifact Creature")

        d.castAwakening(2, golem)

        val projected = d.state.projectedState
        d.plusOneCounters(golem) shouldBe 2
        projected.getPower(golem) shouldBe 4
        projected.getToughness(golem) shouldBe 4
        projected.hasSubtype(golem, "Golem") shouldBe true
        projected.hasSubtype(golem, "Construct") shouldBe false
    }

    test("X = 0 on a noncreature artifact makes a 0/0 that dies") {
        val d = driver()
        val equipment = d.putPermanentOnBattlefield(d.player1, "Bonesplitter")

        d.castAwakening(0, equipment)

        d.findPermanent(d.player1, "Bonesplitter") shouldBe null
        d.getGraveyardCardNames(d.player1) shouldContain "Bonesplitter"
    }
})
