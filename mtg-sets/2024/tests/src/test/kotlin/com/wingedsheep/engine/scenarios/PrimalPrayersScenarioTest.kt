package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.PrimalPrayers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Primal Prayers (MH3) — two energy on entry; creature spells with mana value 3 or less may be cast
 * by paying {E} instead of their mana costs, and a spell cast that way may be cast as though it had
 * flash. The flash belongs to the {E} cast only: the same creature cast for mana stays sorcery-speed.
 */
class PrimalPrayersScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + PrimalPrayers)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    fun GameTestDriver.castsOf(card: EntityId) =
        legalActions(player1).filter { (it.action as? CastSpell)?.cardId == card }
    fun GameTestDriver.castForEnergy(card: EntityId) = submit(
        CastSpell(player1, card, useAlternativeCost = true, alternativeCostType = AlternativeCostType.GRANTED)
    )

    test("entering gives two energy") {
        val d = driver()
        val prayers = d.putCardInHand(d.player1, "Primal Prayers")
        d.giveMana(d.player1, Color.GREEN, 4)
        d.castSpell(d.player1, prayers).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.energy() shouldBe 2
    }

    test("a mana value 3 or less creature is cast for one energy and no mana") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Primal Prayers")
        d.giveEnergy(2)
        val bears = d.putCardInHand(d.player1, "Grizzly Bears")

        d.castsOf(bears).any { it.actionType == "CastWithAlternativeCost" } shouldBe true
        d.castForEnergy(bears).error shouldBe null
        d.energy() shouldBe 1
        d.bothPass()
        d.findPermanent(d.player1, "Grizzly Bears") shouldNotBe null
    }

    test("the energy cast may be made at instant speed, the mana cast may not") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Primal Prayers")
        d.giveEnergy(1)
        val bears = d.putCardInHand(d.player1, "Grizzly Bears")
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.giveMana(d.player1, Color.GREEN, 2)

        val offered = d.castsOf(bears)
        offered.map { it.actionType } shouldBe listOf("CastWithAlternativeCost")
        (offered.single().action as CastSpell).alternativeCostType shouldBe AlternativeCostType.GRANTED

        d.submitExpectFailure(CastSpell(d.player1, bears))
        d.castForEnergy(bears).error shouldBe null
        d.energy() shouldBe 0
        d.bothPass()
        d.findPermanent(d.player1, "Grizzly Bears") shouldNotBe null
    }

    test("a creature with mana value 4 or more is not covered") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Primal Prayers")
        d.giveEnergy(2)
        val force = d.putCardInHand(d.player1, "Force of Nature")

        d.castsOf(force).filter { it.actionType == "CastWithAlternativeCost" }.shouldBeEmpty()
        d.castForEnergy(force).error shouldNotBe null
        d.energy() shouldBe 2
    }

    test("a noncreature spell with mana value 3 or less is not covered") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Primal Prayers")
        d.giveEnergy(2)
        val growth = d.putCardInHand(d.player1, "Giant Growth")

        d.castsOf(growth).filter { it.actionType == "CastWithAlternativeCost" }.shouldBeEmpty()
        d.energy() shouldBe 2
    }

    test("without energy there is no energy cast, and no instant-speed cast at all") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Primal Prayers")
        val bears = d.putCardInHand(d.player1, "Grizzly Bears")
        d.castsOf(bears).filter { it.actionType == "CastWithAlternativeCost" }.shouldBeEmpty()

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.giveMana(d.player1, Color.GREEN, 2)
        d.castsOf(bears).shouldBeEmpty()
    }
})
