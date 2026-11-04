package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * `ZoneChangeCause.NotCycled` — "and it wasn't cycled" (Abandoned Sarcophagus).
 *
 * A card is cycled when it is discarded to pay a cycling cost (CR 702.29c); typecycling costs are
 * cycling costs (CR 702.29f). The cycling handlers stamp the card through
 * `discardCards(asCyclingCost = true)`, so the replacement lets exactly that move through and
 * catches every other route into the graveyard. The trigger form (`isDiscarded(cause = NotCycled)`)
 * reads the same flag off the discard event.
 */
class NotCycledZoneChangeCauseTest : FunSpec({

    val exiler = card("Not Cycled Exiler") {
        manaCost = "{3}"
        typeLine = "Artifact"
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Any.withCycling().nontoken().ownedByYou(),
                    to = Zone.GRAVEYARD,
                ),
                requiredCause = ZoneChangeCause.NotCycled,
            )
        )
    }
    val discardOne = card("Discard One Test") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }
    val millTwo = card("Mill Two Test") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.mill(2) }
    }
    val payoff = card("Uncycled Discard Payoff") {
        manaCost = "{3}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.cycling("{1}"))
        triggeredAbility {
            trigger = Triggers.self.isDiscarded(cause = ZoneChangeCause.NotCycled)
            effect = Effects.GainLife(3)
        }
    }
    val typecycler = card("Islandcycling Test") {
        manaCost = "{4}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.typecycling("Island", ManaCost.parse("{1}")))
    }

    fun newDriver(withExiler: Boolean = true): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        listOf(exiler, discardOne, millTwo, payoff, typecycler).forEach(d::registerCard)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (withExiler) d.putPermanentOnBattlefield(d.player1, exiler.name)
        return d
    }

    test("a card discarded to pay its own cycling cost was cycled — it reaches the graveyard") {
        val d = newDriver()
        val angel = d.putCardInHand(d.player1, "Angel of the God-Pharaoh")
        d.giveMana(d.player1, Color.BLUE, 2)
        d.submit(CycleCard(d.player1, angel)).error shouldBe null
        d.getGraveyard(d.player1) shouldContain angel
        d.getExile(d.player1) shouldNotContain angel
    }

    test("typecycling is cycling (CR 702.29f) — the typecycled card reaches the graveyard") {
        val d = newDriver()
        val card = d.putCardInHand(d.player1, typecycler.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.submit(TypecycleCard(d.player1, card)).error shouldBe null
        d.getGraveyard(d.player1) shouldContain card
        d.getExile(d.player1) shouldNotContain card
    }

    test("a cycling card discarded to an effect wasn't cycled — it is exiled") {
        val d = newDriver()
        val angel = d.putCardInHand(d.player1, "Angel of the God-Pharaoh")
        val spell = d.putCardInHand(d.player1, discardOne.name)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpell(d.player1, spell).outcome shouldBe Outcome.Done
        d.bothPass()
        if (d.pendingDecision != null) d.submitCardSelection(d.player1, listOf(angel))
        d.getExile(d.player1) shouldContain angel
        d.getGraveyard(d.player1) shouldNotContain angel
    }

    test("a milled cycling card is exiled; a card without cycling is not") {
        val d = newDriver()
        val angel = d.putCardOnTopOfLibrary(d.player1, "Angel of the God-Pharaoh")
        val bears = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, millTwo.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, spell).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getExile(d.player1) shouldContain angel
        d.getGraveyard(d.player1) shouldContain bears
    }

    test("only cards its controller owns — another player's milled cycling card is untouched") {
        val d = newDriver(withExiler = false)
        d.putPermanentOnBattlefield(d.player2, exiler.name)
        val angel = d.putCardOnTopOfLibrary(d.player1, "Angel of the God-Pharaoh")
        val spell = d.putCardInHand(d.player1, millTwo.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, spell).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getGraveyard(d.player1) shouldContain angel
    }

    test("the trigger form skips the cycling discard and fires on any other discard") {
        val d = newDriver(withExiler = false)
        val cycled = d.putCardInHand(d.player1, payoff.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.submit(CycleCard(d.player1, cycled)).error shouldBe null
        if (d.state.stack.isNotEmpty()) d.bothPass() // resolve the cycling draw
        d.state.stack.size shouldBe 0
        d.getLifeTotal(d.player1) shouldBe 20

        val discarded = d.putCardInHand(d.player1, payoff.name)
        val spell = d.putCardInHand(d.player1, discardOne.name)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpell(d.player1, spell).outcome shouldBe Outcome.Done
        d.bothPass()
        if (d.pendingDecision != null) d.submitCardSelection(d.player1, listOf(discarded))
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23
    }
})
