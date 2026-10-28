package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.TurntimberSymbiosis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Turntimber Symbiosis (ZNR #215) — {4}{G}{G}{G} Sorcery.
 *
 *   Look at the top seven cards of your library. You may put a creature card from among them onto
 *   the battlefield. If that card has mana value 3 or less, it enters with three additional +1/+1
 *   counters on it. Put the rest on the bottom of your library in a random order.
 *
 * The counters hinge on the chosen card's mana value, read before it moves.
 */
class TurntimberSymbiosisScenarioTest : FunSpec({

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Seven known cards on top: [creature] plus six Forests. Returns the caster, the creature and the Forests. */
    fun setup(creature: String): Triple<GameTestDriver, EntityId, Pair<EntityId, List<EntityId>>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(TurntimberSymbiosis))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!
        val forests = (1..6).map { d.putCardOnTopOfLibrary(you, "Forest") }
        val creatureId = d.putCardOnTopOfLibrary(you, creature)

        val spell = d.putCardInHand(you, "Turntimber Symbiosis")
        d.giveMana(you, Color.GREEN, 7)
        d.castSpell(you, spell).outcome shouldBe Outcome.Done
        d.bothPass()
        return Triple(d, you, creatureId to forests)
    }

    test("a creature with mana value 3 or less enters with three additional +1/+1 counters") {
        val (d, you, cards) = setup("Centaur Courser")
        val (courser, forests) = cards

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(you, listOf(courser))

        val permanent = d.findPermanent(you, "Centaur Courser")
        permanent shouldNotBe null
        d.plusOneCounters(permanent!!) shouldBe 3
        d.state.projectedState.getPower(permanent) shouldBe 6
        d.state.projectedState.getToughness(permanent) shouldBe 6

        d.state.getZone(ZoneKey(you, Zone.LIBRARY)).takeLast(6) shouldContainExactlyInAnyOrder forests
    }

    test("a creature with mana value 4 or more enters without counters") {
        val (d, you, cards) = setup("Force of Nature")
        val (force, _) = cards

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(you, listOf(force))

        val permanent = d.findPermanent(you, "Force of Nature")
        permanent shouldNotBe null
        d.plusOneCounters(permanent!!) shouldBe 0
    }

    test("declining puts all seven cards on the bottom") {
        val (d, you, cards) = setup("Centaur Courser")
        val (courser, forests) = cards

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(you, emptyList())

        d.findPermanent(you, "Centaur Courser") shouldBe null
        d.state.getZone(ZoneKey(you, Zone.LIBRARY)).takeLast(7) shouldContainExactlyInAnyOrder forests + courser
    }
})
