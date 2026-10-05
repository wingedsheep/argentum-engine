package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.TolarianKraken
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tolarian Kraken (M21 #80) — "Whenever you draw a card, you may pay {1}. When you do, you may tap
 * or untap target creature."
 */
class TolarianKrakenScenarioTest : FunSpec({

    val drawOne = card("Test Draw One") {
        manaCost = "{U}"
        colorIdentity = "U"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
    }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(TolarianKraken, drawOne))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Tolarian Kraken")
        return Triple(d, me, d.getOpponent(me))
    }

    /** Cast the draw spell and resolve it plus the Kraken's draw trigger, stopping at the pay prompt. */
    fun drawACard(d: GameTestDriver, me: EntityId) {
        val spell = d.putCardInHand(me, "Test Draw One")
        d.giveMana(me, Color.BLUE, 1)
        d.castSpell(me, spell).error shouldBe null
        d.bothPass() // resolve the draw spell — the Kraken triggers
        d.bothPass() // resolve the draw trigger
    }

    listOf(0 to true, 1 to false).forEach { (mode, tappedAfter) ->
        test("paying {1} lets you ${if (mode == 0) "tap" else "untap"} target creature via a reflexive trigger") {
            val (d, me, opp) = setup()
            val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
            if (mode == 1) d.tapPermanent(bears)
            d.giveColorlessMana(me, 1)

            drawACard(d, me)

            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            d.submitYesNo(me, true).error shouldBe null

            val targets = d.pendingDecision as ChooseTargetsDecision
            d.submitDecision(me, TargetsResponse(targets.id, mapOf(0 to listOf(bears)))).error shouldBe null
            d.bothPass() // resolve the reflexive trigger

            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            d.submitYesNo(me, true).error shouldBe null
            val choice = d.pendingDecision as ChooseOptionDecision
            d.submitDecision(me, OptionChosenResponse(choice.id, mode)).error shouldBe null

            d.isTapped(bears) shouldBe tappedAfter
            d.state.getEntity(me)!!.get<ManaPoolComponent>()
                ?.colorless shouldBe 0
        }
    }

    test("declining to pay puts no reflexive trigger on the stack") {
        val (d, me, opp) = setup()
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.giveColorlessMana(me, 1)

        drawACard(d, me)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, false).error shouldBe null
        d.pendingDecision shouldBe null
        d.stackSize shouldBe 0
        d.isTapped(bears) shouldBe false
    }

    test("an opponent drawing a card does not trigger it") {
        val (d, me, opp) = setup()
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.giveColorlessMana(me, 1)
        d.passPriorityUntil(Step.DRAW) // opponent's turn draw step
        d.activePlayer shouldBe opp
        d.stackSize shouldBe 0
        d.isTapped(bears) shouldBe false
    }
})
