package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.NightDealings
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Night Dealings (CHK #132) — "Whenever a source you control deals damage to another player, put that
 * many theft counters on this enchantment. {2}{B}{B}, Remove X theft counters from this enchantment:
 * Search your library for a nonland card with mana value X, reveal it, put it into your hand, then
 * shuffle."
 */
class NightDealingsScenarioTest : FunSpec({

    val tutor = NightDealings.activatedAbilities[0].id

    fun GameTestDriver.theft(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.THEFT) ?: 0

    /** Pass around the table until the stack is empty — works for any seat count. */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) passPriority(state.priorityPlayerId!!)
    }

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED)
        castSpell(caster, bolt, listOf(target)).outcome shouldBe Outcome.Done
        resolveStack()
    }

    fun duel(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + NightDealings)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("damage your source deals to an opponent becomes that many theft counters") {
        val d = duel()
        val dealings = d.putPermanentOnBattlefield(d.player1, "Night Dealings")

        d.bolt(d.player1, d.player2)
        d.resolveStack()

        d.getLifeTotal(d.player2) shouldBe 17
        d.theft(dealings) shouldBe 3
    }

    test("damage to yourself is not damage to another player") {
        val d = duel()
        val dealings = d.putPermanentOnBattlefield(d.player1, "Night Dealings")

        d.bolt(d.player1, d.player1)

        d.getLifeTotal(d.player1) shouldBe 17
        d.theft(dealings) shouldBe 0
    }

    test("an opponent's source damaging its own controller's opponent (you) adds nothing") {
        val d = duel()
        val dealings = d.putPermanentOnBattlefield(d.player1, "Night Dealings")

        d.passPriority(d.player1)
        d.bolt(d.player2, d.player1)

        d.theft(dealings) shouldBe 0
    }

    test("removing X theft counters finds a nonland card with mana value exactly X") {
        val d = duel()
        val dealings = d.putPermanentOnBattlefield(d.player1, "Night Dealings")
        d.bolt(d.player1, d.player2)
        d.resolveStack()
        d.theft(dealings) shouldBe 3

        val bears = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val boltInLibrary = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")

        d.giveMana(d.player1, Color.BLACK, 2)
        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, dealings, tutor, xValue = 2)).outcome shouldBe Outcome.Done
        withClue("the X theft counters are paid as the cost") { d.theft(dealings) shouldBe 1 }
        d.resolveStack()

        val decision = d.pendingDecision as SelectCardsDecision
        withClue("only the mana-value-2 nonland card is findable — not the Bolt, not a Swamp") {
            decision.options shouldContainExactly listOf(bears)
        }
        d.submitCardSelection(d.player1, listOf(bears))

        d.state.getZone(d.player1, Zone.HAND).contains(bears) shouldBe true
        d.state.getZone(d.player1, Zone.LIBRARY).contains(boltInLibrary) shouldBe true
    }

    test("X can't exceed the theft counters on it") {
        val d = duel()
        val dealings = d.putPermanentOnBattlefield(d.player1, "Night Dealings")
        d.bolt(d.player1, d.player2)
        d.resolveStack()

        d.giveMana(d.player1, Color.BLACK, 2)
        d.giveColorlessMana(d.player1, 2)
        (d.submit(ActivateAbility(d.player1, dealings, tutor, xValue = 4)).outcome is Outcome.Done) shouldBe false
        d.theft(dealings) shouldBe 3
    }

    test("Two-Headed Giant: damage to your teammate counts — another player, not an opponent") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + NightDealings)
        val seats = d.initMultiplayer(
            decks = List(4) { Deck.of("Swamp" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, teammate) = seats[0] to seats[1]
        val dealings = d.putPermanentOnBattlefield(you, "Night Dealings")

        d.bolt(you, teammate)
        d.resolveStack()

        d.theft(dealings) shouldBe 3
    }
})
